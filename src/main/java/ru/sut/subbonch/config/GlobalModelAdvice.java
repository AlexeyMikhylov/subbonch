package ru.sut.subbonch.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.UserRepository;
import ru.sut.subbonch.security.LoginFailureHandler;

/**
 * Глобальные атрибуты модели для всех шаблонов: {@code currentUser} и
 * {@code myClubs}. {@code myClubs} загружается в транзакции, поэтому с
 * {@code open-in-view=false} не падает с {@code LazyInitializationException}.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository;

    public GlobalModelAdvice(UserRepository userRepository,
                             ClubMemberRepository clubMemberRepository) {
        this.userRepository = userRepository;
        this.clubMemberRepository = clubMemberRepository;
    }

    @ModelAttribute("currentUser")
    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        String email;
        if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        } else {
            email = String.valueOf(principal);
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    @ModelAttribute
    public void exposeLoginFlash(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        moveFlash(session, LoginFailureHandler.SESSION_LOGIN_ERROR, "loginError", model);
        moveFlash(session, LoginFailureHandler.SESSION_BLOCK_REASON, "blockReason", model);
    }

    private static void moveFlash(HttpSession session, String sessionKey, String modelKey, Model model) {
        Object value = session.getAttribute(sessionKey);
        if (value != null) {
            session.removeAttribute(sessionKey);
            model.addAttribute(modelKey, value);
        }
    }

    /**
     * Явное имя атрибута обязательно: без него Spring выведет имя из типа
     * ({@code user}) и создаст пустой transient {@code User} вместо текущего.
     */
    @Transactional(readOnly = true)
    @ModelAttribute("myClubs")
    public List<Club> myClubs(@ModelAttribute("currentUser") User currentUser) {
        if (currentUser == null) {
            return List.of();
        }
        List<ClubMember> memberships = clubMemberRepository.findByUser(currentUser);
        return memberships.stream()
                .map(ClubMember::getClub)
                .toList();
    }
}
