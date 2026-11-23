package ru.sut.subbonch.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
    void profilePage_returnsViewAndProfile() {
        ProfileResponse profile = new ProfileResponse(1, "I", "P", "u@sut.ru", null, null, null, 0, 0);
        when(profileService.getMyProfile()).thenReturn(profile);
        Model model = new ConcurrentModel();

        String view = profileController.profilePage(model);

        assertThat(view).isEqualTo("profile");
        assertThat(model.getAttribute("profile")).isEqualTo(profile);
    }

    @Test
    void editProfilePage_returnsViewAndForm() {
        ProfileResponse profile = new ProfileResponse(1, "I", "P", "u@sut.ru", "about", null, null, 1, 2);
        when(profileService.getMyProfileForEdit()).thenReturn(profile);
        Model model = new ConcurrentModel();

        String view = profileController.editProfilePage(model);

        assertThat(view).isEqualTo("profile-edit");
        assertThat(model.getAttribute("profile")).isEqualTo(profile);
        ProfileEditRequest form = (ProfileEditRequest) model.getAttribute("profileForm");
        assertThat(form.getFirstName()).isEqualTo("I");
        assertThat(form.getLastName()).isEqualTo("P");
        assertThat(form.getAbout()).isEqualTo("about");
    }

    @Test
    void editProfile_validationError_returnsEditView() {
        ProfileEditRequest request = new ProfileEditRequest();
        request.setFirstName("");
        ProfileResponse profile = new ProfileResponse(1, "I", "P", "u@sut.ru", null, null, null, 0, 0);
        doThrow(new IllegalArgumentException("Имя обязательно")).when(profileService).updateMyProfile(request);
        when(profileService.getMyProfileForEdit()).thenReturn(profile);
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        String view = profileController.editProfile(request, model, redirectAttributes);

        assertThat(view).isEqualTo("profile-edit");
        assertThat(model.getAttribute("error")).isEqualTo("Имя обязательно");
        assertThat(model.getAttribute("profileForm")).isEqualTo(request);
    }

    @Test
    void editProfile_success_redirectsToProfile() {
        ProfileEditRequest request = new ProfileEditRequest();
        request.setFirstName("Ivan");
        request.setLastName("Petrov");
        when(profileService.updateMyProfile(request))
                .thenReturn(new ProfileResponse(1, "Ivan", "Petrov", "u@sut.ru", null, null, null, 0, 0));
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes = mock(RedirectAttributes.class);

        String view = profileController.editProfile(request, model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profile");
        verify(redirectAttributes).addFlashAttribute(eq("profileSuccess"), any());
    }
}
