package controller.Book;

import dao.BookDAO;
import dao.ChapterDAO;
import model.Book;
import model.Chapter;
import service.PDFExtractor;
import service.ChapterAIDetector;

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
    private static final Logger logger = Logger.getLogger(BookCrud.class.getName());

    @Override
    public void init() throws ServletException {
        bookDAO = new BookDAO();
        chapterDAO = new ChapterDAO();
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
            int id = Integer.parseInt(idParam);
            
            chapterDAO.deleteChaptersByBookId(id);
            boolean success = bookDAO.deleteBook(id);

            if (success) {
                response.sendRedirect("bookcrud?action=list&success=Xóa sách thành công!");
            } else {
                response.sendRedirect("bookcrud?action=list&error=Xóa sách thất bại.");
            }
        } catch (NumberFormatException e) {
            response.sendRedirect("bookcrud?action=list&error=ID sách không hợp lệ.");
        }
    }

  private void uploadBookWithPDF(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    Part filePart = request.getPart("pdfFile");
    String title = request.getParameter("title");
    String author = request.getParameter("author");
    String major = request.getParameter("major");
    String description = request.getParameter("description");

    // Validation
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

    // Setup upload directory
    String uploadPath = getServletContext().getRealPath("/uploads");
    File uploadDir = new File(uploadPath);
    if (!uploadDir.exists()) uploadDir.mkdirs();

    // Save file
    String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
    String safeFileName = System.currentTimeMillis() + "_" + fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
    String filePath = uploadPath + File.separator + safeFileName;
    filePart.write(filePath);

    try {
        // Step 1: Extract text from PDF
        logger.info("Step 1: Extracting text from PDF...");
        String fullText = PDFExtractor.extractText(filePath);
        if (isEmpty(fullText)) {
            throw new Exception("Không thể trích xuất nội dung từ file PDF.");
        }
        logger.info("✅ Extracted " + fullText.length() + " characters");

        // Step 2: Detect chapters using regex patterns
        logger.info("Step 2: Detecting chapters with regex patterns...");
        service.ChapterDetector detector = new service.ChapterDetector();
        List<Chapter> chapters = detector.detectChapters(fullText);
        
        if (chapters == null || chapters.isEmpty()) {
            throw new Exception("Không thể phát hiện chương.");
        }
        logger.info("✅ Detected " + chapters.size() + " chapters");

        // Step 3: Save book to database
        logger.info("Step 3: Saving book to database...");
        Book book = new Book(title, author, major, description);
        book.setFilePath("/uploads/" + safeFileName);
        int bookId = bookDAO.insertBook(book);
        
        if (bookId <= 0) {
            throw new Exception("Không thể lưu sách vào database.");
        }
        logger.info("✅ Book saved with ID: " + bookId);

        // Step 4: Generate summaries with Ollama
        logger.info("Step 4: Generating summaries with Ollama...");
        service.OllamaService ollamaService = new service.OllamaService();
        
        if (!ollamaService.isServerHealthy()) {
            logger.warning("⚠️ Ollama Flask API not running!");
            logger.warning("   Chapters will be saved without AI summaries.");
            logger.warning("   To enable summaries: python app.py (port 5001)");
            
            for (Chapter chapter : chapters) {
                chapter.setBookID(bookId);
                chapter.setSummary("Chương " + chapter.getChapterNumber() + ": " + chapter.getTitle());
            }
        } else {
            logger.info("✅ Ollama Flask API is running, generating summaries...");
            int successCount = 0;
            
            for (Chapter chapter : chapters) {
                chapter.setBookID(bookId);
                
                try {
                    String contentSample = chapter.getContent().substring(0, 
                        Math.min(3000, chapter.getContent().length()));
                    
                    String summary = ollamaService.generateChapterSummary(
                        chapter.getTitle(), contentSample);
                    
                    chapter.setSummary(summary);
                    successCount++;
                    
                    logger.info("  ✓ Chapter " + chapter.getChapterNumber() + " summarized");
                    
                } catch (Exception e) {
                    logger.warning("⚠️ Summary failed for chapter " + chapter.getChapterNumber() + 
                                 ": " + e.getMessage());
                    chapter.setSummary("Chương " + chapter.getChapterNumber() + ": " + chapter.getTitle());
                }
            }
            
            logger.info("✅ Generated " + successCount + "/" + chapters.size() + " summaries");
        }
        
        // Save chapters to database
        chapterDAO.insertChaptersBatch(chapters);
        logger.info("✅ Chapters saved to database");

        // Step 5: Index chapters to FAISS
        logger.info("Step 5: Indexing chapters to FAISS...");
        try {
            service.FAISSService faissService = new service.FAISSService();
            service.LocalEmbeddingService embeddingService = new service.LocalEmbeddingService();
            
            if (!embeddingService.isHealthy()) {
                logger.warning("⚠️ Embedding service not available (port 5001)");
                logger.warning("   FAISS indexing skipped.");
                logger.warning("   To enable: python app.py with /embed endpoint");
            } else {
                logger.info("✅ Embedding service available, indexing...");
                
                List<Chapter> savedChapters = chapterDAO.getChaptersByBookId(bookId);
                
                // Create index if not exists
                try {
                    faissService.createIndex(384);
                    logger.info("  Created new FAISS index with 384 dimensions");
                } catch (Exception e) {
                    logger.info("  FAISS index already exists or create failed, continuing...");
                }
                
                List<Integer> chapterIds = new ArrayList<>();
                List<float[]> embeddings = new ArrayList<>();
                
                for (Chapter chapter : savedChapters) {
                    try {
                        // Embed: title + summary + content sample
                        String textToEmbed = chapter.getTitle() + ". " + 
                            (chapter.getSummary() != null ? chapter.getSummary() + ". " : "") +
                            chapter.getContent().substring(0, Math.min(1000, chapter.getContent().length()));
                        
                        float[] embedding = embeddingService.generateEmbedding(textToEmbed);
                        
                        chapterIds.add(chapter.getChapterID());
                        embeddings.add(embedding);
                        
                        logger.info("  ✓ Embedded chapter " + chapter.getChapterNumber());
                        
                    } catch (Exception e) {
                        logger.warning("  ✗ Embedding failed for chapter " + chapter.getChapterNumber() + 
                                     ": " + e.getMessage());
                    }
                }
                
                if (!chapterIds.isEmpty()) {
                    faissService.addVectors(chapterIds, embeddings);
                    faissService.saveIndex();
                    logger.info("✅ Indexed " + chapterIds.size() + " chapters to FAISS");
                } else {
                    logger.warning("⚠️ No chapters were embedded");
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "FAISS indexing failed (non-critical): " + e.getMessage(), e);
            logger.warning("   Book and chapters were saved successfully.");
            logger.warning("   Only FAISS indexing failed - RAG features may not work.");
        }

        // Success message
        String message = "✅ Upload thành công! " + chapters.size() + " chương đã được xử lý.";
        request.getSession().setAttribute("successMessage", message);
        response.sendRedirect("bookcrud?action=list");

    } catch (Exception e) {
        logger.log(Level.SEVERE, "Error processing PDF upload", e);
        
        // Delete uploaded file on error
        File uploadedFile = new File(filePath);
        if (uploadedFile.exists()) {
            uploadedFile.delete();
            logger.info("Deleted failed upload: " + safeFileName);
        }
        
        request.setAttribute("error", "Lỗi xử lý file: " + e.getMessage());
        request.getRequestDispatcher("book-upload.jsp").forward(request, response);
    }
}

    // ========================= UTILITY =========================

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}