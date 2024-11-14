package com.oneul.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActionService {

    private final String API_KEY = "${chatGPT.api.key}";
    private final String ENDPOINT = "${chatGPT.api.url}";
    private final String RECOMMENDATION_PROMPT = "아래의 일기를 읽고, 사용자가 오늘 하면 좋을 행동을 무조건 4가지 추천해주고 해당 행동의 간략한 설명도 해줘. 각 행동은 명확하게 구분되어야 하며, '추천 행동 n:' 형식으로 작성해줘.\n일기:\n";

    @Value("${spring.datasource.url}")
    private String URL;

    @Value("${spring.datasource.username}")
    private String USERNAME;

    @Value("${spring.datasource.password}")
    private String SQL_PASSWORD;

    @Autowired
    private DiaryService diaryService;

    public List<Map<String, String>> getActionRecommend(String token) {
        Integer diaryId = diaryService.getTodayDiaryID(token);
        List<Map<String, String>> recommendations = new ArrayList<>();
        
        if (diaryId == null) {
            throw new RuntimeException("오늘 작성된 일기가 없습니다.");
        }
    
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD)) {
            // 데이터베이스에서 추천 행동을 조회
            String sql = "SELECT action_name, description FROM Oneul.ActionRecommend WHERE diary_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, diaryId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        Map<String, String> actionMap = new HashMap<>();
                        actionMap.put("action_name", resultSet.getString("action_name"));
                        actionMap.put("description", resultSet.getString("description"));
                        recommendations.add(actionMap);
                    }
                }
            }
            
            // 추천 행동이 이미 존재하면 반환
            if (!recommendations.isEmpty()) {
                return recommendations;
            }
    
            // 추천 행동이 없으면 새로 생성
            recommendations = getRecommendations(token);
    
            // 생성된 추천 행동을 데이터베이스에 저장
            String insertSql = "INSERT INTO Oneul.ActionRecommend (diary_id, action_name, description) VALUES (?, ?, ?)";
            try (PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                for (Map<String, String> recommendation : recommendations) {
                    insertStatement.setInt(1, diaryId);
                    insertStatement.setString(2, recommendation.get("action_name"));
                    insertStatement.setString(3, recommendation.get("description"));
                    insertStatement.addBatch();
                }
                insertStatement.executeBatch();
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("추천 행동을 처리하는 중 오류가 발생했습니다.", e);
        }
    
        return recommendations;
    }
    

    public List<Map<String, String>> getRecommendations(String token) {
        Integer diaryId = diaryService.getTodayDiaryID(token);

        if (diaryId == null) {
            throw new RuntimeException("오늘 작성된 일기가 없습니다.");
        }

        String diaryText = diaryService.getDiaryContentById(diaryId);

        if (diaryText == null || diaryText.isEmpty()) {
            throw new RuntimeException("일기 내용 가져오기 실패!");
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.set("Content-Type", "application/json");

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", RECOMMENDATION_PROMPT + diaryText);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode messagesJson = objectMapper.valueToTree(messages);

        String requestJson = "{"
                + "\"model\": \"gpt-3.5-turbo\","
                + "\"messages\": " + messagesJson.toString()
                + "}";

        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        ResponseEntity<String> response = restTemplate.exchange(ENDPOINT, HttpMethod.POST, entity, String.class);

        return parseRecommendations(response.getBody());
    }

    private List<Map<String, String>> parseRecommendations(String responseBody) {
        List<Map<String, String>> recommendations = new ArrayList<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode choices = root.path("choices");
            if (choices.isArray()) {
                String recommendationText = choices.get(0).path("message").path("content").asText();

                String[] actions = recommendationText.split("(?=추천 행동 [0-9]+:)"); 
                int count = 0;

                for (String action : actions) {
                    if (count >= 4) break; 
                    int colonIndex = action.indexOf('\n');
                    if (colonIndex != -1) {
                        String actionName = action.substring(0, colonIndex).trim();
                        String description = action.substring(colonIndex + 1).trim();

                        Map<String, String> actionMap = new HashMap<>();
                        actionMap.put("action_name", actionName);
                        actionMap.put("description", description);
                        recommendations.add(actionMap);
                        count++;
                    }
                }

                while (recommendations.size() < 4) {
                    Map<String, String> actionMap = new HashMap<>();
                    actionMap.put("action_name", "기본 추천 행동 " + (recommendations.size() + 1));
                    actionMap.put("description", "기본 설명 " + (recommendations.size() + 1));
                    recommendations.add(actionMap);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return recommendations;
    }
}
