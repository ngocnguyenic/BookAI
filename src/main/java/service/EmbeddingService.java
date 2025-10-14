package service;

import config.ConfigAPIKey;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;


public class EmbeddingService {
    
    private static final Logger logger = Logger.getLogger(EmbeddingService.class.getName());
    
    // Default embedding URL (khác với generateContent URL)
    private static final String DEFAULT_EMBEDDING_URL = "https://generativelanguage.googleapis.com/v1beta/models/embedding-001:embedContent";
    
    private final String embeddingUrl;
    private final String apiKey;
    
    public EmbeddingService() {
        // Đọc API key từ config (dùng chung với Gemini)
        String configKey = ConfigAPIKey.getProperty("gemini.api.key");
        
        if (configKey == null || configKey.trim().isEmpty()) {
            throw new RuntimeException("CRITICAL: gemini.api.key is not configured! Check your config.properties file.");
        }
        this.apiKey = configKey.trim();
        
        // Dùng URL riêng cho embedding (không dùng gemini.base.url)
        this.embeddingUrl = DEFAULT_EMBEDDING_URL;
        
        logger.info(" EmbeddingService initialized successfully");
        logger.info(" Embedding URL: " + embeddingUrl);
        logger.info(" API Key: " + apiKey.substring(0, 10) + "...");
    }
    
    /**
     * Generate embedding vector cho một đoạn text
     * @param text Text cần embed
     * @return float[] vector với dimension 768
     */
    public float[] generateEmbedding(String text) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }
        
        // Truncate text nếu quá dài (Gemini limit ~2048 tokens)
        if (text.length() > 8000) {
            text = text.substring(0, 8000);
            logger.warning("Text truncated to 8000 characters for embedding");
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
        
        logger.fine("Sending embedding request for text length: " + text.length());
        
        // Send request
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
    
    /**
     * Parse response và extract embedding vector
     */
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
        
        logger.info(" Generated embedding with dimension: " + vector.length);
        return vector;
    }

    public float[][] generateEmbeddingsBatch(String[] texts) throws IOException {
        float[][] embeddings = new float[texts.length][];
        
        for (int i = 0; i < texts.length; i++) {
            logger.info("Generating embedding " + (i + 1) + "/" + texts.length);
            embeddings[i] = generateEmbedding(texts[i]);
            
            // Rate limiting: wait 200ms between requests
            if (i < texts.length - 1) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        logger.info("✓ Generated " + texts.length + " embeddings successfully");
        return embeddings;
    }
}