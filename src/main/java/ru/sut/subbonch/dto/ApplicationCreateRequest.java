package ru.sut.subbonch.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApplicationCreateRequest {
    private String clubName;
    private String appDescription;
    private String reason;
}
