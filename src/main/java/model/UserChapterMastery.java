package model;

import java.sql.Timestamp;

public class UserChapterMastery {
    private int masteryID;
    private int userID;
    private int chapterID;
    private int totalQuestions;
    private int correctAnswers;
    private double masteryScore; 
    private Timestamp lastUpdated;
    
    public UserChapterMastery() {}
    
    public UserChapterMastery(int userID, int chapterID, int totalQuestions, 
                             int correctAnswers, double masteryScore) {
        this.userID = userID;
        this.chapterID = chapterID;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.masteryScore = masteryScore;
    }
    
    
    public int getMasteryID() { return masteryID; }
    public void setMasteryID(int masteryID) { this.masteryID = masteryID; }
    
    public int getUserID() { return userID; }
    public void setUserID(int userID) { this.userID = userID; }
    
    public int getChapterID() { return chapterID; }
    public void setChapterID(int chapterID) { this.chapterID = chapterID; }
    
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    
    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    
    public double getMasteryScore() { return masteryScore; }
    public void setMasteryScore(double masteryScore) { this.masteryScore = masteryScore; }
    
    public Timestamp getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }
}