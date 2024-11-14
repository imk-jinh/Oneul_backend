package com.oneul.service;

import com.oneul.model.Diary_List;
import com.oneul.model.Member;
// import com.oneul.model.Hashtag;
import com.oneul.dto.*;

import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.Date;
// import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
// import java.sql.SQLException;
// import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ChatListService {
    @Value("${spring.datasource.url}")
    String URL;

    @Value("${spring.datasource.username}")
    String USERNAME;

    @Value("${spring.datasource.password}")
    String SQL_PASSWORD;

    Diary_List diaryList;

    public List<Map<String, Object>> getList(int userId) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
            connection.setAutoCommit(false); // 유사 트랜잭션모드

            // Step 1: Retrieve relevant diary entries
            String selectDiarySql = "SELECT id, user_id, partner_id FROM Oneul.ChatList WHERE (user_id = ? OR partner_id = ?) AND end_flag = 0";
            statement = connection.prepareStatement(selectDiarySql);
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            resultSet = statement.executeQuery();

            // Iterate through the diary list results
            while (resultSet.next()) {
                int chatId = resultSet.getInt("id");
                int fetchedUserId = resultSet.getInt("user_id");
                int fetchedPartnerId = resultSet.getInt("partner_id");

                // Identify the ID that does not match the provided userId
                int otherUserId = (fetchedUserId == userId) ? fetchedPartnerId : fetchedUserId;

                // Step 2: Retrieve the last chat message for this chat room
                String selectChatSql = "SELECT * FROM Oneul.Chats WHERE chat_id = ? ORDER BY date DESC LIMIT 1";
                PreparedStatement chatStatement = connection.prepareStatement(selectChatSql);
                chatStatement.setInt(1, chatId);

                ResultSet chatResultSet = chatStatement.executeQuery();

                String text = null;
                String formattedDate = null;
                Boolean flag = true;
                if (chatResultSet.next()) {
                    text = chatResultSet.getString("text");
                    Integer text_user = chatResultSet.getInt("user_id");

                    if (otherUserId == text_user) {
                        flag = chatResultSet.getBoolean("read_flag");
                    }

                    Timestamp timestamp = chatResultSet.getTimestamp("date");

                    // Define the date format pattern you want
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    if (timestamp != null) {
                        formattedDate = dateFormat.format(timestamp); // Format the timestamp to the desired pattern
                    }

                }

                // Step 3: Retrieve member information
                String selectMemberSql = "SELECT * FROM Oneul.Member WHERE user_id = ?";
                PreparedStatement memberStatement = connection.prepareStatement(selectMemberSql);
                memberStatement.setInt(1, otherUserId);
                ResultSet memberResultSet = memberStatement.executeQuery();

                String img = null;
                String name = null;
                if (memberResultSet.next()) {
                    img = memberResultSet.getString("image");
                    name = memberResultSet.getString("nickname");
                }

                // Add data to the result list
                Map<String, Object> chatData = new HashMap<>();
                chatData.put("otherUserId", otherUserId);
                chatData.put("chatId", chatId);
                chatData.put("text", text);
                chatData.put("img", img);
                chatData.put("name", name);
                chatData.put("date", formattedDate);
                chatData.put("flag", flag);

                results.add(chatData);

                // Close the statements and result sets for chat and member queries
                chatStatement.close();
                chatResultSet.close();
                memberStatement.close();
                memberResultSet.close();
            }

            connection.commit();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (connection != null) {
                    connection.rollback(); // 롤백
                }
            } catch (Exception rollbackEx) {
                rollbackEx.printStackTrace();
            }
        } finally {
            try {
                if (resultSet != null)
                    resultSet.close();
                if (statement != null)
                    statement.close();
                if (connection != null)
                    connection.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return results; // Return the list of maps
    }

    public boolean addDiary(String token, String title, Date date, List<String> tags, String emotion, String text,
            List<String> filePath2) {
        Connection connection;
        PreparedStatement getUserIdStatement = null;
        PreparedStatement insertDiaryStatement = null;
        PreparedStatement insertTagStatement = null;
        PreparedStatement insertPhotoStatement = null;
        ResultSet useridResult = null; // 실행 결과 받아오는 변수
        ResultSet diaryIdResult = null;

        try {
            connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
            connection.setAutoCommit(false);// 유사 트랜잭션모드

            // token값으로 user_id를 추출
            String getUserIdSql = "SELECT user_id FROM Oneul.Member WHERE token = ?";
            getUserIdStatement = connection.prepareStatement(getUserIdSql);
            getUserIdStatement.setString(1, token);

            useridResult = getUserIdStatement.executeQuery();

            int userid = 0;
            if (useridResult.next()) {
                userid = useridResult.getInt("user_id");
            }

            // 다이어리 추가 SQL문
            String insertDiarySql = "INSERT INTO Oneul.DiaryList (user_id, title, text, date, emotion) VALUES (?, ?, ?, ?, ?)";
            insertDiaryStatement = connection.prepareStatement(insertDiarySql, Statement.RETURN_GENERATED_KEYS);
            insertDiaryStatement.setInt(1, userid);
            insertDiaryStatement.setString(2, title);
            insertDiaryStatement.setString(3, text);
            insertDiaryStatement.setDate(4, date);
            insertDiaryStatement.setString(5, emotion);

            int rowsAffected = insertDiaryStatement.executeUpdate();
            if (rowsAffected == 0) { // 삽입 실패 시 롤백.
                connection.rollback();
                return false;
            }

            // 새로 추가된 다이어리의 diary_id 가져오기
            diaryIdResult = insertDiaryStatement.getGeneratedKeys();
            int diaryId = 0;
            if (diaryIdResult.next()) {
                diaryId = diaryIdResult.getInt(1);
            }

            // Hashtags 테이블에 태그 추가
            String insertTagSql = "INSERT INTO Oneul.Hashtags (diary_id, text) VALUES (?, ?)";
            insertTagStatement = connection.prepareStatement(insertTagSql);
            for (String tag : tags) {
                insertTagStatement.setInt(1, diaryId);
                insertTagStatement.setString(2, tag);
                insertTagStatement.addBatch(); // 배치 작업으로 추가
            }
            insertTagStatement.executeBatch(); // 배치 실행

            // Photos 테이블에 사진 추가.
            String insertPhotoSql = "INSERT INTO Oneul.Photos (diary_id, text) VALUES (?,?)";
            insertPhotoStatement = connection.prepareStatement(insertPhotoSql);

            for (String filepath : filePath2) {
                // String imgUrl = s3TestService.saveImgS3(img); // 실제 S3 업로드 로직
                insertPhotoStatement.setInt(1, diaryId);
                insertPhotoStatement.setString(2, filepath);
                insertPhotoStatement.addBatch(); // 배치 작업으로 추가
            }
            insertPhotoStatement.executeBatch(); // 배치 실행

            connection.commit(); // 여기
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateDiary(DiaryUpdateRequest request) {
        Connection connection;
        PreparedStatement statement = null;

        try {
            connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
            connection.setAutoCommit(false);// 유사 트랜잭션모드

            // 다이어리 정보 업데이트 SQL 쿼리
            String updateSql = "UPDATE Oneul.DiaryList SET title = ?, text = ?, emotion = ? WHERE diary_id = ?";
            statement = connection.prepareStatement(updateSql);
            statement.setString(1, request.getTitle());
            statement.setString(2, request.getText());
            statement.setString(3, request.getEmotion());
            statement.setInt(4, request.getDiary_id());

            int rowsAffected = statement.executeUpdate();

            // 태그 업데이트 SQL 쿼리
            if (!request.getTags().isEmpty()) {
                String deleteTagsSql = "DELETE FROM Oneul.Hashtags WHERE diary_id = ?";
                statement = connection.prepareStatement(deleteTagsSql);
                statement.setInt(1, request.getDiary_id());
                statement.executeUpdate();

                String insertTagSql = "INSERT INTO Oneul.Hashtags (diary_id, text) VALUES (?, ?)";
                statement = connection.prepareStatement(insertTagSql);

                for (String tag : request.getTags()) {
                    statement.setInt(1, request.getDiary_id());
                    statement.setString(2, tag);
                    statement.addBatch();
                }
                statement.executeBatch();
            }

            // 이미지 업데이트 SQL 쿼리
            if (!request.getImgs().isEmpty()) {
                String deleteImgsSql = "DELETE FROM Oneul.Photos WHERE diary_id = ?";
                statement = connection.prepareStatement(deleteImgsSql);
                statement.setInt(1, request.getDiary_id());
                statement.executeUpdate();

                String insertImgsSql = "INSERT INTO Oneul.Photos (diary_id, text) VALUES (?, ?)";
                statement = connection.prepareStatement(insertImgsSql);

                for (String img : request.getImgs()) {
                    statement.setInt(1, request.getDiary_id());
                    statement.setString(2, img);
                    statement.addBatch();
                }
                statement.executeBatch();
            }

            connection.commit(); // 트랜잭션 커밋

            if (rowsAffected > 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 새로운 메서드 추가: 오늘 날짜의 일기를 가져옴
    public String getTodayDiary(String token) {
        Connection connection;
        PreparedStatement statement = null;
        ResultSet resultSet = null; // 실행 결과 받아오는 변수

        try {
            connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
            connection.setAutoCommit(false); // 유사 트랜잭션모드

            // token값으로 user_id를 추출
            String getUserIdSql = "SELECT user_id FROM Oneul.Member WHERE token = ?";
            statement = connection.prepareStatement(getUserIdSql);
            statement.setString(1, token);

            resultSet = statement.executeQuery();
            int userId = 0;
            if (resultSet.next()) {
                userId = resultSet.getInt("user_id");
            }

            // 오늘 날짜의 일기를 검색하는 SQL 쿼리
            String getTodayDiarySql = "SELECT diary_id, title, text, date, emotion FROM Oneul.DiaryList WHERE user_id = ? AND date = CURRENT_DATE";
            statement = connection.prepareStatement(getTodayDiarySql);
            statement.setInt(1, userId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String text = resultSet.getString("text");
                return text;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object[] getTodayDiaryNId(String token) {
        Connection connection;
        PreparedStatement statement = null;
        ResultSet resultSet = null; // 실행 결과 받아오는 변수

        try {
            connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
            connection.setAutoCommit(false); // 유사 트랜잭션모드

            // token값으로 user_id를 추출
            String getUserIdSql = "SELECT user_id FROM Oneul.Member WHERE token = ?";
            statement = connection.prepareStatement(getUserIdSql);
            statement.setString(1, token);

            resultSet = statement.executeQuery();
            int userId = 0;
            if (resultSet.next()) {
                userId = resultSet.getInt("user_id");
            }

            // 오늘 날짜의 일기를 검색하는 SQL 쿼리
            String getTodayDiarySql = "SELECT diary_id, text FROM Oneul.DiaryList WHERE user_id = ? AND date = CURRENT_DATE";
            statement = connection.prepareStatement(getTodayDiarySql);
            statement.setInt(1, userId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int diaryId = resultSet.getInt("diary_id");
                String text = resultSet.getString("text");
                return new Object[] { diaryId, text };
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer getTodayDiaryID(String token) {
        Connection connection;
        PreparedStatement statement = null;
        ResultSet resultSet = null; // 실행 결과 받아오는 변수

        try {
            connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
            connection.setAutoCommit(false); // 유사 트랜잭션모드

            // token값으로 user_id를 추출
            String getUserIdSql = "SELECT user_id FROM Oneul.Member WHERE token = ?";
            statement = connection.prepareStatement(getUserIdSql);
            statement.setString(1, token);

            resultSet = statement.executeQuery();
            int userId = 0;
            if (resultSet.next()) {
                userId = resultSet.getInt("user_id");
            }

            // 오늘 날짜의 일기 ID를 검색하는 SQL 쿼리
            String getTodayDiaryIdSql = "SELECT diary_id FROM Oneul.DiaryList WHERE user_id = ? AND date = CURRENT_DATE";
            statement = connection.prepareStatement(getTodayDiaryIdSql);
            statement.setInt(1, userId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int diaryId = resultSet.getInt("diary_id");
                return diaryId;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // 일기를 찾지 못한 경우 null 반환
    }

}