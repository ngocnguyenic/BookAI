package controller.Book;

import dao.BookDAO;
import dao.ChapterDAO;
import model.Book;
import model.Chapter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class BookDetailServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(BookDetailServlet.class.getName());
    
    private BookDAO bookDAO;
    private ChapterDAO chapterDAO;
    
    @Override
    public void init() throws ServletException {
        bookDAO = new BookDAO();
        chapterDAO = new ChapterDAO();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String bookIdStr = request.getParameter("id");
        
        // Validation
        if (bookIdStr == null || bookIdStr.trim().isEmpty()) {
            logger.warning("❌ Missing book ID parameter");
            response.sendRedirect("bookcrud?action=list&error=Missing book ID");
            return;
        }
        
        try {
            int bookId = Integer.parseInt(bookIdStr);
            logger.info("📖 Loading book details for ID: " + bookId);
            
            // Get book info
            Book book = bookDAO.getBookById(bookId);
            
            if (book == null) {
                logger.warning("❌ Book not found: ID=" + bookId);
                response.sendRedirect("bookcrud?action=list&error=Book not found");
                return;
            }
            
            logger.info("✅ Book found: " + book.getTitle());
            
            List<Chapter> chapters = chapterDAO.getChaptersByBookId(bookId);
            
            logger.info("✅ Loaded " + chapters.size() + " chapters");
            long chaptersWithSummary = chapters.stream()
                .filter(c -> c.getSummary() != null && !c.getSummary().trim().isEmpty())
                .count();
            
            logger.info("  - Chapters with summary: " + chaptersWithSummary);
            logger.info("  - Chapters without summary: " + (chapters.size() - chaptersWithSummary));
            
            // Set attributes
            request.setAttribute("book", book);
            request.setAttribute("chapters", chapters);
            
            // Forward to JSP
            request.getRequestDispatcher("book-detail.jsp").forward(request, response);
            
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "❌ Invalid book ID format: " + bookIdStr, e);
            response.sendRedirect("bookcrud?action=list&error=Invalid book ID");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Database error: " + e.getMessage(), e);
            request.setAttribute("error", "Database error: " + e.getMessage());
            request.getRequestDispatcher("error.jsp").forward(request, response);
        }
    }
}