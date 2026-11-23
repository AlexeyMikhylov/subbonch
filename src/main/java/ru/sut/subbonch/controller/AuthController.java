package ru.sut.subbonch.controller;

import java.net.URI;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
    public ResponseEntity<?> register(@ModelAttribute RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/login?registered"))
                    .build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email уже занят"));
        }
    }
}
