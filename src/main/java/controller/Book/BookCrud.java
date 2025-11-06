package controller.Book;

import dao.BookDAO;
import dao.ChapterDAO;
import model.Book;
import model.Chapter;
import service.SmartChapterExtractor;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,    
    maxFileSize = 1024 * 1024 * 50,      
    maxRequestSize = 1024 * 1024 * 100   
)
public class BookCrud extends HttpServlet {

    private BookDAO bookDAO;
    private ChapterDAO chapterDAO;
    private service.BookValidator bookValidator;
    private static final Logger logger = Logger.getLogger(BookCrud.class.getName());

    @Override
    public void init() throws ServletException {
        bookDAO = new BookDAO();
        chapterDAO = new ChapterDAO();
        bookValidator = new service.BookValidator(bookDAO);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if (isEmpty(action)) action = "list";

        try {
            switch (action) {
                case "new"    -> showNewForm(request, response);
                case "insert" -> insertBook(request, response);
                case "edit"   -> showEditForm(request, response);
                case "update" -> updateBook(request, response);
                case "delete" -> deleteBook(request, response);
                case "upload" -> uploadBookWithPDF(request, response);
                case "list"   -> listBooks(request, response);
                default       -> listBooks(request, response);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in BookCrud servlet", e);
            request.setAttribute("error", "Hệ thống gặp lỗi: " + e.getMessage());
            request.getRequestDispatcher("error.jsp").forward(request, response);
        }
    }

    private void listBooks(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {
        
        List<Book> listBook = bookDAO.getAllBooks(0, 50);
        request.setAttribute("listBook", listBook);
        request.getRequestDispatcher("book-list.jsp").forward(request, response);
    }

    private void showNewForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("book-form.jsp").forward(request, response);
    }

    private void insertBook(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        String title = request.getParameter("title");
        String author = request.getParameter("author");
        String major = request.getParameter("major");
        String description = request.getParameter("description");

        if (isEmpty(title) || isEmpty(author)) {
            response.sendRedirect("bookcrud?action=new&error=Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        Book newBook = new Book(title, author, major, description);
        int bookId = bookDAO.insertBook(newBook);

        response.sendRedirect("bookcrud?action=list&success=Thêm sách thành công! ID=" + bookId);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {
        String idParam = request.getParameter("id");
        if (isEmpty(idParam)) {
            response.sendRedirect("bookcrud?action=list");
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            Book existingBook = bookDAO.getBookById(id);
            if (existingBook == null) {
                response.sendRedirect("bookcrud?action=list&error=Sách không tồn tại.");
                return;
            }
            request.setAttribute("book", existingBook);
            request.getRequestDispatcher("book-form.jsp").forward(request, response);
        } catch (NumberFormatException e) {
            response.sendRedirect("bookcrud?action=list&error=ID sách không hợp lệ.");
        }
    }

    private void updateBook(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        String idParam = request.getParameter("id");
        if (isEmpty(idParam)) {
            response.sendRedirect("bookcrud?action=list&error=ID không hợp lệ.");
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            String title = request.getParameter("title");
            String author = request.getParameter("author");
            String major = request.getParameter("major");
            String description = request.getParameter("description");

            if (isEmpty(title) || isEmpty(author)) {
                response.sendRedirect("bookcrud?action=edit&id=" + id + "&error=Vui lòng nhập đầy đủ thông tin.");
                return;
            }


            Book book = new Book(id, title, author, major, description);
            boolean success = bookDAO.updateBook(book);

            if (success) {
                response.sendRedirect("bookcrud?action=list&success=Cập nhật sách thành công!");
            } else {
                response.sendRedirect("bookcrud?action=edit&id=" + id + "&error=Cập nhật thất bại.");
            }
        } catch (NumberFormatException e) {
            response.sendRedirect("bookcrud?action=list&error=ID sách không hợp lệ.");
        }
    }

    private void deleteBook(HttpServletRequest request, HttpServletResponse response)
            throws IOException, SQLException {
        String idParam = request.getParameter("id");
        if (isEmpty(idParam)) {
            response.sendRedirect("bookcrud?action=list&error=ID không hợp lệ.");
            return;
        }

        try {
            int bookId = Integer.parseInt(idParam);
            
            logger.info("==========================================");
            logger.info("DELETING BOOK ID: " + bookId);
            logger.info("==========================================");
            

            List<Chapter> chapters = chapterDAO.getChaptersByBookId(bookId);
            logger.info("Found " + chapters.size() + " chapters to delete");
            

         
            
            chapterDAO.deleteChaptersByBookId(bookId);
            logger.info("✅ Deleted all chapters");
            

            boolean success = bookDAO.deleteBook(bookId);

            if (success) {
                logger.info("✅✅✅ BOOK DELETED SUCCESSFULLY ✅✅✅");
                response.sendRedirect("bookcrud?action=list&success=Xóa sách thành công!");
            } else {
                response.sendRedirect("bookcrud?action=list&error=Xóa sách thất bại.");
            }
        } catch (NumberFormatException e) {
            response.sendRedirect("bookcrud?action=list&error=ID sách không hợp lệ.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during book deletion", e);
            response.sendRedirect("bookcrud?action=list&error=Lỗi database: " + e.getMessage());
        }
    }
    
    private void uploadBookWithPDF(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Part filePart = request.getPart("pdfFile");
        String title = request.getParameter("title");
        String author = request.getParameter("author");
        String major = request.getParameter("major");
        String description = request.getParameter("description");

        if (filePart == null || filePart.getSize() == 0) {
            request.setAttribute("error", "Vui lòng chọn file PDF.");
            request.getRequestDispatcher("book-upload.jsp").forward(request, response);
            return;
        }

        if (isEmpty(title) || isEmpty(author)) {
            request.setAttribute("error", "Vui lòng nhập tiêu đề và tác giả.");
            request.getRequestDispatcher("book-upload.jsp").forward(request, response);
            return;
        }

        String uploadPath = config.FileUploadConfig.getUploadPath();
        File uploadDir = config.FileUploadConfig.getUploadDir();
        
        logger.info("==========================================");
        logger.info("UPLOAD CONFIGURATION");
        logger.info("==========================================");
        logger.info("Upload directory: " + uploadPath);
        logger.info("Directory exists: " + uploadDir.exists());
        logger.info("Directory writable: " + uploadDir.canWrite());

        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        String safeFileName = System.currentTimeMillis() + "_" + fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String filePath = uploadPath + File.separator + safeFileName;
        
        logger.info("Target file path: " + filePath);
        
        filePart.write(filePath);
        
        File savedFile = new File(filePath);
        if (!savedFile.exists()) {
            logger.severe("❌ File was not saved successfully!");
            request.setAttribute("error", "Không thể lưu file PDF.");
            request.getRequestDispatcher("book-upload.jsp").forward(request, response);
            return;
        }
        
        logger.info("✅ File saved successfully: " + savedFile.length() + " bytes");
        logger.info("   Absolute path: " + savedFile.getAbsolutePath());
        

        SmartChapterExtractor.ExtractionResult extractionResult = null;
        int bookId = -1;
        List<Chapter> chapters = null;
        
        try {

            logger.info("==========================================");
            logger.info("STEP 1: SMART CHAPTER EXTRACTION");
            logger.info("==========================================");
            
            SmartChapterExtractor extractor = new SmartChapterExtractor();
            extractionResult = extractor.extractChapters(filePath);
            
            if (!extractionResult.success || extractionResult.chapters.isEmpty()) {
                throw new Exception("Extraction failed: " + 
                    (extractionResult.error != null ? extractionResult.error : "No chapters detected"));
            }
            
            chapters = extractionResult.chapters;
            logger.info("✅ Extraction method: " + extractionResult.method);
            logger.info("✅ Total chapters detected: " + chapters.size());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ STEP 1 FAILED: Chapter extraction", e);
            savedFile.delete();
            logger.info("Deleted file due to extraction failure");
            request.setAttribute("error", "Không thể phát hiện chương trong PDF: " + e.getMessage());
            request.getRequestDispatcher("book-upload.jsp").forward(request, response);
            return;
        }
        
        try {
            logger.info("==========================================");
            logger.info("STEP 2: SAVING BOOK TO DATABASE");
            logger.info("==========================================");
            
 
            Book book = new Book(title, author, major, description);
            book.setFilePath("/viewpdf?file=" + safeFileName);
            
            logger.info("Book object created: " + book.toString());
            
            bookId = bookDAO.insertBook(book);
            
            if (bookId <= 0) {
                throw new SQLException("BookDAO.insertBook returned invalid ID: " + bookId);
            }
            logger.info("✅ Book saved with ID: " + bookId);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ STEP 2 FAILED: Database insert", e);
            savedFile.delete();
            logger.info("Deleted file due to database failure");
            request.setAttribute("error", "Không thể lưu thông tin sách vào database: " + e.getMessage());
            request.getRequestDispatcher("book-upload.jsp").forward(request, response);
            return;
        }
        
        try {
            logger.info("==========================================");
            logger.info("STEP 3: SAVING CHAPTERS");
            logger.info("==========================================");
            
            for (Chapter chapter : chapters) {
                chapter.setBookID(bookId);
                chapter.setSummary(null);
            }
            
            chapterDAO.insertChaptersBatch(chapters);
            logger.info("✅ Saved " + chapters.size() + " chapters");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ STEP 3 FAILED: Chapter save", e);
            try {
                bookDAO.deleteBook(bookId);
                logger.info("Rolled back book ID: " + bookId);
                savedFile.delete();
                logger.info("Deleted file after rollback");
            } catch (Exception rollbackError) {
                logger.log(Level.SEVERE, "Rollback failed!", rollbackError);
            }
            request.setAttribute("error", "Không thể lưu chapters: " + e.getMessage());
            request.getRequestDispatcher("book-upload.jsp").forward(request, response);
            return;
        }
        try {
            logger.info("==========================================");
            logger.info("STEP 4: INDEXING TO FAISS (OPTIONAL)");
            logger.info("==========================================");
            
            service.FAISSService faissService = new service.FAISSService();
            service.LocalEmbeddingService embeddingService = new service.LocalEmbeddingService();
            
            if (!embeddingService.isHealthy()) {
                logger.warning(" Embedding service not available - skipping FAISS");
            } else {
                logger.info("Embedding service available");
                
                List<Chapter> savedChapters = chapterDAO.getChaptersByBookId(bookId);
                
                try {
                    faissService.createIndex(384);
                } catch (Exception e) {
                    logger.info("FAISS index already exists");
                }
                
                List<Integer> chapterIds = new ArrayList<>();
                List<float[]> embeddings = new ArrayList<>();
                
                for (Chapter chapter : savedChapters) {
                    try {
                        String textToEmbed = chapter.getTitle() + ". " + 
                            chapter.getContent().substring(0, Math.min(1000, chapter.getContent().length()));
                        
                        float[] embedding = embeddingService.generateEmbedding(textToEmbed);
                        chapterIds.add(chapter.getChapterID());
                        embeddings.add(embedding);
                        
                    } catch (Exception e) {
                        logger.warning("Embedding failed for chapter " + chapter.getChapterNumber());
                    }
                }
                
                if (!chapterIds.isEmpty()) {
                    faissService.addVectors(chapterIds, embeddings);
                    faissService.saveIndex();
                    logger.info("✅ Indexed " + chapterIds.size() + " chapters");
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "⚠️ FAISS indexing failed (non-critical): " + e.getMessage());
        }

        // ✅ SUCCESS!
        logger.info("==========================================");
        logger.info("✅✅✅ UPLOAD SUCCESSFUL ✅✅✅");
        logger.info("==========================================");
        logger.info("Book ID: " + bookId);
        logger.info("File: " + safeFileName);
        logger.info("Chapters: " + chapters.size());
        logger.info("==========================================");
        
        String message = String.format(
            "✅ Upload thành công! Sách '%s' với %d chương đã được lưu (ID: %d)",
            title, chapters.size(), bookId
        );
        
        request.getSession().setAttribute("successMessage", message);
        response.sendRedirect("bookcrud?action=list");
    }

    // ========================= UTILITY =========================

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}