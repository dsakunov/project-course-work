package com.studenthub.notes;

import java.util.List;
import java.util.Map;

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
@RequestMapping("/api/notes")
public class NoteController {

	private final NoteRepository noteRepository;

	public NoteController(NoteRepository noteRepository) {
		this.noteRepository = noteRepository;
	}

	@GetMapping
	public List<Note> getNotes(HttpSession session) {
		return noteRepository.findAllByUserIdOrderByCreatedAtDesc(SessionUserResolver.get(session).userId());
	}

	@GetMapping("/count")
	public Map<String, Long> getNotesCount(HttpSession session) {
		return Map.of("count", noteRepository.countByUserId(SessionUserResolver.get(session).userId()));
	}

	@PostMapping
	public ResponseEntity<Note> createNote(
			HttpSession session,
			@RequestBody NoteRequest request) {
		Note note = new Note();
		applyRequest(note, request, SessionUserResolver.get(session).userId());
		return ResponseEntity.ok(noteRepository.save(note));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Note> updateNote(
			HttpSession session,
			@PathVariable Long id,
			@RequestBody NoteRequest request) {
		return noteRepository.findByIdAndUserId(id, SessionUserResolver.get(session).userId())
				.map(note -> {
					applyRequest(note, request, SessionUserResolver.get(session).userId());
					return ResponseEntity.ok(noteRepository.save(note));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteNote(HttpSession session, @PathVariable Long id) {
		return noteRepository.findByIdAndUserId(id, SessionUserResolver.get(session).userId())
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
