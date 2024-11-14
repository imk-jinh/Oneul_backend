package com.oneul.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

import com.oneul.dto.ListChatResponse;
import com.oneul.service.RandomChatService;

@CrossOrigin
@RequiredArgsConstructor
@Controller
@RequestMapping("/RandomChat")
public class RandomChatController {
    private final RandomChatService randomChatService;

    // 채팅방 리스트 조회
    @GetMapping("/getList")
    public ResponseEntity<Object> getList(@CookieValue(value = "Authorization") String token) {
        // token값이 존재하지 않거나 토큰이 비어있을때
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        List<ListChatResponse> response = randomChatService.getList(token);

        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"server error\", \"status\": \"500\"}");
        }
    }

}
