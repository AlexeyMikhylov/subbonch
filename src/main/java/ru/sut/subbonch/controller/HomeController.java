package ru.sut.subbonch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.sut.subbonch.service.PostService;

/** Главная/лента, вход, регистрация. */
@Controller
public class HomeController {

    private final PostService postService;

    public HomeController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping({"/", "/feed"})
    public String feed(@RequestParam(name = "tab", required = false) String tab, Model model) {
        boolean subscriptionsTab = "subscriptions".equalsIgnoreCase(tab);
        model.addAttribute("active", "feed");
        model.addAttribute("feedTab", subscriptionsTab ? "subscriptions" : "foryou");
        if (subscriptionsTab) {
            var posts = postService.getSubscriptionsFeed();
            model.addAttribute("postsList", posts);
            model.addAttribute("subscriptionsGuest", posts.isEmpty() && !isAuthenticated());
            model.addAttribute("clubRolesById", postService.getCurrentUserRolesByClubIds(
                    posts.stream().map(p -> p.clubId()).collect(java.util.stream.Collectors.toSet())));
        } else {
            var posts = postService.getForYouFeed();
            model.addAttribute("postsList", posts);
            model.addAttribute("clubRolesById", postService.getCurrentUserRolesByClubIds(
                    posts.stream().map(p -> p.clubId()).collect(java.util.stream.Collectors.toSet())));
        }
        return "feed";
    }

    @GetMapping("/index")
    public String index() {
        return "redirect:/";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    private boolean isAuthenticated() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String s && "anonymousUser".equals(s));
    }
}
