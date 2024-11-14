package com.oneul.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.oneul.dto.UserDataResponse;

@Service
@RequiredArgsConstructor
public class MypageService {
    @Value("${spring.datasource.url}")
    String URL;

    @Value("${spring.datasource.username}")
    String USERNAME;

    @Value("${spring.datasource.password}")
    String SQL_PASSWORD;

    // 토큰 정보로 유저id 찾아낸 뒤, 이름, 프로필 이미지, 작성 일기수, 생성된 시 개수, 채팅 횟수 출력
    public UserDataResponse getUserInfo(String token) {
        String username = "";
        String user_img = "";
        int diaryCount = 0;
        int poem_count = 0;
        int chatCount = 0;
        int userid = 0;

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD)) {
            connection.setAutoCommit(false); // 유사 트랜잭션모드

            // token값으로 user_id를 추출
            String getUserId = "SELECT user_id FROM Oneul.Member WHERE token = ?";
            try (PreparedStatement statement = connection.prepareStatement(getUserId)) {
                statement.setString(1, token);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        userid = result.getInt("user_id");
                    }
                }
            }

            // 사용자의 이름과 프사 갖고오는 sql
            String getUserInfoSql = "SELECT nickname, image FROM Oneul.Member WHERE user_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(getUserInfoSql)) {
                statement.setInt(1, userid);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        username = result.getString("nickname");
                        user_img = result.getString("image");
                    }
                }
            }

            // 작성 다이어리 갯수 갖고오는 sql
            String getCountDiarysql = "SELECT COUNT(diary_id) AS diary_count FROM Oneul.DiaryList WHERE user_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(getCountDiarysql)) {
                statement.setInt(1, userid);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        diaryCount = result.getInt("diary_count");
                    }
                }
            }

            // 생성된 시의 갯수 갖고오는 sql
            String getCountPoemsql = "SELECT COUNT(id) AS poem_count FROM Oneul.Poems WHERE user_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(getCountPoemsql)) {
                statement.setInt(1, userid);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        poem_count = result.getInt("poem_count");
                    }
                }
            }

            // 채팅 매칭 횟수 갖고오는 sql
            String getChatMatchCountsql = "SELECT COUNT(id) AS chatCount FROM Oneul.ChatList WHERE user_id = ? OR partner_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(getChatMatchCountsql)) {
                statement.setInt(1, userid);
                statement.setInt(2, userid);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        chatCount = result.getInt("chatCount");
                    }
                }
            }

            UserDataResponse response = new UserDataResponse(username, user_img, diaryCount, poem_count, chatCount);
            System.out.println(response.toString());

            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getMyLanguage(String token) {
        int userid = 0;
        String language = "";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD)) {
            connection.setAutoCommit(false); // 유사 트랜잭션모드

            // token값으로 user_id를 추출
            String getUserId = "SELECT user_id FROM Oneul.Member WHERE token = ?";
            try (PreparedStatement statement = connection.prepareStatement(getUserId)) {
                statement.setString(1, token);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        userid = result.getInt("user_id");
                    }
                }
            }

            // 사용자의 설정된 언어 갖고옴
            String getLanguagesql = "SELECT language FROM Oneul.Member WHERE user_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(getLanguagesql)) {
                statement.setInt(1, userid);
                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        language = result.getString("language");
                    }
                }
            }

            return "{\"Language\": \"" + language + "\"}";
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean setViewMode(String token, String mode) {
        // mode가 "dark" 또는 "white"가 아닌 경우 오류 메시지 출력 후 종료
        if (!mode.equals("dark") && !mode.equals("white")) {
            System.err.println("Invalid mode: " + mode);
            return false;
        }

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD)) {
            String updateSql = "UPDATE Oneul.Member SET viewMode = ? WHERE token = ?";
            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setString(1, mode);
                statement.setString(2, token);

                int rowsAffected = statement.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
