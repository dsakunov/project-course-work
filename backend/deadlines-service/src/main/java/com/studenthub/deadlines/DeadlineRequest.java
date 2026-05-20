package com.studenthub.deadlines;

import java.time.LocalDate;

public record DeadlineRequest(Long userId, String title, LocalDate dueDate, Boolean completed, Boolean important) {
}
