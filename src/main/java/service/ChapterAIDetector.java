package service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.Chapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ChapterAIDetector {
    
    private static final Logger logger = Logger.getLogger(ChapterAIDetector.class.getName());
    private final GeminiService geminiService;
    private final Gson gson;
    
    // Configuration
    private static final int MAX_TEXT_LENGTH = 30000; // Giới hạn text gửi cho Gemini
    private static final int MIN_CHAPTER_LENGTH = 500; // Độ dài tối thiểu của chapter
    private static final int MAX_CHAPTER_LENGTH = 10000; // Độ dài tối đa của chapter
    
    public ChapterAIDetector() {
        this.geminiService = new GeminiService();
        this.gson = new Gson();
    }
    
    /**
     * Main method: Phát hiện và chia chapters từ full text
     */
    public List<Chapter> detectChapters(String fullBookText) {
        if (fullBookText == null || fullBookText.trim().isEmpty()) {
            logger.warning("Full book text is empty!");
            return new ArrayList<>();
        }
        
        try {
            logger.info("Starting chapter detection for text length: " + fullBookText.length());
            
            // Step 1: Phân tích cấu trúc sách
            BookStructure structure = analyzeBookStructure(fullBookText);
            logger.info("Book structure detected: " + structure);
            
            // Step 2: Thử pattern-based detection trước
            List<Chapter> chapters = tryPatternBasedDetection(fullBookText, structure);
            
            // Step 3: Nếu không tìm thấy pattern rõ ràng, dùng AI semantic analysis
            if (chapters.isEmpty() || chapters.size() < 2) {
                logger.info("Pattern detection failed, using AI semantic analysis...");
                chapters = aiSemanticDetection(fullBookText);
            }
            
            // Step 4: Validate và refine chapters
            chapters = validateAndRefineChapters(chapters, fullBookText);
            
            // Step 5: Generate summaries cho từng chapter
            chapters = generateChapterSummaries(chapters);
            
            logger.info("Chapter detection completed. Total chapters: " + chapters.size());
            return chapters;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in chapter detection", e);
            // Fallback: Chia đều nếu AI fail
            return fallbackSimpleDivision(fullBookText);
        }
    }
    
    /**
     * Step 1: Phân tích cấu trúc tổng quan của sách
     */
    private BookStructure analyzeBookStructure(String text) {
        BookStructure structure = new BookStructure();
        
        // Sample đầu tiên 2000 chars để phân tích
        String sample = text.substring(0, Math.min(2000, text.length()));
        
        // Detect language
        structure.language = detectLanguage(sample);
        
        // Detect common patterns
        structure.hasNumberedChapters = sample.matches("(?i).*(CHƯƠNG|Chapter|PHẦN|Bài)\\s*\\d+.*");
        
        // Estimate chapter count based on length
        structure.estimatedChapters = Math.max(3, text.length() / 3000);
        
        // Detect writing style
        if (sample.contains("public class") || sample.contains("function") || sample.contains("def ")) {
            structure.documentType = "technical";
        } else if (sample.matches(".*\\d+\\.\\d+.*")) {
            structure.documentType = "textbook";
        } else {
            structure.documentType = "general";
        }
        
        return structure;
    }
    
    /**
     * Step 2: Pattern-based detection (nhanh, chính xác nếu có pattern rõ)
     */
    private List<Chapter> tryPatternBasedDetection(String text, BookStructure structure) {
        List<Chapter> chapters = new ArrayList<>();
        
        // Các patterns thường gặp
        String[] patterns = {
            "(?i)(CHƯƠNG|Chương)\\s+(\\d+|[IVXLC]+)\\s*[:：]?\\s*([^\n\r]{0,100})",
            "(?i)(Chapter|CHAPTER)\\s+(\\d+|[IVXLC]+)\\s*[:：]?\\s*([^\n\r]{0,100})",
            "(?i)(PHẦN|Phần|BÀI|Bài)\\s+(\\d+|[IVXLC]+)\\s*[:：]?\\s*([^\n\r]{0,100})",
            "(?m)^(\\d+)\\s*\\.\\s+([A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼỀỀỂỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪỬỮỰỲỴỶỸ][^\n]{0,80})",
        };
        
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
            Matcher m = p.matcher(text);
            
            List<ChapterBoundary> boundaries = new ArrayList<>();
            while (m.find()) {
                ChapterBoundary boundary = new ChapterBoundary();
                boundary.startIndex = m.start();
                boundary.chapterNumber = extractChapterNumber(m.group(2));
                boundary.title = m.group(3) != null ? m.group(3).trim() : "Chương " + boundary.chapterNumber;
                boundaries.add(boundary);
            }
            
            // Nếu tìm thấy ít nhất 2 chapters, dùng pattern này
            if (boundaries.size() >= 2) {
                logger.info("Found " + boundaries.size() + " chapters using pattern: " + pattern);
                chapters = extractChaptersFromBoundaries(text, boundaries);
                break;
            }
        }
        
        return chapters;
    }
    
    /**
     * Step 3: AI Semantic Detection (dùng khi không có pattern rõ ràng)
     */
    private List<Chapter> aiSemanticDetection(String fullText) throws IOException {
        // Truncate text nếu quá dài
        String textToAnalyze = fullText.length() > MAX_TEXT_LENGTH 
            ? fullText.substring(0, MAX_TEXT_LENGTH) 
            : fullText;
        
        // Call Gemini API với prompt đã có sẵn trong GeminiService
        String jsonResponse = geminiService.detectChaptersWithAI(textToAnalyze);
        
        logger.info("AI Response: " + jsonResponse.substring(0, Math.min(500, jsonResponse.length())));
        
        return parseAIResponse(jsonResponse, fullText);
    }
    
    /**
     * Parse JSON response từ Gemini API
     */
    private List<Chapter> parseAIResponse(String jsonResponse, String originalText) {
        List<Chapter> chapters = new ArrayList<>();
        
        try {
            // Clean response (remove markdown code blocks if any)
            jsonResponse = jsonResponse.trim();
            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.substring(7);
            }
            if (jsonResponse.startsWith("```")) {
                jsonResponse = jsonResponse.substring(3);
            }
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            }
            jsonResponse = jsonResponse.trim();
            
            JsonObject root = JsonParser.parseString(jsonResponse).getAsJsonObject();
            
            if (!root.has("chapters")) {
                logger.warning("Response doesn't have 'chapters' field");
                return chapters;
            }
            
            JsonArray chaptersArray = root.getAsJsonArray("chapters");
            
            for (int i = 0; i < chaptersArray.size(); i++) {
                JsonObject chapterObj = chaptersArray.get(i).getAsJsonObject();
                
                Chapter chapter = new Chapter();
                chapter.setChapterNumber(chapterObj.get("chapterNumber").getAsInt());
                chapter.setTitle(chapterObj.get("title").getAsString());
                
                // Content từ AI hoặc extract từ original text
                if (chapterObj.has("content")) {
                    String content = chapterObj.get("content").getAsString();
                    // Validate content length
                    if (content.length() > MIN_CHAPTER_LENGTH) {
                        chapter.setContent(content);
                    } else {
                        // Nếu content quá ngắn, cố gắng extract nhiều hơn từ original text
                        chapter.setContent(extractContentByTitle(originalText, chapter.getTitle()));
                    }
                } else {
                    // Fallback: extract content by title
                    chapter.setContent(extractContentByTitle(originalText, chapter.getTitle()));
                }
                
                chapters.add(chapter);
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing AI response", e);
        }
        
        return chapters;
    }
    
    /**
     * Extract chapters từ boundaries đã tìm được
     */
    private List<Chapter> extractChaptersFromBoundaries(String text, List<ChapterBoundary> boundaries) {
        List<Chapter> chapters = new ArrayList<>();
        
        for (int i = 0; i < boundaries.size(); i++) {
            ChapterBoundary current = boundaries.get(i);
            ChapterBoundary next = (i + 1 < boundaries.size()) ? boundaries.get(i + 1) : null;
            
            int startIdx = current.startIndex;
            int endIdx = next != null ? next.startIndex : text.length();
            
            String content = text.substring(startIdx, endIdx).trim();
            
            // Skip nếu content quá ngắn
            if (content.length() < MIN_CHAPTER_LENGTH) {
                continue;
            }
            
            // Truncate nếu quá dài
            if (content.length() > MAX_CHAPTER_LENGTH) {
                content = content.substring(0, MAX_CHAPTER_LENGTH) + "...";
            }
            
            Chapter chapter = new Chapter();
            chapter.setChapterNumber(current.chapterNumber);
            chapter.setTitle(current.title);
            chapter.setContent(content);
            
            chapters.add(chapter);
        }
        
        return chapters;
    }
    
    /**
     * Step 4: Validate và refine chapters
     */
    private List<Chapter> validateAndRefineChapters(List<Chapter> chapters, String fullText) {
        List<Chapter> refined = new ArrayList<>();
        
        for (Chapter chapter : chapters) {
            // Skip chapters quá ngắn
            if (chapter.getContent() == null || chapter.getContent().length() < MIN_CHAPTER_LENGTH) {
                logger.warning("Skipping chapter " + chapter.getChapterNumber() + " - too short");
                continue;
            }
            
            // Clean title
            String title = chapter.getTitle();
            if (title == null || title.isEmpty()) {
                title = "Chương " + chapter.getChapterNumber();
            }
            title = cleanTitle(title);
            chapter.setTitle(title);
            
            // Clean content
            String content = cleanContent(chapter.getContent());
            chapter.setContent(content);
            
            refined.add(chapter);
        }
        
        // Re-number chapters
        for (int i = 0; i < refined.size(); i++) {
            refined.get(i).setChapterNumber(i + 1);
        }
        
        return refined;
    }
    
 
