package com.studenthub.notes;

import java.util.List;

import org.springframework.http.ResponseEntity;
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
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })
public class NoteController {

	private final NoteRepository noteRepository;

	public NoteController(NoteRepository noteRepository) {
		this.noteRepository = noteRepository;
	}

	@GetMapping
	public List<Note> getNotes() {
		return noteRepository.findAllByOrderByCreatedAtDesc();
	}

	@PostMapping
	public ResponseEntity<Note> createNote(@RequestBody NoteRequest request) {
		Note note = new Note();
		applyRequest(note, request);
		return ResponseEntity.ok(noteRepository.save(note));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Note> updateNote(@PathVariable Long id, @RequestBody NoteRequest request) {
		return noteRepository.findById(id)
				.map(note -> {
					applyRequest(note, request);
					return ResponseEntity.ok(noteRepository.save(note));
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
		if (!noteRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}

		noteRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	private void applyRequest(Note note, NoteRequest request) {
		note.setUserId(request.userId() != null ? request.userId() : 1L);
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
