package ru.sut.subbonch.club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.dto.ClubEditRequest;
import ru.sut.subbonch.dto.ClubEditResponse;
import ru.sut.subbonch.dto.ClubMemberListItemResponse;
import ru.sut.subbonch.dto.PageResponse;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.ClubRepository;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.service.ClubManagementService;

@ExtendWith(MockitoExtension.class)
class ClubManagementServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClubManagementService clubManagementService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMembers_clubAdmin_returnsMembersPage() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", false);
        User memberUser = user(2, "member@sut.ru", "Ivan", "Petrov", false);
        Club club = club(10);
        ClubMember adminMembership = member(100, admin, club, ClubRole.ADMIN, LocalDateTime.of(2026, 1, 1, 10, 0));
        ClubMember targetMembership = member(101, memberUser, club, ClubRole.MEMBER, LocalDateTime.of(2026, 1, 2, 10, 0));
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(admin, club)).thenReturn(Optional.of(adminMembership));
        when(clubMemberRepository.findByClub(eq(club), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(adminMembership, targetMembership), Pageable.ofSize(20).withPage(0), 2));

        PageResponse<ClubMemberListItemResponse> response = clubManagementService.getMembers(10, 0, 20);

        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.content()).hasSize(2);
        ClubMemberListItemResponse first = response.content().get(1);
        assertThat(first.userId()).isEqualTo(2);
        assertThat(first.firstName()).isEqualTo("Ivan");
        assertThat(first.lastName()).isEqualTo("Petrov");
        assertThat(first.email()).isEqualTo("member@sut.ru");
        assertThat(first.userRole()).isEqualTo(ClubRole.MEMBER);
        assertThat(first.joinedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 10, 0));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(clubMemberRepository).findByClub(eq(club), pageableCaptor.capture());
        Sort sort = pageableCaptor.getValue().getSort();
        assertThat(sort.getOrderFor("joinedAt")).isNotNull();
        assertThat(sort.getOrderFor("joinedAt").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(sort.getOrderFor("id")).isNotNull();
    }

    @Test
    void getMembers_siteAdmin_returnsMembersPage() {
        User siteAdmin = user(1, "site@sut.ru", "Site", "Admin", true);
        User memberUser = user(2, "member@sut.ru", "Ivan", "Petrov", false);
        Club club = club(10);
        ClubMember targetMembership = member(101, memberUser, club, ClubRole.MEMBER, LocalDateTime.of(2026, 1, 2, 10, 0));
        authenticate("site@sut.ru");

        when(userRepository.findByEmail("site@sut.ru")).thenReturn(Optional.of(siteAdmin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByClub(eq(club), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(targetMembership), Pageable.ofSize(20).withPage(0), 1));

        PageResponse<ClubMemberListItemResponse> response = clubManagementService.getMembers(10, 0, 20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).userId()).isEqualTo(2);
        verify(clubMemberRepository, never()).findByUserAndClub(siteAdmin, club);
    }

    @Test
    void getMembers_memberRole_returnsForbidden() {
        User current = user(1, "member@sut.ru", "M", "U", false);
        Club club = club(10);
        ClubMember membership = member(100, current, club, ClubRole.MEMBER, LocalDateTime.now());
        authenticate("member@sut.ru");

        when(userRepository.findByEmail("member@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> clubManagementService.getMembers(10, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(clubMemberRepository, never()).findByClub(eq(club), any(Pageable.class));
    }

    @Test
    void getMembers_moderatorRole_returnsForbidden() {
        User current = user(1, "mod@sut.ru", "M", "U", false);
        Club club = club(10);
        ClubMember membership = member(100, current, club, ClubRole.MODERATOR, LocalDateTime.now());
        authenticate("mod@sut.ru");

        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> clubManagementService.getMembers(10, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getMembers_nonMember_returnsForbidden() {
        User current = user(1, "user@sut.ru", "M", "U", false);
        Club club = club(10);
        authenticate("user@sut.ru");

        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubManagementService.getMembers(10, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getMembers_unauthenticated_returnsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> clubManagementService.getMembers(10, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(clubRepository, never()).findById(any());
    }

    @Test
    void getMembers_anonymousPrincipal_returnsUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThatThrownBy(() -> clubManagementService.getMembers(10, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void getMembers_clubNotFound_returnsNotFound() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", true);
        authenticate("admin@sut.ru");
        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubManagementService.getMembers(999, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getMembers_paginationClamped() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", true);
        Club club = club(10);
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByClub(eq(club), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(50).withPage(0), 0));

        clubManagementService.getMembers(10, -1, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(clubMemberRepository).findByClub(eq(club), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void getMembers_zeroOrNegativeSize_usesDefault() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", true);
        Club club = club(10);
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByClub(eq(club), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(20).withPage(0), 0));

        clubManagementService.getMembers(10, 0, 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(clubMemberRepository).findByClub(eq(club), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void assignModerator_memberByClubAdmin_success() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", false);
        User target = user(2, "member@sut.ru", "Ivan", "Petrov", false);
        Club club = club(10);
        ClubMember adminMembership = member(100, admin, club, ClubRole.ADMIN, LocalDateTime.now());
        ClubMember targetMembership = member(101, target, club, ClubRole.MEMBER, LocalDateTime.now());
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(admin, club)).thenReturn(Optional.of(adminMembership));
        when(userRepository.findById(2)).thenReturn(Optional.of(target));
        when(clubMemberRepository.findByUserAndClub(target, club)).thenReturn(Optional.of(targetMembership));
        when(clubMemberRepository.save(any(ClubMember.class))).thenAnswer(inv -> inv.getArgument(0));

        ClubMemberListItemResponse response = clubManagementService.assignModerator(10, 2);

        assertThat(response.userId()).isEqualTo(2);
        assertThat(response.userRole()).isEqualTo(ClubRole.MODERATOR);
        assertThat(targetMembership.getUserRole()).isEqualTo(ClubRole.MODERATOR);
        verify(clubMemberRepository).save(targetMembership);
    }

    @Test
    void removeModerator_moderatorByClubAdmin_success() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", false);
        User target = user(2, "mod@sut.ru", "Ivan", "Petrov", false);
        Club club = club(10);
        ClubMember adminMembership = member(100, admin, club, ClubRole.ADMIN, LocalDateTime.now());
        ClubMember targetMembership = member(101, target, club, ClubRole.MODERATOR, LocalDateTime.now());
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(admin, club)).thenReturn(Optional.of(adminMembership));
        when(userRepository.findById(2)).thenReturn(Optional.of(target));
        when(clubMemberRepository.findByUserAndClub(target, club)).thenReturn(Optional.of(targetMembership));
        when(clubMemberRepository.save(any(ClubMember.class))).thenAnswer(inv -> inv.getArgument(0));

        ClubMemberListItemResponse response = clubManagementService.removeModerator(10, 2);

        assertThat(response.userId()).isEqualTo(2);
        assertThat(response.userRole()).isEqualTo(ClubRole.MEMBER);
        assertThat(targetMembership.getUserRole()).isEqualTo(ClubRole.MEMBER);
        verify(clubMemberRepository).save(targetMembership);
    }

    @Test
    void assignModerator_siteAdmin_success() {
        User siteAdmin = user(1, "site@sut.ru", "Site", "Admin", true);
        User target = user(2, "member@sut.ru", "Ivan", "Petrov", false);
        Club club = club(10);
        ClubMember targetMembership = member(101, target, club, ClubRole.MEMBER, LocalDateTime.now());
        authenticate("site@sut.ru");

        when(userRepository.findByEmail("site@sut.ru")).thenReturn(Optional.of(siteAdmin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(userRepository.findById(2)).thenReturn(Optional.of(target));
        when(clubMemberRepository.findByUserAndClub(target, club)).thenReturn(Optional.of(targetMembership));
        when(clubMemberRepository.save(any(ClubMember.class))).thenAnswer(inv -> inv.getArgument(0));

        ClubMemberListItemResponse response = clubManagementService.assignModerator(10, 2);

        assertThat(response.userRole()).isEqualTo(ClubRole.MODERATOR);
        verify(clubMemberRepository, never()).findByUserAndClub(siteAdmin, club);
    }

    @Test
    void assignModerator_nonMember_returnsNotFound() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", false);
        User target = user(2, "user@sut.ru", "Ivan", "Petrov", false);
        Club club = club(10);
        ClubMember adminMembership = member(100, admin, club, ClubRole.ADMIN, LocalDateTime.now());
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(admin, club)).thenReturn(Optional.of(adminMembership));
        when(userRepository.findById(2)).thenReturn(Optional.of(target));
        when(clubMemberRepository.findByUserAndClub(target, club)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubManagementService.assignModerator(10, 2))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(clubMemberRepository, never()).save(any(ClubMember.class));
    }

    @Test
    void assignModerator_alreadyModerator_returnsBadRequest() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", false);
        User target = user(2, "mod@sut.ru", "Ivan", "Petrov", false);
        Club club = club(10);
        ClubMember adminMembership = member(100, admin, club, ClubRole.ADMIN, LocalDateTime.now());
        ClubMember targetMembership = member(101, target, club, ClubRole.MODERATOR, LocalDateTime.now());
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(admin, club)).thenReturn(Optional.of(adminMembership));
        when(userRepository.findById(2)).thenReturn(Optional.of(target));
        when(clubMemberRepository.findByUserAndClub(target, club)).thenReturn(Optional.of(targetMembership));

        assertThatThrownBy(() -> clubManagementService.assignModerator(10, 2))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(clubMemberRepository, never()).save(any(ClubMember.class));
    }

    @Test
    void assignModerator_targetIsAdmin_returnsBadRequest() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", false);
        User target = user(2, "admin2@sut.ru", "Other", "Admin", false);
        Club club = club(10);
        ClubMember adminMembership = member(100, admin, club, ClubRole.ADMIN, LocalDateTime.now());
        ClubMember targetMembership = member(101, target, club, ClubRole.ADMIN, LocalDateTime.now());
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(admin, club)).thenReturn(Optional.of(adminMembership));
        when(userRepository.findById(2)).thenReturn(Optional.of(target));
        when(clubMemberRepository.findByUserAndClub(target, club)).thenReturn(Optional.of(targetMembership));

        assertThatThrownBy(() -> clubManagementService.assignModerator(10, 2))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(clubMemberRepository, never()).save(any(ClubMember.class));
    }

    @Test
    void removeModerator_nonModerator_returnsBadRequest() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", false);
        User target = user(2, "member@sut.ru", "Ivan", "Petrov", false);
        Club club = club(10);
        ClubMember adminMembership = member(100, admin, club, ClubRole.ADMIN, LocalDateTime.now());
        ClubMember targetMembership = member(101, target, club, ClubRole.MEMBER, LocalDateTime.now());
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(admin, club)).thenReturn(Optional.of(adminMembership));
        when(userRepository.findById(2)).thenReturn(Optional.of(target));
        when(clubMemberRepository.findByUserAndClub(target, club)).thenReturn(Optional.of(targetMembership));

        assertThatThrownBy(() -> clubManagementService.removeModerator(10, 2))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(clubMemberRepository, never()).save(any(ClubMember.class));
    }

    @Test
    void removeModerator_targetIsAdmin_returnsBadRequest() {
        User siteAdmin = user(1, "site@sut.ru", "Site", "Admin", true);
        User target = user(2, "admin2@sut.ru", "Other", "Admin", false);
        Club club = club(10);
        ClubMember targetMembership = member(101, target, club, ClubRole.ADMIN, LocalDateTime.now());
        authenticate("site@sut.ru");

        when(userRepository.findByEmail("site@sut.ru")).thenReturn(Optional.of(siteAdmin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(userRepository.findById(2)).thenReturn(Optional.of(target));
        when(clubMemberRepository.findByUserAndClub(target, club)).thenReturn(Optional.of(targetMembership));

        assertThatThrownBy(() -> clubManagementService.removeModerator(10, 2))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(clubMemberRepository, never()).save(any(ClubMember.class));
    }

    @Test
    void assignModerator_nonAdminCaller_returnsForbidden() {
        User current = user(1, "member@sut.ru", "M", "U", false);
        Club club = club(10);
        ClubMember membership = member(100, current, club, ClubRole.MEMBER, LocalDateTime.now());
        authenticate("member@sut.ru");

        when(userRepository.findByEmail("member@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> clubManagementService.assignModerator(10, 2))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userRepository, never()).findById(2);
        verify(clubMemberRepository, never()).save(any(ClubMember.class));
    }

    @Test
    void removeModerator_nonAdminCaller_returnsForbidden() {
        User current = user(1, "mod@sut.ru", "M", "U", false);
        Club club = club(10);
        ClubMember membership = member(100, current, club, ClubRole.MODERATOR, LocalDateTime.now());
        authenticate("mod@sut.ru");

        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> clubManagementService.removeModerator(10, 2))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userRepository, never()).findById(2);
        verify(clubMemberRepository, never()).save(any(ClubMember.class));
    }

    @Test
    void assignModerator_clubNotFound_returnsNotFound() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", true);
        authenticate("admin@sut.ru");
        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubManagementService.assignModerator(999, 2))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void assignModerator_targetUserNotFound_returnsNotFound() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", true);
        Club club = club(10);
        authenticate("admin@sut.ru");
        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(userRepository.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubManagementService.assignModerator(10, 2))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(clubMemberRepository, never()).save(any(ClubMember.class));
    }

    @Test
    void getClubForEdit_clubAdmin_returnsClubData() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", false);
        Club club = club(10);
        ClubMember adminMembership = member(100, admin, club, ClubRole.ADMIN, LocalDateTime.now());
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(admin, club)).thenReturn(Optional.of(adminMembership));

        ClubEditResponse response = clubManagementService.getClubForEdit(10);

        assertThat(response.clubId()).isEqualTo(10);
        assertThat(response.club()).isEqualTo("Club 10");
        assertThat(response.about()).isNull();
        assertThat(response.pictureUrl()).isNull();
        assertThat(response.updatedAt()).isEqualTo(club.getUpdatedAt());
    }

    @Test
    void updateClub_clubAdmin_updatesEditableFields() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", false);
        User creator = user(5, "creator@sut.ru", "Creator", "User", false);
        Club club = club(10);
        club.setCreatedBy(creator);
        club.setAbout("old");
        club.setPictureUrl("old.png");
        LocalDateTime createdAt = club.getCreatedAt();
        ClubMember adminMembership = member(100, admin, club, ClubRole.ADMIN, LocalDateTime.now());
        ClubEditRequest request = editRequest("New name", "New about", "new.png");
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(admin, club)).thenReturn(Optional.of(adminMembership));
        when(clubRepository.existsByClubIgnoreCaseAndClubIdNot("New name", 10)).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));

        ClubEditResponse response = clubManagementService.updateClub(10, request);

        assertThat(response.clubId()).isEqualTo(10);
        assertThat(response.club()).isEqualTo("New name");
        assertThat(response.about()).isEqualTo("New about");
        assertThat(response.pictureUrl()).isEqualTo("new.png");
        assertThat(club.getClubId()).isEqualTo(10);
        assertThat(club.getCreatedBy()).isSameAs(creator);
        assertThat(club.getCreatedAt()).isEqualTo(createdAt);
        verify(clubRepository).save(club);
    }

    @Test
    void updateClub_siteAdmin_updatesClub() {
        User siteAdmin = user(1, "site@sut.ru", "Site", "Admin", true);
        Club club = club(10);
        ClubEditRequest request = editRequest("Renamed", "About", null);
        authenticate("site@sut.ru");

        when(userRepository.findByEmail("site@sut.ru")).thenReturn(Optional.of(siteAdmin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubRepository.existsByClubIgnoreCaseAndClubIdNot("Renamed", 10)).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> inv.getArgument(0));

        ClubEditResponse response = clubManagementService.updateClub(10, request);

        assertThat(response.club()).isEqualTo("Renamed");
        assertThat(response.about()).isEqualTo("About");
        assertThat(response.pictureUrl()).isNull();
        verify(clubMemberRepository, never()).findByUserAndClub(siteAdmin, club);
    }

    @Test
    void updateClub_nonAdmin_returnsForbidden() {
        User current = user(1, "member@sut.ru", "M", "U", false);
        Club club = club(10);
        ClubMember membership = member(100, current, club, ClubRole.MEMBER, LocalDateTime.now());
        ClubEditRequest request = editRequest("Name", "About", null);
        authenticate("member@sut.ru");

        when(userRepository.findByEmail("member@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> clubManagementService.updateClub(10, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(clubRepository, never()).save(any(Club.class));
    }

    @Test
    void updateClub_emptyName_returnsBadRequest() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", true);
        Club club = club(10);
        ClubEditRequest request = editRequest("   ", "About", null);
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));

        assertThatThrownBy(() -> clubManagementService.updateClub(10, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(clubRepository, never()).save(any(Club.class));
    }

    @Test
    void updateClub_tooLongFields_returnsBadRequest() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", true);
        Club club = club(10);
        authenticate("admin@sut.ru");
        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));

        assertThatThrownBy(() -> clubManagementService.updateClub(10, editRequest("a".repeat(101), "ok", null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> clubManagementService.updateClub(10, editRequest("ok", "a".repeat(256), null)))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> clubManagementService.updateClub(10, editRequest("ok", "ok", "a".repeat(256))))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(clubRepository, never()).save(any(Club.class));
    }

    @Test
    void updateClub_duplicateName_returnsConflict() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", true);
        Club club = club(10);
        ClubEditRequest request = editRequest("Taken", "About", null);
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubRepository.existsByClubIgnoreCaseAndClubIdNot("Taken", 10)).thenReturn(true);

        assertThatThrownBy(() -> clubManagementService.updateClub(10, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(clubRepository, never()).save(any(Club.class));
    }

    @Test
    void updateClub_duplicateNameRace_returnsConflict() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", true);
        Club club = club(10);
        ClubEditRequest request = editRequest("Taken", "About", null);
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(10)).thenReturn(Optional.of(club));
        when(clubRepository.existsByClubIgnoreCaseAndClubIdNot("Taken", 10)).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> clubManagementService.updateClub(10, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void updateClub_clubNotFound_returnsNotFound() {
        User admin = user(1, "admin@sut.ru", "Admin", "User", true);
        ClubEditRequest request = editRequest("Name", "About", null);
        authenticate("admin@sut.ru");

        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(admin));
        when(clubRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubManagementService.updateClub(999, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(clubRepository, never()).save(any(Club.class));
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "pw", List.of()));
    }

    private static User user(Integer id, String email, String firstName, String lastName, boolean isAppAdmin) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAppAdmin(isAppAdmin);
        return user;
    }

    private static Club club(Integer id) {
        Club club = new Club();
        club.setClubId(id);
        club.setClub("Club " + id);
        club.setActive(true);
        club.setBlocked(false);
        club.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        club.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return club;
    }

    private static ClubMember member(Integer id, User user, Club club, ClubRole role, LocalDateTime joinedAt) {
        ClubMember member = new ClubMember();
        member.setId(id);
        member.setUser(user);
        member.setClub(club);
        member.setUserRole(role);
        member.setJoinedAt(joinedAt);
        return member;
    }

    private static ClubEditRequest editRequest(String club, String about, String pictureUrl) {
        ClubEditRequest request = new ClubEditRequest();
        request.setClub(club);
        request.setAbout(about);
        request.setPictureUrl(pictureUrl);
        return request;
    }
}
