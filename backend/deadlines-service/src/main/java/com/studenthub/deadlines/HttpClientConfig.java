package com.studenthub.deadlines;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

	@Bean
	RestTemplate restTemplate(
			RestTemplateBuilder builder,
			@Value("${app.http.timeout-millis}") long timeoutMillis) {
		Duration timeout = Duration.ofMillis(timeoutMillis);
		return builder
				.connectTimeout(timeout)
				.readTimeout(timeout)
				.build();
	}
}
