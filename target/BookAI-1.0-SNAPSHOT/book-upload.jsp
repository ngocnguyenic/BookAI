<!-- book-upload.jsp -->
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>📤 Upload Sách PDF</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body class="container mt-4">

<h2>📤 Upload Sách từ File PDF</h2>

<% if (request.getAttribute("error") != null) { %>
    <div class="alert alert-danger"><%= request.getAttribute("error") %></div>
<% } %>

<% if (session.getAttribute("successMessage") != null) { %>
    <div class="alert alert-success"><%= session.getAttribute("successMessage") %></div>
    <% session.removeAttribute("successMessage"); %>
<% } %>

<form action="bookcrud?action=upload" method="post" enctype="multipart/form-data" class="border p-4 rounded">
    <div class="mb-3">
        <label class="form-label">Tiêu đề sách</label>
        <input type="text" name="title" class="form-control" required>
    </div>
    <div class="mb-3">
        <label class="form-label">Tác giả</label>
        <input type="text" name="author" class="form-control" required>
    </div>
    <div class="mb-3">
        <label class="form-label">Chuyên ngành</label>
        <input type="text" name="major" class="form-control">
    </div>
    <div class="mb-3">
        <label class="form-label">Mô tả</label>
        <textarea name="description" class="form-control" rows="3"></textarea>
    </div>
    <div class="mb-3">
        <label class="form-label">Chọn file PDF</label>
        <input type="file" name="pdfFile" accept=".pdf" class="form-control" required>
    </div>
    <button type="submit" class="btn btn-success">📤 Upload & Xử lý bằng AI</button>
    <a href="book-list.jsp" class="btn btn-secondary ms-2">Hủy</a>
</form>

</body>
</html>