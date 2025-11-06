package dao;

import connect.DBConnection;
import model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class BookDAO {

    private static final Logger logger = Logger.getLogger(BookDAO.class.getName());

    public int insertBook(Book book) throws SQLException {
        String sql = "INSERT INTO [Book] ([Title], [Author], [Major], [Description], [FilePath]) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getMajor());
            ps.setString(4, book.getDescription());
            ps.setString(5, book.getFilePath());

            logger.info("Inserting book: " + book.toString());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Insert thất bại, không có dòng nào được thêm.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    logger.info("✅ Book inserted successfully with ID: " + generatedId);
                    return generatedId;
                }
                throw new SQLException("Insert thành công nhưng không lấy được BookID.");
            }
        } catch (SQLException e) {
            logger.severe("❌ Insert book failed: " + e.getMessage());
            throw e;
        }
    }

    public List<Book> getAllBooks(int offset, int limit) throws SQLException {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT [BookID], [Title], [Author], [Description], [Major], [FilePath] " +
                     "FROM [Book] ORDER BY [BookID] DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, offset);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // ✅ Đúng thứ tự: bookID, title, author, description, major, filePath
                    books.add(new Book(
                        rs.getInt("BookID"),
                        rs.getString("Title"),
                        rs.getString("Author"),
                        rs.getString("Description"),  // ✅ Sửa
                        rs.getString("Major"),        // ✅ Sửa
                        rs.getString("FilePath")
                    ));
                }
            }
        }
        return books;
    }


    public Book getBookById(int id) throws SQLException {
        String sql = "SELECT [BookID], [Title], [Author], [Description], [Major], [FilePath] " +
                     "FROM [Book] WHERE [BookID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // ✅ Đúng thứ tự
                    return new Book(
                        rs.getInt("BookID"),
                        rs.getString("Title"),
                        rs.getString("Author"),
                        rs.getString("Description"),  // ✅ Sửa
                        rs.getString("Major"),        // ✅ Sửa
                        rs.getString("FilePath")
                    );
                }
            }
        }
        return null;
    }

    public boolean updateBook(Book book) throws SQLException {
        String sql = "UPDATE [Book] SET [Title] = ?, [Author] = ?, [Major] = ?, [Description] = ?, [FilePath] = ? " +
                     "WHERE [BookID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getMajor());
            ps.setString(4, book.getDescription());
            ps.setString(5, book.getFilePath());
            ps.setInt(6, book.getBookID());

            logger.info("Updating book ID " + book.getBookID());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteBook(int id) throws SQLException {
        String sql = "DELETE FROM [Book] WHERE [BookID] = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            boolean deleted = ps.executeUpdate() > 0;
            if (deleted) {
                logger.info("✅ Book ID " + id + " deleted");
            }
            return deleted;
        }
    }

    public boolean isTitleExists(String title, Integer excludeBookId) throws SQLException {
        String sql = excludeBookId != null
            ? "SELECT COUNT(*) FROM [Book] WHERE LOWER(LTRIM(RTRIM([Title]))) = LOWER(LTRIM(RTRIM(?))) AND [BookID] != ?"
            : "SELECT COUNT(*) FROM [Book] WHERE LOWER(LTRIM(RTRIM([Title]))) = LOWER(LTRIM(RTRIM(?)))";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, title.trim());
            if (excludeBookId != null) {
                ps.setInt(2, excludeBookId);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean exists = rs.getInt(1) > 0;
                    if (exists) {
                        logger.warning("Duplicate title found: " + title);
                    }
                    return exists;
                }
            }
        }
        return false;
    }
    
    public String getBookTitleByFilePath(String filePath, Integer excludeBookId) throws SQLException {
        if (filePath == null || filePath.trim().isEmpty()) {
            return null;
        }
        
        String sql = excludeBookId != null
            ? "SELECT [Title] FROM [Book] WHERE [FilePath] = ? AND [BookID] != ?"
            : "SELECT [Title] FROM [Book] WHERE [FilePath] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, filePath.trim());
            if (excludeBookId != null) {
                ps.setInt(2, excludeBookId);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String title = rs.getString("Title");
                    logger.warning("Duplicate file path found: " + filePath + " (used by: " + title + ")");
                    return title;
                }
            }
        }
        return null;
    }
}