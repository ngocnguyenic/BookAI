package model;

public class Book {
    private int bookID;
    private String title;
    private String author;
    private String major;      
    private String description;

    public Book() {}

    public Book(int bookID, String title, String author, String major, String description) {
        this.bookID = bookID;
        this.title = title;
        this.author = author;
        this.major = major;
        this.description = description;
    }

    public Book(String title, String author, String major, String description) {
        this.title = title;
        this.author = author;
        this.major = major;
        this.description = description;
    }

    public int getBookID() {
        return bookID;
    }

    public void setBookID(int bookID) {
        this.bookID = bookID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    
    @Override
    public String toString() {
        return "Book{" +
                "bookID=" + bookID +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", major='" + major + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
