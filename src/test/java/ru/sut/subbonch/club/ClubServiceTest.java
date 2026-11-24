package ru.sut.subbonch.club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.dto.ClubListItemResponse;
import ru.sut.subbonch.dto.ClubPageResponse;
import ru.sut.subbonch.dto.PageResponse;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.ClubRepository;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.service.ClubService;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClubService clubService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCatalog_nullQuery_returnsWholeCatalogPage() {
        ClubListItemResponse item = new ClubListItemResponse(1, "Chess", "About", null, 5L);
        Page<ClubListItemResponse> repoPage = new PageImpl<>(List.of(item), Pageable.ofSize(20).withPage(0), 1);
        when(clubRepository.searchCatalog(eq(null), any(Pageable.class))).thenReturn(repoPage);

        PageResponse<ClubListItemResponse> response = clubService.getCatalog(null, 0, 20);

        assertThat(response.content()).containsExactly(item);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(clubRepository).searchCatalog(eq(null), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void getCatalog_blankQuery_treatedAsWholeCatalog() {
        when(clubRepository.searchCatalog(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20).withPage(0), 0));

        PageResponse<ClubListItemResponse> response = clubService.getCatalog("   ", 0, 20);

        verify(clubRepository).searchCatalog(eq(null), any(Pageable.class));
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
    }

    @Test
    void getCatalog_query_normalizesAndFiltersByPattern() {
        when(clubRepository.searchCatalog(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20).withPage(0), 0));

        clubService.getCatalog("  Chess  ", 0, 20);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(clubRepository).searchCatalog(queryCaptor.capture(), any(Pageable.class));
        assertThat(queryCaptor.getValue()).isEqualTo("%chess%");
    }

    @Test
    void getCatalog_queryEscapesLikeWildcards() {
        when(clubRepository.searchCatalog(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20).withPage(0), 0));

        clubService.getCatalog("100%_a[b\\c", 0, 20);

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(clubRepository).searchCatalog(queryCaptor.capture(), any(Pageable.class));
        assertThat(queryCaptor.getValue()).isEqualTo("%100\\%\\_a\\[b\\\\c%");
    }

    @Test
    void getCatalog_negativePage_clampedToZero() {
        when(clubRepository.searchCatalog(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20).withPage(0), 0));

        clubService.getCatalog(null, -5, 20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(clubRepository).searchCatalog(eq(null), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
    }

    @Test
    void getCatalog_tooLargeSize_clampedToMax() {
        when(clubRepository.searchCatalog(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(50).withPage(0), 0));

        clubService.getCatalog(null, 0, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(clubRepository).searchCatalog(eq(null), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void getCatalog_zeroOrNegativeSize_usesDefault() {
        when(clubRepository.searchCatalog(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20).withPage(0), 0));

        clubService.getCatalog(null, 0, 0);
        ArgumentCaptor<Pageable> first = ArgumentCaptor.forClass(Pageable.class);
        verify(clubRepository).searchCatalog(eq(null), first.capture());
        assertThat(first.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void getCatalog_emptyResult_returnsEmptyPage() {
        when(clubRepository.searchCatalog(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20).withPage(0), 0));

        PageResponse<ClubListItemResponse> response = clubService.getCatalog("missing", 0, 20);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
    }

    @Test
    void getCatalog_usesCatalogSearchQueryThatExcludesBlockedAndInactive() {
        when(clubRepository.searchCatalog(eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20).withPage(0), 0));

        clubService.getCatalog(null, 0, 20);

        // Repository searchCatalog enforces isActive=true and isBlocked=false in JPQL.
        verify(clubRepository).searchCatalog(eq(null), any(Pageable.class));
    }

    @Test
    void getClubPage_existingActiveClub_returnsDetailsAndTeam() {
        Club club = club(10, "Chess", "About", "pic.jpg");
        User adminUser = user(1, "Ivan", "Admin");
        User modUser = user(2, "Petr", "Mod");
        ClubMember adminMember = member(100, adminUser, club, ClubRole.ADMIN);
        ClubMember modMember = member(101, modUser, club, ClubRole.MODERATOR);

        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.countByClub(club)).thenReturn(5L);
        when(clubMemberRepository.findByClubAndUserRoleIn(eq(club), any()))
                .thenReturn(List.of(modMember, adminMember));

        ClubPageResponse response = clubService.getClubPage(10);

        assertThat(response.clubId()).isEqualTo(10);
        assertThat(response.club()).isEqualTo("Chess");
        assertThat(response.about()).isEqualTo("About");
        assertThat(response.pictureUrl()).isEqualTo("pic.jpg");
        assertThat(response.createdAt()).isEqualTo(club.getCreatedAt());
        assertThat(response.membersCount()).isEqualTo(5L);
        assertThat(response.userRoleInClub()).isEqualTo("NONE");
        assertThat(response.team()).hasSize(2);
        assertThat(response.team().get(0).userRole()).isEqualTo(ClubRole.ADMIN);
        assertThat(response.team().get(0).userId()).isEqualTo(1);
        assertThat(response.team().get(0).firstName()).isEqualTo("Ivan");
        assertThat(response.team().get(0).lastName()).isEqualTo("Admin");
        assertThat(response.team().get(1).userRole()).isEqualTo(ClubRole.MODERATOR);
    }

    @Test
    void getClubPage_guest_getsNone() {
        Club club = club(10, "Chess", "About", null);
        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.countByClub(club)).thenReturn(1L);
        when(clubMemberRepository.findByClubAndUserRoleIn(eq(club), any())).thenReturn(List.of());

        ClubPageResponse response = clubService.getClubPage(10);

        assertThat(response.userRoleInClub()).isEqualTo("NONE");
    }

    @Test
    void getClubPage_authenticatedMember_getsMemberRole() {
        Club club = club(10, "Chess", "About", null);
        User current = user(5, "User", "Member");
        current.setEmail("member@sut.ru");
        ClubMember membership = member(200, current, club, ClubRole.MEMBER);
        authenticate("member@sut.ru");

        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(userRepository.findByEmail("member@sut.ru")).thenReturn(Optional.of(current));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.of(membership));
        when(clubMemberRepository.countByClub(club)).thenReturn(2L);
        when(clubMemberRepository.findByClubAndUserRoleIn(eq(club), any())).thenReturn(List.of());

        ClubPageResponse response = clubService.getClubPage(10);

        assertThat(response.userRoleInClub()).isEqualTo("MEMBER");
    }

    @Test
    void getClubPage_authenticatedModerator_getsModeratorRole() {
        Club club = club(10, "Chess", "About", null);
        User current = user(6, "Mod", "User");
        current.setEmail("mod@sut.ru");
        ClubMember membership = member(201, current, club, ClubRole.MODERATOR);
        authenticate("mod@sut.ru");

        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(current));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.of(membership));
        when(clubMemberRepository.countByClub(club)).thenReturn(2L);
        when(clubMemberRepository.findByClubAndUserRoleIn(eq(club), any())).thenReturn(List.of());

        ClubPageResponse response = clubService.getClubPage(10);

        assertThat(response.userRoleInClub()).isEqualTo("MODERATOR");
    }

    @Test
    void getClubPage_authenticatedAdmin_getsAdminRole() {
        Club club = club(10, "Chess", "About", null);
        User current = user(7, "Admin", "User");
        current.setEmail("admin@sut.ru");
        ClubMember membership = member(202, current, club, ClubRole.ADMIN);
        authenticate("admin@sut.ru");

        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(current));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.of(membership));
        when(clubMemberRepository.countByClub(club)).thenReturn(2L);
        when(clubMemberRepository.findByClubAndUserRoleIn(eq(club), any())).thenReturn(List.of());

        ClubPageResponse response = clubService.getClubPage(10);

        assertThat(response.userRoleInClub()).isEqualTo("ADMIN");
    }

    @Test
    void getClubPage_authenticatedNonMember_getsNone() {
        Club club = club(10, "Chess", "About", null);
        User current = user(8, "Other", "User");
        current.setEmail("other@sut.ru");
        authenticate("other@sut.ru");

        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(userRepository.findByEmail("other@sut.ru")).thenReturn(Optional.of(current));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.empty());
        when(clubMemberRepository.countByClub(club)).thenReturn(1L);
        when(clubMemberRepository.findByClubAndUserRoleIn(eq(club), any())).thenReturn(List.of());

        ClubPageResponse response = clubService.getClubPage(10);

        assertThat(response.userRoleInClub()).isEqualTo("NONE");
    }

    @Test
    void getClubPage_nonexistentClub_returns404() {
        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubService.getClubPage(999))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getClubPage_blockedOrInactiveClub_returns404() {
        // Repository query filters by isActive=true and isBlocked=false, so blocked/inactive returns empty.
        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(11)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubService.getClubPage(11))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getClubPage_nullId_returns404() {
        assertThatThrownBy(() -> clubService.getClubPage(null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "pw", List.of()));
    }

    private static Club club(Integer id, String name, String about, String pictureUrl) {
        Club club = new Club();
        club.setClubId(id);
        club.setClub(name);
        club.setAbout(about);
        club.setPictureUrl(pictureUrl);
        club.setActive(true);
        club.setBlocked(false);
        club.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        club.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return club;
    }

    private static User user(Integer id, String firstName, String lastName) {
        User user = new User();
        user.setUserId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(firstName.toLowerCase() + "@sut.ru");
        return user;
    }

    private static ClubMember member(Integer id, User user, Club club, ClubRole role) {
        ClubMember member = new ClubMember();
        member.setId(id);
        member.setUser(user);
        member.setClub(club);
        member.setUserRole(role);
        member.setJoinedAt(LocalDateTime.of(2026, 1, 2, 12, 0));
        return member;
    }
}
