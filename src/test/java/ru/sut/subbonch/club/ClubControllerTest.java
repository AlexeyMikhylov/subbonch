package ru.sut.subbonch.club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import ru.sut.subbonch.controller.ClubController;
import ru.sut.subbonch.service.ClubMembershipService;
import ru.sut.subbonch.service.ClubService;
import ru.sut.subbonch.service.PostService;

@ExtendWith(MockitoExtension.class)
class ClubControllerTest {

    @Mock
    private ClubService clubService;

    @Mock
    private ClubMembershipService clubMembershipService;

    @Mock
    private PostService postService;

    @InjectMocks
    private ClubController clubController;

    @Test
    void join_returnsToCatalogWithFilters() {
        String view = clubController.joinClub(
                3, "/clubs?q=chess&page=1&size=20", null, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/clubs?q=chess&page=1&size=20");
    }

    @Test
    void leave_returnsToClubPageWhenNoReturnUrl() {
        String view = clubController.leaveClub(
                7, null, "http://localhost:8080/clubs/7", new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/clubs/7");
    }

    @Test
    void join_externalReturnUrl_staysOnOwnHost() {
        String view = clubController.joinClub(
                3, "https://evil.example/phish", null, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/phish");
    }

    @Test
    void join_protocolRelativeReturnUrl_fallsBackToClubPage() {
        String view = clubController.joinClub(
                3, "//evil.example/phish", null, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/clubs/3");
    }

    @Test
    void join_backslashReturnUrl_fallsBackToClubPage() {
        String view = clubController.joinClub(
                3, "/\\evil.example/phish", null, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/clubs/3");
    }

    @Test
    void join_invalidReturnButValidReferer_usesReferer() {
        String view = clubController.joinClub(
                3, "//evil.example", "http://localhost:8080/clubs?page=2", new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/clubs?page=2");
    }

    @Test
    void join_membershipError_stillRedirectsBack() {
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "Вы уже являетесь участником клуба"))
                .when(clubMembershipService).joinClub(3);

        String view = clubController.joinClub(
                3, "/clubs", null, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/clubs");
    }

    @Test
    void join_membershipError_whenNoReturnInfo_redirectsToClub() {
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "Вы уже являетесь участником клуба"))
                .when(clubMembershipService).joinClub(3);

        String view = clubController.joinClub(3, null, null, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/clubs/3");
    }

    @Test
    void catalog_doesNotRedirect() {
        org.mockito.Mockito.when(clubService.getCatalog(org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(new ru.sut.subbonch.dto.PageResponse<>(
                        java.util.List.of(), 0, 20, 0, 0));

        String view = clubController.catalog(null, 0, 20, new org.springframework.ui.ExtendedModelMap());

        assertThat(view).isEqualTo("catalog");
        verifyNoInteractions(clubMembershipService);
    }
}
