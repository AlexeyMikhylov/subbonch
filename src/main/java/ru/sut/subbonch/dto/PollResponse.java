package ru.sut.subbonch.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PollResponse(
        Integer pollId,
        String title,
        boolean isMultiple,
        LocalDateTime closesAt,
        boolean closed,
        boolean alreadyVoted,
        List<PollOptionResponse> options
) {
}
