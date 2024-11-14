package com.oneul.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.oneul.dto.UserDataResponse;
import com.oneul.service.MypageService;

@CrossOrigin
@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/myPage")
public class MypageController {
    private final MypageService myPageService;

    @GetMapping("/getInfo")
    public ResponseEntity<Object> getUserInfo(@CookieValue(value = "Authorization") String token) {

        // token값이 존재하지 않거나 토큰이 비어있을때
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }
        UserDataResponse response = myPageService.getUserInfo(token);

        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"server error\", \"status\": \"500\"}");
        }
    }

    @GetMapping("/getMyLanguage")
    public ResponseEntity<String> getMyLanguage(@CookieValue(value = "Authorization") String token) {
        // token값이 존재하지 않거나 토큰이 비어있을때
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        String result = myPageService.getMyLanguage(token);

        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"server error\", \"status\": \"500\"}");
        }
    }

    @PostMapping("/setViewMode")
    public ResponseEntity<Object> setViewMode(@CookieValue(value = "Authorization") String token,
            @RequestParam("mode") String mode) {
        // token값이 존재하지 않거나 토큰이 비어있을때
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        // white 또는 dark 모드 정상 적용 됐는지 true/false
        boolean result = myPageService.setViewMode(token, mode);

        if (result == true) {
            return ResponseEntity.ok("200: success");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"server error\", \"status\": \"500\"}");
        }
    }
}