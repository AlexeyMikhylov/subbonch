package ru.sut.subbonch.entity;

/**
 * Роль пользователя внутри клуба (таблица {@code Club_members.user_role}).
 * В БД хранится строкой в верхнем регистре, поэтому {@code EnumType.STRING}.
 */
public enum ClubRole {
    MEMBER,
    MODERATOR,
    ADMIN
}
