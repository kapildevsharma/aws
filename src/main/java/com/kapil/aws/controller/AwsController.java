package com.kapil.aws.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kapil.aws.dto.QueueRequest;
import com.kapil.aws.entities.Person;
import com.kapil.aws.service.PersonService;
import com.kapil.aws.service.S3Service;
import com.kapil.aws.service.SQSService;

import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.sqs.model.Message;

@RestController
@RequestMapping("/api/aws")
public class AwsController {

    private static final Logger logger = LoggerFactory.getLogger(AwsController.class);

    private final SQSService sqsService;
	private final PersonService personService;
	private final S3Service s3Service;

	public AwsController(SQSService sqsService, PersonService personService, S3Service s3Service) {
		this.sqsService = sqsService;
		this.personService = personService;
		this.s3Service = s3Service;
	}
	
	/** --------------------------------------Start SQS Services -------------------------------------- */
	/**
	 * Send message into queue of SQS , Example: {"message":"Share message125", "queue":"MyQueue"}
	 */
	@PostMapping("/sqs/send")
    public ResponseEntity<String> sendMessage(@RequestBody QueueRequest queueRequest) {
        logger.info("Sending message '{}' to queue '{}'", queueRequest.getMessage(), queueRequest.getQueueName());
        String messageId = sqsService.sendMessage(queueRequest.getQueueName(), queueRequest.getMessage());
        return ResponseEntity.ok("Message sent with ID: " + messageId);
    }
	
	@GetMapping("/sqs/receive")
	public ResponseEntity<List<String>> receiveMessage(@RequestParam String queue) {
		List<Message> messages = sqsService.receiveMessages(queue);
		List<String> formattedMessages = new ArrayList<String>();
		
		for (Message message : messages) {
			logger.info("Received Message '{}' with details '{}'" , message.body(), message.toString());
			formattedMessages.add("Message:: "+ message.body() + " , ");
		}
        logger.info("Received {} messages from queue '{}'", messages.size(), queue);
        return ResponseEntity.ok(formattedMessages);

	}

	@GetMapping("/sqs/{queue}/total-message")
    public ResponseEntity<Integer> getTotalMessageInQueue(@PathVariable String queue) {
		return ResponseEntity.ok(sqsService.getTotalMessageInQueues(queue));
    }


	@GetMapping("/sqs/queues")
    public ResponseEntity<List<String>> getSQSQueues() {
        return ResponseEntity.ok(sqsService.getListQueues());
    }

    @GetMapping("/sqs/receive-process")
    public ResponseEntity<String> receiveProcessAndHandleFailedMessages(@RequestParam String queue, @RequestParam String dlq) {
        sqsService.receiveProcessAndHandleFailedMessages(queue, dlq);
        return ResponseEntity.ok("Processing and handling failed messages completed.");
    }
    
    @GetMapping("/sqs/retry-move")
    public ResponseEntity<String> handleMessageRetryAndMoveToDLQ(@RequestParam String queue, @RequestParam String dlq) {
        return ResponseEntity.ok(sqsService.handleMessageRetryAndMoveToDLQ(queue, dlq));
    }

    @GetMapping("/sqs/process-mark")
    public ResponseEntity<String> processMessageForMarkAsRead(@RequestParam String queue) {
        return ResponseEntity.ok(sqsService.processMessageForMarkAsRead(queue));
    }
	/** --------------------------------------End SQS Services -------------------------------------- */
	
    
	/** --------------------------------------Start S3 Bucket Services -------------------------------------- */
	/**
	 * Create new bucket
	 * @param bucketName
	 * @return list of objects
	 * GET http://localhost:8080/api/aws/s3/bucket/gankan/object
	 */
	@PostMapping("/s3/create")
	public ResponseEntity<String> createBucket(@RequestBody String bucketName) {
        try {
            String result = s3Service.createBucket(bucketName);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating bucket: " + e.getMessage());
        }
    }

