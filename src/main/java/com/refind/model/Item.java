package com.refind.model;

import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ItemType;

import java.time.LocalDateTime;

public class Item {
    private Long id;
    private String title;
    private String description;
    private ItemType type;
    private ItemStatus status;
    private Category category;
    private Location location;
    private LocalDateTime reportedAt;
    private String imagePath;
    private User reportedBy;

    public Item() {
    }

    public Item(String title, String description, ItemType type, ItemStatus status,
                Category category, Location location, LocalDateTime reportedAt,
                String imagePath, User reportedBy) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.status = status;
        this.category = category;
        this.location = location;
        this.reportedAt = reportedAt;
        this.imagePath = imagePath;
        this.reportedBy = reportedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public User getReportedBy() { return reportedBy; }
    public void setReportedBy(User reportedBy) { this.reportedBy = reportedBy; }
}
