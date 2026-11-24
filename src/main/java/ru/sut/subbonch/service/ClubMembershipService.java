package ru.sut.subbonch.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.ClubRepository;
import ru.sut.subbonch.repository.UserRepository;

@Service
public class ClubMembershipService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;

    public ClubMembershipService(ClubRepository clubRepository,
                                 ClubMemberRepository clubMemberRepository,
                                 UserRepository userRepository) {
        this.clubRepository = clubRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void joinClub(Integer clubId) {
        User currentUser = getCurrentUserOrThrow();
        Club club = clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(clubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Клуб не найден"));

        if (clubMemberRepository.existsByUserAndClub(currentUser, club)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Вы уже являетесь участником клуба");
        }

        ClubMember member = new ClubMember();
        member.setUser(currentUser);
        member.setClub(club);
        member.setUserRole(ClubRole.MEMBER);

        try {
            clubMemberRepository.save(member);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Вы уже являетесь участником клуба", ex);
        }
    }

    @Transactional
    public void leaveClub(Integer clubId) {
        User currentUser = getCurrentUserOrThrow();
        Club club = clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(clubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Клуб не найден"));

        ClubMember member = clubMemberRepository.findByUserAndClub(currentUser, club)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Вы не являетесь участником клуба"));

        if (member.getUserRole() == ClubRole.MODERATOR || member.getUserRole() == ClubRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Сначала передайте права");
        }

        clubMemberRepository.delete(member);
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
