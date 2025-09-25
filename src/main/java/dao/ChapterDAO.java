// dao/ChapterDAO.java — PHIÊN BẢN ĐƠN GIẢN, ĐỦ XÀI CHO SINH VIÊN NĂM NHẤT 😊

package dao;

import connect.DBConnection;
import model.Chapter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChapterDAO {


    public List<Chapter> getChaptersByBookId(int bookId) {
        List<Chapter> chapters = new ArrayList<>();
        String sql = "SELECT ChapterID, BookID, ChapterNumber, Title, Content, Summary FROM Chapter WHERE BookID = ? ORDER BY ChapterNumber";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                Chapter chap = new Chapter();
                chap.setChapterID(rs.getInt("ChapterID"));
                chap.setBookID(rs.getInt("BookID"));
                chap.setChapterNumber(rs.getInt("ChapterNumber"));
                chap.setTitle(rs.getString("Title"));
                chap.setContent(rs.getString("Content"));
                chap.setSummary(rs.getString("Summary"));
                chapters.add(chap);
            }
            
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        
        return chapters;
    }


    public Integer findChapterIdByTitle(String title) {
        if (title == null) return null;
        String sql = "SELECT ChapterID FROM Chapter WHERE Title = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("ChapterID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}