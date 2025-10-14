package controller.AI;

import com.google.gson.Gson;
import model.Chapter;
import service.RAGQueryService;
import service.OllamaService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RAGChatServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(RAGChatServlet.class.getName());
    private final Gson gson = new Gson();
    private final RAGQueryService ragService = new RAGQueryService();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json;charset=UTF-8");
        
        String question = request.getParameter("question");
        
        if (question == null || question.trim().isEmpty()) {
            sendError(response, "Vui lòng nhập câu hỏi");
            return;
        }
        
        try {
            logger.info("Received RAG query: " + question);
            
            RAGQueryService.RAGResponse result = ragService.query(question, 3);
            
            ApiResponse apiResponse = new ApiResponse(
                result.answer,
                convertToSourceInfo(result.sources)
            );
            
            logger.info("RAG response generated successfully");
            response.getWriter().write(gson.toJson(apiResponse));
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "RAG query failed", e);
            sendError(response, "Lỗi: " + e.getMessage());
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
    
    private List<SourceInfo> convertToSourceInfo(List<Chapter> chapters) {
        List<SourceInfo> sources = new ArrayList<>();
        if (chapters != null) {
            for (Chapter ch : chapters) {
                sources.add(new SourceInfo(
                    ch.getChapterID(),
                    ch.getChapterNumber(),
                    ch.getTitle()
                ));
            }
        }
        return sources;
    }
    
    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.getWriter().write(gson.toJson(new ErrorResponse(message)));
    }
    
    static class ApiResponse {
        String answer;
        List<SourceInfo> sources;
        
        ApiResponse(String answer, List<SourceInfo> sources) {
            this.answer = answer;
            this.sources = sources;
        }
    }
    
    static class SourceInfo {
        int chapterId;
        int chapterNumber;
        String title;
        
        SourceInfo(int id, int num, String title) {
            this.chapterId = id;
            this.chapterNumber = num;
            this.title = title;
        }
    }
    
    static class ErrorResponse {
        String error;
        ErrorResponse(String error) { this.error = error; }
    }
}