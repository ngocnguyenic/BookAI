package service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.Chapter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
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
    
    private static final int MAX_TEXT_LENGTH = 30000;
    private static final int MIN_CHAPTER_LENGTH = 500;
    private static final int MAX_CHAPTER_LENGTH = 10000;
    
    public ChapterAIDetector() {
        this.geminiService = new GeminiService();
        this.gson = new Gson();
    }
    
    /**
     * PHƯƠNG THỨC CHÍNH - Nhận thêm pdfPath
     */
    public List<Chapter> detectChapters(String fullBookText, String pdfPath) {
        if (fullBookText == null || fullBookText.trim().isEmpty()) {
            logger.warning("Full book text is empty!");
            return new ArrayList<>();
        }
        
        try {
            logger.info("Starting chapter detection for text length: " + fullBookText.length());
            
            // ========== METHOD 1: TOC EXTRACTION (CHÍNH XÁC NHẤT) ==========
            if (pdfPath != null && !pdfPath.isEmpty()) {
                logger.info("Method 1: Attempting TOC extraction from PDF...");
                List<Chapter> tocChapters = extractChaptersFromTOC(pdfPath);
                
                if (tocChapters.size() >= 3) {
                    logger.info("✅ Successfully extracted " + tocChapters.size() + " chapters from TOC");
                    return validateAndRefineChapters(tocChapters, fullBookText);
                }
            }
            
            // ========== METHOD 2: PATTERN DETECTION ==========
            logger.info("Method 2: Trying pattern-based detection...");
            BookStructure structure = analyzeBookStructure(fullBookText);
            List<Chapter> chapters = tryPatternBasedDetection(fullBookText, structure);
            
            if (chapters.size() >= 2) {
                logger.info("✅ Pattern detection found " + chapters.size() + " chapters");
                return validateAndRefineChapters(chapters, fullBookText);
            }
            
            // ========== METHOD 3: AI SEMANTIC ==========
            logger.info("Method 3: Using AI semantic analysis...");
            chapters = aiSemanticDetection(fullBookText);
            
            if (!chapters.isEmpty()) {
                return validateAndRefineChapters(chapters, fullBookText);
            }
            
            // ========== FALLBACK ==========
            logger.warning("All methods failed, using fallback division");
            return fallbackSimpleDivision(fullBookText);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in chapter detection", e);
            return fallbackSimpleDivision(fullBookText);
        }
    }
    
    /**
     * Backward compatibility - method cũ không có pdfPath
     */
    public List<Chapter> detectChapters(String fullBookText) {
        return detectChapters(fullBookText, null);
    }
    
    /**
     * ========== METHOD 1: TOC EXTRACTION ==========
     * Extract chapters từ Table of Contents trong PDF
     */
    private List<Chapter> extractChaptersFromTOC(String pdfPath) {
        List<Chapter> chapters = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            
            // Step 1: Extract TOC text từ 15 trang đầu
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(15, document.getNumberOfPages()));
            String tocText = stripper.getText(document);
            
            // Step 2: Parse TOC entries
            List<TOCEntry> tocEntries = parseTOC(tocText);
            
            if (tocEntries.isEmpty()) {
                logger.info("No TOC entries found");
                return chapters;
            }
            
            // Step 3: Validate entries
            tocEntries = validateTOCEntries(tocEntries, document.getNumberOfPages());
            
            if (tocEntries.size() < 3) {
                logger.info("Too few valid TOC entries: " + tocEntries.size());
                return chapters;
            }
            
            // Step 4: Extract content cho từng chapter
            for (TOCEntry entry : tocEntries) {
                stripper.setStartPage(entry.startPage);
                stripper.setEndPage(entry.endPage);
                
                String content = stripper.getText(document).trim();
                
                if (content.length() < MIN_CHAPTER_LENGTH) {
                    logger.warning("Chapter " + entry.chapterNumber + " too short, skipping");
                    continue;
                }
                
                Chapter chapter = new Chapter();
                chapter.setChapterNumber(entry.chapterNumber);
                chapter.setTitle(entry.title);
                chapter.setContent(content);
                
                chapters.add(chapter);
                
                logger.info("Extracted: Chapter " + entry.chapterNumber + ": " + entry.title + 
                           " (pages " + entry.startPage + "-" + entry.endPage + ")");
            }
            
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to extract TOC from PDF", e);
        }
        
        return chapters;
    }
    
    /**
     * Parse TOC text thành structured entries
     */
    private List<TOCEntry> parseTOC(String tocText) {
        List<TOCEntry> entries = new ArrayList<>();
        
        // Patterns cho TOC (theo thứ tự ưu tiên)
        Pattern[] patterns = {
            // "Chapter 1: Java Primer .................. 27"
            Pattern.compile("(?m)^\\s*Chapter\\s+(\\d+)\\s*[:：]?\\s*([^\\.\n]{3,80}?)[\\s\\.]{3,}(\\d{1,4})\\s*$", Pattern.CASE_INSENSITIVE),
            
            // "Chapter 1  Java Primer  27" (spaces only)
            Pattern.compile("(?m)^\\s*Chapter\\s+(\\d+)\\s+([A-Z][^\\d\n]{3,60}?)\\s{2,}(\\d{1,4})\\s*$", Pattern.CASE_INSENSITIVE),
            
            // "1. Java Primer .................. 27"
            Pattern.compile("(?m)^\\s*(\\d+)\\s*\\.\\s+([A-Z][^\\.\n]{3,80}?)[\\s\\.]{3,}(\\d{1,4})\\s*$"),
            
            // Vietnamese: "Chương 1: ..."
            Pattern.compile("(?m)^\\s*Chương\\s+(\\d+)\\s*[:：]?\\s*([^\\.\n]{3,80}?)[\\s\\.]{3,}(\\d{1,4})\\s*$", Pattern.CASE_INSENSITIVE),
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(tocText);
            
            while (matcher.find()) {
                try {
                    int chapterNum = Integer.parseInt(matcher.group(1).trim());
                    String title = matcher.group(2).trim()
                        .replaceAll("\\s+", " ")
                        .replaceAll("[\\.\\_\\-]+$", "")
                        .trim();
                    int pageNum = Integer.parseInt(matcher.group(3).trim());
                    
                    // Validate basic sanity
                    if (chapterNum > 0 && chapterNum < 100 && 
                        pageNum > 0 && pageNum < 10000 && 
                        title.length() >= 3) {
                        
                        TOCEntry entry = new TOCEntry();
                        entry.chapterNumber = chapterNum;
                        entry.title = title;
                        entry.startPage = pageNum;
                        entries.add(entry);
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
            
            // Nếu tìm đủ entries, stop
            if (entries.size() >= 3) {
                logger.info("Found " + entries.size() + " TOC entries using pattern: " + pattern.pattern());
                break;
            } else {
                entries.clear(); // Reset để thử pattern tiếp theo
            }
        }
        
        return entries;
    }
    
    /**
     * Validate và set endPage cho TOC entries
     */
    private List<TOCEntry> validateTOCEntries(List<TOCEntry> entries, int totalPages) {
        List<TOCEntry> valid = new ArrayList<>();
        
        for (int i = 0; i < entries.size(); i++) {
            TOCEntry entry = entries.get(i);
            
            // Validate page number
            if (entry.startPage < 1 || entry.startPage > totalPages) {
                continue;
            }
            
            // Set endPage
            if (i + 1 < entries.size()) {
                entry.endPage = entries.get(i + 1).startPage - 1;
            } else {
                entry.endPage = totalPages;
            }
            
            // Skip nếu chapter quá ngắn (< 2 pages) hoặc quá dài (> 200 pages)
            int pageCount = entry.endPage - entry.startPage + 1;
            if (pageCount < 2 || pageCount > 200) {
                logger.warning("Chapter " + entry.chapterNumber + " has " + pageCount + " pages - suspicious");
                continue;
            }
            
            valid.add(entry);
        }
        
        return valid;
    }
    
    // ========== CÁC METHODS CŨ GIỮ NGUYÊN ==========
    
    private BookStructure analyzeBookStructure(String text) {
        // ... code cũ giữ nguyên ...
        BookStructure structure = new BookStructure();
        String sample = text.substring(0, Math.min(2000, text.length()));
        structure.language = detectLanguage(sample);
        structure.hasNumberedChapters = sample.matches("(?i).*(CHƯƠNG|Chapter|PHẦN|Bài)\\s*\\d+.*");
        structure.estimatedChapters = Math.max(3, text.length() / 3000);
        if (sample.contains("public class") || sample.contains("function") || sample.contains("def ")) {
            structure.documentType = "technical";
        } else if (sample.matches(".*\\d+\\.\\d+.*")) {
            structure.documentType = "textbook";
        } else {
            structure.documentType = "general";
        }
        return structure;
    }
    
    private List<Chapter> tryPatternBasedDetection(String text, BookStructure structure) {
        // ... code cũ giữ nguyên ...
        List<Chapter> chapters = new ArrayList<>();
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
            if (boundaries.size() >= 2) {
                logger.info("Found " + boundaries.size() + " chapters using pattern: " + pattern);
                chapters = extractChaptersFromBoundaries(text, boundaries);
                break;
            }
        }
        return chapters;
    }
    
    private List<Chapter> aiSemanticDetection(String fullText) throws IOException, InterruptedException {
        // ... code cũ giữ nguyên ...
        String textToAnalyze = fullText.length() > MAX_TEXT_LENGTH 
            ? fullText.substring(0, MAX_TEXT_LENGTH) 
            : fullText;
        String jsonResponse = geminiService.detectChaptersWithAI(textToAnalyze);
        logger.info("AI Response: " + jsonResponse.substring(0, Math.min(500, jsonResponse.length())));
        return parseAIResponse(jsonResponse, fullText);
    }
    
    private List<Chapter> parseAIResponse(String jsonResponse, String originalText) {
        // ... code cũ giữ nguyên ...
        List<Chapter> chapters = new ArrayList<>();
        try {
            jsonResponse = jsonResponse.trim();
            if (jsonResponse.startsWith("```json")) jsonResponse = jsonResponse.substring(7);
            if (jsonResponse.startsWith("```")) jsonResponse = jsonResponse.substring(3);
            if (jsonResponse.endsWith("```")) jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
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
                if (chapterObj.has("content")) {
                    String content = chapterObj.get("content").getAsString();
                    if (content.length() > MIN_CHAPTER_LENGTH) {
                        chapter.setContent(content);
                    } else {
                        chapter.setContent(extractContentByTitle(originalText, chapter.getTitle()));
                    }
                } else {
                    chapter.setContent(extractContentByTitle(originalText, chapter.getTitle()));
                }
                chapters.add(chapter);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error parsing AI response", e);
        }
        return chapters;
    }
    
    private List<Chapter> extractChaptersFromBoundaries(String text, List<ChapterBoundary> boundaries) {
        // ... code cũ giữ nguyên ...
        List<Chapter> chapters = new ArrayList<>();
        for (int i = 0; i < boundaries.size(); i++) {
            ChapterBoundary current = boundaries.get(i);
            ChapterBoundary next = (i + 1 < boundaries.size()) ? boundaries.get(i + 1) : null;
            int startIdx = current.startIndex;
            int endIdx = next != null ? next.startIndex : text.length();
            String content = text.substring(startIdx, endIdx).trim();
            if (content.length() < MIN_CHAPTER_LENGTH) continue;
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
    
    private List<Chapter> validateAndRefineChapters(List<Chapter> chapters, String fullText) {
        // ... code cũ giữ nguyên ...
        List<Chapter> refined = new ArrayList<>();
        for (Chapter chapter : chapters) {
            if (chapter.getContent() == null || chapter.getContent().length() < MIN_CHAPTER_LENGTH) {
                logger.warning("Skipping chapter " + chapter.getChapterNumber() + " - too short");
                continue;
            }
            String title = chapter.getTitle();
            if (title == null || title.isEmpty()) {
                title = "Chương " + chapter.getChapterNumber();
            }
            title = cleanTitle(title);
            chapter.setTitle(title);
            String content = cleanContent(chapter.getContent());
            chapter.setContent(content);
            refined.add(chapter);
        }
        for (int i = 0; i < refined.size(); i++) {
            refined.get(i).setChapterNumber(i + 1);
        }
        return refined;
    }
    
    private List<Chapter> fallbackSimpleDivision(String text) {
        // ... code cũ giữ nguyên ...
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
    
    // Utility methods giữ nguyên
    private String detectLanguage(String sample) {
        return sample.matches(".*[ăâđêôơưĂÂĐÊÔƠƯ].*") ? "vietnamese" : "english";
    }
    
    private int extractChapterNumber(String str) {
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return romanToInt(str.trim());
        }
    }
    
    private int romanToInt(String s) {
        int result = 0, prevValue = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            int value = switch (s.charAt(i)) {
                case 'I' -> 1; case 'V' -> 5; case 'X' -> 10;
                case 'L' -> 50; case 'C' -> 100; default -> 0;
            };
            result += (value < prevValue) ? -value : value;
            prevValue = value;
        }
        return result;
    }
    
    private String cleanTitle(String title) {
        title = title.replaceAll("\\s+", " ").trim();
        title = title.replaceAll("^(CHƯƠNG|Chapter|PHẦN|Bài)\\s*\\d+\\s*[:：]?\\s*", "");
        if (title.length() > 100) title = title.substring(0, 100) + "...";
        return title;
    }
    
    private String cleanContent(String content) {
        content = content.replaceAll("\\s+", " ");
        content = content.replaceAll("(?m)^\\s*\\d+\\s*$", "");
        return content.trim();
    }
    
    private String extractContentByTitle(String fullText, String title) {
        int startIdx = fullText.indexOf(title);
        if (startIdx == -1) return "";
        int endIdx = Math.min(startIdx + 2000, fullText.length());
        return fullText.substring(startIdx, endIdx);
    }
    
    // Inner classes
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
    
    private static class TOCEntry {
        int chapterNumber;
        String title;
        int startPage;
        int endPage;
    }
}