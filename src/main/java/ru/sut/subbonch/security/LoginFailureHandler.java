package ru.sut.subbonch.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * Показывает причину блокировки на {@code /login}; во всех остальных случаях —
 * общее сообщение, чтобы не раскрывать, существует ли email.
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    public static final String SESSION_BLOCK_REASON = "blockReason";
    public static final String SESSION_LOGIN_ERROR = "loginError";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof BlockedUserException blocked) {
            String reason = blocked.getBlockReason();
            request.getSession().setAttribute(SESSION_BLOCK_REASON,
                    reason == null || reason.isBlank() ? "Аккаунт заблокирован" : reason);
        } else {
            request.getSession().setAttribute(SESSION_LOGIN_ERROR,
                    "Неверный email или пароль");
        }
        response.sendRedirect(request.getContextPath() + "/login?error");
    }
}
