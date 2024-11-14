package com.oneul.service;

import java.sql.*;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oneul.model.Member;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
	@Value("${spring.datasource.url}")
	String URL;

	@Value("${spring.datasource.username}")
	String USERNAME;

	@Value("${spring.datasource.password}")
	String SQL_PASSWORD;

	private final MemRepo memRepo;

	public String checkEmail(String email) {
		String sql = "SELECT COUNT(*) FROM Oneul.Member WHERE email = ?";
		try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, email);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					int count = resultSet.getInt(1);
					return count == 0 ? "no" : "ok";
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "error";
	}

	// 로그인 할때만 사용됨
	public String getToken(String email) {
		String token = UUID.randomUUID().toString();
		String sql;
		int count;
		int rowsUpdated;

		try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD)) {

			sql = "SELECT * from Oneul.Member WHERE email = ?";
			try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
				preparedStatement.setString(1, email);
				try (ResultSet rs = preparedStatement.executeQuery()) {
					if (rs.next()) {
						token = rs.getString("token");
						if (token != null) {
							return token;
						}
					}
				}
			}

			// 중복되지 않는 토큰 생성
			while (true) {
				token = UUID.randomUUID().toString();
				sql = "SELECT COUNT(*) from Oneul.Member WHERE token = ?";
				try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
					preparedStatement.setString(1, token);
					try (ResultSet rs = preparedStatement.executeQuery()) {
						if (rs.next()) {
							count = rs.getInt(1);
							if (count == 0)
								break;
						}
					}
				}
			}

			// 토큰 업데이트
			sql = "UPDATE Oneul.Member SET token = ? WHERE email = ?";
			try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
				preparedStatement.setString(1, token);
				preparedStatement.setString(2, email);

				rowsUpdated = preparedStatement.executeUpdate();
				if (rowsUpdated > 0) {
					return token;
				} else {
					log.error("해당 이메일을 찾을 수 없습니다.");
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return "error";
	}

	public boolean addMember(Member member) {
		String sql = "INSERT INTO Oneul.Member (email, kakao, google, age) VALUES (?, ?, ?, ?)";
		try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, member.getEmail());
			preparedStatement.setBoolean(2, member.isKakao());
			preparedStatement.setBoolean(3, member.isGoogle());
			preparedStatement.setString(4, member.getAge());

			preparedStatement.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public Member getMember(String token) {
		String sql = "SELECT * FROM Oneul.Member WHERE token = ?";
		Member member = null;

		try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, token);
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					member = new Member();
					member.setUser_id(resultSet.getInt("user_id"));
					member.setEmail(resultSet.getString("email"));
					member.setNickname(resultSet.getString("nickname"));
					member.setToken(resultSet.getString("token"));
					member.setLanguage(resultSet.getString("language"));
					member.setImage(resultSet.getString("image"));
					member.setAge(resultSet.getString("age"));
					member.setGoogle(resultSet.getBoolean("google"));
					member.setKakao(resultSet.getBoolean("kakao"));
					member.setGender(resultSet.getString("gender"));
				}
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return member;
	}

	public boolean logout(String token) {
		String sql = "DELETE FROM Oneul.MemNtoken WHERE token  = ?";

		try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, token);
			preparedStatement.executeUpdate();

			sql = "UPDATE Oneul.Member SET token = NULL WHERE token = ?";
			try (PreparedStatement updateStatement = connection.prepareStatement(sql)) {
				updateStatement.setString(1, token);
				int rowsUpdated = updateStatement.executeUpdate();
				return rowsUpdated > 0;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean setNickname(String token, String name) {
		String sql = "UPDATE Oneul.Member SET nickname = ? WHERE token = ?";

		try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD);
				PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

			preparedStatement.setString(1, name);
			preparedStatement.setString(2, token);

			int rowsAffected = preparedStatement.executeUpdate();
			return rowsAffected > 0;

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean setLanguage(String token, String language) {
		String sql = "UPDATE Oneul.Member SET language = ? WHERE token = ?";

		try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD)) {
			connection.setAutoCommit(false); // 트랜잭션 설정

			try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
				preparedStatement.setString(1, language);
				preparedStatement.setString(2, token);

				int rowsAffected = preparedStatement.executeUpdate();
				connection.commit();
				return rowsAffected > 0;

			} catch (SQLException e) {
				connection.rollback(); // 실패 시 롤백
				e.printStackTrace();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean saveImagePathToDB(String token, String filePath) {
		String sql = "UPDATE Oneul.Member SET image = ? WHERE token = ?";

		try (Connection connection = DriverManager.getConnection(URL, USERNAME, SQL_PASSWORD)) {
			connection.setAutoCommit(false); // 트랜잭션 설정

			try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
				preparedStatement.setString(1, filePath);
				preparedStatement.setString(2, token);

				int rowsAffected = preparedStatement.executeUpdate();
				connection.commit();
				return rowsAffected > 0;

			} catch (SQLException e) {
				connection.rollback(); // 실패 시 롤백
				e.printStackTrace();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
}
