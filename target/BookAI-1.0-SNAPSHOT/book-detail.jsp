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
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
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
            transition: all 0.3s ease;
        }
        .chapter-item:hover {
            box-shadow: 0 4px 12px rgba(0,0,0,0.12);
        }
        .chapter-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            margin-bottom: 15px;
        }
        .chapter-info {
            display: flex;
            align-items: baseline;
            gap: 12px;
            flex: 1;
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
        .summary-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
        }
        .summary-label {
            font-weight: 600;
            color: #2d3748;
            font-size: 0.9rem;
        }
        .summary-content {
            color: #4a5568;
            line-height: 1.7;
            white-space: pre-wrap;
        }
        .summary-placeholder {
            color: #a0aec0;
            font-style: italic;
            text-align: center;
            padding: 20px;
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
        .no-chapters {
            text-align: center;
            padding: 40px;
            color: #a0aec0;
        }
        
        .btn-generate {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border: none;
            padding: 8px 18px;
            border-radius: 6px;
            font-size: 0.9rem;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.3s ease;
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }
        .btn-generate:hover:not(:disabled) {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
        }
        .btn-generate:disabled {
            opacity: 0.6;
            cursor: not-allowed;
        }
        .btn-generate .spinner {
            width: 14px;
            height: 14px;
            border: 2px solid rgba(255,255,255,0.3);
            border-top-color: white;
            border-radius: 50%;
            animation: spin 0.8s linear infinite;
        }
        @keyframes spin {
            to { transform: rotate(360deg); }
        }
        
        .status-badge {
            padding: 4px 10px;
            border-radius: 12px;
            font-size: 0.8rem;
            font-weight: 500;
        }
        .badge-no-summary {
            background: #e2e8f0;
            color: #64748b;
        }
        .badge-has-summary {
            background: #d1fae5;
            color: #065f46;
        }
        .badge-cached {
            background: #dbeafe;
            color: #1e40af;
        }
        
        .mcq-container {
            margin-top: 15px;
            background: white;
            border-radius: 6px;
            border: 1px solid #e2e8f0;
        }
        .mcq-header {
            padding: 15px;
            border-bottom: 1px solid #e2e8f0;
            background: #f8fafc;
        }
        .mcq-item {
            padding: 15px;
            border-bottom: 1px solid #f1f5f9;
        }
        .mcq-item:last-child {
            border-bottom: none;
        }
        .mcq-question {
            font-weight: 600;
            color: #1e293b;
            margin-bottom: 12px;
        }
        .mcq-option {
            padding: 10px 15px;
            margin: 6px 0;
            border: 1px solid #e2e8f0;
            border-radius: 4px;
            cursor: pointer;
            transition: all 0.2s;
        }
        .mcq-option:hover {
            background: #f8fafc;
            border-color: #cbd5e1;
        }
        .mcq-option.selected {
            background: #dbeafe;
            border-color: #3b82f6;
        }
        .mcq-option.correct {
            background: #dcfce7;
            border-color: #22c55e;
        }
        .mcq-option.wrong {
            background: #fee2e2;
            border-color: #ef4444;
        }
        .mcq-answer {
            margin-top: 12px;
            padding: 15px;
            background: #f8fafb;
            border-radius: 6px;
            display: none;
            border: 1px solid #e2e8f0;
        }
        .mcq-answer.show {
            display: block;
        }
        .ai-feedback-box {
            margin-top: 10px;
            padding: 12px;
            background: #f0f9ff;
            border-radius: 6px;
            border: 1px solid #e0f2fe;
        }
        .ai-feedback-box strong {
            color: #1e40af;
            display: block;
            margin-bottom: 5px;
            font-size: 0.9rem;
        }
        .ai-feedback-box p {
            color: #1e3a8a;
            margin: 0;
            line-height: 1.6;
            font-size: 0.95rem;
        }
        .submit-container {
            padding: 20px;
            text-align: center;
            border-top: 2px solid #e2e8f0;
            background: #f8fafc;
        }
        .btn-submit {
            padding: 12px 40px;
            background: #10b981;
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 1rem;
            font-weight: 600;
        }
        .btn-submit:hover {
            background: #059669;
        }
        .btn-submit:disabled {
            opacity: 0.6;
            cursor: not-allowed;
        }
        
        .next-action-section {
            margin-top: 15px;
            padding: 15px;
            background: rgba(255,255,255,0.15);
            border-radius: 6px;
            border-top: 2px solid rgba(255,255,255,0.3);
        }
        
        .recommendation-badge {
            display: inline-block;
            padding: 6px 12px;
            background: rgba(255,255,255,0.2);
            border-radius: 12px;
            font-size: 0.85rem;
            margin-right: 8px;
            margin-bottom: 8px;
        }
        
        .btn-next-quiz {
            padding: 10px 25px;
            background: white;
            color: #667eea;
            border: none;
            border-radius: 6px;
            font-weight: 600;
            cursor: pointer;
            margin-top: 10px;
            transition: all 0.3s;
        }
        
        .btn-next-quiz:hover {
            background: #f0f0f0;
            transform: translateY(-2px);
        }
        
        @keyframes fadeIn {
            from {
                opacity: 0;
                transform: translateY(-10px);
            }
            to {
                opacity: 1;
                transform: translateY(0);
            }
        }
        .fade-in {
            animation: fadeIn 0.5s ease;
        }
    </style>
</head>
<body>
<div class="container">
    <a href="bookcrud?action=list" class="back-btn">
        <i class="fas fa-arrow-left"></i> Quay lại danh sách
    </a>
    
    <div class="book-header">
        <h1 class="book-title"><%= book.getTitle() %></h1>
        <div class="book-meta">
            <span><i class="fas fa-user"></i> Tác giả: <strong><%= book.getAuthor() %></strong></span>
            <% if (book.getMajor() != null && !book.getMajor().isEmpty()) { %>
                <span class="ms-3"><i class="fas fa-book"></i> Chuyên ngành: <%= book.getMajor() %></span>
            <% } %>
        </div>
        <% if (book.getDescription() != null && !book.getDescription().isEmpty()) { %>
            <p class="mt-3 mb-0" style="color: #4a5568;"><%= book.getDescription() %></p>
        <% } %>
    </div>

    <%
        if (chapters != null && !chapters.isEmpty()) {
            for (Chapter chap : chapters) {
                boolean hasSummary = chap.getSummary() != null && !chap.getSummary().trim().isEmpty();
    %>
    <div class="chapter-item">
        <div class="chapter-header">
            <div class="chapter-info">
                <span class="chapter-num">Chương <%= chap.getChapterNumber() %></span>
                <h3 class="chapter-title"><%= chap.getTitle() %></h3>
            </div>
            
            <% if (!hasSummary) { %>
                <button class="btn-generate" 
                        onclick="generateSummary(<%= chap.getChapterID() %>, this)"
                        data-chapter-id="<%= chap.getChapterID() %>">
                    <i class="fas fa-play"></i>
                    <span class="btn-text">Tạo tóm tắt</span>
                </button>
            <% } else { %>
                <span class="status-badge badge-has-summary">
                    <i class="fas fa-check-circle"></i> Đã có tóm tắt
                </span>
            <% } %>
        </div>
        
        <div class="summary-section">
            <div class="summary-header">
                <div class="summary-label">
                    <i class="fas fa-file-alt"></i> Tóm tắt nội dung
                </div>
            </div>
            
            <div id="summary-<%= chap.getChapterID() %>" class="summary-display">
                <% if (hasSummary) { %>
                    <div class="summary-content"><%= chap.getSummary() %></div>
                <% } else { %>
                    <div class="summary-placeholder">
                        <i class="fas fa-magic"></i> 
                        Nhấn nút "Tạo tóm tắt" để tạo tóm tắt bằng AI
                    </div>
                <% } %>
            </div>
        </div>

        <% if (hasSummary) { %>
        <div id="qa-container-<%= chap.getChapterID() %>" style="margin-top: 12px;">
            <button class="btn-generate" 
                    style="background: #48bb78;"
                    onclick="generateQA(<%= chap.getChapterID() %>, this)">
                <i class="fas fa-question-circle"></i>
                <span class="btn-text">Tạo câu hỏi trắc nghiệm</span>
            </button>
            <span id="qa-status-<%= chap.getChapterID() %>" style="margin-left: 10px;"></span>
        </div>
        <% } %>
    </div>
    <%
            }
        } else {
    %>
    <div class="no-chapters">
        <i class="fas fa-book-open fa-3x mb-3"></i>
        <p>Chưa có chương nào được phát hiện.</p>
    </div>
    <%
        }
    %>
</div>

<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script>
$(document).ready(function() {
    console.log("Page loaded");
});

function generateSummary(chapterId, button) {
    button.disabled = true;
    var btnText = button.querySelector('.btn-text');
    btnText.textContent = 'Đang tạo...';
    button.innerHTML = '<div class="spinner"></div> <span class="btn-text">Đang tạo...</span>';
    
    $.ajax({
        url: 'chaptersummaryservlet',
        method: 'POST',
        data: { chapterId: chapterId },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                var summaryDiv = $('#summary-' + chapterId);
                var summaryHtml = '<div class="summary-content fade-in">' + 
                                  escapeHtml(response.summary) + 
                                  '</div>';
                summaryDiv.html(summaryHtml);
                
                var badge = response.cached ? 
                    '<span class="status-badge badge-cached"><i class="fas fa-clock"></i> Đã lưu trước</span>' :
                    '<span class="status-badge badge-has-summary"><i class="fas fa-check-circle"></i> Đã tạo xong</span>';
                
                $(button).replaceWith(badge);
                showNotification('success', 'Tóm tắt đã được tạo thành công!');
                
                var qaButton = '<div id="qa-container-' + chapterId + '" style="margin-top: 12px;">' +
                    '<button class="btn-generate" style="background: #48bb78;" onclick="generateQA(' + chapterId + ', this)">' +
                    '<i class="fas fa-question-circle"></i> <span class="btn-text">Tạo câu hỏi trắc nghiệm</span>' +
                    '</button>' +
                    '<span id="qa-status-' + chapterId + '" style="margin-left: 10px;"></span>' +
                    '</div>';
                summaryDiv.closest('.summary-section').after(qaButton);
                
            } else {
                button.disabled = false;
                button.innerHTML = '<i class="fas fa-play"></i> <span class="btn-text">Tạo tóm tắt</span>';
                showNotification('error', response.error || 'Có lỗi xảy ra');
            }
        },
        error: function(xhr, status, error) {
            button.disabled = false;
            button.innerHTML = '<i class="fas fa-play"></i> <span class="btn-text">Tạo tóm tắt</span>';
            showNotification('error', 'Lỗi kết nối: ' + error);
        }
    });
}

