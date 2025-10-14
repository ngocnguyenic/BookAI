package service;

import dao.ChapterDAO;
import model.Chapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class RAGQueryService {
    
    private static final Logger logger = Logger.getLogger(RAGQueryService.class.getName());
    
    private final FAISSService faissService;
    private final ChapterDAO chapterDAO;
    private final OllamaService ollamaService;
    private final LocalEmbeddingService embeddingService; // THÊM
    
    public RAGQueryService() {
        this.faissService = new FAISSService();
        this.chapterDAO = new ChapterDAO();
        this.ollamaService = new OllamaService();
        this.embeddingService = new LocalEmbeddingService(); // THÊM
    }
    
    public RAGResponse query(String question, int topK) throws Exception {
        logger.info("RAG Query: " + question);
        
        // Step 1: Generate embedding cho question - DÙNG LOCAL
        logger.info("Generating query embedding with LocalEmbeddingService...");
        float[] queryEmbedding = embeddingService.generateEmbedding(question);
        
        // Step 2: Search similar vectors trong FAISS
        List<Integer> chapterIds = faissService.searchSimilarByVector(queryEmbedding, topK);
        
        if (chapterIds == null || chapterIds.isEmpty()) {
            return new RAGResponse(
                "Không tìm thấy thông tin liên quan trong cơ sở dữ liệu.",
                new ArrayList<>()
            );
        }
        
        logger.info("Found " + chapterIds.size() + " relevant chapters");
        
        // Step 3: Lấy chapter content
        List<Chapter> relevantChapters = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        
        for (Integer chapterId : chapterIds) {
            Chapter chapter = chapterDAO.getChapterById(chapterId);
            if (chapter != null) {
                relevantChapters.add(chapter);
                context.append("=== ").append(chapter.getTitle()).append(" ===\n");
                if (chapter.getSummary() != null) {
                    context.append(chapter.getSummary()).append("\n\n");
                }
                String contentSample = chapter.getContent().substring(0, 
                    Math.min(1500, chapter.getContent().length()));
                context.append(contentSample).append("\n\n");
            }
        }
        
        // Step 4: Generate answer với Ollama
        logger.info("Generating answer with Ollama...");
        String answer;
        try {
            answer = ollamaService.answerQuestion(question, context.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Answer generation interrupted", e);
        }
        
        logger.info("✅ RAG response generated successfully");
        return new RAGResponse(answer, relevantChapters);
    }
    
    public static class RAGResponse {
        public String answer;
        public List<Chapter> sources;
        
        public RAGResponse(String answer, List<Chapter> sources) {
            this.answer = answer;
            this.sources = sources;
        }
    }
}