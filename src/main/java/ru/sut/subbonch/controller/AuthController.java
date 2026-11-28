package ru.sut.subbonch.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ru.sut.subbonch.dto.RegisterRequest;
import ru.sut.subbonch.service.AuthService;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterRequest request, Model model) {
        try {
            authService.register(request);
            return "redirect:/login?registered";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            repopulateForm(request, model);
            return "register";
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("errorMessage", "Email уже занят");
            repopulateForm(request, model);
            return "register";
        }
    }

    private void repopulateForm(RegisterRequest request, Model model) {
        model.addAttribute("email", request == null ? "" : request.getEmail());
        model.addAttribute("firstName", request == null ? "" : request.getFirstName());
        model.addAttribute("lastName", request == null ? "" : request.getLastName());
    }
}
