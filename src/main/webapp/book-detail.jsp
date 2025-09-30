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
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= book.getTitle() %> - Chi tiết</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body {
            background: #f5f7fa;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
        }
        .container {
            max-width: 900px;
            padding: 40px 20px;
        }
        .book-header {
            background: white;
            padding: 30px;
            border-radius: 8px;
            margin-bottom: 30px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
        }
        .book-title {
            font-size: 2rem;
            font-weight: 600;
            color: #1a202c;
            margin-bottom: 10px;
        }
        .book-meta {
            color: #718096;
            font-size: 0.95rem;
        }
        .chapter-item {
            background: white;
            padding: 25px;
            margin-bottom: 20px;
            border-radius: 8px;
            border-left: 3px solid #4299e1;
            box-shadow: 0 1px 3px rgba(0,0,0,0.08);
        }
        .chapter-header {
            display: flex;
            align-items: baseline;
            gap: 12px;
            margin-bottom: 15px;
        }
        .chapter-num {
            background: #4299e1;
            color: white;
            padding: 2px 10px;
            border-radius: 4px;
            font-size: 0.85rem;
            font-weight: 500;
        }
        .chapter-title {
            font-size: 1.25rem;
            font-weight: 600;
            color: #2d3748;
            margin: 0;
        }
        .summary-section {
            background: #f7fafc;
            padding: 18px;
            border-radius: 6px;
            margin-top: 12px;
        }
        .summary-label {
            font-weight: 600;
            color: #2d3748;
            font-size: 0.9rem;
            margin-bottom: 8px;
        }
        .summary-content {
            color: #4a5568;
            line-height: 1.7;
            white-space: pre-wrap;
        }
        .back-btn {
            color: #4299e1;
            text-decoration: none;
            font-weight: 500;
            margin-bottom: 20px;
            display: inline-block;
        }
        .back-btn:hover {
            color: #2b6cb0;
        }
        .loading-text {
            color: #cbd5e0;
            font-style: italic;
        }
        .no-chapters {
            text-align: center;
            padding: 40px;
            color: #a0aec0;
        }
    </style>
</head>
<body>
<div class="container">
    <a href="bookcrud?action=list" class="back-btn">← Quay lại danh sách</a>
    
    <div class="book-header">
        <h1 class="book-title"><%= book.getTitle() %></h1>
        <div class="book-meta">
            <span>Tác giả: <strong><%= book.getAuthor() %></strong></span>
            <% if (book.getMajor() != null && !book.getMajor().isEmpty()) { %>
                <span class="ms-3">Chuyên ngành: <%= book.getMajor() %></span>
            <% } %>
        </div>
        <% if (book.getDescription() != null && !book.getDescription().isEmpty()) { %>
            <p class="mt-3 mb-0" style="color: #4a5568;"><%= book.getDescription() %></p>
        <% } %>
    </div>

    <%
        if (chapters != null && !chapters.isEmpty()) {
            for (Chapter chap : chapters) {
    %>
    <div class="chapter-item">
        <div class="chapter-header">
            <span class="chapter-num">Chương <%= chap.getChapterNumber() %></span>
            <h3 class="chapter-title"><%= chap.getTitle() %></h3>
        </div>
        
        <div class="summary-section">
            <div class="summary-label">Tóm tắt nội dung</div>
            <%
                if (chap.getSummary() != null && !chap.getSummary().trim().isEmpty()) {
            %>
                <div class="summary-content"><%= chap.getSummary() %></div>
            <%
                } else {
            %>
                <div class="loading-text">Đang tạo tóm tắt...</div>
            <%
                }
            %>
        </div>
    </div>
    <%
            }
        } else {
    %>
    <div class="no-chapters">
        <p>Chưa có chương nào được phát hiện.</p>
    </div>
    <%
        }
    %>
</div>
</body>
</html>