package com.kapil.aws.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.netty.util.internal.StringUtil;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest;
import software.amazon.awssdk.services.sqs.model.ListQueuesResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
public class SQSService {

	private static final Logger logger = LoggerFactory.getLogger(SQSService.class);

	private final SqsClient sqsClient;
	private final List<String> queueList = new ArrayList<String>();

	public SQSService(SqsClient sqsClient) {
		this.sqsClient = sqsClient;
	}

	public String getQueueURL(String queueName) {
		GetQueueUrlResponse getQueueUrlResponse = sqsClient
				.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
		return getQueueUrlResponse.queueUrl();
	}

	public String sendMessage(String queueName, String messageBody) {
		if(!isValidQueue(queueName)) {
			return "Entered Queue is not valid. Please try again with valid queue";
		}
		logger.info("Sending message '{}' into queue '{}'", messageBody, queueName);
		String queueUrl = getQueueURL(queueName);
		SendMessageRequest sendMessageRequest = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody)
				.build();

		SendMessageResponse sendMessageResponse = sqsClient.sendMessage(sendMessageRequest);
		return sendMessageResponse.messageId();
	}

	public Integer getTotalMessageInQueues(String queueName) {
		String queueUrl = getQueueURL(queueName);
		logger.info("Receving total count of message for queueURL '{}' for queue'{}'", queueUrl, queueName);

		GetQueueAttributesRequest attributesRequest = GetQueueAttributesRequest.builder().queueUrl(queueUrl)
				.attributeNames(Collections.singletonList(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)).build();

		GetQueueAttributesResponse attributesResponse = sqsClient.getQueueAttributes(attributesRequest);
		String unreadMessages = attributesResponse.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);

		logger.info("Total Unread Messages: " + unreadMessages);
		return unreadMessages != null ? Integer.parseInt(unreadMessages) : 0;
	}

	public List<Message> receiveMessages(String queueName) {
		if(!isValidQueue(queueName)) {
			return new ArrayList<>();
		}
		String queueUrl = getQueueURL(queueName);
		logger.info("Receving message for queueURL '{}'", queueUrl);

		ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder().queueUrl(queueUrl)
				.maxNumberOfMessages(5) // Max messages to fetch in one batch
				.visibilityTimeout(60) // Time for which the message will be invisible to others
				.build();

		ReceiveMessageResponse response = sqsClient.receiveMessage(receiveMessageRequest);

		for (Message message : response.messages()) {
			logger.info("Received message: '{}'", message.body());
			// Change visibility timeout to extend message processing time
			changeMessageVisibility(queueUrl, message, 30); // Retry after 30 seconds
			// Delete message from the queue after processing and mark as read
			deleteMessage(queueUrl, message);
		}

		return response.messages();
	}

	// Polls SQS queue for messages as same as listener
	public void listenForMessages(String queue) {
		String queueUrl = getQueueURL(queue);

		ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder().queueUrl(queueUrl)
				.maxNumberOfMessages(10) // Max messages to receive at once
				.waitTimeSeconds(20) // Long polling: wait for messages
				.build();

		while (true) {
			ReceiveMessageResponse response = sqsClient.receiveMessage(receiveMessageRequest);

			if (!response.messages().isEmpty()) {
				for (Message message : response.messages()) {
					logger.info("Received message: '{}'", message.body());
					// Process the message (your listener logic here)
					changeMessageVisibility(queueUrl, message, 30); // Retry after 30 seconds

					// Delete the message from the queue after processing it for marking as read
					deleteMessage(queueUrl, message);
				}
			}
		}
	}

	// list of all queue
	public List<String> getListQueues() {
		ListQueuesRequest listQueuesRequest = ListQueuesRequest.builder().build();
		ListQueuesResponse listQueuesResponse = sqsClient.listQueues(listQueuesRequest);
		queueList.addAll(listQueuesResponse.queueUrls());
		return listQueuesResponse.queueUrls();
	}

	// retry after sometime, move to DLQ
	public String handleMessageRetryAndMoveToDLQ(String queue, String deadLetterQueue) {
		String queueUrl = getQueueURL(queue);
		List<Message> messages = getMessagesForUpdation(queueUrl);
		StringBuffer strBuffer = new StringBuffer();
		for (Message message : messages) {
			strBuffer.append("'" + message.body() + "' ");
			// Change visibility timeout to delay further retries
			changeMessageVisibility(queueUrl, message, 60); // Retry after 60 seconds
			// Optionally move the message to the DLQ manually (you can also rely on SQS
			// redrive policy)
			sendToDLQ(deadLetterQueue, message);
		}
		return "Successfully, message [" + strBuffer.toString() + "] update it's visiblity and move it into DLQ.";
	}

	// Mark the message as read (delete it from the queue)
	public String processMessageForMarkAsRead(String queue) {
		String queueUrl = getQueueURL(queue);
		StringBuffer strBuffer = new StringBuffer();
		List<Message> messages = getMessagesForUpdation(queueUrl);
		for (Message message : messages) {
			strBuffer.append("'" + message.body() + "' ");
			deleteMessage(queueUrl, message); // Mark the message as read (delete it from the queue)
		}
		return "Successfully, message [" + strBuffer.toString() + "]  is marked as read in queue " + queue + ".";
	}

	// Improved version for handling message processing, retrying, and sending to
	// DLQ if failed
	public void receiveProcessAndHandleFailedMessages(String queue, String deadLetterQueue) {
		// get queueUrl to fetch message
		String queueUrl = getQueueURL(queue);

		// Receive messages from the queue
		List<Message> messages = getMessagesForUpdation(queueUrl);

		for (Message message : messages) {
			try {
				// Process the message (business logic goes here)
				processMessage(message);

				// uncomment below line to throw exception and retry after sometime, move to DLQ
				// throw new Exception("Invalid message received: " + message);

				// If processing is successful, delete the message to mark it as read
				deleteMessage(queueUrl, message);
			} catch (Exception e) {
				logger.error("Error processing, message '{}' to mark it as read ", message.messageId(), e.getMessage());
				logger.info("Updating message visibility into queue '{}' and move to DLQ '{}'", queue, deadLetterQueue);
				// If processing fails, adjust the visibility timeout to delay retries
				changeMessageVisibility(queueUrl, message, 60); // Retry after 60 seconds
				// Move the message to the Dead Letter Queue (DLQ) for further inspection
				sendToDLQ(deadLetterQueue, message);
			}
		}
	}

	// read message here and processing if we want like save s3 bucket, lambda
	// function etc
	private void processMessage(Message message) {
		System.out.println("During processing of messages: " + message.messageId() + " :: " + message.body());
	}

	private List<Message> getMessagesForUpdation(String queueUrl) {
		// Construct the request to receive messages
		ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder().queueUrl(queueUrl)
				.maxNumberOfMessages(2) // Adjust the number of messages you want to receive
				.visibilityTimeout(30) // Visibility timeout for each message in seconds
				.waitTimeSeconds(20) // Enable long polling for 20 seconds
				.build();

		ReceiveMessageResponse response = sqsClient.receiveMessage(receiveMessageRequest);
		List<Message> messages = response.messages();
		return messages;
	}

	// Change message visibility timeout (for retries)
	private void changeMessageVisibility(String queueUrl, Message message, int visibilityTimeout) {
		ChangeMessageVisibilityRequest visibilityRequest = ChangeMessageVisibilityRequest.builder().queueUrl(queueUrl)
				.receiptHandle(message.receiptHandle()).visibilityTimeout(visibilityTimeout).build();

		sqsClient.changeMessageVisibility(visibilityRequest);
		System.out.println("Message visibility changed: " + message.messageId() + " :: " + message.body());
	}

	// Send the message to DLQ manually
	private void sendToDLQ(String deadLetterQueue, Message message) {
		String deadLetterQueueUrl = getQueueURL(deadLetterQueue);

		SendMessageRequest sendMessageRequest = SendMessageRequest.builder().queueUrl(deadLetterQueueUrl) // DLQ URL
				.messageBody(message.body()).messageAttributes(message.messageAttributes()).build();

		sqsClient.sendMessage(sendMessageRequest);
		logger.info("Sucessfully, sent message '{}' into DLQ '{}'", message.body(), deadLetterQueue);

	}

	// Delete the message from the queue (mark it as read)
	private void deleteMessage(String queueUrl, Message message) {
		// Build the request to delete the message
		DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder().queueUrl(queueUrl)
				.receiptHandle(message.receiptHandle()) // The receipt handle is required to delete the message
				.build();

		// Send the delete request to SQS
		sqsClient.deleteMessage(deleteMessageRequest);

		// Log successful deletion
		logger.info("Sucessfully, mark as mesage and remove message '{}' from queue '{}'", message.body(), queueUrl);
	}

	
	public boolean isValidQueue(String queue) {
		boolean flag = StringUtil.isNullOrEmpty(queue)? false: true;
		if(flag) {
			flag = queueList.contains(queue);
			logger.info("Queue '{}' is valid or not '{}'", queue, flag);

		}
		
		return flag;
	}
}
