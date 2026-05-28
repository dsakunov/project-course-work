package com.studenthub.deadlines;

import jakarta.servlet.http.HttpSession;

public final class SessionUserResolver {

	public static final String USER_ID = "USER_ID";
	public static final String USERNAME = "USERNAME";

	private SessionUserResolver() {
	}

	public static SessionUser get(HttpSession session) {
		if (session == null) {
			return null;
		}

		Object userId = session.getAttribute(USER_ID);
		Object username = session.getAttribute(USERNAME);

		if (userId instanceof Number id && username instanceof String name) {
			return new SessionUser(id.longValue(), name);
		}

		return null;
	}
}
