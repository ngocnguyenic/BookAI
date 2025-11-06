package controller.Chapter;

import dao.ChapterDAO;
import dao.QADao;
import model.Chapter;
import model.QA;
import service.OllamaService;
import service.TaggingService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.google.gson.Gson;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/api/chapter/generate-qa")
public class QAGenerationServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(QAGenerationServlet.class.getName());
    
    private ChapterDAO chapterDAO;
    private QADao qaDao;
    private OllamaService ollamaService;
    private TaggingService taggingService; 
    private Gson gson;
    
    private static final int DEFAULT_NUM_QUESTIONS = 5;
    
    @Override
    public void init() throws ServletException {
        chapterDAO = new ChapterDAO();
        qaDao = new QADao();
        ollamaService = new OllamaService();
        taggingService = new TaggingService(); 
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
            String numQuestionsStr = request.getParameter("numQuestions");
            
            if (chapterIdStr == null || chapterIdStr.isEmpty()) {
                result.put("success", false);
                result.put("error", "Missing chapter ID");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            int chapterId = Integer.parseInt(chapterIdStr);
            int numQuestions = DEFAULT_NUM_QUESTIONS;
            
            if (numQuestionsStr != null && !numQuestionsStr.isEmpty()) {
                numQuestions = Integer.parseInt(numQuestionsStr);
                numQuestions = Math.max(3, Math.min(10, numQuestions));
            }
            
            logger.info("🎯 Generating " + numQuestions + " MCQs for chapter ID: " + chapterId);
            
            // Check for existing Q&As
            int existingCount = qaDao.getQACountByChapterId(chapterId);
            if (existingCount > 0) {
                logger.info("✅ Using cached MCQs (" + existingCount + " found)");
                List<model.QA> qas = qaDao.getQAsByChapterId(chapterId);
                
                result.put("success", true);
                result.put("qas", qas);
                result.put("cached", true);
                result.put("count", qas.size());
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
 
            Chapter chapter = chapterDAO.getChapterById(chapterId);
            if (chapter == null) {
                result.put("success", false);
                result.put("error", "Chapter not found");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
         
            if (chapter.getSummary() == null || chapter.getSummary().trim().isEmpty()) {
                logger.warning("⚠️ No summary found for chapter " + chapter.getChapterNumber());
                result.put("success", false);
                result.put("error", "Vui lòng tạo tóm tắt trước khi tạo câu hỏi");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            // Check Ollama health
            if (!ollamaService.isServerHealthy()) {
                logger.warning("⚠️ Ollama service is not available");
                result.put("success", false);
                result.put("error", "Ollama service không khả dụng. Vui lòng khởi động Flask API");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            logger.info("🤖 Generating NEW MCQs for chapter " + chapter.getChapterNumber() + "...");
            
            // Generate Q&As
            List<QADao.QAItem> qaItems = ollamaService.generateMCQ(chapter.getSummary(), numQuestions);
            
            if (qaItems.isEmpty()) {
                result.put("success", false);
                result.put("error", "Không thể tạo câu hỏi từ tóm tắt này");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            // Insert Q&As into database
            int inserted = qaDao.insertQABatch(chapterId, qaItems, "ollama");
            
            if (inserted == 0) {
                result.put("success", false);
                result.put("error", "Failed to save MCQs to database");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            logger.info("✅ Successfully saved " + inserted + " MCQs");
            
            // ✅ NEW: Auto-tag all newly created Q&As
            logger.info("🏷️ Starting auto-tagging for " + inserted + " Q&As...");
            
            List<model.QA> savedQAs = qaDao.getQAsByChapterId(chapterId);
            
            // Tag in background thread to avoid timeout
            new Thread(() -> {
                int tagged = 0;
                int failed = 0;
                
                for (QA qa : savedQAs) {
                    if (!qa.isAutoTagged()) {
                        try {
                            logger.info("🏷️ Tagging Q&A #" + qa.getQAID());
                            taggingService.tagQA(qa);
                            tagged++;
                            
                            // Rate limiting
                            Thread.sleep(1000); // 1 second delay between tags
                            
                        } catch (Exception e) {
                            logger.severe("❌ Failed to tag Q&A #" + qa.getQAID() + ": " + e.getMessage());
                            failed++;
                        }
                    }
                }
                
                logger.info("✅ Auto-tagging completed: " + tagged + " tagged, " + failed + " failed");
                
            }).start();
            
            logger.info("🏷️ Auto-tagging started in background thread");
            
            result.put("success", true);
            result.put("qas", savedQAs);
            result.put("cached", false);
            result.put("count", savedQAs.size());
            result.put("message", "Q&As generated successfully. Auto-tagging in progress...");
            
        } catch (NumberFormatException e) {
            logger.warning("❌ Invalid parameter format: " + e.getMessage());
            result.put("success", false);
            result.put("error", "Invalid parameter format");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Database error: " + e.getMessage(), e);
            result.put("success", false);
            result.put("error", "Database error: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error generating MCQs: " + e.getMessage(), e);
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
        result.put("error", "Use POST method to generate MCQs");
        
        response.getWriter().write(gson.toJson(result));
    }
}