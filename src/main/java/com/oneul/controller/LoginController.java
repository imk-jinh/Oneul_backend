package com.oneul.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.oneul.service.KaKaoService;
import com.oneul.service.MemberService;
import com.oneul.model.*;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/member")
public class LoginController {

	private final MemberService memberService;
	// @Value("${spring.datasource.url}")
	// String URL;

	@Value("${my.loginUrl}")
	String LOGIN_URL;

	@Value("${my.dev}")
	String Dev;

	@Value("${kakao.restApiKey}")
	String restApiKey;

	@Value("${kakao.redirectUri}")
	String redirectUri;

	@Autowired
	KaKaoService ks;

	@GetMapping("/kakao")
	public String getCI(@RequestParam String code, Model model, HttpServletResponse response) throws IOException {
		String p = "/test.html";
		String access_token = ks.getToken(code);
		Map<String, Object> userInfo = ks.getUserInfo(access_token);
		System.out.println("[kakao] " + userInfo);

		List<Object> result = emailCheck(userInfo, true);

		String cookie = (String) result.get(0); // 첫 번째 값(인덱스 0)은 토큰입니다.
		boolean success = (boolean) result.get(1); // 두 번째 값(인덱스 1)은 성공 여부입니다.

		if (!cookie.startsWith("error")) {
			String cookieValue = String.format("Authorization=%s; Path=/; Secure; HttpOnly;", cookie);

			if (Dev.equals("dev")) {
				response.addHeader("set-cookie", cookieValue);
				p = success ? "redirect:http://" + LOGIN_URL + "/choiceLanguage"
						: "redirect:http://" + LOGIN_URL + "/home";
			} else {
				response.addHeader("set-cookie", cookieValue + " Domain=.oneuldiary.com");
				p = success ? "redirect:https://" + LOGIN_URL + "/choiceLanguage"
						: "redirect:https://" + LOGIN_URL + "/home";
			}
		} else {
			p = "redirect:https://" + LOGIN_URL + "?error=cookie_creation_failed";
		}
		return p;
	}

	public List<Object> emailCheck(Map<String, Object> userInfo, boolean isKakao) {// @RequestBody String inputjson
		/*
		 * {age_range=, nickname=z_on, id=2986897736, email=xks642@kakao.com}
		 * userInfo.get("nickname")
		 */
		List<Object> resultList = new ArrayList<>();
		JSONObject jsonObject = new JSONObject();
		ObjectMapper objectMapper;
		JsonNode jsonNode;
		String email = (String) userInfo.get("email");
		System.out.println("email@@@@@@@@@@@@@@@@@@@@@");
		System.out.println(email);
		System.out.println("email@@@@@@@@@@@@@@@@@@@@@");
		String nickname = (String) userInfo.get("nickname");
		String age_range = (String) userInfo.get("age_range");
		String ka_go;
		String token;
		String isMem;
		String re = "error";
		Boolean sig = false;

		try {

			isMem = memberService.checkEmail(email);// checkEmail로 변경하기

			if (isMem == "no") { // 신규
				if (isKakao) {
					ka_go = "kakao";
				} else {
					ka_go = "google";
				}
				if (!signup(email, nickname, age_range, ka_go)) {
					resultList.add(re);
					resultList.add(sig);
					return resultList;
				}

				sig = true;
			} else if (isMem == "error") {
				resultList.add(re);
				resultList.add(sig);
				return resultList;
			}

			token = memberService.getToken(email);

			if (token == "error") {
				resultList.add(re);
				resultList.add(sig);
				return resultList;
			}

		} catch (Exception e) {
			e.printStackTrace();
			resultList.add(re);
			resultList.add(sig);
			return resultList;
		}

		re = token;
		resultList.add(re);
		resultList.add(sig);
		return resultList;
	}

	// 회원가입
	public boolean signup(String email, String name, String age, String ka_go) {
		try {
			boolean kakao = false;
			boolean google = false;

			switch (ka_go) {
				case "kakao":
					kakao = true;
					break;
				case "google":
					google = true;
					break;
			}

			Member member = new Member();
			member.setEmail(email);
			member.setNickname(name);
			member.setKakao(kakao);
			member.setGoogle(google);
			member.setAge(age);

			return memberService.addMember(member);

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@GetMapping("/logout")
	public ResponseEntity<JSONObject> logout(@CookieValue(value = "Authorization") Cookie cookie,
			HttpServletResponse resp) {
		JSONObject resultObj = new JSONObject();
		String token = cookie.getValue();
		boolean re;

		cookie = new Cookie("Authorization", null);
		cookie.setValue(null);
		cookie.setPath("/"); // 쿠키 경로설정 필수
		cookie.setMaxAge(0);
		resp.addCookie(cookie);

		re = memberService.logout(token);
		if (!re) {
			resultObj.put("status", "500");
			resultObj.put("message", "error-DB");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultObj);
		}

		resultObj.put("status", "200");
		resultObj.put("message", "success");

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultObj);

	}

}
