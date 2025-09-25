<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, model.Chapter, model.Book" %>
<%
    Book book = (Book) request.getAttribute("book");
    List<Chapter> chapters = (List<Chapter>) request.getAttribute("chapters");

    if (book == null) {
        response.sendRedirect("bookcrud?action=list");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <title>📖 <%= book.getTitle() %> - Chi tiết chương</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <style>
        body { padding: 40px; background: #f8f9fa; }
        .chapter-card { background: white; padding: 25px; margin-bottom: 30px; border-radius: 12px; box-shadow: 0 3px 10px rgba(0,0,0,0.08); }
        .chapter-title { font-size: 1.5rem; color: #2c3e50; margin-bottom: 15px; font-weight: 600; }
        .chapter-number { display: inline-block; background: #3498db; color: white; padding: 4px 10px; border-radius: 20px; font-size: 0.9rem; }
        .summary-box { background: #ecf0f1; padding: 20px; border-left: 4px solid #3498db; margin-top: 20px; border-radius: 0 8px 8px 0; }
        .summary-title { font-weight: bold; color: #2980b9; margin-bottom: 8px; }
        .back-link { margin-bottom: 30px; display: inline-block; text-decoration: none; color: #3498db; }
        .back-link:hover { text-decoration: underline; }
        .loading { color: #e74c3c; font-style: italic; }
    </style>
</head>
<body>

<div class="container">
    <a href="bookcrud?action=list" class="back-link">← Quay lại danh sách sách</a>

    <div class="text-center mb-4">
        <h1 class="display-5">📖 <%= book.getTitle() %></h1>
        <p class="lead text-muted">Tác giả: <strong><%= book.getAuthor() %></strong></p>
        <p><%= book.getDescription() != null ? book.getDescription() : "Chưa có mô tả." %></p>
    </div>

    <hr>

    <%
        if (chapters != null && !chapters.isEmpty()) {
            for (int i = 0; i < chapters.size(); i++) {
                Chapter chap = chapters.get(i);
    %>
        <div class="chapter-card">
            <div>
                <span class="chapter-number">Chương <%= chap.getChapterNumber() %></span>
                <h3 class="chapter-title"><%= chap.getTitle() %></h3>
            </div>

            <!-- Hiển thị tóm tắt -->
            <div class="summary-box">
                <div class="summary-title">📝 Tóm tắt chương:</div>
                <%
                    if (chap.getSummary() != null && !chap.getSummary().trim().isEmpty()) {
                %>
                    <p><%= chap.getSummary().replace("\n", "<br>") %></p>
                <%
                    } else {
                %>
                    <p class="loading">⏳ Đang tạo tóm tắt bằng AI...</p>
                <%
                    }
                %>
            </div>

       
        </div>
    <%
            }
        } else {
    %>
        <div class="alert alert-warning text-center">
            📄 Chưa có chương nào. Có thể do lỗi khi xử lý file PDF.
        </div>
    <%
        }
    %>

</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>