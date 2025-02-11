package com.kapil.aws.service;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

@Service
public class AwsSecretsManagerService {

    private final SecretsManagerClient secretsManagerClient;

	@Value("${aws.secretName}")
    private String secretName;
    
    public AwsSecretsManagerService(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;
    }

    public String getSecret() {
    	try {
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse secretValueResponse = secretsManagerClient.getSecretValue(getSecretValueRequest);
            return secretValueResponse.secretString();
        } catch (SecretsManagerException e) {
            throw new RuntimeException("Error retrieving secret from Secrets Manager", e);
        }
    }
    
    public JsonObject parseSecretJson(String secretString) {
        JsonElement jsonElement = JsonParser.parseString(secretString); // Use parseString to parse JSON
        return jsonElement.getAsJsonObject(); // Get as JsonObject
    }
}