function generateQA(chapterId, button) {
    button.disabled = true;
    var btnText = button.querySelector('.btn-text');
    btnText.textContent = 'Đang tạo...';
    button.innerHTML = '<div class="spinner"></div> <span class="btn-text">Đang tạo...</span>';
    
    $.ajax({
        url: 'api/chapter/generate-qa',
        method: 'POST',
        data: { chapterId: chapterId, numQuestions: 5 },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                $('#qa-status-' + chapterId).html(
                    '<span class="status-badge badge-has-summary">' +
                    '<i class="fas fa-check"></i> Đã tạo ' + response.count + ' câu hỏi</span>'
                );
                
                displayMCQs(chapterId, response.qas);
                button.style.display = 'none';
                showNotification('success', 'Đã tạo ' + response.count + ' câu hỏi trắc nghiệm!');
            } else {
                button.disabled = false;
                button.innerHTML = '<i class="fas fa-question-circle"></i> <span class="btn-text">Tạo câu hỏi trắc nghiệm</span>';
                showNotification('error', response.error || 'Lỗi tạo câu hỏi');
            }
        },
        error: function(xhr, status, error) {
            button.disabled = false;
            button.innerHTML = '<i class="fas fa-question-circle"></i> <span class="btn-text">Tạo câu hỏi trắc nghiệm</span>';
            showNotification('error', 'Lỗi: ' + error);
        }
    });
}

