package ru.sut.subbonch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.dto.ClubEditRequest;
import ru.sut.subbonch.dto.ClubEditResponse;
import ru.sut.subbonch.service.ClubManagementService;

@Controller
@RequestMapping("/clubs")
public class ClubManagementController {

    private final ClubManagementService clubManagementService;

    public ClubManagementController(ClubManagementService clubManagementService) {
        this.clubManagementService = clubManagementService;
    }

    @GetMapping("/{id}/members")
    public String members(@PathVariable Integer id,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("clubId", id);
            model.addAttribute("membersPage", clubManagementService.getMembers(id, page, size));
            model.addAttribute("active", "catalog");
            return "club-members";
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ex.getReason() == null ? "Нет доступа к участникам клуба" : ex.getReason());
            if (ex.getStatusCode().value() == 404) {
                return "redirect:/clubs";
            }
            return "redirect:/clubs/" + id;
        }
    }

    @PostMapping("/{id}/members/{userId}/moderator/assign")
    public String assignModerator(@PathVariable Integer id,
                                  @PathVariable Integer userId,
                                  RedirectAttributes redirectAttributes) {
        try {
            clubManagementService.assignModerator(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Модератор назначен.");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason() == null ? "Не удалось назначить модератора" : ex.getReason());
        }
        return "redirect:/clubs/" + id + "/members";
    }

    @PostMapping("/{id}/members/{userId}/moderator/remove")
    public String removeModerator(@PathVariable Integer id,
                                  @PathVariable Integer userId,
                                  RedirectAttributes redirectAttributes) {
        try {
            clubManagementService.removeModerator(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Модератор снят.");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason() == null ? "Не удалось снять модератора" : ex.getReason());
        }
        return "redirect:/clubs/" + id + "/members";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ClubEditResponse current = clubManagementService.getClubForEdit(id);
            if (!model.containsAttribute("clubForm")) {
                ClubEditRequest form = new ClubEditRequest();
                form.setClub(current.club());
                form.setAbout(current.about());
                form.setPictureUrl(current.pictureUrl());
                model.addAttribute("clubForm", form);
            }
            model.addAttribute("clubId", id);
            model.addAttribute("active", "catalog");
            return "club-edit";
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ex.getReason() == null ? "Нет доступа к редактированию клуба" : ex.getReason());
            if (ex.getStatusCode().value() == 404) {
                return "redirect:/clubs";
            }
            return "redirect:/clubs/" + id;
        }
    }

    @PostMapping("/{id}/edit")
    public String editClub(@PathVariable Integer id,
                           @ModelAttribute("clubForm") ClubEditRequest request,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            clubManagementService.updateClub(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Клуб обновлён.");
            return "redirect:/clubs/" + id;
        } catch (ResponseStatusException ex) {
            model.addAttribute("errorMessage", ex.getReason() == null ? "Не удалось сохранить клуб" : ex.getReason());
            model.addAttribute("clubForm", request);
            model.addAttribute("clubId", id);
            model.addAttribute("active", "catalog");
            return "club-edit";
        }
    }
}
