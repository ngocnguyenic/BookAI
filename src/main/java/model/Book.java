package model;

public class Book {
    private int bookID;
    private String title;
    private String author;
    private String description; 
    private String major;
    private String filePath;
   
    public Book(int bookID, String title, String author, String description, String major, String filePath) {
        this.bookID = bookID;
        this.title = title;
        this.author = author;
        this.description = description;
        this.major = major;
        this.filePath = filePath;
    }
    public Book(String title, String author, String major, String description) {
        this.title = title;
        this.author = author;
        this.major = major;
        this.description = description;
    }

    public Book(int bookID, String title, String author, String major, String description) {
        this.bookID = bookID;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    @Override
    public String toString() {
        return "Book{" +
                "bookID=" + bookID +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", description='" + description + '\'' +
                ", major='" + major + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}