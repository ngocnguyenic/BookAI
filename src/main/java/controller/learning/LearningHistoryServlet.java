package controller.learning;

import com.google.gson.Gson;
import dao.AdaptiveLearningDAO;
import model.UserChapterMastery;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class LearningHistoryServlet extends HttpServlet {
    
    private static final Logger logger = Logger.getLogger(LearningHistoryServlet.class.getName());
    private AdaptiveLearningDAO adaptiveDAO;
    private Gson gson;
    
    @Override
    public void init() throws ServletException {
        adaptiveDAO = new AdaptiveLearningDAO();
        gson = new Gson();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String action = request.getParameter("action");
        Map<String, Object> result = new HashMap<>();
        
        try {
            HttpSession session = request.getSession(false);
            Integer userID = (session != null) ? (Integer) session.getAttribute("userID") : 1;
            
            if ("getChapterHistory".equals(action)) {
                String chapterIdStr = request.getParameter("chapterId");
                if (chapterIdStr == null) {
                    result.put("success", false);
                    result.put("error", "Missing chapterId");
                } else {
                    int chapterID = Integer.parseInt(chapterIdStr);
                    Map<String, Object> history = adaptiveDAO.getChapterHistory(userID, chapterID);
                    result.put("success", true);
                    result.put("data", history);
                }
            } else {
                logger.info("Getting learning history for user " + userID);
                
                List<UserChapterMastery> history = adaptiveDAO.getUserLearningHistory(userID);
                
                if (history == null || history.isEmpty()) {
                    result.put("success", true);
                    result.put("message", "Bạn chưa làm bài tập nào");
                    result.put("history", new Object[0]);
                } else {
                    result.put("success", true);
                    result.put("history", history);
                    result.put("totalChapters", history.size());
                    
                    int totalQuestions = 0;
                    int totalCorrect = 0;
                    for (UserChapterMastery mastery : history) {
                        totalQuestions += mastery.getTotalQuestions();
                        totalCorrect += mastery.getCorrectAnswers();
                    }
                    
                    result.put("totalQuestionsAnswered", totalQuestions);
                    result.put("totalCorrectAnswers", totalCorrect);
                    result.put("overallAccuracy", totalQuestions > 0 ? 
                        Math.round((totalCorrect * 100.0 / totalQuestions) * 10) / 10.0 : 0);
                    
                    logger.info("Found " + history.size() + " chapters with progress");
                }
            }
            
        } catch (Exception e) {
            logger.severe("Error getting learning history: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        response.getWriter().write(gson.toJson(result));
    }
}