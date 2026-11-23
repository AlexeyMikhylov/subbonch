package ru.sut.subbonch.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.sut.subbonch.controller.AuthController;
import ru.sut.subbonch.dto.RegisterRequest;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void register_success_redirectsToLoginRegistered() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@sut.ru");
        when(authService.register(request)).thenReturn(new User());

        ResponseEntity<?> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/login?registered"));
        verify(authService).register(request);
    }

    @Test
    void register_validationError_returnsBadRequest() {
        RegisterRequest request = new RegisterRequest();
        doThrow(new IllegalArgumentException("Email уже занят")).when(authService).register(request);

        ResponseEntity<?> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Email уже занят"));
    }

    @Test
    void register_duplicateEmailRace_returnsBadRequest() {
        RegisterRequest request = new RegisterRequest();
        doThrow(new DataIntegrityViolationException("duplicate")).when(authService).register(request);

        ResponseEntity<?> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Email уже занят"));
    }
}
