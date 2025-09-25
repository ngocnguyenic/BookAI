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

    // ========================= UPLOAD PDF =========================

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

        // save file to uploads/
        String uploadPath = getServletContext().getRealPath("/uploads");
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
        String safeFileName = System.currentTimeMillis() + "_" + fileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String filePath = uploadPath + File.separator + safeFileName;
        filePart.write(filePath);

        try {
            // 1. extract text
            String fullText = PDFExtractor.extractText(filePath);
            if (isEmpty(fullText)) {
                throw new Exception("Không thể trích xuất nội dung từ file PDF.");
            }

            // 2. detect chapters bằng AI
            ChapterAIDetector aiDetector = new ChapterAIDetector();
            List<Chapter> chapters = aiDetector.detectChapters(fullText);
            if (chapters == null || chapters.isEmpty()) {
                throw new Exception("AI không thể chia chương.");
            }

            // 3. insert book
            Book book = new Book(title, author, major, description);
            book.setFilePath("/uploads/" + safeFileName);
            int bookId = bookDAO.insertBook(book);
            if (bookId <= 0) {
                throw new Exception("Không thể lưu thông tin sách.");
            }
            request.getSession().setAttribute("successMessage",
                "Upload sách và chia chương bằng AI thành công! Tổng: " + chapters.size() + " chương.");
            response.sendRedirect("bookcrud?action=list");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Lỗi khi xử lý upload sách", e);
            request.setAttribute("error", "Lỗi khi xử lý file: " + e.getMessage());
            request.getRequestDispatcher("book-upload.jsp").forward(request, response);
        }
    }

    // ========================= UTIL =========================

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
