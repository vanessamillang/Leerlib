package com.millanvanesa.leerlib_1;

public class SupportTicket {
    private String id;
    private String subject;
    private String status;
    private String problem; // Cambiado de description a problem

    public SupportTicket() {
        // Constructor vacío requerido por Firestore
    }

    public SupportTicket(String id, String subject, String status, String problem) {
        this.id = id;
        this.subject = subject;
        this.status = status;
        this.problem = problem;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }
}
