package com.millanvanesa.leerlib_1;

public class Comment {
    private String username;
    private String comentario;
    private String userImageUrl;

    public Comment() {
        // Constructor vacío necesario para Firebase
    }

    public Comment(String username, String comentario, String userImageUrl) {
        this.username = username;
        this.comentario = comentario;
        this.userImageUrl = userImageUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getUserImageUrl() {
        return userImageUrl;
    }

    public void setUserImageUrl(String userImageUrl) {
        this.userImageUrl = userImageUrl;
    }
}
