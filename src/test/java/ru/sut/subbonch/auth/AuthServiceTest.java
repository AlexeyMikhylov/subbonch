package ru.sut.subbonch.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.sut.subbonch.dto.RegisterRequest;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_validRequest_normalizesEmailEncodesPasswordAndSavesUser() {
        RegisterRequest request = request("  User@SUT.ru  ", "Ivan", "Petrov", "secret123", "secret123");
        when(userRepository.existsByEmail("user@sut.ru")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User toSave = captor.getValue();

        assertThat(toSave.getEmail()).isEqualTo("user@sut.ru");
        assertThat(toSave.getFirstName()).isEqualTo("Ivan");
        assertThat(toSave.getLastName()).isEqualTo("Petrov");
        assertThat(toSave.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getPasswordHash()).isEqualTo("bcrypt-hash");
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    void register_duplicateEmail_throwsIllegalArgumentException() {
        RegisterRequest request = request("user@sut.ru", "Ivan", "Petrov", "secret123", "secret123");
        when(userRepository.existsByEmail("user@sut.ru")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email уже занят");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_passwordMismatch_throwsIllegalArgumentException() {
        RegisterRequest request = request("user@sut.ru", "Ivan", "Petrov", "secret123", "other");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пароли не совпадают");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_blankEmail_throwsIllegalArgumentException() {
        RegisterRequest request = request("  ", "Ivan", "Petrov", "secret123", "secret123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Некорректный email");
    }

    @Test
    void register_blankFirstName_throwsIllegalArgumentException() {
        RegisterRequest request = request("user@sut.ru", " ", "Petrov", "secret123", "secret123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Имя обязательно");
    }

    @Test
    void register_blankLastName_throwsIllegalArgumentException() {
        RegisterRequest request = request("user@sut.ru", "Ivan", " ", "secret123", "secret123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Фамилия обязательна");
    }

    @Test
    void register_blankPassword_throwsIllegalArgumentException() {
        RegisterRequest request = request("user@sut.ru", "Ivan", "Petrov", " ", " ");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пароль обязателен");
    }

    @Test
    void register_shortPassword_throwsIllegalArgumentException() {
        RegisterRequest request = request("user@sut.ru", "Ivan", "Petrov", "short", "short");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пароль должен содержать не менее 8 символов");
    }

    @Test
    void register_tooLongEmail_throwsIllegalArgumentException() {
        String longEmail = "a".repeat(250) + "@sut.ru";
        RegisterRequest request = request(longEmail, "Ivan", "Petrov", "secret123", "secret123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email слишком длинный");
    }

    @Test
    void register_tooLongFirstName_throwsIllegalArgumentException() {
        RegisterRequest request = request("user@sut.ru", "a".repeat(101), "Petrov", "secret123", "secret123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Имя слишком длинное");
    }

    private static RegisterRequest request(String email, String firstName, String lastName,
                                           String password, String passwordConfirm) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setPassword(password);
        request.setPasswordConfirm(passwordConfirm);
        return request;
    }
}
