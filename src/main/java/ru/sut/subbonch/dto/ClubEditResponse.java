package ru.sut.subbonch.dto;

import java.time.LocalDateTime;

public record ClubEditResponse(
        Integer clubId,
        String club,
        String about,
        String pictureUrl,
        LocalDateTime updatedAt
) {
}
