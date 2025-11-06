package service;

import model.Chapter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public class ChapterDetector {
    
    private static final Logger logger = Logger.getLogger(ChapterDetector.class.getName());
    
    // Multiple patterns với độ ưu tiên giảm dần
    private static final String[] CHAPTER_PATTERNS = {
        // Pattern 1: "Chapter X" followed by title on SAME line
        // Example: "Chapter 1 Java Primer" or "Chapter 1: Java Primer"
        "(?m)^\\s*(Chapter|CHAPTER)\\s+(\\d+|[IVX]+)\\s*[:\\-–—]?\\s+([A-Z][A-Za-z\\s]{5,80})\\s*$",
        
        // Pattern 2: Vietnamese "Chương X: Title"
        "(?m)^\\s*(Chương|CHƯƠNG)\\s+(\\d+|[IVX]+)\\s*[:\\-–—]?\\s+([A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼỀỀỂỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪỬỮỰỲỴỶỸ][\\w\\s]{5,80})\\s*$",
        
        // Pattern 3: "Section X: Title"
        "(?m)^\\s*(Section|SECTION|Bài|BÀI)\\s+(\\d+|[IVX]+)\\s*[:\\-–—]?\\s+([A-Z][A-Za-z\\s]{5,80})\\s*$",
        
        // Pattern 4: Numbered with clear title (more strict)
        // Must start with capital, at least 2 words, and reasonable length
        "(?m)^\\s*(\\d+)\\s*[\\.\\)]\\s+([A-Z][A-Za-z]+(?:\\s+[A-Z][A-Za-z]+){1,8})\\s*$",
        
        // Pattern 5: All caps title (for books with all-caps chapter headings)
        "(?m)^\\s*([A-Z\\s]{10,60})\\s*$"
    };
    
    public List<Chapter> detectChapters(String fullText) {
        logger.info("Detecting chapters from text length: " + fullText.length());
        
        List<Chapter> chapters = null;
        
        // Thử từng pattern cho đến khi tìm được chapters
        for (int i = 0; i < CHAPTER_PATTERNS.length; i++) {
            logger.info("Trying pattern " + (i + 1) + "/" + CHAPTER_PATTERNS.length);
            chapters = tryPattern(fullText, CHAPTER_PATTERNS[i]);
            
            if (chapters != null && chapters.size() >= 2) {
                logger.info("✅ Found " + chapters.size() + " chapters with pattern " + (i + 1));
                return chapters;
            }
        }
        
        // Fallback: Coi cả sách là 1 chương
        logger.warning("No chapters found, treating entire text as one chapter");
        Chapter chap = new Chapter();
        chap.setChapterNumber(1);
        chap.setTitle("Toàn bộ nội dung");
        chap.setContent(fullText.trim());
        
        chapters = new ArrayList<>();
        chapters.add(chap);
        return chapters;
    }
    
    private List<Chapter> tryPattern(String fullText, String patternString) {
        List<Chapter> chapters = new ArrayList<>();
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(fullText);
        
        List<MatchResult> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.toMatchResult());
        }
        
        if (matches.isEmpty()) {
            return chapters;
        }
        
        for (int i = 0; i < matches.size(); i++) {
            MatchResult match = matches.get(i);
            int start = match.start();
            int end = (i == matches.size() - 1) ? fullText.length() : matches.get(i + 1).start();
            
            String content = fullText.substring(start, end).trim();
            
            // Skip chapters quá ngắn (< 1000 chars for better quality)
            if (content.length() < 1000) {
                logger.warning("Skipping chapter " + (i + 1) + " - too short (" + content.length() + " chars)");
                continue;
            }
            
            // Extract title
            String title = extractTitle(match);
            
            // Validate title quality
            if (!isValidTitle(title)) {
                logger.warning("Skipping chapter " + (i + 1) + " - invalid title: '" + title + "'");
                continue;
            }
            
            Chapter chap = new Chapter();
            chap.setChapterNumber(chapters.size() + 1);
            chap.setTitle(cleanTitle(title));
            chap.setContent(content);
            
            chapters.add(chap);
            
            logger.info("  Chapter " + chap.getChapterNumber() + ": " + chap.getTitle() + 
                       " (" + content.length() + " chars)");
        }
        
        return chapters;
    }
    
    /**
     * Extract title from match, handling different pattern structures
     */
    private String extractTitle(MatchResult match) {
        String title = match.group().trim();
        
        // If we have captured groups, try to get the title part
        if (match.groupCount() >= 2) {
            // Last group is usually the title
            String lastGroup = match.group(match.groupCount());
            if (lastGroup != null && !lastGroup.trim().isEmpty()) {
                return lastGroup.trim();
            }
        }
        
        return title;
    }
    
    /**
     * Validate if extracted title is meaningful
     */
    private boolean isValidTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        
        title = title.trim();
        
        // Reject if too short
        if (title.length() < 5) {
            return false;
        }
        
        // Reject if only punctuation/numbers
        if (title.matches("^[\\d\\s\\.,:;\\-–—]+$")) {
            return false;
        }
        
        // Reject if starts with lowercase or punctuation
        if (title.matches("^[a-z\\.,;:].*")) {
            return false;
        }
        
        // Must have at least 2 alphabetic characters
        int alphaCount = 0;
        for (char c : title.toCharArray()) {
            if (Character.isLetter(c)) {
                alphaCount++;
            }
        }
        if (alphaCount < 2) {
            return false;
        }
        
        return true;
    }
    
    private String cleanTitle(String title) {
        // Remove leading chapter markers
        title = title.replaceAll("^(Chương|CHƯƠNG|Bài|BÀI|PHẦN|Phần|Chapter|CHAPTER|Section|SECTION)\\s+[\\dIVX]+\\s*[:\\-–—]?\\s*", "");
        
        // Remove leading/trailing punctuation
        title = title.replaceAll("^[\\s\\.,;:\\-–—]+|[\\s\\.,;:\\-–—]+$", "");
        
        // Trim and limit length
        title = title.trim();
        if (title.length() > 100) {
            title = title.substring(0, 100) + "...";
        }
        
        // If empty after cleaning, return default
        if (title.isEmpty() || !isValidTitle(title)) {
            title = "Untitled Chapter";
        }
        
        return title;
    }
}