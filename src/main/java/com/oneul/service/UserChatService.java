package com.oneul.service;

import com.oneul.model.Diary_List;
import com.oneul.model.Member;
import com.oneul.config.WebSocketConfig;
import com.oneul.dto.*;

import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserChatService {
    @Value("${spring.datasource.url}")
    String URL;

    @Value("${spring.datasource.username}")
    String USERNAME;

    @Value("${spring.datasource.password}")
    String SQL_PASSWORD;

    @Autowired
    private WebSocketConfig webSocketConfig;

    Diary_List diaryList;

    public List<Map<String, Object>> getChat(int roomId) {
        List<Map<String, Object>> results = new ArrayList<>();
        String selectDiarySql = "SELECT id, user_id, read_flag, date, text FROM Oneul.Chats WHERE chat_id = ? ORDER BY date ASC";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement statement = connection.prepareStatement(selectDiarySql)) {

            connection.setAutoCommit(false); // 유사 트랜잭션모드
            statement.setInt(1, roomId);

            try (ResultSet resultSet = statement.executeQuery()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                while (resultSet.next()) {
                    int chatId = resultSet.getInt("id");
                    int fetchedUserId = resultSet.getInt("user_id");
                    boolean readFlag = resultSet.getBoolean("read_flag");
                    Timestamp date = resultSet.getTimestamp("date");
                    String text = resultSet.getString("text");
                    String formattedDate = dateFormat.format(date);

                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("chatId", chatId);
                    chatData.put("sender", fetchedUserId);
                    chatData.put("readFlag", readFlag);
                    chatData.put("date", formattedDate);
                    chatData.put("text", text);

                    results.add(chatData);
                }
            }

            connection.commit();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results; // Return the list of maps
    }

    public void send(int roomId, Member member, String message) {
        String insertChatSql = "INSERT INTO Oneul.Chats (chat_id, user_id, text, read_flag) VALUES (?, ?, ?, ?)";
        String getUserInfoSql = "SELECT m.user_id, m.nickname, m.image " +
                "FROM Oneul.Member m " +
                "JOIN ( " +
                "    SELECT CASE " +
                "               WHEN user_id != ? THEN user_id " +
                "               ELSE partner_id " +
                "           END AS other_id " +
                "    FROM Oneul.ChatList " +
                "    WHERE id = ? " +
                ") AS subquery ON m.user_id = subquery.other_id";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement insertStatement = connection.prepareStatement(insertChatSql);
                PreparedStatement getUserInfoStmt = connection.prepareStatement(getUserInfoSql)) {

            connection.setAutoCommit(false); // 트랜잭션 모드 활성화

            // Step 1: Insert the message into the Chats table
            insertStatement.setInt(1, roomId);
            insertStatement.setInt(2, member.getUser_id());
            insertStatement.setString(3, message);
            insertStatement.setBoolean(4, false);
            insertStatement.executeUpdate();

            // Step 2: Get other user's information
            getUserInfoStmt.setInt(1, member.getUser_id());
            getUserInfoStmt.setInt(2, roomId);

            try (ResultSet resultSet = getUserInfoStmt.executeQuery()) {
                if (resultSet.next()) {
                    int otherId = resultSet.getInt("user_id");
                    webSocketConfig.sendMessageNotification(member.getUser_id(), otherId, roomId, member.getImage(),
                            member.getNickname(), message);
                }
            }

            connection.commit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Object> getRoomData(Integer room, Integer id) {
        Map<String, Object> result = new HashMap<>();
        String selectRoomSql = "SELECT user_id, partner_id FROM Oneul.ChatList WHERE id = ?";
        String selectUserSql = "SELECT user_id, email, nickname, image FROM Oneul.Member WHERE user_id = ?";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement selectRoomStmt = connection.prepareStatement(selectRoomSql);
                PreparedStatement selectUserStmt = connection.prepareStatement(selectUserSql)) {

            connection.setAutoCommit(false); // 트랜잭션 모드 활성화

            // Step 1: Get room data
            selectRoomStmt.setInt(1, room);
            try (ResultSet resultSet = selectRoomStmt.executeQuery()) {
                if (resultSet.next()) {
                    int userId = resultSet.getInt("user_id");
                    int partnerId = resultSet.getInt("partner_id");

                    int otherUserId = (userId != id) ? userId : partnerId;

                    // Step 2: Get user information
                    selectUserStmt.setInt(1, otherUserId);
                    try (ResultSet userResultSet = selectUserStmt.executeQuery()) {
                        if (userResultSet.next()) {
                            result.put("user", userResultSet.getInt("user_id"));
                            result.put("name", userResultSet.getString("nickname"));
                            result.put("profile", userResultSet.getString("image"));
                        }
                    }
                }
            }

            connection.commit();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public boolean markAllMessagesAsRead(Integer room, int id) {
        String selectRoomSql = "SELECT user_id, partner_id FROM Oneul.ChatList WHERE id = ?";
        String updateSql = "UPDATE Oneul.Chats SET read_flag = 1 WHERE user_id = ? AND chat_id = ?";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement selectRoomStmt = connection.prepareStatement(selectRoomSql);
                PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {

            connection.setAutoCommit(false); // 트랜잭션 모드 활성화

            // Step 1: Get room data
            selectRoomStmt.setInt(1, room);
            try (ResultSet resultSet = selectRoomStmt.executeQuery()) {
                if (resultSet.next()) {
                    int userId = resultSet.getInt("user_id");
                    int partnerId = resultSet.getInt("partner_id");

                    // Determine the other user ID
                    int otherUserId = (userId != id) ? userId : partnerId;

                    // Step 2: Update messages as read
                    updateStatement.setInt(1, otherUserId);
                    updateStatement.setInt(2, room);

                    int rowsUpdated = updateStatement.executeUpdate();

                    if (rowsUpdated > 0) {
                        connection.commit(); // Commit if successful
                        return true;
                    } else {
                        connection.rollback(); // Rollback on failure
                        return false;
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
