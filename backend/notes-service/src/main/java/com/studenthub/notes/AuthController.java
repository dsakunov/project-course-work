package com.studenthub.notes;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/session")
	public ResponseEntity<?> session(HttpSession session) {
		SessionUser user = SessionUserResolver.get(session);
		if (user == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthErrorResponse("Not authenticated"));
		}

		return ResponseEntity.ok(new AuthResponse(user.userId(), user.username()));
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody AuthRequest request, HttpSession session) {
		String username = clean(request.username());
		String password = request.password() == null ? "" : request.password();

		if (username.isEmpty() || password.length() < 4) {
			return ResponseEntity.badRequest().body(new AuthErrorResponse("Username and password are required"));
		}

		if (userRepository.existsByUsername(username)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthErrorResponse("Username already exists"));
		}

		AppUser user = new AppUser();
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(password));
		AppUser savedUser = userRepository.save(user);
		SessionUserResolver.save(session, savedUser);

		return ResponseEntity.ok(new AuthResponse(savedUser.getId(), savedUser.getUsername()));
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpSession session) {
		String username = clean(request.username());
		String password = request.password() == null ? "" : request.password();

		return userRepository.findByUsername(username)
				.filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
				.<ResponseEntity<?>>map(user -> {
					SessionUserResolver.save(session, user);
					return ResponseEntity.ok(new AuthResponse(user.getId(), user.getUsername()));
				})
				.orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new AuthErrorResponse("Invalid username or password")));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpSession session) {
		session.invalidate();
		return ResponseEntity.noContent().build();
	}

	private String clean(String value) {
		return value == null ? "" : value.trim();
	}
}