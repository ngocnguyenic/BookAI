package service;

import config.ConfigAPIKey;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;

public class EmbeddingService {
    
    private static final Logger logger = Logger.getLogger(EmbeddingService.class.getName());
    private static final String DEFAULT_EMBEDDING_URL = "https://generativelanguage.googleapis.com/v1beta/models/embedding-001:embedContent";
    
    private final String embeddingUrl;
    private final String apiKey;
    private final ConcurrentHashMap<String, float[]> embeddingCache;
    private static final int MAX_CACHE_SIZE = 10000;
    
    public EmbeddingService() {
        String configKey = ConfigAPIKey.getProperty("gemini.api.key");
        
        if (configKey == null || configKey.trim().isEmpty()) {
            throw new RuntimeException("CRITICAL: gemini.api.key is not configured! Check your config.properties file.");
        }
        this.apiKey = configKey.trim();
        this.embeddingUrl = DEFAULT_EMBEDDING_URL;
        this.embeddingCache = new ConcurrentHashMap<>();
        
        logger.info("✅ EmbeddingService initialized successfully");
        logger.info("   Embedding URL: " + embeddingUrl);
        logger.info("   Model: embedding-001 (768 dimensions)");
        logger.info("   Cache enabled: Max " + MAX_CACHE_SIZE + " entries");
    }
    
    public float[] generateEmbedding(String text) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }

        String textHash = calculateHash(text);
        if (embeddingCache.containsKey(textHash)) {
            logger.fine("✅ Cache hit for text hash: " + textHash.substring(0, 8));
            return embeddingCache.get(textHash);
        }
        
        logger.fine("❌ Cache miss, calling Gemini API...");
        float[] embedding = generateEmbeddingFromAPI(text);
        
        // Store in cache
        if (embeddingCache.size() < MAX_CACHE_SIZE) {
            embeddingCache.put(textHash, embedding);
            logger.fine("📥 Cached embedding (cache size: " + embeddingCache.size() + ")");
        } else {
            logger.warning("⚠️ Cache full (" + MAX_CACHE_SIZE + "), not caching this embedding");
        }
        
        return embedding;
    }
    
    private float[] generateEmbeddingFromAPI(String text) throws IOException {
        // Truncate if too long
        if (text.length() > 8000) {
            text = text.substring(0, 8000);
            logger.warning("⚠️ Text truncated to 8000 characters");
        }
        
        String urlStr = embeddingUrl + "?key=" + apiKey;
        URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        
        JSONObject content = new JSONObject();
        content.put("model", "models/embedding-001");
        
        JSONObject part = new JSONObject();
        part.put("text", text);
        
        JSONArray parts = new JSONArray();
        parts.put(part);
        
        JSONObject contentObj = new JSONObject();
        contentObj.put("parts", parts);
        
        content.put("content", contentObj);
        
        logger.fine("📤 Sending embedding request (text length: " + text.length() + ")");
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(content.toString().getBytes(StandardCharsets.UTF_8));
        }
        
        int code = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        Scanner scanner;
        
        if (code == 200) {
            scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name());
        } else {
            scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8.name());
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
            scanner.close();
            throw new IOException("Embedding API error " + code + ": " + response);
        }
        
        while (scanner.hasNextLine()) {
            response.append(scanner.nextLine());
        }
        scanner.close();
        
        return parseEmbeddingResponse(response.toString());
    }
    
    private float[] parseEmbeddingResponse(String jsonResponse) {
        JSONObject root = new JSONObject(jsonResponse);
        
        if (!root.has("embedding")) {
            throw new RuntimeException("Response doesn't contain embedding field. Response: " + jsonResponse);
        }
        
        JSONObject embedding = root.getJSONObject("embedding");
        JSONArray values = embedding.getJSONArray("values");
        
        float[] vector = new float[values.length()];
        for (int i = 0; i < values.length(); i++) {
            vector[i] = (float) values.getDouble(i);
        }
        
        logger.info("✅ Generated embedding with dimension: " + vector.length);
        return vector;
    }
    
    public float[] generateQAEmbedding(String question, String answer) throws IOException {
        String combinedText = String.format(
            "Câu hỏi: %s\nCâu trả lời: %s",
            question.trim(),
            answer.trim()
        );
        
        logger.fine("🔄 Generating Q&A embedding (Q:" + question.length() + " chars, A:" + answer.length() + " chars)");
        return generateEmbedding(combinedText);
    }
    
    public float[] generateQuestionEmbedding(String question) throws IOException {
        String formatted = "Câu hỏi: " + question.trim();
        return generateEmbedding(formatted);
    }

    public float[] generateChapterEmbedding(String chapterTitle, String summary) throws IOException {
        String combined = String.format(
            "Chương: %s\nTóm tắt: %s",
            chapterTitle.trim(),
            summary.trim()
        );
        return generateEmbedding(combined);
    }
    
    public float[][] generateEmbeddingsBatch(String[] texts) throws IOException {
        float[][] embeddings = new float[texts.length][];
        
        for (int i = 0; i < texts.length; i++) {
            logger.info("📝 Generating embedding " + (i + 1) + "/" + texts.length);
            embeddings[i] = generateEmbedding(texts[i]);
            
            if (i < texts.length - 1) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        logger.info("✅ Generated " + texts.length + " embeddings successfully");
        return embeddings;
    }
    
    private String calculateHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }
    
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    

    public boolean isServiceHealthy() {
        try {
            float[] testVector = generateEmbedding("test");
            boolean healthy = (testVector != null && testVector.length == 768);
            
            if (healthy) {
                logger.info("✅ Embedding service is healthy (768 dims)");
            } else {
                logger.warning("❌ Embedding service returned unexpected dimension: " + testVector.length);
            }
            return healthy;
            
        } catch (Exception e) {
            logger.severe("❌ Embedding service health check failed: " + e.getMessage());
            return false;
        }
    }
    

    public String getCacheStats() {
        return String.format("Cache: %d/%d entries (%.1f%% full)", 
            embeddingCache.size(), 
            MAX_CACHE_SIZE,
            (embeddingCache.size() * 100.0 / MAX_CACHE_SIZE));
    }
    

    public void clearCache() {
        embeddingCache.clear();
        logger.info("🗑️ Embedding cache cleared");
    }
}
