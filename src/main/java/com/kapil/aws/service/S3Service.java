package com.kapil.aws.service;

import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class S3Service {

	private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

	private final S3Client s3Client;

	public S3Service(S3Client s3Client) {
		this.s3Client = s3Client;
	}

	// Create an S3 bucket
	public String createBucket(String bucket) {
		try {
			CreateBucketRequest createBucketRequest = CreateBucketRequest.builder().bucket(bucket).build();
			s3Client.createBucket(createBucketRequest);
			logger.info("Successfully new bukcet '{}' created .", bucket);
            return "Bucket created successfully: " + bucket;
		} catch (S3Exception e) {
			logger.error("Error, creating new bucket" + e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Bucket creation failed: " + e.awsErrorDetails().errorMessage());
		}
	}

	// List of bucket in the S3 bucket
	public ListBucketsResponse listBuckets() {
		// List all buckets
		ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
		ListBucketsResponse listBucketsResponse = s3Client.listBuckets(listBucketsRequest);

		listBucketsResponse.buckets().forEach(s3Object -> {
			System.out.println("Bucket: " + s3Object.name() + " (region :" + s3Object.bucketRegion() + " )");
		});
		return listBucketsResponse;

	}

	// List of objects in particular bucket in the S3 bucket
	public ListObjectsV2Response listObjects(String s3BucketName) {
		ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder().bucket(s3BucketName).build();
		ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);

		listObjectsResponse.contents().forEach(s3Object -> {
			System.out.println("Object: " + s3Object.key() + " (" + s3Object.size() + " bytes)");
		});
		return listObjectsResponse;
	}

	// Upload a file to S3
	public void uploadFile(String s3BucketName, String keyName, String filePath) {
		PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(s3BucketName).key(keyName).build();

		try {
			s3Client.putObject(putObjectRequest, RequestBody.fromFile(Paths.get(filePath)));
			logger.info("Successfully, file '{}' is uploaded into bucket '{}' with keyName '{}'", filePath,
					s3BucketName, keyName);

		} catch (S3Exception e) {
			System.err.println("Error uploading file: " + e.awsErrorDetails().errorMessage());
		}
	}

	// Download a file from S3
	public void downloadFile(String s3BucketName, String keyName, String downloadPath) {
		GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(s3BucketName).key(keyName).build();
		try {
			logger.info("Downloading file '{}' from '{}' bucket into location '{}'", keyName, s3BucketName, downloadPath);
			s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(Paths.get(downloadPath)));
			logger.info("Successfully, file '{}' is download from '{}' bucket into specified location '{}'", keyName,
					s3BucketName, downloadPath);
		} catch (S3Exception e) {
			logger.error("Error downloading file: " + e.awsErrorDetails().errorMessage());
		}
	}

	// Delete an object from the bucket
	public void deleteObject(String s3BucketName, String keyName) {
		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(s3BucketName).key(keyName)
				.build();
		try {
			System.out.println("deleteObjectRequest: " + deleteObjectRequest.toString());
			s3Client.deleteObject(deleteObjectRequest);
			System.out.println("File deleted successfully: " + keyName);
			logger.info("Successfully, file '{}' is deleted from '{}' bucket.", keyName, s3BucketName);
		} catch (S3Exception e) {
			logger.error("Error deleting file: " + e.awsErrorDetails().errorMessage());
		}
	}

}
