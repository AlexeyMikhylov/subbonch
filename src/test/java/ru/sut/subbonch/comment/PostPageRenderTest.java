package ru.sut.subbonch.comment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import ru.sut.subbonch.controller.CommentController;
import ru.sut.subbonch.dto.CommentResponse;
import ru.sut.subbonch.dto.PostListItemResponse;
import ru.sut.subbonch.repository.PostRepository;
import ru.sut.subbonch.service.CommentService;
import ru.sut.subbonch.service.PostService;

/**
 * Проверяет, что шаблон страницы поста реально рендерит дерево комментариев
 * на любую глубину (фрагмент {@code fragments/comments.html} рекурсивный).
 */
class PostPageRenderTest {

    private MockMvc mockMvc;
    private PostService postService;

    @BeforeEach
    void setUp() {
        postService = mock(PostService.class);
        CommentService commentService = mock(CommentService.class);
        PostRepository postRepository = mock(PostRepository.class);
        CommentController controller = new CommentController(commentService, postRepository, postService);

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        viewResolver.setCharacterEncoding("UTF-8");

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void postPage_rendersRepliesOfAnyDepth() throws Exception {
        CommentResponse thirdLevel = comment(3, "Третий уровень", List.of());
        CommentResponse secondLevel = comment(2, "Второй уровень", List.of(thirdLevel));
        CommentResponse firstLevel = comment(1, "Первый уровень", List.of(secondLevel));

        PostListItemResponse post = new PostListItemResponse(
                10, "Пост", "Текст", LocalDateTime.now(), false,
                1, "Иван", "Иванов", null,
                5, "Клуб", null, null, null, null,
                List.of(firstLevel));

        when(postService.getPost(10)).thenReturn(post);
        when(postService.getCurrentUserRolesByClubIds(any())).thenReturn(Map.of(5, "MEMBER"));

        mockMvc.perform(get("/posts/10"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Первый уровень")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Второй уровень")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Третий уровень")))
                // два блока комментариев-ответов могут быть только вложенными друг в друга
                .andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
                        "(?s).*class=\"comment-replies\".*class=\"comment-replies\".*")));
    }

    private static CommentResponse comment(Integer id, String text, List<CommentResponse> replies) {
        return new CommentResponse(id, 1, "Иван", "Иванов", null, "MEMBER",
                text, LocalDateTime.now(), null, replies);
    }
}
