package com.refind.ui;

import com.refind.model.Item;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FoundItemTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {
            "ID", "Title", "Category", "Location", "Reported By", "Reported At", "Status"
    };

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final List<Item> items = new ArrayList<>();

    public void setItems(List<Item> newItems) {
        items.clear();

        if (newItems != null) {
            items.addAll(newItems);
        }

        fireTableDataChanged();
    }

    public Item getItemAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < items.size()) {
            return items.get(rowIndex);
        }

        return null;
    }

    @Override
    public int getRowCount() {
        return items.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Item item = items.get(rowIndex);

        if (item == null) {
            return "";
        }

        switch (columnIndex) {
            case 0:
                return item.getId();

            case 1:
                return item.getTitle();

            case 2:
                if (item.getCategory() != null) {
                    return item.getCategory().getName() != null
                            ? item.getCategory().getName()
                            : "Category #" + item.getCategory().getId();
                }
                return "-";

            case 3:
                if (item.getLocation() != null) {
                    StringBuilder loc = new StringBuilder();

                    if (item.getLocation().getBuilding() != null
                            && !item.getLocation().getBuilding().isBlank()) {
                        loc.append(item.getLocation().getBuilding());
                    }

                    if (item.getLocation().getRoom() != null
                            && !item.getLocation().getRoom().isBlank()) {

                        if (loc.length() > 0) {
                            loc.append(", ");
                        }

                        loc.append("Room ")
                                .append(item.getLocation().getRoom());
                    }

                    if (loc.length() == 0
                            && item.getLocation().getCampus() != null) {
                        loc.append(item.getLocation().getCampus());
                    }

                    return loc.length() > 0
                            ? loc.toString()
                            : "Location #" + item.getLocation().getId();
                }

                return "-";

            case 4:
                if (item.getReportedBy() != null) {
                    return item.getReportedBy().getName() != null
                            ? item.getReportedBy().getName()
                            : "User #" + item.getReportedBy().getId();
                }

                return "-";

            case 5:
                return item.getReportedAt() != null
                        ? item.getReportedAt().format(FORMATTER)
                        : "-";

            case 6:
                return item.getStatus() != null
                        ? item.getStatus().name()
                        : "";

            default:
                return "";
        }
    }
}