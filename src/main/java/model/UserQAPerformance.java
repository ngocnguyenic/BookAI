package model;

import java.sql.Timestamp;

public class UserQAPerformance {
    private int performanceID;
    private int userID;
    private int qaID;
    private int chapterID;
    private boolean isCorrect;
    private int timeSpent; 
    private Timestamp attemptedAt;
    

    public UserQAPerformance() {}
    
    public UserQAPerformance(int userID, int qaID, int chapterID, boolean isCorrect, int timeSpent) {
        this.userID = userID;
        this.qaID = qaID;
        this.chapterID = chapterID;
        this.isCorrect = isCorrect;
        this.timeSpent = timeSpent;
    }

    public int getPerformanceID() { return performanceID; }
    public void setPerformanceID(int performanceID) { this.performanceID = performanceID; }
    
    public int getUserID() { return userID; }
    public void setUserID(int userID) { this.userID = userID; }
    
    public int getQaID() { return qaID; }
    public void setQaID(int qaID) { this.qaID = qaID; }
    
    public int getChapterID() { return chapterID; }
    public void setChapterID(int chapterID) { this.chapterID = chapterID; }
    
    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
    
    public int getTimeSpent() { return timeSpent; }
    public void setTimeSpent(int timeSpent) { this.timeSpent = timeSpent; }
    
    public Timestamp getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(Timestamp attemptedAt) { this.attemptedAt = attemptedAt; }
}
