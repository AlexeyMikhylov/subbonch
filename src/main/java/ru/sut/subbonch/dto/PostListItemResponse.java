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
        String authorProfilePictureUrl,
        Integer clubId,
        String clubName,
        String clubPictureUrl,
        String clubAbout,
        String imageUrl,
        PollResponse poll,
        List<CommentResponse> comments
) {
    /**
     * Общее число комментариев поста, включая ответы на комментарии (любой глубины).
     * Считается по дереву, а не по {@code comments.size()}, иначе теряются вложенные ответы.
     */
    public int getCommentsCount() {
        return countComments(comments);
    }

    private static int countComments(List<CommentResponse> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (CommentResponse item : items) {
            total += 1 + countComments(item.replies());
        }
        return total;
    }
}
