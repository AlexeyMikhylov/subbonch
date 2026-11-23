package ru.sut.subbonch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Опрос, привязанный к посту. Таблица {@code Poll}; один опрос на пост
 * (unique {@code post_id}). Вопрос хранится в колонке {@code title}.
 */
@Entity
@Table(
        name = "Poll",
        uniqueConstraints = @UniqueConstraint(name = "UQ_Poll_post", columnNames = "post_id")
)
@Getter
@Setter
@NoArgsConstructor
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "poll_id")
    private Integer pollId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private Post post;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "is_multiple", nullable = false)
    private boolean isMultiple = false;

    /** После этой даты голосовать нельзя; {@code null} — опрос без дедлайна. */
    @Column(name = "closes_at")
    private LocalDateTime closesAt;

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
        return "Poll{pollId=" + pollId + ", title='" + title + '\'' + ", isMultiple=" + isMultiple + '}';
    }
}
