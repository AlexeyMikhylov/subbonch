package ru.sut.subbonch.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import ru.sut.subbonch.dto.PollOptionResponse;
import ru.sut.subbonch.dto.PollResponse;
import ru.sut.subbonch.dto.PostCreateRequest;
import ru.sut.subbonch.dto.PostListItemResponse;
import ru.sut.subbonch.entity.AttachmentPost;
import ru.sut.subbonch.entity.AttachmentType;
import ru.sut.subbonch.entity.Ballot;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.Poll;
import ru.sut.subbonch.entity.Post;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.entity.Vote;
import ru.sut.subbonch.repository.AttachmentPostRepository;
import ru.sut.subbonch.repository.BallotRepository;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.ClubRepository;
import ru.sut.subbonch.repository.PollRepository;
import ru.sut.subbonch.repository.PostRepository;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.repository.VoteRepository;

@Service
public class PostService {

    static final int MAX_TITLE_LENGTH = 255;
    static final int MAX_IMAGE_URL_LENGTH = 512;
    static final int MIN_POLL_OPTIONS = 2;
    static final int MAX_POLL_TEXT_LENGTH = 255;

    private final PostRepository postRepository;
    private final AttachmentPostRepository attachmentPostRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;
    private final PollRepository pollRepository;
    private final VoteRepository voteRepository;
    private final BallotRepository ballotRepository;

    public PostService(PostRepository postRepository,
                       AttachmentPostRepository attachmentPostRepository,
                       ClubRepository clubRepository,
                       ClubMemberRepository clubMemberRepository,
                       UserRepository userRepository,
                       PollRepository pollRepository,
                       VoteRepository voteRepository,
                       BallotRepository ballotRepository) {
        this.postRepository = postRepository;
        this.attachmentPostRepository = attachmentPostRepository;
        this.clubRepository = clubRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.userRepository = userRepository;
        this.pollRepository = pollRepository;
        this.voteRepository = voteRepository;
        this.ballotRepository = ballotRepository;
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

        createPollIfRequested(savedPost, request);

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
        Map<Integer, PollResponse> pollByPostId = loadPollsByPostIds(postIds);
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
                        imageUrlByPostId.get(post.getPostId()),
                        pollByPostId.get(post.getPostId())))
                .toList();
    }

    private Map<Integer, PollResponse> loadPollsByPostIds(List<Integer> postIds) {
        List<Poll> polls = pollRepository.findByPostPostIdIn(postIds);
        if (polls.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Integer> pollIds = polls.stream()
                .map(Poll::getPollId)
                .toList();

        Map<Integer, List<Vote>> votesByPollId = voteRepository.findByPollPollIdIn(pollIds).stream()
                .collect(Collectors.groupingBy(vote -> vote.getPoll().getPollId()));

        Set<Integer> selectedVoteIds = loadSelectedVoteIds(pollIds);
        LocalDateTime now = LocalDateTime.now();

        Map<Integer, PollResponse> result = new HashMap<>();
        for (Poll poll : polls) {
            List<Vote> votes = votesByPollId.getOrDefault(poll.getPollId(), List.of()).stream()
                    .sorted(Comparator.comparing(Vote::getVoteId))
                    .toList();
            List<PollOptionResponse> options = votes.stream()
                    .map(vote -> new PollOptionResponse(
                            vote.getVoteId(),
                            vote.getVote(),
                            vote.getTotal(),
                            selectedVoteIds.contains(vote.getVoteId())))
                    .toList();
            boolean closed = poll.getClosesAt() != null && !now.isBefore(poll.getClosesAt());
            boolean alreadyVoted = options.stream().anyMatch(PollOptionResponse::selectedByCurrentUser);
            result.put(poll.getPost().getPostId(), new PollResponse(
                    poll.getPollId(),
                    poll.getTitle(),
                    poll.isMultiple(),
                    poll.getClosesAt(),
                    closed,
                    alreadyVoted,
                    options));
        }
        return result;
    }

    private Set<Integer> loadSelectedVoteIds(List<Integer> pollIds) {
        User currentUser = getCurrentUserOptional();
        if (currentUser == null || pollIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Ballot> ballots = ballotRepository.findByUserAndPollIds(currentUser, pollIds);
        if (ballots.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Integer> selectedVoteIds = new HashSet<>();
        for (Ballot ballot : ballots) {
            selectedVoteIds.add(ballot.getVote().getVoteId());
        }
        return selectedVoteIds;
    }

    private User getCurrentUserOptional() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof String s && "anonymousUser".equals(s)) {
            return null;
        }
        String email;
        if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        } else {
            email = String.valueOf(principal);
        }
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return null;
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    private void createPollIfRequested(Post post, PostCreateRequest request) {
        String pollTitle = normalizeOptional(request.getPollTitle());
        List<String> normalizedOptions = normalizePollOptions(request.getPollOptions());
        String pollClosesAtRaw = normalizeOptional(request.getPollClosesAt());
        boolean pollMultiple = Boolean.TRUE.equals(request.getPollMultiple());

        boolean pollRequested = pollTitle != null || !normalizedOptions.isEmpty() || pollClosesAtRaw != null;
        if (!pollRequested) {
            return;
        }

        validatePoll(pollTitle, normalizedOptions);
        LocalDateTime closesAt = parsePollClosesAt(pollClosesAtRaw);

        Poll poll = new Poll();
        poll.setPost(post);
        poll.setTitle(pollTitle);
        poll.setMultiple(pollMultiple);
        poll.setClosesAt(closesAt);
        Poll savedPoll = pollRepository.save(poll);

        for (String optionText : normalizedOptions) {
            Vote vote = new Vote();
            vote.setPoll(savedPoll);
            vote.setVote(optionText);
            vote.setTotal(0);
            voteRepository.save(vote);
        }
    }

    private static void validatePoll(String pollTitle, List<String> options) {
        if (pollTitle == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Вопрос опроса не может быть пустым");
        }
        if (pollTitle.length() > MAX_POLL_TEXT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Вопрос опроса не должен превышать 255 символов");
        }
        if (options.size() < MIN_POLL_OPTIONS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Опрос должен содержать минимум 2 варианта ответа");
        }
        for (String option : options) {
            if (option.length() > MAX_POLL_TEXT_LENGTH) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Вариант ответа не должен превышать 255 символов");
            }
        }
    }

    private static List<String> normalizePollOptions(List<String> rawOptions) {
        if (rawOptions == null || rawOptions.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String rawOption : rawOptions) {
            String option = normalizeOptional(rawOption);
            if (option != null) {
                normalized.add(option);
            }
        }
        return normalized;
    }

    private static LocalDateTime parsePollClosesAt(String rawClosesAt) {
        if (rawClosesAt == null) {
            return null;
        }
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(rawClosesAt, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректная дата закрытия опроса");
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
