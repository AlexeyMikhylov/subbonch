package ru.sut.subbonch.entity;

/**
 * Статус заявки на создание клуба (таблица {@code Applications.app_status}).
 * CHECK-ограничение БД допускает только эти три значения.
 */
public enum ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
