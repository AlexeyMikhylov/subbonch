package ru.sut.subbonch.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.sut.subbonch.entity.Post;
import ru.sut.subbonch.repository.PostRepository;
import ru.sut.subbonch.service.CommentService;
import ru.sut.subbonch.service.PostService;

@Controller
@RequestMapping("/posts")
public class CommentController {

    private final CommentService commentService;
    private final PostRepository postRepository;
    private final PostService postService;

    public CommentController(CommentService commentService, PostRepository postRepository, PostService postService) {
        this.commentService = commentService;
        this.postRepository = postRepository;
        this.postService = postService;
    }

    @GetMapping("/{id}")
    public String postPage(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            var post = postService.getPost(id);
            model.addAttribute("post", post);
            model.addAttribute("userRoleInClub", postService.getCurrentUserRolesByClubIds(java.util.Set.of(post.clubId()))
                    .getOrDefault(post.clubId(), "NONE"));
            model.addAttribute("active", "feed");
            return "post";
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ex.getReason() == null ? "Пост не найден" : ex.getReason());
            return "redirect:/feed";
        }
    }

    @PostMapping("/{id}/comments")
    public String createComment(@PathVariable Integer id,
                                @RequestParam(name = "comment", required = false) String comment,
                                @RequestParam(name = "parentId", required = false) Integer parentId,
                                @RequestParam(name = "returnUrl", required = false) String returnUrl,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        try {
            Integer clubId = commentService.createComment(id, comment, parentId);
            redirectAttributes.addFlashAttribute("successMessage", "Комментарий добавлен.");
            return "redirect:" + resolveSuccessRedirect(returnUrl, clubId);
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    ex.getReason() == null ? "Не удалось добавить комментарий" : ex.getReason());
            return "redirect:" + resolveErrorRedirect(id, returnUrl, request);
        }
    }

    private String resolveSuccessRedirect(String returnUrl, Integer clubId) {
        if (isSafeLocalPath(returnUrl)) {
            return returnUrl;
        }
        return "/clubs/" + clubId;
    }

    private String resolveErrorRedirect(Integer postId, String returnUrl, HttpServletRequest request) {
        if (isSafeLocalPath(returnUrl)) {
            return returnUrl;
        }
        String referer = request.getHeader("Referer");
        if (isSafeReferer(referer, request)) {
            return referer;
        }
        if (postId != null) {
            Integer clubId = postRepository.findById(postId)
                    .map(Post::getClub)
                    .map(club -> club.getClubId())
                    .orElse(null);
            if (clubId != null) {
                return "/clubs/" + clubId;
            }
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
