package service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import model.Chapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFTableOfContentsExtractor {
    
    /**
     * Extract TOC từ vài trang đầu của PDF
     * @param pdfPath
     * @throws java.io.IOException
     */
    public List<TOCEntry> extractTOC(String pdfPath) throws IOException {
        List<TOCEntry> tocEntries = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            // Chỉ lấy 10-15 trang đầu (thường là nơi có TOC)
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(15, document.getNumberOfPages()));
            
            String tocText = stripper.getText(document);
            
            // Parse TOC với nhiều patterns
            tocEntries = parseTOC(tocText);
            
            // Validate TOC entries có page numbers hợp lý
            tocEntries = validateTOCEntries(tocEntries, document.getNumberOfPages());
        }
        
        return tocEntries;
    }
    
    /**
     * Parse TOC text thành structured entries
     */
    private List<TOCEntry> parseTOC(String tocText) {
        List<TOCEntry> entries = new ArrayList<>();
        
        // Pattern cho TOC entry: "Chapter X: Title .... Page"
        // Ví dụ: "Chapter 1: Java Primer ......................... 27"
        //        "Chapter 2: Object-Oriented Design ............ 51"
        
        Pattern[] patterns = {
            // Pattern 1: "Chapter X: Title ... PageNum"
            Pattern.compile("(?m)^\\s*(Chapter|CHAPTER)\\s+(\\d+)\\s*[:\\-]?\\s*([^\\.]+?)[\\s\\.]+?(\\d{1,4})\\s*$"),
            
            // Pattern 2: "X. Title ... PageNum"  
            Pattern.compile("(?m)^\\s*(\\d+)\\s*\\.\\s+([^\\.]+?)[\\s\\.]+?(\\d{1,4})\\s*$"),
            
            // Pattern 3: Vietnamese "Chương X: Title ... Trang"
            Pattern.compile("(?m)^\\s*(Chương|CHƯƠNG)\\s+(\\d+)\\s*[:\\-]?\\s*([^\\.]+?)[\\s\\.]+?(\\d{1,4})\\s*$"),
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(tocText);
            
            while (matcher.find()) {
                TOCEntry entry = new TOCEntry();
                
                // Extract chapter number   
                String chapterNumStr = matcher.group(2);
                if (chapterNumStr == null) {
                    chapterNumStr = matcher.group(1);
                }
                entry.chapterNumber = Integer.parseInt(chapterNumStr.trim());
                
                // Extract title
                entry.title = matcher.group(3).trim()
                    .replaceAll("\\s+", " ")
                    .replaceAll("[\\.\\_\\-]+$", "")
                    .trim();
                
                // Extract page number
                String pageStr = matcher.group(4);
                entry.startPage = Integer.parseInt(pageStr.trim());
                
                entries.add(entry);
            }
            
            // Nếu tìm thấy entries hợp lý, dừng lại
            if (entries.size() >= 3) {
                break;
            }
        }
        
        return entries;
    }
    
    /**
     * Validate TOC entries
     */
    private List<TOCEntry> validateTOCEntries(List<TOCEntry> entries, int totalPages) {
        List<TOCEntry> valid = new ArrayList<>();
        
        for (int i = 0; i < entries.size(); i++) {
            TOCEntry entry = entries.get(i);
            
            // Skip nếu page number không hợp lý
            if (entry.startPage < 1 || entry.startPage > totalPages) {
                continue;
            }
            
            // Set endPage từ entry tiếp theo
            if (i + 1 < entries.size()) {
                entry.endPage = entries.get(i + 1).startPage - 1;
            } else {
                entry.endPage = totalPages;
            }
            
            // Skip nếu chapter quá ngắn (< 2 pages)
            if (entry.endPage - entry.startPage < 2) {
                continue;
            }
            
            valid.add(entry);
        }
        
        return valid;
    }
    
    
    public List<Chapter> extractChaptersByTOC(String pdfPath, List<TOCEntry> tocEntries) 
            throws IOException {
        
        List<Chapter> chapters = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            for (TOCEntry entry : tocEntries) {
                // Extract text từ page range
                stripper.setStartPage(entry.startPage);
                stripper.setEndPage(entry.endPage);
                
                String content = stripper.getText(document);
                
                // Clean content
                content = content.trim();
                
                // Skip nếu content quá ngắn
                if (content.length() < 500) {
                    continue;
                }
                
                Chapter chapter = new Chapter();
                chapter.setChapterNumber(entry.chapterNumber);
                chapter.setTitle(entry.title);
                chapter.setContent(content);
                
                chapters.add(chapter);
            }
        }
        
        return chapters;
    }
    
    /**
     * TOC Entry structure
     */
    public static class TOCEntry {
        public int chapterNumber;
        public String title;
        public int startPage;
        public int endPage;
        
        @Override
        public String toString() {
            return String.format("Chapter %d: %s (Pages %d-%d)", 
                chapterNumber, title, startPage, endPage);
        }
    }
}