var mcqData = {};

function displayMCQs(chapterId, qas) {
    if (!qas || qas.length === 0) return;
    
    mcqData[chapterId] = qas;
    
    var html = '<div class="mcq-container" id="mcq-container-' + chapterId + '">';
    html += '<div class="mcq-header">';
    html += '<strong>Câu hỏi trắc nghiệm (' + qas.length + ' câu)</strong>';
    html += '</div>';
    
    for (var i = 0; i < qas.length; i++) {
        var qa = qas[i];
        var lines = qa.question.split('\n');
        var questionText = lines[0];
        var options = [];
        
        for (var k = 1; k < lines.length; k++) {
            var line = lines[k].trim();
            if (line && line.match(/^[A-D]\./)) {
                options.push(line);
            }
        }
        
        html += '<div class="mcq-item" id="mcq-item-' + chapterId + '-' + i + '">';
        html += '<div class="mcq-question">Câu ' + (i + 1) + ': ' + escapeHtml(questionText) + '</div>';
        
        for (var j = 0; j < options.length; j++) {
            var optionId = 'opt-' + chapterId + '-' + i + '-' + j;
            var optionLetter = options[j].charAt(0);
            html += '<div class="mcq-option" id="' + optionId + '" data-letter="' + optionLetter + '" onclick="selectOption(\'' + optionId + '\', ' + chapterId + ', ' + i + ')">';
            html += escapeHtml(options[j]);
            html += '</div>';
        }
        
        html += '<div class="mcq-answer" id="answer-' + chapterId + '-' + i + '">';
        html += '<strong>Đáp án đúng:</strong> ' + escapeHtml(qa.answer);
        html += '</div>';
        html += '</div>';
    }
    
    html += '<div class="submit-container">';
    html += '<button class="btn-submit" onclick="submitAllAnswers(' + chapterId + ', event)">Nộp bài</button>';
    html += '</div>';
    html += '</div>';
    
    $('#qa-container-' + chapterId).after(html);
}

