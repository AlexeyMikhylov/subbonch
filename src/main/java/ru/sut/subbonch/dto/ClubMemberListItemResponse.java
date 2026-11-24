package ru.sut.subbonch.dto;

import java.time.LocalDateTime;
import ru.sut.subbonch.entity.ClubRole;

public record ClubMemberListItemResponse(
        Integer userId,
        String firstName,
        String lastName,
        String email,
        ClubRole userRole,
        LocalDateTime joinedAt
) {
}
