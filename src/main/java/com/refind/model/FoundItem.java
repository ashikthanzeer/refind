package com.refind.model;

import com.refind.model.enums.ItemStatus;

import java.time.LocalDateTime;

public class FoundItem extends Item {

    public FoundItem() {
        super();
    }

    public FoundItem(String title,
                     String description,
                     Category category,
                     Location location,
                     ItemStatus status,
                     LocalDateTime reportedAt,
                     String imagePath,
                     User reportedBy) {
        super(title, description, category, location, status, reportedAt, imagePath, reportedBy);
    }
}
