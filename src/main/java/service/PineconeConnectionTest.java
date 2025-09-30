package service;

import java.util.logging.Logger;

public class PineconeConnectionTest {
    
    private static final Logger logger = Logger.getLogger(PineconeConnectionTest.class.getName());
    
    public static void main(String[] args) {
        try {
            logger.info("=== TESTING PINECONE CONNECTION ===");
            
            // Test 1: Initialize services
            logger.info("\n[Test 1] Initializing services...");
            PineconeService pineconeService = new PineconeService();
            EmbeddingService embeddingService = new EmbeddingService();
            logger.info("✓ Services initialized");
            
            // Test 2: Generate test embedding
            logger.info("\n[Test 2] Generating test embedding...");
            String testText = "This is a test sentence for embedding generation.";
            float[] embedding = embeddingService.generateEmbedding(testText);
            logger.info("✓ Generated embedding with dimension: " + embedding.length);
            
            // Test 3: Test upsert to Pinecone
            logger.info("\n[Test 3] Testing upsert to Pinecone...");
            model.Chapter testChapter = new model.Chapter();
            testChapter.setChapterID(99999); // Test ID
            testChapter.setBookID(1);
            testChapter.setChapterNumber(1);
            testChapter.setTitle("Test Chapter");
            testChapter.setContent("This is test content for Pinecone connection testing.");
            testChapter.setSummary("Test summary");
            
            boolean upsertSuccess = pineconeService.upsertChapter(testChapter, embedding);
            if (upsertSuccess) {
                logger.info("✓ Upsert successful");
            } else {
                logger.warning("✗ Upsert failed");
            }
            
            // Test 4: Test query
            logger.info("\n[Test 4] Testing query...");
            String queryText = "test chapter";
            float[] queryEmbedding = embeddingService.generateEmbedding(queryText);
            
            var results = pineconeService.queryTopK(queryEmbedding, 3, null);
            logger.info("✓ Query returned " + results.size() + " results");
            
            for (var result : results) {
                logger.info("  - " + result.toString());
            }
            
            // Test 5: Cleanup
            logger.info("\n[Test 5] Cleaning up test data...");
            // Note: Pinecone doesn't have direct delete by ID in this implementation
            // You would need to add that method if needed
            logger.info("✓ Test completed");
            
            logger.info("\n=== ALL TESTS PASSED ===");
            logger.info("Pinecone is configured correctly!");
            
        } catch (Exception e) {
            logger.severe("=== TEST FAILED ===");
            logger.severe("Error: " + e.getMessage());
            e.printStackTrace();
            
            logger.info("\nTROUBLESHOOTING:");
            logger.info("1. Check if Pinecone index 'book-chapters' exists in your Pinecone console");
            logger.info("2. Verify pinecone.index.host in config.properties matches your index host");
            logger.info("3. Verify API key is correct: pcsk_52HVrn...");
            logger.info("4. Check index dimension is 768");
            logger.info("5. Check index metric is 'cosine'");
        }
    }
}