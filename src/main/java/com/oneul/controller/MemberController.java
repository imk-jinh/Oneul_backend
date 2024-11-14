package com.oneul.controller;

import java.nio.file.Files;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// import com.oneul.service.KaKaoService;
import com.oneul.service.MemberService;
import com.oneul.service.S3TestService;
import com.oneul.model.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@CrossOrigin
@RequiredArgsConstructor
@Controller
@Slf4j
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final S3TestService testService;

    @GetMapping("/CheckMember")
    public ResponseEntity<String> CheckMember(
            @CookieValue(value = "Authorization") String token) {

        try {
            Member result = memberService.getMember(token);

            // Check if the member exists
            if (result != null) {
                return ResponseEntity.ok("200: success");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"message\": \"server error\", \"status\": \"500\"}");
            }
        } catch (Exception e) {
            // Log the exception and return a 500 response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"server error\", \"status\": \"500\"}");
        }

    }

    @PostMapping("/setName")
    public ResponseEntity<String> setMemberName(
            @CookieValue(value = "Authorization") String token,
            @RequestParam("Name") String name) {
        // if (token == null || token.isEmpty()) {
        // return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        // .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        // }

        boolean result = memberService.setNickname(token, name);

        if (result == true) {
            return ResponseEntity.ok("200: success");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"server error\", \"status\": \"500\"}");
        }
    }

    @PostMapping("/choiceLanguage")
    public ResponseEntity<String> setLanguage(@CookieValue(value = "Authorization") String token,
            @RequestParam("Language") String language) {
        // token값이 존재하지 않거나 토큰이 비어있을때
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
        }

        // 언어 선택이 성공적으로 수행됐는지 true, false
        boolean result = memberService.setLanguage(token, language);

        if (result == true) {
            return ResponseEntity.ok("200: success");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\": \"server error\", \"status\": \"500\"}");
        }
    }

    // 사용자 프로필 이미지 설정 처리
    @PostMapping("/setImg")
    public ResponseEntity<String> setImg(@CookieValue(value = "Authorization") String token,
            @RequestPart("imgs") MultipartFile img) {
        try {
            // token값이 존재하지 않거나 토큰이 비어있을때
            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
            }

            String filePath = testService.saveOneImgS3(img);

            // 이미지 DB 정상 저장 True, False
            boolean result = memberService.saveImagePathToDB(token, filePath);

            if (result) {
                return ResponseEntity.ok("File uploaded successfully and path saved to database.");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to save file path to database.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        }
    }

    @PostMapping("/setImgNull")
    public ResponseEntity<String> setImgNull(@CookieValue(value = "Authorization") String token) {
        try {
            // token값이 존재하지 않거나 토큰이 비어있을때
            if (token == null || token.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"message\": \"Unauthorized\", \"status\": \"401\"}");
            }

            String filePath = "https://oneulimg.s3.ap-northeast-2.amazonaws.com/basicImg.svg";

            // 이미지 DB 정상 저장 True, False
            boolean result = memberService.saveImagePathToDB(token, filePath);

            if (result) {
                return ResponseEntity.ok("File uploaded successfully and path saved to database.");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to save file path to database.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        }
    }

}