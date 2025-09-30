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

    // Optimized: Summary chi tiết hơn
 public String generateChapterSummary(String chapterTitle, String chapterContent) throws IOException {
    String prompt = """
        Bạn là giảng viên đại học. Tạo bản tóm tắt CHI TIẾT cho chương sách sau theo ĐÚNG FORMAT bên dưới:
        
        FORMAT BẮT BUỘC:
        
        TỔNG QUAN:
        [2-3 câu giới thiệu chung về chương này]
        
        NỘI DUNG CHÍNH:
        • [Điểm quan trọng 1 - giải thích 1-2 câu]
        • [Điểm quan trọng 2 - giải thích 1-2 câu]
        • [Điểm quan trọng 3 - giải thích 1-2 câu]
        • [Điểm quan trọng 4 - giải thích 1-2 câu]
        
        KHÁI NIỆM THEN CHỐT:
        - [Khái niệm 1]: [Định nghĩa ngắn]
        - [Khái niệm 2]: [Định nghĩa ngắn]
        - [Khái niệm 3]: [Định nghĩa ngắn]
        
        ỨNG DỤNG/VÍ DỤ:
        [Ví dụ thực tế hoặc ứng dụng của nội dung chương - 2-3 câu]
        
        ---
        
        YÊU CẦU:
        - Độ dài tối thiểu: 250 từ
        - PHẢI có đủ 4 phần: TỔNG QUAN, NỘI DUNG CHÍNH (4 điểm), KHÁI NIỆM (3 khái niệm), ỨNG DỤNG
        - Mỗi điểm phải có giải thích cụ thể, KHÔNG được chỉ liệt kê
        - Viết bằng tiếng Việt học thuật, rõ ràng
        - Dựa 100% vào nội dung được cung cấp
        
        CHƯƠNG: """ + chapterTitle + """
        
        NỘI DUNG:
        """ + chapterContent + """
        
        BẮT ĐẦU TÓM TẮT THEO FORMAT:
        """;

    return callGemini(prompt);
}
    public String generateQAPairs(String chapterTitle, String chapterContent) throws IOException {
        String prompt = """
            Tạo 3 câu hỏi ôn tập và đáp án cho chương này.
            Trả về JSON format: [{"question":"...","answer":"..."}]
            
            CHƯƠNG: """ + chapterTitle + """
            NỘI DUNG: """ + chapterContent;

        return callGemini(prompt);
    }

    public String detectChaptersWithAI(String fullBookText) throws IOException {
        String prompt = """
            Phân tích và chia sách thành các chương logic.
            
            QUY TẮC:
            1) Bỏ qua lời nói đầu, mục lục, phụ lục
            2) Mỗi chương có: chapterNumber (int), title (ngắn gọn), content (toàn bộ)
            3) Ưu tiên các tiêu đề rõ ràng trong sách
            4) Merge các phần nhỏ thành chương lớn hơn
            5) TRẢ VỀ JSON: {"chapters":[{"chapterNumber":1,"title":"...","content":"..."}]}
            
            NỘI DUNG SÁCH:
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

        String requestBody = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}]}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        
        try (Scanner scanner = new Scanner(
            code == 200 ? conn.getInputStream() : conn.getErrorStream(), 
            StandardCharsets.UTF_8)) {
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
        }

        if (code != 200) {
            throw new IOException("Gemini API error: " + code + " - " + response);
        }

        return parseGeminiResponse(response.toString());
    }

    private String parseGeminiResponse(String json) {
        JSONObject root = new JSONObject(json);
        
        if (!root.has("candidates") || root.getJSONArray("candidates").isEmpty()) {
            return "";
        }

        JSONObject firstCandidate = root.getJSONArray("candidates").getJSONObject(0);
        JSONArray parts = firstCandidate.getJSONObject("content").getJSONArray("parts");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            if (parts.getJSONObject(i).has("text")) {
                sb.append(parts.getJSONObject(i).getString("text")).append("\n");
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