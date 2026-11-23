package ru.sut.subbonch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.dto.PostCreateRequest;
import ru.sut.subbonch.service.PostService;

@Controller
@RequestMapping("/clubs")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping("/{id}/posts/create")
    public String createPost(@PathVariable Integer id,
                             @ModelAttribute PostCreateRequest request,
                             RedirectAttributes redirectAttributes) {
        try {
            postService.createPost(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Пост создан.");
            return "redirect:/clubs/" + id;
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ex.getReason() == null ? "Не удалось создать пост" : ex.getReason());
            if (ex.getStatusCode().value() == 404) {
                return "redirect:/clubs";
            }
            return "redirect:/clubs/" + id;
        }
    }

    @PostMapping("/{id}/posts/{postId}/delete")
    public String deletePost(@PathVariable Integer id,
                             @PathVariable Integer postId,
                             RedirectAttributes redirectAttributes) {
        try {
            postService.deletePost(id, postId);
            redirectAttributes.addFlashAttribute("successMessage", "Пост удалён.");
            return "redirect:/clubs/" + id;
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ex.getReason() == null ? "Не удалось удалить пост" : ex.getReason());
            if (ex.getStatusCode().value() == 404) {
                String reason = ex.getReason();
                if ("Клуб не найден".equals(reason) || "Клуб недоступен".equals(reason)) {
                    return "redirect:/clubs";
                }
                return "redirect:/clubs/" + id;
            }
            return "redirect:/clubs/" + id;
        }
    }
}
