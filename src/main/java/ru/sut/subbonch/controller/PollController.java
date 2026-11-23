package ru.sut.subbonch.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.sut.subbonch.service.PollService;

@Controller
@RequestMapping("/polls")
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    @PostMapping("/{pollId}/vote")
    public String vote(@PathVariable Integer pollId,
                       @RequestParam(name = "voteId", required = false) Integer voteId,
                       @RequestParam(name = "returnUrl", required = false) String returnUrl,
                       HttpServletRequest request,
                       RedirectAttributes redirectAttributes) {
        String redirectTarget = resolveRedirectTarget(returnUrl, request);
        try {
            pollService.vote(pollId, voteId);
            redirectAttributes.addFlashAttribute("successMessage", "Голос учтён.");
            return "redirect:" + redirectTarget;
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ex.getReason() == null ? "Не удалось проголосовать" : ex.getReason());
            return "redirect:" + redirectTarget;
        } catch (DataIntegrityViolationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Вы уже голосовали в этом опросе");
            return "redirect:" + redirectTarget;
        }
    }

    private String resolveRedirectTarget(String returnUrl, HttpServletRequest request) {
        if (isSafeLocalPath(returnUrl)) {
            return returnUrl;
        }
        String referer = request.getHeader("Referer");
        if (isSafeReferer(referer, request)) {
            return referer;
        }
        return "/feed";
    }

    private boolean isSafeLocalPath(String url) {
        return url != null
                && url.startsWith("/")
                && !url.startsWith("//")
                && !url.startsWith("/\\");
    }

    private boolean isSafeReferer(String referer, HttpServletRequest request) {
        if (referer == null || referer.isBlank()) {
            return false;
        }
        if (isSafeLocalPath(referer)) {
            return true;
        }
        try {
            URI uri = URI.create(referer);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return false;
            }
            if (!scheme.equalsIgnoreCase(request.getScheme())) {
                return false;
            }
            if (!host.equalsIgnoreCase(request.getServerName())) {
                return false;
            }
            int refererPort = uri.getPort() == -1 ? defaultPort(scheme) : uri.getPort();
            int requestPort = request.getServerPort();
            return refererPort == requestPort;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private int defaultPort(String scheme) {
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return 80;
        }
        return -1;
    }
}
