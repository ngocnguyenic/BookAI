<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>📤 Upload Sách PDF</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
    <style>
        .error-message {
            color: #dc3545;
            font-size: 0.875rem;
            margin-top: 0.25rem;
            display: none;
        }
        .error-message.show {
            display: block;
        }
        .form-control.is-invalid {
            border-color: #dc3545;
            background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 12 12' width='12' height='12' fill='none' stroke='%23dc3545'%3e%3ccircle cx='6' cy='6' r='4.5'/%3e%3cpath stroke-linejoin='round' d='M5.8 3.6h.4L6 6.5z'/%3e%3ccircle cx='6' cy='8.2' r='.6' fill='%23dc3545' stroke='none'/%3e%3c/svg%3e");
            background-repeat: no-repeat;
            background-position: right calc(0.375em + 0.1875rem) center;
            background-size: calc(0.75em + 0.375rem) calc(0.75em + 0.375rem);
            padding-right: calc(1.5em + 0.75rem);
        }
        .form-control.is-valid {
            border-color: #198754;
            background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 8 8'%3e%3cpath fill='%23198754' d='M2.3 6.73L.6 4.53c-.4-1.04.46-1.4 1.1-.8l1.1 1.4 3.4-3.8c.6-.63 1.6-.27 1.2.7l-4 4.6c-.43.5-.8.4-1.1.1z'/%3e%3c/svg%3e");
            background-repeat: no-repeat;
            background-position: right calc(0.375em + 0.1875rem) center;
            background-size: calc(0.75em + 0.375rem) calc(0.75em + 0.375rem);
            padding-right: calc(1.5em + 0.75rem);
        }
        .char-counter {
            font-size: 0.75rem;
            color: #6c757d;
            float: right;
            margin-top: 0.25rem;
        }
        .char-counter.warning {
            color: #ffc107;
            font-weight: bold;
        }
        .char-counter.danger {
            color: #dc3545;
            font-weight: bold;
        }
        .file-info {
            font-size: 0.875rem;
            color: #6c757d;
            margin-top: 0.5rem;
        }
        .alert-danger ul {
            margin-bottom: 0;
            padding-left: 1.5rem;
        }
    </style>
