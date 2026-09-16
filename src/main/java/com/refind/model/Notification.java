package com.refind.model;

import java.time.LocalDateTime;

public class Notification {
    private Long id;
    private User user;
    private String text;
    private boolean read;
    private LocalDateTime createdAt;

    public Notification() {}
    public Notification(User user, String text, boolean read, LocalDateTime createdAt) {
        this.user = user;
        this.text = text;
        this.read = read;
        this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
