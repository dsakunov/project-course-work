package com.studenthub.deadlines;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/dashboard")
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
	public DashboardResponse getDashboard(HttpServletRequest request, HttpSession session) {
		SessionUser user = SessionUserResolver.get(session);
		List<Deadline> deadlines = deadlineRepository.findAllByUserIdOrderByDueDateAscCreatedAtDesc(user.userId());
		NotesServiceStatus notesServiceStatus = requestNotesCount(request);
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

	private NotesServiceStatus requestNotesCount(HttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();
		String sessionCookie = findSessionCookie(request);
		if (sessionCookie != null) {
			headers.add(HttpHeaders.COOKIE, sessionCookie);
		}

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

	private String findSessionCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		return Arrays.stream(cookies)
				.filter(cookie -> "STUDENT_HUB_SESSION".equals(cookie.getName()))
				.findFirst()
				.map(cookie -> cookie.getName() + "=" + cookie.getValue())
				.orElse(null);
	}

	private record NotesServiceStatus(Long notesCount, boolean available) {
	}
}
