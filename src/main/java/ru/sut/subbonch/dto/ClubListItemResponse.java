package ru.sut.subbonch.dto;

public record ClubListItemResponse(
        Integer clubId,
        String club,
        String about,
        String pictureUrl,
        long membersCount
) {
}
