package com.oneul.controller;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.oneul.dto.*;
import com.oneul.model.Member;
import com.oneul.service.*;

import lombok.RequiredArgsConstructor;
// import oracle.jdbc.proxy.annotation.Post;

// @Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/userchat")
public class UserChatController {
    private final UserChatService userChatService;
    private final MemberService memberService;

    @PostMapping("/get")
    public ResponseEntity<Object> getchatlist(@CookieValue(value = "Authorization") String token,
            @RequestBody Map<String, Object> request) {

        String roomStr = (String) request.get("room");
        Integer room = Integer.parseInt(roomStr);

        // token값이 존재하지 않거나 토큰이 비어있을때
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        try {
            Member member = memberService.getMember(token);

            if (member == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid token", "status", "401"));
            }

            List<Map<String, Object>> response = userChatService.getChat(room);

            if (response != null && !response.isEmpty()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
        } catch (Exception e) {
            // Log the exception for debugging purposes
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Server error", "status", "500"));
        }
    }

    @PostMapping("/send")
    public ResponseEntity<Object> send(@CookieValue(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> request) {
        // Check if the token is missing or empty
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized", "status", "401"));
        }

        // Extract room and message from the request body
        String roomStr = (String) request.get("room");
        String message = (String) request.get("message");

        if (roomStr == null || message == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid input data", "status", "400"));
        }

        try {
            Integer room = Integer.parseInt(roomStr); // Convert roomStr to Integer

            Member member = memberService.getMember(token);
            if (member == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid token", "status", "401"));
            }

            // Call the service to send the message
            userChatService.send(room, member, message);

            return ResponseEntity.ok(Map.of("message", "Message sent successfully"));

        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid room number format", "status", "400"));
        } catch (Exception e) {
            // Log the exception for debugging purposes
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Server error", "status", "500"));
        }
    }

    @PostMapping("/getRoom")
    public ResponseEntity<Object> getRoom(@CookieValue(value = "Authorization") String token,
            @RequestBody Map<String, Object> request) {

        String roomStr = (String) request.get("room");
        Integer room = Integer.parseInt(roomStr);

        // token값이 존재하지 않거나 토큰이 비어있을때
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        try {
            Member member = memberService.getMember(token);

            if (member == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid token", "status", "401"));
            }

            Map<String, Object> response = userChatService.getRoomData(room, member.getUser_id());

            if (response != null && !response.isEmpty()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
        } catch (Exception e) {
            // Log the exception for debugging purposes
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Server error", "status", "500"));
        }
    }

    @PostMapping("/readAllMessages")
    public ResponseEntity<Object> readAllMessages(@CookieValue(value = "Authorization") String token,
            @RequestBody Map<String, Object> request) {

        String roomStr = (String) request.get("room");
        Integer room = Integer.parseInt(roomStr);

        // token값이 존재하지 않거나 토큰이 비어있을때
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Unauthorized", "status", "401"));
        }

        try {
            // 토큰으로 사용자 정보 가져오기
            Member member = memberService.getMember(token);

            if (member == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid token", "status", "401"));
            }

            // 사용자가 채팅방의 모든 메시지를 읽었다고 처리
            boolean success = userChatService.markAllMessagesAsRead(room, member.getUser_id());

            if (success) {
                // 성공적으로 처리된 경우 응답
                return ResponseEntity.ok(Map.of("message", "All messages marked as read", "status", "200"));
            } else {
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(Map.of("message", "No messages to mark as read", "status", "204"));
            }
        } catch (Exception e) {
            // 예외 발생 시 서버 에러 응답
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Server error", "status", "500"));
        }
    }

}