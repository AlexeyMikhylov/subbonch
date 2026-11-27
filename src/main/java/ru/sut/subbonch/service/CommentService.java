package ru.sut.subbonch.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.dto.CommentResponse;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.Comment;
import ru.sut.subbonch.entity.Post;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.CommentRepository;
import ru.sut.subbonch.repository.PostRepository;
import ru.sut.subbonch.repository.UserRepository;

@Service
public class CommentService {

    static final int MAX_COMMENT_LENGTH = 2000;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository;

    public CommentService(CommentRepository commentRepository,
                          PostRepository postRepository,
                          UserRepository userRepository,
                          ClubMemberRepository clubMemberRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.clubMemberRepository = clubMemberRepository;
    }

    @Transactional
    public Integer createComment(Integer postId, String commentText, Integer parentId) {
        User currentUser = getCurrentUserOrThrow();

        if (postId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пост не найден"));
        if (post.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя комментировать удалённый пост");
        }

        String normalizedComment = normalizeRequired(commentText);
        validateComment(normalizedComment);

        Comment parent = null;
        if (parentId != null) {
            parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Родительский комментарий не найден"));
            if (parent.isDeleted()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя ответить на удалённый комментарий");
            }
            if (parent.getPost() == null || parent.getPost().getPostId() == null
                    || !parent.getPost().getPostId().equals(post.getPostId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя ответить на комментарий другого поста");
            }
        }

        Comment comment = new Comment();
        comment.setUser(currentUser);
        comment.setPost(post);
        comment.setParent(parent);
        comment.setComment(normalizedComment);
        comment.setDeleted(false);
        comment.setEdited(false);
        commentRepository.save(comment);
        return post.getClub().getClubId();
    }

    @Transactional
    public Integer deleteComment(Integer commentId) {
        User currentUser = getCurrentUserOrThrow();
        if (commentId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Комментарий не найден");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Комментарий не найден"));
        if (comment.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Комментарий уже удалён");
        }

        Post post = comment.getPost();
        if (post == null || post.getClub() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Комментарий не найден");
        }

        boolean isAuthor = comment.getUser() != null
                && comment.getUser().getUserId() != null
                && comment.getUser().getUserId().equals(currentUser.getUserId());
        boolean canModerate = clubMemberRepository.findByUserAndClub(currentUser, post.getClub())
                .map(m -> m.getUserRole() == ClubRole.MODERATOR || m.getUserRole() == ClubRole.ADMIN)
                .orElse(false);
        if (!isAuthor && !canModerate) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Недостаточно прав для удаления комментария");
        }

        comment.setDeleted(true);
        commentRepository.save(comment);
        return post.getClub().getClubId();
    }

    @Transactional(readOnly = true)
    public Map<Integer, List<CommentResponse>> getCommentTreesByPostIds(Collection<Integer> postIds, Integer clubId) {
        if (postIds == null || postIds.isEmpty() || clubId == null) {
            return Collections.emptyMap();
        }
        List<Comment> comments = commentRepository
                .findByPostPostIdInAndIsDeletedFalseOrderByCreatedAtAsc(postIds);
        if (comments.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Integer> authorIds = comments.stream()
                .map(c -> c.getUser().getUserId())
                .collect(Collectors.toSet());
        Map<Integer, String> roleByUserId = loadRoleByUserId(clubId, authorIds);

        Map<Integer, CommentResponse> byId = new LinkedHashMap<>();
        Map<Integer, Integer> postIdByCommentId = new HashMap<>();
        for (Comment c : comments) {
            Integer parentId = c.getParent() != null ? c.getParent().getCommentId() : null;
            CommentResponse response = new CommentResponse(
                    c.getCommentId(),
                    c.getUser().getUserId(),
                    c.getUser().getFirstName(),
                    c.getUser().getLastName(),
                    c.getUser().getProfilePictureUrl(),
                    roleByUserId.get(c.getUser().getUserId()),
                    c.getComment(),
                    c.getCreatedAt(),
                    parentId,
                    new ArrayList<>());
            byId.put(c.getCommentId(), response);
            postIdByCommentId.put(c.getCommentId(), c.getPost().getPostId());
        }

        Map<Integer, List<CommentResponse>> rootsByPostId = new HashMap<>();
        for (Comment c : comments) {
            CommentResponse response = byId.get(c.getCommentId());
            Integer postId = postIdByCommentId.get(c.getCommentId());
            Integer parentId = response.parentId();
            CommentResponse parentResponse = parentId != null ? byId.get(parentId) : null;
            if (parentResponse != null) {
                parentResponse.replies().add(response);
            } else {
                rootsByPostId.computeIfAbsent(postId, k -> new ArrayList<>()).add(response);
            }
        }
        return rootsByPostId;
    }

    private Map<Integer, String> loadRoleByUserId(Integer clubId, Set<Integer> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ClubMember> members = clubMemberRepository.findByClubClubIdAndUserUserIdIn(clubId, authorIds);
        Map<Integer, String> roleByUserId = new HashMap<>();
        for (ClubMember member : members) {
            roleByUserId.put(member.getUser().getUserId(), member.getUserRole().name());
        }
        return roleByUserId;
    }

    private static void validateComment(String comment) {
        if (comment == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Текст комментария не может быть пустым");
        }
        if (comment.length() > MAX_COMMENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Комментарий не должен превышать 2000 символов");
        }
    }

    private static String normalizeRequired(String value) {
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
