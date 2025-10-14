package controller.Book;

import dao.BookDAO;
import dao.ChapterDAO;
import model.Book;
import model.Chapter;

// THAY ĐỔI NÀY:
// import service.GeminiService;
import service.OllamaService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BookDetailServlet extends HttpServlet {
    
    private BookDAO bookDAO;
    private ChapterDAO chapterDAO;
    
    // THAY ĐỔI NÀY:
    // private GeminiService geminiService;
    private OllamaService ollamaService;
    
    @Override
    public void init() {
        bookDAO = new BookDAO();
        chapterDAO = new ChapterDAO();
        
   
        ollamaService = new OllamaService();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String idParam = request.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            response.sendRedirect("bookcrud?action=list");
            return;
        }
        
        try {
            int bookId = Integer.parseInt(idParam);
            Book book = bookDAO.getBookById(bookId);
            
            if (book == null) {
                response.sendRedirect("bookcrud?action=list");
                return;
            }
            
            List<Chapter> chapters = chapterDAO.getChaptersByBookId(bookId);

            for (Chapter chap : chapters) {
                if (chap.getSummary() == null || chap.getSummary().trim().isEmpty()) {
                    try {
                        String summary = ollamaService.generateChapterSummary(
                            chap.getTitle(), 
                            chap.getContent()
                        );
                        chap.setSummary(summary);
                        
                    } catch (Exception e) {
                        Logger.getLogger(BookDetailServlet.class.getName())
                            .log(Level.WARNING, "Failed to generate summary", e);
                        chap.setSummary("Không thể tóm tắt chương này.");
                    }
                }
            }
            
            request.setAttribute("book", book);
            request.setAttribute("chapters", chapters);
            request.getRequestDispatcher("/book-detail.jsp").forward(request, response);
            
        } catch (NumberFormatException e) {
            response.sendRedirect("bookcrud?action=list");
        } catch (SQLException ex) {
            Logger.getLogger(BookDetailServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}