package controller.learning;

import com.google.gson.Gson;
import dao.AdaptiveLearningDAO;
import model.QA;
import model.UserChapterMastery;
import service.IRTService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AdaptiveQuizServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(AdaptiveQuizServlet.class.getName());
    private AdaptiveLearningDAO adaptiveDAO;
    private IRTService irtService;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        adaptiveDAO = new AdaptiveLearningDAO();
        irtService = new IRTService();
        gson = new Gson();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get user from session
            HttpSession session = request.getSession(false);
            Integer userID = (session != null) ? (Integer) session.getAttribute("userID") : 1;
            
            String chapterIdStr = request.getParameter("chapterId");
            if (chapterIdStr == null) {
                result.put("success", false);
                result.put("error", "Missing chapterId");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            int chapterID = Integer.parseInt(chapterIdStr);
            
            logger.info("Getting adaptive question for user " + userID + ", chapter " + chapterID);
            
            // Get mastery info
            UserChapterMastery mastery = adaptiveDAO.getMasteryScore(userID, chapterID);
            double masteryScore = (mastery != null) ? mastery.getMasteryScore() : 0.0;
            
            // Get user ability (theta)
            double theta = irtService.estimateUserAbility(userID, chapterID);
            
            // Select optimal question using IRT
            QA question = irtService.selectOptimalQuestion(userID, chapterID);
            
            if (question == null) {
                result.put("success", false);
                result.put("error", "No suitable questions found");
                response.getWriter().write(gson.toJson(result));
                return;
            }
            
            // Calculate predicted probability
            double beta = irtService.estimateItemDifficulty(question.getDifficulty());
            double predictedProb = irtService.calculateProbability(theta, beta);
            
            result.put("success", true);
            result.put("question", question);
            result.put("masteryScore", masteryScore);
            result.put("userTheta", theta);
            result.put("itemBeta", beta);
            result.put("predictedProbability", predictedProb);
            result.put("totalQuestions", mastery != null ? mastery.getTotalQuestions() : 0);
            result.put("correctAnswers", mastery != null ? mastery.getCorrectAnswers() : 0);
            
            logger.info("Selected question: QA #" + question.getQAID() + 
                       " (difficulty: " + question.getDifficulty() + 
                       ", beta: " + String.format("%.2f", beta) + 
                       ", P(correct): " + String.format("%.2f", predictedProb) + ")");
            
        } catch (Exception e) {
            logger.severe("Error: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
}