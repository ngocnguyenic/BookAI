<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, model.Book" %>
<%
    List<Book> listBook = (List<Book>) request.getAttribute("listBook");
    if (listBook == null) {
        response.sendRedirect("bookcrud?action=list");
        return;
    }
%>
<html>
<head>
    <title>📚 Quản lý sách</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <script src="https://code.responsivevoice.org/responsivevoice.js?key=eW5KOf7Q"></script>
    <style>
        .tts-controls { margin-top: 10px; padding: 10px; background: #f8f9fa; border-radius: 5px; border: 1px solid #dee2e6; }
        .tts-btn { margin: 2px; padding: 5px 10px; font-size: 12px; }
        .voice-settings { margin-top: 8px; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
        .voice-settings label { font-size: 12px; margin: 0; }
        .voice-settings select, .voice-settings input { font-size: 12px; padding: 3px 6px; }
        #aiResult { max-height: 200px; overflow-y: auto; }
        .action-btn { white-space: nowrap; }
    </style>
</head>
<body class="container mt-4">

<h2 class="mb-3">📚 Quản lý sách</h2>

<!-- NÚT UPLOAD PDF -->
<div class="d-flex justify-content-between align-items-center mb-4">
    <a href="book-upload.jsp" class="btn btn-info">
        📤 Upload sách PDF
    </a>
</div>

<!-- FORM THÊM SÁCH THỦ CÔNG -->
<form action="bookcrud" method="post" class="mb-4">
    <input type="hidden" name="action" value="insert"/>
    <div class="row g-2">
        <div class="col-md-3"><input type="text" name="title" class="form-control" placeholder="Tiêu đề" required></div>
        <div class="col-md-2"><input type="text" name="author" class="form-control" placeholder="Tác giả" required></div>
        <div class="col-md-2"><input type="text" name="major" class="form-control" placeholder="Chuyên ngành"></div>
        <div class="col-md-3"><input type="text" name="description" class="form-control" placeholder="Mô tả"></div>
        <div class="col-md-2"><button type="submit" class="btn btn-primary w-100">➕ Thêm sách</button></div>
    </div>
</form>

<!-- BẢNG DANH SÁCH SÁCH -->
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
                <!-- NÚT MỞ FILE PDF (nếu có) -->
                <%
                    if (b.getFilePath() != null && !b.getFilePath().trim().isEmpty()) {
                %>
                    <a href="<%= request.getContextPath() %><%= b.getFilePath() %>" target="_blank" class="btn btn-secondary btn-sm action-btn">
                        📄 Mở PDF
                    </a>
                <%
                    }
                %>
                <!-- NÚT XEM CHƯƠNG -->
                <a href="bookdetail?id=<%= b.getBookID() %>" class="btn btn-info btn-sm action-btn">
                    📖 Chương
                </a>
                <!-- NÚT EDIT/DELETE -->
                <a href="bookcrud?action=edit&id=<%= b.getBookID() %>" class="btn btn-warning btn-sm action-btn">Sửa</a>
                <a href="bookcrud?action=delete&id=<%= b.getBookID() %>" class="btn btn-danger btn-sm action-btn"
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

<!-- NÚT AI CHAT (giữ nguyên) -->
<button class="btn btn-success position-fixed shadow" style="bottom: 20px; right: 20px;" 
        data-bs-toggle="modal" data-bs-target="#aiChatModal">
    🤖 AI Chat
</button>

<!-- MODAL AI CHAT (giữ nguyên) -->
<div class="modal fade" id="aiChatModal" tabindex="-1">
  <div class="modal-dialog modal-lg modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header bg-primary text-white">
        <h5 class="modal-title">AI Assistant with Text-to-Speech</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <form id="aiForm">
          <div class="mb-2">
            <label class="form-label">Chapter Title:</label>
            <input type="text" id="aiTitle" class="form-control" placeholder="Enter chapter title" required>
          </div>
          <div class="mb-2">
            <label class="form-label">Chapter Content:</label>
            <textarea id="aiContent" class="form-control" rows="4" placeholder="Enter content..." required></textarea>
          </div>
          <div class="mb-3">
            <label class="form-label">Function:</label>
            <select id="aiAction" class="form-select">
              <option value="summary">Summary</option>
              <option value="qa">Q&A</option>
            </select>
          </div>
          <button type="submit" class="btn btn-primary w-100" id="submitBtn">
            Ask AI
          </button>
        </form>
        
        <hr>
        
        <label class="form-label">AI Response:</label>
        <div id="aiResult" class="p-3 border rounded bg-light" style="min-height: 100px; white-space: pre-wrap;">
          Waiting for AI response...
        </div>
        
        <div class="tts-controls" id="ttsControls" style="display: none;">
          <div class="mb-2"><strong>Text-to-Speech Controls</strong></div>
          <div class="d-flex gap-2 mb-2">
            <button type="button" class="btn btn-success btn-sm tts-btn" onclick="startSpeaking()" id="playBtn">Play</button>
            <button type="button" class="btn btn-danger btn-sm tts-btn" onclick="stopSpeaking()" id="stopBtn" disabled>Stop</button>
          </div>
          <div class="voice-settings">
            <label>Voice:</label>
            <select id="voiceSelect" class="form-select form-select-sm" style="width: auto;">
              <option value="Vietnamese Female">Vietnamese Female</option>
              <option value="Vietnamese Male">Vietnamese Male</option>
              <option value="UK English Female">UK English Female</option>
              <option value="UK English Male">UK English Male</option>
              <option value="US English Female">US English Female</option>
            </select>
            <label>Speed:</label>
            <input type="range" id="speedRange" min="0.1" max="2" step="0.1" value="1" class="form-range" style="width: 80px;">
            <span id="speedValue">1.0x</span>
            <label>Pitch:</label>
            <input type="range" id="pitchRange" min="0" max="2" step="0.1" value="1" class="form-range" style="width: 80px;">
            <span id="pitchValue">1.0</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>

<!-- SCRIPTS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
let currentText = '';

document.getElementById("aiForm").addEventListener("submit", async function(e) {
    e.preventDefault();
    const title = document.getElementById("aiTitle").value;
    const content = document.getElementById("aiContent").value;
    const action = document.getElementById("aiAction").value;
    const resultDiv = document.getElementById("aiResult");

    resultDiv.innerHTML = "AI is processing...";

    const params = new URLSearchParams();
    params.append("action", action);
    params.append("title", title);
    params.append("content", content);

    const response = await fetch("aiservlet", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params
    });

    const responseText = await response.text();
    resultDiv.innerHTML = responseText;
    currentText = responseText;
    if (currentText.trim()) {
        document.getElementById("ttsControls").style.display = "block";
    }
});

function startSpeaking() {
    if (!currentText.trim()) return;
    responsiveVoice.cancel();
    document.getElementById("playBtn").disabled = true;
    document.getElementById("stopBtn").disabled = false;
    responsiveVoice.speak(currentText, document.getElementById("voiceSelect").value, {
        rate: parseFloat(document.getElementById("speedRange").value),
        pitch: parseFloat(document.getElementById("pitchRange").value),
        onend: () => {
            document.getElementById("playBtn").disabled = false;
            document.getElementById("stopBtn").disabled = true;
        }
    });
}

function stopSpeaking() {
    responsiveVoice.cancel();
    document.getElementById("playBtn").disabled = false;
    document.getElementById("stopBtn").disabled = true;
}

document.getElementById('aiChatModal').addEventListener('hidden.bs.modal', function () {
    stopSpeaking();
});

// Hiển thị giá trị speed/pitch
document.getElementById('speedRange').addEventListener('input', function() {
    document.getElementById('speedValue').textContent = this.value + 'x';
});
document.getElementById('pitchRange').addEventListener('input', function() {
    document.getElementById('pitchValue').textContent = this.value;
});
</script>
</body>
</html>