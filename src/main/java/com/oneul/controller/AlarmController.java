package com.oneul.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

import com.oneul.dto.AlarmResponse;
import com.oneul.service.*;

@RequiredArgsConstructor
@Controller
@RequestMapping("/Alarm")
public class AlarmController {
    private final AlarmService alarmService;

    @GetMapping("/getMyAlarm")
    public ResponseEntity<Object> getMyAlarm(@CookieValue(value = "Authorization") String token) {
        // token값이 존재하지 않거나 토큰이 비어있을때
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        List<AlarmResponse> responses = alarmService.getMyAlarm(token);

        if (responses != null) {
            return ResponseEntity.ok(responses);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"server error\", \"status\": \"500\"}");
        }
    }

    @GetMapping("/readAlarm")
    public ResponseEntity<Object> readAlarm(
            @CookieValue(value = "Authorization") String token,
            @RequestParam(value = "alarmId") Long alarmId) {

        // token값이 존재하지 않거나 토큰이 비어있을 때
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        try {
            // AlarmService를 통해 알람을 읽음 상태로 변경
            boolean isUpdated = alarmService.readAlarm(token, alarmId);

            // 알람 읽음 상태 변경에 성공한 경우
            if (isUpdated) {
                return ResponseEntity.ok("{\"message\": \"Alarm marked as read\", \"status\": \"200\"}");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"message\": \"Alarm not found\", \"status\": \"404\"}");
            }
        } catch (Exception e) {
            // 예외 발생 시 500 에러 응답
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"Server error\", \"status\": \"500\"}");
        }
    }

    @PostMapping("/readAll")
    public ResponseEntity<Object> readAllAlarms(
            @CookieValue(value = "Authorization") String token,
            @RequestBody List<Long> alarmIds) {

        // token 값이 없을 경우 처리
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        try {
            boolean success = alarmService.markAllAsRead(token, alarmIds);

            if (success) {
                return ResponseEntity.ok("{\"message\": \"All alarms marked as read\", \"status\": \"200\"}");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"message\": \"Alarms not found\", \"status\": \"404\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"Server error\", \"status\": \"500\"}");
        }
    }
}
