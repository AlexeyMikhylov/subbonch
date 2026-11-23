package ru.sut.subbonch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.sut.subbonch.dto.ApplicationCreateRequest;
import ru.sut.subbonch.service.ApplicationService;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/create")
    public String create(@ModelAttribute("applicationForm") ApplicationCreateRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            applicationService.createApplication(request);
            redirectAttributes.addFlashAttribute("applicationSuccess", "Заявка отправлена на рассмотрение.");
            return "redirect:/profile";
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("applicationError",
                    ex.getReason() == null ? "Не удалось отправить заявку" : ex.getReason());
            redirectAttributes.addFlashAttribute("applicationForm", request);
            return "redirect:/profile";
        }
    }
}
