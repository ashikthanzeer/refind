package com.refind.model;

import com.refind.model.enums.ItemStatus;

import java.time.LocalDate;

public class FoundItem extends Item {

    private ItemStatus status;

    public FoundItem() {
        super();
    }

    public FoundItem(String title,
                     String description,
                     Category category,
                     String location,
                     LocalDate date,
                     String imagePath,
                     User reportedBy,
                     ItemStatus status) {
        super(title, description, category, location, date, imagePath, reportedBy);
        this.status = status;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }
}
