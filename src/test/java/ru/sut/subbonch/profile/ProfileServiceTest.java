package ru.sut.subbonch.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.sut.subbonch.dto.ProfileEditRequest;
import ru.sut.subbonch.dto.ProfileResponse;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.CommentRepository;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.service.ProfileService;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private ProfileService profileService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(10);
        user.setEmail("user@sut.ru");
        user.setFirstName("Ivan");
        user.setLastName("Petrov");
        user.setAbout("about");
        user.setProfilePictureUrl("http://img");
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@sut.ru", "pw", List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProfile_returnsDtoWithCounts() {
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(clubMemberRepository.countByUser(user)).thenReturn(3L);
        when(commentRepository.countByUserAndIsDeletedFalse(user)).thenReturn(7L);

        ProfileResponse response = profileService.getMyProfile();

        assertThat(response.userId()).isEqualTo(10);
        assertThat(response.email()).isEqualTo("user@sut.ru");
        assertThat(response.firstName()).isEqualTo("Ivan");
        assertThat(response.lastName()).isEqualTo("Petrov");
        assertThat(response.about()).isEqualTo("about");
        assertThat(response.profilePictureUrl()).isEqualTo("http://img");
        assertThat(response.clubsCount()).isEqualTo(3L);
        assertThat(response.commentsCount()).isEqualTo(7L);
    }

    @Test
    void updateMyProfile_updatesOnlyAllowedFields() {
        ProfileEditRequest request = new ProfileEditRequest();
        request.setFirstName("  Petr  ");
        request.setLastName(" Ivanov ");
        request.setAbout("  new about  ");
        request.setProfilePictureUrl("  http://new  ");

        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clubMemberRepository.countByUser(user)).thenReturn(1L);
        when(commentRepository.countByUserAndIsDeletedFalse(user)).thenReturn(2L);

        ProfileResponse response = profileService.updateMyProfile(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getFirstName()).isEqualTo("Petr");
        assertThat(saved.getLastName()).isEqualTo("Ivanov");
        assertThat(saved.getAbout()).isEqualTo("new about");
        assertThat(saved.getProfilePictureUrl()).isEqualTo("http://new");
        assertThat(saved.getEmail()).isEqualTo("user@sut.ru");
        assertThat(response.email()).isEqualTo("user@sut.ru");
    }

    @Test
    void updateMyProfile_blankFirstName_throws() {
        ProfileEditRequest request = new ProfileEditRequest();
        request.setFirstName(" ");
        request.setLastName("Ivanov");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.updateMyProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Имя обязательно");
    }

    @Test
    void updateMyProfile_blankLastName_throws() {
        ProfileEditRequest request = new ProfileEditRequest();
        request.setFirstName("Petr");
        request.setLastName("");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.updateMyProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Фамилия обязательна");
    }

    @Test
    void updateMyProfile_emptyAboutBecomesNull() {
        ProfileEditRequest request = new ProfileEditRequest();
        request.setFirstName("Petr");
        request.setLastName("Ivanov");
        request.setAbout("   ");
        request.setProfilePictureUrl("");

        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(clubMemberRepository.countByUser(user)).thenReturn(0L);
        when(commentRepository.countByUserAndIsDeletedFalse(user)).thenReturn(0L);

        profileService.updateMyProfile(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAbout()).isNull();
        assertThat(captor.getValue().getProfilePictureUrl()).isNull();
    }

    @Test
    void updateMyProfile_tooLongFirstName_throws() {
        ProfileEditRequest request = new ProfileEditRequest();
        request.setFirstName("a".repeat(101));
        request.setLastName("Ivanov");
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.updateMyProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Имя слишком длинное");
    }

    @Test
    void updateMyProfile_tooLongAbout_throws() {
        ProfileEditRequest request = new ProfileEditRequest();
        request.setFirstName("Petr");
        request.setLastName("Ivanov");
        request.setAbout("a".repeat(256));
        when(userRepository.findByEmail("user@sut.ru")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.updateMyProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Поле «О себе» слишком длинное");
    }
}
