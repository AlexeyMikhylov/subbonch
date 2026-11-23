package ru.sut.subbonch.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.service.UserAdminService;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRegistry sessionRegistry;

    @InjectMocks
    private UserAdminService userAdminService;

    @Test
    void blockUser_setsFlagsAndExpiresSessions() {
        User user = new User();
        user.setUserId(5);
        user.setEmail("user@sut.ru");
        user.setBlocked(false);

        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername("user@sut.ru").password("x").roles("USER").build();
        SessionInformation sessionInfo = mock(SessionInformation.class);

        when(userRepository.findById(5)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(principal));
        when(sessionRegistry.getAllSessions(principal, false)).thenReturn(List.of(sessionInfo));

        User blocked = userAdminService.blockUser(5, "  spam  ");

        assertThat(blocked.isBlocked()).isTrue();
        assertThat(blocked.getBlockReason()).isEqualTo("spam");
        verify(sessionInfo).expireNow();
        verify(userRepository).save(user);
    }

    @Test
    void blockUser_blankReason_throws() {
        assertThatThrownBy(() -> userAdminService.blockUser(5, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Причина блокировки обязательна");
    }

    @Test
    void blockUser_userNotFound_throws() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAdminService.blockUser(99, "reason"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Пользователь не найден");
    }

    @Test
    void unblockUser_clearsFlags() {
        User user = new User();
        user.setUserId(5);
        user.setEmail("user@sut.ru");
        user.setBlocked(true);
        user.setBlockReason("spam");
        when(userRepository.findById(5)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User unblocked = userAdminService.unblockUser(5);

        assertThat(unblocked.isBlocked()).isFalse();
        assertThat(unblocked.getBlockReason()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void blockUser_selfBlock_throws() {
        User user = new User();
        user.setUserId(5);
        user.setEmail("admin@sut.ru");
        when(userRepository.findById(5)).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@sut.ru", "pw", List.of()));

        assertThatThrownBy(() -> userAdminService.blockUser(5, "reason"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Нельзя заблокировать собственный аккаунт");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }
}
