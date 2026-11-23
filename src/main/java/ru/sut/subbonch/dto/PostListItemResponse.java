package ru.sut.subbonch.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostListItemResponse(
        Integer postId,
        String title,
        String body,
        LocalDateTime createdAt,
        boolean isEdited,
        Integer authorId,
        String authorFirstName,
        String authorLastName,
        String imageUrl,
        PollResponse poll,
        List<CommentResponse> comments
) {
}
