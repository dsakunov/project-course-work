package com.studenthub.deadlines;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadlineRepository extends JpaRepository<Deadline, Long> {

	List<Deadline> findAllByOrderByDueDateAscCreatedAtDesc();

	List<Deadline> findAllByUserIdOrderByDueDateAscCreatedAtDesc(Long userId);

	java.util.Optional<Deadline> findByIdAndUserId(Long id, Long userId);

	long countByUserId(Long userId);
}
