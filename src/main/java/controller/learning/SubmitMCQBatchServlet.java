// controller/learning/SubmitMCQBatchServlet.java
package controller.learning;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dao.AdaptiveLearningDAO;
import model.UserQAPerformance;
import service.OllamaService;
import service.IRTService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SubmitMCQBatchServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(SubmitMCQBatchServlet.class.getName());
    
    private AdaptiveLearningDAO adaptiveDAO;
    private OllamaService ollamaService;
    private IRTService irtService;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        adaptiveDAO = new AdaptiveLearningDAO();
        ollamaService = new OllamaService();
        irtService = new IRTService();
        gson = new Gson();
        logger.info("SubmitMCQBatchServlet initialized with IRT support");
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get user from session
            HttpSession session = request.getSession(false);
            Integer userID = (session != null) ? (Integer) session.getAttribute("userID") : null;
            if (userID == null) {
                userID = 1; 
                logger.warning("No user session, using demo user ID: 1");
            }

            // Parse JSON request
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JsonObject requestData = gson.fromJson(sb.toString(), JsonObject.class);
            int chapterId = requestData.get("chapterId").getAsInt();
            JsonArray answersArray = requestData.getAsJsonArray("answers");
            
            logger.info("Processing " + answersArray.size() + " MCQ answers for user " + userID);
            
            // Ước lượng khả năng ban đầu của user (theta)
            double currentTheta = irtService.estimateUserAbility(userID, chapterId);
            logger.info("User " + userID + " initial theta: " + String.format("%.2f", currentTheta));
            
            List<Map<String, Object>> evaluations = new ArrayList<>();
            int totalCorrect = 0;
            double totalAIScore = 0;
            
            // Process each answer
            for (int i = 0; i < answersArray.size(); i++) {
                JsonObject answerObj = answersArray.get(i).getAsJsonObject();
                
                int qaId = answerObj.get("qaId").getAsInt();
                String question = answerObj.get("question").getAsString();
                String correctAnswer = answerObj.get("correctAnswer").getAsString();
                String userAnswer = answerObj.get("userAnswer").getAsString();
                String difficulty = answerObj.get("difficulty").getAsString();
                
                logger.info("Q" + (i+1) + ": QA #" + qaId + " (difficulty: " + difficulty + ")");
                
                boolean isCorrect = false;
                String feedback = "";
                double score = 0;
                
                // Check answer
                if (userAnswer.isEmpty()) {
                    feedback = "Bạn chưa chọn đáp án nào.";
                    score = 0;
                } else {
                    char userLetter = userAnswer.trim().charAt(0);
                    char correctLetter = correctAnswer.trim().charAt(0);
                    isCorrect = (userLetter == correctLetter);
                    
                    if (isCorrect) {
                        try {
                            feedback = generatePositiveFeedback(question, correctAnswer, difficulty);
                            score = 100;
                        } catch (Exception e) {
                            feedback = "Chính xác! Bạn đã nắm vững kiến thức này.";
                            score = 100;
                        }
                    } else {
                        try {
                            feedback = generateExplanation(question, correctAnswer, userAnswer, difficulty);
                            score = 30; 
                        } catch (Exception e) {
                            feedback = "Chưa chính xác. Hãy xem lại đáp án đúng và giải thích bên dưới.";
                            score = 30;
                        }
                    }
                }

                // Save performance
                UserQAPerformance performance = new UserQAPerformance();
                performance.setUserID(userID);
                performance.setQaID(qaId);
                performance.setChapterID(chapterId);
                performance.setCorrect(isCorrect);
                performance.setTimeSpent(0); 
                
                String level = score >= 85 ? "excellent" : score >= 60 ? "good" : score >= 40 ? "basic" : "poor";
                
                adaptiveDAO.saveUserPerformanceWithAI(performance, level, score, feedback);
                
                // IRT: Update user ability (theta) after each response
                try {
                    double itemBeta = irtService.estimateItemDifficulty(difficulty);
                    double predictedProb = irtService.calculateProbability(currentTheta, itemBeta);
                    
                    logger.info("  IRT: beta=" + String.format("%.2f", itemBeta) + 
                               ", P(correct)=" + String.format("%.2f", predictedProb));
                    
                    // Update theta based on response
                    currentTheta = irtService.updateUserAbility(currentTheta, itemBeta, isCorrect);
                    
                    logger.info("  Updated theta: " + String.format("%.2f", currentTheta));
                    
                } catch (Exception e) {
                    logger.warning("IRT update failed: " + e.getMessage());
                }
                
                // Add to evaluations
                Map<String, Object> evaluation = new HashMap<>();
                evaluation.put("qaId", qaId);
                evaluation.put("isCorrect", isCorrect);
                evaluation.put("score", score);
                evaluation.put("feedback", feedback);
                evaluations.add(evaluation);
                
                if (isCorrect) totalCorrect++;
                totalAIScore += score;
            }
            
            // Get final mastery score
            var mastery = adaptiveDAO.getMasteryScore(userID, chapterId);
            double masteryScore = (mastery != null) ? mastery.getMasteryScore() : 0;
            
            // IRT: Check if user achieved mastery
            boolean hasAchievedMastery = irtService.hasAchievedMastery(currentTheta, 1.0);
            
            logger.info("Evaluation complete:");
            logger.info("  Correct: " + totalCorrect + "/" + answersArray.size());
            logger.info("  Mastery Score: " + String.format("%.1f%%", masteryScore));
            logger.info("  Final Theta: " + String.format("%.2f", currentTheta));
            logger.info("  Has Mastery: " + hasAchievedMastery);
            
            // Return results
            result.put("success", true);
            result.put("evaluations", evaluations);
            result.put("masteryScore", masteryScore);
            result.put("totalCorrect", totalCorrect);
            result.put("totalQuestions", answersArray.size());
            result.put("userTheta", currentTheta);
            result.put("hasAchievedMastery", hasAchievedMastery);
            
        } catch (Exception e) {
            logger.severe("Error: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", "Server error: " + e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
    
    /**
     * Generate positive feedback for correct answers
     */
    private String generatePositiveFeedback(String question, String answer, String difficulty) 
            throws Exception {
        
        String prompt = "Câu hỏi: " + question + "\n" +
                       "Đáp án đúng: " + answer + "\n" +
                       "Độ khó: " + difficulty + "\n\n" +
                       "Sinh viên đã trả lời ĐÚNG. Hãy cho 1 câu nhận xét ngắn gọn (tối đa 20 từ), " +
                       "khuyến khích và giải thích vì sao đáp án này đúng. " +
                       "Viết bằng tiếng Việt, thân thiện.";
        
        return ollamaService.answerQuestion(prompt, "");
    }
    
    /**
     * Generate explanation for wrong answers
     */
    private String generateExplanation(String question, String correctAnswer, 
                                      String userAnswer, String difficulty) throws Exception {
        
        String prompt = "Câu hỏi: " + question + "\n" +
                       "Đáp án đúng: " + correctAnswer + "\n" +
                       "Sinh viên chọn: " + userAnswer + "\n" +
                       "Độ khó: " + difficulty + "\n\n" +
                       "Sinh viên đã trả lời SAI. Hãy giải thích ngắn gọn (tối đa 30 từ) " +
                       "vì sao đáp án đúng là đúng và đáp án sinh viên chọn là sai. " +
                       "Viết bằng tiếng Việt, thân thiện, giúp sinh viên hiểu.";
        
        return ollamaService.answerQuestion(prompt, "");
    }
}