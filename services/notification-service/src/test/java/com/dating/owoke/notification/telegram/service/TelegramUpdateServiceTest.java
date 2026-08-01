package com.dating.owoke.notification.telegram.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class TelegramUpdateServiceTest {

    @Mock
    private BotCommandService commandService;

    @Mock
    private TelegramBotClient botClient;

    private TelegramUpdateService service;

    @BeforeEach
    void setUp() {
        service = new TelegramUpdateService(commandService, botClient);
    }

    @Test
    void acknowledgementFailureDoesNotReplayCommittedDecision() throws Exception {
        JsonNode update = callbackUpdate();
        when(commandService.handleDateProposalDecision(
                101L, 202L, 303L, 404L, "date:a:proposal:couple"))
                .thenReturn(new BotReply(303L, "Запрос отправлен"));
        doThrow(new IllegalStateException("Telegram callback query is too old"))
                .when(botClient).answerCallbackQuery("callback-1", "Запрос отправлен");

        assertThatCode(() -> service.process(update)).doesNotThrowAnyException();

        verify(commandService).handleDateProposalDecision(
                101L, 202L, 303L, 404L, "date:a:proposal:couple");
        verify(botClient).answerCallbackQuery("callback-1", "Запрос отправлен");
    }

    @Test
    void decisionFailureIsPropagatedSoPollingCanRetryTheUpdate() throws Exception {
        JsonNode update = callbackUpdate();
        when(commandService.handleDateProposalDecision(
                101L, 202L, 303L, 404L, "date:a:proposal:couple"))
                .thenThrow(new IllegalStateException("Database is unavailable"));

        assertThatThrownBy(() -> service.process(update))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database is unavailable");

        verifyNoInteractions(botClient);
    }

    private JsonNode callbackUpdate() throws Exception {
        return JsonMapper.builder().build().readTree("""
                {
                  "update_id": 101,
                  "callback_query": {
                    "id": "callback-1",
                    "from": {"id": 202},
                    "message": {"message_id": 404, "chat": {"id": 303}},
                    "data": "date:a:proposal:couple"
                  }
                }
                """);
    }
}
