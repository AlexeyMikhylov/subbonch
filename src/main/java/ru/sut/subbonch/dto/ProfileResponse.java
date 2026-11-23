package ru.sut.subbonch.dto;

import java.time.LocalDateTime;

public record ProfileResponse(
        Integer userId,
        String firstName,
        String lastName,
        String email,
        String about,
        String profilePictureUrl,
        LocalDateTime createdAt,
        long clubsCount,
        long commentsCount
) {
}