function selectOption(optionId, chapterId, questionIndex) {
    $('[id^="opt-' + chapterId + '-' + questionIndex + '-"]').removeClass('selected');
    $('#' + optionId).addClass('selected');
}

function submitAllAnswers(chapterId, event) {
    var qas = mcqData[chapterId];
    if (!qas) return;
    
    var submitBtn = event.target;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<div class="spinner"></div> Đang đánh giá...';
    
    var userAnswers = [];
    for (var i = 0; i < qas.length; i++) {
        var qa = qas[i];
        var selectedOption = $('[id^="opt-' + chapterId + '-' + i + '-"].selected');
        
        var userAnswer = {
            qaId: qa.qaid || qa.qaID || qa.QAID,
            chapterId: chapterId,
            question: qa.question.split('\n')[0],
            correctAnswer: qa.answer,
            userAnswer: selectedOption.length > 0 ? selectedOption.text().trim() : '',
            difficulty: qa.difficulty || 'medium'
        };
        
        userAnswers.push(userAnswer);
    }
    
    $.ajax({
        url: 'submit-mcq-batch',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({
            chapterId: chapterId,
            answers: userAnswers
        }),
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                displayResults(chapterId, response.evaluations, response.masteryScore, response.userTheta, response.hasAchievedMastery);
            } else {
                showNotification('error', response.error || 'Lỗi đánh giá bài làm');
                submitBtn.disabled = false;
                submitBtn.innerHTML = 'Nộp bài';
            }
        },
        error: function(xhr, status, error) {
            showNotification('error', 'Lỗi kết nối: ' + error);
            submitBtn.disabled = false;
            submitBtn.innerHTML = 'Nộp bài';
        }
    });
}

