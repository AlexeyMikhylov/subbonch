package ru.sut.subbonch.dto;

import java.util.ArrayList;
import java.util.List;
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

    /** Вопрос опроса (Poll.title). Если пустой и опрос не запрошен — пост создаётся без опроса. */
    private String pollTitle;

    /** Разрешить выбор нескольких вариантов. */
    private Boolean pollMultiple;

    /** Дедлайн опроса в формате datetime-local (yyyy-MM-dd'T'HH:mm). */
    private String pollClosesAt;

    /** Варианты ответа опроса (Vote.vote). */
    private List<String> pollOptions = new ArrayList<>();
}
