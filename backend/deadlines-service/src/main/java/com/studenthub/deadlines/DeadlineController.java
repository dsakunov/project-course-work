package com.studenthub.deadlines;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deadlines")
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })
public class DeadlineController {

	private final DeadlineRepository deadlineRepository;

	public DeadlineController(DeadlineRepository deadlineRepository) {
		this.deadlineRepository = deadlineRepository;
	}

	@GetMapping
	public List<Deadline> getDeadlines(@AuthenticationPrincipal AuthenticatedUser user) {
		return deadlineRepository.findAllByUserIdOrderByDueDateAscCreatedAtDesc(user.userId());
	}

	@PostMapping
	public ResponseEntity<Deadline> createDeadline(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestBody DeadlineRequest request) {
		Deadline deadline = new Deadline();
		applyRequest(deadline, request, user.userId());
		return ResponseEntity.ok(deadlineRepository.save(deadline));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Deadline> updateDeadline(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long id,
			@RequestBody DeadlineRequest request) {
		return deadlineRepository.findByIdAndUserId(id, user.userId())
				.map(deadline -> {
					applyRequest(deadline, request, user.userId());
					return ResponseEntity.ok(deadlineRepository.save(deadline));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDeadline(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
		return deadlineRepository.findByIdAndUserId(id, user.userId())
				.map(deadline -> {
					deadlineRepository.delete(deadline);
					return ResponseEntity.noContent().<Void>build();
				})
				.orElse(ResponseEntity.notFound().build());
	}

	private void applyRequest(Deadline deadline, DeadlineRequest request, Long userId) {
		deadline.setUserId(userId);
		deadline.setTitle(clean(request.title(), "Без названия"));
		deadline.setDueDate(request.dueDate() != null ? request.dueDate() : LocalDate.now());
	}

	private String clean(String value, String fallback) {
		if (value == null || value.trim().isEmpty()) {
			return fallback;
		}

		return value.trim();
	}
}
