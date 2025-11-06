package controller.Chapter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.QADao;
import model.QA;
import service.TaggingService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaggingServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(TaggingServlet.class.getName());
    private final Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String action = request.getParameter("action");
        String qaIdParam = request.getParameter("qaId");
        String chapterIdParam = request.getParameter("chapterId");
        
        JsonObject jsonResponse = new JsonObject();
        
        try {
            TaggingService taggingService = new TaggingService();
            QADao qaDao = new QADao();
            
            // ========== ACTION: Get Statistics ==========
            if ("stats".equals(action)) {
                TaggingService.TaggingStats stats = taggingService.getTaggingStats();
                
                if (stats != null) {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("totalQAs", stats.totalQAs);
                    jsonResponse.addProperty("tagged", stats.tagged);
                    jsonResponse.addProperty("untagged", stats.untagged);
                    jsonResponse.addProperty("percentageTagged", stats.percentageTagged);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("error", "Failed to get statistics");
                }
                
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }
            
            // ========== ACTION: Tag All Untagged ==========
            if ("tagAll".equals(action)) {
                logger.info("🔄 Starting tag all untagged Q&As...");
                
                // Run in background thread to avoid timeout
                new Thread(() -> {
                    try {
                        taggingService.tagAllUntagged();
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error tagging all", e);
                    }
                }).start();
                
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Auto-tagging started in background. Check logs for progress.");
                
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }
            
            // ========== ACTION: Tag by Chapter ID ==========
            if (chapterIdParam != null) {
                int chapterId = Integer.parseInt(chapterIdParam);
                logger.info("🏷️ Tagging Q&As for chapter: " + chapterId);
                
                // Get all Q&As in chapter
                List<QA> qas = qaDao.getQAsByChapterId(chapterId);
                
                if (qas.isEmpty()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("error", "No Q&As found for chapter " + chapterId);
                    response.getWriter().write(gson.toJson(jsonResponse));
                    return;
                }
                
                // Filter untagged only
                int untaggedCount = 0;
                for (QA qa : qas) {
                    if (!qa.isAutoTagged()) {
                        untaggedCount++;
                    }
                }
                
                if (untaggedCount == 0) {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "All Q&As in this chapter are already tagged");
                    jsonResponse.addProperty("total", qas.size());
                    response.getWriter().write(gson.toJson(jsonResponse));
                    return;
                }
                
                // Run tagging in background
                final int finalUntaggedCount = untaggedCount;
                new Thread(() -> {
                    try {
                        logger.info("Starting background tagging for " + finalUntaggedCount + " Q&As");
                        for (QA qa : qas) {
                            if (!qa.isAutoTagged()) {
                                taggingService.tagQA(qa);
                                Thread.sleep(1000); // Rate limiting
                            }
                        }
                        logger.info("✅ Completed tagging for chapter " + chapterId);
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Error in background tagging", e);
                    }
                }).start();
                
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Tagging started for " + untaggedCount + " Q&As");
                jsonResponse.addProperty("total", qas.size());
                jsonResponse.addProperty("untagged", untaggedCount);
                
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }
            if (qaIdParam != null) {
                int qaId = Integer.parseInt(qaIdParam);
                logger.info("🏷️ Tagging single Q&A: " + qaId);
                
                QA qa = qaDao.getQAById(qaId);
                
                if (qa == null) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("error", "Q&A not found: " + qaId);
                    response.getWriter().write(gson.toJson(jsonResponse));
                    return;
                }

                if (qa.isAutoTagged()) {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Q&A already tagged");
                    jsonResponse.addProperty("bloomLevel", qa.getBloomLevel());
                    jsonResponse.addProperty("questionTypeTag", qa.getQuestionTypeTag());
                    response.getWriter().write(gson.toJson(jsonResponse));
                    return;
                }
                
                taggingService.tagQA(qa);              
                QA updatedQA = qaDao.getQAById(qaId);
                List<QADao.Tag> tags = qaDao.getTagsByQAId(qaId);
                
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Q&A tagged successfully");
                jsonResponse.addProperty("bloomLevel", updatedQA.getBloomLevel());
                jsonResponse.addProperty("questionTypeTag", updatedQA.getQuestionTypeTag());
                jsonResponse.addProperty("tagCount", tags.size());
                
                // Add tags array
                StringBuilder tagsStr = new StringBuilder();
                for (int i = 0; i < tags.size(); i++) {
                    if (i > 0) tagsStr.append(", ");
                    tagsStr.append(tags.get(i).getTagName());
                }
                jsonResponse.addProperty("tags", tagsStr.toString());
                
                response.getWriter().write(gson.toJson(jsonResponse));
                return;
            }
            
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("error", "Missing required parameter: qaId, chapterId, or action");
            response.getWriter().write(gson.toJson(jsonResponse));
            
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Invalid number format", e);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("error", "Invalid number format: " + e.getMessage());
            response.getWriter().write(gson.toJson(jsonResponse));
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in TaggingServlet", e);
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("error", "Server error: " + e.getMessage());
            response.getWriter().write(gson.toJson(jsonResponse));
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {

        String action = request.getParameter("action");
        
        if ("stats".equals(action)) {
            doPost(request, response);
        } else {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("error", "Use POST method for tagging operations");
            jsonResponse.addProperty("info", "GET only supports: ?action=stats");
            
            response.getWriter().write(gson.toJson(jsonResponse));
        }
    }
}