</head>
<body class="container mt-4">
    <div class="row justify-content-center">
        <div class="col-lg-8">
            <h2 class="mb-4">📤 Upload Sách từ File PDF</h2>
            
            <!-- Error Messages -->
            <% if (request.getAttribute("error") != null) { %>
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    <strong>❌ Lỗi:</strong><br>
                    <%= request.getAttribute("error") %>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            <% } %>
            
            <!-- Success Messages -->
            <% if (session.getAttribute("successMessage") != null) { %>
                <div class="alert alert-success alert-dismissible fade show" role="alert">
                    <%= session.getAttribute("successMessage") %>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
                <% session.removeAttribute("successMessage"); %>
            <% } %>
            
            <form id="uploadForm" action="bookcrud?action=upload" method="post" 
                  enctype="multipart/form-data" class="border p-4 rounded shadow-sm bg-white" novalidate>
                
                <!-- Title -->
                <div class="mb-3">
                    <label for="title" class="form-label">
                        Tiêu đề sách <span class="text-danger">*</span>
                    </label>
                    <input type="text" id="title" name="title" class="form-control" 
                           maxlength="200" required>
                    <div class="char-counter">
                        <span id="titleCount">0</span>/200
                    </div>
                    <div class="error-message" id="titleError"></div>
                </div>
                
                <!-- Author -->
                <div class="mb-3">
                    <label for="author" class="form-label">
                        Tác giả <span class="text-danger">*</span>
                    </label>
                    <input type="text" id="author" name="author" class="form-control" 
                           maxlength="100" required>
                    <div class="char-counter">
                        <span id="authorCount">0</span>/100
                    </div>
                    <div class="error-message" id="authorError"></div>
                </div>
                
                <!-- Major -->
                <div class="mb-3">
                    <label for="major" class="form-label">Chuyên ngành</label>
                    <input type="text" id="major" name="major" class="form-control" 
                           maxlength="100">
                    <div class="char-counter">
                        <span id="majorCount">0</span>/100
                    </div>
                    <div class="error-message" id="majorError"></div>
                    <small class="form-text text-muted">Ví dụ: Khoa học máy tính, Điện tử viễn thông...</small>
                </div>
                
                <!-- Description -->
                <div class="mb-3">
                    <label for="description" class="form-label">Mô tả</label>
                    <textarea id="description" name="description" class="form-control" 
                              rows="3" maxlength="1000"></textarea>
                    <div class="char-counter">
                        <span id="descriptionCount">0</span>/1000
                    </div>
                    <div class="error-message" id="descriptionError"></div>
                    <small class="form-text text-muted">Mô tả ngắn gọn về nội dung sách</small>
                </div>
                
                <!-- PDF File -->
                <div class="mb-4">
                    <label for="pdfFile" class="form-label">
                        Chọn file PDF <span class="text-danger">*</span>
                    </label>
                    <input type="file" id="pdfFile" name="pdfFile" 
                           class="form-control" accept=".pdf" required>
                    <div class="file-info" id="fileInfo"></div>
                    <div class="error-message" id="fileError"></div>
                    <small class="form-text text-muted">
                        Chấp nhận file PDF tối đa 50MB. Hệ thống sẽ tự động phát hiện chương.
                    </small>
                </div>
                
                <!-- Submit Buttons -->
                <div class="d-flex gap-2">
                    <button type="submit" class="btn btn-success" id="submitBtn">
                        <span id="submitText">📤 Upload & Xử lý bằng AI</span>
                        <span id="submitSpinner" class="spinner-border spinner-border-sm d-none" role="status">
                            <span class="visually-hidden">Đang xử lý...</span>
                        </span>
                    </button>
                    <a href="bookcrud?action=list" class="btn btn-secondary">❌ Hủy</a>
                </div>
            </form>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Validation config
        const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
        const DANGEROUS_CHARS = /[<>"'&]/;
        
        // Get form elements
        const form = document.getElementById('uploadForm');
        const titleInput = document.getElementById('title');
        const authorInput = document.getElementById('author');
        const majorInput = document.getElementById('major');
        const descriptionInput = document.getElementById('description');
        const pdfFileInput = document.getElementById('pdfFile');
        const submitBtn = document.getElementById('submitBtn');
        
        // Character counters
        function setupCharCounter(input, counterId, max) {
            const counter = document.getElementById(counterId);
            input.addEventListener('input', function() {
                const count = this.value.length;
                counter.textContent = count;
                
                const parent = counter.parentElement;
                parent.classList.remove('warning', 'danger');
                
                if (count > max * 0.9) {
                    parent.classList.add('danger');
                } else if (count > max * 0.7) {
                    parent.classList.add('warning');
                }
            });
        }
        
        setupCharCounter(titleInput, 'titleCount', 200);
        setupCharCounter(authorInput, 'authorCount', 100);
        setupCharCounter(majorInput, 'majorCount', 100);
        setupCharCounter(descriptionInput, 'descriptionCount', 1000);
        
        // Field validation functions
        function validateTitle() {
            const value = titleInput.value.trim();
            const errorDiv = document.getElementById('titleError');
            
            if (value.length === 0) {
                showError(titleInput, errorDiv, '❌ Tiêu đề không được để trống');
                return false;
            }
            
            if (value.length > 200) {
                showError(titleInput, errorDiv, '❌ Tiêu đề quá dài (tối đa 200 ký tự)');
                return false;
            }
            
            if (DANGEROUS_CHARS.test(value)) {
                showError(titleInput, errorDiv, '⚠️ Tiêu đề chứa ký tự không hợp lệ: < > " \' &');
                return false;
            }
            
            showValid(titleInput, errorDiv);
            return true;
        }
        
        function validateAuthor() {
            const value = authorInput.value.trim();
            const errorDiv = document.getElementById('authorError');
            
            if (value.length === 0) {
                showError(authorInput, errorDiv, '❌ Tác giả không được để trống');
                return false;
            }
            
            if (value.length > 100) {
                showError(authorInput, errorDiv, '❌ Tên tác giả quá dài (tối đa 100 ký tự)');
                return false;
            }
            
            if (DANGEROUS_CHARS.test(value)) {
                showError(authorInput, errorDiv, '⚠️ Tên tác giả chứa ký tự không hợp lệ');
                return false;
            }
            
            showValid(authorInput, errorDiv);
            return true;
        }
        
        function validateMajor() {
            const value = majorInput.value.trim();
            const errorDiv = document.getElementById('majorError');
            
            if (value.length > 100) {
                showError(majorInput, errorDiv, '❌ Chuyên ngành quá dài (tối đa 100 ký tự)');
                return false;
            }
            
            if (value.length > 0) {
                showValid(majorInput, errorDiv);
            } else {
                clearValidation(majorInput, errorDiv);
            }
            return true;
        }
        
        function validateDescription() {
            const value = descriptionInput.value.trim();
            const errorDiv = document.getElementById('descriptionError');
            
            if (value.length > 1000) {
                showError(descriptionInput, errorDiv, '❌ Mô tả quá dài (tối đa 1000 ký tự)');
                return false;
            }
            
            if (value.length > 0) {
                showValid(descriptionInput, errorDiv);
            } else {
                clearValidation(descriptionInput, errorDiv);
            }
            return true;
        }
        
        function validateFile() {
            const file = pdfFileInput.files[0];
            const errorDiv = document.getElementById('fileError');
            const infoDiv = document.getElementById('fileInfo');
            
            if (!file) {
                showError(pdfFileInput, errorDiv, '❌ Vui lòng chọn file PDF');
                infoDiv.textContent = '';
                return false;
            }
            
            // Check file type
            if (!file.name.toLowerCase().endsWith('.pdf')) {
                showError(pdfFileInput, errorDiv, '❌ Chỉ chấp nhận file PDF');
                infoDiv.textContent = '';
                return false;
            }
            
            // Check file size
            if (file.size > MAX_FILE_SIZE) {
                const sizeMB = (file.size / 1024 / 1024).toFixed(2);
                showError(pdfFileInput, errorDiv, `❌ File quá lớn (${sizeMB}MB). Tối đa 50MB`);
                infoDiv.textContent = '';
                return false;
            }
            
            // Show file info
            const sizeMB = (file.size / 1024 / 1024).toFixed(2);
            infoDiv.innerHTML = `✅ <strong>${file.name}</strong> (${sizeMB} MB)`;
            showValid(pdfFileInput, errorDiv);
            return true;
        }
        
        // UI helper functions
        function showError(input, errorDiv, message) {
            input.classList.remove('is-valid');
            input.classList.add('is-invalid');
            errorDiv.textContent = message;
            errorDiv.classList.add('show');
        }
        
        function showValid(input, errorDiv) {
            input.classList.remove('is-invalid');
            input.classList.add('is-valid');
            errorDiv.classList.remove('show');
        }
        
        function clearValidation(input, errorDiv) {
            input.classList.remove('is-valid', 'is-invalid');
            errorDiv.classList.remove('show');
        }
        
        // Attach blur validation
        titleInput.addEventListener('blur', validateTitle);
        authorInput.addEventListener('blur', validateAuthor);
        majorInput.addEventListener('blur', validateMajor);
        descriptionInput.addEventListener('blur', validateDescription);
        pdfFileInput.addEventListener('change', validateFile);
        
        // Form submission
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            
            // Validate all fields
            const isTitleValid = validateTitle();
            const isAuthorValid = validateAuthor();
            const isMajorValid = validateMajor();
            const isDescriptionValid = validateDescription();
            const isFileValid = validateFile();
            
            if (isTitleValid && isAuthorValid && isMajorValid && isDescriptionValid && isFileValid) {
                // Show loading state
                submitBtn.disabled = true;
                document.getElementById('submitText').classList.add('d-none');
                document.getElementById('submitSpinner').classList.remove('d-none');
                
                // Submit form
                this.submit();
            } else {
                // Scroll to first error
                const firstError = document.querySelector('.is-invalid');
                if (firstError) {
                    firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    firstError.focus();
                }
            }
        });
    </script>
</body>
</html>