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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Голос пользователя за конкретный вариант опроса. Таблица {@code Ballot}.
 * Ссылки на {@code Poll} нет — опрос определяется через {@code Vote.poll_id};
 * правило «один голос в одиночном опросе» проверяется в сервисе.
 */
@Entity
@Table(
        name = "Ballot",
        uniqueConstraints = @UniqueConstraint(name = "UQ_user_option", columnNames = {"user_id", "vote_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class Ballot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ballot_id")
    private Integer ballotId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Override
    public String toString() {
        return "Ballot{ballotId=" + ballotId + '}';
    }
}
