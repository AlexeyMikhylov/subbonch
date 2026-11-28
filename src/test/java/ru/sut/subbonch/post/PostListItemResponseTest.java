package ru.sut.subbonch.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.sut.subbonch.dto.CommentResponse;
import ru.sut.subbonch.dto.PostListItemResponse;

class PostListItemResponseTest {

    @Test
    void commentsCount_emptyOrNull_isZero() {
        assertThat(postWith(null).getCommentsCount()).isZero();
        assertThat(postWith(List.of()).getCommentsCount()).isZero();
    }

    @Test
    void commentsCount_includesTopLevelOnly_whenNoReplies() {
        PostListItemResponse post = postWith(List.of(comment(1, List.of()), comment(2, List.of())));

        assertThat(post.getCommentsCount()).isEqualTo(2);
    }

    @Test
    void commentsCount_includesRepliesOfAnyDepth() {
        CommentResponse deepReply = comment(4, List.of(comment(5, List.of())));
        CommentResponse reply = comment(2, List.of(comment(3, List.of()), deepReply));
        CommentResponse root = comment(1, List.of(reply));

        PostListItemResponse post = postWith(List.of(root, comment(6, List.of())));

        assertThat(post.getCommentsCount()).isEqualTo(6);
    }

    private static PostListItemResponse postWith(List<CommentResponse> comments) {
        return new PostListItemResponse(
                1, "Заголовок", "Текст", null, false,
                1, "Иван", "Иванов", null,
                1, "Клуб", null, "О клубе", null, null, comments);
    }

    private static CommentResponse comment(Integer id, List<CommentResponse> replies) {
        return new CommentResponse(id, 1, "Иван", "Иванов", null, "MEMBER",
                "текст", null, null, replies);
    }
}
