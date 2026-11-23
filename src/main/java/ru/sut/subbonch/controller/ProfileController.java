package ru.sut.subbonch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.sut.subbonch.dto.ProfileEditRequest;
import ru.sut.subbonch.dto.ProfileResponse;
import ru.sut.subbonch.service.ProfileService;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String profilePage(Model model) {
        model.addAttribute("profile", profileService.getMyProfile());
        return "profile";
    }

    @GetMapping("/edit")
    public String editProfilePage(Model model) {
        ProfileResponse current = profileService.getMyProfileForEdit();
        if (!model.containsAttribute("profileForm")) {
            ProfileEditRequest form = new ProfileEditRequest();
            form.setFirstName(current.firstName());
            form.setLastName(current.lastName());
            form.setAbout(current.about());
            form.setProfilePictureUrl(current.profilePictureUrl());
            model.addAttribute("profileForm", form);
        }
        model.addAttribute("profile", current);
        return "profile-edit";
    }

    @PostMapping("/edit")
    public String editProfile(@ModelAttribute("profileForm") ProfileEditRequest request,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            profileService.updateMyProfile(request);
            redirectAttributes.addFlashAttribute("profileSuccess", "Профиль сохранён.");
            return "redirect:/profile";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("profileForm", request);
            model.addAttribute("profile", profileService.getMyProfileForEdit());
            return "profile-edit";
        }
    }
}
