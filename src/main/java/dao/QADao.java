package dao;

import connect.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class QADao {

    public static class QAItem {
        private final String question;
        private final String answer;

        public QAItem(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
    }

    public int insertQABatch(int chapterId, List<QAItem> qas, String generatedBy) throws SQLException {
        if (qas == null || qas.isEmpty()) return 0;
        String sql = "INSERT INTO QA (ChapterID, Question, Answer, GeneratedBy) VALUES (?, ?, ?, ?)";
        int inserted = 0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (QAItem item : qas) {
                ps.setInt(1, chapterId);
                ps.setString(2, item.getQuestion());
                ps.setString(3, item.getAnswer());
                ps.setString(4, generatedBy);
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            conn.commit();

            for (int r : results) {
                
                if (r >= 0) inserted += r;
                else inserted += 1; 
            }
        }
        return inserted;
    }
}
