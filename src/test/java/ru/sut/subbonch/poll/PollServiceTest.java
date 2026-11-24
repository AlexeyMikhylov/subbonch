package ru.sut.subbonch.poll;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.entity.Ballot;
import ru.sut.subbonch.entity.Poll;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.entity.Vote;
import ru.sut.subbonch.repository.BallotRepository;
import ru.sut.subbonch.repository.PollRepository;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.repository.VoteRepository;
import ru.sut.subbonch.service.PollService;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock
    private PollRepository pollRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private BallotRepository ballotRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PollService pollService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void vote_successSingleChoice_savesBallotAndIncrementsTotal() {
        User user = user(1, "user@sut.ru");
        Poll poll = poll(10, false, null);
        Vote voteOption = vote(100, poll);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(pollRepository.findByIdForUpdate(10)).thenReturn(Optional.of(poll));
        when(voteRepository.findById(100)).thenReturn(Optional.of(voteOption));
        when(ballotRepository.findByUserAndVotePoll(user, poll)).thenReturn(List.of());
        when(voteRepository.incrementTotal(100)).thenReturn(1);

        pollService.vote(10, 100);

        ArgumentCaptor<Ballot> ballotCaptor = ArgumentCaptor.forClass(Ballot.class);
        verify(ballotRepository).save(ballotCaptor.capture());
        verify(ballotRepository).flush();
        verify(voteRepository).incrementTotal(100);
        Ballot saved = ballotCaptor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getVote()).isEqualTo(voteOption);
    }

    @Test
    void vote_unauthorized_throws401() {
        assertThatThrownBy(() -> pollService.vote(10, 100))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
                });

        verify(ballotRepository, never()).save(any(Ballot.class));
        verify(voteRepository, never()).incrementTotal(any());
    }

    @Test
    void vote_pollNotFound_throws404() {
        User user = user(1, "user@sut.ru");
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(pollRepository.findByIdForUpdate(10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.vote(10, 100))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value())
                        .isEqualTo(HttpStatus.NOT_FOUND.value()));

        verify(ballotRepository, never()).save(any(Ballot.class));
    }

    @Test
    void vote_voteNotInPoll_throws400() {
        User user = user(1, "user@sut.ru");
        Poll poll = poll(10, false, null);
        Poll anotherPoll = poll(11, false, null);
        Vote voteOption = vote(100, anotherPoll);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(pollRepository.findByIdForUpdate(10)).thenReturn(Optional.of(poll));
        when(voteRepository.findById(100)).thenReturn(Optional.of(voteOption));

        assertThatThrownBy(() -> pollService.vote(10, 100))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                    assertThat(rse.getReason()).contains("не принадлежит");
                });

        verify(ballotRepository, never()).save(any(Ballot.class));
        verify(voteRepository, never()).incrementTotal(any());
    }

    @Test
    void vote_closedPoll_throws400() {
        User user = user(1, "user@sut.ru");
        Poll poll = poll(10, false, LocalDateTime.now().minusMinutes(1));
        Vote voteOption = vote(100, poll);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(pollRepository.findByIdForUpdate(10)).thenReturn(Optional.of(poll));
        when(voteRepository.findById(100)).thenReturn(Optional.of(voteOption));

        assertThatThrownBy(() -> pollService.vote(10, 100))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                    assertThat(rse.getReason()).contains("закрыто");
                });

        verify(ballotRepository, never()).save(any(Ballot.class));
        verify(voteRepository, never()).incrementTotal(any());
    }

    @Test
    void vote_duplicateSingleChoice_throws400() {
        User user = user(1, "user@sut.ru");
        Poll poll = poll(10, false, null);
        Vote voteOption = vote(100, poll);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(pollRepository.findByIdForUpdate(10)).thenReturn(Optional.of(poll));
        when(voteRepository.findById(100)).thenReturn(Optional.of(voteOption));
        when(ballotRepository.findByUserAndVotePoll(user, poll)).thenReturn(List.of(new Ballot()));

        assertThatThrownBy(() -> pollService.vote(10, 100))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                    assertThat(rse.getReason()).contains("уже голосовали");
                });

        verify(ballotRepository, never()).save(any(Ballot.class));
        verify(voteRepository, never()).incrementTotal(any());
    }

    @Test
    void vote_duplicateOptionInMultiplePoll_throws400() {
        User user = user(1, "user@sut.ru");
        Poll poll = poll(10, true, null);
        Vote voteOption = vote(100, poll);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(pollRepository.findByIdForUpdate(10)).thenReturn(Optional.of(poll));
        when(voteRepository.findById(100)).thenReturn(Optional.of(voteOption));
        when(ballotRepository.existsByUserAndVote(user, voteOption)).thenReturn(true);

        assertThatThrownBy(() -> pollService.vote(10, 100))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                    assertThat(rse.getReason()).contains("за этот вариант");
                });

        verify(ballotRepository, never()).save(any(Ballot.class));
        verify(voteRepository, never()).incrementTotal(any());
    }

    @Test
    void vote_concurrentDuplicate_throws400() {
        User user = user(1, "user@sut.ru");
        Poll poll = poll(10, false, null);
        Vote voteOption = vote(100, poll);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(pollRepository.findByIdForUpdate(10)).thenReturn(Optional.of(poll));
        when(voteRepository.findById(100)).thenReturn(Optional.of(voteOption));
        when(ballotRepository.findByUserAndVotePoll(user, poll)).thenReturn(List.of());
        when(ballotRepository.save(any(Ballot.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> pollService.vote(10, 100))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value())
                        .isEqualTo(HttpStatus.BAD_REQUEST.value()));

        verify(voteRepository, never()).incrementTotal(any());
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "password", List.of()));
    }

    private static User user(Integer id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        return user;
    }

    private static Poll poll(Integer id, boolean multiple, LocalDateTime closesAt) {
        Poll poll = new Poll();
        poll.setPollId(id);
        poll.setTitle("Poll");
        poll.setMultiple(multiple);
        poll.setClosesAt(closesAt);
        return poll;
    }

    private static Vote vote(Integer id, Poll poll) {
        Vote vote = new Vote();
        vote.setVoteId(id);
        vote.setPoll(poll);
        vote.setVote("Option");
        vote.setTotal(0);
        return vote;
    }
}
