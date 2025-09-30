package controller.AI;

import com.google.gson.Gson;
import service.RAGQueryService;
import service.RAGQueryService.RAGResponse;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RAGQueryServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(RAGQueryServlet.class.getName());
    private final Gson gson = new Gson();
    private final RAGQueryService ragService = new RAGQueryService();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        
        try {
            // Get parameters
            String question = request.getParameter("question");
            String bookIdParam = request.getParameter("bookId");
            String topKParam = request.getParameter("topK");
            
            // Validation
            if (question == null || question.trim().isEmpty()) {
                sendError(response, "Question parameter is required");
                return;
            }
            
            // Parse optional parameters
            Integer bookId = null;
            if (bookIdParam != null && !bookIdParam.isEmpty()) {
                try {
                    bookId = Integer.parseInt(bookIdParam);
                } catch (NumberFormatException e) {
                    sendError(response, "Invalid bookId format");
                    return;
                }
            }
            
            int topK = 3; // Default
            if (topKParam != null && !topKParam.isEmpty()) {
                try {
                    topK = Integer.parseInt(topKParam);
                    topK = Math.min(Math.max(topK, 1), 10); // Clamp between 1-10
                } catch (NumberFormatException e) {
                    // Use default
                }
            }
            
            logger.info("Processing RAG query: " + question);
            
            // Execute RAG query
            RAGResponse ragResponse = ragService.query(question, bookId, topK);
            
            // Build JSON response
            response.getWriter().write(gson.toJson(new ApiResponse(
                true,
                ragResponse.answer,
                ragResponse.relevantChapters,
                ragResponse.totalRetrieved
            )));
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing RAG query", e);
            sendError(response, "Internal error: " + e.getMessage());
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
    
    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.getWriter().write(gson.toJson(new ApiResponse(false, message, null, 0)));
    }
    
    private static class ApiResponse {
        boolean success;
        String answer;
        Object relevantChapters;
        int totalRetrieved;
        
        ApiResponse(boolean success, String answer, Object relevantChapters, int totalRetrieved) {
            this.success = success;
            this.answer = answer;
            this.relevantChapters = relevantChapters;
            this.totalRetrieved = totalRetrieved;
        }
    }
}