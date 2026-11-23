package ru.sut.subbonch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Вариант ответа опроса. Таблица {@code Vote}: текст варианта — в колонке
 * {@code vote}, счётчик — в {@code total}.
 */
@Entity
@Table(name = "Vote")
@Getter
@Setter
@NoArgsConstructor
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    private Integer voteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(name = "vote", nullable = false, length = 255)
    private String vote;

    /** Денормализованный счётчик голосов; поддерживается сервисным слоем. */
    @Column(name = "total", nullable = false)
    private int total = 0;

    @Override
    public String toString() {
        return "Vote{voteId=" + voteId + ", total=" + total + '}';
    }
}
