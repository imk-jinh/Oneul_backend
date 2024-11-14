package com.oneul.service;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HomeService {
    @Value("${spring.datasource.url}")
    String URL;

    @Value("${spring.datasource.username}")
    String USERNAME;

    @Value("${spring.datasource.password}")
    String SQL_PASSWORD;

    @Value("${acntBck.imgPath}")
    String imgPath;

    public int getUserIdByToken(String token) {
        String sql = "SELECT user_id FROM Oneul.Member WHERE token = ?";
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, token);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("user_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int getDiarySum(int user_id) {
        String sql = "SELECT COUNT(diary_id) AS diary_sum FROM Oneul.DiaryList WHERE user_id = ?";
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, user_id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("diary_sum");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<Integer> getDiaryIdByUserId(int user_id) {
        List<Integer> diaryIds = new ArrayList<>();
        String sql = "SELECT diary_id FROM Oneul.DiaryList WHERE user_id = ? AND date BETWEEN ? AND ?";
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, user_id);

            YearMonth thisMonth = YearMonth.now();
            LocalDate firstDayOfThisMonth = thisMonth.atDay(1);
            LocalDate lastDayOfThisMonth = thisMonth.atEndOfMonth();

            preparedStatement.setDate(2, java.sql.Date.valueOf(firstDayOfThisMonth));
            preparedStatement.setDate(3, java.sql.Date.valueOf(lastDayOfThisMonth));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    diaryIds.add(resultSet.getInt("diary_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return diaryIds;
    }

    public List<String> getHashtagsByUserIdAndMonth(int userId, int year, int month) {
        List<String> hashtags = new ArrayList<>();
        String sql = "SELECT h.text " +
                     "FROM Oneul.Hashtags h " +
                     "JOIN Oneul.DiaryList d ON h.diary_id = d.diary_id " +
                     "WHERE d.user_id = ? AND YEAR(d.date) = ? AND MONTH(d.date) = ?";
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, year);
            preparedStatement.setInt(3, month);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    hashtags.add(resultSet.getString("text"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return hashtags;
    }

    public List<String> getMonthDiaries(int userId, int year, int month) {
        List<String> monthDiaries = new ArrayList<>();
        String sql = "SELECT text FROM Oneul.DiaryList WHERE user_id = ? AND MONTH(date) = ? AND YEAR(date) = ?";
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, month);
            preparedStatement.setInt(3, year);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    monthDiaries.add(resultSet.getString("text"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return monthDiaries;
    }

    public List<Map<String, Object>> getDiariesByMonth(int userId, int year, int month) {
        List<Map<String, Object>> monthDiaries = new ArrayList<>();
        String sql = "SELECT d.diary_id, d.title, d.date, d.emotion, d.text, " +
                     "GROUP_CONCAT(DISTINCT h.text) AS hashtags, " +
                     "GROUP_CONCAT(DISTINCT p.text) AS photoTexts " +
                     "FROM Oneul.DiaryList d " +
                     "LEFT JOIN Oneul.Hashtags h ON d.diary_id = h.diary_id " +
                     "LEFT JOIN Oneul.Photos p ON d.diary_id = p.diary_id " +
                     "WHERE d.user_id = ? AND MONTH(d.date) = ? AND YEAR(d.date) = ? " +
                     "GROUP BY d.diary_id, d.title, d.date, d.emotion, d.text";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, month);
            preparedStatement.setInt(3, year);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, Object> diaryInfo = new LinkedHashMap<>();
                    diaryInfo.put("diaryId", resultSet.getInt("diary_id"));
                    diaryInfo.put("title", resultSet.getString("title"));
                    diaryInfo.put("date", resultSet.getDate("date").toLocalDate());
                    diaryInfo.put("emotion", resultSet.getString("emotion"));
                    diaryInfo.put("text", resultSet.getString("text"));

                    String hashtags = resultSet.getString("hashtags");
                    diaryInfo.put("hashtags", hashtags != null ? Arrays.asList(hashtags.split(",")) : new ArrayList<>());

                    String photoTexts = resultSet.getString("photoTexts");
                    diaryInfo.put("photoTexts", photoTexts != null ? Arrays.asList(photoTexts.split(",")) : new ArrayList<>());

                    monthDiaries.add(diaryInfo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return monthDiaries;
    }

    public List<Map<String, Object>> getTagDiary(int userId, String hashtag) {
        List<Map<String, Object>> tagDiaries = new ArrayList<>();
        String sql = "SELECT d.title, d.text, d.date " +
                     "FROM Oneul.DiaryList d " +
                     "JOIN Oneul.Hashtags h ON d.diary_id = h.diary_id " +
                     "WHERE d.user_id = ? AND h.text = ?";
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, userId);
            preparedStatement.setString(2, hashtag);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, Object> diaryInfo = new HashMap<>();
                    diaryInfo.put("title", resultSet.getString("title"));
                    diaryInfo.put("text", resultSet.getString("text"));
                    diaryInfo.put("date", resultSet.getDate("date").toLocalDate());
                    tagDiaries.add(diaryInfo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tagDiaries;
    }

    public Map<String, Object> getDiaryInfoById(int diaryId) {
        Map<String, Object> diaryInfo = new HashMap<>();
        String sql = "SELECT title, text, date FROM Oneul.DiaryList WHERE diary_id = ?";
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, diaryId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    diaryInfo.put("title", resultSet.getString("title"));
                    diaryInfo.put("text", resultSet.getString("text"));
                    diaryInfo.put("date", resultSet.getDate("date").toLocalDate());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return diaryInfo;
    }

    public List<Integer> getDiaryIdByHashtag(int userId, String hashtag) {
        List<Integer> diaryIds = new ArrayList<>();
        String sql = "SELECT d.diary_id " +
                     "FROM Oneul.DiaryList d " +
                     "JOIN Oneul.Hashtags h ON d.diary_id = h.diary_id " +
                     "WHERE d.user_id = ? AND h.text = ?";
        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, userId);
            preparedStatement.setString(2, hashtag);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    diaryIds.add(resultSet.getInt("diary_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return diaryIds;
    }
}
