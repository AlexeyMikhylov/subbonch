package ru.sut.subbonch.security;

import java.util.List;
import java.util.Locale;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.UserRepository;

/**
 * Загружает пользователя по email (он же логин). Админ сайта
 * ({@code is_app_admin}) получает authority {@code ROLE_SITE_ADMIN};
 * роль внутри клуба здесь не используется — она проверяется сервисами.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_SITE_ADMIN = "ROLE_SITE_ADMIN";

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.isBlocked()) {
            throw new BlockedUserException(user.getBlockReason());
        }
        if (!user.isActive()) {
            throw new DisabledException("User account is disabled");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(authorities(user))
                .build();
    }

    /** Полномочия пользователя: базовая роль и, при флаге, админ сайта. */
    public static List<GrantedAuthority> authorities(User user) {
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(ROLE_USER));
        if (user.isAppAdmin()) {
            authorities.add(new SimpleGrantedAuthority(ROLE_SITE_ADMIN));
        }
        return authorities;
    }
}
