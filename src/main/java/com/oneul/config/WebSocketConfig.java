package com.oneul.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    @Lazy
    private SimpMessagingTemplate messagingTemplate; // 필드 주입과 Lazy 로딩을 함께 사용

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*"); // SockJS 지원 추가
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    public void sendMatchNotification(int userId, int partnerId, long chatroomid) {
        messagingTemplate.convertAndSendToUser(String.valueOf(userId),
                "/queue/match", chatroomid);
        messagingTemplate.convertAndSendToUser(String.valueOf(partnerId),
                "/queue/match", chatroomid);
    }

    public void sendMessageNotification(int sender, int userId, int roomId, String img, String name, String message) {
        try {
            // JSON 객체를 생성하기 위해 Map 사용
            Map<String, Object> payload = new HashMap<>();
            payload.put("roomId", roomId);
            payload.put("img", img);
            payload.put("text", message);
            payload.put("name", name);
            payload.put("sender", sender);

            // ObjectMapper를 사용하여 Map을 JSON 문자열로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // JSON 문자열을 소켓으로 전송
            messagingTemplate.convertAndSendToUser(String.valueOf(userId),
                    "/queue/message", jsonPayload);

        } catch (Exception e) {
            // 예외 처리
            e.printStackTrace();
        }
    }

}
