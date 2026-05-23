package com.co.jarvis.config;

import com.co.jarvis.config.security.JwtProvider;
import com.co.jarvis.dto.presale.WsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PreSaleWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    public PreSaleWebSocketHandler(JwtProvider jwtProvider, ObjectMapper objectMapper) {
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractToken(session);
        if (token == null || jwtProvider.getTokenUser(token) == null) {
            log.warn("WebSocket connection rejected — invalid or missing token. Session: {}", session.getId());
            try {
                session.close(CloseStatus.NOT_ACCEPTABLE);
            } catch (Exception ignored) {}
            return;
        }
        sessions.add(session);
        log.info("WebSocket client connected. Session: {}, total sessions: {}", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket client disconnected. Session: {}, total sessions: {}", session.getId(), sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error. Session: {}", session.getId(), exception);
        sessions.remove(session);
    }

    public void broadcast(WsMessage<?> message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Error serializing WebSocket message", e);
            return;
        }

        int sent = 0;
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                    sent++;
                } catch (Exception e) {
                    log.warn("Error sending WebSocket message to session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
        log.info("WebSocket broadcast sent to {} / {} sessions", sent, sessions.size());
    }

    private String extractToken(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery();
        if (query == null) return null;
        return Arrays.stream(query.split("&"))
                .filter(p -> p.startsWith("token="))
                .map(p -> p.substring(6))
                .findFirst()
                .map(t -> URLDecoder.decode(t, StandardCharsets.UTF_8))
                .orElse(null);
    }
}