private List<Chapter> generateChapterSummaries(List<Chapter> chapters) {
    for (Chapter chapter : chapters) {
        try {
         
            String contentSample = chapter.getContent().substring(0, 
                Math.min(3000, chapter.getContent().length()));
            
            String summary = geminiService.generateChapterSummary(
                chapter.getTitle(), 
                contentSample
            );
            
            chapter.setSummary(summary);
            
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to generate summary for chapter " 
                + chapter.getChapterNumber(), e);
            chapter.setSummary("Chương " + chapter.getChapterNumber() + ": " + chapter.getTitle());
        }
    }
    
    return chapters;
}
    
    /**
     * Fallback: Chia đều nếu tất cả methods khác fail
     */
    private List<Chapter> fallbackSimpleDivision(String text) {
        logger.warning("Using fallback simple division");
        
        List<Chapter> chapters = new ArrayList<>();
        int targetChapters = Math.max(3, text.length() / 3000);
        int chapterLength = text.length() / targetChapters;
        
        for (int i = 0; i < targetChapters; i++) {
            int start = i * chapterLength;
            int end = Math.min(start + chapterLength, text.length());
            
            Chapter chapter = new Chapter();
            chapter.setChapterNumber(i + 1);
            chapter.setTitle("Phần " + (i + 1));
            chapter.setContent(text.substring(start, end));
            chapter.setSummary("Phần " + (i + 1) + " của tài liệu");
            
            chapters.add(chapter);
        }
        
        return chapters;
    }
    
    // ==================== UTILITY METHODS ====================
    
    private String detectLanguage(String sample) {
        if (sample.matches(".*[ăâđêôơưĂÂĐÊÔƠƯ].*")) {
            return "vietnamese";
        } else {
            return "english";
        }
    }
    
    private int extractChapterNumber(String str) {
        try {
            // Try parse as integer
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            // Try parse Roman numerals
            return romanToInt(str.trim());
        }
    }
    
    private int romanToInt(String s) {
        int result = 0;
        int prevValue = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            int value = switch (s.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                case 'L' -> 50;
                case 'C' -> 100;
                default -> 0;
            };
            if (value < prevValue) {
                result -= value;
            } else {
                result += value;
            }
            prevValue = value;
        }
        return result;
    }
    
    private String cleanTitle(String title) {
        // Remove extra whitespace
        title = title.replaceAll("\\s+", " ").trim();
        
        // Remove leading numbers/patterns
        title = title.replaceAll("^(CHƯƠNG|Chapter|PHẦN|Bài)\\s*\\d+\\s*[:：]?\\s*", "");
        
        // Limit length
        if (title.length() > 100) {
            title = title.substring(0, 100) + "...";
        }
        
        return title;
    }
    
    private String cleanContent(String content) {
        // Remove excessive whitespace
        content = content.replaceAll("\\s+", " ");
        
        // Remove page numbers patterns
        content = content.replaceAll("(?m)^\\s*\\d+\\s*$", "");
        
        return content.trim();
    }
    
    private String extractContentByTitle(String fullText, String title) {
        // Tìm vị trí của title trong text
        int startIdx = fullText.indexOf(title);
        if (startIdx == -1) {
            return "";
        }
        
        // Extract khoảng 2000 chars từ vị trí đó
        int endIdx = Math.min(startIdx + 2000, fullText.length());
        return fullText.substring(startIdx, endIdx);
    }
    
    // ==================== INNER CLASSES ====================
    
    private static class BookStructure {
        String language = "vietnamese";
        String documentType = "general";
        boolean hasNumberedChapters = false;
        int estimatedChapters = 5;
        
        @Override
        public String toString() {
            return String.format("BookStructure{lang=%s, type=%s, numbered=%s, est=%d}",
                language, documentType, hasNumberedChapters, estimatedChapters);
        }
    }
    
    private static class ChapterBoundary {
        int startIndex;
        int chapterNumber;
        String title;
    }
}