package ru.sut.subbonch.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class LoginFailureHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    private LoginFailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LoginFailureHandler();
        when(request.getSession()).thenReturn(session);
        when(request.getContextPath()).thenReturn("");
    }

    @Test
    void onAuthenticationFailure_whenBlocked_setsBlockReasonAndRedirects() throws Exception {
        handler.onAuthenticationFailure(request, response, new BlockedUserException("Нарушение правил"));

        verify(session).setAttribute(LoginFailureHandler.SESSION_BLOCK_REASON, "Нарушение правил");
        verify(response).sendRedirect("/login?error");
    }

    @Test
    void onAuthenticationFailure_whenBlockedWithoutReason_usesDefaultMessage() throws Exception {
        handler.onAuthenticationFailure(request, response, new BlockedUserException(" "));

        verify(session).setAttribute(LoginFailureHandler.SESSION_BLOCK_REASON, "Аккаунт заблокирован");
        verify(response).sendRedirect("/login?error");
    }

    @Test
    void onAuthenticationFailure_whenBadCredentials_setsGenericError() throws Exception {
        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

        verify(session).setAttribute(LoginFailureHandler.SESSION_LOGIN_ERROR, "Неверный email или пароль");
        verify(response).sendRedirect("/login?error");
    }
}