function displayResults(chapterId, evaluations, masteryScore, userTheta, hasAchievedMastery) {
    var correctCount = 0;
    var totalScore = 0;
    
    for (var i = 0; i < evaluations.length; i++) {
        var eval = evaluations[i];
        var qa = mcqData[chapterId][i];
        var correctLetter = qa.answer.charAt(0);
        
        $('[id^="opt-' + chapterId + '-' + i + '-"]').each(function() {
            var optionLetter = $(this).data('letter');
            $(this).css('pointer-events', 'none');
            
            if (optionLetter === correctLetter) {
                $(this).addClass('correct');
            }
        });
        
        var selectedOption = $('[id^="opt-' + chapterId + '-' + i + '-"].selected');
        if (selectedOption.length > 0) {
            var selectedLetter = selectedOption.data('letter');
            if (selectedLetter !== correctLetter) {
                selectedOption.addClass('wrong');
            } else {
                correctCount++;
            }
        }
        
        var feedbackHtml = '<div class="mcq-answer show">';
        feedbackHtml += '<div style="margin-bottom: 10px;"><strong>Đáp án đúng:</strong> ' + escapeHtml(qa.answer) + '</div>';
        
        if (eval.feedback) {
            feedbackHtml += '<div class="ai-feedback-box">';
            feedbackHtml += '<strong>Nhận xét AI:</strong>';
            feedbackHtml += '<p>' + escapeHtml(eval.feedback) + '</p>';
            feedbackHtml += '</div>';
        }
        
        feedbackHtml += '</div>';
        $('#answer-' + chapterId + '-' + i).replaceWith(feedbackHtml);
        
        totalScore += eval.score || 0;
    }
    
    var avgScore = Math.round(totalScore / evaluations.length);
    var level = getLevelFromScore(masteryScore);
    
    var resultHtml = '<div style="margin-top: 20px; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 8px; color: white;">';
    resultHtml += '<h4 style="margin: 0 0 15px 0; color: white;"><i class="fas fa-chart-line"></i> Kết quả đánh giá</h4>';
    
    resultHtml += '<div style="background: rgba(255,255,255,0.15); padding: 15px; border-radius: 6px; margin-bottom: 10px;">';
    resultHtml += '<div style="display: flex; justify-content: space-around; text-align: center;">';
    resultHtml += '<div><div style="font-size: 2rem; font-weight: bold;">' + correctCount + '/' + evaluations.length + '</div><div style="font-size: 0.9rem; opacity: 0.9;">Câu đúng</div></div>';
    resultHtml += '<div><div style="font-size: 2rem; font-weight: bold;">' + avgScore + '%</div><div style="font-size: 0.9rem; opacity: 0.9;">Điểm TB</div></div>';
    resultHtml += '<div><div style="font-size: 2rem; font-weight: bold;">' + Math.round(masteryScore) + '%</div><div style="font-size: 0.9rem; opacity: 0.9;">Độ thành thạo</div></div>';
    resultHtml += '</div></div>';
    
    resultHtml += '<div style="text-align: center; padding: 10px; background: rgba(255,255,255,0.1); border-radius: 4px;">';
    resultHtml += '<strong style="font-size: 1.1rem;">' + level.icon + ' ' + level.text + '</strong>';
    resultHtml += '</div>';
    
    resultHtml += '<div style="margin-top: 15px; padding: 12px; background: rgba(255,255,255,0.1); border-radius: 4px; font-size: 0.9rem;">';
    resultHtml += '<strong>Phân tích mức độ hiểu:</strong><br>';
    resultHtml += level.analysis;
    resultHtml += '</div>';
    
    // GỢI Ý CÂU HỎI TIẾP THEO DỰA TRÊN IRT
    resultHtml += getNextQuestionRecommendation(masteryScore, userTheta, hasAchievedMastery, chapterId);
    
    resultHtml += '</div>';
    
    $('#mcq-container-' + chapterId + ' .submit-container').replaceWith(resultHtml);
    
    $('html, body').animate({
        scrollTop: $('#mcq-container-' + chapterId).offset().top - 100
    }, 500);
    
    showNotification('success', 'Đã đánh giá xong! Độ thành thạo: ' + Math.round(masteryScore) + '%');
}

