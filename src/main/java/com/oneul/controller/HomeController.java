package com.oneul.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneul.service.HomeService;
import com.oneul.service.MemberService;
import com.oneul.model.*;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/home")
public class HomeController {

    private final MemberService memberService;
    private final HomeService homeService;

    @GetMapping("/gethomeuserinfo")
    public ResponseEntity<String> getHomeUserInfo(@CookieValue(value = "Authorization") String token) {

        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        Member member = memberService.getMember(token);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"fail to find member\", \"status\": \"500\"}");
        }

        int diarySum = homeService.getDiarySum(member.getUser_id()); // 사용자의 일기 수를 가져옵니다.

        // 회원 정보가 정상적으로 불러와졌을 때 해당 닉네임 값을 JSON 형태로 반환합니다.
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // user_id와 nickname을 함께 JSON 형태로 반환
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user_id", member.getUser_id());
            responseData.put("nickname", member.getNickname());
            responseData.put("img", member.getImage());
            responseData.put("diary_count", diarySum);
            responseData.put("language", member.getLanguage());
            responseData.put("email", member.getEmail());
            String json = objectMapper.writeValueAsString(responseData);
            return ResponseEntity.ok(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"server error\", \"status\": \"500\"}");
        }
    }

    @PostMapping("/getmonthtag")
    public ResponseEntity<List<String>> getMonthTag(@CookieValue(value = "Authorization") String token,
            @RequestBody Map<String, Object> formData) {
        // @RequestParam("year") int year,
        // @RequestParam("month") int month

        int year = (int) formData.get("year"); // int로 형변환
        int month = (int) formData.get("month"); // int로 형변환

        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.emptyList());
        }

        Member member = memberService.getMember(token);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }

        int userId = member.getUser_id();

        // 수정된 부분: 사용자 ID와 함께 호출
        List<String> hashtags = homeService.getHashtagsByUserIdAndMonth(userId, year, month);

        return ResponseEntity.ok(hashtags);
    }

    @PostMapping("/getmonthdiary")
    public ResponseEntity<List<Map<String, Object>>> getMonthDiary(@CookieValue(value = "Authorization") String token,
            @RequestBody Map<String, Object> formData) {

        int year = (int) formData.get("year"); // int로 형변환
        int month = (int) formData.get("month"); // int로 형변환

        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.emptyList());
        }
        Member member = memberService.getMember(token);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }

        int userId = member.getUser_id();
        List<Map<String, Object>> monthDiaries = homeService.getDiariesByMonth(userId, year, month);

        return ResponseEntity.ok(monthDiaries);
    }

    @GetMapping("/gettagdiary")
    public ResponseEntity<List<Map<String, Object>>> getTagDiary(@CookieValue(value = "Authorization") String token,
            @RequestBody Map<String, Object> formData) {
        // @RequestParam("hashtag") String hashtag

        String hashtag = (String) formData.get("hashtag");

        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.emptyList());
        }
        Member member = memberService.getMember(token);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }

        int userId = member.getUser_id();
        List<Integer> diaryIds = homeService.getDiaryIdByHashtag(userId, hashtag);

        if (diaryIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.emptyList());
        }

        List<Map<String, Object>> tagDiaries = new ArrayList<>();

        for (int diaryId : diaryIds) {
            Map<String, Object> diaryInfo = homeService.getDiaryInfoById(diaryId);
            tagDiaries.add(diaryInfo);
        }

        return ResponseEntity.ok(tagDiaries);
    }
}
