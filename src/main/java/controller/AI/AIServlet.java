package controller.AI;

import service.OllamaService; 
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AIServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(AIServlet.class.getName());
    private final OllamaService ollamaService = new OllamaService(); // THAY ĐỔI
    private final Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        
        String action = request.getParameter("action");
        String chapterTitle = request.getParameter("title");
        String chapterContent = request.getParameter("content");
        
        // Validation
        if (action == null || action.trim().isEmpty()) {
            sendErrorResponse(response, "Missing 'action' parameter. Use 'summary' or 'qa'");
            return;
        }
        
        if (chapterTitle == null || chapterTitle.trim().isEmpty()) {
            sendErrorResponse(response, "Missing 'title' parameter");
            return;
        }
        
        if (chapterContent == null || chapterContent.trim().isEmpty()) {
            sendErrorResponse(response, "Missing 'content' parameter");
            return;
        }
        
        try {
            logger.info("AI Action: " + action + " for chapter: " + chapterTitle);
            
            String result;
            
            if ("summary".equalsIgnoreCase(action)) {

                result = ollamaService.generateChapterSummary(chapterTitle, chapterContent);
                
            } else if ("qa".equalsIgnoreCase(action)) {

                result = ollamaService.generateStudyQuestions(chapterTitle, chapterContent);
                
            } else if ("concepts".equalsIgnoreCase(action)) {

                result = ollamaService.extractKeyConcepts(chapterTitle, chapterContent);
                
            } else {
                sendErrorResponse(response, "Unknown action '" + action + "'. Use 'summary', 'qa', or 'concepts'");
                return;
            }
            
            JsonObject jsonResponse = new JsonObject();
            jsonResponse.addProperty("action", action);
            jsonResponse.addProperty("title", chapterTitle);
            jsonResponse.addProperty("result", result);
            jsonResponse.addProperty("success", true);
            
            response.getWriter().write(gson.toJson(jsonResponse));
            logger.info("AI response generated successfully");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Request interrupted", e);
            sendErrorResponse(response, "Request interrupted: " + e.getMessage());
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO error during AI processing", e);
            sendErrorResponse(response, "Error communicating with AI service: " + e.getMessage());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error in AI servlet", e);
            sendErrorResponse(response, "Unexpected error: " + e.getMessage());
        }
    }
    
    private void sendErrorResponse(HttpServletResponse response, String errorMessage) 
            throws IOException {
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("success", false);
        errorJson.addProperty("error", errorMessage);
        response.getWriter().write(gson.toJson(errorJson));
    }
}