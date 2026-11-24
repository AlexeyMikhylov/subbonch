package ru.sut.subbonch.dto;

import ru.sut.subbonch.entity.ClubRole;

public record ClubTeamMemberResponse(
        Integer userId,
        String firstName,
        String lastName,
        ClubRole userRole
) {
}
