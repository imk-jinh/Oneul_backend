package com.oneul.service;

import com.oneul.model.Diary_List;
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
// import java.sql.SQLException;
// import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DiaryService {
    @Value("${spring.datasource.url}")
    String URL;

    @Value("${spring.datasource.username}")
    String USERNAME;

    @Value("${spring.datasource.password}")
    String SQL_PASSWORD;

    Diary_List diaryList;

    public List<DiaryDetailResponse> getDetailDiary(String token) {
        List<DiaryDetailResponse> responses = new ArrayList<>(); // 각 diary_id에 대한 응답을 저장할 리스트 생성

        // token값으로 diary_id들을 추출
        String getDiaryId = "SELECT d.diary_id\n" + //
                "FROM Oneul.DiaryList d\n" + //
                "INNER JOIN Oneul.Member m ON d.user_id = m.user_id\n" + //
                "WHERE m.token = ?;";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement getDiaryIdStatement = connection.prepareStatement(getDiaryId)) {

            connection.setAutoCommit(false);// 유사 트랜잭션모드

            getDiaryIdStatement.setString(1, token);
            List<Integer> diaryIds = new ArrayList<>();

            try (ResultSet resultSet = getDiaryIdStatement.executeQuery()) {
                while (resultSet.next()) {
                    int diaryId = resultSet.getInt("diary_id");
                    diaryIds.add(diaryId);
                }
            }

            for (int diaryId : diaryIds) {
                String title = null;
                String text = null;
                Date date = null;
                String emotion = null;
                List<String> hashtagTexts = new ArrayList<>(); // 해시태그 텍스트를 저장할 리스트 생성
                List<String> ImgList = new ArrayList<>(); // 이미지 경로 텍스트를 저장할 리스트 생성

                String getDataSql = "SELECT title, text, date, emotion FROM Oneul.DiaryList WHERE diary_id = ?";
                try (PreparedStatement getDataStatement = connection.prepareStatement(getDataSql)) {
                    getDataStatement.setInt(1, diaryId);

                    try (ResultSet getDataResultSet = getDataStatement.executeQuery()) {
                        if (getDataResultSet.next()) {
                            title = getDataResultSet.getString("title");
                            text = getDataResultSet.getString("text");
                            date = getDataResultSet.getDate("date");
                            emotion = getDataResultSet.getString("emotion");
                        }
                    }
                }

                String searchHashTagSql = "SELECT text FROM Oneul.Hashtags WHERE diary_id = ?";
                try (PreparedStatement searchHashTagStatement = connection.prepareStatement(searchHashTagSql)) {
                    searchHashTagStatement.setInt(1, diaryId);

                    try (ResultSet hashtagResultSet = searchHashTagStatement.executeQuery()) {
                        while (hashtagResultSet.next()) {
                            String hashtagText = hashtagResultSet.getString("text");
                            hashtagTexts.add(hashtagText);
                        }
                    }
                }

                String searchImgsSql = "SELECT text FROM Oneul.Photos WHERE diary_id = ?";
                try (PreparedStatement searchImgsStatement = connection.prepareStatement(searchImgsSql)) {
                    searchImgsStatement.setInt(1, diaryId);

                    try (ResultSet imgResultSet = searchImgsStatement.executeQuery()) {
                        while (imgResultSet.next()) {
                            String photoText = imgResultSet.getString("text");
                            ImgList.add(photoText);
                        }
                    }
                }

                connection.commit(); // 여기까지 트랜잭션 처리

                DiaryDetailResponse response = new DiaryDetailResponse(title, date, hashtagTexts, emotion, text, ImgList);
                System.out.println(response.toString());
                responses.add(response);
            }
            return responses; // 모든 diary_id에 대한 응답 객체들을 담고 있는 리스트 반환

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addDiary(String token, String title, Date date, List<String> tags, String emotion, String text,
                            List<String> filePath2) {
        Integer existDiaryId = getTodayDiaryID(token);
        if (existDiaryId != null) {
            System.out.println("이미 작성된 일기 있음요");
            return false;
        }

        String getUserIdSql = "SELECT user_id FROM Oneul.Member WHERE token = ?";
        String insertDiarySql = "INSERT INTO Oneul.DiaryList (user_id, title, text, date, emotion) VALUES (?, ?, ?, ?, ?)";
        String insertTagSql = "INSERT INTO Oneul.Hashtags (diary_id, text) VALUES (?, ?)";
        String insertPhotoSql = "INSERT INTO Oneul.Photos (diary_id, text) VALUES (?,?)";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement getUserIdStatement = connection.prepareStatement(getUserIdSql);
             PreparedStatement insertDiaryStatement = connection.prepareStatement(insertDiarySql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement insertTagStatement = connection.prepareStatement(insertTagSql);
             PreparedStatement insertPhotoStatement = connection.prepareStatement(insertPhotoSql)) {

            connection.setAutoCommit(false);// 유사 트랜잭션모드

            getUserIdStatement.setString(1, token);

            int userid;
            try (ResultSet useridResult = getUserIdStatement.executeQuery()) {
                if (useridResult.next()) {
                    userid = useridResult.getInt("user_id");
                } else {
                    return false; // 유효한 사용자 ID가 없는 경우
                }
            }

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

            int diaryId;
            try (ResultSet diaryIdResult = insertDiaryStatement.getGeneratedKeys()) {
                if (diaryIdResult.next()) {
                    diaryId = diaryIdResult.getInt(1);
                } else {
                    return false; // diary_id를 얻지 못한 경우
                }
            }

            for (String tag : tags) {
                insertTagStatement.setInt(1, diaryId);
                insertTagStatement.setString(2, tag);
                insertTagStatement.addBatch(); // 배치 작업으로 추가
            }
            insertTagStatement.executeBatch(); // 배치 실행

            for (String filepath : filePath2) {
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
        String updateSql = "UPDATE Oneul.DiaryList SET title = ?, text = ?, emotion = ? WHERE diary_id = ?";
        String deleteTagsSql = "DELETE FROM Oneul.Hashtags WHERE diary_id = ?";
        String insertTagSql = "INSERT INTO Oneul.Hashtags (diary_id, text) VALUES (?, ?)";
        String deleteImgsSql = "DELETE FROM Oneul.Photos WHERE diary_id = ?";
        String insertImgsSql = "INSERT INTO Oneul.Photos (diary_id, text) VALUES (?, ?)";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement updateStatement = connection.prepareStatement(updateSql);
             PreparedStatement deleteTagsStatement = connection.prepareStatement(deleteTagsSql);
             PreparedStatement insertTagStatement = connection.prepareStatement(insertTagSql);
             PreparedStatement deleteImgsStatement = connection.prepareStatement(deleteImgsSql);
             PreparedStatement insertImgsStatement = connection.prepareStatement(insertImgsSql)) {

            connection.setAutoCommit(false);// 유사 트랜잭션모드

            updateStatement.setString(1, request.getTitle());
            updateStatement.setString(2, request.getText());
            updateStatement.setString(3, request.getEmotion());
            updateStatement.setInt(4, request.getDiary_id());

            int rowsAffected = updateStatement.executeUpdate();

            if (!request.getTags().isEmpty()) {
                deleteTagsStatement.setInt(1, request.getDiary_id());
                deleteTagsStatement.executeUpdate();

                for (String tag : request.getTags()) {
                    insertTagStatement.setInt(1, request.getDiary_id());
                    insertTagStatement.setString(2, tag);
                    insertTagStatement.addBatch();
                }
                insertTagStatement.executeBatch();
            }

            if (!request.getImgs().isEmpty()) {
                deleteImgsStatement.setInt(1, request.getDiary_id());
                deleteImgsStatement.executeUpdate();

                for (String img : request.getImgs()) {
                    insertImgsStatement.setInt(1, request.getDiary_id());
                    insertImgsStatement.setString(2, img);
                    insertImgsStatement.addBatch();
                }
                insertImgsStatement.executeBatch();
            }

            connection.commit();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteDiary(int diary_id) {
        String deleteDiarySql = "DELETE FROM Oneul.DiaryList WHERE diary_id = ?";
        String deleteTagsSql = "DELETE FROM Oneul.Hashtags WHERE diary_id = ?";
        String deleteImgsSql = "DELETE FROM Oneul.Photos WHERE diary_id = ?";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement deleteDiaryStatement = connection.prepareStatement(deleteDiarySql);
             PreparedStatement deleteTagsStatement = connection.prepareStatement(deleteTagsSql);
             PreparedStatement deleteImgsStatement = connection.prepareStatement(deleteImgsSql)) {

            connection.setAutoCommit(false);// 유사 트랜잭션모드

            deleteTagsStatement.setInt(1, diary_id);
            deleteTagsStatement.executeUpdate();

            deleteImgsStatement.setInt(1, diary_id);
            deleteImgsStatement.executeUpdate();

            int rowsAffected = deleteDiaryStatement.executeUpdate();
            connection.commit(); // 여기까지 트랜잭션 처리
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // 새로운 메서드 추가: 오늘 날짜의 일기를 가져옴
    public String getTodayDiary(String token) {
        String getTodayDiarySql = "SELECT text FROM Oneul.DiaryList WHERE user_id = ? AND date = CURRENT_DATE";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement getUserIdStatement = connection.prepareStatement("SELECT user_id FROM Oneul.Member WHERE token = ?");
             PreparedStatement getDiaryStatement = connection.prepareStatement(getTodayDiarySql)) {

            connection.setAutoCommit(false);

            // token으로 user_id 추출
            getUserIdStatement.setString(1, token);
            try (ResultSet resultSet = getUserIdStatement.executeQuery()) {
                int userId = 0;
                if (resultSet.next()) {
                    userId = resultSet.getInt("user_id");
                }

                // 오늘 날짜의 일기 검색
                getDiaryStatement.setInt(1, userId);
                try (ResultSet diaryResultSet = getDiaryStatement.executeQuery()) {
                    if (diaryResultSet.next()) {
                        return diaryResultSet.getString("text");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Object[] getTodayDiaryNId(String token) {
        String getTodayDiarySql = "SELECT diary_id, text FROM Oneul.DiaryList WHERE user_id = ? AND date = CURRENT_DATE";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement getUserIdStatement = connection.prepareStatement("SELECT user_id FROM Oneul.Member WHERE token = ?");
             PreparedStatement getDiaryStatement = connection.prepareStatement(getTodayDiarySql)) {

            connection.setAutoCommit(false);

            // token으로 user_id 추출
            getUserIdStatement.setString(1, token);
            try (ResultSet resultSet = getUserIdStatement.executeQuery()) {
                int userId = 0;
                if (resultSet.next()) {
                    userId = resultSet.getInt("user_id");
                }

                // 오늘 날짜의 일기 검색
                getDiaryStatement.setInt(1, userId);
                try (ResultSet diaryResultSet = getDiaryStatement.executeQuery()) {
                    if (diaryResultSet.next()) {
                        int diaryId = diaryResultSet.getInt("diary_id");
                        String text = diaryResultSet.getString("text");
                        return new Object[]{diaryId, text};
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Integer getTodayDiaryID(String token) {
        String getTodayDiaryIdSql = "SELECT diary_id FROM Oneul.DiaryList WHERE user_id = ? AND date = CURRENT_DATE";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement getUserIdStatement = connection.prepareStatement("SELECT user_id FROM Oneul.Member WHERE token = ?");
             PreparedStatement getDiaryIdStatement = connection.prepareStatement(getTodayDiaryIdSql)) {

            connection.setAutoCommit(false);

            // token으로 user_id 추출
            getUserIdStatement.setString(1, token);
            try (ResultSet resultSet = getUserIdStatement.executeQuery()) {
                int userId = 0;
                if (resultSet.next()) {
                    userId = resultSet.getInt("user_id");
                }

                // 오늘 날짜의 일기 ID 검색
                getDiaryIdStatement.setInt(1, userId);
                try (ResultSet diaryIdResultSet = getDiaryIdStatement.executeQuery()) {
                    if (diaryIdResultSet.next()) {
                        return diaryIdResultSet.getInt("diary_id");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getDiaryContentById(Integer diaryId) {
        String getDiaryContentSql = "SELECT text FROM Oneul.DiaryList WHERE diary_id = ?";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement getDiaryContentStatement = connection.prepareStatement(getDiaryContentSql)) {

            // diary_id로 일기 내용 검색
            getDiaryContentStatement.setInt(1, diaryId);
            try (ResultSet diaryContentResultSet = getDiaryContentStatement.executeQuery()) {
                if (diaryContentResultSet.next()) {
                    return diaryContentResultSet.getString("text");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}