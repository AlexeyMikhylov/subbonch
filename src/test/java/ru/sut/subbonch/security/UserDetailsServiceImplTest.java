package ru.sut.subbonch.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_whenUserNotFound_throwsUsernameNotFoundException() {
        when(userRepository.findByEmail("missing@sut.ru")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@sut.ru"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void loadUserByUsername_whenBlocked_throwsBlockedUserExceptionWithReason() {
        User user = activeUser("blocked@sut.ru", false);
        user.setBlocked(true);
        user.setBlockReason("spam");
        when(userRepository.findByEmail("blocked@sut.ru")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("blocked@sut.ru"))
                .isInstanceOf(BlockedUserException.class)
                .satisfies(ex -> assertThat(((BlockedUserException) ex).getBlockReason()).isEqualTo("spam"));
    }

    @Test
    void loadUserByUsername_whenInactive_throwsDisabledException() {
        User user = activeUser("inactive@sut.ru", false);
        user.setActive(false);
        when(userRepository.findByEmail("inactive@sut.ru")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("inactive@sut.ru"))
                .isInstanceOf(DisabledException.class)
                .hasMessage("User account is disabled");
    }

    @Test
    void loadUserByUsername_whenActive_returnsUserDetailsWithRoleUser() {
        User user = activeUser("user@sut.ru", false);
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("user@sut.ru");

        assertThat(details.getUsername()).isEqualTo("user@sut.ru");
        assertThat(details.getPassword()).isEqualTo("hashed-password");
        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(UserDetailsServiceImpl.ROLE_USER);
    }

    @Test
    void loadUserByUsername_whenAppAdmin_returnsRoleSiteAdmin() {
        User user = activeUser("admin@sut.ru", true);
        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("admin@sut.ru");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        UserDetailsServiceImpl.ROLE_USER,
                        UserDetailsServiceImpl.ROLE_SITE_ADMIN);
    }

    @Test
    void loadUserByUsername_normalizesEmailBeforeLookup() {
        User user = activeUser("user@sut.ru", false);
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));

        userDetailsService.loadUserByUsername("  User@SUT.ru  ");

        verify(userRepository).findByEmail("user@sut.ru");
    }

    private static User activeUser(String email, boolean appAdmin) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setActive(true);
        user.setBlocked(false);
        user.setAppAdmin(appAdmin);
        return user;
    }
}
