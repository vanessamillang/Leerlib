package com.millanvanesa.leerlib_1;

public class Ticket {
    private String id;
    private String subject;
    private String status;

    public Ticket(String id, String subject, String status) {
        this.id = id;
        this.subject = subject;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
