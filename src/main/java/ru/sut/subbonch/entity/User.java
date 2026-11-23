package ru.sut.subbonch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Пользователь платформы. Таблица {@code Users} — существующая legacy-схема:
 * PK {@code user_id} (Integer identity), флаг админа сайта {@code is_app_admin}.
 */
@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "about", length = 255)
    private String about;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_blocked", nullable = false)
    private boolean isBlocked = false;

    @Column(name = "block_reason", length = 255)
    private String blockReason;

    @Column(name = "is_app_admin", nullable = false)
    private boolean isAppAdmin = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Email — единственный идентификатор входа, поэтому храним его в нижнем
     * регистре: иначе {@code UQ_Users_email} не поймает дубль вида
     * {@code Ivan@mail.ru} / {@code ivan@mail.ru}.
     */
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Пароль в логи не попадает. */
    @Override
    public String toString() {
        return "User{userId=" + userId
                + ", email='" + email + '\''
                + ", firstName='" + firstName + '\''
                + ", lastName='" + lastName + '\''
                + ", isActive=" + isActive
                + ", isBlocked=" + isBlocked
                + ", isAppAdmin=" + isAppAdmin
                + '}';
    }
}
