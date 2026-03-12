package com.kapil.aws.service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListBucketsIterable;
import software.amazon.awssdk.services.s3.waiters.S3AsyncWaiter;


@Service
public class S3Service {

	private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

	private final S3Client s3Client;
    private final S3AsyncClient s3AsyncClient;

	public S3Service(S3Client s3Client, S3AsyncClient s3AsyncClient) {
		this.s3Client = s3Client;
        this.s3AsyncClient = s3AsyncClient;
	}

	/** Create a new S3 bucket
     * @param bucket the name of the S3 bucket to be created
     * @return a message indicating the result of the bucket creation operation
     * @throws RuntimeException if there is an error during bucket creation
     */
	public String createBucket(String bucket) {
		try {
			CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().bucket(bucket).build();
			s3Client.createBucket(createBucketRequest);
			logger.info("Successfully new bucket '{}' created .", bucket);
            return "Bucket created successfully: " + bucket;
		} catch (S3Exception e) {
            logger.error("Error, creating new bucket{}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Bucket creation failed: " + e.awsErrorDetails().errorMessage());
		}
	}

    /**
     * Creates an S3 bucket asynchronously.
     *
     * @param bucketName the name of the S3 bucket to create
     * @return a {@link CompletableFuture} that completes when the bucket is created and ready
     * @throws RuntimeException if there is a failure while creating the bucket
     */
    public CompletableFuture<Void> createBucketAsync(String bucketName) {
        CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                .bucket(bucketName).build();

        CompletableFuture<CreateBucketResponse> response = s3AsyncClient.createBucket(bucketRequest);
        return response.thenCompose(resp -> {
            S3AsyncWaiter s3Waiter = s3AsyncClient.waiter();
            HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
                    .bucket(bucketName).build();

            CompletableFuture<WaiterResponse<HeadBucketResponse>> waiterResponseFuture =
                    s3Waiter.waitUntilBucketExists(bucketRequestWait);
            return waiterResponseFuture.thenAccept(waiterResponse -> {
                waiterResponse.matched().response().ifPresent(headBucketResponse -> {
                    logger.info("{} is ready", bucketName);
                });
            });
        }).whenComplete((resp, ex) -> {
            if (ex != null) {
                throw new RuntimeException("Failed to create bucket", ex);
            }
        });
    }

    /** List all S3 buckets
     * @return a {@link ListBucketsResponse} containing the list of all S3 buckets
     */
	public ListBucketsResponse listBuckets() {
		// List all buckets
		ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
		ListBucketsResponse listBucketsResponse = s3Client.listBuckets(listBucketsRequest);

		listBucketsResponse.buckets().forEach(bucket -> {
			System.out.println("Bucket: " + bucket.name() + " (region :" + bucket.bucketRegion() + " )");
		});
        // Using paginator to list buckets
        ListBucketsIterable response = s3Client.listBucketsPaginator();
        response.buckets().forEach(bucket -> System.out.println("Bucket Name: " + bucket.name()));

		return listBucketsResponse;

	}

	/** List objects in an S3 bucket
     * @param s3BucketName the name of the S3 bucket to list objects from
     * @return a {@link ListObjectsV2Response} containing the list of objects in the specified S3 bucket
     */
	public ListObjectsV2Response listObjects(String s3BucketName) {
		ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder().bucket(s3BucketName).build();
		ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);

