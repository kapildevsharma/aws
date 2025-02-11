package com.kapil.aws.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kapil.aws.service.RedisService;

@RestController
public class RedisController {

	private final RedisService redisService;

	// Constructor Injection
	public RedisController(RedisService redisService) {
		this.redisService = redisService;
	}

	// Set key-value in Redis
	@PutMapping("/set")
	public ResponseEntity<String> setKey(@RequestParam String key, @RequestParam String value) {
		try {
			redisService.setValue(key, value);
			return ResponseEntity.ok("Value set in Redis successfully!");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Failed to set value in Redis: " + e.getMessage());
		}
	}

	// Get value from Redis
	@GetMapping("/get")
	public ResponseEntity<String> getKey(@RequestParam String key) {
		try {
			String value = redisService.getValue(key);
			if (value != null) {
				return ResponseEntity.ok(value);
			} else {
				return ResponseEntity.status(404).body("Key not found in Redis!");
			}
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Failed to get value from Redis: " + e.getMessage());
		}
	}

	// Delete key from Redis
	@DeleteMapping("/delete")
	public ResponseEntity<String> deleteKey(@RequestParam String key) {
		try {
			redisService.deleteValue(key);
			return ResponseEntity.ok("Value deleted from Redis successfully!");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Failed to delete value from Redis: " + e.getMessage());
		}
	}

}
