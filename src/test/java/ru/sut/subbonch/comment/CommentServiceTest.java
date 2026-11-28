package ru.sut.subbonch.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.dto.CommentResponse;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.Comment;
import ru.sut.subbonch.entity.Post;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.CommentRepository;
import ru.sut.subbonch.repository.PostRepository;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.service.CommentService;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @InjectMocks
    private CommentService commentService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createComment_successRootComment_createsTrimmedCommentAndReturnsClubId() {
        User user = user(1, "user@sut.ru");
        Club club = activeClub(10);
        Post post = activePost(100, club, user);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(postRepository.findById(100)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment comment = inv.getArgument(0);
            comment.setCommentId(200);
            return comment;
        });

        Integer clubId = commentService.createComment(100, "  Hello world  ", null);

        assertThat(clubId).isEqualTo(10);
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        Comment saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getPost()).isEqualTo(post);
        assertThat(saved.getParent()).isNull();
        assertThat(saved.getComment()).isEqualTo("Hello world");
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.isEdited()).isFalse();
    }

    @Test
    void createComment_unauthorized_throws401() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> commentService.createComment(100, "text", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_anonymousPrincipal_throws401() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThatThrownBy(() -> commentService.createComment(100, "text", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_emptyComment_throws400() {
        User user = user(1, "user@sut.ru");
        Club club = activeClub(10);
        Post post = activePost(100, club, user);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(postRepository.findById(100)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.createComment(100, "   ", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("не может быть пустым");
                });

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_tooLongComment_throws400() {
        User user = user(1, "user@sut.ru");
        Club club = activeClub(10);
        Post post = activePost(100, club, user);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(postRepository.findById(100)).thenReturn(Optional.of(post));

        String longComment = "a".repeat(2001);

        assertThatThrownBy(() -> commentService.createComment(100, longComment, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("2000");
                });

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_postNotFound_throws404() {
        User user = user(1, "user@sut.ru");
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(postRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(999, "text", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_postDeleted_throws400() {
        User user = user(1, "user@sut.ru");
        Club club = activeClub(10);
        Post post = activePost(100, club, user);
        post.setDeleted(true);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(postRepository.findById(100)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.createComment(100, "text", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("удалённый пост");
                });

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_replySuccess_createsNestedComment() {
        User user = user(1, "user@sut.ru");
        Club club = activeClub(10);
        Post post = activePost(100, club, user);
        Comment parent = comment(50, post, user, "Parent", false);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(postRepository.findById(100)).thenReturn(Optional.of(post));
        when(commentRepository.findById(50)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        Integer clubId = commentService.createComment(100, "  Reply  ", 50);

        assertThat(clubId).isEqualTo(10);
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        Comment saved = captor.getValue();
        assertThat(saved.getParent()).isEqualTo(parent);
        assertThat(saved.getPost()).isEqualTo(post);
        assertThat(saved.getComment()).isEqualTo("Reply");
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    void createComment_parentNotFound_throws404() {
        User user = user(1, "user@sut.ru");
        Club club = activeClub(10);
        Post post = activePost(100, club, user);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(postRepository.findById(100)).thenReturn(Optional.of(post));
        when(commentRepository.findById(55)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createComment(100, "text", 55))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("Родительский комментарий не найден");
                });

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_parentInOtherPost_throws400() {
        User user = user(1, "user@sut.ru");
        Club club = activeClub(10);
        Post post = activePost(100, club, user);
        Post otherPost = activePost(101, club, user);
        Comment parent = comment(50, otherPost, user, "Parent", false);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(postRepository.findById(100)).thenReturn(Optional.of(post));
        when(commentRepository.findById(50)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> commentService.createComment(100, "text", 50))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("другого поста");
                });

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void createComment_parentDeleted_throws400() {
        User user = user(1, "user@sut.ru");
        Club club = activeClub(10);
        Post post = activePost(100, club, user);
        Comment parent = comment(50, post, user, "Parent", true);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(postRepository.findById(100)).thenReturn(Optional.of(post));
        when(commentRepository.findById(50)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> commentService.createComment(100, "text", 50))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).contains("удалённый комментарий");
                });

        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void getCommentTreesByPostIds_emptyInput_returnsEmptyMap() {
        assertThat(commentService.getCommentTreesByPostIds(List.of(), 10)).isEmpty();
        assertThat(commentService.getCommentTreesByPostIds(null, 10)).isEmpty();
        assertThat(commentService.getCommentTreesByPostIds(List.of(1), null)).isEmpty();

        verify(commentRepository, never()).findByPostPostIdInAndIsDeletedFalseOrderByCreatedAtAsc(any());
    }

    @Test
    void getCommentTreesByPostIds_buildsTreeWithRolesAndReplies() {
        User author = user(1, "a@sut.ru");
        author.setFirstName("Ivan");
        author.setLastName("Petrov");
        User mod = user(2, "m@sut.ru");
        mod.setFirstName("Mod");
        mod.setLastName("User");
        Club club = activeClub(10);
        Post post = activePost(100, club, author);

        Comment root = comment(200, post, author, "Root", false);
        Comment reply = comment(201, post, mod, "Reply", false);
        reply.setParent(root);
        Comment otherPostRoot = comment(202, activePost(101, club, author), author, "Other", false);

        when(commentRepository.findByPostPostIdInAndIsDeletedFalseOrderByCreatedAtAsc(List.of(100, 101)))
                .thenReturn(List.of(root, reply, otherPostRoot));
        when(clubMemberRepository.findByClubClubIdAndUserUserIdIn(10, Set.of(1, 2)))
                .thenReturn(List.of(member(author, club, ClubRole.ADMIN), member(mod, club, ClubRole.MODERATOR)));

        Map<Integer, List<CommentResponse>> result = commentService.getCommentTreesByPostIds(List.of(100, 101), 10);

        assertThat(result).containsOnlyKeys(100, 101);
        assertThat(result.get(100)).hasSize(1);
        CommentResponse rootResp = result.get(100).get(0);
        assertThat(rootResp.commentId()).isEqualTo(200);
        assertThat(rootResp.authorId()).isEqualTo(1);
        assertThat(rootResp.authorFirstName()).isEqualTo("Ivan");
        assertThat(rootResp.authorLastName()).isEqualTo("Petrov");
        assertThat(rootResp.authorRoleInClub()).isEqualTo("ADMIN");
        assertThat(rootResp.comment()).isEqualTo("Root");
        assertThat(rootResp.parentId()).isNull();
        assertThat(rootResp.replies()).hasSize(1);

        CommentResponse replyResp = rootResp.replies().get(0);
        assertThat(replyResp.commentId()).isEqualTo(201);
        assertThat(replyResp.authorRoleInClub()).isEqualTo("MODERATOR");
        assertThat(replyResp.parentId()).isEqualTo(200);
        assertThat(replyResp.replies()).isEmpty();

        assertThat(result.get(101)).hasSize(1);
        assertThat(result.get(101).get(0).authorRoleInClub()).isEqualTo("ADMIN");
    }

    @Test
    void getCommentTreesByPostIds_nestedReplies_keepsThreeLevelHierarchy() {
        User author = user(1, "a@sut.ru");
        Club club = activeClub(10);
        Post post = activePost(100, club, author);

        Comment root = comment(200, post, author, "Root", false);
        Comment reply = comment(201, post, author, "Reply", false);
        reply.setParent(root);
        Comment nestedReply = comment(202, post, author, "Nested", false);
        nestedReply.setParent(reply);

        when(commentRepository.findByPostPostIdInAndIsDeletedFalseOrderByCreatedAtAsc(List.of(100)))
                .thenReturn(List.of(root, reply, nestedReply));
        when(clubMemberRepository.findByClubClubIdAndUserUserIdIn(10, Set.of(1)))
                .thenReturn(List.of(member(author, club, ClubRole.MEMBER)));

        Map<Integer, List<CommentResponse>> result = commentService.getCommentTreesByPostIds(List.of(100), 10);

        assertThat(result.get(100)).hasSize(1);
        CommentResponse rootResp = result.get(100).get(0);
        CommentResponse replyResp = rootResp.replies().get(0);
        CommentResponse nestedResp = replyResp.replies().get(0);

        assertThat(rootResp.commentId()).isEqualTo(200);
        assertThat(replyResp.commentId()).isEqualTo(201);
        assertThat(replyResp.parentId()).isEqualTo(200);
        assertThat(nestedResp.commentId()).isEqualTo(202);
        assertThat(nestedResp.parentId()).isEqualTo(201);
        assertThat(nestedResp.replies()).isEmpty();
    }

    @Test
    void getCommentTreesByPostIds_deletedParentFilteredByRepo_childBecomesRoot() {
        User author = user(1, "a@sut.ru");
        Club club = activeClub(10);
        Post post = activePost(100, club, author);
        Comment deletedParent = comment(200, post, author, "Deleted", true);
        Comment child = comment(201, post, author, "Child", false);
        child.setParent(deletedParent);

        // Repository returns only non-deleted comments.
        when(commentRepository.findByPostPostIdInAndIsDeletedFalseOrderByCreatedAtAsc(List.of(100)))
                .thenReturn(List.of(child));
        when(clubMemberRepository.findByClubClubIdAndUserUserIdIn(10, Set.of(1)))
                .thenReturn(List.of(member(author, club, ClubRole.MEMBER)));

        Map<Integer, List<CommentResponse>> result = commentService.getCommentTreesByPostIds(List.of(100), 10);

        assertThat(result.get(100)).hasSize(1);
        CommentResponse childResp = result.get(100).get(0);
        assertThat(childResp.commentId()).isEqualTo(201);
        assertThat(childResp.parentId()).isEqualTo(200);
        assertThat(childResp.replies()).isEmpty();
    }

    @Test
    void getCommentTreesByPostIds_noComments_returnsEmptyMap() {
        when(commentRepository.findByPostPostIdInAndIsDeletedFalseOrderByCreatedAtAsc(List.of(100)))
                .thenReturn(List.of());

        Map<Integer, List<CommentResponse>> result = commentService.getCommentTreesByPostIds(List.of(100), 10);

        assertThat(result).isEmpty();
        verify(clubMemberRepository, never()).findByClubClubIdAndUserUserIdIn(any(), any());
    }

    @Test
    void deleteComment_author_success_marksDeleted() {
        User author = user(1, "author@test.local");
        Club club = activeClub(10);
        Post post = activePost(100, club, author);
        Comment c = comment(200, post, author, "text", false);
        authenticate(author.getEmail());
        when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
        when(commentRepository.findById(200)).thenReturn(Optional.of(c));

        Integer clubId = commentService.deleteComment(200);

        assertThat(clubId).isEqualTo(10);
        assertThat(c.isDeleted()).isTrue();
        verify(commentRepository).save(c);
    }

    @Test
    void deleteComment_moderator_success() {
        User author = user(1, "author@test.local");
        User mod = user(2, "mod@test.local");
        Club club = activeClub(10);
        Post post = activePost(100, club, author);
        Comment c = comment(200, post, author, "text", false);
        authenticate(mod.getEmail());
        when(userRepository.findByEmail(mod.getEmail())).thenReturn(Optional.of(mod));
        when(commentRepository.findById(200)).thenReturn(Optional.of(c));
        when(clubMemberRepository.findByUserAndClub(mod, club)).thenReturn(Optional.of(member(mod, club, ClubRole.MODERATOR)));

        Integer clubId = commentService.deleteComment(200);

        assertThat(clubId).isEqualTo(10);
        assertThat(c.isDeleted()).isTrue();
    }

    @Test
    void deleteComment_notAuthorOrModerator_throws403() {
        User author = user(1, "author@test.local");
        User other = user(2, "other@test.local");
        Club club = activeClub(10);
        Post post = activePost(100, club, author);
        Comment c = comment(200, post, author, "text", false);
        authenticate(other.getEmail());
        when(userRepository.findByEmail(other.getEmail())).thenReturn(Optional.of(other));
        when(commentRepository.findById(200)).thenReturn(Optional.of(c));
        when(clubMemberRepository.findByUserAndClub(other, club)).thenReturn(Optional.of(member(other, club, ClubRole.MEMBER)));

        assertThatThrownBy(() -> commentService.deleteComment(200))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void deleteComment_alreadyDeleted_throws400() {
        User author = user(1, "author@test.local");
        Club club = activeClub(10);
        Post post = activePost(100, club, author);
        Comment c = comment(200, post, author, "text", true);
        authenticate(author.getEmail());
        when(userRepository.findByEmail(author.getEmail())).thenReturn(Optional.of(author));
        when(commentRepository.findById(200)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> commentService.deleteComment(200))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void deleteComment_notFound_throws404() {
        User user = user(1, "u@test.local");
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(commentRepository.findById(200)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(200))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "pw", List.of()));
    }

    private static User user(Integer id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }

    private static Club activeClub(Integer id) {
        Club club = new Club();
        club.setClubId(id);
        club.setClub("Club " + id);
        club.setActive(true);
        club.setBlocked(false);
        club.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        club.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return club;
    }

    private static Post activePost(Integer id, Club club, User user) {
        Post post = new Post();
        post.setPostId(id);
        post.setClub(club);
        post.setUser(user);
        post.setTitle("Title");
        post.setBody("Body");
        post.setDeleted(false);
        post.setEdited(false);
        post.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        post.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return post;
    }

    private static Comment comment(Integer id, Post post, User user, String text, boolean deleted) {
        Comment comment = new Comment();
        comment.setCommentId(id);
        comment.setPost(post);
        comment.setUser(user);
        comment.setComment(text);
        comment.setDeleted(deleted);
        comment.setEdited(false);
        comment.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        comment.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return comment;
    }

    private static ClubMember member(User user, Club club, ClubRole role) {
        ClubMember member = new ClubMember();
        member.setUser(user);
        member.setClub(club);
        member.setUserRole(role);
        member.setJoinedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return member;
    }
}
