package com.kapil.aws.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.JsonObject;
import com.kapil.aws.service.AwsSecretsManagerService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DataSourceConfig {

	private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

	
	private final AwsSecretsManagerService awsSecretsManagerService;

    public DataSourceConfig(AwsSecretsManagerService awsSecretsManagerService) {
        this.awsSecretsManagerService = awsSecretsManagerService;
    }

    @Bean
    public HikariDataSource dataSource() {
        String secretString = awsSecretsManagerService.getSecret();

        // Parse the secret JSON string
        JsonObject secretJson = awsSecretsManagerService.parseSecretJson(secretString);
        logger.debug("SecretsManager, Get SecretJson for db details: "+ secretJson);
		
        // Extract the RDS credentials from the JSON object
        String username = secretJson.has("username") ?secretJson.get("username").getAsString(): null;
        String password = secretJson.has("password") ?secretJson.get("password").getAsString(): null;
        String dbName = secretJson.has("dbname") ? secretJson.get("dbname").getAsString() : null;
        String host = secretJson.has("host") ? secretJson.get("host").getAsString() : null;
        String port = secretJson.has("port") ? secretJson.get("port").getAsString() : null;

        // Validate that none of the required fields are null
        if (username == null || password == null || dbName == null || host == null || port == null) {
            throw new RuntimeException("One or more required fields are missing in the secret JSON: username, password, dbname, host, port");
        }
        		
        // Configure HikariCP
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        // Optionally configure other Hikari settings
        hikariConfig.setMaximumPoolSize(10);
        
        return new HikariDataSource(hikariConfig);
    }
}
