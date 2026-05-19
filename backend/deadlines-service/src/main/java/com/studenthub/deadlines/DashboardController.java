package com.studenthub.deadlines;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = { "http://localhost:5173", "http://127.0.0.1:5173" })
public class DashboardController {

	private final DeadlineRepository deadlineRepository;
	private final RestTemplate restTemplate;
	private final String notesServiceUrl;

	public DashboardController(
			DeadlineRepository deadlineRepository,
			RestTemplate restTemplate,
			@Value("${app.notes-service.url}") String notesServiceUrl) {
		this.deadlineRepository = deadlineRepository;
		this.restTemplate = restTemplate;
		this.notesServiceUrl = notesServiceUrl;
	}

	@GetMapping
	public DashboardResponse getDashboard(
			@AuthenticationPrincipal AuthenticatedUser user,
			@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
		List<Deadline> deadlines = deadlineRepository.findAllByUserIdOrderByDueDateAscCreatedAtDesc(user.userId());
		NotesServiceStatus notesServiceStatus = requestNotesCount(authorization);
		List<DeadlineSummary> nearestDeadlines = deadlines.stream()
				.limit(3)
				.map(DeadlineSummary::from)
				.toList();

		return new DashboardResponse(
				notesServiceStatus.notesCount(),
				(long) deadlines.size(),
				nearestDeadlines,
				notesServiceStatus.available());
	}

	private NotesServiceStatus requestNotesCount(String authorization) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, authorization);
		try {
			NotesCountResponse response = restTemplate.exchange(
					notesServiceUrl + "/api/notes/count",
					HttpMethod.GET,
					new HttpEntity<>(headers),
					NotesCountResponse.class)
					.getBody();

			Long count = response != null && response.count() != null ? response.count() : 0L;
			return new NotesServiceStatus(count, true);
		} catch (RestClientException exception) {
			return new NotesServiceStatus(null, false);
		}
	}

	private record NotesServiceStatus(Long notesCount, boolean available) {
	}
}
