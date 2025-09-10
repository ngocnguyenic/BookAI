<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*, model.Book" %>
<html>
<head>
    <title>Book Management</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css">
</head>
<body class="container mt-4">

<h2 class="mb-3">Book Management</h2>

<!-- FORM ADD NEW BOOK -->
<form action="bookcrud" method="post" class="mb-4">
    <input type="hidden" name="action" value="insert"/>
    <div class="row g-2">
        <div class="col-md-3">
            <input type="text" name="title" class="form-control" placeholder="Title" required>
        </div>
        <div class="col-md-2">
            <input type="text" name="author" class="form-control" placeholder="Author" required>
        </div>
        <div class="col-md-2">
            <input type="text" name="major" class="form-control" placeholder="Major" required>
        </div>
        <div class="col-md-3">
            <input type="text" name="description" class="form-control" placeholder="Description">
        </div>
        <div class="col-md-2">
            <button type="submit" class="btn btn-primary w-100">Add Book</button>
        </div>
    </div>
</form>

<!-- BOOK LIST TABLE -->
<table class="table table-bordered table-hover align-middle">
    <thead class="table-dark">
        <tr class="text-center">
            <th>ID</th>
            <th>Title</th>
            <th>Author</th>
            <th>Major</th>
            <th>Description</th>
            <th>Actions</th>
        </tr>
    </thead>
    <tbody>
    <%
        List<Book> listBook = (List<Book>) request.getAttribute("listBook");
        if (listBook != null && !listBook.isEmpty()) {
            for (Book b : listBook) {
    %>
        <tr>
            <td class="text-center"><%= b.getBookID() %></td>
            <td><%= b.getTitle() %></td>
            <td><%= b.getAuthor() %></td>
            <td><%= b.getMajor() %></td>
            <td><%= b.getDescription() %></td>
            <td class="text-center">
                <a href="bookcrud?action=edit&id=<%= b.getBookID() %>" class="btn btn-warning btn-sm">Edit</a>
                <a href="bookcrud?action=delete&id=<%= b.getBookID() %>" 
                   class="btn btn-danger btn-sm"
                   onclick="return confirm('Are you sure to delete this book?');">Delete</a>
            </td>
        </tr>
    <%
            }
        } else {
    %>
        <tr>
            <td colspan="6" class="text-center">No books found.</td>
        </tr>
    <%
        }
    %>
    </tbody>
</table>

<!-- AI Chat Button -->
<button class="btn btn-success position-fixed shadow" 
        style="bottom: 20px; right: 20px; z-index: 999;" 
        data-bs-toggle="modal" data-bs-target="#aiChatModal">
     AI Chat
</button>

<!-- AI Chat Modal -->
<div class="modal fade" id="aiChatModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header bg-primary text-white">
        <h5 class="modal-title">AI Assistant</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
      </div>
      <div class="modal-body">
        <form id="aiForm">
          <div class="mb-2">
            <input type="text" id="aiTitle" class="form-control" placeholder="Chapter Title" required>
          </div>
          <div class="mb-2">
            <textarea id="aiContent" class="form-control" rows="4" placeholder="Chapter Content" required></textarea>
          </div>
          <div class="mb-2">
            <select id="aiAction" class="form-select">
              <option value="summary">Summary</option>
              <option value="qa">Q&A</option>
            </select>
          </div>
          <button type="submit" class="btn btn-primary w-100">Ask AI</button>
        </form>
        <hr>
        <div id="aiResult" class="p-2 border rounded bg-light" 
             style="min-height: 80px; white-space: pre-wrap;">
          Result will appear here...
        </div>
      </div>
    </div>
  </div>
</div>

<!-- Bootstrap JS + Fetch Logic -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
document.getElementById("aiForm").addEventListener("submit", async function(e) {
    e.preventDefault();

    const title = document.getElementById("aiTitle").value;
    const content = document.getElementById("aiContent").value;
    const action = document.getElementById("aiAction").value;

    const params = new URLSearchParams();
    params.append("action", action);
    params.append("title", title);
    params.append("content", content);

    document.getElementById("aiResult").innerText = "Loading...";

    try {
        const res = await fetch("aiservlet", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: params
        });
        const text = await res.text();
        document.getElementById("aiResult").innerText = text;
    } catch (err) {
        document.getElementById("aiResult").innerText = "Error: " + err;
    }
});
</script>

</body>
</html>
