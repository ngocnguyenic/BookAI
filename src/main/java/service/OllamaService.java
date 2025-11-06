package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import config.ConfigAPIKey;
import dao.QADao;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

public class OllamaService {
    
    private static final Logger logger = Logger.getLogger(OllamaService.class.getName());
    
    private final String FLASK_API_URL;
    private final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    
    private final HttpClient httpClient;
    private final Gson gson;
    
    private static final int FLASK_TIMEOUT_SECONDS = 180;
    private static final int OLLAMA_TIMEOUT_SECONDS = 180;
    private static final int HEALTH_CHECK_TIMEOUT = 5;
    
    public OllamaService() {
        String configUrl = ConfigAPIKey.getProperty("ollama.flask.api.url");
        this.FLASK_API_URL = (configUrl != null) ? configUrl : "http://localhost:5001/chat";
        
        logger.info("Ollama Service initialized");
        logger.info("Flask API: " + this.FLASK_API_URL);
        logger.info("Ollama API: " + this.OLLAMA_API_URL);
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.gson = new Gson();
    }
    
    public boolean isServerHealthy() {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("message", "ping");
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FLASK_API_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(HEALTH_CHECK_TIMEOUT))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            boolean healthy = response.statusCode() == 200;
            if (healthy) {
                logger.fine("Flask API health check: OK");
            } else {
                logger.warning("Flask API health check failed (status: " + response.statusCode() + ")");
            }
            return healthy;
            
        } catch (Exception e) {
            logger.warning("Flask API not reachable: " + e.getMessage());
            return false;
        }
    }
    
    public boolean isOllamaHealthy() {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "llama3.2");
            requestBody.put("prompt", "ping");
            requestBody.put("stream", false);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_API_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(HEALTH_CHECK_TIMEOUT))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            logger.warning("Ollama API not reachable: " + e.getMessage());
            return false;
        }
    }
    
    private String sendFlaskMessage(String message) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("message", message);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(FLASK_API_URL))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(FLASK_TIMEOUT_SECONDS))
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Flask API request failed: " + response.statusCode());
        }
        
        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        
        if (jsonResponse.has("error")) {
            throw new IOException("Flask API error: " + jsonResponse.get("error").getAsString());
        }
        
        return jsonResponse.get("response").getAsString();
    }
    
    private String sendOllamaRequest(String requestBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_API_URL))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(OLLAMA_TIMEOUT_SECONDS))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Ollama API request failed: " + response.statusCode());
        }
        
        return response.body();
    }
    
    
    public String generateChapterSummary(String chapterTitle, String chapterContent) 
            throws IOException, InterruptedException {
        
        String contentSample = chapterContent.substring(0, 
            Math.min(3000, chapterContent.length()));
        
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
        
        logger.info("Generating summary for: " + chapterTitle);
        long startTime = System.currentTimeMillis();
        
        String summary = sendFlaskMessage(prompt);
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info(String.format("Summary generated in %.1fs (%d chars)", 
            duration / 1000.0, summary.length()));
        
        return summary.trim();
    }
    
    
    public List<QADao.QAItem> generateMCQ(String summary, int numQuestions) throws Exception {
        logger.info("Generating " + numQuestions + " MCQs...");
        
        String prompt = buildMCQPrompt(summary, numQuestions);
        
        JSONObject request = new JSONObject();
        request.put("model", "llama3.2");
        request.put("prompt", prompt);
        request.put("stream", false);
        request.put("temperature", 0.7);
        
        long startTime = System.currentTimeMillis();
        String jsonResponse = sendOllamaRequest(request.toString());
        
        JSONObject responseObj = new JSONObject(jsonResponse);
        String generatedText = responseObj.getString("response");
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info(String.format("MCQ response received in %.1fs", duration / 1000.0));
        
        String jsonStr = extractJSON(generatedText);
        List<QADao.QAItem> mcqs = parseMCQsFromJSON(jsonStr);
        
        logger.info("Successfully parsed " + mcqs.size() + " MCQs");
        logDifficultyDistribution(mcqs);
        
        return mcqs;
    }
    
    private String buildMCQPrompt(String summary, int numQuestions) {
        return "BỐI CẢNH: Bạn là giảng viên đại học, tạo câu hỏi trắc nghiệm cho sinh viên.\n\n" +
               
               "TÓM TẮT CHƯƠNG:\n" + summary + "\n\n" +
               
               "NHIỆM VỤ: Tạo " + numQuestions + " câu hỏi TRẮC NGHIỆM (4 đáp án).\n\n" +
               
               "YÊU CẦU:\n" +
               "1. Mỗi câu hỏi có 4 đáp án (A, B, C, D)\n" +
               "2. CHỈ 1 đáp án đúng\n" +
               "3. Các đáp án sai phải hợp lý, không quá rõ ràng\n" +
               "4. Đáp án đúng được lưu trong trường 'answer' dạng text đầy đủ\n" +
               "5. Phân bố độ khó: 30-40% easy, 40-50% medium, 20-30% hard\n\n" +
               
               "PHÂN BỐ ĐỘ KHÓ:\n" +
               "EASY: Ghi nhớ định nghĩa, khái niệm cơ bản\n" +
               "MEDIUM: Hiểu và vận dụng, phân biệt khái niệm\n" +
               "HARD: Phân tích, đánh giá, tổng hợp\n\n" +
               
               "FORMAT JSON:\n" +
               "{\n" +
               "  \"qas\": [\n" +
               "    {\n" +
               "      \"question\": \"Câu hỏi rõ ràng?\\nA. Đáp án A\\nB. Đáp án B đúng\\nC. Đáp án C\\nD. Đáp án D\",\n" +
               "      \"answer\": \"B. Đáp án B đúng. Giải thích: ...\",\n" +
               "      \"difficulty\": \"medium\"\n" +
               "    }\n" +
               "  ]\n" +
               "}\n\n" +
               
               "LƯU Ý:\n" +
               "- Tạo đúng " + numQuestions + " câu hỏi\n" +
               "- CHỈ trả về JSON (không có ```json hay ```)\n" +
               "- difficulty CHỈ là: \"easy\", \"medium\", hoặc \"hard\"\n" +
               "- question chứa câu hỏi + 4 đáp án A, B, C, D (dùng \\n để xuống dòng)\n" +
               "- answer chứa đáp án đúng (ví dụ: 'B. Nội dung') + giải thích ngắn\n" +
               "- Viết hoàn toàn bằng tiếng Việt\n" +
               "- Đảm bảo JSON hợp lệ";
    }
    
    private List<QADao.QAItem> parseMCQsFromJSON(String jsonStr) throws Exception {
    List<QADao.QAItem> result = new ArrayList<>();
    
    try {
        JSONObject obj = new JSONObject(jsonStr);
        JSONArray qasArray = obj.getJSONArray("qas");
        
        for (int i = 0; i < qasArray.length(); i++) {
            JSONObject qa = qasArray.getJSONObject(i);
            
            // Kiểm tra xem có đủ field không
            if (!qa.has("question")) {
                logger.warning("Question " + (i+1) + " missing 'question' field, skipping");
                continue;
            }
            
            if (!qa.has("answer")) {
                logger.warning("Question " + (i+1) + " missing 'answer' field, skipping");
                continue;
            }
            
            String question = qa.getString("question");
            String answer = qa.getString("answer");
            String difficulty = qa.optString("difficulty", "medium").toLowerCase();
            
            // Validate difficulty
            if (!difficulty.equals("easy") && !difficulty.equals("medium") && !difficulty.equals("hard")) {
                logger.warning("Invalid difficulty '" + difficulty + "', defaulting to medium");
                difficulty = "medium";
            }
            
            result.add(new QADao.QAItem(question, answer, difficulty, "mcq"));
        }
        
        if (result.isEmpty()) {
            throw new Exception("No valid MCQs parsed from response");
        }
        
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Failed to parse MCQs from JSON: " + e.getMessage());
        logger.severe("JSON string: " + jsonStr);
        throw new Exception("Invalid JSON response from Ollama: " + e.getMessage());
    }
    
    return result;
}
    
    public String generateShortFeedback(String prompt) throws Exception {
        logger.info("Generating short feedback...");
        
        JSONObject request = new JSONObject();
        request.put("model", "llama3.2");
        request.put("prompt", prompt);
        request.put("stream", false);
        request.put("temperature", 0.7);
        
        String jsonResponse = sendOllamaRequest(request.toString());
        JSONObject responseObj = new JSONObject(jsonResponse);
        String generatedText = responseObj.getString("response");
        
        return generatedText.trim();
    }
    

    public AnswerEvaluationResult evaluateUserAnswer(String question, String correctAnswer, 
                                                      String userAnswer, String difficulty) 
            throws Exception {
        
        logger.info("Evaluating user answer for difficulty: " + difficulty);
        
        String prompt = buildEvaluationPrompt(question, correctAnswer, userAnswer, difficulty);
        
        JSONObject request = new JSONObject();
        request.put("model", "llama3.2");
        request.put("prompt", prompt);
        request.put("stream", false);
        request.put("temperature", 0.3);
        
        String jsonResponse = sendOllamaRequest(request.toString());
        JSONObject responseObj = new JSONObject(jsonResponse);
        String generatedText = responseObj.getString("response");
        
        return parseEvaluationResult(generatedText);
    }
    
    private String buildEvaluationPrompt(String question, String correctAnswer, 
                                         String userAnswer, String difficulty) {
        return "BẠN LÀ GIẢNG VIÊN ĐẠI HỌC, đánh giá câu trả lời của sinh viên.\n\n" +
               
               "THÔNG TIN:\n" +
               "Độ khó câu hỏi: " + difficulty.toUpperCase() + "\n\n" +
               
               "Câu hỏi:\n" + question + "\n\n" +
               
               "Đáp án chuẩn:\n" + correctAnswer + "\n\n" +
               
               "Câu trả lời của sinh viên:\n" + userAnswer + "\n\n" +
               
               "NHIỆM VỤ ĐÁNH GIÁ:\n" +
               "Đánh giá câu trả lời theo 4 tiêu chí:\n\n" +
               
               "1. ĐỘ CHÍNH XÁC: Nội dung có đúng?\n" +
               "2. ĐỘ ĐẦY ĐỦ: Có đủ các ý chính?\n" +
               "3. ĐỘ SÂU: Hiểu bề mặt hay sâu sắc?\n" +
               "4. DIỄN ĐẠT: Rõ ràng, mạch lạc?\n\n" +
               
               "THANG ĐIỂM:\n" +
               "POOR (0-40): Sai hoàn toàn hoặc không liên quan\n" +
               "BASIC (41-60): Đúng một phần nhưng chưa đầy đủ\n" +
               "GOOD (61-85): Đúng và đầy đủ các ý chính\n" +
               "EXCELLENT (86-100): Hoàn hảo về nội dung và diễn đạt\n\n" +
               
               "YÊU CẦU OUTPUT:\n" +
               "Trả về JSON với format:\n" +
               "{\n" +
               "  \"score\": 75,\n" +
               "  \"level\": \"good\",\n" +
               "  \"isCorrect\": true,\n" +
               "  \"feedback\": \"Câu trả lời đúng và khá đầy đủ...\",\n" +
               "  \"strengths\": [\"Giải thích rõ khái niệm\", \"Có ví dụ\"],\n" +
               "  \"improvements\": [\"Nên bổ sung thêm...\", \"Có thể phân tích sâu hơn...\"]\n" +
               "}\n\n" +
               
               "CHÚ Ý:\n" +
               "- score: 0-100\n" +
               "- level: CHỈ được là \"poor\", \"basic\", \"good\", hoặc \"excellent\"\n" +
               "- isCorrect: true nếu >=60 điểm, false nếu <60\n" +
               "- feedback: Nhận xét tổng quan (2-3 câu)\n" +
               "- strengths: Mảng điểm mạnh (nếu có)\n" +
               "- improvements: Mảng gợi ý cải thiện (nếu có)\n" +
               "- CHỈ TRẢ VỀ JSON, KHÔNG CÓ TEXT KHÁC\n" +
               "- Feedback bằng tiếng Việt, thân thiện nhưng chuyên nghiệp";
    }
    
    private AnswerEvaluationResult parseEvaluationResult(String jsonText) throws Exception {
        String cleanJson = extractJSON(jsonText);
        JSONObject obj = new JSONObject(cleanJson);
        
        AnswerEvaluationResult result = new AnswerEvaluationResult();
        result.score = obj.getDouble("score");
        result.level = obj.getString("level").toLowerCase();
        result.isCorrect = obj.getBoolean("isCorrect");
        result.feedback = obj.getString("feedback");
        
        if (obj.has("strengths")) {
            JSONArray strengthsArr = obj.getJSONArray("strengths");
            result.strengths = new ArrayList<>();
            for (int i = 0; i < strengthsArr.length(); i++) {
                result.strengths.add(strengthsArr.getString(i));
            }
        }
        
        if (obj.has("improvements")) {
            JSONArray improvementsArr = obj.getJSONArray("improvements");
            result.improvements = new ArrayList<>();
            for (int i = 0; i < improvementsArr.length(); i++) {
                result.improvements.add(improvementsArr.getString(i));
            }
        }
        
        if (!result.level.equals("poor") && !result.level.equals("basic") && 
            !result.level.equals("good") && !result.level.equals("excellent")) {
            logger.warning("Invalid level '" + result.level + "', defaulting to basic");
            result.level = "basic";
        }
        
        logger.info("Evaluation completed: " + result.level + " (" + result.score + "/100)");
        return result;
    }
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
    
    logger.info("Generating study questions for: " + chapterTitle);
    String questions = sendFlaskMessage(prompt);
    logger.info("Study questions generated");
    
    return questions.trim();
}

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
    
    logger.info("Extracting key concepts from: " + chapterTitle);
    String concepts = sendFlaskMessage(prompt);
    logger.info("Key concepts extracted");
    
    return concepts.trim();
}
 
    
    public QATaggingResult autoTagQA(String question, String answer, String difficulty) 
            throws Exception {
        
        String prompt = buildTaggingPrompt(question, answer, difficulty);
        
        JSONObject request = new JSONObject();
        request.put("model", "llama3.2");
        request.put("prompt", prompt);
        request.put("stream", false);
        request.put("temperature", 0.3);
        
        String jsonResponse = sendOllamaRequest(request.toString());
        JSONObject responseObj = new JSONObject(jsonResponse);
        String generatedText = responseObj.getString("response");

        return parseTaggingResult(generatedText);
    }

    private String buildTaggingPrompt(String question, String answer, String difficulty) {
        return "Phân tích câu hỏi và câu trả lời sau, trả về thông tin dạng JSON:\n\n" +
               "Câu hỏi: " + question + "\n" +
               "Câu trả lời: " + answer + "\n" +
               "Độ khó: " + difficulty + "\n\n" +
               "Yêu cầu phân tích:\n" +
               "1. bloom_level: Xác định mức độ tư duy (remember/understand/apply/analyze/evaluate/create)\n" +
               "2. question_type: Loại câu hỏi (definition/comparison/explanation/application/analysis)\n" +
               "3. topics: Danh sách 3-5 chủ đề/khái niệm chính (mảng string)\n" +
               "4. concepts: Các khái niệm quan trọng được đề cập (mảng string)\n" +
               "5. confidence: Độ tin cậy của phân loại (0.0-1.0)\n\n" +
               "Trả về ĐÚNG format JSON:\n" +
               "{\n" +
               "  \"bloom_level\": \"understand\",\n" +
               "  \"question_type\": \"definition\",\n" +
               "  \"topics\": [\"OOP\", \"Java\", \"Interface\"],\n" +
               "  \"concepts\": [\"Abstraction\", \"Polymorphism\"],\n" +
               "  \"confidence\": 0.95\n" +
               "}\n\n" +
               "CHỈ TRẢ VỀ JSON, KHÔNG CÓ TEXT KHÁC.";
    }
    
    private QATaggingResult parseTaggingResult(String jsonText) throws Exception {
        String cleanJson = extractJSON(jsonText);
        JSONObject obj = new JSONObject(cleanJson);
        
        QATaggingResult result = new QATaggingResult();
        result.bloomLevel = obj.getString("bloom_level");
        result.questionType = obj.getString("question_type");
        result.confidence = obj.getDouble("confidence");

        JSONArray topicsArr = obj.getJSONArray("topics");
        result.topics = new ArrayList<>();
        for (int i = 0; i < topicsArr.length(); i++) {
            result.topics.add(topicsArr.getString(i));
        }

        JSONArray conceptsArr = obj.getJSONArray("concepts");
        result.concepts = new ArrayList<>();
        for (int i = 0; i < conceptsArr.length(); i++) {
            result.concepts.add(conceptsArr.getString(i));
        }
        
        logger.info("Auto-tagging completed: " + result);
        return result;
    }
    private String extractJSON(String text) {
        text = text.trim();
        
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        
        return text.trim();
    }
    
    private void logDifficultyDistribution(List<QADao.QAItem> qas) {
        Map<String, Integer> distribution = new HashMap<>();
        for (QADao.QAItem qa : qas) {
            distribution.merge(qa.getDifficulty(), 1, Integer::sum);
        }
        logger.info("Difficulty distribution: " + distribution);
    }
    
    // ============================================
    // LEGACY METHODS (Keep for backward compatibility)
    // ============================================
    
    public String answerQuestion(String question, String context) 
            throws IOException, InterruptedException {
        
        String contextSample = context.length() > 0 
            ? context.substring(0, Math.min(4000, context.length())) 
            : "";
        
        String prompt = contextSample.isEmpty() 
            ? question
            : String.format(
                "Dựa vào ngữ cảnh sau, hãy trả lời câu hỏi bằng tiếng Việt:\n\n" +
                "Ngữ cảnh:\n%s\n\n" +
                "Câu hỏi: %s\n\n" +
                "Trả lời chi tiết, dễ hiểu và chính xác.",
                contextSample,
                question
            );
        
        logger.info("Answering question...");
        String answer = sendFlaskMessage(prompt);
        logger.info("Answer generated");
        
        return answer.trim();
    }
    
    public void testConnection() {
        try {
            logger.info("========================================");
            logger.info("TESTING OLLAMA SERVICE");
            logger.info("========================================");

            logger.info("1. Testing Flask API...");
            if (!isServerHealthy()) {
                logger.severe("Flask API is not running!");
                logger.info("To start: cd your_flask_directory && python app.py");
            } else {
                logger.info("Flask API is running");
            }

            logger.info("\n2. Testing Ollama API...");
            if (!isOllamaHealthy()) {
                logger.severe("Ollama API is not running!");
                logger.info("To start: ollama serve && ollama pull llama3.2");
            } else {
                logger.info("Ollama API is running");
            }
            
            logger.info("\n========================================");
            logger.info("OllamaService is ready!");
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Test failed", e);
        }
    }
    
    public static class AnswerEvaluationResult {
        public double score;
        public String level;
        public boolean isCorrect;
        public String feedback;
        public List<String> strengths;
        public List<String> improvements;
        
        @Override
        public String toString() {
            return String.format("Score: %.1f/100, Level: %s, Correct: %s", 
                score, level, isCorrect);
        }
    }
    
    public static class QATaggingResult {
        public String bloomLevel;
        public String questionType;
        public List<String> topics;
        public List<String> concepts;
        public double confidence;
        
        @Override
        public String toString() {
            return String.format("Bloom:%s, Type:%s, Topics:%s (conf:%.2f)", 
                bloomLevel, questionType, topics, confidence);
        }
    }
}