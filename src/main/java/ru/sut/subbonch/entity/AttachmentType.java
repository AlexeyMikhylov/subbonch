package ru.sut.subbonch.entity;

/**
 * Тип вложения поста/комментария. В БД ограничен CHECK-константой:
 * допустимы только {@code IMAGE}, {@code FILE}, {@code VIDEO}.
 */
public enum AttachmentType {
    IMAGE,
    FILE,
    VIDEO
}
