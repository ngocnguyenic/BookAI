package controller.Chapter;
import dao.ChapterDAO;
import model.Chapter;
import service.OllamaService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ChapterSummaryServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(ChapterSummaryServlet.class.getName());
    private ChapterDAO chapterDAO;
    private OllamaService ollamaService;
    private Gson gson;
    @Override
    public void init() throws ServletException {
        chapterDAO = new ChapterDAO();
        ollamaService = new OllamaService();
        gson = new Gson();
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String chapterIdStr = request.getParameter("chapterId");
            if (chapterIdStr == null || chapterIdStr.isEmpty()) {
                result.put("success", false);
                result.put("error", "Missing chapter ID");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            int chapterId = Integer.parseInt(chapterIdStr);
            logger.info("Generating summary for chapter ID: " + chapterId);
            Chapter chapter = chapterDAO.getChapterById(chapterId);
            if (chapter == null) {
                result.put("success", false);
                result.put("error", "Chapter not found");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            
            if (chapter.getSummary() != null && !chapter.getSummary().trim().isEmpty()) {
                logger.info("✅ Using cached summary for chapter " + chapter.getChapterNumber());
                result.put("success", true);
                result.put("summary", chapter.getSummary());
                result.put("cached", true);
                result.put("chapterNumber", chapter.getChapterNumber());
                result.put("chapterTitle", chapter.getTitle());
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            if (!ollamaService.isServerHealthy()) {
                logger.warning("⚠️ Ollama service is not available");
                result.put("success", false);
                result.put("error", "Ollama service không khả dụng. Vui lòng khởi động Flask API (python app.py)");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            
            logger.info("Generating NEW summary for chapter " + chapter.getChapterNumber() + "...");
            
            String contentSample = chapter.getContent().substring(0, 
                Math.min(3000, chapter.getContent().length()));
            
            String summary = ollamaService.generateChapterSummary(
                chapter.getTitle(), 
                contentSample
            );
            
            
            boolean updated = chapterDAO.updateChapterSummary(chapterId, summary);
            
            if (!updated) {
                result.put("success", false);
                result.put("error", "Failed to save summary to database");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            logger.info("✅ Summary generated and saved for chapter " + chapter.getChapterNumber());
            
            result.put("success", true);
            result.put("summary", summary);
            result.put("cached", false);
            result.put("chapterNumber", chapter.getChapterNumber());
            result.put("chapterTitle", chapter.getTitle());
            
        } catch (NumberFormatException e) {
            logger.warning("❌ Invalid chapter ID format: " + e.getMessage());
            result.put("success", false);
            result.put("error", "Invalid chapter ID format");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Database error: " + e.getMessage(), e);
            result.put("success", false);
            result.put("error", "Database error: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error generating summary: " + e.getMessage(), e);
            result.put("success", false);
            result.put("error", "Error: " + e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", "Use POST method to generate summaries");
        
        response.getWriter().write(gson.toJson(result));
    }
}