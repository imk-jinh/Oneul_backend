package com.oneul.service;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oneul.dto.AlarmResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlarmService {
    @Value("${spring.datasource.url}")
    String URL;

    @Value("${spring.datasource.username}")
    String USERNAME;

    @Value("${spring.datasource.password}")
    String SQL_PASSWORD;

    public List<AlarmResponse> getMyAlarm(String token) {
        List<AlarmResponse> AlarmList = new ArrayList<>();

        String getDiaryId = "SELECT n.id, n.text, n.read_flag, n.date, n.category " +
                "FROM Oneul.Notifications n " +
                "INNER JOIN Oneul.Member m ON n.user_id = m.user_id " +
                "WHERE m.token = ?;";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement statement = connection.prepareStatement(getDiaryId)) {

            connection.setAutoCommit(false); // 유사 트랜잭션 모드
            statement.setString(1, token);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int alarmId = resultSet.getInt("id");
                    String category = resultSet.getString("category");
                    boolean read_flag = resultSet.getBoolean("read_flag");
                    Date date = resultSet.getDate("date");
                    String text = resultSet.getString("text");

                    AlarmResponse response = new AlarmResponse(alarmId, category, read_flag, date, text);
                    AlarmList.add(response);
                }
            }

            return AlarmList;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean readAlarm(String token, Long alarmId) {
        String updateAlarmSql = "UPDATE Oneul.Notifications n " +
                "INNER JOIN Oneul.Member m ON n.user_id = m.user_id " +
                "SET n.read_flag = true " +
                "WHERE n.id = ? AND m.token = ?";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement statement = connection.prepareStatement(updateAlarmSql)) {

            connection.setAutoCommit(false); // 유사 트랜잭션 모드
            statement.setLong(1, alarmId);
            statement.setString(2, token);

            int rowsUpdated = statement.executeUpdate();
            connection.commit();

            return rowsUpdated > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean markAllAsRead(String token, List<Long> alarmIds) {
        String updateSql = "UPDATE Oneul.Notifications n " +
                "INNER JOIN Oneul.Member m ON n.user_id = m.user_id " +
                "SET n.read_flag = true " +
                "WHERE n.id = ? AND m.token = ?";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement statement = connection.prepareStatement(updateSql)) {

            connection.setAutoCommit(false);

            for (Long alarmId : alarmIds) {
                statement.setLong(1, alarmId);
                statement.setString(2, token);
                statement.addBatch();
            }

            int[] updateCounts = statement.executeBatch();
            connection.commit();

            for (int count : updateCounts) {
                if (count == 0) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}
