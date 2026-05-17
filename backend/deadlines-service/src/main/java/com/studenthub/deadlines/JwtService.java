package com.studenthub.deadlines;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JwtService {

	private final String secret;
	private final ObjectMapper objectMapper;

	public JwtService(
			@Value("${app.jwt.secret}") String secret,
			ObjectMapper objectMapper) {
		this.secret = secret;
		this.objectMapper = objectMapper;
	}

	public AuthenticatedUser parseToken(String token) {
		try {
			String[] parts = token.split("\\.");
			if (parts.length != 3) {
				return null;
			}

			String unsignedToken = parts[0] + "." + parts[1];
			if (!sign(unsignedToken).equals(parts[2])) {
				return null;
			}

			Map<String, Object> payload = objectMapper.readValue(
					Base64.getUrlDecoder().decode(parts[1]),
					new TypeReference<Map<String, Object>>() {
					});
			long exp = ((Number) payload.get("exp")).longValue();
			if (Instant.now().getEpochSecond() >= exp) {
				return null;
			}

			Long userId = ((Number) payload.get("userId")).longValue();
			String username = (String) payload.get("username");
			return new AuthenticatedUser(userId, username);
		} catch (Exception exception) {
			return null;
		}
	}

	private String sign(String value) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
	}
}
