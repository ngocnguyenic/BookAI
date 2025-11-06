package dao;

import connect.DBConnection;
import static connect.DBConnection.getConnection;
import model.QA;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.UserQAPerformance;

public class QADao {
    
    private static final Logger logger = Logger.getLogger(QADao.class.getName());
  
    public List<QA> getQAsByChapterId(int chapterId) throws SQLException {
        List<QA> qas = new ArrayList<>();
        String sql = "SELECT [QAID], [ChapterID], [Question], [Answer], [Difficulty], [QuestionType], " +
                     "[BloomLevel], [QuestionTypeTag], [AutoTagged], [VectorIndexed], [UpdatedAt] " +
                     "FROM [QA] WHERE [ChapterID] = ? ORDER BY [QAID]";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, chapterId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    QA qa = new QA();
                    qa.setQAID(rs.getInt("QAID"));
                    qa.setChapterID(rs.getInt("ChapterID"));
                    qa.setQuestion(rs.getString("Question"));
                    qa.setAnswer(rs.getString("Answer"));
                    qa.setDifficulty(rs.getString("Difficulty"));
                    qa.setQuestionType(rs.getString("QuestionType"));
                    qa.setBloomLevel(rs.getString("BloomLevel"));
                    qa.setQuestionTypeTag(rs.getString("QuestionTypeTag"));
                    qa.setAutoTagged(rs.getBoolean("AutoTagged"));
                    qa.setVectorIndexed(rs.getBoolean("VectorIndexed"));
                    qa.setUpdatedAt(rs.getTimestamp("UpdatedAt"));
                    
                    qas.add(qa);
                }
            }
        }
        
        logger.info("✅ Retrieved " + qas.size() + " Q&As for chapter ID: " + chapterId);
        return qas;
    }
    
    public int insertQA(int chapterId, String question, String answer, String difficulty, String questionType) 
            throws SQLException {
        String sql = "INSERT INTO [QA] ([ChapterID], [Question], [Answer], [Difficulty], [QuestionType]) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setInt(1, chapterId);
            ps.setString(2, question);
            ps.setString(3, answer);
            ps.setString(4, difficulty);
            ps.setString(5, questionType);
            
            int affected = ps.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int qaId = rs.getInt(1);
                        logger.info("✅ Inserted Q&A with ID: " + qaId);
                        return qaId;
                    }
                }
            }
            
            throw new SQLException("Failed to insert Q&A, no ID obtained");
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Failed to insert Q&A", e);
            throw e;
        }
    }
    
    public int insertQABatch(int chapterId, List<QAItem> qas, String generatedBy) throws SQLException {
        if (qas == null || qas.isEmpty()) {
            logger.warning("⚠️ No Q&As to insert");
            return 0;
        }
        
        String sql = "INSERT INTO [QA] ([ChapterID], [Question], [Answer], [Difficulty], [QuestionType]) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        logger.info("📝 Preparing to insert " + qas.size() + " Q&As for chapter " + chapterId);
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            
            for (QAItem item : qas) {
                ps.setInt(1, chapterId);
                ps.setString(2, item.getQuestion());
                ps.setString(3, item.getAnswer());
                ps.setString(4, item.getDifficulty());
                ps.setString(5, item.getQuestionType());
                ps.addBatch();
            }
            
            int[] results = ps.executeBatch();
            conn.commit();
            
            int inserted = 0;
            for (int r : results) {
                if (r >= 0) inserted += r;
                else inserted += 1;
            }
            
            logger.info("✅ Successfully inserted " + inserted + " Q&As");
            return inserted;
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Failed to insert Q&As batch", e);
            throw e;
        }
    }

    public int getQACountByChapterId(int chapterId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM [QA] WHERE [ChapterID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, chapterId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public void deleteQAsByChapterId(int chapterId) throws SQLException {
        String sql = "DELETE FROM [QA] WHERE [ChapterID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, chapterId);
            int deleted = ps.executeUpdate();
            
            logger.info("✅ Deleted " + deleted + " Q&As for chapter ID: " + chapterId);
        }
    }
    
    public int getOrCreateTag(String tagName, String tagType) throws SQLException {
        // Try to get existing tag
        String selectSql = "SELECT [TagID] FROM [Tags] WHERE [TagName] = ? AND [TagType] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            
            ps.setString(1, tagName);
            ps.setString(2, tagType);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("TagID");
                }
            }
        }
        
        String insertSql = "INSERT INTO [Tags] ([TagName], [TagType]) VALUES (?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, tagName);
            ps.setString(2, tagType);
            
            int affected = ps.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int tagId = rs.getInt(1);
                        logger.fine("✅ Created new tag: " + tagName + " (ID: " + tagId + ")");
                        return tagId;
                    }
                }
            }
            
            throw new SQLException("Failed to create tag: " + tagName);
        }
    }
    
    public void addTagToQA(int qaId, int tagId, float confidence) throws SQLException {
        String sql = "INSERT INTO [QATags] ([QAID], [TagID], [Confidence]) VALUES (?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, qaId);
            ps.setInt(2, tagId);
            ps.setFloat(3, confidence);
            
            ps.executeUpdate();
            logger.fine("✅ Linked Q&A #" + qaId + " to Tag #" + tagId);
            
        } catch (SQLException e) {
            if (!e.getMessage().contains("PRIMARY KEY")) {
                throw e;
            }
        }
    }
    
    public QA getQAById(int qaId) throws SQLException {
    
        return null;
    
    }
    public List<Tag> getTagsByQAId(int qaId) throws SQLException {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT t.[TagID], t.[TagName], t.[TagType], t.[Description], qt.[Confidence] " +
                     "FROM [Tags] t " +
                     "INNER JOIN [QATags] qt ON t.[TagID] = qt.[TagID] " +
                     "WHERE qt.[QAID] = ? " +
                     "ORDER BY qt.[Confidence] DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, qaId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Tag tag = new Tag();
                    tag.setTagID(rs.getInt("TagID"));
                    tag.setTagName(rs.getString("TagName"));
                    tag.setTagType(rs.getString("TagType"));
                    tag.setDescription(rs.getString("Description"));
                    tag.setConfidence(rs.getFloat("Confidence"));
                    tags.add(tag);
                }
            }
        }
        
        return tags;
    }
    public List<UserQAPerformance> getAllPerformanceForQA(int qaID) throws SQLException {
    String sql = "SELECT * FROM UserQAPerformance WHERE QAID = ? ORDER BY AttemptedAt DESC";
    List<UserQAPerformance> performances = new ArrayList<>();
    
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        
        ps.setInt(1, qaID);
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            UserQAPerformance perf = new UserQAPerformance();
            perf.setPerformanceID(rs.getInt("PerformanceID"));
            perf.setUserID(rs.getInt("UserID"));
            perf.setQaID(rs.getInt("QAID"));
            perf.setChapterID(rs.getInt("ChapterID"));
            perf.setCorrect(rs.getBoolean("IsCorrect"));
            perf.setTimeSpent(rs.getInt("TimeSpent"));
            perf.setAttemptedAt(rs.getTimestamp("AttemptedAt"));
            
            performances.add(perf);
        }
    }
    
    return performances;
}