function getNextQuestionRecommendation(masteryScore, userTheta, hasAchievedMastery, chapterId) {
    var html = '<div class="next-action-section">';
    html += '<strong style="display: block; margin-bottom: 10px;"><i class="fas fa-lightbulb"></i> Bước tiếp theo:</strong>';
    
    if (typeof hasAchievedMastery === 'undefined') {
        hasAchievedMastery = masteryScore >= 85;
    }
    
    if (typeof userTheta === 'undefined') {
        userTheta = (masteryScore / 100) * 2 - 1;
    }
    
    if (hasAchievedMastery) {
        // Đã thành thạo - Gợi ý câu hỏi khó hơn
        html += '<p style="margin-bottom: 10px; opacity: 0.95;">Xuất sắc! Bạn đã thành thạo chương này. Hãy thử thách bản thân với:</p>';
        html += '<div>';
        html += '<span class="recommendation-badge"><i class="fas fa-brain"></i> Câu hỏi phân tích sâu</span>';
        html += '<span class="recommendation-badge"><i class="fas fa-rocket"></i> Câu hỏi nâng cao</span>';
        html += '<span class="recommendation-badge"><i class="fas fa-trophy"></i> Ứng dụng thực tế</span>';
        html += '</div>';
        html += '<button class="btn-next-quiz" onclick="generateAdvancedQuiz(' + chapterId + ', \'hard\')"><i class="fas fa-fire"></i> Tạo câu hỏi nâng cao</button>';
        
    } else if (masteryScore >= 70) {
        // Tốt - Gợi ý câu hỏi củng cố
        html += '<p style="margin-bottom: 10px; opacity: 0.95;">Bạn đang học rất tốt! Tiếp tục củng cố với:</p>';
        html += '<div>';
        html += '<span class="recommendation-badge"><i class="fas fa-sync"></i> Ôn tập khái niệm</span>';
        html += '<span class="recommendation-badge"><i class="fas fa-arrow-up"></i> Tăng độ khó dần</span>';
        html += '<span class="recommendation-badge"><i class="fas fa-puzzle-piece"></i> Bài tập ứng dụng</span>';
        html += '</div>';
        html += '<button class="btn-next-quiz" onclick="generateAdvancedQuiz(' + chapterId + ', \'medium\')"><i class="fas fa-layer-group"></i> Tạo câu hỏi nâng cao hơn</button>';
        
    } else if (masteryScore >= 50) {
        // Khá - Gợi ý ôn lại và luyện tập
        html += '<p style="margin-bottom: 10px; opacity: 0.95;">Bạn đã nắm được cơ bản. Hãy luyện tập thêm với:</p>';
        html += '<div>';
        html += '<span class="recommendation-badge"><i class="fas fa-repeat"></i> Ôn lại kiến thức</span>';
        html += '<span class="recommendation-badge"><i class="fas fa-book-reader"></i> Đọc lại tóm tắt</span>';
        html += '<span class="recommendation-badge"><i class="fas fa-dumbbell"></i> Luyện tập cơ bản</span>';
        html += '</div>';
        html += '<button class="btn-next-quiz" onclick="generateAdvancedQuiz(' + chapterId + ', \'easy\')"><i class="fas fa-redo"></i> Làm lại với câu tương tự</button>';
        
    } else {
        // Cần cải thiện - Gợi ý học lại
        html += '<p style="margin-bottom: 10px; opacity: 0.95;">Đừng nản lòng! Hãy bắt đầu lại từ những điều cơ bản:</p>';
        html += '<div>';
        html += '<span class="recommendation-badge"><i class="fas fa-book"></i> Đọc lại tóm tắt</span>';
        html += '<span class="recommendation-badge"><i class="fas fa-list"></i> Học từng khái niệm</span>';
        html += '<span class="recommendation-badge"><i class="fas fa-step-forward"></i> Từng bước một</span>';
        html += '</div>';
        html += '<button class="btn-next-quiz" onclick="generateAdvancedQuiz(' + chapterId + ', \'easy\')"><i class="fas fa-play"></i> Bắt đầu với câu dễ hơn</button>';
    }
    
    html += '</div>';
    return html;
}

