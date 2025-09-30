package service;

import config.ConfigAPIKey;
import model.Chapter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import io.pinecone.clients.Pinecone;
import org.openapitools.db_control.client.model.*;

public class PineconeService {
    
    private static final Logger logger = Logger.getLogger(PineconeService.class.getName());
    
    private final String apiKey;
    private final String environment;
    private final String indexName;
    private final String indexUrl;
    
    public PineconeService() {
        this.apiKey = ConfigAPIKey.getProperty("pinecone.api.key");
        this.environment = ConfigAPIKey.getProperty("pinecone.environment");
        this.indexName = ConfigAPIKey.getProperty("pinecone.index.name");
        
        // Lấy host từ config (copy từ Pinecone console)
        String host = ConfigAPIKey.getProperty("pinecone.index.host");
        
        if (host != null && !host.isEmpty()) {
            this.indexUrl = "https://" + host;
        } else {
            // Fallback: try to build URL (may not work with all Pinecone versions)
            this.indexUrl = String.format("https://%s.svc.%s.pinecone.io", indexName, environment);
            logger.warning("⚠️ pinecone.index.host not set in config. Using fallback URL.");
        }
        
        logger.info("Pinecone Index URL: " + indexUrl);
    }
    
    /**
     * Upsert (insert/update) một chapter vector vào Pinecone
     */
    public boolean upsertChapter(Chapter chapter, float[] embedding) throws IOException {
        List<Chapter> chapters = new ArrayList<>();
        chapters.add(chapter);
        
        float[][] embeddings = new float[1][];
        embeddings[0] = embedding;
        
        return upsertChaptersBatch(chapters, embeddings);
    }
    
    /**
     * Upsert nhiều chapters cùng lúc (batch)
     */
    public boolean upsertChaptersBatch(List<Chapter> chapters, float[][] embeddings) throws IOException {
        if (chapters.size() != embeddings.length) {
            throw new IllegalArgumentException("Chapters and embeddings size mismatch");
        }
        
        String urlStr = indexUrl + "/vectors/upsert";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Api-Key", apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        // Build request body
        JSONObject request = new JSONObject();
        JSONArray vectors = new JSONArray();
        
        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            float[] embedding = embeddings[i];
            
            JSONObject vector = new JSONObject();
            vector.put("id", "chapter_" + chapter.getChapterID());
            
            // Convert float[] to JSONArray
            JSONArray values = new JSONArray();
            for (float v : embedding) {
                values.put(v);
            }
            vector.put("values", values);
            
            // Metadata
            JSONObject metadata = new JSONObject();
            metadata.put("bookId", chapter.getBookID());
            metadata.put("chapterNumber", chapter.getChapterNumber());
            metadata.put("title", chapter.getTitle());
            metadata.put("summary", chapter.getSummary() != null ? chapter.getSummary() : "");
            // Store only first 500 chars of content in metadata
            String contentPreview = chapter.getContent().substring(0, 
                Math.min(500, chapter.getContent().length()));
            metadata.put("contentPreview", contentPreview);
            
            vector.put("metadata", metadata);
            vectors.put(vector);
        }
        
        request.put("vectors", vectors);
        request.put("namespace", ""); // Default namespace
        
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.toString().getBytes(StandardCharsets.UTF_8));
        }
        
        int code = conn.getResponseCode();
        Scanner scanner = new Scanner(
            code == 200 ? conn.getInputStream() : conn.getErrorStream(),
            StandardCharsets.UTF_8.name()
        );
        
        StringBuilder response = new StringBuilder();
        while (scanner.hasNextLine()) {
            response.append(scanner.nextLine());
        }
        scanner.close();
        
        if (code != 200) {
            throw new IOException("Pinecone upsert error: " + code + " - " + response);
        }
        
        logger.info("✅ Upserted " + chapters.size() + " vectors to Pinecone");
        return true;
    }
    
    /**
     * Query Pinecone để tìm các chapters tương tự
     */
    public List<SearchResult> queryTopK(float[] queryEmbedding, int topK, Integer bookId) throws IOException {
        String urlStr = indexUrl + "/query";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Api-Key", apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        // Build request
        JSONObject request = new JSONObject();
        
        // Query vector
        JSONArray vector = new JSONArray();
        for (float v : queryEmbedding) {
            vector.put(v);
        }
        request.put("vector", vector);
        request.put("topK", topK);
        request.put("includeMetadata", true);
        request.put("namespace", "");
        
        // Filter by bookId if provided
        if (bookId != null) {
            JSONObject filter = new JSONObject();
            filter.put("bookId", bookId);
            request.put("filter", filter);
        }
        
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.toString().getBytes(StandardCharsets.UTF_8));
        }
        
        int code = conn.getResponseCode();
        Scanner scanner = new Scanner(
            code == 200 ? conn.getInputStream() : conn.getErrorStream(),
            StandardCharsets.UTF_8.name()
        );
        
        StringBuilder response = new StringBuilder();
        while (scanner.hasNextLine()) {
            response.append(scanner.nextLine());
        }
        scanner.close();
        
        if (code != 200) {
            throw new IOException("Pinecone query error: " + code + " - " + response);
        }
        
        return parseQueryResults(response.toString());
    }
    
    /**
     * Parse query results từ Pinecone response
     */
    private List<SearchResult> parseQueryResults(String jsonResponse) {
        List<SearchResult> results = new ArrayList<>();
        
        JSONObject root = new JSONObject(jsonResponse);
        JSONArray matches = root.getJSONArray("matches");
        
        for (int i = 0; i < matches.length(); i++) {
            JSONObject match = matches.getJSONObject(i);
            
            SearchResult result = new SearchResult();
            result.id = match.getString("id");
            result.score = (float) match.getDouble("score");
            
            if (match.has("metadata")) {
                JSONObject metadata = match.getJSONObject("metadata");
                result.bookId = metadata.optInt("bookId", -1);
                result.chapterNumber = metadata.optInt("chapterNumber", -1);
                result.title = metadata.optString("title", "");
                result.summary = metadata.optString("summary", "");
                result.contentPreview = metadata.optString("contentPreview", "");
            }
            
            results.add(result);
        }
        
        logger.info("Found " + results.size() + " matching results");
        return results;
    }
    
    /**
     * Xóa vectors của một book khỏi Pinecone
     */
    public boolean deleteBookVectors(int bookId) throws IOException {
        String urlStr = indexUrl + "/vectors/delete";
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Api-Key", apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        JSONObject request = new JSONObject();
        JSONObject filter = new JSONObject();
        filter.put("bookId", bookId);
        request.put("filter", filter);
        request.put("namespace", "");
        request.put("deleteAll", false);
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(request.toString().getBytes(StandardCharsets.UTF_8));
        }
        
        int code = conn.getResponseCode();
        logger.info("Deleted vectors for bookId=" + bookId + ", response code: " + code);
        
        return code == 200;
    }
    
    /**
     * Inner class để store search results
     */
    public static class SearchResult {
        public String id;
        public float score;
        public int bookId;
        public int chapterNumber;
        public String title; 
        public String summary;
        public String contentPreview;
        
        @Override
        public String toString() {
            return String.format("SearchResult{id=%s, score=%.4f, chapter=%d, title=%s}",
                id, score, chapterNumber, title);
        }
    }
}