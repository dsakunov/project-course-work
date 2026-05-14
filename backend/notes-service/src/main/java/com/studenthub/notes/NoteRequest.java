package com.studenthub.notes;

public record NoteRequest(Long userId, String title, String content) {
}
