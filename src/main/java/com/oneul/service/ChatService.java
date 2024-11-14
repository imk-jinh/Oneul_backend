package com.oneul.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final DiaryService diaryService;
    private final PoetryService poetryService;

    @Value("${spring.datasource.url}")
    String URL;

    @Value("${spring.datasource.username}")
    String USERNAME;

    @Value("${spring.datasource.password}")
    String SQL_PASSWORD;

    private final String API_KEY = "${chatGPT.api.key}";
    private final String ENDPOINT = "${chatGPT.api.url}";

    private final String chat_template = "일기에 작성될 적당한 주제가 나오면 대화 마무리를 한다.\n" +
            "필수 : 한번의 답변에 한가지 질문만 한다. 여러가지 질문하지 않는다. 반말해야한다 반말로 친근하게 한다. 너는 상대방과 제일 친한 친구다! 친근한 말투로 대화해야 한다.\n" +
            "0. 상대방의 상황에 있어서 상대방의 마음이 어땠는지 속마음을 묻는 질문을 종종한다.\n" +
            "1. 한국어를 사용해서 대화한다.\n" +
            "2. 첫 대답은 오늘 어땠는지 질문한다.\n" +
            "3. 오늘 하루에 대한 질문을 이어간다.\n" +
            "4. 대화할 때 여러가지 질문하지 않고 하나만 질문한다.\n" +
            "5. 공감할 때를 제외하고 스스로의 경험에 대해서 이야기하지 않는다.\n" +
            "6. 같은 주제에 대한 질문은 3번 정도만 하고 다른 질문으로 넘어간다.\n" +
            "7. 대답하기 편한 개방형 질문하기.";

    private final String summarize_template = "필수 : Ai는 대화의 보조이고 주요 관점은 사용자의 글이야. 모든 말은 반말로 한다.\n" +
            "0. 친한 친구가 대신 일기를 작성해주는 느낌으로 대화내용을 기반으로 사용자의 일기 작성한다.\n" +
            "1. \"사용자 :\", \"AI : \"는 태그일 뿐 일기에 포함하지 않는다.\n" +
            "대화내용을 기반으로 일기를 작성해라";

    private final String poem_template = "재치있고 창의적인 시를 최대한 길게 작성해줘\n" +
            "첫 번째 줄은 제목으로 출력해줘";

    private final String emotion_template = "필수 : 주어진 일기를 대상으로 아래 7가지 감정을 예측 분류해 꼭! 아래 7가지중 하나만을 선택하고 답변은 감정 단어 하나만 출력해\n"
            +
            "기쁨\n" +
            "슬픔\n" +
            "분노\n" +
            "두려움\n" +
            "혐오\n" +
            "놀람\n" +
            "중립\n";

    public String callChatGPT(String message, List<Map<String, String>> history, String template) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.set("Content-Type", "application/json");

        // 시스템 메시지 추가
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", template);

        // 사용자 메시지 추가
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);

        // 이전 대화 히스토리를 JSON 형식으로 변환
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage);

        for (Map<String, String> entry : history) {
            Map<String, String> messageEntry = new HashMap<>();
            if ("mine".equals(entry.get("sender"))) {
                messageEntry.put("role", "user");
            } else if ("theirs".equals(entry.get("sender"))) {
                messageEntry.put("role", "assistant");
            }
            messageEntry.put("content", entry.get("text"));
            messages.add(messageEntry);
        }
        messages.add(userMessage);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode messagesJson = objectMapper.valueToTree(messages);

        String requestJson = "{"
                + "\"model\": \"gpt-3.5-turbo\","
                + "\"messages\": " + messagesJson.toString()
                + "}";

        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
        ResponseEntity<String> response = restTemplate.exchange(ENDPOINT, HttpMethod.POST, entity, String.class);

        return response.getBody();
    }

    public String callChatGPT_emotion(String message, String diary, String template) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.set("Content-Type", "application/json");

        // 시스템 메시지 추가
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", template);

        // 사용자 메시지 추가
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", diary);

        // 이전 대화 히스토리를 JSON 형식으로 변환
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode messagesJson = objectMapper.valueToTree(messages);

        String requestJson = "{"
                + "\"model\": \"gpt-3.5-turbo\","
                + "\"messages\": " + messagesJson.toString()
                + "}";

        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
        ResponseEntity<String> response = restTemplate.exchange(ENDPOINT, HttpMethod.POST, entity, String.class);

        return response.getBody();
    }

    public String generateResponse(String message, Object history) {
        List<Map<String, String>> chatHistory = (List<Map<String, String>>) history;
        return callChatGPT(message, chatHistory, chat_template);
    }

    public String generateDailyResponse(Object history) {
        List<Map<String, String>> chatHistory = (List<Map<String, String>>) history;
        String openAiResponse = callChatGPT("위 대화 내용을 기반으로 사용자 직접쓴 일기처럼 일기를 작성해줘", chatHistory, summarize_template);

        return openAiResponse;
    }

    public String callChatGPTForPoem(String diary, String template) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.set("Content-Type", "application/json");

        // 시스템 메시지 추가
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", template);

        // 사용자 메시지 추가
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", diary);

        // 메시지를 JSON 형식으로 변환
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode messagesJson = objectMapper.valueToTree(messages);

        String requestJson = "{"
                + "\"model\": \"gpt-3.5-turbo\","
                + "\"messages\": " + messagesJson.toString()
                + "}";

        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
        ResponseEntity<String> response = restTemplate.exchange(ENDPOINT, HttpMethod.POST, entity, String.class);

        return response.getBody();
    }

    public String generateDailyPoem(String token, int user_id) {
        Object[] diaryData = diaryService.getTodayDiaryNId(token);
        int diary_id = 0;
        String diary = null;
        if (diaryData != null) {
            diary_id = (int) diaryData[0]; // Extract the diary ID
            diary = (String) diaryData[1]; // Extract the diary text

            // Now you can use the id and diary variables as needed
        } else {
            // Handle the case where no diary is found
            System.out.println("No diary entry found for today.");
        }

        String openAiResponse = callChatGPTForPoem(diary, poem_template);

        // Parse the OpenAI response
        ObjectMapper objectMapper = new ObjectMapper();
        String title = "";
        String content = "";

        try {
            JsonNode root = objectMapper.readTree(openAiResponse);
            JsonNode messageNode = root.path("choices").get(0).path("message").path("content");

            if (messageNode != null) {
                String[] lines = messageNode.asText().split("\n", 2);
                if (lines.length > 0) {
                    title = lines[0].trim(); // First line is the title
                    content = lines.length > 1 ? lines[1].trim() : ""; // The rest is the content
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing the response";
        }
        if (diary_id != 0) {
            poetryService.insertPoem(user_id, diary_id, title, content);
        }
        // Return or use title and content as needed
        return openAiResponse; // Or return a custom object containing title and content
    }

    public String generateDailyResponseWithEmotion(String diary) {
        while (true) {
            String openAiResponse = callChatGPT_emotion("", diary, emotion_template);

            System.out.println("AI Response: " + openAiResponse); // 로그 출력

            // 감정 매핑
            Map<String, Integer> emotionMap = new HashMap<>();
            emotionMap.put("기쁨", 1);
            emotionMap.put("슬픔", 2);
            emotionMap.put("분노", 3);
            emotionMap.put("두려움", 4);
            emotionMap.put("혐오", 5);
            emotionMap.put("놀람", 6);
            emotionMap.put("중립", 7);

            for (Map.Entry<String, Integer> entry : emotionMap.entrySet()) {
                if (openAiResponse.contains(entry.getKey())) {
                    return entry.getValue().toString();
                }
            }
        }
    }
}
