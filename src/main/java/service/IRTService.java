package service;

import dao.AdaptiveLearningDAO;
import dao.QADao;
import model.QA;
import model.UserQAPerformance;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class IRTService {
    
    private static final Logger logger = Logger.getLogger(IRTService.class.getName());
    
    private AdaptiveLearningDAO adaptiveDAO;
    private QADao qaDao;
    
    private static final double INITIAL_THETA = 0.0; 
    private static final double LEARNING_RATE = 0.3;  
    
    public IRTService() {
        this.adaptiveDAO = new AdaptiveLearningDAO();
        this.qaDao = new QADao();
    }
    
    public double calculateProbability(double theta, double beta) {
        double exponent = theta - beta;
        return Math.exp(exponent) / (1.0 + Math.exp(exponent));
    }
    

    public double estimateUserAbility(int userID, int chapterID) throws SQLException {
        List<UserQAPerformance> history = adaptiveDAO.getUserHistory(userID, chapterID);
        
        if (history.isEmpty()) {
            logger.info("User " + userID + " has no history, using initial theta: " + INITIAL_THETA);
            return INITIAL_THETA;
        }
        
        
        double theta = INITIAL_THETA;
        
        int totalAttempts = history.size();
        int correctAnswers = 0;
        
        for (UserQAPerformance perf : history) {
            if (perf.isCorrect()) {
                correctAnswers++;
            }
        }
        
        double correctRate = (double) correctAnswers / totalAttempts;

        if (correctRate == 0.0) {
            theta = -2.0; // 
        } else if (correctRate == 1.0) {
            theta = 2.0; 
        } else {
            theta = Math.log(correctRate / (1.0 - correctRate));
        }
        
        logger.info("User " + userID + " estimated ability (theta): " + 
                   String.format("%.2f", theta) + " (correct rate: " + 
                   String.format("%.1f%%", correctRate * 100) + ")");
        
        return theta;
    }

    public double estimateItemDifficulty(String difficulty) {
        switch (difficulty.toLowerCase()) {
            case "easy":
                return -1.0; 
            case "medium":
                return 0.0;  
            case "hard":
                return 1.0;  
            default:
                return 0.0;
        }
    }
    

    public double estimateItemDifficultyFromData(int qaID) throws SQLException {

        List<UserQAPerformance> allAttempts = qaDao.getAllPerformanceForQA(qaID);
        
        if (allAttempts.isEmpty()) {
 
            QA qa = qaDao.getQAById(qaID);
            return estimateItemDifficulty(qa.getDifficulty());
        }

        int totalAttempts = allAttempts.size();
        int correctCount = 0;
        
        for (UserQAPerformance perf : allAttempts) {
            if (perf.isCorrect()) {
                correctCount++;
            }
        }
        
        double correctRate = (double) correctCount / totalAttempts;
        
        
        double beta;
        if (correctRate >= 0.95) {
            beta = -2.0;
        } else if (correctRate <= 0.05) {
            beta = 2.0;
        } else {
            beta = -Math.log(correctRate / (1.0 - correctRate));
        }
        
        logger.info("QA #" + qaID + " estimated difficulty (beta): " + 
                   String.format("%.2f", beta) + " (correct rate: " + 
                   String.format("%.1f%%", correctRate * 100) + ")");
        
        return beta;
    }

    public QA selectOptimalQuestion(int userID, int chapterID) throws SQLException {
        
        double theta = estimateUserAbility(userID, chapterID);

        List<QA> allQuestions = qaDao.getQAsByChapterID(chapterID);
        
        if (allQuestions.isEmpty()) {
            logger.warning("No questions found for chapter " + chapterID);
            return null;
        }

        QA optimalQuestion = null;
        double minDistance = Double.MAX_VALUE;
        
        for (QA qa : allQuestions) {
            double beta = estimateItemDifficulty(qa.getDifficulty());

            double probability = calculateProbability(theta, beta);

            double distance = Math.abs(probability - 0.5);
            
            if (distance < minDistance) {
                minDistance = distance;
                optimalQuestion = qa;
            }
            
            logger.fine("QA #" + qa.getQAID() + " (beta=" + beta + 
                       "): P(correct)=" + String.format("%.2f", probability));
        }
        
        if (optimalQuestion != null) {
            double optimalBeta = estimateItemDifficulty(optimalQuestion.getDifficulty());
            double optimalProb = calculateProbability(theta, optimalBeta);
            
            logger.info("Selected optimal question:");
            logger.info("  QA #" + optimalQuestion.getQAID());
            logger.info("  Difficulty: " + optimalQuestion.getDifficulty());
            logger.info("  Beta: " + String.format("%.2f", optimalBeta));
            logger.info("  P(correct): " + String.format("%.2f", optimalProb));
            logger.info("  User theta: " + String.format("%.2f", theta));
        }
        
        return optimalQuestion;
    }
    
    
    public double updateUserAbility(double currentTheta, double itemBeta, 
                                    boolean isCorrect) {
        
        double predictedProb = calculateProbability(currentTheta, itemBeta);
        
        
        double observed = isCorrect ? 1.0 : 0.0;
        double error = observed - predictedProb;
        
        double newTheta = currentTheta + LEARNING_RATE * error;
        
        logger.log(Level.INFO, "Theta update: {0} -> {1} (error: {2})", new Object[]{String.format("%.2f", currentTheta), String.format("%.2f", newTheta), String.format("%.2f", error)});
        
        return newTheta;
    }
    

    public double calculateInformation(double theta, double beta) {
        double prob = calculateProbability(theta, beta);
        return prob * (1.0 - prob);
    }
    
    
    public boolean hasAchievedMastery(double theta, double masteryThreshold) {
        return theta >= masteryThreshold;
    }
}