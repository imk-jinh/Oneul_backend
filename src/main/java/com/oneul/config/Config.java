package com.oneul.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 로그인 인터셉터

// import com.oneul.Interceptor.LoginInterceptor;

import org.springframework.core.convert.converter.Converter;
import java.text.ParseException;
import java.text.SimpleDateFormat;

@Configuration
public class Config implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://localhost");
        config.addAllowedOrigin("https://oneuldiary.com");
        config.addAllowedOrigin("https://www.oneuldiary.com");
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        config.addExposedHeader("set-cookie"); // 브라우저에 쿠키 설정을 노출합니다.

        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(0);
        return bean;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToSqlDateConverter());
    }

    private static class StringToSqlDateConverter implements Converter<String, java.sql.Date> {

        @Override
        public java.sql.Date convert(String source) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                java.util.Date parsedDate = dateFormat.parse(source);
                return new java.sql.Date(parsedDate.getTime());
            } catch (ParseException e) {
                throw new IllegalArgumentException(
                        "Invalid date format. Please provide the date in yyyy-MM-dd format.");
            }
        }
    }

    // 로그인 인터셉터
    // @Override
    // public void addInterceptors(InterceptorRegistry registry) {
    // registry.addInterceptor(new LoginInterceptor())
    // .order(1)
    // .addPathPatterns("/**");
    // // .excludePathPatterns("/member/**","/AD/**"); // 제외할 경우
    // }
}
