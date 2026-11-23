package ru.sut.subbonch.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
import ru.sut.subbonch.dto.PostCreateRequest;
import ru.sut.subbonch.dto.PostListItemResponse;
import ru.sut.subbonch.entity.AttachmentPost;
import ru.sut.subbonch.entity.AttachmentType;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.Post;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.AttachmentPostRepository;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.ClubRepository;
import ru.sut.subbonch.repository.PostRepository;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.service.PostService;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private AttachmentPostRepository attachmentPostRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPost_moderatorSuccess_createsPostAndRedirectData() {
        User moderator = user(1, "mod@sut.ru");
        Club club = activeClub(10);
        authenticate("mod@sut.ru");
        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(moderator));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(moderator, club))
                .thenReturn(Optional.of(member(moderator, club, ClubRole.MODERATOR)));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post post = inv.getArgument(0);
            post.setPostId(100);
            return post;
        });

        PostCreateRequest request = request("  Title  ", "  Body  ", " ");

        Post created = postService.createPost(10, request);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        Post saved = captor.getValue();
        assertThat(saved.getClub()).isEqualTo(club);
        assertThat(saved.getUser()).isEqualTo(moderator);
        assertThat(saved.getTitle()).isEqualTo("Title");
        assertThat(saved.getBody()).isEqualTo("Body");
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.isEdited()).isFalse();
        assertThat(created.getPostId()).isEqualTo(100);
        verify(attachmentPostRepository, never()).save(any(AttachmentPost.class));
    }

    @Test
    void createPost_adminSuccess_createsImageAttachment() {
        User admin = user(2, "admin@sut.ru");
        Club club = activeClub(10);
        authenticate("admin@sut.ru");
        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(admin, club))
                .thenReturn(Optional.of(member(admin, club, ClubRole.ADMIN)));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post post = inv.getArgument(0);
            post.setPostId(101);
            return post;
        });

        PostCreateRequest request = request("Title", "Body", " https://example.com/img.png ");

        postService.createPost(10, request);

        ArgumentCaptor<AttachmentPost> captor = ArgumentCaptor.forClass(AttachmentPost.class);
        verify(attachmentPostRepository).save(captor.capture());
        AttachmentPost attachment = captor.getValue();
        assertThat(attachment.getPost().getPostId()).isEqualTo(101);
        assertThat(attachment.getAttachType()).isEqualTo(AttachmentType.IMAGE);
        assertThat(attachment.getAttachUrl()).isEqualTo("https://example.com/img.png");
    }

    @Test
    void createPost_memberRole_returnsForbidden() {
        User current = user(3, "member@sut.ru");
        Club club = activeClub(10);
        authenticate("member@sut.ru");
        when(userRepository.findByEmail("member@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(current, club))
                .thenReturn(Optional.of(member(current, club, ClubRole.MEMBER)));

        assertThatThrownBy(() -> postService.createPost(10, request("Title", "Body", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(postRepository, never()).save(any(Post.class));
        verify(attachmentPostRepository, never()).save(any(AttachmentPost.class));
    }

    @Test
    void createPost_guest_returnsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> postService.createPost(10, request("Title", "Body", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(clubRepository, never()).findById(any());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void createPost_anonymousPrincipal_returnsUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThatThrownBy(() -> postService.createPost(10, request("Title", "Body", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void createPost_emptyTitle_returnsBadRequest() {
        User moderator = user(1, "mod@sut.ru");
        Club club = activeClub(10);
        authenticate("mod@sut.ru");
        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(moderator));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(moderator, club))
                .thenReturn(Optional.of(member(moderator, club, ClubRole.MODERATOR)));

        assertThatThrownBy(() -> postService.createPost(10, request("  ", "Body", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void createPost_emptyBody_returnsBadRequest() {
        User moderator = user(1, "mod@sut.ru");
        Club club = activeClub(10);
        authenticate("mod@sut.ru");
        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(moderator));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(moderator, club))
                .thenReturn(Optional.of(member(moderator, club, ClubRole.MODERATOR)));

        assertThatThrownBy(() -> postService.createPost(10, request("Title", "   ", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void createPost_unavailableClub_returnsNotFound() {
        User moderator = user(1, "mod@sut.ru");
        Club blockedClub = activeClub(10);
        blockedClub.setBlocked(true);
        authenticate("mod@sut.ru");
        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(moderator));
        when(clubRepository.findById(10)).thenReturn(Optional.of(blockedClub));

        assertThatThrownBy(() -> postService.createPost(10, request("Title", "Body", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(clubMemberRepository, never()).findByUserAndClub(any(), any());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void createPost_nonexistentClub_returnsNotFound() {
        User moderator = user(1, "mod@sut.ru");
        authenticate("mod@sut.ru");
        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(moderator));
        when(clubRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(999, request("Title", "Body", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void deletePost_moderatorSuccess_marksDeleted() {
        User moderator = user(1, "mod@sut.ru");
        Club club = activeClub(10);
        Post post = post(100, club, moderator, false);
        authenticate("mod@sut.ru");
        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(moderator));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(moderator, club))
                .thenReturn(Optional.of(member(moderator, club, ClubRole.MODERATOR)));
        when(postRepository.findByPostIdAndClubClubId(100, 10)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        postService.deletePost(10, 100);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
    }

    @Test
    void deletePost_adminSuccess_marksDeleted() {
        User admin = user(2, "admin@sut.ru");
        Club club = activeClub(10);
        Post post = post(100, club, admin, false);
        authenticate("admin@sut.ru");
        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(admin, club))
                .thenReturn(Optional.of(member(admin, club, ClubRole.ADMIN)));
        when(postRepository.findByPostIdAndClubClubId(100, 10)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        postService.deletePost(10, 100);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
    }

    @Test
    void deletePost_memberRole_returnsForbidden() {
        User current = user(3, "member@sut.ru");
        Club club = activeClub(10);
        authenticate("member@sut.ru");
        when(userRepository.findByEmail("member@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(current, club))
                .thenReturn(Optional.of(member(current, club, ClubRole.MEMBER)));

        assertThatThrownBy(() -> postService.deletePost(10, 100))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void deletePost_guest_returnsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> postService.deletePost(10, 100))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void deletePost_postOfAnotherClub_returnsNotFound() {
        User moderator = user(1, "mod@sut.ru");
        Club club = activeClub(10);
        authenticate("mod@sut.ru");
        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(moderator));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(moderator, club))
                .thenReturn(Optional.of(member(moderator, club, ClubRole.MODERATOR)));
        when(postRepository.findByPostIdAndClubClubId(100, 10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(10, 100))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void deletePost_alreadyDeleted_returnsBadRequest() {
        User moderator = user(1, "mod@sut.ru");
        Club club = activeClub(10);
        Post post = post(100, club, moderator, true);
        authenticate("mod@sut.ru");
        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(moderator));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(moderator, club))
                .thenReturn(Optional.of(member(moderator, club, ClubRole.MODERATOR)));
        when(postRepository.findByPostIdAndClubClubId(100, 10)).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(10, 100))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("Пост уже удалён");
                });

        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void deletePost_nonexistentPost_returnsNotFound() {
        User moderator = user(1, "mod@sut.ru");
        Club club = activeClub(10);
        authenticate("mod@sut.ru");
        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(moderator));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(moderator, club))
                .thenReturn(Optional.of(member(moderator, club, ClubRole.MODERATOR)));
        when(postRepository.findByPostIdAndClubClubId(999, 10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(10, 999))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(postRepository, never()).save(any(Post.class));
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

    private static ClubMember member(User user, Club club, ClubRole role) {
        ClubMember member = new ClubMember();
        member.setUser(user);
        member.setClub(club);
        member.setUserRole(role);
        member.setJoinedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return member;
    }

    private static Post post(Integer id, Club club, User user, boolean isDeleted) {
        Post post = new Post();
        post.setPostId(id);
        post.setClub(club);
        post.setUser(user);
        post.setTitle("Title");
        post.setBody("Body");
        post.setDeleted(isDeleted);
        post.setEdited(false);
        post.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        post.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return post;
    }

    private static PostCreateRequest request(String title, String body, String imageUrl) {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle(title);
        request.setBody(body);
        request.setImageUrl(imageUrl);
        return request;
    }

    @Test
    void getClubPosts_returnsOnlyActivePostsSortedDescWithAuthorAndImage() {
        Club club = activeClub(10);
        User author = user(1, "author@sut.ru");
        author.setFirstName("Ivan");
        author.setLastName("Petrov");

        Post newer = post(101, club, author, false);
        newer.setTitle("New");
        newer.setBody("New body");
        newer.setCreatedAt(LocalDateTime.of(2026, 5, 2, 12, 0));
        newer.setEdited(true);

        Post older = post(100, club, author, false);
        older.setTitle("Old");
        older.setBody("Old body");
        older.setCreatedAt(LocalDateTime.of(2026, 5, 1, 12, 0));
        older.setEdited(false);

        when(postRepository.findActiveByClubIdWithUser(10)).thenReturn(List.of(newer, older));

        AttachmentPost image = new AttachmentPost();
        image.setAttachmentId(1);
        image.setPost(newer);
        image.setAttachType(AttachmentType.IMAGE);
        image.setAttachUrl("https://example.com/new.png");
        when(attachmentPostRepository.findByPostPostIdIn(List.of(101, 100))).thenReturn(List.of(image));

        List<PostListItemResponse> result = postService.getClubPosts(10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).postId()).isEqualTo(101);
        assertThat(result.get(0).title()).isEqualTo("New");
        assertThat(result.get(0).body()).isEqualTo("New body");
        assertThat(result.get(0).isEdited()).isTrue();
        assertThat(result.get(0).authorId()).isEqualTo(1);
        assertThat(result.get(0).authorFirstName()).isEqualTo("Ivan");
        assertThat(result.get(0).authorLastName()).isEqualTo("Petrov");
        assertThat(result.get(0).imageUrl()).isEqualTo("https://example.com/new.png");
        assertThat(result.get(1).postId()).isEqualTo(100);
        assertThat(result.get(1).imageUrl()).isNull();

        verify(postRepository).findActiveByClubIdWithUser(10);
        verify(attachmentPostRepository).findByPostPostIdIn(List.of(101, 100));
    }

    @Test
    void getClubPosts_emptyList_returnsEmptyAndSkipsAttachmentQuery() {
        when(postRepository.findActiveByClubIdWithUser(10)).thenReturn(List.of());

        List<PostListItemResponse> result = postService.getClubPosts(10);

        assertThat(result).isEmpty();
        verify(attachmentPostRepository, never()).findByPostPostIdIn(any());
    }

    @Test
    void getClubPosts_nullClubId_returnsEmpty() {
        List<PostListItemResponse> result = postService.getClubPosts(null);

        assertThat(result).isEmpty();
        verify(postRepository, never()).findActiveByClubIdWithUser(any());
        verify(attachmentPostRepository, never()).findByPostPostIdIn(any());
    }
}
