package ru.sut.subbonch.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.sut.subbonch.controller.ProfileController;
import ru.sut.subbonch.dto.ProfileEditRequest;
import ru.sut.subbonch.dto.ProfileResponse;
import ru.sut.subbonch.service.ProfileService;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController profileController;

    @Test
    void getProfile_returnsOk() {
        ProfileResponse profile = new ProfileResponse(1, "I", "P", "u@sut.ru", null, null, null, 0, 0);
        when(profileService.getMyProfile()).thenReturn(profile);

        ResponseEntity<ProfileResponse> response = profileController.getProfile();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(profile);
    }

    @Test
    void getProfileForEdit_returnsOk() {
        ProfileResponse profile = new ProfileResponse(1, "I", "P", "u@sut.ru", null, null, null, 0, 0);
        when(profileService.getMyProfileForEdit()).thenReturn(profile);

        ResponseEntity<ProfileResponse> response = profileController.getProfileForEdit();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(profile);
    }

    @Test
    void editProfile_validationError_returnsBadRequest() {
        ProfileEditRequest request = new ProfileEditRequest();
        doThrow(new IllegalArgumentException("Имя обязательно")).when(profileService).updateMyProfile(request);

        ResponseEntity<?> response = profileController.editProfile(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Имя обязательно"));
    }
}
