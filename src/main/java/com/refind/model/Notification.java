package com.refind.model;

import java.time.LocalDateTime;

public class Notification {
    private Long id;
    private User user;
    private String text;
    private boolean readFlag;
    private LocalDateTime createdAt;

    public Notification() {}
    public Notification(User user, String text, boolean readFlag, LocalDateTime createdAt) {
        this.user = user;
        this.text = text;
        this.readFlag = readFlag;
        this.createdAt = createdAt;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isReadFlag() { return readFlag; }
    public void setReadFlag(boolean readFlag) { this.readFlag = readFlag; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
