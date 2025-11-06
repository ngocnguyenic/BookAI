package service;

import model.Chapter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SmartChapterExtractor {
    
    private static final Logger logger = Logger.getLogger(SmartChapterExtractor.class.getName());

    private static final int MIN_CHAPTERS = 1;
    private static final int MIN_CONTENT_LENGTH = 300;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MIN_MEANINGFUL_WORDS = 2;
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
        "^(Chapter|CHAPTER|Chương|CHƯƠNG)\\s+\\d+.*"
    );
    private static final Pattern NUMBERED_PATTERN = Pattern.compile(
        "^\\d+\\s*[\\.\\)]\\s+.*"
    );
    private static final Pattern TITLE_PREFIX_PATTERN = Pattern.compile(
        "^(Chương|CHƯƠNG|Bài|BÀI|PHẦN|Phần|Chapter|CHAPTER|Section|SECTION)\\s+[\\dIVX]+\\s*[:\\-–—]?\\s*"
    );
    private static final Pattern TITLE_CLEANUP_PATTERN = Pattern.compile(
        "^[\\s\\.,;:\\-–—]+|[\\s\\.,;:\\-–—]+$"
    );
    private static final Pattern INVALID_TITLE_PATTERN = Pattern.compile(
        "^[\\d\\s\\.,:;\\-–—]+$"
    );
    
    private final PDFTableOfContentsExtractor tocExtractor;
    private final ChapterDetector chapterDetector;
    private PDDocument cachedDocument;
    private String cachedPdfPath;
    
    public SmartChapterExtractor() {
        this.tocExtractor = new PDFTableOfContentsExtractor();
        this.chapterDetector = new ChapterDetector();
    }
    
    public ExtractionResult extractChapters(String pdfPath) {
        logger.log(Level.INFO, "🔍 Starting smart chapter extraction for: {0}", pdfPath);
        
        try (PDDocument document = PDDocument.load(new File(pdfPath))) {
            this.cachedDocument = document;
            this.cachedPdfPath = pdfPath;
            List<ExtractionStrategy> strategies = Arrays.asList(
                new BookmarkStrategy(),
                new TOCStrategy(),
                new RegexStrategy()
            );
            
            for (ExtractionStrategy strategy : strategies) {
                try {
                    logger.info("Trying " + strategy.getName() + "...");
                    ExtractionResult result = strategy.extract(pdfPath, document);
                    if (result.success && !result.chapters.isEmpty()) {
                        logger.info("✅ " + strategy.getName() + ": Found " + 
                                  result.chapters.size() + " chapters");
                        return result;
                    }
                    logger.info("⚠️ " + strategy.getName() + ": No valid result");
                } catch (Exception e) {
                    logger.log(Level.WARNING, "⚠️ " + strategy.getName() + " failed: {0}", 
                             e.getMessage());
                }
            }
            logger.warning("All extraction methods failed, treating as single chapter");
            return createFallbackSingleChapter(pdfPath, document);
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "❌ Error loading PDF: {0}", e.getMessage());
            return ExtractionResult.failure(e.getMessage());
        } finally {
            this.cachedDocument = null;
            this.cachedPdfPath = null;
        }
    }
    
    private ExtractionResult createFallbackSingleChapter(String pdfPath, PDDocument document) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);
            
            if (fullText.length() < MIN_CONTENT_LENGTH) {
                return ExtractionResult.failure("PDF content too short: " + fullText.length() + " chars");
            }
            String title = extractTitleFromText(fullText);
            
            Chapter chapter = new Chapter();
            chapter.setChapterNumber(1);
            chapter.setTitle(title);
            chapter.setContent(fullText.trim());
            
            List<Chapter> chapters = Collections.singletonList(chapter);
            
            logger.info("✅ FALLBACK: Created single chapter '" + title + "' (" + fullText.length() + " chars)");
            return ExtractionResult.success(chapters, "Single Chapter Fallback");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fallback extraction failed", e);
            return ExtractionResult.failure("All methods failed: " + e.getMessage());
        }
    }
    
    private String extractTitleFromText(String text) {
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String cleaned = line.trim();
            if (!cleaned.isEmpty() && cleaned.length() < 200 && cleaned.length() > 5) {
                return cleaned;
            }
        }
        
        if (text.length() > 100) {
            return text.substring(0, 100).trim() + "...";
        }
        return "Complete Document";
    }
    
    private interface ExtractionStrategy {
        String getName();
        ExtractionResult extract(String pdfPath, PDDocument document) throws Exception;
    }
    
    private class BookmarkStrategy implements ExtractionStrategy {
        @Override
        public String getName() {
            return "Method 1: PDF Bookmarks";
        }
        
        @Override
        public ExtractionResult extract(String pdfPath, PDDocument document) throws IOException {
            PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
            
            if (outline == null) {
                return ExtractionResult.failure("No outline found");
            }
            List<BookmarkEntry> allBookmarks = new ArrayList<>();
            extractBookmarks(outline, allBookmarks, document);
            
            if (allBookmarks.isEmpty()) {
                return ExtractionResult.failure("No bookmarks extracted");
            }
            
            logger.info("  Found " + allBookmarks.size() + " total bookmarks");
            
            List<BookmarkEntry> chapterBookmarks = allBookmarks.stream()
                .filter(b -> isChapterBookmark(b.title))
                .collect(Collectors.toList());
            
            if (chapterBookmarks.isEmpty()) {
                return ExtractionResult.failure("No chapter bookmarks found");
            }
            
            logger.info("  Filtered to " + chapterBookmarks.size() + " chapter bookmarks");
            
            List<Chapter> chapters = convertBookmarksToChapters(
                chapterBookmarks, 
                document
            );
            
            return ExtractionResult.success(chapters, "PDF Bookmarks");
        }
        
        private boolean isChapterBookmark(String title) {
            String trimmed = title.trim();
            return CHAPTER_PATTERN.matcher(trimmed).matches() || 
                   NUMBERED_PATTERN.matcher(trimmed).matches();
        }
    }
    
    private class TOCStrategy implements ExtractionStrategy {
        @Override
        public String getName() {
            return "Method 2: TOC Text Parsing";
        }
        
        @Override
        public ExtractionResult extract(String pdfPath, PDDocument document) throws Exception {
            List<PDFTableOfContentsExtractor.TOCEntry> tocEntries = 
                tocExtractor.extractTOC(pdfPath);
                      
            if (tocEntries.isEmpty()) {
                return ExtractionResult.failure("No TOC entries found");
            }
            
            List<Chapter> chapters = tocExtractor.extractChaptersByTOC(pdfPath, tocEntries);
            
            if (chapters.isEmpty()) {
                return ExtractionResult.failure("Failed to extract chapters from TOC");
            }
            
            return ExtractionResult.success(chapters, "TOC Text Parsing");
        }
    }
    
    private class RegexStrategy implements ExtractionStrategy {
        @Override
        public String getName() {
            return "Method 3: Regex Pattern Detection";
        }
        
        @Override
        public ExtractionResult extract(String pdfPath, PDDocument document) throws IOException {
            String fullText = PDFExtractor.extractText(pdfPath);
            List<Chapter> chapters = chapterDetector.detectChapters(fullText);
            return ExtractionResult.success(chapters, "Regex Pattern Detection");
        }
    }
    private void extractBookmarks(PDOutlineNode node, List<BookmarkEntry> bookmarks, 
                                   PDDocument document) {
        PDOutlineItem current = node.getFirstChild();
        
        while (current != null) {
            try {
                int pageNum = getPageNumberFromOutlineItem(current, document);
                
                if (pageNum > 0) {
                    BookmarkEntry entry = new BookmarkEntry();
                    entry.title = current.getTitle();
                    entry.pageNumber = pageNum;
                    bookmarks.add(entry);
                    
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("  ✓ Bookmark: '" + entry.title + "' -> page " + pageNum);
                    }
                }
            } catch (Exception e) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning("  ✗ Error extracting bookmark: " + e.getMessage());
                }
            }
            
            current = current.getNextSibling();
        }
    }
    
    private int getPageNumberFromOutlineItem(PDOutlineItem item, PDDocument document) {
        try {

            if (item.getDestination() instanceof PDPageDestination) {
                PDPageDestination dest = (PDPageDestination) item.getDestination();
                return document.getPages().indexOf(dest.getPage()) + 1;
            }
            
            if (item.getAction() != null && 
                item.getAction() instanceof PDActionGoTo) {
                
                PDActionGoTo gotoAction = (PDActionGoTo) item.getAction();
                
                if (gotoAction.getDestination() instanceof PDPageDestination) {
                    PDPageDestination dest = (PDPageDestination) gotoAction.getDestination();
                    return document.getPages().indexOf(dest.getPage()) + 1;
                }
            }
        } catch (IOException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("  Unable to get page number: " + e.getMessage());
            }
        }
        
        return -1;
    }
    
    private List<Chapter> convertBookmarksToChapters(List<BookmarkEntry> bookmarks, 
                                                      PDDocument document) throws IOException {
        List<Chapter> chapters = new ArrayList<>();
        int totalPages = document.getNumberOfPages();
        
        for (int i = 0; i < bookmarks.size(); i++) {
            BookmarkEntry bookmark = bookmarks.get(i);
            
            int startPage = bookmark.pageNumber;
            int endPage = (i + 1 < bookmarks.size()) 
                ? bookmarks.get(i + 1).pageNumber - 1 
                : totalPages;
            if (startPage <= 0 || startPage > totalPages || endPage > totalPages) {
                logger.warning("  Invalid page range for: " + bookmark.title);
                continue;
            }
            
            String content = extractTextFromPages(document, startPage, endPage);
            
            if (content.length() < MIN_CONTENT_LENGTH) {
                logger.warning("  Skipping '" + bookmark.title + "' - too short");
                continue;
            }
            
            Chapter chapter = createChapter(
                chapters.size() + 1,
                bookmark.title,
                content,
                startPage,
                endPage
            );
            
            chapters.add(chapter);
        }
        
        return chapters;
    }
    
    private Chapter createChapter(int number, String title, String content, 
                                  int startPage, int endPage) {
        Chapter chapter = new Chapter();
        chapter.setChapterNumber(number);
        chapter.setTitle(cleanTitle(title));
        chapter.setContent(content);
        
        logger.info(String.format("  📖 Chapter %d: %s (pages %d-%d, %d chars)",
                   number, chapter.getTitle(), startPage, endPage, content.length()));
        
        return chapter;
    }
    private PDFTextStripper textStripper;
    
    private String extractTextFromPages(PDDocument document, int startPage, int endPage) 
            throws IOException {
        if (textStripper == null) {
            textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
        }
        
        textStripper.setStartPage(startPage);
        textStripper.setEndPage(endPage);
        return textStripper.getText(document);
    }
    private String cleanTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "Untitled Chapter";
        }       
        title = TITLE_PREFIX_PATTERN.matcher(title).replaceAll("");
        title = TITLE_CLEANUP_PATTERN.matcher(title).replaceAll("");
        title = title.trim();
        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH) + "...";
        }
        if (!isValidTitle(title)) {
            return "Untitled Chapter";
        }
        
        return title;
    }
    
    private boolean isValidTitle(String title) {
        if (title.isEmpty() || INVALID_TITLE_PATTERN.matcher(title).matches()) {
            return false;
        }
        
        long meaningfulWords = Arrays.stream(title.split("\\s+"))
            .filter(word -> word.length() >= 3 && word.matches(".*[A-Za-z].*"))
            .count();
        
        return meaningfulWords >= MIN_MEANINGFUL_WORDS;
    }
    
    private static class BookmarkEntry {
        String title;
        int pageNumber;
    }
    
    public static class ExtractionResult {
        public final List<Chapter> chapters;
        public final String method;
        public final boolean success;
        public final String error;
        
        private ExtractionResult(List<Chapter> chapters, String method, 
                                boolean success, String error) {
            this.chapters = chapters != null ? new ArrayList<>(chapters) : new ArrayList<>();
            this.method = method;
            this.success = success;
            this.error = error;
        }
        
        public static ExtractionResult success(List<Chapter> chapters, String method) {
            return new ExtractionResult(chapters, method, true, null);
        }
        
        public static ExtractionResult failure(String error) {
            return new ExtractionResult(null, "Unknown", false, error);
        }
        
        // Getters
        public List<Chapter> getChapters() {
            return new ArrayList<>(chapters);
        }
        
        public String getMethod() {
            return method;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getError() {
            return error;
        }
        
        public String getMethodDescription() {
            switch (method) {
                case "PDF Bookmarks":
                    return "Sử dụng PDF bookmarks (chuẩn nhất, có số trang chính xác)";
                case "TOC Text Parsing":
                    return "Phân tích trang mục lục (TOC) của sách";
                case "Regex Pattern Detection":
                    return "Phát hiện pattern trong nội dung (fallback)";
                case "Single Chapter Fallback":
                    return "Toàn bộ PDF được coi là 1 chương duy nhất";
                default:
                    return "Không xác định";
            }
        }
    }
}