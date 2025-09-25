// service/ChapterAIDetector.java
package service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.Chapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChapterAIDetector {

    private final GeminiService geminiService;

    public ChapterAIDetector() {
        this.geminiService = new GeminiService();
    }

    public List<Chapter> detectChapters(String fullBookText) throws IOException {
        try {
     
            String aiResponse = geminiService.detectChaptersWithAI(fullBookText);

          
            JsonObject jsonResponse = JsonParser.parseString(aiResponse).getAsJsonObject();

            
            String cleanText = extractCleanTextFromGeminiResponse(jsonResponse);

            if (cleanText == null || cleanText.trim().isEmpty() || "{}".equals(cleanText.trim())) {
                throw new RuntimeException("AI trả về nội dung rỗng.");
            }

    
            JsonObject resultJson = JsonParser.parseString(cleanText).getAsJsonObject();
            JsonArray chaptersArray = resultJson.getAsJsonArray("chapters");

            List<Chapter> chapters = new ArrayList<>();
            if (chaptersArray != null) {
                for (int i = 0; i < chaptersArray.size(); i++) {
                    JsonObject chapterJson = chaptersArray.get(i).getAsJsonObject();
                    Chapter chapter = new Chapter();
                    chapter.setChapterNumber(chapterJson.get("chapterNumber").getAsInt());
                    chapter.setTitle(chapterJson.get("title").getAsString());
                    chapter.setContent(chapterJson.get("content").getAsString());
                    chapters.add(chapter);
                }
            }

            if (chapters.isEmpty()) {
                throw new RuntimeException("AI không phát hiện được chương nào.");
            }

            return chapters;

        } catch (Exception e) {
            System.err.println("❌ Lỗi khi dùng AI detect chương: " + e.getMessage());
            e.printStackTrace();
           
            return new ChapterDetector().detectChapters(fullBookText);
        }
    }

    private String extractCleanTextFromGeminiResponse(JsonObject jsonResponse) {
        try {
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                JsonArray parts = firstCandidate.getAsJsonObject("content").getAsJsonArray("parts");
                if (parts != null && !parts.isEmpty()) {
                    return parts.get(0).getAsJsonObject().get("text").getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";
    }
}