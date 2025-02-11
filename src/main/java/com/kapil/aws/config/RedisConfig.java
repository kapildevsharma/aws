package com.kapil.aws.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;

@Configuration
@EnableCaching
public class RedisConfig {

	private final String redisHost;
	private final int redisPort;
	private final boolean useSsl;

	public RedisConfig(@Value("${spring.data.redis.host}") String redisHost,
			@Value("${spring.data.redis.port}") int redisPort,
			@Value("${spring.data.redis.ssl.enabled}") boolean useSsl) {
		this.redisHost = redisHost;
		this.redisPort = redisPort;
		this.useSsl = useSsl;
	}

	@Bean
	public LettuceConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);

		LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = LettuceClientConfiguration
				.builder().commandTimeout(Duration.ofSeconds(60));

		// Add SSL configuration only if enabled
		if (useSsl) {
			clientConfigBuilder.useSsl();
		}

		LettuceClientConfiguration clientConfig = clientConfigBuilder.build();

		return new LettuceConnectionFactory(redisConfig, clientConfig);
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
	    template.setConnectionFactory(redisConnectionFactory);

	    // Use String serializer for keys and hash keys
	    StringRedisSerializer stringSerializer = new StringRedisSerializer();
	    template.setKeySerializer(stringSerializer);
	    template.setHashKeySerializer(stringSerializer);

	    // Configure Jackson JSON serializer for values
	    ObjectMapper objectMapper = new ObjectMapper();
	    objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

	    Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

	    template.setValueSerializer(jsonSerializer);
	    template.setHashValueSerializer(jsonSerializer);

	    template.afterPropertiesSet();
	    return template;
	}

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig();
		return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(cacheConfig).build();
	}
}
