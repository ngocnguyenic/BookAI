package service;

import config.ConfigAPIKey;
import org.json.JSONArray;
import org.json.JSONObject;

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
        String prompt = "Summarize the following book chapter in a concise and clear way for students:\n\n"
                + "Chapter Title: " + chapterTitle + "\n\n"
                + "Content: " + chapterContent;

        return callGemini(prompt);
    }

    public String generateQAPairs(String chapterTitle, String chapterContent) throws IOException {
        String prompt = "Generate 3 study questions and their answers based on the following chapter. "
                + "Return as JSON array with fields 'question' and 'answer'.\n\n"
                + "Chapter Title: " + chapterTitle + "\n\n"
                + "Content: " + chapterContent;

        return callGemini(prompt);
    }

    public String detectChaptersWithAI(String fullBookText) throws IOException {
        String prompt = """
ou are an expert in analyzing the structure of academic books. Read the BOOK_CONTENT below and split it into a logical sequence of chapters.

Rules:
1) Ignore front matter: Preface, Foreword, Introduction, Table of Contents, and Appendices — unless they contain substantive instructional content. Only include these if they add meaningful content.
2) For each chapter produce an object with:
   - "chapterNumber": integer (start at 1 and increment by 1)
   - "title": short title (<= 50 characters)
   - "content": the full chapter text, preserving paragraphs and sentence order (do NOT summarize or invent content).
3) If the book already has explicit chapter headings, use them. If you must infer a title, keep it concise (3–7 words).
4) Prefer coherent, reasonably sized chapters; avoid creating many tiny chapters. If uncertain, merge related parts rather than split.
5) OUTPUT RULE (must obey): return **only** a single valid JSON object with this exact top-level shape:
{
  "chapters": [
    { "chapterNumber": 1, "title": "...", "content": "..." },
    ...
  ]
}
Do NOT include any additional text, commentary, Markdown, or metadata. Ensure the JSON is UTF-8 and syntactically valid (properly escaped).
6) If no chapters can be identified, return: { "chapters": [] }

BOOK_CONTENT:
""" + fullBookText;

        return callGemini(prompt);
    }


    private String callGemini(String prompt) throws IOException {
        String urlStr = apiUrl + "?key=" + apiKey;
        URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

    
        String requestBody = "{ \"contents\": [ { \"parts\": [ { \"text\": \"" 
                + escapeJson(prompt) + "\" } ] } ] }";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        Scanner scanner;

        if (code == 200) {
            scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name());
        } else {
            scanner = new Scanner(conn.getErrorStream(), StandardCharsets.UTF_8.name());
        }

        while (scanner.hasNextLine()) {
            response.append(scanner.nextLine());
        }
        scanner.close();

        if (code != 200) {
            throw new IOException("Gemini API error: " + code + " - " + response);
        }

        return parseGeminiResponse(response.toString());
    }



    private String parseGeminiResponse(String json) {
        JSONObject root = new JSONObject(json);

        if (!root.has("candidates")) {
            return json;
        }

        JSONArray candidates = root.getJSONArray("candidates");
        if (candidates.isEmpty()) {
            return "";
        }

        JSONObject firstCandidate = candidates.getJSONObject(0);
        JSONObject content = firstCandidate.getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.getJSONObject(i);
            if (part.has("text")) {
                sb.append(part.getString("text")).append("\n");
            }
        }

        return sb.toString().trim();
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
