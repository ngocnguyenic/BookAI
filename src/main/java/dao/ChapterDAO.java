// dao/ChapterDAO.java — CẬP NHẬT THÊM BATCH INSERT CHO RAG
package dao;

import connect.DBConnection;
import model.Chapter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChapterDAO {
    
    /**
     * Lấy tất cả chapters của một book
     */
    public List<Chapter> getChaptersByBookId(int bookId) {
        List<Chapter> chapters = new ArrayList<>();
        String sql = "SELECT ChapterID, BookID, ChapterNumber, Title, Content, Summary " +
                     "FROM Chapter WHERE BookID = ? ORDER BY ChapterNumber";
        
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
    
    /**
     * Tìm ChapterID theo title
     */
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
    
    /**
     * BATCH INSERT - Lưu nhiều chapters cùng lúc (cho RAG)
     * Đây là method quan trọng để lưu kết quả chia chương từ AI
     */
    public boolean insertChaptersBatch(List<Chapter> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return false;
        }
        
        String sql = "INSERT INTO Chapter (BookID, ChapterNumber, Title, Content, Summary) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu transaction
            
            ps = conn.prepareStatement(sql);
            
            for (Chapter chapter : chapters) {
                ps.setInt(1, chapter.getBookID());
                ps.setInt(2, chapter.getChapterNumber());
                ps.setString(3, chapter.getTitle());
                ps.setString(4, chapter.getContent());
                ps.setString(5, chapter.getSummary());
                ps.addBatch(); // Thêm vào batch
            }
            
            int[] results = ps.executeBatch(); // Execute tất cả cùng lúc
            conn.commit(); // Commit transaction
            
            System.out.println("✅ Đã insert thành công " + results.length + " chapters");
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi insert chapters batch: " + e.getMessage());
            e.printStackTrace();
            
            // Rollback nếu có lỗi
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
            
        } finally {
            // Restore auto-commit và đóng resources
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Insert một chapter đơn lẻ (nếu cần)
     */
    public int insertChapter(Chapter chapter) {
        String sql = "INSERT INTO Chapter (BookID, ChapterNumber, Title, Content, Summary) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, chapter.getBookID());
            ps.setInt(2, chapter.getChapterNumber());
            ps.setString(3, chapter.getTitle());
            ps.setString(4, chapter.getContent());
            ps.setString(5, chapter.getSummary());
            
            int affected = ps.executeUpdate();
            
            if (affected > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1); // Return ChapterID
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return -1;
    }
    
    /**
     * Xóa tất cả chapters của một book (dùng khi xóa book hoặc re-process)
     */
    public boolean deleteChaptersByBookId(int bookId) {
        String sql = "DELETE FROM Chapter WHERE BookID = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, bookId);
            int affected = ps.executeUpdate();
            
            System.out.println("Đã xóa " + affected + " chapters của BookID=" + bookId);
            return true;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Đếm số chapters của một book
     */
    public int countChaptersByBookId(int bookId) {
        String sql = "SELECT COUNT(*) as total FROM Chapter WHERE BookID = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, bookId);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("total");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    /**
 * Lấy một chapter cụ thể theo ChapterID
 */
public Chapter getChapterById(int chapterId) {
    String sql = "SELECT ChapterID, BookID, ChapterNumber, Title, Content, Summary " +
                 "FROM Chapter WHERE ChapterID = ?";
    
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        
        ps.setInt(1, chapterId);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            Chapter chap = new Chapter();
            chap.setChapterID(rs.getInt("ChapterID"));
            chap.setBookID(rs.getInt("BookID"));
            chap.setChapterNumber(rs.getInt("ChapterNumber"));
            chap.setTitle(rs.getString("Title"));
            chap.setContent(rs.getString("Content"));
            chap.setSummary(rs.getString("Summary"));
            return chap;
        }
        
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return null;
}
    
    /**
     * Kiểm tra xem book đã có chapters chưa
     */
    public boolean hasChapters(int bookId) {
        return countChaptersByBookId(bookId) > 0;
    }
}