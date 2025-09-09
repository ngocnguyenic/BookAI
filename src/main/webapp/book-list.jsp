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
<table class="table table-bordered table-hover">
    <thead class="table-dark">
        <tr>
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
        // Lấy danh sách sách từ servlet
        List<Book> listBook = (List<Book>) request.getAttribute("listBook");
        if (listBook != null) {
            for (Book b : listBook) {
    %>
        <tr>
            <td><%= b.getBookID() %></td>
            <td><%= b.getTitle() %></td>
            <td><%= b.getAuthor() %></td>
            <td><%= b.getMajor() %></td>
            <td><%= b.getDescription() %></td>
            <td>
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

</body>
</html>