	@GetMapping("/s3/buckets")
	public String getBucket() {
		ListBucketsResponse listObjectsResponse = s3Service.listBuckets();
		return listObjectsResponse.buckets().toString();
	}

	@GetMapping("/s3/bucket/{bucketName}/object")
	public String getBucketObjects(@PathVariable String bucketName) {
		ListObjectsV2Response listObjectsResponse = s3Service.listObjects(bucketName);
		return listObjectsResponse.contents().toString();
	}

	/**
	 * Save and upload new object in bucket as file, images, etc.
	 * @param s3BucketName example: gankan
	 * @param keyName      example: kapil
	 * @param filePath  to file which will be upload   example: C:\\Kapil\\My Doc\\Education\\Python\\Details.txt
	 * POST http://localhost:8080/api/aws/s3/bucket/save-object
	 */
	@PostMapping("/s3/bucket/save-object")
	public ResponseEntity<String> setBucketObject(@RequestParam String s3BucketName, @RequestParam String keyName,
			@RequestParam String filePath) {
		s3Service.uploadFile(s3BucketName, keyName, filePath);
        return ResponseEntity.ok("File uploaded successfully to bucket '" + s3BucketName + "'");
	}

	/**
	 * @param s3BucketName example: gankan
	 * @param keyName      example: kapil
	 * @param filePathLocation where we download object from bucket     
	 * Example: filePath : "C:\\Kapil\\Manulink\\Details"
	 */
	@GetMapping("/s3/bucket/object")
	public ResponseEntity<String> getBucketObject(@RequestParam String s3BucketName, @RequestParam String keyName, @RequestParam String filePath) {
		s3Service.downloadFile(s3BucketName, keyName, filePath);
        return ResponseEntity.ok("File downloaded successfully.");

	}

	/**
	 * Delete object from bucket
	 * @param s3BucketName
	 * @param fileName(object)
	 * Delete : http://localhost:8080/api/aws/s3/bucket/delete-object?s3BucketName=gankan&fileName=kapil
	 */
	@DeleteMapping("/s3/bucket/delete-object")
	public ResponseEntity<String> deleteBucketObject(@RequestParam String s3BucketName, @RequestParam String fileName) {
		s3Service.deleteObject(s3BucketName, fileName);
        return ResponseEntity.ok("File '" + fileName + "' deleted from bucket '" + s3BucketName + "'");

	}
	
	/** --------------------------------------End S3 Bucket Services -------------------------------------- */
	
	/** --------------------------------------Start RDS Services -------------------------------------- */
	@GetMapping("/persons")
	public ResponseEntity<List<Person>> getPersons() {
        List<Person> persons = personService.getPersons();
        logger.info("Retrieved {} persons from database.", persons.size());
        return ResponseEntity.ok(persons);
	}
	
	@GetMapping("/person/{personId}")
	public ResponseEntity<Optional<Person>> getPersonById(@Valid @PathVariable int personId) {
		System.out.println("Get Person Details by person id:" + personId);
		Optional<Person> person = personService.getPersonById(personId);
        if (person.isPresent()) {
            return ResponseEntity.ok(person);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Optional.empty());
        }
	}

	@PostMapping("/person")
	public ResponseEntity<String> savePerson(@Valid @RequestBody Person person) {
		System.out.println("Saving new person information: " + person.toString());
		personService.savePerson(person);
        return ResponseEntity.status(HttpStatus.CREATED).body("Person saved successfully");

	}

	@DeleteMapping("/person/{personId}")
	public ResponseEntity<String> removePerson(@Valid @PathVariable int personId) {
		personService.removePerson(personId);
		System.out.println("Remove person information for person id:"+personId);
        return ResponseEntity.ok("Person with ID " + personId + " removed.");
	}

	/** --------------------------------------End RDS Services -------------------------------------- */

	// Global exception handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred: " + e.getMessage());
    }
}
