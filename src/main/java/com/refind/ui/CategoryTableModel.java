package com.refind.ui;

import com.refind.model.Category;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class CategoryTableModel extends AbstractTableModel {

    private static final String[] COLUMNS = {"ID", "Category Name"};
    private final List<Category> categories = new ArrayList<>();

    public void setCategories(List<Category> newCategories) {
        categories.clear();
        if (newCategories != null) categories.addAll(newCategories);
        fireTableDataChanged();
    }

    public Category getCategoryAt(int row) {
        return row >= 0 && row < categories.size() ? categories.get(row) : null;
    }

    @Override public int getRowCount() { return categories.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int column) { return COLUMNS[column]; }

    @Override
    public Object getValueAt(int row, int column) {
        Category category = categories.get(row);
        return column == 0 ? category.getId() : category.getName();
    }
}
