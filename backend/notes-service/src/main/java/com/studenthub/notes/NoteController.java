package com.studenthub.notes;

import java.util.List;
import java.util.Map;

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
@RequestMapping("/api/notes")
@CrossOrigin(origins = {
		"http://localhost:5173",
		"http://127.0.0.1:5173",
		"http://localhost:3000",
		"http://127.0.0.1:3000"
})
public class NoteController {

	private final NoteRepository noteRepository;

	public NoteController(NoteRepository noteRepository) {
		this.noteRepository = noteRepository;
	}

	@GetMapping
	public List<Note> getNotes(@AuthenticationPrincipal AuthenticatedUser user) {
		return noteRepository.findAllByUserIdOrderByCreatedAtDesc(user.userId());
	}

	@GetMapping("/count")
	public Map<String, Long> getNotesCount(@AuthenticationPrincipal AuthenticatedUser user) {
		return Map.of("count", noteRepository.countByUserId(user.userId()));
	}

	@PostMapping
	public ResponseEntity<Note> createNote(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestBody NoteRequest request) {
		Note note = new Note();
		applyRequest(note, request, user.userId());
		return ResponseEntity.ok(noteRepository.save(note));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Note> updateNote(
			@AuthenticationPrincipal AuthenticatedUser user,
			@PathVariable Long id,
			@RequestBody NoteRequest request) {
		return noteRepository.findByIdAndUserId(id, user.userId())
				.map(note -> {
					applyRequest(note, request, user.userId());
					return ResponseEntity.ok(noteRepository.save(note));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteNote(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
		return noteRepository.findByIdAndUserId(id, user.userId())
				.map(note -> {
					noteRepository.delete(note);
					return ResponseEntity.noContent().<Void>build();
				})
				.orElse(ResponseEntity.notFound().build());
	}

	private void applyRequest(Note note, NoteRequest request, Long userId) {
		note.setUserId(userId);
		note.setTitle(clean(request.title(), "Без названия"));
		note.setContent(clean(request.content(), ""));
	}

	private String clean(String value, String fallback) {
		if (value == null || value.trim().isEmpty()) {
			return fallback;
		}

		return value.trim();
	}
}
