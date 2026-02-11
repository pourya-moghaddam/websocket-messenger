package io.github.pourya_moghaddam.websocketmessenger.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatHandler extends TextWebSocketHandler {

    private final Map<WebSocketSession, String> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        var usernameOpt = getUsername(session);
        if (usernameOpt.isEmpty() || usernameOpt.get().isBlank()) {
            session.close(CloseStatus.BAD_DATA.withReason("Username is required and must not be blank."));
            return;
        }
        var username = usernameOpt.get();
        sessions.put(session, username);
        log.info("Client connected: {} as {}", session.getId(), username);
        broadcastSystemMessage(String.format("%s has joined the chat.", username), session);
        session.sendMessage(new TextMessage("Welcome to this chat!"));
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        var username = sessions.get(session);
        if (username == null) return;
        log.info("Received from {}: {}", username, message.getPayload());
        broadcastUserMessage(String.format("%s: %s", username, message.getPayload()), session);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        var username = sessions.remove(session);
        if (username == null) return;
        log.info("Client disconnected: {}", username);
        broadcastSystemMessage(String.format("%s has left the chat.", username), null);
    }

    private Optional<String> getUsername(WebSocketSession session) {
        return Optional.ofNullable(session.getUri())
                .map(uri -> UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("user"))
                .filter(name -> !name.trim().isEmpty());
    }

    private void broadcastSystemMessage(String message, WebSocketSession exclude) throws IOException {
        for (var session : sessions.keySet()) {
            if (session.isOpen() && !session.equals(exclude)) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    private void broadcastUserMessage(String message, WebSocketSession sender) throws IOException {
        for (var session : sessions.keySet()) {
            if (session.isOpen() && session != sender) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }
}
