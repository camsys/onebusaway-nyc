package org.onebusaway.api.web.interceptors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class BaseSocketHandler extends TextWebSocketHandler {

    private static Logger _log = LoggerFactory.getLogger(BaseSocketHandler.class);
    private final Map<String, CopyOnWriteArrayList<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    String keyIdentifier;

    public void setKeyIdentifier(String keyIdentifier){
        this.keyIdentifier = keyIdentifier;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String key = getKeyFromSession(session);
        if(key!=null) {
            sessions.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(session);
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        //todo: wrap session with ConcurrentWebSocketSessionDecorator
        super.handleTextMessage(session,message);
    }

    private String getKeyFromSession(WebSocketSession session) {
        // Extract the vehicle ID from the URI query parameter
        UriComponents uriComponents = UriComponentsBuilder.fromUri(session.getUri()).build();
        String key = uriComponents.getQueryParams().getFirst(keyIdentifier);
        return key;
    }


    public Set<String> getKeyList(){
        return sessions.keySet();
    }

    public void broadcastUpdate(TextMessage update, String key) {
        List<WebSocketSession> sessions = this.sessions.getOrDefault(key, new CopyOnWriteArrayList<>());
        Iterator<WebSocketSession> iterator = sessions.iterator();
        List<WebSocketSession> sessionsToRemove = new ArrayList<>();
        while (iterator.hasNext()) {
            WebSocketSession session = iterator.next();
            try {
                if (session.isOpen()) {
                    session.sendMessage(update);
                } else {
                    sessionsToRemove.add(session);
                }
            } catch (IOException e) {
                try {
                    session.close();
                } catch (IOException ex) {
                    _log.error("failed to close",session,ex);
                }
                sessionsToRemove.add(session);
            }
        }
        sessions.removeAll(sessionsToRemove);
    }
}

