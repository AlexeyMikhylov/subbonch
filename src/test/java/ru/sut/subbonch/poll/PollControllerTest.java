package ru.sut.subbonch.poll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import ru.sut.subbonch.controller.PollController;
import ru.sut.subbonch.service.PollService;

@ExtendWith(MockitoExtension.class)
class PollControllerTest {

    @Mock
    private PollService pollService;

    @InjectMocks
    private PollController pollController;

    @Test
    void vote_success_usesSafeReturnUrl() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);

        String view = pollController.vote(10, 100, "/clubs/5", request, ra);

        assertThat(view).isEqualTo("redirect:/clubs/5");
        verify(pollService).vote(10, 100);
        verify(ra).addFlashAttribute("successMessage", "Голос учтён.");
    }

    @Test
    void vote_unsafeReturnUrl_usesSameOriginReferer() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);
        when(request.getHeader("Referer")).thenReturn("https://example.com/clubs/5");
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("example.com");
        when(request.getServerPort()).thenReturn(443);

        String view = pollController.vote(10, 100, "https://evil.com", request, ra);

        assertThat(view).isEqualTo("redirect:https://example.com/clubs/5");
    }

    @Test
    void vote_noSafeRedirect_fallsBackToFeed() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);
        when(request.getHeader("Referer")).thenReturn("https://evil.com/x");

        String view = pollController.vote(10, 100, "//evil.com", request, ra);

        assertThat(view).isEqualTo("redirect:/feed");
    }

    @Test
    void vote_serviceError_addsFlashErrorAndRedirects() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Голосование закрыто"))
                .when(pollService).vote(10, 100);

        String view = pollController.vote(10, 100, "/clubs/5", request, ra);

        assertThat(view).isEqualTo("redirect:/clubs/5");
        verify(ra).addFlashAttribute("errorMessage", "Голосование закрыто");
    }
}
