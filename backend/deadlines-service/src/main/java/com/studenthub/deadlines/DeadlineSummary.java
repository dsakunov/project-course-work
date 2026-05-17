package com.studenthub.deadlines;

import java.time.LocalDate;

public record DeadlineSummary(Long id, String title, LocalDate dueDate) {

	static DeadlineSummary from(Deadline deadline) {
		return new DeadlineSummary(deadline.getId(), deadline.getTitle(), deadline.getDueDate());
	}
}
