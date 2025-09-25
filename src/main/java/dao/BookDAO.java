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
        String sql = "INSERT INTO [Book] ([Title], [Author], [Major], [Description], [FilePath]) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getMajor());
            ps.setString(4, book.getDescription());
            ps.setString(5, book.getFilePath());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Insert thất bại, không có dòng nào được thêm.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("Insert thành công nhưng không lấy được BookID.");
            }
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
                    books.add(new Book(
                        rs.getInt("BookID"),
                        rs.getString("Title"),
                        rs.getString("Author"),
                        rs.getString("Description"),
                        rs.getString("Major"),
                        rs.getString("FilePath")
                    ));
                }
            }
        }
        return books;
    }


    public Book getBookById(int id) throws SQLException {
        String sql = "SELECT [BookID], [Title], [Author], [Description], [Major], [FilePath] FROM [Book] WHERE [BookID] = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Book(
                        rs.getInt("BookID"),
                        rs.getString("Title"),
                        rs.getString("Author"),
                        rs.getString("Description"),
                        rs.getString("Major"),
                        rs.getString("FilePath")
                    );
                }
            }
        }
        return null;
    }

    public boolean updateBook(Book book) throws SQLException {
        String sql = "UPDATE [Book] SET [Title] = ?, [Author] = ?, [Major] = ?, [Description] = ?, [FilePath] = ? WHERE [BookID] = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getMajor());
            ps.setString(4, book.getDescription());
            ps.setString(5, book.getFilePath());
            ps.setInt(6, book.getBookID());

            return ps.executeUpdate() > 0;
        }
    }

   
    public boolean deleteBook(int id) throws SQLException {
        String sql = "DELETE FROM [Book] WHERE [BookID] = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
}
