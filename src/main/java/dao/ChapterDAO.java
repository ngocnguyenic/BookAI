package dao;

import connect.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ChapterDAO {
    public Integer findChapterIdByTitle(String title) throws SQLException {
        if (title == null) return null;
        String sql = "SELECT ChapterID FROM Chapter WHERE Title = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("ChapterID");
                } else {
                    return null;
                }
            }
        }
    }
}
