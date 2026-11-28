package ru.sut.subbonch.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Integer commentId,
        Integer authorId,
        String authorFirstName,
        String authorLastName,
        String authorProfilePictureUrl,
        String authorRoleInClub,
        String comment,
        LocalDateTime createdAt,
        Integer parentId,
        List<CommentResponse> replies
) {
}
