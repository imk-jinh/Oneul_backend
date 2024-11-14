package com.oneul.service;

import org.springframework.beans.factory.annotation.Value;

import com.oneul.config.WebSocketConfig;

import java.lang.reflect.Member;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.sql.Statement;

import org.joda.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MatchService {
    @Value("${spring.datasource.url}")
    String URL;

    @Value("${spring.datasource.username}")
    String USERNAME;

    @Value("${spring.datasource.password}")
    String SQL_PASSWORD;

    @Autowired
    private WebSocketConfig webSocketConfig;

    // 새로운 메서드 추가: 오늘 날짜의 일기를 가져옴

    public int matchMember(int oldestUserId) {
        String findDiaryQuery = "SELECT text FROM Oneul.DiaryList WHERE user_id = ? AND date = CURDATE()";
        String findPotentialMatchesQuery = "SELECT mp.user_id, dl.text " +
                "FROM Oneul.MatchmakingProgress mp " +
                "JOIN Oneul.DiaryList dl ON mp.user_id = dl.user_id " +
                "LEFT JOIN Oneul.ChatList cl1 ON mp.user_id = cl1.user_id AND cl1.partner_id = ? AND cl1.end_flag = 0 "
                +
                "LEFT JOIN Oneul.ChatList cl2 ON mp.user_id = cl2.partner_id AND cl2.user_id = ? AND cl2.end_flag = 0 "
                +
                "WHERE dl.date = CURDATE() AND mp.user_id != ? " +
                "AND cl1.user_id IS NULL AND cl2.partner_id IS NULL " +
                "ORDER BY mp.date ASC " +
                "LIMIT 30"; // 최대 30명의 매칭 후보만 가져옴

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement findDiaryStmt = connection.prepareStatement(findDiaryQuery);
                PreparedStatement findPotentialMatchesStmt = connection.prepareStatement(findPotentialMatchesQuery)) {

            // oldestUserId 사용자의 오늘 일기 가져오기
            findDiaryStmt.setInt(1, oldestUserId);
            ResultSet oldestUserDiaryResult = findDiaryStmt.executeQuery();

            if (!oldestUserDiaryResult.next()) {
                System.out.println("오늘 날짜의 일기가 없는 사용자 ID: " + oldestUserId);
                return -1;
            }

            String oldestUserDiaryText = oldestUserDiaryResult.getString("text");

            // 매칭 후보들 찾기 (최대 30명, 이미 매칭된 사용자 제외)
            findPotentialMatchesStmt.setInt(1, oldestUserId);
            findPotentialMatchesStmt.setInt(2, oldestUserId);
            findPotentialMatchesStmt.setInt(3, oldestUserId);
            ResultSet potentialMatchesResult = findPotentialMatchesStmt.executeQuery();

            int bestMatchUserId = -1;
            double bestSimilarity = -1.0;

            // 후보들을 순회하며 가장 유사한 사용자 찾기
            while (potentialMatchesResult.next()) {
                int potentialMatchUserId = potentialMatchesResult.getInt("user_id");
                String potentialMatchDiaryText = potentialMatchesResult.getString("text");

                double similarity = calculateTextSimilarity(oldestUserDiaryText, potentialMatchDiaryText);

                // 유사도가 더 높은 사용자를 찾으면 업데이트
                if (similarity > bestSimilarity || bestMatchUserId == -1) {
                    bestSimilarity = similarity;
                    bestMatchUserId = potentialMatchUserId;
                }
            }

            // 가장 유사한 사용자가 있으면 매칭 진행
            if (bestMatchUserId != -1) {
                return bestMatchUserId;
            } else {
                // 이 경우는 발생하지 않음 (반드시 한 명 매칭)
                return -1;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private double calculateTextSimilarity(String text1, String text2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return (double) intersection.size() / union.size();
    }

    // 일기가 비슷한 사람 하루를 비슷하게 산 사람 끼리 대화 매칭
    // 매칭하고 싶어요 클릭하면 -> 대기 테이블 인풋 들어가
    // 매칭 시켜주는 함수가 있어
    // 1분마다 테이블이 비었는지 확인 안비었으면 매칭 함수 실행 -> 144번 호출

    // 대기열에 인풋을 넣는 것과 순차적으로 처리하는 부분 구분돼야 한다. 세마포

    // 대기열이 값이 존재함을 어떻게 인지할 수 있을까? 효율적으로

    // 매칭하기 함수 누르면 접근 공유시킨다. B 눌렀어

    // 대기열 테이블에 매칭 예정인 사람의 아이디를 넣을 컬럼 -> 유사한 일기인데 대기열에
    // 들어가려 하는데 비었으면 등록이되고 없으면

    public void matchUsersUntilEmpty() {
        String selectOldestUserQuery = "SELECT * FROM Oneul.MatchmakingProgress ORDER BY date ASC LIMIT 1";
        String deleteUserQuery = "DELETE FROM Oneul.MatchmakingProgress WHERE user_id = ?";
        String checkUsersExistQuery = "SELECT COUNT(*) AS user_count FROM Oneul.MatchmakingProgress";
        String insertChatListQuery = "INSERT INTO Oneul.ChatList (user_id, partner_id, createdAt, end_time, end_flag, count) VALUES (?, ?, ?, ?, 0, 0)";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement selectOldestUserStmt = connection.prepareStatement(selectOldestUserQuery);
                PreparedStatement deleteUserStmt = connection.prepareStatement(deleteUserQuery);
                PreparedStatement checkUsersExistStmt = connection.prepareStatement(checkUsersExistQuery);
                PreparedStatement insertChatListStmt = connection.prepareStatement(insertChatListQuery,
                        Statement.RETURN_GENERATED_KEYS)) {

            boolean condition = true;

            while (condition) {

                ResultSet userCountResult = checkUsersExistStmt.executeQuery();
                if (userCountResult.next()) {
                    int userCount = userCountResult.getInt("user_count");
                    condition = userCount > 1;
                }

                if (!condition) {
                    break;
                }

                ResultSet oldestUserResult = selectOldestUserStmt.executeQuery();
                if (oldestUserResult.next()) {
                    int oldestUserId = oldestUserResult.getInt("user_id");

                    int matchUserId = matchMember(oldestUserId);

                    if (matchUserId != -1) {
                        String checkMatchSql = "SELECT id FROM Oneul.ChatList WHERE (user_id = ? AND partner_id = ?) OR (user_id = ? AND partner_id = ?)";
                        try (PreparedStatement checkMatchStmt = connection.prepareStatement(checkMatchSql)) {
                            checkMatchStmt.setInt(1, oldestUserId);
                            checkMatchStmt.setInt(2, matchUserId);
                            checkMatchStmt.setInt(3, matchUserId);
                            checkMatchStmt.setInt(4, oldestUserId);
                            try (ResultSet checkMatchResult = checkMatchStmt.executeQuery()) {
                                if (checkMatchResult.next()) {
                                    long existingMatchId = checkMatchResult.getLong("id");
                                    LocalDateTime now = LocalDateTime.now();
                                    Timestamp endTime = Timestamp.valueOf(now.plusDays(1)); // 24시간 뒤

                                    deleteUserStmt.setInt(1, oldestUserId);
                                    deleteUserStmt.executeUpdate();
                                    deleteUserStmt.setInt(1, matchUserId);
                                    deleteUserStmt.executeUpdate();

                                    String updateFlagSql = "UPDATE Oneul.ChatList SET end_flag = 0, end_time = ? WHERE id = ?";
                                    try (PreparedStatement updateFlagStmt = connection
                                            .prepareStatement(updateFlagSql)) {
                                        updateFlagStmt.setTimestamp(1, endTime); // Set the new end_time
                                        updateFlagStmt.setLong(2, existingMatchId); // Set the id of the existing match
                                        updateFlagStmt.executeUpdate();
                                    }

                                    webSocketConfig.sendMatchNotification(oldestUserId, matchUserId,
                                            existingMatchId);

                                } else {
                                    // If no match exists, proceed to insert a new match
                                    LocalDateTime now = LocalDateTime.now();
                                    Timestamp createdAt = Timestamp.valueOf(now);
                                    Timestamp endTime = Timestamp.valueOf(now.plusDays(1)); // 24시간 뒤

                                    insertChatListStmt.setInt(1, oldestUserId);
                                    insertChatListStmt.setInt(2, matchUserId);
                                    insertChatListStmt.setTimestamp(3, createdAt);
                                    insertChatListStmt.setTimestamp(4, endTime);

                                    deleteUserStmt.setInt(1, oldestUserId);
                                    deleteUserStmt.executeUpdate();
                                    deleteUserStmt.setInt(1, matchUserId);
                                    deleteUserStmt.executeUpdate();

                                    insertChatListStmt.executeUpdate();

                                    try (ResultSet generatedKeys = insertChatListStmt.getGeneratedKeys()) {
                                        if (generatedKeys.next()) {
                                            long insertedId = generatedKeys.getLong(1); // Assuming the 'id' column is
                                                                                        // the first column in the
                                                                                        // result set
                                            webSocketConfig.sendMatchNotification(oldestUserId, matchUserId,
                                                    insertedId);
                                        } else {
                                        }

                                    }
                                }

                            }

                        }
                    }
                } else {
                    System.out.println("No users in waiting table.");
                    break;
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addProgress(Integer userId, Integer diaryId) {
        System.out.println("addProgress start");

        String checkExistenceSql = "SELECT COUNT(*) FROM Oneul.MatchmakingProgress WHERE user_id = ? AND diary_id = ?";
        String insertSql = "INSERT INTO Oneul.MatchmakingProgress (user_id, diary_id, date) VALUES (?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement checkExistenceStmt = connection.prepareStatement(checkExistenceSql);
                PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {

            // Check if the entry already exists
            checkExistenceStmt.setInt(1, userId);
            checkExistenceStmt.setInt(2, diaryId);

            ResultSet resultSet = checkExistenceStmt.executeQuery();
            if (resultSet.next() && resultSet.getInt(1) == 0) {
                // Entry does not exist, proceed with the insertion
                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, diaryId);

                // Use current timestamp for the date
                Timestamp currentTimestamp = Timestamp.valueOf(LocalDateTime.now());
                insertStmt.setTimestamp(3, currentTimestamp);

                insertStmt.executeUpdate();
                System.out.println("Progress added for user_id: " + userId + " and diary_id: " + diaryId);
            } else {
                // Entry already exists
                System.out.println("Entry already exists for user_id: " + userId + " and diary_id: " + diaryId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeFromQueue(int userId) {
        String deleteUserQuery = "DELETE FROM Oneul.MatchmakingProgress WHERE user_id = ?";

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
                PreparedStatement deleteUserStmt = connection.prepareStatement(deleteUserQuery)) {

            deleteUserStmt.setInt(1, userId);
            int rowsAffected = deleteUserStmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("User :" + userId + " 성공적으로 삭제되었습니다.");
            } else {
                System.out.println("User " + userId + " not found in queue.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}