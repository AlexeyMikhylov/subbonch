package ru.sut.subbonch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.sut.subbonch.service.ApplicationService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ApplicationService applicationService;

    public AdminController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public String adminPage(Model model) {
        model.addAttribute("applications", applicationService.getPendingApplications());
        model.addAttribute("active", "admin");
        return "admin";
    }

    @PostMapping("/applications/{id}/approve")
    public String approve(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            applicationService.approveApplication(id);
            redirectAttributes.addFlashAttribute("successMessage", "Заявка одобрена, клуб создан.");
            return "redirect:/admin";
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ex.getReason() == null ? "Не удалось одобрить заявку" : ex.getReason());
            return "redirect:/admin";
        }
    }
}
