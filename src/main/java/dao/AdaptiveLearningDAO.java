package dao;

import connect.DBConnection;
import model.UserQAPerformance;
import model.UserChapterMastery;
import model.QA;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import service.IRTService;

public class AdaptiveLearningDAO extends DBConnection {
    
    private static final Logger logger = Logger.getLogger(AdaptiveLearningDAO.class.getName());
    

   public boolean saveUserPerformanceWithAI(UserQAPerformance performance, 
                                         String understandingLevel,
                                         double aiScore,
                                         String aiFeedback) throws SQLException {
    String sql = "INSERT INTO UserQAPerformance " +
                 "(UserID, QAID, ChapterID, IsCorrect, TimeSpent, UnderstandingLevel, AIScore, AIFeedback) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        
        ps.setInt(1, performance.getUserID());
        ps.setInt(2, performance.getQaID());
        ps.setInt(3, performance.getChapterID());
        ps.setBoolean(4, performance.isCorrect());
        ps.setInt(5, performance.getTimeSpent());
        ps.setString(6, understandingLevel);
        ps.setDouble(7, aiScore);
        ps.setString(8, aiFeedback);
        
        int result = ps.executeUpdate();
        
        if (result > 0) {
            // Cập nhật mastery score (có tính AIScore)
            updateMasteryScoreWithAI(performance.getUserID(), performance.getChapterID());
            logger.info("✅ Saved performance with AI evaluation for User " + performance.getUserID());
            return true;
        }
        
        return false;
    }
}

