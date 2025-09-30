package service;

import dao.ChapterDAO;
import model.Chapter;
import service.PineconeService.SearchResult;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Complete RAG (Retrieval-Augmented Generation) Service
 * Kết hợp vector search với LLM generation
 */
public class RAGQueryService {
    
    private static final Logger logger = Logger.getLogger(RAGQueryService.class.getName());
    
    private final EmbeddingService embeddingService;
    private final PineconeService pineconeService;
    private final GeminiService geminiService;
    private final ChapterDAO chapterDAO;
    
    public RAGQueryService() {
        this.embeddingService = new EmbeddingService();
        this.pineconeService = new PineconeService();
        this.geminiService = new GeminiService();
        this.chapterDAO = new ChapterDAO();
    }
    
    /**
     * Main RAG Query Method
     * @param question Câu hỏi của user
     * @param bookId (optional) Giới hạn search trong một book cụ thể
     * @param topK Số lượng chapters liên quan cần retrieve
     * @return Answer được generate từ relevant context
     */
    public RAGResponse query(String question, Integer bookId, int topK) throws IOException {
        logger.info("🔍 RAG Query: " + question);
        
        // Step 1: Convert question thành embedding vector
        logger.info("Step 1: Generating query embedding...");
        float[] queryEmbedding = embeddingService.generateEmbedding(question);
        
        // Step 2: Search Pinecone để tìm relevant chapters
        logger.info("Step 2: Searching Pinecone for relevant chapters...");
        List<SearchResult> searchResults = pineconeService.queryTopK(queryEmbedding, topK, bookId);
        
        if (searchResults.isEmpty()) {
            return new RAGResponse(
                "Không tìm thấy thông tin liên quan trong cơ sở dữ liệu.",
                null,
                0
            );
        }
        
        // Step 3: Fetch full content từ database
        logger.info("Step 3: Fetching full chapter content...");
        List<Chapter> relevantChapters = fetchFullChapters(searchResults);
        
        // Step 4: Build context từ relevant chapters
        logger.info("Step 4: Building context...");
        String context = buildContext(relevantChapters, searchResults);
        
        // Step 5: Generate answer using Gemini với context
        logger.info("Step 5: Generating answer with LLM...");
        String answer = generateAnswerWithContext(question, context);
        
        // Step 6: Build response với citations
        RAGResponse response = new RAGResponse(answer, relevantChapters, searchResults.size());
        response.searchResults = searchResults;
        
        logger.info("✅ RAG Query completed successfully");
        return response;
    }
    
    /**
     * Fetch full chapter content từ database dựa trên search results
     */
    private List<Chapter> fetchFullChapters(List<SearchResult> searchResults) {
        return searchResults.stream()
            .map(result -> {
                try {
                    // Extract chapterID từ id (format: "chapter_123")
                    int chapterId = Integer.parseInt(result.id.replace("chapter_", ""));
                    return chapterDAO.getChapterById(chapterId);
                } catch (Exception e) {
                    logger.warning("Failed to fetch chapter: " + result.id);
                    return null;
                }
            })
            .filter(chapter -> chapter != null)
            .collect(Collectors.toList());
    }
    
    /**
     * Build context string từ relevant chapters
     */
    private String buildContext(List<Chapter> chapters, List<SearchResult> searchResults) {
        StringBuilder context = new StringBuilder();
        
        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            SearchResult result = searchResults.get(i);
            
            context.append(String.format("\n=== NGUỒN %d (Similarity: %.3f) ===\n", i + 1, result.score));
            context.append(String.format("Chương %d: %s\n", chapter.getChapterNumber(), chapter.getTitle()));
            
            if (chapter.getSummary() != null && !chapter.getSummary().isEmpty()) {
                context.append("Tóm tắt: ").append(chapter.getSummary()).append("\n\n");
            }
            
            // Lấy 1500 chars đầu của content
            String content = chapter.getContent();
            if (content.length() > 1500) {
                content = content.substring(0, 1500) + "...";
            }
            context.append("Nội dung: ").append(content).append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * Generate answer sử dụng LLM với retrieved context
     */
    private String generateAnswerWithContext(String question, String context) throws IOException {
        String prompt = buildRAGPrompt(question, context);
        
        // Sử dụng detectChaptersWithAI vì nó có sẵn raw call
        // Hoặc có thể tạo method mới trong GeminiService
        String rawResponse = callGeminiForRAG(prompt);
        
        return rawResponse;
    }
    
    /**
     * Build prompt cho RAG
     */
    private String buildRAGPrompt(String question, String context) {
        return """
            Bạn là một trợ lý AI thông minh giúp sinh viên học tập.
            
            NHIỆM VỤ: Trả lời câu hỏi dựa trên NGỮ CẢNH được cung cấp từ tài liệu học tập.
            
            QUY TẮC QUAN TRỌNG:
            1. CHỈ trả lời dựa trên thông tin có trong NGỮ CẢNH
            2. Nếu không tìm thấy thông tin, nói rõ "Không tìm thấy thông tin trong tài liệu"
            3. Trích dẫn nguồn khi trả lời (ví dụ: "Theo Chương 2...")
            4. Trả lời ngắn gọn, súc tích, dễ hiểu
            5. Sử dụng tiếng Việt rõ ràng
            
            NGỮ CẢNH:
            """ + context + """
            
            CÂU HỎI: """ + question + """
            
            TRẢ LỜI (với trích dẫn nguồn):
            """;
    }
    
    /**
     * Call Gemini API trực tiếp cho RAG
     */
    private String callGeminiForRAG(String prompt) throws IOException {
        // Reuse existing GeminiService infrastructure
        // Trick: dùng generateChapterSummary nhưng với prompt custom
        return geminiService.generateChapterSummary("RAG Query", prompt);
    }
    
    /**
     * Response object chứa answer và metadata
     */
    public static class RAGResponse {
        public String answer;
        public List<Chapter> relevantChapters;
        public int totalRetrieved;
        public List<SearchResult> searchResults;
        
        public RAGResponse(String answer, List<Chapter> relevantChapters, int totalRetrieved) {
            this.answer = answer;
            this.relevantChapters = relevantChapters;
            this.totalRetrieved = totalRetrieved;
        }
        
        @Override
        public String toString() {
            return String.format("RAGResponse{answer_length=%d, relevant_chapters=%d}",
                answer.length(), relevantChapters != null ? relevantChapters.size() : 0);
        }
    }
}