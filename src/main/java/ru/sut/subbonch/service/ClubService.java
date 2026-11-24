package ru.sut.subbonch.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.dto.ClubListItemResponse;
import ru.sut.subbonch.dto.ClubPageResponse;
import ru.sut.subbonch.dto.ClubTeamMemberResponse;
import ru.sut.subbonch.dto.PageResponse;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.ClubRepository;
import ru.sut.subbonch.repository.UserRepository;

@Service
public class ClubService {

    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 50;

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;

    public ClubService(ClubRepository clubRepository,
                       ClubMemberRepository clubMemberRepository,
                       UserRepository userRepository) {
        this.clubRepository = clubRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClubListItemResponse> getCatalog(String q, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = normalizeSize(size);
        String searchPattern = normalizeQuery(q);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "clubId"));
        Page<ClubListItemResponse> result = clubRepository.searchCatalog(searchPattern, pageable);

        return new PageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ClubPageResponse getClubPage(Integer clubId) {
        if (clubId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Клуб не найден");
        }
        Club club = clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(clubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Клуб не найден"));

        User currentUser = getCurrentUserOrNull();
        String userRoleInClub = "NONE";
        if (currentUser != null) {
            userRoleInClub = clubMemberRepository.findByUserAndClub(currentUser, club)
                    .map(member -> member.getUserRole().name())
                    .orElse("NONE");
        }

        long membersCount = clubMemberRepository.countByClub(club);
        List<ClubTeamMemberResponse> team = clubMemberRepository
                .findByClubAndUserRoleIn(club, List.of(ClubRole.ADMIN, ClubRole.MODERATOR))
                .stream()
                .sorted(Comparator
                        .comparing((ClubMember m) -> m.getUserRole() != ClubRole.ADMIN)
                        .thenComparing(m -> m.getUser().getLastName(), Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(m -> m.getUser().getFirstName(), Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(ClubMember::getId, Comparator.nullsLast(Integer::compareTo)))
                .map(this::toTeamMemberResponse)
                .toList();

        return new ClubPageResponse(
                club.getClubId(),
                club.getClub(),
                club.getAbout(),
                club.getPictureUrl(),
                club.getCreatedAt(),
                membersCount,
                userRoleInClub,
                team
        );
    }

    private ClubTeamMemberResponse toTeamMemberResponse(ClubMember member) {
        User user = member.getUser();
        return new ClubTeamMemberResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                member.getUserRole()
        );
    }

    private User getCurrentUserOrNull() {
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

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private static String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String escaped = trimmed.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .replace("[", "\\[");
        return "%" + escaped + "%";
    }
}
