package com.studenthub.notes;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })
public class AuthController {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody AuthRequest request) {
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

		return ResponseEntity.ok(new AuthResponse(jwtService.createToken(savedUser)));
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody AuthRequest request) {
		String username = clean(request.username());
		String password = request.password() == null ? "" : request.password();

		return userRepository.findByUsername(username)
				.filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
				.<ResponseEntity<?>>map(user -> ResponseEntity.ok(new AuthResponse(jwtService.createToken(user))))
				.orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new AuthErrorResponse("Invalid username or password")));
	}

	private String clean(String value) {
		return value == null ? "" : value.trim();
	}
}
