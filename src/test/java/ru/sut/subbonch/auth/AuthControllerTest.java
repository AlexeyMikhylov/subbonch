package ru.sut.subbonch.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
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
        Model model = new ExtendedModelMap();

        String view = authController.register(request, model);

        assertThat(view).isEqualTo("redirect:/login?registered");
        verify(authService).register(request);
    }

    @Test
    void register_validationError_showsRegisterFormWithMessage() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@sut.ru");
        request.setFirstName("Иван");
        doThrow(new IllegalArgumentException("Пароли не совпадают")).when(authService).register(request);
        Model model = new ExtendedModelMap();

        String view = authController.register(request, model);

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Пароли не совпадают");
        assertThat(model.getAttribute("email")).isEqualTo("user@sut.ru");
        assertThat(model.getAttribute("firstName")).isEqualTo("Иван");
    }

    @Test
    void register_duplicateEmailRace_showsRegisterFormWithMessage() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@sut.ru");
        doThrow(new DataIntegrityViolationException("duplicate")).when(authService).register(request);
        Model model = new ExtendedModelMap();

        String view = authController.register(request, model);

        assertThat(view).isEqualTo("register");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("Email уже занят");
    }
}
