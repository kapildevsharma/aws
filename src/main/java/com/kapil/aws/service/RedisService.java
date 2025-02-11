package com.kapil.aws.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RedisService {

	private static final Logger logger = LoggerFactory.getLogger(RedisService.class);
	private final RedisTemplate<String, Object> redisTemplate;
	private final ObjectMapper objectMapper;

	public RedisService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	// Set a value in Redis
	public void setValue(String key, Object value) {
		try {
			// Serialize the object to JSON string if needed
			String valueAsString = objectMapper.writeValueAsString(value);
			redisTemplate.opsForValue().set(key, valueAsString);
			logger.info("Successfully set key '{}' with value '{}'", key, valueAsString);
		} catch (Exception e) {
			logger.error("Error setting value for key '{}' in Redis", key, e);
		}
	}

	// Get a value from Redis
	public String getValue(String key) {
		try {
			String value = (String) redisTemplate.opsForValue().get(key);
			if (value != null) {
				logger.info("Successfully retrieved value for key '{}'", key);
			} else {
				logger.warn("No value found for key '{}'", key);
			}
			return value;
		} catch (Exception e) {
			logger.error("Error retrieving value for key '{}' from Redis", key, e);
			return null;
		}
	}

	// Delete a key from Redis
	public void deleteValue(String key) {
		try {
			redisTemplate.opsForValue().getAndDelete(key);
			logger.info("Successfully deleted key '{}'", key);
		} catch (Exception e) {
			logger.error("Error deleting key '{}' from Redis", key, e);
		}
	}

}
