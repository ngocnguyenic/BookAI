package controller.AI;

import service.GeminiService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AIServlet extends HttpServlet {

    private final GeminiService geminiService = new GeminiService();

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

        try {
            String rawResult;
            if ("summary".equalsIgnoreCase(action)) {
                rawResult = geminiService.generateChapterSummary(chapterTitle, chapterContent);
            } else if ("qa".equalsIgnoreCase(action)) {
                rawResult = geminiService.generateQAPairs(chapterTitle, chapterContent);
            } else {
                rawResult = "{\"error\":\"Unknown action. Use 'summary' or 'qa'\"}";
            }

            
            String cleanText = rawResult;
            try {
                JsonObject json = JsonParser.parseString(rawResult).getAsJsonObject();
                JsonArray candidates = json.getAsJsonArray("candidates");
                if (candidates != null && candidates.size() > 0) {
                    JsonObject first = candidates.get(0).getAsJsonObject();
                    JsonArray parts = first.getAsJsonObject("content").getAsJsonArray("parts");
                    if (parts != null && parts.size() > 0) {
                        cleanText = parts.get(0).getAsJsonObject().get("text").getAsString();
                    }
                }
            } catch (JsonSyntaxException parseEx) {
                
                cleanText = rawResult;
            }

            response.getWriter().write(cleanText);

        } catch (IOException e) {
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
