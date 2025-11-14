package service;

import dao.QADao;
import org.junit.jupiter.api.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OllamaServiceTest {
    
    private OllamaService service;
    private static final List<TestResult> testResults = new ArrayList<>();
    
    private static final String TEST_CHAPTER = 
        "Machine Learning là nhánh của Trí tuệ nhân tạo (AI), cho phép máy tính " +
        "học từ dữ liệu mà không cần lập trình tường minh. Có 3 loại chính:\n" +
        "1. Supervised Learning: Học có giám sát với dữ liệu có nhãn\n" +
        "2. Unsupervised Learning: Học không giám sát, tự phát hiện pattern\n" +
        "3. Reinforcement Learning: Học qua thưởng phạt trong môi trường";
    
    @BeforeAll
    static void globalSetup() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("OLLAMA SERVICE TEST");
        System.out.println("=".repeat(70));
        System.out.println("Start time: " + LocalDateTime.now());
        System.out.println();
    }
    
    @BeforeEach
    void setUp() {
        service = new OllamaService();
    }
    
    @Test
    @Order(1)
    @DisplayName("Test 1: Generate Chapter Summary")
    void testGenerateSummary() {
        long start = System.currentTimeMillis();
        try {
            System.out.println("\nTest 1: Generate Chapter Summary...");
            
            String summary = service.generateChapterSummary(
                "Chương 1: Machine Learning Cơ bản", 
                TEST_CHAPTER
            );
            
            assertNotNull(summary, "Summary null");
            assertFalse(summary.trim().isEmpty(), "Summary rỗng");
            assertTrue(summary.length() >= 50, "Summary quá ngắn: " + summary.length() + " chars");
            
            long time = System.currentTimeMillis() - start;
            System.out.println("Summary length: " + summary.length() + " chars");
            System.out.println("Preview: " + truncate(summary, 100));
            System.out.println("Time: " + time + "ms");
            
            recordResult("Generate Summary", "PASSED", time, 
                "Length: " + summary.length() + " chars", null);
            
        } catch (IOException | InterruptedException e) {
            long time = System.currentTimeMillis() - start;
            System.out.println("Failed: " + e.getMessage());
            recordResult("Generate Summary", "FAILED", time, null, e.getMessage());
            fail(e.getMessage());
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("Test 2: Generate Multiple Choice Questions")
    void testGenerateMCQ() {
        long start = System.currentTimeMillis();
        try {
            System.out.println("\nTest 2: Generate MCQ...");
            
            String summary = "Machine Learning gồm 3 loại: supervised (có nhãn), " +
                "unsupervised (không nhãn), reinforcement (thưởng phạt).";
            
            int numQuestions = 3;
            List<QADao.QAItem> mcqs = null;
            
            
            int maxRetries = 3;
            Exception lastError = null;
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    mcqs = service.generateMCQ(summary, numQuestions);
                    break; 
                } catch (Exception e) {
                    lastError = e;
                    if (e.getMessage().contains("Invalid JSON") && attempt < maxRetries) {
                        System.out.println("Attempt " + attempt + " failed, retrying...");
                        Thread.sleep(2000); 
                    } else {
                        throw e;
                    }
                }
            }
            
            if (mcqs == null) {
                throw lastError != null ? lastError : new Exception("Failed to generate MCQs");
            }
            
            assertNotNull(mcqs, "MCQs null");
            assertFalse(mcqs.isEmpty(), "MCQs rỗng");
            
            Map<String, Integer> difficultyCount = new HashMap<>();
            
            for (int i = 0; i < mcqs.size(); i++) {
                QADao.QAItem mcq = mcqs.get(i);
                String question = mcq.getQuestion();
                String answer = mcq.getAnswer();
                String difficulty = mcq.getDifficulty();
                
                assertNotNull(question, "Question " + (i+1) + " null");
                assertTrue(question.contains("A."), "Q" + (i+1) + " thiếu đáp án A");
                assertTrue(question.contains("B."), "Q" + (i+1) + " thiếu đáp án B");
                assertTrue(question.contains("C."), "Q" + (i+1) + " thiếu đáp án C");
                assertTrue(question.contains("D."), "Q" + (i+1) + " thiếu đáp án D");
                assertNotNull(answer, "Answer " + (i+1) + " null");
                
                difficultyCount.merge(difficulty, 1, Integer::sum);
                
                System.out.println("  Q" + (i+1) + ": [" + difficulty + "] " + 
                    truncate(question.split("\n")[0], 60));
            }
            
            long time = System.currentTimeMillis() - start;
            System.out.println("Difficulty distribution: " + difficultyCount);
            System.out.println("Total MCQs: " + mcqs.size());
            System.out.println("Time: " + time + "ms");
            
            recordResult("Generate MCQ", "PASSED", time, 
                "Generated " + mcqs.size() + " MCQs", null);
            
        } catch (Exception e) {
            long time = System.currentTimeMillis() - start;
            System.out.println("Failed: " + e.getMessage());
            recordResult("Generate MCQ", "FAILED", time, null, e.getMessage());
            fail(e.getMessage());
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("Test 3: Evaluate User Answer")
    void testEvaluateAnswer() {
        long start = System.currentTimeMillis();
        try {
            System.out.println("\nTest 3: Evaluate User Answer...");
            
            String question = "Machine Learning là gì?";
            String correctAnswer = "ML là nhánh của AI cho phép máy tính học từ dữ liệu";
            String userAnswer = "Machine Learning là lĩnh vực của trí tuệ nhân tạo " +
                "giúp máy tính tự động học từ dữ liệu.";
            
            OllamaService.AnswerEvaluationResult result = 
                service.evaluateUserAnswer(question, correctAnswer, userAnswer, "medium");
            
            assertNotNull(result, "Result null");
            assertNotNull(result.feedback, "Feedback null");
            assertTrue(result.score >= 0 && result.score <= 100, 
                "Score ngoài range 0-100: " + result.score);
            
            long time = System.currentTimeMillis() - start;
            System.out.println("Score: " + String.format("%.1f", result.score) + "/100");
            System.out.println("Level: " + result.level);
            System.out.println("Correct: " + result.isCorrect);
            System.out.println("Feedback: " + truncate(result.feedback, 80));
            System.out.println("Time: " + time + "ms");
            
            recordResult("Evaluate Answer", "PASSED", time, 
                "Score: " + result.score + ", Level: " + result.level, null);
            
        } catch (Exception e) {
            long time = System.currentTimeMillis() - start;
            System.out.println("Failed: " + e.getMessage());
            recordResult("Evaluate Answer", "FAILED", time, null, e.getMessage());
            fail(e.getMessage());
        }
    }
    
    @AfterAll
    static void exportResults() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("EXPORTING TEST RESULTS");
        System.out.println("=".repeat(70));
        
        try {
            String csvFile = exportToCSV(testResults);
            System.out.println("CSV exported: " + csvFile);
            
            printSummary(testResults);
            
        } catch (IOException e) {
            System.err.println("Export failed: " + e.getMessage());
        }
    }
    
    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
    
    private static void recordResult(String testName, String status, long time, 
                                     String notes, String error) {
        TestResult result = new TestResult();
        result.testName = testName;
        result.status = status;
        result.executionTime = time;
        result.notes = notes != null ? notes : "";
        result.error = error != null ? error : "";
        result.timestamp = LocalDateTime.now();
        testResults.add(result);
    }
    
    private static String exportToCSV(List<TestResult> results) throws IOException {
        String filename = "ollama-test-results.csv";
        File file = new File(filename);
        boolean fileExists = file.exists();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(file, true), "UTF-8"))) {
            
            if (!fileExists) {
                writer.write('\ufeff');
                writer.println("Test Name,Status,Execution Time (ms),Notes,Error,Timestamp");
            }

            for (TestResult r : results) {
                writer.printf("%s,%s,%d,\"%s\",\"%s\",%s%n",
                    r.testName, r.status, r.executionTime,
                    escapeCSV(r.notes), escapeCSV(r.error),
                    r.timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
            }
        }
        
        return file.getAbsolutePath();
    }
    
    private static void printSummary(List<TestResult> results) {
        long totalPassed = results.stream().filter(r -> r.status.equals("PASSED")).count();
        long totalFailed = results.stream().filter(r -> r.status.equals("FAILED")).count();
        long totalTime = results.stream().mapToLong(r -> r.executionTime).sum();
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST SUMMARY");
        System.out.println("=".repeat(70));
        System.out.println("Total Tests: " + results.size());
        System.out.println("Passed: " + totalPassed);
        System.out.println("Failed: " + totalFailed);
        System.out.println("Total Time: " + totalTime + "ms");
        System.out.println("Avg Time: " + (totalTime / results.size()) + "ms");
                if (totalFailed > 0) {
            System.out.println("\nFAILED TESTS:");
            results.stream()
                .filter(r -> r.status.equals("FAILED"))
                .forEach(r -> System.out.println("  - " + r.testName + ": " + 
                    truncate(r.error, 60)));
        }
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("End time: " + LocalDateTime.now());
        System.out.println("=".repeat(70));
    }
    
    private static String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
    
    static class TestResult {
        String testName;
        String status;
        long executionTime;
        String notes;
        String error;
        LocalDateTime timestamp;
    }
}