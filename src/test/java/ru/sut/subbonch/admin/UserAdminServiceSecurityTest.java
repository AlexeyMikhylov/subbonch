package ru.sut.subbonch.admin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.sut.subbonch.service.UserAdminService;

@SpringBootTest
class UserAdminServiceSecurityTest {

    @Autowired
    private UserAdminService userAdminService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blockUser_withoutSiteAdminRole_throwsAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user@sut.ru", "pw", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThatThrownBy(() -> userAdminService.blockUser(1, "reason"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unblockUser_withoutSiteAdminRole_throwsAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user@sut.ru", "pw", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThatThrownBy(() -> userAdminService.unblockUser(1))
                .isInstanceOf(AccessDeniedException.class);
    }
}
