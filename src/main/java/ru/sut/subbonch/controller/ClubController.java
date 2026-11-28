package ru.sut.subbonch.controller;

import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.service.ClubMembershipService;
import ru.sut.subbonch.service.ClubService;
import ru.sut.subbonch.service.PostService;

@Controller
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final ClubMembershipService clubMembershipService;
    private final PostService postService;

    public ClubController(ClubService clubService,
                          ClubMembershipService clubMembershipService,
                          PostService postService) {
        this.clubService = clubService;
        this.clubMembershipService = clubMembershipService;
        this.postService = postService;
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
            model.addAttribute("postsList", postService.getClubPosts(id));
            model.addAttribute("active", "catalog");
            return "club";
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ex.getReason() == null ? "Клуб не найден" : ex.getReason());
            return "redirect:/clubs";
        }
    }

    @PostMapping("/{id}/join")
    public String joinClub(@PathVariable Integer id,
                           @RequestParam(required = false) String returnUrl,
                           @RequestHeader(value = HttpHeaders.REFERER, required = false) String referer,
                           RedirectAttributes redirectAttributes) {
        try {
            clubMembershipService.joinClub(id);
            redirectAttributes.addFlashAttribute("successMessage", "Вы вступили в клуб.");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason() == null ? "Не удалось вступить в клуб" : ex.getReason());
        }
        return redirectBack(returnUrl, referer, id);
    }

    @PostMapping("/{id}/leave")
    public String leaveClub(@PathVariable Integer id,
                            @RequestParam(required = false) String returnUrl,
                            @RequestHeader(value = HttpHeaders.REFERER, required = false) String referer,
                            RedirectAttributes redirectAttributes) {
        try {
            clubMembershipService.leaveClub(id);
            redirectAttributes.addFlashAttribute("successMessage", "Вы вышли из клуба.");
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getReason() == null ? "Не удалось выйти из клуба" : ex.getReason());
        }
        return redirectBack(returnUrl, referer, id);
    }

    /**
     * Возвращает туда, откуда пришёл запрос (каталог с фильтрами, страница клуба и т.п.),
     * принимая только локальные пути — внешние адреса отбрасываются (open redirect).
     */
    private String redirectBack(String returnUrl, String referer, Integer clubId) {
        String localPath = sanitizeLocalPath(returnUrl);
        if (localPath == null) {
            localPath = sanitizeLocalPath(referer);
        }
        return "redirect:" + (localPath != null ? localPath : "/clubs/" + clubId);
    }

    private static String sanitizeLocalPath(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.indexOf('\r') >= 0 || trimmed.indexOf('\n') >= 0) {
            return null;
        }
        String path = trimmed;
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                URI uri = URI.create(trimmed);
                String rawQuery = uri.getRawQuery();
                path = uri.getRawPath() + (rawQuery == null ? "" : "?" + rawQuery);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        // Только абсолютный путь на своём же хосте: "/clubs", но не "//evil.example"
        // и не "/\evil.example" (браузеры трактуют обратный слэш как разделитель).
        String lowerPath = path.toLowerCase(java.util.Locale.ROOT);
        if (!path.startsWith("/")
                || path.startsWith("//")
                || path.indexOf('\\') >= 0
                || lowerPath.startsWith("/%5c")
                || lowerPath.startsWith("/%2f")) {
            return null;
        }
        return path;
    }
}
