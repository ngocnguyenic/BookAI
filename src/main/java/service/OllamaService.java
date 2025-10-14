package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import config.ConfigAPIKey;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;


public class OllamaService {
    
    private static final Logger logger = Logger.getLogger(OllamaService.class.getName());
    
    // Flask API endpoint - Load từ config
    private final String FLASK_API_URL;
    
    private final HttpClient httpClient;
    private final Gson gson;
    
    public OllamaService() {
        // Load Flask API URL từ config.properties sử dụng ConfigAPIKey
        String configUrl = ConfigAPIKey.getProperty("ollama.flask.api.url");
        this.FLASK_API_URL = (configUrl != null) ? configUrl : "http://localhost:5001/chat";
        
        logger.info("✅ Ollama Flask API URL loaded from config: " + this.FLASK_API_URL);
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.gson = new Gson();
    }
    
    /**
     * Kiểm tra Flask server có đang chạy không
     */
    public boolean isServerHealthy() {
        try {
            // Test với một message đơn giản
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("message", "ping");
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FLASK_API_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            boolean healthy = response.statusCode() == 200;
            logger.info("Flask API health: " + (healthy ? "✅ OK" : "❌ FAILED"));
            return healthy;
            
        } catch (Exception e) {
            logger.warning("Flask API not reachable: " + e.getMessage());
            logger.warning("Make sure Flask server is running: python app.py");
            return false;
        }
    }
    
