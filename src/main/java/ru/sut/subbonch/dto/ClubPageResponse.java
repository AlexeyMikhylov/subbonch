package ru.sut.subbonch.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ClubPageResponse(
        Integer clubId,
        String club,
        String about,
        String pictureUrl,
        LocalDateTime createdAt,
        long membersCount,
        String userRoleInClub,
        List<ClubTeamMemberResponse> team
) {
}
