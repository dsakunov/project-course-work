package com.studenthub.notes;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {

	List<Note> findAllByOrderByCreatedAtDesc();

	List<Note> findAllByUserIdOrderByCreatedAtDesc(Long userId);

	java.util.Optional<Note> findByIdAndUserId(Long id, Long userId);

	long countByUserId(Long userId);
}
