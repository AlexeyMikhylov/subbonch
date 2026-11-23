package ru.sut.subbonch.service;

import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sut.subbonch.dto.RegisterRequest;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Запрос регистрации обязателен");
        }

        String email = normalizeEmail(request.getEmail());
        String firstName = trimToNull(request.getFirstName());
        String lastName = trimToNull(request.getLastName());
        String password = request.getPassword();
        String passwordConfirm = request.getPasswordConfirm();

        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Некорректный email");
        }
        if (email.length() > 255) {
            throw new IllegalArgumentException("Email слишком длинный");
        }
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
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Пароль обязателен");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Пароль должен содержать не менее 8 символов");
        }
        if (password.length() > 72) {
            throw new IllegalArgumentException("Пароль слишком длинный");
        }
        if (!password.equals(passwordConfirm)) {
            throw new IllegalArgumentException("Пароли не совпадают");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email уже занят");
        }

        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    private static String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
