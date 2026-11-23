package ru.sut.subbonch.service;

import org.springframework.dao.DataIntegrityViolationException;
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
import ru.sut.subbonch.dto.ClubEditRequest;
import ru.sut.subbonch.dto.ClubEditResponse;
import ru.sut.subbonch.dto.ClubMemberListItemResponse;
import ru.sut.subbonch.dto.PageResponse;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.ClubRepository;
import ru.sut.subbonch.repository.UserRepository;

@Service
public class ClubManagementService {

    static final int DEFAULT_SIZE = 20;
    static final int MAX_SIZE = 50;

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;

    public ClubManagementService(ClubRepository clubRepository,
                                 ClubMemberRepository clubMemberRepository,
                                 UserRepository userRepository) {
        this.clubRepository = clubRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClubMemberListItemResponse> getMembers(Integer clubId, int page, int size) {
        User currentUser = getCurrentUserOrThrow();
        Club club = getClubOrThrow(clubId);
        assertCanManageClub(currentUser, club);

        int safePage = Math.max(page, 0);
        int safeSize = normalizeSize(size);
        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.ASC, "joinedAt").and(Sort.by(Sort.Direction.ASC, "id"))
        );

        Page<ClubMember> members = clubMemberRepository.findByClub(club, pageable);
        Page<ClubMemberListItemResponse> mapped = members.map(this::toResponse);

        return new PageResponse<>(
                mapped.getContent(),
                mapped.getNumber(),
                mapped.getSize(),
                mapped.getTotalElements(),
                mapped.getTotalPages()
        );
    }

    @Transactional
    public ClubMemberListItemResponse assignModerator(Integer clubId, Integer targetUserId) {
        User currentUser = getCurrentUserOrThrow();
        Club club = getClubOrThrow(clubId);
        assertCanManageClub(currentUser, club);
        ClubMember targetMembership = getTargetMembershipOrThrow(club, targetUserId);

        if (targetMembership.getUserRole() == ClubRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя изменить роль администратора клуба");
        }
        if (targetMembership.getUserRole() != ClubRole.MEMBER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Назначить модератора можно только участнику с ролью MEMBER");
        }

        targetMembership.setUserRole(ClubRole.MODERATOR);
        ClubMember saved = clubMemberRepository.save(targetMembership);
        return toResponse(saved);
    }

    @Transactional
    public ClubMemberListItemResponse removeModerator(Integer clubId, Integer targetUserId) {
        User currentUser = getCurrentUserOrThrow();
        Club club = getClubOrThrow(clubId);
        assertCanManageClub(currentUser, club);
        ClubMember targetMembership = getTargetMembershipOrThrow(club, targetUserId);

        if (targetMembership.getUserRole() == ClubRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нельзя изменить роль администратора клуба");
        }
        if (targetMembership.getUserRole() != ClubRole.MODERATOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Снять модератора можно только с пользователя с ролью MODERATOR");
        }

        targetMembership.setUserRole(ClubRole.MEMBER);
        ClubMember saved = clubMemberRepository.save(targetMembership);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ClubEditResponse getClubForEdit(Integer clubId) {
        User currentUser = getCurrentUserOrThrow();
        Club club = getClubOrThrow(clubId);
        assertCanManageClub(currentUser, club);
        return toEditResponse(club);
    }

    @Transactional
    public ClubEditResponse updateClub(Integer clubId, ClubEditRequest request) {
        User currentUser = getCurrentUserOrThrow();
        Club club = getClubOrThrow(clubId);
        assertCanManageClub(currentUser, club);

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Запрос на редактирование клуба не заполнен");
        }

        String normalizedName = normalizeRequired(request.getClub());
        String about = normalizeOptional(request.getAbout());
        String pictureUrl = normalizeOptional(request.getPictureUrl());
        validateEditValues(normalizedName, about, pictureUrl);

        if (!normalizedName.equalsIgnoreCase(club.getClub())
                && clubRepository.existsByClubIgnoreCaseAndClubIdNot(normalizedName, club.getClubId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Клуб с таким названием уже существует");
        }

        club.setClub(normalizedName);
        club.setAbout(about);
        club.setPictureUrl(pictureUrl);

        try {
            Club saved = clubRepository.save(club);
            return toEditResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Клуб с таким названием уже существует", ex);
        }
    }

    private Club getClubOrThrow(Integer clubId) {
        if (clubId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Клуб не найден");
        }
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Клуб не найден"));
    }

    private ClubMember getTargetMembershipOrThrow(Club club, Integer targetUserId) {
        if (targetUserId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Участник клуба не найден");
        }
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        return clubMemberRepository.findByUserAndClub(targetUser, club)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Участник клуба не найден"));
    }

    private void assertCanManageClub(User currentUser, Club club) {
        if (currentUser.isAppAdmin()) {
            return;
        }
        ClubMember membership = clubMemberRepository.findByUserAndClub(currentUser, club)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Недостаточно прав"));
        if (membership.getUserRole() != ClubRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Недостаточно прав");
        }
    }

    private ClubMemberListItemResponse toResponse(ClubMember member) {
        User user = member.getUser();
        return new ClubMemberListItemResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                member.getUserRole(),
                member.getJoinedAt()
        );
    }

    private ClubEditResponse toEditResponse(Club club) {
        return new ClubEditResponse(
                club.getClubId(),
                club.getClub(),
                club.getAbout(),
                club.getPictureUrl(),
                club.getUpdatedAt()
        );
    }

    private void validateEditValues(String normalizedName, String about, String pictureUrl) {
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название клуба не может быть пустым");
        }
        if (normalizedName.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название клуба не должно превышать 100 символов");
        }
        if (about != null && about.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Описание клуба не должно превышать 255 символов");
        }
        if (pictureUrl != null && pictureUrl.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ссылка на изображение не должна превышать 255 символов");
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

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
