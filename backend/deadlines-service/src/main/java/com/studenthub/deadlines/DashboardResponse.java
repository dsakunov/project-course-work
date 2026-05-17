package com.studenthub.deadlines;

import java.util.List;

public record DashboardResponse(
		Long notesCount,
		Long deadlinesCount,
		List<DeadlineSummary> nearestDeadlines,
		boolean notesServiceAvailable) {
}
