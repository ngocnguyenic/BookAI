package service;

import dao.BookDAO;
import model.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Service để validate Book trước khi insert/update
 * Đảm bảo data integrity và business rules
 */
public class BookValidator {
    
    private static final Logger logger = Logger.getLogger(BookValidator.class.getName());
    
    // Validation constants
    private static final int MIN_TITLE_LENGTH = 1;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MIN_AUTHOR_LENGTH = 1;
    private static final int MAX_AUTHOR_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_MAJOR_LENGTH = 100;
    
    // Regex patterns
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[<>\"'&]");
    private static final Pattern SQL_INJECTION = Pattern.compile("(?i)(--|;|'|\"|\\/\\*|\\*\\/|xp_|sp_|exec|execute|drop|delete|insert|update|union|select)");
    
    private final BookDAO bookDAO;
    
    public BookValidator(BookDAO bookDAO) {
        this.bookDAO = bookDAO;
    }
    
    /**
     * Validation Result với error messages
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = new ArrayList<>(errors);
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, new ArrayList<>());
        }
        
        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
        
        public String getErrorMessageHtml() {
            if (errors.isEmpty()) return "";
            StringBuilder html = new StringBuilder("<ul>");
            for (String error : errors) {
                html.append("<li>").append(error).append("</li>");
            }
            html.append("</ul>");
            return html.toString();
        }
    }
    
    // ==================== PUBLIC VALIDATION METHODS ====================
    
    /**
     * Validate khi INSERT (không có BookID)
     */
    public ValidationResult validateForInsert(Book book) {
        List<String> errors = new ArrayList<>();
        
        // Basic validations
        validateTitle(book.getTitle(), errors);
        validateAuthor(book.getAuthor(), errors);
        validateMajor(book.getMajor(), errors);
        validateDescription(book.getDescription(), errors);
        
        // Business rule validations
        try {
            checkDuplicateTitle(book.getTitle(), null, errors);
            
            if (book.getFilePath() != null && !book.getFilePath().trim().isEmpty()) {
                checkDuplicateFilePath(book.getFilePath(), null, errors);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during validation", e);
            errors.add("Lỗi kiểm tra database: " + e.getMessage());
        }
        
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
    
    /**
     * Validate khi UPDATE (có BookID)
     */
    public ValidationResult validateForUpdate(Book book) {
        List<String> errors = new ArrayList<>();
        
        // Check BookID exists
        if (book.getBookID() <= 0) {
            errors.add("BookID không hợp lệ");
            return ValidationResult.failure(errors);
        }
        
        // Basic validations
        validateTitle(book.getTitle(), errors);
        validateAuthor(book.getAuthor(), errors);
        validateMajor(book.getMajor(), errors);
        validateDescription(book.getDescription(), errors);
        
        // Business rule validations (exclude current book)
        try {
            checkDuplicateTitle(book.getTitle(), book.getBookID(), errors);
            
            if (book.getFilePath() != null && !book.getFilePath().trim().isEmpty()) {
                checkDuplicateFilePath(book.getFilePath(), book.getBookID(), errors);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during validation", e);
            errors.add("Lỗi kiểm tra database: " + e.getMessage());
        }
        
        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }
    
    // ==================== FIELD VALIDATIONS ====================
    
    /**
     * Validate Title
     */
    private void validateTitle(String title, List<String> errors) {
        if (title == null || title.trim().isEmpty()) {
            errors.add("❌ Tiêu đề không được để trống");
            return;
        }
        
        String trimmed = title.trim();
        
        if (trimmed.length() < MIN_TITLE_LENGTH) {
            errors.add("❌ Tiêu đề quá ngắn (tối thiểu " + MIN_TITLE_LENGTH + " ký tự)");
        }
        
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            errors.add("❌ Tiêu đề quá dài (tối đa " + MAX_TITLE_LENGTH + " ký tự)");
        }
        
        // Check for dangerous characters
        if (SPECIAL_CHARS.matcher(trimmed).find()) {
            errors.add("⚠️ Tiêu đề chứa ký tự đặc biệt không hợp lệ: < > \" ' &");
        }
        
        // Check for SQL injection attempts
        if (SQL_INJECTION.matcher(trimmed).find()) {
            errors.add("⚠️ Tiêu đề chứa từ khóa không được phép");
        }
    }
    
    /**
     * Validate Author
     */
    private void validateAuthor(String author, List<String> errors) {
        if (author == null || author.trim().isEmpty()) {
            errors.add("❌ Tác giả không được để trống");
            return;
        }
        
        String trimmed = author.trim();
        
        if (trimmed.length() < MIN_AUTHOR_LENGTH) {
            errors.add("❌ Tên tác giả quá ngắn (tối thiểu " + MIN_AUTHOR_LENGTH + " ký tự)");
        }
        
        if (trimmed.length() > MAX_AUTHOR_LENGTH) {
            errors.add("❌ Tên tác giả quá dài (tối đa " + MAX_AUTHOR_LENGTH + " ký tự)");
        }
        
        if (SPECIAL_CHARS.matcher(trimmed).find()) {
            errors.add("⚠️ Tên tác giả chứa ký tự đặc biệt không hợp lệ");
        }
    }
    
    /**
     * Validate Major (optional field)
     */
    private void validateMajor(String major, List<String> errors) {
        if (major == null || major.trim().isEmpty()) {
            return; // Major is optional
        }
        
        String trimmed = major.trim();
        
        if (trimmed.length() > MAX_MAJOR_LENGTH) {
            errors.add("❌ Chuyên ngành quá dài (tối đa " + MAX_MAJOR_LENGTH + " ký tự)");
        }
    }
    
    /**
     * Validate Description (optional field)
     */
    private void validateDescription(String description, List<String> errors) {
        if (description == null || description.trim().isEmpty()) {
            return; // Description is optional
        }
        
        String trimmed = description.trim();
        
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            errors.add("❌ Mô tả quá dài (tối đa " + MAX_DESCRIPTION_LENGTH + " ký tự)");
        }
    }
    
    // ==================== BUSINESS RULE VALIDATIONS ====================
    
    /**
     * Check duplicate title (case-insensitive)
     * @param title Title to check
     * @param excludeBookId Book ID to exclude (for update), null for insert
     * @param errors Error list to append to
     */
    private void checkDuplicateTitle(String title, Integer excludeBookId, List<String> errors) 
            throws SQLException {
        
        if (title == null || title.trim().isEmpty()) {
            return;
        }
        
        boolean exists = bookDAO.isTitleExists(title, excludeBookId);
        
        if (exists) {
            errors.add("❌ Tiêu đề '" + title.trim() + "' đã tồn tại trong hệ thống");
            logger.warning("Duplicate title validation failed: " + title);
        }
    }
    
    /**
     * Check duplicate file path
     * @param filePath File path to check
     * @param excludeBookId Book ID to exclude (for update), null for insert
     * @param errors Error list to append to
     */
    private void checkDuplicateFilePath(String filePath, Integer excludeBookId, List<String> errors) 
            throws SQLException {
        
        if (filePath == null || filePath.trim().isEmpty()) {
            return;
        }
        
        String existingTitle = bookDAO.getBookTitleByFilePath(filePath, excludeBookId);
        
        if (existingTitle != null) {
            errors.add("❌ File PDF này đã được sử dụng cho sách: '" + existingTitle + "'");
            logger.warning("Duplicate file path validation failed: " + filePath);
        }
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Quick validation - chỉ check required fields
     */
    public boolean isValidQuick(Book book) {
        return book != null 
            && book.getTitle() != null && !book.getTitle().trim().isEmpty()
            && book.getAuthor() != null && !book.getAuthor().trim().isEmpty();
    }
    
    /**
     * Sanitize input - remove dangerous characters
     */
    public String sanitize(String input) {
        if (input == null) return null;
        return input.trim()
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;");
    }
    
    /**
     * Log validation result
     */
    public void logValidationResult(ValidationResult result, String operation) {
        if (result.isValid()) {
            logger.info("✅ Validation passed for " + operation);
        } else {
            logger.warning("❌ Validation failed for " + operation + ": " + result.getErrorMessage());
        }
    }
}