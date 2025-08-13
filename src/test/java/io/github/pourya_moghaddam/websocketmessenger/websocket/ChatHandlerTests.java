package io.github.pourya_moghaddam.websocketmessenger.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

class ChatHandlerTests {

    private ChatHandler chatHandler;
    private WebSocketSession session1;
    private WebSocketSession session2;

    @BeforeEach
    void setUp() {
        chatHandler = new ChatHandler();
        session1 = mock(WebSocketSession.class);
        session2 = mock(WebSocketSession.class);

        when(session1.getId()).thenReturn("1");
        when(session2.getId()).thenReturn("2");
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
    }

    private void mockUri(WebSocketSession session, String user) {
        String encodedUser = URLEncoder.encode(user, StandardCharsets.UTF_8);
        when(session.getUri()).thenReturn(URI.create("ws://localhost:8080/chat?user=" + encodedUser));
    }

    @Test
    void afterConnectionEstablished_sendsWelcomeAndBroadcastsJoinMessage() throws Exception {
        mockUri(session1, "user1");
        chatHandler.afterConnectionEstablished(session1);
        verify(session1).sendMessage(new TextMessage("Welcome to this chat!"));

        mockUri(session2, "user2");
        chatHandler.afterConnectionEstablished(session2);
        verify(session2).sendMessage(new TextMessage("Welcome to this chat!"));

        verify(session1).sendMessage(new TextMessage("user2 has joined the chat."));
        verify(session1, never()).sendMessage(new TextMessage("user1 has joined the chat."));
        verify(session2, never()).sendMessage(new TextMessage("user2 has joined the chat."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    void afterConnectionEstablished_withBlankUser_closesSession(String user) throws Exception {
        mockUri(session1, user);
        chatHandler.afterConnectionEstablished(session1);

        verify(session1).close(CloseStatus.BAD_DATA.withReason("Username is required and must not be blank."));
    }

    @Test
    void handleTextMessage_broadcastsToAllSessions() throws Exception {
        mockUri(session1, "user1");
        mockUri(session2, "user2");

        chatHandler.afterConnectionEstablished(session1);
        chatHandler.afterConnectionEstablished(session2);

        TextMessage message = new TextMessage("hello");
        chatHandler.handleTextMessage(session1, message);

        verify(session2).sendMessage(new TextMessage("user1: hello"));
        verify(session1, never()).sendMessage(new TextMessage("user1: hello"));
    }

    @Test
    void afterConnectionClosed_broadcastsLeaveMessage() throws Exception {
        mockUri(session1, "user1");
        mockUri(session2, "user2");

        chatHandler.afterConnectionEstablished(session1);
        chatHandler.afterConnectionEstablished(session2);

        chatHandler.afterConnectionClosed(session1, CloseStatus.NORMAL);

        verify(session2).sendMessage(new TextMessage("user1 has left the chat."));
    }
}
