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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FAISSService {
    
    private static final Logger logger = Logger.getLogger(FAISSService.class.getName());
    
    private final String serverUrl;
    private final HttpClient httpClient;
    private final Gson gson;
    
    public FAISSService() {
        String configUrl = ConfigAPIKey.getProperty("faiss.server.url");
        this.serverUrl = (configUrl != null) ? configUrl : "http://localhost:5000";
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
        
        logger.info("✅ FAISSService initialized: " + this.serverUrl);
    }
    
    public boolean isServerHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/health"))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject health = JsonParser.parseString(response.body()).getAsJsonObject();
                logger.info("FAISS server health: " + health);
                return true;
            }
            return false;
            
        } catch (Exception e) {
            logger.warning("FAISS server not reachable: " + e.getMessage());
            return false;
        }
    }
    
    public void createIndex(int dimension) throws IOException, InterruptedException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("dimension", dimension);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/create_index"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Failed to create FAISS index: " + response.body());
        }
        
        logger.info("✅ FAISS index created with dimension: " + dimension);
    }
    
    public void addVector(int id, float[] vector) 
            throws IOException, InterruptedException {
        List<Integer> ids = new ArrayList<>();
        ids.add(id);
        
        List<float[]> vectors = new ArrayList<>();
        vectors.add(vector);
        
        addVectors(ids, vectors);
    }
    
    public void addVectors(List<Integer> ids, List<float[]> vectors) 
            throws IOException, InterruptedException {
        
        if (ids.size() != vectors.size()) {
            throw new IllegalArgumentException("IDs and vectors size mismatch");
        }
        
        // Build request body
        JsonObject requestBody = new JsonObject();
        
        JsonArray idsArray = new JsonArray();
        for (Integer id : ids) {
            idsArray.add(id);
        }
        
        JsonArray vectorsArray = new JsonArray();
        for (float[] vector : vectors) {
            JsonArray vectorArray = new JsonArray();
            for (float v : vector) {
                vectorArray.add(v);
            }
            vectorsArray.add(vectorArray);
        }
        
        requestBody.add("ids", idsArray);
        requestBody.add("vectors", vectorsArray);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/add_vectors"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMinutes(2))
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Failed to add vectors: " + response.body());
        }
        
        logger.info("✅ Added " + ids.size() + " vectors to FAISS");
    }
    
    /**
     * Search similar vectors - Dùng embedding vector trực tiếp
     */
    public List<Integer> searchSimilarByVector(float[] queryVector, int topK) 
            throws IOException, InterruptedException {
        
        JsonObject requestBody = new JsonObject();
        
        JsonArray vectorArray = new JsonArray();
        for (float v : queryVector) {
            vectorArray.add(v);
        }
        
        requestBody.add("vector", vectorArray);
        requestBody.addProperty("k", topK);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/search"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("FAISS search failed: " + response.body());
        }
        
        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray results = jsonResponse.getAsJsonArray("results");
        
        List<Integer> chapterIds = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            JsonObject result = results.get(i).getAsJsonObject();
            chapterIds.add(result.get("chapter_id").getAsInt());
        }
        
        logger.info("✅ FAISS search returned " + chapterIds.size() + " results");
        return chapterIds;
    }
    
    /**
     * Save FAISS index to disk
     */
    public void saveIndex() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/save"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody())
            .timeout(Duration.ofSeconds(30))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Failed to save FAISS index: " + response.body());
        }
        
        logger.info("✅ FAISS index saved to disk");
    }
    
    /**
     * Get number of vectors in index
     */
    public int getVectorCount() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/count"))
            .GET()
            .timeout(Duration.ofSeconds(5))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Failed to get vector count: " + response.body());
        }
        
        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        
        if (jsonResponse.has("error")) {
            logger.warning("Vector count error: " + jsonResponse.get("error").getAsString());
            return 0;
        }
        
        int count = jsonResponse.get("count").getAsInt();
        logger.info("FAISS index has " + count + " vectors");
        return count;
    }
    
    /**
     * Clear all vectors from index
     */
    public void clearIndex() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serverUrl + "/clear"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        
        HttpResponse<String> response = httpClient.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Failed to clear FAISS index: " + response.body());
        }
        
        logger.info("✅ FAISS index cleared");
    }
}