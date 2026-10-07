package com.refind.ui;

import com.refind.model.User;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"ID", "Name", "Email", "Role", "Created At"};
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final List<User> users = new ArrayList<>();

    public void setUsers(List<User> newUsers) {
        users.clear();
        if (newUsers != null) users.addAll(newUsers);
        fireTableDataChanged();
    }

    public User getUserAt(int row) {
        return row >= 0 && row < users.size() ? users.get(row) : null;
    }

    @Override public int getRowCount() { return users.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int column) { return COLUMNS[column]; }

    @Override
    public Object getValueAt(int row, int column) {
        User user = users.get(row);
        return switch (column) {
            case 0 -> user.getId();
            case 1 -> user.getName();
            case 2 -> user.getEmail();
            case 3 -> user.getRole() != null ? user.getRole().name() : "";
            case 4 -> user.getCreatedAt() != null
                    ? user.getCreatedAt().format(FORMATTER) : "-";
            default -> "";
        };
    }
}
