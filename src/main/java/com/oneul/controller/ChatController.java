package com.oneul.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneul.model.Member;
import com.oneul.service.ChatService;
import com.oneul.service.MemberService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Controller
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final MemberService memberService;

    @PostMapping("/gen")
    public ResponseEntity<String> generateChat(@RequestBody Map<String, Object> request) {
        String userMessage = (String) request.get("message");
        Object history = request.get("history");
        String responseJson = chatService.generateResponse(userMessage, history);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseJson);
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing response");
        }
    }

    @PostMapping("/makeDaily")
    public ResponseEntity<String> generateDaily(@RequestBody Map<String, Object> request) {
        Object history = request.get("history");
        String response = chatService.generateDailyResponse(history);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response);
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing response");
        }
    }
    

    @PostMapping("/predictEmotion")
    public ResponseEntity<String> predictEmotion(@RequestBody String diary) {
        String emotion = chatService.generateDailyResponseWithEmotion(diary);
        return ResponseEntity.ok(emotion);
    }

    @GetMapping("/genpoem")
    public ResponseEntity<String> getPoetry(@CookieValue(value = "Authorization") String token) {
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        System.out.println(token);
        Member member;
        member = memberService.getMember(token);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"fail to find member\", \"status\": \"500\"}");
        }
        try {
            String peom = chatService.generateDailyPoem(token, member.getUser_id());
            return ResponseEntity.ok(peom);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing response");
        }

    }

}
