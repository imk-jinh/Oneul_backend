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
import com.oneul.service.*;

import lombok.RequiredArgsConstructor;
// import oracle.jdbc.proxy.annotation.Post;

// @Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/diary")
public class DiaryController {
    private final DiaryService diaryService;
    private final S3TestService testService;

    @GetMapping("/getDetailDiary")
    public ResponseEntity<Object> getDetailDiary(@CookieValue(value = "Authorization") String token) {
        // token값이 존재하지 않거나 토큰이 비어있을때
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        List<DiaryDetailResponse> response = diaryService.getDetailDiary(token);

        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"server error\", \"status\": \"500\"}");
        }
    }

    @PostMapping("/addDiary")
    public ResponseEntity<String> addDiary(
            @CookieValue(value = "Authorization") String token,
            @RequestParam("Title") String title,
            @RequestParam("Date") Date date,
            @RequestParam("Tag") List<String> tags,
            @RequestParam("Emotion") String emotion,
            @RequestParam("Text") String text,
            @RequestPart(value = "imgs", required = false) List<MultipartFile> imgs) {

        // 필수 입력항목이 누락된 경우 예외 처리

        List<String> filePath2 = testService.saveImgS3(imgs);

        // 성공적으로 처리 완료시
        boolean result = diaryService.addDiary(token, title, date, tags, emotion, text, filePath2);

        if (result == true) {
            return ResponseEntity.ok("200: success");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"server error\", \"status\": \"500\"}");
        }
    }

    @PostMapping("/updateDiary")
    public ResponseEntity<String> updateDiary(@RequestBody DiaryUpdateRequest request) {

        // 필수 입력항목이 누락된 경우 예외 처리

        // 성공적으로 처리 완료시
        boolean result = diaryService.updateDiary(request);

        if (result == true) {
            return ResponseEntity.ok("200: success");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"server error\", \"status\": \"500\"}");
        }
    }
}