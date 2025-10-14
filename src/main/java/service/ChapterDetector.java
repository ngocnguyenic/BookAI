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
        // Vietnamese
        "(?m)^\\s*(Chương|CHƯƠNG)\\s+([\\dIVX]+)\\s*[:\\-–—]?\\s*(.{0,100})$",
        "(?m)^\\s*(Bài|BÀI)\\s+([\\dIVX]+)\\s*[:\\-–—]?\\s*(.{0,100})$",
        "(?m)^\\s*(PHẦN|Phần)\\s+([\\dIVX]+)\\s*[:\\-–—]?\\s*(.{0,100})$",
        
        // English
        "(?m)^\\s*(Chapter|CHAPTER)\\s+([\\dIVX]+)\\s*[:\\-–—]?\\s*(.{0,100})$",
        "(?m)^\\s*(Section|SECTION)\\s+([\\dIVX]+)\\s*[:\\-–—]?\\s*(.{0,100})$",
        
        // Numbered only: "1. Something" or "1 Something"
        "(?m)^\\s*(\\d+)\\s*[\\.\\)]\\s+([A-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼỀỀỂỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪỬỮỰỲỴỶỸ].{3,80})$"
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
            
            // Skip chapters quá ngắn (< 300 chars)
            if (content.length() < 30) {
                logger.warning("Skipping chapter " + (i + 1) + " - too short (" + content.length() + " chars)");
                continue;
            }
            
            // Extract title
            String title = match.group().trim();
            if (match.groupCount() >= 3 && match.group(3) != null && !match.group(3).trim().isEmpty()) {
                title = match.group(3).trim();
            }
            
            Chapter chap = new Chapter();
            chap.setChapterNumber(i + 1);
            chap.setTitle(cleanTitle(title));
            chap.setContent(content);
            
            chapters.add(chap);
            
            logger.info("  Chapter " + (i + 1) + ": " + chap.getTitle() + 
                       " (" + content.length() + " chars)");
        }
        
        return chapters;
    }
    
    private String cleanTitle(String title) {
        // Remove leading chapter markers
        title = title.replaceAll("^(Chương|CHƯƠNG|Bài|BÀI|PHẦN|Phần|Chapter|CHAPTER|Section|SECTION)\\s+[\\dIVX]+\\s*[:\\-–—]?\\s*", "");
        
        // Trim and limit length
        title = title.trim();
        if (title.length() > 100) {
            title = title.substring(0, 100) + "...";
        }
        
        // If empty, return default
        if (title.isEmpty()) {
            title = "Untitled Chapter";
        }
        
        return title;
    }
}