function generateAdvancedQuiz(chapterId, targetDifficulty) {
    showNotification('info', 'Đang tạo bộ câu hỏi ' + getDifficultyLabel(targetDifficulty) + '...');
    
    // Tạo số câu hỏi dựa trên difficulty
    var numQuestions = targetDifficulty === 'hard' ? 3 : 5;
    
    $.ajax({
        url: 'api/chapter/generate-qa',
        method: 'POST',
        data: { 
            chapterId: chapterId, 
            numQuestions: numQuestions,
            targetDifficulty: targetDifficulty
        },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                // Xóa bộ câu hỏi cũ
                $('#mcq-container-' + chapterId).remove();
                
                // Hiển thị bộ mới
                displayMCQs(chapterId, response.qas);
                
                // Scroll to new quiz
                $('html, body').animate({
                    scrollTop: $('#mcq-container-' + chapterId).offset().top - 100
                }, 500);
                
                showNotification('success', 'Đã tạo ' + response.count + ' câu hỏi ' + getDifficultyLabel(targetDifficulty) + '!');
            } else {
                showNotification('error', response.error || 'Lỗi tạo câu hỏi');
            }
        },
        error: function(xhr, status, error) {
            showNotification('error', 'Lỗi: ' + error);
        }
    });
}

function getDifficultyLabel(difficulty) {
    var labels = {
        'easy': 'dễ hơn',
        'medium': 'trung bình',
        'hard': 'nâng cao'
    };
    return labels[difficulty] || difficulty;
}

function getLevelFromScore(score) {
    if (score >= 85) {
        return { 
            icon: '🎓', 
            text: 'Xuất sắc - Bạn đã thành thạo!',
            analysis: 'Bạn đã nắm vững kiến thức chương này. Hãy tiếp tục duy trì và chuyển sang chương tiếp theo để mở rộng kiến thức.'
        };
    }
    if (score >= 70) {
        return { 
            icon: '✅', 
            text: 'Tốt - Tiếp tục phát huy!',
            analysis: 'Bạn đã hiểu khá tốt nội dung chương này. Hãy ôn lại một số khái niệm còn chưa chắc chắn để đạt mức độ thành thạo cao hơn.'
        };
    }
    if (score >= 50) {
        return { 
            icon: '📖', 
            text: 'Khá - Cần luyện tập thêm',
            analysis: 'Bạn đã nắm được một số khái niệm cơ bản. Nên đọc lại tóm tắt và làm thêm bài tập để củng cố kiến thức.'
        };
    }
    return { 
        icon: '💪', 
        text: 'Cần cải thiện - Hãy ôn lại kiến thức',
        analysis: 'Bạn cần dành thêm thời gian để đọc và hiểu nội dung chương này. Hãy tập trung vào các khái niệm cơ bản trước, sau đó làm lại bài tập.'
    };
}

function escapeHtml(text) {
    if (!text) return '';
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showNotification(type, message) {
    var bgColor = type === 'success' ? 'success' : (type === 'info' ? 'info' : 'danger');
    var notification = $('<div>')
        .addClass('alert alert-' + bgColor)
        .css({
            position: 'fixed',
            top: '20px',
            right: '20px',
            zIndex: 9999,
            minWidth: '300px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
        })
        .html('<i class="fas fa-' + (type === 'success' ? 'check-circle' : (type === 'info' ? 'info-circle' : 'exclamation-circle')) + '"></i> ' + message);
    
    $('body').append(notification);
    
    setTimeout(function() {
        notification.fadeOut(300, function() {
            $(this).remove();
        });
    }, 5000);
}
</script>

</body>
</html>