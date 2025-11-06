package service;

import dao.QADao;
import java.util.List;
import java.util.logging.Logger;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class VectorIndexingService {
    
    private static final Logger logger = Logger.getLogger(VectorIndexingService.class.getName());
    
    private final EmbeddingService embeddingService;
    private final FAISSService faissService;
    private final QADao qaDao;
    
    public VectorIndexingService() {
        this.embeddingService = new EmbeddingService();
        this.faissService = new FAISSService();
        this.qaDao = new QADao(); 
        
        logger.info("✅ VectorIndexingService initialized");
    }
    
    public void indexQA(int qaId, String question, String answer) {
        try {
            logger.info("📊 Indexing QA #" + qaId + " into FAISS...");
  
            float[] embedding = embeddingService.generateQAEmbedding(question, answer);
            logger.info("   Generated embedding: " + embedding.length + " dimensions");

            faissService.addVector(qaId, embedding);
            logger.info("   Added to FAISS index");

            String checksum = calculateChecksum(embedding);
            logger.info("   Vector checksum: " + checksum.substring(0, 8) + "...");
 
            qaDao.saveVectorMetadata(qaId, "embedding-001", embedding.length, checksum);
            logger.info("   Saved vector metadata");

            qaDao.markAsVectorIndexed(qaId);
            logger.info("   Marked as indexed");
            
            logger.info("✅ QA #" + qaId + " indexed successfully");
            
        } catch (Exception e) {
            logger.severe("❌ Failed to index QA #" + qaId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void batchIndexQAs(List<Integer> qaIds) {
        logger.info("🔄 Starting batch indexing for " + qaIds.size() + " Q&As...");
        
        int success = 0;
        int failed = 0;
        
        for (int i = 0; i < qaIds.size(); i++) {
            int qaId = qaIds.get(i);
            logger.info("Processing " + (i + 1) + "/" + qaIds.size() + ": QA #" + qaId);
            
            try {
                List<model.QA> qas = qaDao.getQAsByChapterId(qaId); // Need chapter ID

                logger.warning("⚠️ Skipping QA #" + qaId + " - need getQAById method");
                
            } catch (Exception e) {
                logger.severe("❌ Failed to index QA #" + qaId + ": " + e.getMessage());
                failed++;
            }
        }
        
        logger.info("✅ Batch indexing completed: " + success + " success, " + failed + " failed");
    }
    
    public List<Integer> searchSimilarQAs(String queryText, int topK) throws Exception {
        logger.info("🔍 Searching similar Q&As for: " + queryText);
        
        float[] queryEmbedding = embeddingService.generateEmbedding(queryText);
        logger.info("   Query embedding generated: " + queryEmbedding.length + " dims");

        List<Integer> similarQAIds = faissService.searchSimilarByVector(queryEmbedding, topK);
        
        logger.info("✅ Found " + similarQAIds.size() + " similar Q&As: " + similarQAIds);
        return similarQAIds;
    }

    public List<Integer> searchSimilarByQuestion(String question, int topK) throws Exception {
        logger.info("🔍 Searching Q&As similar to question: " + question);
        
        float[] queryEmbedding = embeddingService.generateQuestionEmbedding(question);
        return faissService.searchSimilarByVector(queryEmbedding, topK);
    }
    
    public void reindexUnindexed() {
        try {
            logger.info("🔄 Finding unindexed Q&As...");
            
            List<Integer> unindexedIds = qaDao.getUnindexedQAIds();
            logger.info("📊 Found " + unindexedIds.size() + " unindexed Q&As");
            
            if (unindexedIds.isEmpty()) {
                logger.info("✅ All Q&As are already indexed");
                return;
            }

            logger.warning("⚠️ Batch indexing not fully implemented yet");
            
        } catch (Exception e) {
            logger.severe("❌ Failed to reindex: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public boolean isQAIndexed(int qaId) {
        try {
            return qaDao.isQAIndexed(qaId);
        } catch (Exception e) {
            logger.severe("❌ Error checking index status for QA #" + qaId + ": " + e.getMessage());
            return false;
        }
    }
    
    public QADao.IndexStats getIndexingStats() {
        try {
            QADao.IndexStats stats = qaDao.getIndexingStats();
            logger.info("📊 Indexing stats: " + stats);
            return stats;
        } catch (Exception e) {
            logger.severe("❌ Failed to get indexing stats: " + e.getMessage());
            return null;
        }
    }
    
    private String calculateChecksum(float[] vector) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            
            StringBuilder sb = new StringBuilder();
            for (float v : vector) {
                sb.append(v).append(",");
            }
            
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (Exception e) {
            logger.warning("⚠️ Failed to calculate checksum: " + e.getMessage());
            return "unknown";
        }
    }
    
    public boolean isServiceHealthy() {
        boolean embeddingHealthy = embeddingService.isServiceHealthy();
        boolean faissHealthy = faissService.isServerHealthy();
        
        logger.info("📊 Service health check:");
        logger.info("   Embedding: " + (embeddingHealthy ? "✅" : "❌"));
        logger.info("   FAISS: " + (faissHealthy ? "✅" : "❌"));
        
        return embeddingHealthy && faissHealthy;
    }
}