    /**
     * Gửi message tới Flask API và nhận response
     */
    private String sendMessage(String message) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("message", message);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(FLASK_API_URL))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMinutes(3)) // Timeout dài hơn cho summarize
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        logger.info("Sending request to Flask API...");
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Flask API request failed: " + response.statusCode() + 
                " - " + response.body());
        }
        
        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        
        if (jsonResponse.has("error")) {
            throw new IOException("Flask API error: " + jsonResponse.get("error").getAsString());
        }
        
        return jsonResponse.get("response").getAsString();
    }
    
    /**
     * Generate chapter summary - METHOD CHÍNH
     */
    public String generateChapterSummary(String chapterTitle, String chapterContent) 
            throws IOException, InterruptedException {
        
        // Truncate content nếu quá dài (tránh quá tải)
        String contentSample = chapterContent.substring(0, 
            Math.min(3000, chapterContent.length()));
        
        // Tạo prompt cho summarization
        String prompt = String.format(
            "Hãy tóm tắt chương sau bằng tiếng Việt (khoảng 100-150 từ):\n\n" +
            "Tiêu đề: %s\n\n" +
            "Nội dung:\n%s\n\n" +
            "Yêu cầu:\n" +
            "- Tóm tắt súc tích, dễ hiểu\n" +
            "- Nêu các ý chính và khái niệm quan trọng\n" +
            "- Viết bằng tiếng Việt, không quá 150 từ",
            chapterTitle,
            contentSample
        );
        
        logger.info("Generating summary for chapter: " + chapterTitle);
        long startTime = System.currentTimeMillis();
        
        String summary = sendMessage(prompt);
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info(String.format("✅ Summary generated in %.1f seconds (%d chars)", 
            duration / 1000.0, summary.length()));
        
        return summary.trim();
    }
    
    /**
     * Answer questions về chapter content - CHO RAG
     */
    public String answerQuestion(String question, String context) 
            throws IOException, InterruptedException {
        
        String contextSample = context.substring(0, Math.min(4000, context.length()));
        
        String prompt = String.format(
            "Dựa vào ngữ cảnh sau, hãy trả lời câu hỏi bằng tiếng Việt:\n\n" +
            "Ngữ cảnh:\n%s\n\n" +
            "Câu hỏi: %s\n\n" +
            "Trả lời chi tiết, dễ hiểu và chính xác dựa trên ngữ cảnh đã cho.",
            contextSample,
            question
        );
        
        logger.info("Answering question with context length: " + context.length());
        String answer = sendMessage(prompt);
        logger.info("✅ Answer generated");
        
        return answer.trim();
    }
    
    /**
     * Extract key concepts từ chapter - BONUS
     */
    public String extractKeyConcepts(String chapterTitle, String chapterContent) 
            throws IOException, InterruptedException {
        
        String contentSample = chapterContent.substring(0, 
            Math.min(3000, chapterContent.length()));
        
        String prompt = String.format(
            "Từ chương sách sau, hãy liệt kê 5-8 khái niệm/thuật ngữ quan trọng nhất " +
            "kèm giải thích ngắn gọn bằng tiếng Việt:\n\n" +
            "Tiêu đề: %s\n\n" +
            "Nội dung:\n%s\n\n" +
            "Format: Liệt kê dạng bullet points",
            chapterTitle,
            contentSample
        );
        
        return sendMessage(prompt);
    }
    
    /**
     * Explain concept - Giải thích khái niệm cụ thể
     */
    public String explainConcept(String concept, String context) 
            throws IOException, InterruptedException {
        
        String prompt = String.format(
            "Hãy giải thích khái niệm '%s' một cách dễ hiểu bằng tiếng Việt.\n\n" +
            "Ngữ cảnh:\n%s\n\n" +
            "Giải thích chi tiết, có ví dụ minh họa nếu được.",
            concept,
            context.substring(0, Math.min(2000, context.length()))
        );
        
        return sendMessage(prompt);
    }
    
    /**
     * Generate study questions từ chapter
     */
    public String generateStudyQuestions(String chapterTitle, String chapterContent) 
            throws IOException, InterruptedException {
        
        String contentSample = chapterContent.substring(0, 
            Math.min(3000, chapterContent.length()));
        
        String prompt = String.format(
            "Dựa vào nội dung chương sau, hãy tạo 5 câu hỏi ôn tập (có đáp án) " +
            "bằng tiếng Việt:\n\n" +
            "Chương: %s\n\n" +
            "Nội dung:\n%s\n\n" +
            "Format:\n" +
            "1. Câu hỏi 1?\n" +
            "   Đáp án: ...\n" +
            "2. Câu hỏi 2?\n" +
            "   Đáp án: ...",
            chapterTitle,
            contentSample
        );
        
        return sendMessage(prompt);
    }
    
    /**
     * Compare concepts - So sánh các khái niệm
     */
    public String compareConcepts(String concept1, String concept2, String context) 
            throws IOException, InterruptedException {
        
        String prompt = String.format(
            "Hãy so sánh '%s' và '%s' bằng tiếng Việt:\n\n" +
            "Ngữ cảnh:\n%s\n\n" +
            "So sánh về:\n" +
            "- Định nghĩa\n" +
            "- Điểm giống\n" +
            "- Điểm khác\n" +
            "- Ứng dụng",
            concept1,
            concept2,
            context.substring(0, Math.min(3000, context.length()))
        );
        
        return sendMessage(prompt);
    }
    
    /**
     * Test connection
     */
    public void testConnection() {
        try {
            logger.info("Testing Flask API connection...");
            
            if (!isServerHealthy()) {
                logger.severe("❌ Flask API is not running!");
                logger.info("To start Flask server:");
                logger.info("  1. cd to your Flask project directory");
                logger.info("  2. python app.py");
                logger.info("  3. Server should start on http://localhost:5001");
                return;
            }
            
            logger.info("✅ Flask API is running");
            
            // Test với một summary ngắn
            String testSummary = generateChapterSummary(
                "Test Chapter",
                "This is a test content to verify the summarization feature."
            );
            
            logger.info("✅ Test summary received: " + 
                testSummary.substring(0, Math.min(100, testSummary.length())) + "...");
            logger.info("✅ OllamaService is ready to use!");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Flask API test failed", e);
            logger.info("Troubleshooting:");
            logger.info("1. Check if Flask server is running: http://localhost:5001");
            logger.info("2. Check if Ollama is running: ollama serve");
            logger.info("3. Check if model is available: ollama pull gemma3:4b");
        }
    }
    
    
}