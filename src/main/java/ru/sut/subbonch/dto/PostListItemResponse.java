package ru.sut.subbonch.dto;

import java.time.LocalDateTime;

public record PostListItemResponse(
        Integer postId,
        String title,
        String body,
        LocalDateTime createdAt,
        boolean isEdited,
        Integer authorId,
        String authorFirstName,
        String authorLastName,
        String imageUrl
) {
}
