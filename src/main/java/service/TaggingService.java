package service;

import dao.QADao;
import model.QA;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;


public class TaggingService {
    
    private static final Logger logger = Logger.getLogger(TaggingService.class.getName());
    
    private final OllamaService ollamaService;
    private final QADao qaDao;
    public TaggingService() {
        this.ollamaService = new OllamaService();
        this.qaDao = new QADao();
        
        logger.info("✅ TaggingService initialized");
    }
    public void tagQA(int qaId, String question, String answer, String difficulty) {
        try {
            logger.info("🏷️ Starting auto-tagging for QA #" + qaId);
            logger.info("   Question: " + truncate(question, 50));
            
            
            OllamaService.QATaggingResult tagging = ollamaService.autoTagQA(question, answer, difficulty);
            
            logger.info("   AI Result: Bloom=" + tagging.bloomLevel + ", Type=" + tagging.questionType);
            logger.info("   Topics: " + tagging.topics);
            logger.info("   Concepts: " + tagging.concepts);
            
           
            qaDao.updateQAMetadata(qaId, tagging.bloomLevel, tagging.questionType);
            logger.info("   ✅ Updated Q&A metadata");
            
            
            if (tagging.topics != null && !tagging.topics.isEmpty()) {
                for (String topic : tagging.topics) {
                    int tagId = qaDao.getOrCreateTag(topic, "topic");
                    qaDao.addTagToQA(qaId, tagId, (float) tagging.confidence);
                }
                logger.info("   ✅ Added " + tagging.topics.size() + " topic tags");
            }
            
            
            if (tagging.concepts != null && !tagging.concepts.isEmpty()) {
                for (String concept : tagging.concepts) {
                    int tagId = qaDao.getOrCreateTag(concept, "concept");
                    qaDao.addTagToQA(qaId, tagId, (float) (tagging.confidence * 0.9));
                }
                logger.info("   ✅ Added " + tagging.concepts.size() + " concept tags");
            }
            
            
            int bloomTagId = qaDao.getOrCreateTag(tagging.bloomLevel, "bloom_level");
            qaDao.addTagToQA(qaId, bloomTagId, 1.0f);
            logger.info("   ✅ Added Bloom level tag");
            int typeTagId = qaDao.getOrCreateTag(tagging.questionType, "question_type");
            qaDao.addTagToQA(qaId, typeTagId, 1.0f);
            logger.info("   ✅ Added question type tag");
            qaDao.markAsAutoTagged(qaId);
            logger.info("✅ Auto-tagging completed for QA #" + qaId);
            
        } catch (Exception e) {
            logger.severe("❌ Auto-tagging failed for QA #" + qaId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    public void tagQA(QA qa) {
        tagQA(qa.getQAID(), qa.getQuestion(), qa.getAnswer(), qa.getDifficulty());
    }

    public void batchTagQAs(List<QA> qas) {
        logger.info("🔄 Starting batch tagging for " + qas.size() + " Q&As...");
        
        int success = 0;
        int failed = 0;
        
        for (int i = 0; i < qas.size(); i++) {
            QA qa = qas.get(i);
            logger.info("Processing " + (i + 1) + "/" + qas.size() + ": QA #" + qa.getQAID());
            
            try {
                tagQA(qa);
                success++;
                

                if (i < qas.size() - 1) {
                    Thread.sleep(1000); 
                }
                
            } catch (Exception e) {
                logger.severe("❌ Failed to tag QA #" + qa.getQAID() + ": " + e.getMessage());
                failed++;
            }
        }
        
        logger.info("✅ Batch tagging completed: " + success + " success, " + failed + " failed");
    }
    public void tagAllUntagged() {
        try {
            logger.info("🔍 Finding untagged Q&As...");
            
            List<Integer> untaggedIds = qaDao.getUntaggedQAIds();
            logger.info("📊 Found " + untaggedIds.size() + " untagged Q&As");
            
            if (untaggedIds.isEmpty()) {
                logger.info("✅ All Q&As are already tagged");
                return;
            }

            int success = 0;
            int failed = 0;
            
            for (int i = 0; i < untaggedIds.size(); i++) {
                int qaId = untaggedIds.get(i);
                logger.info("Processing " + (i + 1) + "/" + untaggedIds.size() + ": QA #" + qaId);
                
                try {
                    QA qa = qaDao.getQAById(qaId);
                    
                    if (qa != null) {
                        tagQA(qa);
                        success++;

                        if (i < untaggedIds.size() - 1) {
                            Thread.sleep(1000);
                        }
                    } else {
                        logger.warning("⚠️ Q&A #" + qaId + " not found in database");
                        failed++;
                    }
                    
                } catch (Exception e) {
                    logger.severe("❌ Failed to tag QA #" + qaId + ": " + e.getMessage());
                    failed++;
                }
            }
            
            logger.info("✅ Auto-tagging completed: " + success + " success, " + failed + " failed");
            
        } catch (Exception e) {
            logger.severe("❌ Failed to tag untagged Q&As: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    public void retagQAs(List<Integer> qaIds) {
        logger.info("🔄 Re-tagging " + qaIds.size() + " Q&As...");
        
        int success = 0;
        int failed = 0;
        
        for (int i = 0; i < qaIds.size(); i++) {
            int qaId = qaIds.get(i);
            logger.info("Processing " + (i + 1) + "/" + qaIds.size() + ": QA #" + qaId);
            
            try {
                // Get Q&A
                QA qa = qaDao.getQAById(qaId);
                
                if (qa != null) {

                    tagQA(qa);
                    success++;

                    if (i < qaIds.size() - 1) {
                        Thread.sleep(1000);
                    }
                } else {
                    logger.warning("⚠️ Q&A #" + qaId + " not found");
                    failed++;
                }
                
            } catch (Exception e) {
                logger.severe("❌ Failed to re-tag QA #" + qaId + ": " + e.getMessage());
                failed++;
            }
        }
        
        logger.info("✅ Re-tagging completed: " + success + " success, " + failed + " failed");
    }

    public TaggingStats getTaggingStats() {
        try {
            QADao.IndexStats stats = qaDao.getIndexingStats();
            
            TaggingStats result = new TaggingStats();
            result.totalQAs = stats.totalQAs;
            result.tagged = stats.autoTagged;
            result.untagged = stats.totalQAs - stats.autoTagged;
            result.percentageTagged = (stats.autoTagged * 100.0) / Math.max(stats.totalQAs, 1);
            
            logger.info("📊 Tagging stats: " + result);
            return result;
            
        } catch (Exception e) {
            logger.severe("❌ Failed to get tagging stats: " + e.getMessage());
            return null;
        }
    }
    
boolean isServiceHealthy() {
        return ollamaService.isServerHealthy();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "null";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    public static class TaggingStats {
        public int totalQAs;
        public int tagged;
        public int untagged;
        public double percentageTagged;
        
        @Override
        public String toString() {
            return String.format("Total: %d, Tagged: %d (%.1f%%), Untagged: %d",
                totalQAs, tagged, percentageTagged, untagged);
        }
    }
}