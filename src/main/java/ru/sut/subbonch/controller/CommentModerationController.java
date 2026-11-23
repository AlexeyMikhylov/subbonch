package ru.sut.subbonch.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.sut.subbonch.service.CommentService;

@Controller
@RequestMapping("/comments")
public class CommentModerationController {

    private final CommentService commentService;

    public CommentModerationController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/{commentId}/delete")
    public String delete(@PathVariable Integer commentId,
                         @RequestParam(name = "returnUrl", required = false) String returnUrl,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        Integer clubId = null;
        try {
            clubId = commentService.deleteComment(commentId);
            redirectAttributes.addFlashAttribute("successMessage", "Комментарий удалён.");
            return resolveRedirectTarget(returnUrl, request, clubId);
        } catch (ResponseStatusException ex) {
            String message = ex.getReason() != null ? ex.getReason() : "Не удалось удалить комментарий";
            redirectAttributes.addFlashAttribute("errorMessage", message);
            return resolveRedirectTarget(returnUrl, request, clubId);
        }
    }

    private String resolveRedirectTarget(String returnUrl, HttpServletRequest request, Integer clubId) {
        if (isSafeLocalPath(returnUrl)) {
            return "redirect:" + returnUrl;
        }
        String referer = request.getHeader("Referer");
        if (isSafeSameOriginReferer(referer, request)) {
            return "redirect:" + referer;
        }
        if (clubId != null) {
            return "redirect:/clubs/" + clubId;
        }
        return "redirect:/feed";
    }

    private boolean isSafeLocalPath(String value) {
        return value != null && value.startsWith("/") && !value.startsWith("//") && !value.startsWith("/\\");
    }

    private boolean isSafeSameOriginReferer(String referer, HttpServletRequest request) {
        if (referer == null || referer.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(referer);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return false;
            }
            boolean sameScheme = scheme.equalsIgnoreCase(request.getScheme());
            boolean sameHost = host.equalsIgnoreCase(request.getServerName());
            int refererPort = uri.getPort() == -1 ? defaultPort(scheme) : uri.getPort();
            int requestPort = request.getServerPort();
            return sameScheme && sameHost && refererPort == requestPort;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private int defaultPort(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }
}
