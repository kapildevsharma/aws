package com.kapil.aws.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@Configuration
public class AwsSecretsManagerConfig {

	@Value("${aws.region}")
	private String region;

	@Value("${aws.accessKeyId}")
	private String accessKeyId;

	@Value("${aws.secretAccessKey}")
	private String secretAccessKey;

	@Value("${aws.secretName}")
	private String secretName;

	@Bean
	SecretsManagerClient secretsManagerClient() {
		AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
		return SecretsManagerClient.builder().credentialsProvider(StaticCredentialsProvider.create(awsCreds))
				.region(Region.of(region)).build();
	}

	@Bean
	String rdsSecretString(SecretsManagerClient secretsManagerClient) {
		GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder().secretId(secretName).build();

		GetSecretValueResponse secretValueResponse = secretsManagerClient.getSecretValue(getSecretValueRequest);
		return secretValueResponse.secretString(); // Parse the secret value
	}
}
