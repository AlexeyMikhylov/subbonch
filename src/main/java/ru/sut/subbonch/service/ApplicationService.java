package ru.sut.subbonch.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.sut.subbonch.dto.ApplicationCreateRequest;
import ru.sut.subbonch.entity.Application;
import ru.sut.subbonch.entity.ApplicationStatus;
import ru.sut.subbonch.entity.Club;
import ru.sut.subbonch.entity.ClubMember;
import ru.sut.subbonch.entity.ClubRole;
import ru.sut.subbonch.entity.User;
import ru.sut.subbonch.repository.ApplicationRepository;
import ru.sut.subbonch.repository.ClubMemberRepository;
import ru.sut.subbonch.repository.ClubRepository;
import ru.sut.subbonch.repository.UserRepository;
import java.util.List;

@Service
public class ApplicationService {

    static final int MAX_CLUB_NAME_LENGTH = 100;
    static final int MAX_DESCRIPTION_LENGTH = 255;
    static final int MAX_REASON_LENGTH = 255;

    private final ApplicationRepository applicationRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              ClubRepository clubRepository,
                              ClubMemberRepository clubMemberRepository,
                              UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.clubRepository = clubRepository;
        this.clubMemberRepository = clubMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Application> getPendingApplications() {
        return applicationRepository.findByAppStatusOrderByCreatedAtAsc(ApplicationStatus.PENDING);
    }

    @Transactional
    public void approveApplication(Integer applicationId) {
        User admin = getCurrentUserOrThrow();
        if (applicationId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка не найдена");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка не найдена"));

        if (application.getAppStatus() != ApplicationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заявка уже рассмотрена");
        }

        String clubName = application.getClubName();
        if (clubName == null || clubName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Некорректное название клуба в заявке");
        }
        if (clubRepository.existsByClubIgnoreCase(clubName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Клуб с таким названием уже существует");
        }

        User author = application.getAuthor();
        if (author == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Автор заявки не найден");
        }

        Club club = new Club();
        club.setClub(clubName.trim());
        club.setAbout(application.getAppDescription());
        club.setActive(true);
        club.setBlocked(false);
        club.setCreatedBy(author);
        Club savedClub = clubRepository.save(club);

        ClubMember adminMember = new ClubMember();
        adminMember.setUser(author);
        adminMember.setClub(savedClub);
        adminMember.setUserRole(ClubRole.ADMIN);
        clubMemberRepository.save(adminMember);

        application.setAppStatus(ApplicationStatus.APPROVED);
        application.setReviewer(admin);
        applicationRepository.save(application);
    }

    @Transactional
    public Application createApplication(ApplicationCreateRequest request) {
        User currentUser = getCurrentUserOrThrow();
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заполните форму заявки");
        }

        String clubName = normalizeRequired(request.getClubName());
        String appDescription = normalizeRequired(request.getAppDescription());
        String reason = normalizeRequired(request.getReason());

        validate(clubName, appDescription, reason);

        if (clubRepository.existsByClubIgnoreCase(clubName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Клуб с таким названием уже существует");
        }
        if (applicationRepository.existsByAuthorAndAppStatus(currentUser, ApplicationStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "У вас уже есть активная заявка на рассмотрении");
        }

        Application application = new Application();
        application.setAuthor(currentUser);
        application.setClubName(clubName);
        application.setAppDescription(appDescription);
        application.setReason(reason);
        application.setAppStatus(ApplicationStatus.PENDING);
        return applicationRepository.save(application);
    }

    private static void validate(String clubName, String appDescription, String reason) {
        if (clubName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название клуба не может быть пустым");
        }
        if (clubName.length() > MAX_CLUB_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название клуба не должно превышать 100 символов");
        }
        if (appDescription == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Описание клуба не может быть пустым");
        }
        if (appDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Описание клуба не должно превышать 255 символов");
        }
        if (reason == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Причина создания клуба не может быть пустой");
        }
        if (reason.length() > MAX_REASON_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Причина не должна превышать 255 символов");
        }
    }

    private static String normalizeRequired(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private User getCurrentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof String s && "anonymousUser".equals(s)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }
        String email;
        if (principal instanceof UserDetails ud) {
            email = ud.getUsername();
        } else {
            email = String.valueOf(principal);
        }
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация"));
    }
}
