package com.millanvanesa.leerlib_1;

public class RatingItem {
    private String title;
    private String imageUrl;
    private String bookId;
    private boolean isAuthor;
    private boolean isCategory;
    private boolean isInProgress; // Campo para "Seguir Leyendo"
    private boolean isNew; // Nuevo campo para "Más Nuevo"

    // Constructor para libros con múltiples recomendaciones
    public RatingItem(String title, String imageUrl, String bookId, boolean isAuthor, boolean isCategory, boolean isInProgress, boolean isNew) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.bookId = bookId;
        this.isAuthor = isAuthor;
        this.isCategory = isCategory;
        this.isInProgress = isInProgress;
        this.isNew = isNew;
    }

    // Constructor para categorías
    public RatingItem(String title, String imageUrl, boolean isCategory) {
        this(title, imageUrl, null, false, isCategory, false, false);
    }

    // Constructor para libros recomendados por "Más Nuevo"
    public RatingItem(String title, String imageUrl, String bookId, boolean isNew) {
        this(title, imageUrl, bookId, false, false, false, isNew);
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getBookId() {
        return bookId;
    }

    public boolean isAuthor() {
        return isAuthor;
    }

    public boolean isCategory() {
        return isCategory;
    }

    public boolean isInProgress() {
        return isInProgress;
    }

    public boolean isNew() {
        return isNew;
    }
}
