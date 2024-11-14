package com.oneul.controller;

import org.hibernate.mapping.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneul.service.DiaryService;
import com.oneul.service.HomeService;
import com.oneul.service.MatchService;
import com.oneul.service.MemberService;

import lombok.RequiredArgsConstructor;

import com.oneul.model.*;

@RequiredArgsConstructor
@Controller
@RequestMapping("/match")
public class MatchContoroller {

    private final MemberService memberService;
    private final DiaryService diaryService;
    private final MatchService matchService;
    private final HomeService homeService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/matchstart")
    public ResponseEntity<String> getHomeUserInfo(@CookieValue(value = "Authorization") String token) {
        System.out.println("matchstart start");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        try {
            Integer diary = diaryService.getTodayDiaryID(token);
            if (diary == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"message\": \"No diary found for today\", \"status\": \"404\"}");
            }

            Integer member = memberService.getMember(token).getUser_id();

            matchService.addProgress(member, diary);

            // Return a successful response with some meaningful data
            String responseText = "Matching progress started for member ID " + member;
            return ResponseEntity.ok("{\"text\": \"" + responseText + "\"}");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"Server error\", \"status\": \"500\"}");
        }
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(ChatMessage chatMessage) {
        System.out.println("메시지 전송 시도");

        if (chatMessage.getRecipient().isEmpty() || chatMessage.getRecipient().equals("public")) {
            System.out.println("공개 메시지 전송");
            messagingTemplate.convertAndSend("/topic/public", chatMessage);
        } else {
            System.out.println("개인 메시지 전송");
            System.out.println("받는 사람: " + chatMessage.getRecipient());
            System.out.println("메시지 내용: " + chatMessage.getContent());

            // 특정 사용자게 메시지 보내기
            messagingTemplate.convertAndSendToUser(chatMessage.getRecipient(), "/queue/messages",
                    chatMessage);
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelMatching(@CookieValue(value = "Authorization") String token) {
        System.out.println("cancelMatching start");
        try {
            // Cancel matching for the user
            int userId = homeService.getUserIdByToken(token);
            matchService.removeFromQueue(userId);

            return ResponseEntity.ok("{\"message\": \"Matching canceled successfully\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"Server error\", \"status\": \"500\"}");
        }
    }

}