		listObjectsResponse.contents().forEach(s3Object -> {
			System.out.println("Object: " + s3Object.key() + " (" + s3Object.size() + " bytes)");
		});
		return listObjectsResponse;
	}

	/** Upload a file to S3
     * @param s3BucketName the name of the S3 bucket to upload the file to
     * @param key the key (object name) to use for the uploaded file
     * @param objectPath the local file path of the file to be uploaded
     */
	public void uploadFile(String s3BucketName, String key, String objectPath) {
		PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(s3BucketName).key(key).build();

		try {
			s3Client.putObject(putObjectRequest, RequestBody.fromFile(Paths.get(objectPath)));
			logger.info("Successfully, file '{}' is uploaded into bucket '{}' with keyName '{}'", objectPath,
					s3BucketName, key);

		} catch (S3Exception e) {
			System.err.println("Error uploading file: " + e.awsErrorDetails().errorMessage());
		}
	}

    /**
     * Uploads a local file to an AWS S3 bucket asynchronously.
     *
     * @param bucketName the name of the S3 bucket to upload the file to
     * @param key        the key (object name) to use for the uploaded file
     * @param objectPath the local file path of the file to be uploaded
     * @return a {@link CompletableFuture} that completes with the {@link PutObjectResponse} when the upload is successful, or throws a {@link RuntimeException} if the upload fails
     */
    public CompletableFuture<PutObjectResponse> uploadLocalFileAsync(String bucketName, String key, String objectPath) {
        PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(bucketName).key(key).build();

        CompletableFuture<PutObjectResponse> response = s3AsyncClient.putObject(objectRequest, AsyncRequestBody.fromFile(Paths.get(objectPath)));
        return response.whenComplete((resp, ex) -> {
            if (ex != null) {
                throw new RuntimeException("Failed to upload file", ex);
            }
        });
    }


    /** Download a file from S3
     * @param s3BucketName the name of the S3 bucket to download the file from
     * @param keyName the key (object name) of the file to be downloaded
     * @param downloadPath the local file path where the downloaded file will be saved
     */
	public void downloadFile(String s3BucketName, String keyName, String downloadPath) {
		GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(s3BucketName).key(keyName).build();
		try {
			logger.info("Downloading file '{}' from '{}' bucket into location '{}'", keyName, s3BucketName, downloadPath);
			s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(Paths.get(downloadPath)));
			logger.info("Successfully, file '{}' is download from '{}' bucket into specified location '{}'", keyName,
					s3BucketName, downloadPath);
		} catch (S3Exception e) {
            logger.error("Error downloading file: {}", e.awsErrorDetails().errorMessage());
		}
	}

    /**
     * Asynchronously retrieves the bytes of an object from an Amazon S3 bucket and writes them to a local file.
     *
     * @param bucketName the name of the S3 bucket containing the object
     * @param keyName    the key (or name) of the S3 object to retrieve
     * @param path       the local file path where the object's bytes will be written
     * @return a {@link CompletableFuture} that completes when the object bytes have been written to the local file
     */
    public CompletableFuture<Void> getObjectBytesAsync(String bucketName, String keyName, String path) {
        GetObjectRequest objectRequest = GetObjectRequest.builder().key(keyName).bucket(bucketName).build();

        CompletableFuture<ResponseBytes<GetObjectResponse>> response = s3AsyncClient.getObject(objectRequest, AsyncResponseTransformer.toBytes());
        return response.thenAccept(objectBytes -> {
            try {
                byte[] data = objectBytes.asByteArray();
                Path filePath = Paths.get(path);
                Files.write(filePath, data);
                logger.info("Successfully obtained bytes from an S3 object");
            } catch (IOException ex) {
                throw new RuntimeException("Failed to write data to file", ex);
            }
        }).whenComplete((resp, ex) -> {
            if (ex != null) {
                throw new RuntimeException("Failed to get object bytes from S3", ex);
            }
        });
    }

    /** Deletes an object from an S3 bucket.
     *
     * @param s3BucketName the name of the S3 bucket containing the object to be deleted
     * @param keyName the key (or name) of the S3 object to be deleted
     */
	public void deleteObject(String s3BucketName, String keyName) {
		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(s3BucketName).key(keyName)
				.build();
		try {
			System.out.println("deleteObjectRequest: " + deleteObjectRequest.toString());
			s3Client.deleteObject(deleteObjectRequest);
			System.out.println("File deleted successfully: " + keyName);
			logger.info("Successfully, file '{}' is deleted from '{}' bucket.", keyName, s3BucketName);
		} catch (S3Exception e) {
            logger.error("Error deleting file: {}", e.awsErrorDetails().errorMessage());
		}
	}

    /**
     * Deletes an object from an S3 bucket asynchronously.
     *
     * @param bucketName the name of the S3 bucket
     * @param key        the key (file name) of the object to be deleted
     * @return a {@link CompletableFuture} that completes when the object has been deleted
     */
    public CompletableFuture<Void> deleteObjectFromBucketAsync(String bucketName, String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(key).build();

        CompletableFuture<DeleteObjectResponse> response = s3AsyncClient.deleteObject(deleteObjectRequest);
        response.whenComplete((deleteRes, ex) -> {
            if (deleteRes != null) {
                logger.info("{} was deleted", key);
            } else {
                throw new RuntimeException("An S3 exception occurred during delete", ex);
            }
        });
        return response.thenApply(r -> null);
    }

    /**
     * Performs a multipart copy of an object from one S3 bucket to another asynchronously.
     *
     * @param toBucket   the name of the destination S3 bucket
     * @param bucketName the name of the source S3 bucket
     * @param key        the key (file name) of the object to be copied
     * @return a {@link CompletableFuture} that completes with a message indicating the result of the multipart copy operation
     */
    public CompletableFuture<String> performMultiCopy(String toBucket, String bucketName, String key) {
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(toBucket).key(key).build();

        s3AsyncClient.createMultipartUpload(createMultipartUploadRequest)
                .thenApply(createMultipartUploadResponse -> {
                    String uploadId = createMultipartUploadResponse.uploadId();
                    System.out.println("Upload ID: " + uploadId);

                    UploadPartCopyRequest uploadPartCopyRequest = UploadPartCopyRequest.builder()
                            .sourceBucket(bucketName)
                            .destinationBucket(toBucket)
                            .sourceKey(key)
                            .destinationKey(key)
                            .uploadId(uploadId)  // Use the valid uploadId.
                            .partNumber(1)  // Ensure the part number is correct.
                            .copySourceRange("bytes=0-1023")  // Adjust range as needed
                            .build();

                    return s3AsyncClient.uploadPartCopy(uploadPartCopyRequest);
                })
                .thenCompose(uploadPartCopyFuture -> uploadPartCopyFuture)
                .whenComplete((uploadPartCopyResponse, exception) -> {
                    if (exception != null) {
                        // Handle any exceptions.
                        logger.error("Error during upload part copy: {}", exception.getMessage());
                    } else {
                        // Successfully completed the upload part copy.
                        System.out.println("Upload Part Copy completed successfully. ETag: " + uploadPartCopyResponse.copyPartResult().eTag());
                    }
                });
        return null;
    }

    /**
     * Performs a multipart upload to Amazon S3 using the provided S3 client.
     *
     * @param filePath the path to the file to be uploaded
     */
    public void multipartUploadWithS3Client(String filePath, String bucketName, String key) {

        // Initiate the multipart upload.
        CreateMultipartUploadResponse createMultipartUploadResponse = s3Client.createMultipartUpload(b -> b
                .bucket(bucketName).key(key));
        String uploadId = createMultipartUploadResponse.uploadId();

        // Upload the parts of the file.
        int partNumber = 1;
        List<CompletedPart> completedParts = new ArrayList<>();
        ByteBuffer bb = ByteBuffer.allocate(1024 * 1024 * 5); // 5 MB byte buffer

        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            long fileSize = file.length();
            long position = 0;
            while (position < fileSize) {
                file.seek(position);
                long read = file.getChannel().read(bb);

                bb.flip(); // Swap position and limit before reading from the buffer.
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucketName).key(key).uploadId(uploadId).partNumber(partNumber)
                        .build();

                UploadPartResponse partResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromByteBuffer(bb));

                CompletedPart part = CompletedPart.builder().partNumber(partNumber).eTag(partResponse.eTag())
                        .build();
                completedParts.add(part);

                bb.clear();
                position += read;
                partNumber++;
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        // Complete the multipart upload.
        s3Client.completeMultipartUpload(b -> b
                .bucket(bucketName).key(key).uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build()));
    }

}