public List<QA> getQAsByChapterID(int chapterID) throws SQLException {
    String sql = "SELECT * FROM QA WHERE ChapterID = ?";
    List<QA> qas = new ArrayList<>();
    
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        
        ps.setInt(1, chapterID);
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            QA qa = new QA();
            qa.setQAID(rs.getInt("QAID"));
            qa.setChapterID(rs.getInt("ChapterID"));
            qa.setQuestion(rs.getString("Question"));
            qa.setAnswer(rs.getString("Answer"));
            qa.setDifficulty(rs.getString("Difficulty"));
            qa.setQuestionType(rs.getString("QuestionType"));
            qa.setBloomLevel(rs.getString("BloomLevel"));
            
            qas.add(qa);
        }
    }
    
    return qas;
}
    public void updateQAMetadata(int qaId, String bloomLevel, String questionTypeTag) throws SQLException {
        String sql = "UPDATE [QA] SET [BloomLevel] = ?, [QuestionTypeTag] = ?, " +
                     "[AutoTagged] = 1, [UpdatedAt] = GETDATE() WHERE [QAID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, bloomLevel);
            ps.setString(2, questionTypeTag);
            ps.setInt(3, qaId);
            
            ps.executeUpdate();
            logger.fine("✅ Updated metadata for Q&A #" + qaId);
        }
    }
    public void markAsAutoTagged(int qaId) throws SQLException {
        String sql = "UPDATE [QA] SET [AutoTagged] = 1, [UpdatedAt] = GETDATE() WHERE [QAID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, qaId);
            ps.executeUpdate();
        }
    }
    
    public List<Integer> getUntaggedQAIds() throws SQLException {
        List<Integer> qaIds = new ArrayList<>();
        String sql = "SELECT [QAID] FROM [QA] WHERE [AutoTagged] = 0 ORDER BY [QAID]";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                qaIds.add(rs.getInt("QAID"));
            }
        }
        
        logger.info("📊 Found " + qaIds.size() + " untagged Q&As");
        return qaIds;
    }
    
    public Map<String, Integer> getTagStatistics() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT t.[TagName], COUNT(*) as [Count] " +
                     "FROM [Tags] t " +
                     "INNER JOIN [QATags] qt ON t.[TagID] = qt.[TagID] " +
                     "GROUP BY t.[TagName] " +
                     "ORDER BY [Count] DESC";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                stats.put(rs.getString("TagName"), rs.getInt("Count"));
            }
        }
        
        return stats;
    }
    public void saveVectorMetadata(int qaId, String embeddingModel, int dimension, String checksum) 
            throws SQLException {
        String sql = "INSERT INTO [VectorMetadata] ([QAID], [EmbeddingModel], [Dimension], [VectorChecksum]) " +
                     "VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, qaId);
            ps.setString(2, embeddingModel);
            ps.setInt(3, dimension);
            ps.setString(4, checksum);
            
            ps.executeUpdate();
            logger.fine("✅ Saved vector metadata for Q&A #" + qaId);
            
        } catch (SQLException e) {
            if (!e.getMessage().contains("UNIQUE")) {
                throw e;
            }
        }
    }
    
    public void markAsVectorIndexed(int qaId) throws SQLException {
        String sql = "UPDATE [QA] SET [VectorIndexed] = 1, [UpdatedAt] = GETDATE() WHERE [QAID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, qaId);
            ps.executeUpdate();
        }
    }
    
    public boolean isQAIndexed(int qaId) throws SQLException {
        String sql = "SELECT [VectorIndexed] FROM [QA] WHERE [QAID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, qaId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("VectorIndexed");
                }
            }
        }
        return false;
    }
    
    public List<Integer> getUnindexedQAIds() throws SQLException {
        List<Integer> qaIds = new ArrayList<>();
        String sql = "SELECT [QAID] FROM [QA] " +
                     "WHERE [VectorIndexed] = 0 AND [AutoTagged] = 1 " +
                     "ORDER BY [QAID]";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                qaIds.add(rs.getInt("QAID"));
            }
        }
        
        logger.info("📊 Found " + qaIds.size() + " unindexed Q&As");
        return qaIds;
    }
    
    /**
     * Get vector metadata for a Q&A
     */
    public VectorMetadata getVectorMetadata(int qaId) throws SQLException {
        String sql = "SELECT * FROM [VectorMetadata] WHERE [QAID] = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, qaId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    VectorMetadata meta = new VectorMetadata();
                    meta.setVectorID(rs.getInt("VectorID"));
                    meta.setQAID(rs.getInt("QAID"));
                    meta.setEmbeddingModel(rs.getString("EmbeddingModel"));
                    meta.setDimension(rs.getInt("Dimension"));
                    meta.setVectorChecksum(rs.getString("VectorChecksum"));
                    meta.setIndexedAt(rs.getTimestamp("IndexedAt"));
                    return meta;
                }
            }
        }
        return null;
    }
    
    public IndexStats getIndexingStats() throws SQLException {
        IndexStats stats = new IndexStats();
        
        String sql = "SELECT " +
                     "COUNT(*) as Total, " +
                     "SUM(CASE WHEN [AutoTagged] = 1 THEN 1 ELSE 0 END) as Tagged, " +
                     "SUM(CASE WHEN [VectorIndexed] = 1 THEN 1 ELSE 0 END) as Indexed " +
                     "FROM [QA]";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                stats.totalQAs = rs.getInt("Total");
                stats.autoTagged = rs.getInt("Tagged");
                stats.vectorIndexed = rs.getInt("Indexed");
            }
        }
        
        return stats;
    }
    

     
    public int insertQAWithMetadata(
        int chapterId,
        String question,
        String answer,
        String difficulty,
        String questionType,
        String bloomLevel,
        String questionTypeTag,
        List<String> topics,
        List<String> concepts
    ) throws SQLException {
        
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Insert Q&A
            int qaId = insertQA(chapterId, question, answer, difficulty, questionType);
            
            // 2. Update metadata
            updateQAMetadata(qaId, bloomLevel, questionTypeTag);
            
            // 3. Add topic tags
            if (topics != null) {
                for (String topic : topics) {
                    int tagId = getOrCreateTag(topic, "topic");
                    addTagToQA(qaId, tagId, 1.0f);
                }
            }
            
            // 4. Add concept tags
            if (concepts != null) {
                for (String concept : concepts) {
                    int tagId = getOrCreateTag(concept, "concept");
                    addTagToQA(qaId, tagId, 0.9f);
                }
            }
            
            // 5. Mark as auto-tagged
            markAsAutoTagged(qaId);
            
            conn.commit();
            logger.info("✅ Inserted Q&A #" + qaId + " with full metadata");
            
            return qaId;
            
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.severe("❌ Transaction rolled back");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, "Rollback failed", ex);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }
    
    
    public static class QAItem {
        private final String question;
        private final String answer;
        private final String difficulty;
        private final String questionType;
        
        public QAItem(String question, String answer, String difficulty, String questionType) {
            this.question = question;
            this.answer = answer;
            this.difficulty = difficulty != null ? difficulty.toLowerCase() : "medium";
            this.questionType = questionType != null ? questionType.toLowerCase() : "short";
        }
        
        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public String getDifficulty() { return difficulty; }
        public String getQuestionType() { return questionType; }
    }
    
    public static class Tag {
        private int tagID;
        private String tagName;
        private String tagType;
        private String description;
        private float confidence;
        
        // Getters and setters
        public int getTagID() { return tagID; }
        public void setTagID(int tagID) { this.tagID = tagID; }
        
        public String getTagName() { return tagName; }
        public void setTagName(String tagName) { this.tagName = tagName; }
        
        public String getTagType() { return tagType; }
        public void setTagType(String tagType) { this.tagType = tagType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public float getConfidence() { return confidence; }
        public void setConfidence(float confidence) { this.confidence = confidence; }
    }
    
    public static class VectorMetadata {
        private int vectorID;
        private int qaID;
        private String embeddingModel;
        private int dimension;
        private String vectorChecksum;
        private Timestamp indexedAt;
        
        // Getters and setters
        public int getVectorID() { return vectorID; }
        public void setVectorID(int vectorID) { this.vectorID = vectorID; }
        
        public int getQAID() { return qaID; }
        public void setQAID(int qaID) { this.qaID = qaID; }
        
        public String getEmbeddingModel() { return embeddingModel; }
        public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
        
        public int getDimension() { return dimension; }
        public void setDimension(int dimension) { this.dimension = dimension; }
        
        public String getVectorChecksum() { return vectorChecksum; }
        public void setVectorChecksum(String vectorChecksum) { this.vectorChecksum = vectorChecksum; }
        
        public Timestamp getIndexedAt() { return indexedAt; }
        public void setIndexedAt(Timestamp indexedAt) { this.indexedAt = indexedAt; }
    }
    
    public static class IndexStats {
        public int totalQAs;
        public int autoTagged;
        public int vectorIndexed;
        
        @Override
        public String toString() {
            return String.format("Total: %d, Tagged: %d (%.1f%%), Indexed: %d (%.1f%%)",
                totalQAs,
                autoTagged, (autoTagged * 100.0 / Math.max(totalQAs, 1)),
                vectorIndexed, (vectorIndexed * 100.0 / Math.max(totalQAs, 1)));
        }
    }
}