package ru.sut.subbonch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.service.ClubMembershipService;
import ru.sut.subbonch.service.ClubService;

@Controller
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final ClubMembershipService clubMembershipService;

    public ClubController(ClubService clubService, ClubMembershipService clubMembershipService) {
        this.clubService = clubService;
        this.clubMembershipService = clubMembershipService;
    }

    @GetMapping
    public String catalog(@RequestParam(required = false) String q,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          Model model) {
        model.addAttribute("clubsPage", clubService.getCatalog(q, page, size));
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("active", "catalog");
        return "catalog";
    }

    @GetMapping("/{id}")
    public String clubPage(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("clubPage", clubService.getClubPage(id));
            model.addAttribute("active", "catalog");
            return "club";
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ex.getReason() == null ? "Клуб не найден" : ex.getReason());
            return "redirect:/clubs";
        }
    }

    @PostMapping("/{id}/join")
    public String joinClub(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            clubMembershipService.joinClub(id);
            redirectAttributes.addFlashAttribute("successMessage", "Вы вступили в клуб.");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason() == null ? "Не удалось вступить в клуб" : ex.getReason());
        }
        return "redirect:/clubs/" + id;
    }

    @PostMapping("/{id}/leave")
    public String leaveClub(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            clubMembershipService.leaveClub(id);
            redirectAttributes.addFlashAttribute("successMessage", "Вы вышли из клуба.");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason() == null ? "Не удалось выйти из клуба" : ex.getReason());
        }
        return "redirect:/clubs/" + id;
    }
}
