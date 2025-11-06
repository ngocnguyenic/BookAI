package dao;

import connect.DBConnection;
import model.Chapter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChapterDAO {

    private static final Logger logger = Logger.getLogger(ChapterDAO.class.getName());

    /**
     * Get chapter by ID
     */
    public Chapter getChapterById(int chapterId) throws SQLException {
        String sql = "SELECT [ChapterID], [BookID], [ChapterNumber], [Title], [Content], [Summary] " +
                     "FROM [Chapter] WHERE [ChapterID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, chapterId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Chapter chapter = new Chapter();
                    chapter.setChapterID(rs.getInt("ChapterID"));
                    chapter.setBookID(rs.getInt("BookID"));
                    chapter.setChapterNumber(rs.getInt("ChapterNumber"));
                    chapter.setTitle(rs.getString("Title"));
                    chapter.setContent(rs.getString("Content"));
                    chapter.setSummary(rs.getString("Summary"));
                    return chapter;
                }
            }
        }
        return null;
    }

    /**
     * Update summary for a specific chapter (lazy loading)
     */
    public boolean updateChapterSummary(int chapterId, String summary) throws SQLException {
        String sql = "UPDATE [Chapter] SET [Summary] = ? WHERE [ChapterID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, summary);
            ps.setInt(2, chapterId);
            
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("✅ Updated summary for chapter ID: " + chapterId);
            } else {
                logger.warning("⚠️ No rows updated for chapter ID: " + chapterId);
            }
            
            return rowsAffected > 0;
        }
    }

    /**
     * Get all chapters by book ID
     */
    public List<Chapter> getChaptersByBookId(int bookId) throws SQLException {
        List<Chapter> chapters = new ArrayList<>();
        String sql = "SELECT [ChapterID], [BookID], [ChapterNumber], [Title], [Content], [Summary] " +
                     "FROM [Chapter] WHERE [BookID] = ? ORDER BY [ChapterNumber]";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, bookId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Chapter chapter = new Chapter();
                    chapter.setChapterID(rs.getInt("ChapterID"));
                    chapter.setBookID(rs.getInt("BookID"));
                    chapter.setChapterNumber(rs.getInt("ChapterNumber"));
                    chapter.setTitle(rs.getString("Title"));
                    chapter.setContent(rs.getString("Content"));
                    chapter.setSummary(rs.getString("Summary"));
                    chapters.add(chapter);
                }
            }
        }
        
        logger.info("✅ Retrieved " + chapters.size() + " chapters for book ID: " + bookId);
        return chapters;
    }

    /**
     * Insert chapters in batch (for upload)
     * FIXED: Added ps.addBatch() call
     */
    public void insertChaptersBatch(List<Chapter> chapters) throws SQLException {
        if (chapters == null || chapters.isEmpty()) {
            logger.warning("⚠️ No chapters to insert");
            return;
        }
        
        String sql = "INSERT INTO [Chapter] ([BookID], [ChapterNumber], [Title], [Content], [Summary]) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        logger.info("📝 Preparing to insert " + chapters.size() + " chapters...");
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            int batchCount = 0;
            
            for (Chapter chapter : chapters) {
                ps.setInt(1, chapter.getBookID());
                ps.setInt(2, chapter.getChapterNumber());
                ps.setString(3, chapter.getTitle());
                ps.setString(4, chapter.getContent());
                ps.setString(5, chapter.getSummary());  // NULL is OK
                
                ps.addBatch();  // ✅ CRITICAL: Add to batch!
                batchCount++;
                
                // Optional: Execute in smaller batches for large datasets
                if (batchCount % 50 == 0) {
                    ps.executeBatch();
                    logger.info("  ✓ Batch executed: " + batchCount + " chapters");
                }
            }
            
            // Execute remaining batch
            int[] results = ps.executeBatch();
            
            logger.info("✅ Successfully inserted " + results.length + " chapters");
            
            // Verify insertion
            int successCount = 0;
            for (int result : results) {
                if (result > 0 || result == Statement.SUCCESS_NO_INFO) {
                    successCount++;
                }
            }
            
            if (successCount != chapters.size()) {
                logger.warning("⚠️ Expected " + chapters.size() + " insertions, but only " + 
                             successCount + " succeeded");
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Failed to insert chapters batch", e);
            throw e;
        }
    }

    /**
     * Delete all chapters by book ID
     */
    public void deleteChaptersByBookId(int bookId) throws SQLException {
        String sql = "DELETE FROM [Chapter] WHERE [BookID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, bookId);
            int deleted = ps.executeUpdate();
            
            logger.info("✅ Deleted " + deleted + " chapters for book ID: " + bookId);
        }
    }
    
    /**
     * Check if chapter has summary
     */
    public boolean hasSummary(int chapterId) throws SQLException {
        String sql = "SELECT [Summary] FROM [Chapter] WHERE [ChapterID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, chapterId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String summary = rs.getString("Summary");
                    return summary != null && !summary.trim().isEmpty();
                }
            }
        }
        return false;
    }
    
    /**
     * Get chapter count by book ID
     */
    public int getChapterCountByBookId(int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM [Chapter] WHERE [BookID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, bookId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}