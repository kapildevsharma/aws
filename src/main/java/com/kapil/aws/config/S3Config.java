package com.kapil.aws.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {
	
	@Value("${aws.accessKeyId}")
	private String awsAccessKeyId;

	@Value("${aws.secretAccessKey}")
	private String awsSecretAccessKey;

	@Value("${aws.region}")
	private String region;

	@Bean
	S3Client s3Client() {
		S3Client s3Client = S3Client.builder().region(Region.of(region)) // Replace with your AWS region
				.credentialsProvider(StaticCredentialsProvider
						.create(AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey)))
				.build();
		return s3Client;
	}
}
