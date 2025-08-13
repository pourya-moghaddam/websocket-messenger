package io.github.pourya_moghaddam.websocketmessenger.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTests {

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Mock
    private WebSocketHandlerRegistry registry;

    @Test
    void registerWebSocketHandlers_registersChatHandler() {
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigins", new String[]{"*"});
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        when(registry.addHandler(any(ChatHandler.class), anyString())).thenReturn(registration);

        webSocketConfig.registerWebSocketHandlers(registry);

        ArgumentCaptor<ChatHandler> handlerCaptor = ArgumentCaptor.forClass(ChatHandler.class);
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);

        verify(registry).addHandler(handlerCaptor.capture(), pathCaptor.capture());
        assertNotNull(handlerCaptor.getValue());
        assertEquals("/chat", pathCaptor.getValue());

        verify(registration).setAllowedOrigins("*");
    }
}
