package com.oneul.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PoetryService {
    @Value("${spring.datasource.url}")
    String URL;

    @Value("${spring.datasource.username}")
    String USERNAME;

    @Value("${spring.datasource.password}")
    String SQL_PASSWORD;

    public String getPoetryByDiaryId(int diaryId) {
        String poetry = "";
        String sql = "SELECT text FROM Oneul.Poems WHERE diary_id = ?";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, diaryId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    poetry = resultSet.getString("text");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return poetry;
    }

    public void insertPoem(int userId, int diaryId, String title, String text) {
        String checkPoemExistsSql = "SELECT COUNT(*) FROM Oneul.Poems WHERE user_id = ? AND diary_id = ?";
        String insertPoemSql = "INSERT INTO Oneul.Poems (user_id, diary_id, title, text) VALUES (?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement checkPoemStmt = connection.prepareStatement(checkPoemExistsSql);
                PreparedStatement insertPoemStmt = connection.prepareStatement(insertPoemSql)) {

            // 1. 오늘 날짜의 동일한 일기가 이미 있는지 확인
            checkPoemStmt.setInt(1, userId);
            checkPoemStmt.setInt(2, diaryId);
            try (ResultSet resultSet = checkPoemStmt.executeQuery()) {
                resultSet.next();
                int count = resultSet.getInt(1);

                if (count == 0) { // 시가 존재하지 않는 경우에만 추가
                    insertPoemStmt.setInt(1, userId);
                    insertPoemStmt.setInt(2, diaryId);
                    insertPoemStmt.setString(3, title);
                    insertPoemStmt.setString(4, text);

                    insertPoemStmt.executeUpdate();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> getPoetryMonth(int userId, Integer year, Integer month) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        List<Map<String, Object>> poetryList = new ArrayList<>();
        String sql = "SELECT p.title, p.text, p.diary_id, d.date " +
                "FROM Oneul.Poems p " +
                "JOIN Oneul.DiaryList d ON p.diary_id = d.diary_id " +
                "WHERE p.user_id = ? AND YEAR(d.date) = ? AND MONTH(d.date) = ?";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, year);
            preparedStatement.setInt(3, month);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    Map<String, Object> poetryData = new HashMap<>();
                    poetryData.put("title", resultSet.getString("title"));
                    poetryData.put("text", resultSet.getString("text"));
                    poetryData.put("diaryId", resultSet.getInt("diary_id"));
                    Date date = resultSet.getDate("date");
                    poetryData.put("date", dateFormat.format(date));

                    poetryList.add(poetryData);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return poetryList;
    }

}
