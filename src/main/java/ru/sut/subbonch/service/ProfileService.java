package ru.sut.subbonch.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sut.subbonch.dto.ProfileEditRequest;
import ru.sut.subbonch.dto.ProfileResponse;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.CommentRepository;
import ru.sut.subbonch.repository.UserRepository;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final CommentRepository commentRepository;

    public ProfileService(UserRepository userRepository,
                          ClubMemberRepository clubMemberRepository,
                          CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        User user = getCurrentUser();
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfileForEdit() {
        return getMyProfile();
    }

    @Transactional
    public ProfileResponse updateMyProfile(ProfileEditRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Запрос редактирования обязателен");
        }
        User user = getCurrentUser();

        String firstName = trimToNull(request.getFirstName());
        String lastName = trimToNull(request.getLastName());
        String about = trimToNull(request.getAbout());
        String profilePictureUrl = trimToNull(request.getProfilePictureUrl());
        if (firstName == null) {
            throw new IllegalArgumentException("Имя обязательно");
        }
        if (firstName.length() > 100) {
            throw new IllegalArgumentException("Имя слишком длинное");
        }
        if (lastName == null) {
            throw new IllegalArgumentException("Фамилия обязательна");
        }
        if (lastName.length() > 100) {
            throw new IllegalArgumentException("Фамилия слишком длинная");
        }
        if (about != null && about.length() > 255) {
            throw new IllegalArgumentException("Поле «О себе» слишком длинное");
        }
        if (profilePictureUrl != null && profilePictureUrl.length() > 500) {
            throw new IllegalArgumentException("URL фото слишком длинный");
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAbout(about);
        user.setProfilePictureUrl(profilePictureUrl);

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("Пользователь не аутентифицирован");
        }
        Object principal = auth.getPrincipal();
        String email;
        if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        } else {
            email = String.valueOf(principal);
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
    }

    private ProfileResponse toResponse(User user) {
        long clubsCount = clubMemberRepository.countByUser(user);
        long commentsCount = commentRepository.countByUserAndIsDeletedFalse(user);
        return new ProfileResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAbout(),
                user.getProfilePictureUrl(),
                user.getCreatedAt(),
                clubsCount,
                commentsCount
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
