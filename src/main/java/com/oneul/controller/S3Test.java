package com.oneul.controller;

import com.oneul.service.S3TestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
@Service
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/test")
public class S3Test {

    private final S3TestService testService;

    @PostMapping("/upload")
    public ResponseEntity<JSONObject> handleFileUpload(@RequestParam("images") List<MultipartFile> images) {
        JSONObject jsonObject = new JSONObject();

        List<String> imageUrls = testService.saveImgS3(images);

        jsonObject.put("status", "200");
        jsonObject.put("message", "성공");
        jsonObject.put("imageUrls", imageUrls); // 이미지 URL을 추가
        return ResponseEntity.ok().body(jsonObject);
    }
}
