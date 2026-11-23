package ru.sut.subbonch.service;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.UserRepository;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final SessionRegistry sessionRegistry;

    public UserAdminService(UserRepository userRepository, SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @PreAuthorize("hasRole('SITE_ADMIN')")
    @Transactional
    public User blockUser(Integer userId, String blockReason) {
        if (userId == null) {
            throw new IllegalArgumentException("userId обязателен");
        }
        String reason = trimToNull(blockReason);
        if (reason == null) {
            throw new IllegalArgumentException("Причина блокировки обязательна");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (isCurrentUser(user)) {
            throw new IllegalArgumentException("Нельзя заблокировать собственный аккаунт");
        }
        user.setBlocked(true);
        user.setBlockReason(reason);
        User saved = userRepository.save(user);
        expireSessions(saved.getEmail());
        return saved;
    }

    @PreAuthorize("hasRole('SITE_ADMIN')")
    @Transactional
    public User unblockUser(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId обязателен");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        user.setBlocked(false);
        user.setBlockReason(null);
        return userRepository.save(user);
    }

    private void expireSessions(String email) {
        if (email == null) {
            return;
        }
        String normalizedEmail = email.trim().toLowerCase();
        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if (principal instanceof UserDetails details
                    && normalizedEmail.equalsIgnoreCase(details.getUsername())) {
                for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                    session.expireNow();
                }
            }
        }
    }

    private boolean isCurrentUser(User user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Object principal = auth.getPrincipal();
        String email = principal instanceof UserDetails details ? details.getUsername() : String.valueOf(principal);
        return user.getEmail() != null && user.getEmail().equalsIgnoreCase(email);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
