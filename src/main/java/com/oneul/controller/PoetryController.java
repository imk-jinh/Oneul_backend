package com.oneul.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oneul.model.Member;
import com.oneul.service.HomeService;
import com.oneul.service.MemberService;
import com.oneul.service.PoetryService;

@Controller
@RequestMapping("/poetry")
public class PoetryController {

    private final MemberService memberService;
    private final HomeService homeService;
    private final PoetryService poetryService;

    @Autowired
    public PoetryController(MemberService memberService, HomeService homeService, PoetryService poetryService) {
        this.memberService = memberService;
        this.homeService = homeService;
        this.poetryService = poetryService;
    }

    @PostMapping("/getpoetry")
    public ResponseEntity<String> getPoetry(@CookieValue(value = "Authorization") String token,
            @RequestBody Map<String, Object> formData) {
        // year와 month를 안전하게 변환
        Integer year;
        Integer month;
        try {
            year = Integer.valueOf(formData.get("year").toString());
            month = Integer.valueOf(formData.get("month").toString());
        } catch (NumberFormatException | ClassCastException e) {
            return ResponseEntity.badRequest().body("Invalid year or month format");
        }

        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        Member member = memberService.getMember(token);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"fail to find member\", \"status\": \"500\"}");
        }

        int userId = member.getUser_id();

        List<Map<String, Object>> monthDiaries = homeService.getDiariesByMonth(userId, year, month);

        if (monthDiaries.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Diary not found");
        }

        List<Map<String, Object>> poetry = poetryService.getPoetryMonth(userId, year, month);

        if (poetry == null || poetry.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Poetry not found");
        }

        // Convert poetry list to JSON string
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonPoetry;
        try {
            jsonPoetry = objectMapper.writeValueAsString(poetry);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"Error processing poetry data\", \"status\": \"500\"}");
        }

        return ResponseEntity.ok(jsonPoetry);
    }
}
