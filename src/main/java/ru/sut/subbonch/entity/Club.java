package ru.sut.subbonch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Клуб/сообщество. Таблица {@code Clubs}: название в колонке {@code club}
 * (unique), {@code slug} в схеме нет — маршруты идут по {@code club_id}.
 */
@Entity
@Table(name = "Clubs")
@Getter
@Setter
@NoArgsConstructor
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id")
    private Integer clubId;

    @Column(name = "club", nullable = false, unique = true, length = 100)
    private String club;

    @Column(name = "about", length = 255)
    private String about;

    @Column(name = "picture_url", length = 255)
    private String pictureUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_blocked", nullable = false)
    private boolean isBlocked = false;

    /** Автор/создатель клуба. Каскадного удаления нет: клуб не удаляется с пользователем. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    @Override
    public String toString() {
        return "Club{clubId=" + clubId + ", club='" + club + '\'' + ", isActive=" + isActive + '}';
    }
}
