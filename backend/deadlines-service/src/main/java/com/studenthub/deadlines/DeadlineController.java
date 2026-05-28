package com.studenthub.deadlines;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpSession;
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
public class DeadlineController {

	private final DeadlineRepository deadlineRepository;

	public DeadlineController(DeadlineRepository deadlineRepository) {
		this.deadlineRepository = deadlineRepository;
	}

	@GetMapping
	public List<Deadline> getDeadlines(HttpSession session) {
		return deadlineRepository.findAllByUserIdOrderByDueDateAscCreatedAtDesc(SessionUserResolver.get(session).userId());
	}

	@PostMapping
	public ResponseEntity<Deadline> createDeadline(
			HttpSession session,
			@RequestBody DeadlineRequest request) {
		Deadline deadline = new Deadline();
		applyRequest(deadline, request, SessionUserResolver.get(session).userId());
		return ResponseEntity.ok(deadlineRepository.save(deadline));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Deadline> updateDeadline(
			HttpSession session,
			@PathVariable Long id,
			@RequestBody DeadlineRequest request) {
		return deadlineRepository.findByIdAndUserId(id, SessionUserResolver.get(session).userId())
				.map(deadline -> {
					applyRequest(deadline, request, SessionUserResolver.get(session).userId());
					return ResponseEntity.ok(deadlineRepository.save(deadline));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteDeadline(HttpSession session, @PathVariable Long id) {
		return deadlineRepository.findByIdAndUserId(id, SessionUserResolver.get(session).userId())
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
		deadline.setCompleted(Boolean.TRUE.equals(request.completed()));
		deadline.setImportant(Boolean.TRUE.equals(request.important()));
	}

	private String clean(String value, String fallback) {
		if (value == null || value.trim().isEmpty()) {
			return fallback;
		}

		return value.trim();
	}
}
