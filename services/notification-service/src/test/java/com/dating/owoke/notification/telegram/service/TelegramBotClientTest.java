package com.dating.owoke.notification.telegram.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.dating.owoke.notification.telegram.configuration.TelegramBotProperties;
import com.dating.owoke.notification.telegram.dto.TelegramInlineButton;

class TelegramBotClientTest {

    private MockRestServiceServer server;
    private TelegramBotClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TelegramBotClient(
                builder,
                new TelegramBotProperties(true, "polling", "test-token", ""));
    }

    @Test
    void localActionUrlIsOmittedSoTelegramCanDeliverTheText() {
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    assertThat(body).contains("\"chat_id\":123", "\"text\":\"Test message\"")
                            .doesNotContain("reply_markup", "localhost");
                })
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":42}}", MediaType.APPLICATION_JSON));

        assertThat(client.send(123L, "Test message", "http://localhost:5173/dates/1")).isEqualTo("42");
        server.verify();
    }

    @Test
    void publicActionUrlIsSentAsInlineButton() {
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    assertThat(body).contains("reply_markup", "https://owoke.example/dates/1");
                })
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":43}}", MediaType.APPLICATION_JSON));

        assertThat(client.send(123L, "Test message", "https://owoke.example/dates/1")).isEqualTo("43");
        server.verify();
    }

    @Test
    void callbackButtonsAreSentEvenWhenWebsiteUrlIsLocal() {
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    String body = ((MockClientHttpRequest) request).getBodyAsString();
                    assertThat(body)
                            .contains("reply_markup", "callback_data", "date:a:", "date:d:")
                            .doesNotContain("localhost");
                })
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":44}}", MediaType.APPLICATION_JSON));

        assertThat(client.send(
                123L,
                "Test message",
                "http://localhost:5173/dates/1",
                List.of(
                        new TelegramInlineButton("Принять", "date:a:proposal:couple"),
                        new TelegramInlineButton("Отклонить", "date:d:proposal:couple"))))
                .isEqualTo("44");
        server.verify();
    }

    @Test
    void callbackQueryIsAcknowledged() {
        server.expect(requestTo("https://api.telegram.org/bottest-token/answerCallbackQuery"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> assertThat(((MockClientHttpRequest) request).getBodyAsString())
                        .contains("callback_query_id", "callback-1", "Запрос отправлен"))
                .andRespond(withSuccess("{\"ok\":true,\"result\":true}", MediaType.APPLICATION_JSON));

        client.answerCallbackQuery("callback-1", "Запрос отправлен");
        server.verify();
    }

    @Test
    void telegramErrorDescriptionIsPreservedForDiagnostics() {
        server.expect(requestTo("https://api.telegram.org/bottest-token/sendMessage"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"ok\":false,\"description\":\"Bad Request: wrong HTTP URL\"}"));

        assertThatThrownBy(() -> client.send(123L, "Test message", "https://owoke.example/dates/1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("400 BAD_REQUEST")
                .hasMessageContaining("wrong HTTP URL");
        server.verify();
    }
}
