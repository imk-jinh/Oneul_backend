package com.oneul.controller;

import com.oneul.service.ActionService;
import com.oneul.service.DiaryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/action")
public class ActionController {

    @Autowired
    private ActionService actionService;
    // @Autowired
    // private DiaryService diaryService;

    @PostMapping("/makeAction")
    public ResponseEntity<?> makeAction(@CookieValue(value = "Authorization") String token) {
        try {
            // // 토큰을 사용하여 오늘의 일기 텍스트 조회
            // String diaryText = diaryService.getTodayDiary(token);

            // if (diaryText == null || diaryText.isEmpty()) {
            //     return ResponseEntity.status(404).body("오늘 작성된 일기가 없습니다.");
            // }

            // 일기 텍스트를 기반으로 추천 행동 4가지 생성
            List<Map<String, String>> recommendations = actionService.getActionRecommend(token);

            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("추천 행동을 생성하는 중 오류가 발생했습니다.");
        }
    }
}
