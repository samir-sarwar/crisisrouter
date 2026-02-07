package com.crisisrouter.crisisRouter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // The "Radio Channels" the frontend can tune into
        // /topic = public broadcasts (e.g., "New Request created!")
        // /queue = private messages (e.g., "Your request was claimed")
        config.enableSimpleBroker("/topic", "/queue");

        // The prefix for messages sent FROM the frontend TO the server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // This is the URL the frontend uses to connect to the socket
        // setAllowedOriginPatterns("*") is needed for CORS (since you might run React on port 3000)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Fallback if the browser doesn't support raw WebSockets
    }
}