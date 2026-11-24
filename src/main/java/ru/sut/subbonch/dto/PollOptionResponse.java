package ru.sut.subbonch.dto;

public record PollOptionResponse(
        Integer voteId,
        String vote,
        int total,
        boolean selectedByCurrentUser
) {
}
