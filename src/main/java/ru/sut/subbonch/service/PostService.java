package ru.sut.subbonch.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
public class PostService {

    static final int MAX_TITLE_LENGTH = 255;
    static final int MAX_IMAGE_URL_LENGTH = 512;

    private final PostRepository postRepository;
    private final AttachmentPostRepository attachmentPostRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository,
                       AttachmentPostRepository attachmentPostRepository,
                       ClubRepository clubRepository,
                       ClubMemberRepository clubMemberRepository,
                       UserRepository userRepository) {
        this.postRepository = postRepository;
        this.attachmentPostRepository = attachmentPostRepository;
        this.clubRepository = clubRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Post createPost(Integer clubId, PostCreateRequest request) {
        User currentUser = getCurrentUserOrThrow();
        Club club = getAvailableClubOrThrow(clubId);
        assertCanManagePosts(currentUser, club);

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Запрос на создание поста не заполнен");
        }

        String title = normalizeRequired(request.getTitle());
        String body = normalizeRequired(request.getBody());
        String imageUrl = normalizeOptional(request.getImageUrl());
        validateCreateValues(title, body, imageUrl);

        Post post = new Post();
        post.setClub(club);
        post.setUser(currentUser);
        post.setTitle(title);
        post.setBody(body);
        post.setDeleted(false);
        post.setEdited(false);
        Post savedPost = postRepository.save(post);

        if (imageUrl != null) {
            AttachmentPost attachment = new AttachmentPost();
            attachment.setPost(savedPost);
            attachment.setAttachType(AttachmentType.IMAGE);
            attachment.setAttachUrl(imageUrl);
            attachmentPostRepository.save(attachment);
        }

        return savedPost;
    }

    @Transactional
    public void deletePost(Integer clubId, Integer postId) {
        User currentUser = getCurrentUserOrThrow();
        Club club = getAvailableClubOrThrow(clubId);
        assertCanManagePosts(currentUser, club);

        if (postId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден");
        }

        Post post = postRepository.findByPostIdAndClubClubId(postId, clubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден"));
        if (post.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пост уже удалён");
        }

        post.setDeleted(true);
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public List<PostListItemResponse> getClubPosts(Integer clubId) {
        if (clubId == null) {
            return List.of();
        }
        List<Post> posts = postRepository.findActiveByClubIdWithUser(clubId);
        if (posts.isEmpty()) {
            return List.of();
        }
        List<Integer> postIds = posts.stream()
                .map(Post::getPostId)
                .toList();
        Map<Integer, String> imageUrlByPostId = attachmentPostRepository.findByPostPostIdIn(postIds).stream()
                .filter(attachment -> attachment.getAttachType() == AttachmentType.IMAGE)
                .collect(Collectors.toMap(
                        attachment -> attachment.getPost().getPostId(),
                        AttachmentPost::getAttachUrl,
                        (existing, ignored) -> existing));
        return posts.stream()
                .map(post -> new PostListItemResponse(
                        post.getPostId(),
                        post.getTitle(),
                        post.getBody(),
                        post.getCreatedAt(),
                        post.isEdited(),
                        post.getUser().getUserId(),
                        post.getUser().getFirstName(),
                        post.getUser().getLastName(),
                        imageUrlByPostId.get(post.getPostId())))
                .toList();
    }

    private Club getAvailableClubOrThrow(Integer clubId) {
        if (clubId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Клуб не найден");
        }
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Клуб не найден"));
        if (!club.isActive() || club.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Клуб недоступен");
        }
        return club;
    }

    private void assertCanManagePosts(User currentUser, Club club) {
        ClubMember membership = clubMemberRepository.findByUserAndClub(currentUser, club)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Недостаточно прав"));
        if (membership.getUserRole() != ClubRole.MODERATOR && membership.getUserRole() != ClubRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Недостаточно прав");
        }
    }

    private static void validateCreateValues(String title, String body, String imageUrl) {
        if (title == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заголовок поста не может быть пустым");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заголовок поста не должен превышать 255 символов");
        }
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Текст поста не может быть пустым");
        }
        if (imageUrl != null && imageUrl.length() > MAX_IMAGE_URL_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ссылка на изображение не должна превышать 512 символов");
        }
    }

    private static String normalizeRequired(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User getCurrentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof String s && "anonymousUser".equals(s)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }
        String email;
        if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        } else {
            email = String.valueOf(principal);
        }
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация"));
    }
}