private void updateMasteryScoreWithAI(int userID, int chapterID) throws SQLException {
    String sql = "MERGE UserChapterMastery AS target " +
                 "USING (SELECT ? AS UserID, ? AS ChapterID) AS source " +
                 "ON target.UserID = source.UserID AND target.ChapterID = source.ChapterID " +
                 "WHEN MATCHED THEN " +
                 "  UPDATE SET " +
                 "    TotalQuestions = (SELECT COUNT(*) FROM UserQAPerformance " +
                 "                      WHERE UserID = ? AND ChapterID = ?), " +
                 "    CorrectAnswers = (SELECT COUNT(*) FROM UserQAPerformance " +
                 "                      WHERE UserID = ? AND ChapterID = ? AND IsCorrect = 1), " +
                 "    MasteryScore = (" +
                 "      SELECT CASE " +
                 "        WHEN COUNT(*) > 0 THEN " +
                 "          (SUM(CASE WHEN IsCorrect = 1 THEN 60 ELSE 0 END) + SUM(ISNULL(AIScore, 0) * 0.4)) / COUNT(*) " +
                 "        ELSE 0 " +
                 "      END " +
                 "      FROM UserQAPerformance " +
                 "      WHERE UserID = ? AND ChapterID = ?" +
                 "    ), " +
                 "    LastUpdated = GETDATE() " +
                 "WHEN NOT MATCHED THEN " +
                 "  INSERT (UserID, ChapterID, TotalQuestions, CorrectAnswers, MasteryScore) " +
                 "  VALUES (?, ?, " +
                 "          (SELECT COUNT(*) FROM UserQAPerformance WHERE UserID = ? AND ChapterID = ?), " +
                 "          (SELECT COUNT(*) FROM UserQAPerformance WHERE UserID = ? AND ChapterID = ? AND IsCorrect = 1), " +
                 "          (SELECT CASE WHEN COUNT(*) > 0 THEN " +
                 "            (SUM(CASE WHEN IsCorrect = 1 THEN 60 ELSE 0 END) + SUM(ISNULL(AIScore, 0) * 0.4)) / COUNT(*) " +
                 "           ELSE 0 END FROM UserQAPerformance WHERE UserID = ? AND ChapterID = ?));";
    
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        
        ps.setInt(1, userID);
        ps.setInt(2, chapterID);
        ps.setInt(3, userID);
        ps.setInt(4, chapterID);
        ps.setInt(5, userID);
        ps.setInt(6, chapterID);
        ps.setInt(7, userID);
        ps.setInt(8, chapterID);
        ps.setInt(9, userID);
        ps.setInt(10, chapterID);
        ps.setInt(11, userID);
        ps.setInt(12, chapterID);
        ps.setInt(13, userID);
        ps.setInt(14, chapterID);
        ps.setInt(15, userID);
        ps.setInt(16, chapterID);
        
        ps.executeUpdate();
        logger.info("✅ Updated mastery score with AI evaluation for User " + userID);
    }
}
    
    /**
     * @param userID
     * @param chapterID
     * @return 
     * @throws java.sql.SQLException
     */
    public UserChapterMastery getMasteryScore(int userID, int chapterID) throws SQLException {
        String sql = "SELECT * FROM UserChapterMastery WHERE UserID = ? AND ChapterID = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userID);
            ps.setInt(2, chapterID);
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                UserChapterMastery mastery = new UserChapterMastery();
                mastery.setMasteryID(rs.getInt("MasteryID"));
                mastery.setUserID(rs.getInt("UserID"));
                mastery.setChapterID(rs.getInt("ChapterID"));
                mastery.setTotalQuestions(rs.getInt("TotalQuestions"));
                mastery.setCorrectAnswers(rs.getInt("CorrectAnswers"));
                mastery.setMasteryScore(rs.getDouble("MasteryScore"));
                mastery.setLastUpdated(rs.getTimestamp("LastUpdated"));
                return mastery;
            }
            
            return null;
        }
    }
    
   
    public QA getAdaptiveQuestion(int userID, int chapterID) throws SQLException {
        UserChapterMastery mastery = getMasteryScore(userID, chapterID);
        double masteryScore = (mastery != null) ? mastery.getMasteryScore() : 0.0;
        
      
        String difficulty;
        if (masteryScore < 40) {
            difficulty = "easy";
        } else if (masteryScore < 70) {
            difficulty = "medium";
        } else {
            difficulty = "hard";
        }
        
        logger.info("🎯 User " + userID + " mastery: " + masteryScore + "% → difficulty: " + difficulty);
        String sql = "SELECT TOP 1 q.* " +
                     "FROM QA q " +
                     "LEFT JOIN UserQAPerformance p ON q.QAID = p.QAID AND p.UserID = ? " +
                     "WHERE q.ChapterID = ? " +
                     "AND q.Difficulty = ? " +
                     "AND (p.PerformanceID IS NULL OR p.IsCorrect = 0) " +
                     "ORDER BY NEWID()"; // Random
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userID);
            ps.setInt(2, chapterID);
            ps.setString(3, difficulty);
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                QA qa = new QA();
                qa.setQAID(rs.getInt("QAID"));
                qa.setChapterID(rs.getInt("ChapterID"));
                qa.setQuestion(rs.getString("Question"));
                qa.setAnswer(rs.getString("Answer"));
                qa.setDifficulty(rs.getString("Difficulty"));
                qa.setQuestionType(rs.getString("QuestionType"));
                qa.setBloomLevel(rs.getString("BloomLevel"));
                
                logger.info("✅ Selected Q&A #" + qa.getQAID() + " (difficulty: " + difficulty + ")");
                return qa;
            }
            
            logger.info("⚠️ No unused questions, getting random one");
            return getRandomQuestion(chapterID, difficulty);
        }
    }
    

    private QA getRandomQuestion(int chapterID, String difficulty) throws SQLException {
        String sql = "SELECT TOP 1 * FROM QA " +
                     "WHERE ChapterID = ? AND Difficulty = ? " +
                     "ORDER BY NEWID()";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, chapterID);
            ps.setString(2, difficulty);
            
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                QA qa = new QA();
                qa.setQAID(rs.getInt("QAID"));
                qa.setChapterID(rs.getInt("ChapterID"));
                qa.setQuestion(rs.getString("Question"));
                qa.setAnswer(rs.getString("Answer"));
                qa.setDifficulty(rs.getString("Difficulty"));
                qa.setQuestionType(rs.getString("QuestionType"));
                
                return qa;
            }
            
            return null;
        }
    }
    
    
    // Thêm vào AdaptiveLearningDAO.java

public QA getAdaptiveQuestionWithIRT(int userID, int chapterID) throws SQLException {
    IRTService irtService = new IRTService();
    
    try {
        QA optimalQuestion = irtService.selectOptimalQuestion(userID, chapterID);        
        if (optimalQuestion != null) {
            logger.info("IRT selected optimal question for user " + userID);
            return optimalQuestion;
        }
        logger.warning("IRT failed, using fallback method");
        return getAdaptiveQuestion(userID, chapterID);
        
    } catch (SQLException e) {
        logger.severe("IRT error: " + e.getMessage());
        return getAdaptiveQuestion(userID, chapterID);
    }
}
    
    

    public List<UserQAPerformance> getUserHistory(int userID, int chapterID) throws SQLException {
        String sql = "SELECT * FROM UserQAPerformance " +
                     "WHERE UserID = ? AND ChapterID = ? " +
                     "ORDER BY AttemptedAt DESC";
        
        List<UserQAPerformance> history = new ArrayList<>();
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userID);
            ps.setInt(2, chapterID);
            
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
                
                history.add(perf);
            }
        }
        
        return history;
    }
}