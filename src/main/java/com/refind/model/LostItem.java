package com.refind.model;

import java.time.LocalDate;

public class LostItem extends Item {

    public LostItem() {
        super();
    }

    public LostItem(String title,
                    String description,
                    Category category,
                    String location,
                    LocalDate date,
                    String imagePath,
                    User reportedBy) {
        super(title, description, category, location, date, imagePath, reportedBy);
    }
}
