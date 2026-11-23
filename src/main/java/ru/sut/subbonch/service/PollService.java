package ru.sut.subbonch.service;

import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.entity.Ballot;
import ru.sut.subbonch.entity.Poll;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.entity.Vote;
import ru.sut.subbonch.repository.BallotRepository;
import ru.sut.subbonch.repository.PollRepository;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.repository.VoteRepository;

@Service
public class PollService {

    private final PollRepository pollRepository;
    private final VoteRepository voteRepository;
    private final BallotRepository ballotRepository;
    private final UserRepository userRepository;

    public PollService(PollRepository pollRepository,
                       VoteRepository voteRepository,
                       BallotRepository ballotRepository,
                       UserRepository userRepository) {
        this.pollRepository = pollRepository;
        this.voteRepository = voteRepository;
        this.ballotRepository = ballotRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void vote(Integer pollId, Integer voteId) {
        User currentUser = getCurrentUserOrThrow();
        if (pollId == null || voteId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректный запрос голосования");
        }

        Poll poll = pollRepository.findByIdForUpdate(pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Опрос не найден"));

        Vote voteOption = voteRepository.findById(voteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Вариант ответа не найден"));

        if (voteOption.getPoll() == null || voteOption.getPoll().getPollId() == null
                || !voteOption.getPoll().getPollId().equals(poll.getPollId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Вариант ответа не принадлежит опросу");
        }

        if (poll.getClosesAt() != null && !LocalDateTime.now().isBefore(poll.getClosesAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Голосование закрыто");
        }

        if (poll.isMultiple()) {
            if (ballotRepository.existsByUserAndVote(currentUser, voteOption)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Вы уже голосовали за этот вариант");
            }
        } else {
            if (!ballotRepository.findByUserAndVotePoll(currentUser, poll).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Вы уже голосовали в этом опросе");
            }
        }

        Ballot ballot = new Ballot();
        ballot.setUser(currentUser);
        ballot.setVote(voteOption);
        try {
            ballotRepository.save(ballot);
            ballotRepository.flush();
            int updated = voteRepository.incrementTotal(voteOption.getVoteId());
            if (updated != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Не удалось учесть голос");
            }
        } catch (DataIntegrityViolationException ex) {
            if (ballotRepository.existsByUserAndVote(currentUser, voteOption)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Вы уже голосовали за этот вариант", ex);
            }
            if (!poll.isMultiple() && !ballotRepository.findByUserAndVotePoll(currentUser, poll).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Вы уже голосовали в этом опросе", ex);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не удалось учесть голос", ex);
        }
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
