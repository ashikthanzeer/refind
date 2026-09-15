package com.refind.model;

import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ItemType;

import java.time.LocalDateTime;

public class LostItem extends Item {

    public LostItem() {
        super();
        setType(ItemType.LOST);
    }

    public LostItem(String title,
                    String description,
                    Category category,
                    Location location,
                    LocalDateTime reportedAt,
                    String imagePath,
                    User reportedBy) {
        super(title, description, ItemType.LOST, ItemStatus.LOST, category, location, reportedAt, imagePath, reportedBy);
    }
}
