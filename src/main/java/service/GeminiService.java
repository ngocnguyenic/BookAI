package service;

import config.ConfigAPIKey;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import javax.net.ssl.HttpsURLConnection;

public class GeminiService {

    private final String apiUrl = ConfigAPIKey.getProperty("gemini.base.url");
    private final String apiKey = ConfigAPIKey.getProperty("gemini.api.key");

    
    public String generateChapterSummary(String chapterTitle, String chapterContent) throws IOException {
        String urlStr = apiUrl + "?key=" + apiKey;
        URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

        String prompt = "Summarize the following book chapter in a concise and clear way for students:\n\n"
                + "Chapter Title: " + escapeJson(chapterTitle) + "\n\n"
                + "Content: " + escapeJson(chapterContent);

        String requestBody = "{ \"contents\": [ { \"parts\": [ { \"text\": \"" + escapeJson(prompt) + "\" } ] } ] }";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("Gemini API error: " + code);
        }

        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
        }

        return response.toString(); 
    }

    public String generateQAPairs(String chapterTitle, String chapterContent) throws IOException {
        String urlStr = apiUrl + "?key=" + apiKey;
        URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

        String prompt = "Generate 3 study questions and their answers based on the following chapter. "
                + "Return as JSON array with fields 'question' and 'answer'.\n\n"
                + "Chapter Title: " + escapeJson(chapterTitle) + "\n\n"
                + "Content: " + escapeJson(chapterContent);

        String requestBody = "{ \"contents\": [ { \"parts\": [ { \"text\": \"" + escapeJson(prompt) + "\" } ] } ] }";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("Gemini API error: " + code);
        }

        StringBuilder response = new StringBuilder();
        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
        }

        return response.toString();
    }


    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
