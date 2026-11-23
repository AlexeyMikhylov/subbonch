package ru.sut.subbonch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.dto.ApplicationCreateRequest;
import ru.sut.subbonch.entity.Application;
import ru.sut.subbonch.entity.ApplicationStatus;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ApplicationRepository;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.ClubRepository;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.service.ApplicationService;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private ClubMemberRepository clubMemberRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApplicationService applicationService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createApplication_success_createsPending() {
        User user = user(1, "u@test.local");
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(clubRepository.existsByClubIgnoreCase("Chess Club")).thenReturn(false);
        when(applicationRepository.existsByAuthorAndAppStatus(user, ApplicationStatus.PENDING)).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        ApplicationCreateRequest req = new ApplicationCreateRequest();
        req.setClubName("  Chess Club  ");
        req.setAppDescription("  Desc  ");
        req.setReason("  Reason  ");

        Application result = applicationService.createApplication(req);

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(captor.capture());
        Application saved = captor.getValue();
        assertThat(saved.getAuthor()).isEqualTo(user);
        assertThat(saved.getClubName()).isEqualTo("Chess Club");
        assertThat(saved.getAppDescription()).isEqualTo("Desc");
        assertThat(saved.getReason()).isEqualTo("Reason");
        assertThat(saved.getAppStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void createApplication_unauthorized_throws401() {
        ApplicationCreateRequest req = validRequest();

        assertThatThrownBy(() -> applicationService.createApplication(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void createApplication_blankClubName_throws400() {
        User user = user(1, "u@test.local");
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        ApplicationCreateRequest req = validRequest();
        req.setClubName("  ");

        assertThatThrownBy(() -> applicationService.createApplication(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createApplication_tooLongDescription_throws400() {
        User user = user(1, "u@test.local");
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        ApplicationCreateRequest req = validRequest();
        req.setAppDescription("a".repeat(256));

        assertThatThrownBy(() -> applicationService.createApplication(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createApplication_duplicateClubName_throws400() {
        User user = user(1, "u@test.local");
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(clubRepository.existsByClubIgnoreCase("Chess Club")).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(validRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void createApplication_existingPending_throws400() {
        User user = user(1, "u@test.local");
        authenticate(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(clubRepository.existsByClubIgnoreCase("Chess Club")).thenReturn(false);
        when(applicationRepository.existsByAuthorAndAppStatus(user, ApplicationStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> applicationService.createApplication(validRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void getPendingApplications_returnsRepositoryResult() {
        Application a = new Application();
        a.setApplicationId(1);
        when(applicationRepository.findByAppStatusOrderByCreatedAtAsc(ApplicationStatus.PENDING))
                .thenReturn(java.util.List.of(a));

        assertThat(applicationService.getPendingApplications()).containsExactly(a);
        verify(applicationRepository).findByAppStatusOrderByCreatedAtAsc(ApplicationStatus.PENDING);
    }

    @Test
    void approveApplication_success_createsClubAndAdminMember() {
        User admin = user(10, "admin@test.local");
        User author = user(1, "author@test.local");
        Application app = application(5, author, "Chess Club", "Desc", ApplicationStatus.PENDING);
        authenticate(admin.getEmail());
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(applicationRepository.findById(5)).thenReturn(Optional.of(app));
        when(clubRepository.existsByClubIgnoreCase("Chess Club")).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenAnswer(inv -> {
            Club c = inv.getArgument(0);
            c.setClubId(100);
            return c;
        });
        when(clubMemberRepository.save(any(ClubMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        applicationService.approveApplication(5);

        ArgumentCaptor<Club> clubCaptor = ArgumentCaptor.forClass(Club.class);
        verify(clubRepository).save(clubCaptor.capture());
        Club savedClub = clubCaptor.getValue();
        assertThat(savedClub.getClub()).isEqualTo("Chess Club");
        assertThat(savedClub.getAbout()).isEqualTo("Desc");
        assertThat(savedClub.getCreatedBy()).isEqualTo(author);
        assertThat(savedClub.isActive()).isTrue();
        assertThat(savedClub.isBlocked()).isFalse();

        ArgumentCaptor<ClubMember> memberCaptor = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberRepository).save(memberCaptor.capture());
        ClubMember member = memberCaptor.getValue();
        assertThat(member.getUser()).isEqualTo(author);
        assertThat(member.getClub().getClubId()).isEqualTo(100);
        assertThat(member.getUserRole()).isEqualTo(ClubRole.ADMIN);

        assertThat(app.getAppStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(app.getReviewer()).isEqualTo(admin);
        verify(applicationRepository).save(app);
    }

    @Test
    void approveApplication_notPending_throws400() {
        User admin = user(10, "admin@test.local");
        User author = user(1, "author@test.local");
        Application app = application(5, author, "Chess Club", "Desc", ApplicationStatus.APPROVED);
        authenticate(admin.getEmail());
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(applicationRepository.findById(5)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> applicationService.approveApplication(5))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(clubRepository, never()).save(any());
    }

    @Test
    void approveApplication_clubNameConflict_throws400() {
        User admin = user(10, "admin@test.local");
        User author = user(1, "author@test.local");
        Application app = application(5, author, "Chess Club", "Desc", ApplicationStatus.PENDING);
        authenticate(admin.getEmail());
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(applicationRepository.findById(5)).thenReturn(Optional.of(app));
        when(clubRepository.existsByClubIgnoreCase("Chess Club")).thenReturn(true);

        assertThatThrownBy(() -> applicationService.approveApplication(5))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(clubRepository, never()).save(any());
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    void approveApplication_notFound_throws404() {
        User admin = user(10, "admin@test.local");
        authenticate(admin.getEmail());
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(applicationRepository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applicationService.approveApplication(5))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static ApplicationCreateRequest validRequest() {
        ApplicationCreateRequest req = new ApplicationCreateRequest();
        req.setClubName("Chess Club");
        req.setAppDescription("Desc");
        req.setReason("Reason");
        return req;
    }

    private static void authenticate(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "pw", java.util.List.of()));
    }

    private static User user(Integer id, String email) {
        User user = new User();
        user.setUserId(id);
        user.setEmail(email);
        return user;
    }

    private static Application application(Integer id, User author, String clubName, String description, ApplicationStatus status) {
        Application app = new Application();
        app.setApplicationId(id);
        app.setAuthor(author);
        app.setClubName(clubName);
        app.setAppDescription(description);
        app.setReason("Reason");
        app.setAppStatus(status);
        return app;
    }
}
