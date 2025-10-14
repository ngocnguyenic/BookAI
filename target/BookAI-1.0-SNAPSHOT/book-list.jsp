<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, model.Book" %>
<%
    List<Book> listBook = (List<Book>) request.getAttribute("listBook");
    if (listBook == null) {
        response.sendRedirect("bookcrud?action=list");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Quản lý sách</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        .action-btn { white-space: nowrap; margin: 2px; }
        .rag-message {
            padding: 10px 15px;
            margin-bottom: 10px;
            border-radius: 10px;
            max-width: 80%;
            animation: fadeIn 0.3s;
        }
        .rag-message.user {
            background: #e3f2fd;
            margin-left: auto;
            text-align: right;
        }
        .rag-message.assistant {
            background: white;
            border: 1px solid #dee2e6;
        }
        .rag-message.loading {
            text-align: center;
            color: #999;
            font-style: italic;
            max-width: 100%;
        }
        .rag-sources {
            font-size: 0.85em;
            color: #666;
            margin-top: 8px;
            padding-top: 8px;
            border-top: 1px solid #eee;
        }
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }
        .example-badge {
            cursor: pointer;
            transition: all 0.2s;
        }
        .example-badge:hover {
            background-color: #667eea !important;
            color: white !important;
        }
    </style>
</head>
<body class="container mt-4">

<h2 class="mb-3">Quản lý sách</h2>

<div class="d-flex justify-content-between align-items-center mb-4">
    <a href="book-upload.jsp" class="btn btn-info">Upload sách PDF</a>
</div>



<!-- Bảng danh sách -->
<table class="table table-bordered table-hover align-middle">
    <thead class="table-dark">
        <tr class="text-center">
            <th>ID</th>
            <th>Tiêu đề</th>
            <th>Tác giả</th>
            <th>Chuyên ngành</th>
            <th>Mô tả</th>
            <th>Hành động</th>
        </tr>
    </thead>
    <tbody>
    <%
        if (!listBook.isEmpty()) {
            for (Book b : listBook) {
    %>
        <tr>
            <td class="text-center"><%= b.getBookID() %></td>
            <td><%= b.getTitle() %></td>
            <td><%= b.getAuthor() %></td>
            <td><%= b.getMajor() %></td>
            <td><%= b.getDescription() != null ? b.getDescription() : "" %></td>
            <td class="text-center">
                <% if (b.getFilePath() != null && !b.getFilePath().trim().isEmpty()) { %>
                    <a href="<%= request.getContextPath() %><%= b.getFilePath() %>" 
                       target="_blank" class="btn btn-secondary btn-sm action-btn">Mở PDF</a>
                <% } %>
                <a href="bookdetail?id=<%= b.getBookID() %>" class="btn btn-info btn-sm action-btn">Chương</a>
                <a href="bookcrud?action=edit&id=<%= b.getBookID() %>" class="btn btn-warning btn-sm action-btn">Sửa</a>
                <a href="bookcrud?action=delete&id=<%= b.getBookID() %>" 
                   class="btn btn-danger btn-sm action-btn"
                   onclick="return confirm('Xóa sách này?')">Xóa</a>
            </td>
        </tr>
    <%
            }
        } else {
    %>
        <tr><td colspan="6" class="text-center">Chưa có sách nào.</td></tr>
    <%
        }
    %>
    </tbody>
</table>

<!-- Button RAG Chat -->
<button class="btn btn-success position-fixed shadow" 
        style="bottom: 20px; right: 20px; z-index: 1000;" 
        data-bs-toggle="modal" data-bs-target="#ragChatModal">
    RAG Chat
</button>

<!-- Modal RAG Chat -->
<div class="modal fade" id="ragChatModal" tabindex="-1">
  <div class="modal-dialog modal-lg">
    <div class="modal-content">
      <div class="modal-header" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white;">
        <h5 class="modal-title">RAG Chat - Trợ lý học tập AI</h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body p-0">
        <!-- Chat Box -->
        <div id="ragChatBox" style="height: 400px; overflow-y: auto; padding: 20px; background: #f8f9fa;">
          <div class="rag-message assistant">
            Xin chào! Tôi có thể giúp bạn tìm thông tin từ tài liệu trong thư viện. Hãy đặt câu hỏi!
          </div>
        </div>
        
        <!-- Input Box -->
        <div style="padding: 15px; background: white; border-top: 1px solid #dee2e6;">
          <div class="input-group">
            <input type="text" id="ragQuestionInput" class="form-control" 
                   placeholder="Nhập câu hỏi của bạn..." 
                   onkeypress="if(event.key==='Enter') sendRAGQuestion()">
            <button class="btn btn-primary" onclick="sendRAGQuestion()">Gửi</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
async function sendRAGQuestion() {
    const input = document.getElementById('ragQuestionInput');
    const question = input.value.trim();
    
    if (!question) return;
    
    addRAGMessage('user', question);
    input.value = '';
    
    addRAGMessage('loading', 'Đang tìm kiếm và phân tích...');
    
    try {
        const response = await fetch('<%= request.getContextPath() %>/rag-chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: 'question=' + encodeURIComponent(question)
        });
        
        const data = await response.json();
        
        const loadingMsg = document.querySelector('.rag-message.loading');
        if (loadingMsg) loadingMsg.remove();
        
        if (data.error) {
            addRAGMessage('assistant', 'Lỗi: ' + data.error);
        } else {
            let message = data.answer;
            
            if (data.sources && data.sources.length > 0) {
                message += '<div class="rag-sources">Nguồn tham khảo: ';
                data.sources.forEach((s, i) => {
                    message += (i > 0 ? '; ' : '') + 'Chương ' + s.chapterNumber + ': ' + s.title;
                });
                message += '</div>';
            }
            
            addRAGMessage('assistant', message);
        }
    } catch (error) {
        const loadingMsg = document.querySelector('.rag-message.loading');
        if (loadingMsg) loadingMsg.remove();
        addRAGMessage('assistant', 'Lỗi kết nối: ' + error.message);
    }
}

function addRAGMessage(type, content) {
    const chatBox = document.getElementById('ragChatBox');
    const div = document.createElement('div');
    div.className = 'rag-message ' + type;
    
    if (type === 'assistant' && content.includes('<div class="rag-sources">')) {
        div.innerHTML = content;
    } else {
        div.textContent = content;
    }
    
    chatBox.appendChild(div);
    chatBox.scrollTop = chatBox.scrollHeight;
}

function askExample(question) {
    document.getElementById('ragQuestionInput').value = question;
    sendRAGQuestion();
}

document.getElementById('ragChatModal').addEventListener('shown.bs.modal', function () {
    document.getElementById('ragQuestionInput').focus();
});

document.getElementById('ragChatModal').addEventListener('hidden.bs.modal', function () {
    const chatBox = document.getElementById('ragChatBox');
    chatBox.innerHTML = '<div class="rag-message assistant">Xin chào! Tôi có thể giúp bạn tìm thông tin từ tài liệu trong thư viện. Hãy đặt câu hỏi!</div>';
});
</script>
</body>
</html>