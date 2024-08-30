package com.millanvanesa.leerlib_1;

public class Book {
    private String uid;
    private String nombre;
    private String imagenUrl;

    // Constructor vacío necesario para Firestore
    public Book() {}

    public Book(String uid, String nombre, String imagenUrl) {
        this.uid = uid;
        this.nombre = nombre;
        this.imagenUrl = imagenUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }
}
