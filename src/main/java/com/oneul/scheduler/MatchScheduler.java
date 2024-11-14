// package com.oneul.scheduler;

// import java.util.concurrent.Executors;
// import java.util.concurrent.ScheduledExecutorService;
// import java.util.concurrent.TimeUnit;

// import javax.annotation.PostConstruct;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Component;

// import com.oneul.service.MatchService;

// @Component
// public class MatchScheduler {

// @Autowired
// private MatchService matchService;

// @PostConstruct
// public void startScheduler() {
// ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

// // scheduler.scheduleWithFixedDelay(() -> {
// // matchService.matchUsersUntilEmpty();
// // }, 0, 10, TimeUnit.SECONDS); // 10초마다 대기 테이블 확인 및 매칭 서비스 실행
// }
// }
