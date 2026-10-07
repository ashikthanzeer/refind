package com.refind.ui;

import com.refind.model.Location;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class LocationTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"ID", "Campus", "Building", "Room"};
    private final List<Location> locations = new ArrayList<>();

    public void setLocations(List<Location> newLocations) {
        locations.clear();
        if (newLocations != null) locations.addAll(newLocations);
        fireTableDataChanged();
    }

    public Location getLocationAt(int row) {
        return row >= 0 && row < locations.size() ? locations.get(row) : null;
    }

    @Override public int getRowCount() { return locations.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int column) { return COLUMNS[column]; }

    @Override
    public Object getValueAt(int row, int column) {
        Location location = locations.get(row);
        return switch (column) {
            case 0 -> location.getId();
            case 1 -> location.getCampus();
            case 2 -> location.getBuilding();
            case 3 -> location.getRoom();
            default -> "";
        };
    }
}
