package ru.sut.subbonch.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PostCreateRequest {

    private String title;
    private String body;
    private String imageUrl;
}
