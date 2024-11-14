// package com.oneul.Interceptor;

// import javax.servlet.http.Cookie;
// import javax.servlet.http.HttpServletRequest;
// import javax.servlet.http.HttpServletResponse;

// import org.springframework.web.servlet.HandlerInterceptor;

// import lombok.extern.slf4j.Slf4j;

// @Slf4j
// public class LoginInterceptor implements HandlerInterceptor {

// @Override
// public boolean preHandle(HttpServletRequest request, HttpServletResponse
// response, Object handler)
// throws Exception {
// String loginURI = "/member/goKaKaoLogin"; // 나중에 제대로 수정해야함
// String requestURI = request.getRequestURI();
// Cookie[] cookies = request.getCookies();
// if (cookies != null) {
// for (Cookie cookie : cookies) {
// log.info("[interceptor] cookie : " + cookie.getName() + " , " +
// cookie.getValue());
// if ("Authorization".equals(cookie.getName())) {
// log.info("[interceptor] login : " + requestURI);
// return true;
// }
// }
// }
// log.info("[interceptor] logout : " + requestURI);
// /*
// * 로그인 완성되면 아래 코드로 수정하기
// * response.sendRedirect(loginURI);//로그아웃된 경우 로그인 페이지로
// * return false;
// */
// return true;
// }
// }