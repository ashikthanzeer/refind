package com.refind.model;

import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ItemType;

import java.time.LocalDateTime;

public class FoundItem extends Item {

    public FoundItem() {
        super();
        setType(ItemType.FOUND);
    }

    public FoundItem(String title,
                     String description,
                     Category category,
                     Location location,
                     LocalDateTime reportedAt,
                     String imagePath,
                     User reportedBy,
                     ItemStatus status) {
        super(title, description, ItemType.FOUND, status, category, location, reportedAt, imagePath, reportedBy);
    }
}
