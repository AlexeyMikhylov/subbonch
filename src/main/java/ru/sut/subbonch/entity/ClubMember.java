package ru.sut.subbonch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Участие пользователя в клубе с ролью. Таблица {@code Club_members};
 * уникальность пары {@code (user_id, club_id)} уже обеспечена индексом
 * {@code UQ_user_club} в БД.
 */
@Entity
@Table(
        name = "Club_members",
        uniqueConstraints = @UniqueConstraint(name = "UQ_user_club", columnNames = {"user_id", "club_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class ClubMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false, length = 50)
    private ClubRole userRole = ClubRole.MEMBER;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "ClubMember{id=" + id + ", userRole=" + userRole + '}';
    }
}
