package ru.sut.subbonch.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfileEditRequest {

    private String firstName;
    private String lastName;
    private String about;
    private String profilePictureUrl;
}
