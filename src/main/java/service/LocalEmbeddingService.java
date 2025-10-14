package service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import config.ConfigAPIKey;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;


public class LocalEmbeddingService {
    
    private static final Logger logger = Logger.getLogger(LocalEmbeddingService.class.getName());
    
    private final String FLASK_EMBED_URL;
    private final HttpClient httpClient;
    private final Gson gson;
    
    public LocalEmbeddingService() {
        // Load Flask URL từ config
        String baseUrl = ConfigAPIKey.getProperty("ollama.flask.api.url");
        if (baseUrl != null && baseUrl.endsWith("/chat")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 5);
        } else if (baseUrl == null) {
            baseUrl = "http://localhost:5001";
        }
        this.FLASK_EMBED_URL = baseUrl + "/embed";
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        this.gson = new Gson();
        
        logger.info("✅ LocalEmbeddingService initialized");
        logger.info("   Endpoint: " + this.FLASK_EMBED_URL);
    }
    
    /**
     * Generate embedding vector cho text
     * @param text Text cần embed
     * @return float[] embedding vector (384 dimensions)
     */
    public float[] generateEmbedding(String text) throws IOException, InterruptedException {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }
        
        // Truncate nếu quá dài
        if (text.length() > 5000) {
            text = text.substring(0, 5000);
            logger.warning("Text truncated to 5000 characters");
        }
        
        // Build request
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("text", text);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(FLASK_EMBED_URL))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMinutes(1))
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        logger.fine("Sending embedding request for text length: " + text.length());
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Flask embedding API error " + response.statusCode() + 
                ": " + response.body());
        }
        
        return parseEmbeddingResponse(response.body());
    }
    
    /**
     * Parse response và extract embedding vector
     */
    private float[] parseEmbeddingResponse(String jsonResponse) {
        JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
        
        if (root.has("error")) {
            throw new RuntimeException("Embedding error: " + root.get("error").getAsString());
        }
        
        if (!root.has("embedding")) {
            throw new RuntimeException("Response doesn't contain embedding field");
        }
        
        JsonArray embeddingArray = root.getAsJsonArray("embedding");
        float[] vector = new float[embeddingArray.size()];
        
        for (int i = 0; i < embeddingArray.size(); i++) {
            vector[i] = embeddingArray.get(i).getAsFloat();
        }
        
        logger.info("✅ Generated embedding with dimension: " + vector.length);
        return vector;
    }
    
    /**
     * Generate embeddings cho nhiều texts (batch)
     */
    public float[][] generateEmbeddingsBatch(String[] texts) 
            throws IOException, InterruptedException {
        
        float[][] embeddings = new float[texts.length][];
        
        for (int i = 0; i < texts.length; i++) {
            logger.info("Generating embedding " + (i + 1) + "/" + texts.length);
            embeddings[i] = generateEmbedding(texts[i]);
            
            // Nhẹ nhàng delay để tránh quá tải (tùy chọn)
            if (i < texts.length - 1) {
                Thread.sleep(100);
            }
        }
        
        logger.info("✅ Generated " + texts.length + " embeddings successfully");
        return embeddings;
    }
    
    /**
     * Test connection
     */
    public boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FLASK_EMBED_URL.replace("/embed", "/health")))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            logger.warning("Flask embedding service not reachable: " + e.getMessage());
            return false;
        }
    }
}