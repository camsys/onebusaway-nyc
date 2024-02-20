package org.onebusaway.api.web.config;

import org.onebusaway.api.web.interceptors.BaseSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    //todo: enable stomp
    @Bean
    public BaseSocketHandler demoWebSocketHandler() {
        BaseSocketHandler baseWebSocketHandler = new BaseSocketHandler();
        baseWebSocketHandler.setKeyIdentifier("topic");
        return baseWebSocketHandler;
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(demoWebSocketHandler(), "/demo-websocket")
                .addInterceptors(new HttpSessionHandshakeInterceptor())
                .setAllowedOrigins("*")
                .withSockJS();

    }

}