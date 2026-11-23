package ru.sut.subbonch.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClubEditRequest {

    private String club;
    private String about;
    private String pictureUrl;
}
