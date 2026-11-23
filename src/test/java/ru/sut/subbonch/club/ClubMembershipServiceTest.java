package ru.sut.subbonch.club;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.ClubRepository;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.service.ClubMembershipService;

@ExtendWith(MockitoExtension.class)
class ClubMembershipServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClubMembershipService clubMembershipService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void joinClub_success_createsMemberRole() {
        User current = user(1, "member@sut.ru");
        Club club = club(10);
        authenticate("member@sut.ru");
        when(userRepository.findByEmail("member@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.existsByUserAndClub(current, club)).thenReturn(false);

        clubMembershipService.joinClub(10);

        ArgumentCaptor<ClubMember> captor = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberRepository).save(captor.capture());
        ClubMember saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(current);
        assertThat(saved.getClub()).isEqualTo(club);
        assertThat(saved.getUserRole()).isEqualTo(ClubRole.MEMBER);
    }

    @Test
    void joinClub_duplicateMembership_returnsConflict() {
        User current = user(1, "member@sut.ru");
        Club club = club(10);
        authenticate("member@sut.ru");
        when(userRepository.findByEmail("member@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.existsByUserAndClub(current, club)).thenReturn(true);

        assertThatThrownBy(() -> clubMembershipService.joinClub(10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    void joinClub_raceDuplicate_returnsConflict() {
        User current = user(1, "member@sut.ru");
        Club club = club(10);
        authenticate("member@sut.ru");
        when(userRepository.findByEmail("member@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.existsByUserAndClub(current, club)).thenReturn(false);
        when(clubMemberRepository.save(any(ClubMember.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> clubMembershipService.joinClub(10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void joinClub_nonexistentClub_returnsNotFound() {
        User current = user(1, "member@sut.ru");
        authenticate("member@sut.ru");
        when(userRepository.findByEmail("member@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubMembershipService.joinClub(999))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    void joinClub_blockedOrInactiveClub_returnsNotFound() {
        User current = user(1, "member@sut.ru");
        authenticate("member@sut.ru");
        when(userRepository.findByEmail("member@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(11)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubMembershipService.joinClub(11))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void joinClub_guest_returnsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> clubMembershipService.joinClub(10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(clubRepository, never()).findByClubIdAndIsActiveTrueAndIsBlockedFalse(any());
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    void joinClub_anonymousPrincipal_returnsUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThatThrownBy(() -> clubMembershipService.joinClub(10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void joinClub_authenticatedButUserMissing_returnsUnauthorized() {
        authenticate("missing@sut.ru");
        when(userRepository.findByEmail("missing@sut.ru")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubMembershipService.joinClub(10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void leaveClub_member_deletesMembership() {
        User current = user(1, "member@sut.ru");
        Club club = club(10);
        ClubMember member = member(current, club, ClubRole.MEMBER);
        authenticate("member@sut.ru");
        when(userRepository.findByEmail("member@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.of(member));

        clubMembershipService.leaveClub(10);

        verify(clubMemberRepository).delete(member);
    }

    @Test
    void leaveClub_moderator_blockedAndMembershipNotChanged() {
        User current = user(1, "mod@sut.ru");
        Club club = club(10);
        ClubMember member = member(current, club, ClubRole.MODERATOR);
        authenticate("mod@sut.ru");
        when(userRepository.findByEmail("mod@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> clubMembershipService.leaveClub(10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).isEqualTo("Сначала передайте права");
                });

        verify(clubMemberRepository, never()).delete(any());
    }

    @Test
    void leaveClub_admin_blockedAndMembershipNotChanged() {
        User current = user(1, "admin@sut.ru");
        Club club = club(10);
        ClubMember member = member(current, club, ClubRole.ADMIN);
        authenticate("admin@sut.ru");
        when(userRepository.findByEmail("admin@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> clubMembershipService.leaveClub(10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(rse.getReason()).isEqualTo("Сначала передайте права");
                });

        verify(clubMemberRepository, never()).delete(any());
    }

    @Test
    void leaveClub_nonMember_returnsClearError() {
        User current = user(1, "user@sut.ru");
        Club club = club(10);
        authenticate("user@sut.ru");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(current));
        when(clubRepository.findByClubIdAndIsActiveTrueAndIsBlockedFalse(10)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByUserAndClub(current, club)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubMembershipService.leaveClub(10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("Вы не являетесь участником клуба");
                });

        verify(clubMemberRepository, never()).delete(any());
    }

    @Test
    void leaveClub_guest_returnsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> clubMembershipService.leaveClub(10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(clubRepository, never()).findByClubIdAndIsActiveTrueAndIsBlockedFalse(any());
        verify(clubMemberRepository, never()).delete(any());
    }

    @Test
    void leaveClub_anonymousPrincipal_returnsUnauthorized() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThatThrownBy(() -> clubMembershipService.leaveClub(10))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "pw", List.of()));
    }

    private static User user(Integer id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
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

    private static ClubMember member(User user, Club club, ClubRole role) {
        ClubMember member = new ClubMember();
        member.setUser(user);
        member.setClub(club);
        member.setUserRole(role);
        member.setJoinedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return member;
    }
}
