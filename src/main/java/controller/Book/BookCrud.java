package controller.Book;

import dao.BookDAO;
import model.Book;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

public class BookCrud extends HttpServlet {

    private BookDAO bookDAO;

    @Override
    public void init() throws ServletException {
        bookDAO = new BookDAO();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");
        if (action == null) action = "list";

        switch (action) {
            case "new":
                showNewForm(request, response);
                break;
            case "insert":
                insertBook(request, response);
                break;
            case "delete":
                deleteBook(request, response);
                break;
            case "edit":
                showEditForm(request, response);
                break;
            case "update":
                updateBook(request, response);
                break;
            case "list":
            default:
                listBooks(request, response);
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    private void listBooks(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Book> listBook = bookDAO.getAllBooks();
        request.setAttribute("listBook", listBook);
        request.getRequestDispatcher("book-list.jsp").forward(request, response);
    }

    private void showNewForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("book-form.jsp").forward(request, response);
    }

    private void insertBook(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String title = request.getParameter("title");
        String author = request.getParameter("author");
        String isbn = request.getParameter("major");
        String description = request.getParameter("description");

        Book newBook = new Book(title, author, isbn, description);
        bookDAO.insertBook(newBook);
        response.sendRedirect("bookcrud?action=list");
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int id = Integer.parseInt(request.getParameter("id"));
        Book existingBook = bookDAO.getBookById(id);
        request.setAttribute("book", existingBook);
        request.getRequestDispatcher("book-form.jsp").forward(request, response);
    }

    private void updateBook(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int id = Integer.parseInt(request.getParameter("id"));
        String title = request.getParameter("title");
        String author = request.getParameter("author");
        String isbn = request.getParameter("major");
        String description = request.getParameter("description");

        Book book = new Book(id, title, author, isbn, description);
        bookDAO.updateBook(book);
        response.sendRedirect("bookcrud?action=list");
    }

    private void deleteBook(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int id = Integer.parseInt(request.getParameter("id"));
        bookDAO.deleteBook(id);
        response.sendRedirect("bookcrud?action=list");
    }
}
