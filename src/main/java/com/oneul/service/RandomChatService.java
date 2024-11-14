package com.oneul.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oneul.dto.ListChatResponse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class RandomChatService {
    @Value("${spring.datasource.url}")
    String URL;

    @Value("${spring.datasource.username}")
    String USERNAME;

    @Value("${spring.datasource.password}")
    String SQL_PASSWORD;

    public List<ListChatResponse> getList(String token) {
        String sql = "SELECT * FROM Oneul.ChatList WHERE token = ?"; // Example query
        List<ListChatResponse> chatList = null; // Assuming you will populate this later

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement statement = connection.prepareStatement(sql)) {

            connection.setAutoCommit(false); // 트랜잭션 시작
            statement.setString(1, token);

            try (ResultSet resultSet = statement.executeQuery()) {
                // 여기서 ResultSet을 통해 데이터를 읽고 ListChatResponse 리스트를 만듭니다.
                // 예시로만 추가합니다. 실제 로직에 맞게 수정해주세요.
                while (resultSet.next()) {
                    // chatList에 결과 추가 로직 구현 (예시로 추가)
                    // chatList.add(new ListChatResponse(...));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return chatList;
    }
}
