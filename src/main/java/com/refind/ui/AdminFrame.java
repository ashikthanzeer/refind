package com.refind.ui;

import com.refind.exception.ValidationException;
import com.refind.model.Category;
import com.refind.model.Location;
import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.controller.AdminController;
import com.refind.service.CategoryService;
import com.refind.service.LocationService;
import com.refind.service.UserService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * Administrator UI for user, category and location management.
 * All operations are delegated to AdminController.
 */
public class AdminFrame extends JFrame implements SessionContext.SessionListener {

    private final AdminController controller;
    private final SessionContext sessionContext;

    private JTable userTable;
    private JTable categoryTable;
    private JTable locationTable;

    private UserTableModel userTableModel;
    private CategoryTableModel categoryTableModel;
    private LocationTableModel locationTableModel;

    public AdminFrame(UserService userService,
                      CategoryService categoryService,
                      LocationService locationService,
                      SessionContext sessionContext) {
        super("ReFind - Admin Management");
        this.controller = new AdminController(userService, categoryService, locationService);
        this.sessionContext = sessionContext;

        sessionContext.addSessionListener(this);

        initWindow();
        refreshAll();
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1050, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_LIGHT);
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_BOLD);
        tabs.setBackground(UITheme.BG_LIGHT);

        tabs.addTab("Users", buildUsersPanel());
        tabs.addTab("Categories", buildCategoriesPanel());
        tabs.addTab("Locations", buildLocationsPanel());

        root.add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(UITheme.CARD_BG);
        header.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER, 1),
                new EmptyBorder(14, 24, 14, 24)
        ));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setBackground(UITheme.CARD_BG);

        JLabel title = new JLabel("ReFind");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UITheme.PRIMARY);

        JLabel subtitle = new JLabel("|  Admin Management");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        brand.add(title);
        brand.add(subtitle);
        header.add(brand, BorderLayout.WEST);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        userPanel.setBackground(UITheme.CARD_BG);

        User user = sessionContext.getCurrentUser();
        JLabel userLabel = new JLabel(user != null
                ? user.getName() + " [" + user.getRole() + "]"
                : "No active user");
        userLabel.setFont(UITheme.FONT_BOLD);
        userLabel.setForeground(UITheme.DANGER);

        JButton close = UITheme.createSecondaryButton("Close");
        close.addActionListener(e -> dispose());

        userPanel.add(userLabel);
        userPanel.add(close);
        header.add(userPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel buildUsersPanel() {
        JPanel panel = createManagementPanel("User Management",
                "View, update roles and remove user accounts.");

        userTableModel = new UserTableModel();
        userTable = createTable(userTableModel);
        userTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(240);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(100);

        panel.add(new JScrollPane(userTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(UITheme.BG_LIGHT);

        JButton refresh = UITheme.createSecondaryButton("Refresh");
        JButton edit = UITheme.createPrimaryButton("Edit User");
        JButton delete = UITheme.createDangerButton("Delete User");

        refresh.addActionListener(e -> refreshUsers());
        edit.addActionListener(e -> editSelectedUser());
        delete.addActionListener(e -> deleteSelectedUser());

        actions.add(refresh);
        actions.add(edit);
        actions.add(delete);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildCategoriesPanel() {
        JPanel panel = createManagementPanel("Category Management",
                "Add, rename or remove item categories.");

        categoryTableModel = new CategoryTableModel();
        categoryTable = createTable(categoryTableModel);

        panel.add(new JScrollPane(categoryTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(UITheme.BG_LIGHT);

        JButton add = UITheme.createPrimaryButton("Add Category");
        JButton edit = UITheme.createSecondaryButton("Edit Category");
        JButton delete = UITheme.createDangerButton("Delete Category");
        JButton refresh = UITheme.createSecondaryButton("Refresh");

        add.addActionListener(e -> addCategory());
        edit.addActionListener(e -> editCategory());
        delete.addActionListener(e -> deleteCategory());
        refresh.addActionListener(e -> refreshCategories());

        actions.add(refresh);
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildLocationsPanel() {
        JPanel panel = createManagementPanel("Location Management",
                "Add, edit or remove campus locations.");

        locationTableModel = new LocationTableModel();
        locationTable = createTable(locationTableModel);

        panel.add(new JScrollPane(locationTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(UITheme.BG_LIGHT);

        JButton add = UITheme.createPrimaryButton("Add Location");
        JButton edit = UITheme.createSecondaryButton("Edit Location");
        JButton delete = UITheme.createDangerButton("Delete Location");
        JButton refresh = UITheme.createSecondaryButton("Refresh");

        add.addActionListener(e -> addLocation());
        edit.addActionListener(e -> editLocation());
        delete.addActionListener(e -> deleteLocation());
        refresh.addActionListener(e -> refreshLocations());

        actions.add(refresh);
        actions.add(add);
        actions.add(edit);
        actions.add(delete);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createManagementPanel(String titleText, String subtitleText) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UITheme.BG_LIGHT);

        JLabel title = new JLabel(titleText);
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        top.add(title, BorderLayout.NORTH);
        top.add(subtitle, BorderLayout.SOUTH);

        panel.add(top, BorderLayout.NORTH);
        return panel;
    }

    private JTable createTable(javax.swing.table.TableModel model) {
        JTable table = new JTable(model);
        table.setFont(UITheme.FONT_BODY);
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(UITheme.FONT_BOLD);
        table.getTableHeader().setBackground(UITheme.BG_LIGHT);
        table.setShowGrid(true);
        table.setGridColor(UITheme.BORDER);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        return table;
    }

    private User activeUser() {
        return sessionContext.getCurrentUser();
    }

    private void refreshAll() {
        try {
            controller.requireAdmin(activeUser());
            refreshUsers();
            refreshCategories();
            refreshLocations();
        } catch (Exception ex) {
            showError(ex);
            dispose();
        }
    }

    private void refreshUsers() {
        try {
            userTableModel.setUsers(controller.getUsers(activeUser()));
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void refreshCategories() {
        try {
            categoryTableModel.setCategories(controller.getCategories(activeUser()));
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void refreshLocations() {
        try {
            locationTableModel.setLocations(controller.getLocations(activeUser()));
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void editSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            showInfo("Select a user first.");
            return;
        }

        User user = userTableModel.getUserAt(row);
        JTextField name = UITheme.createTextField(25);
        JTextField email = UITheme.createTextField(25);
        JComboBox<Role> role = new JComboBox<>(Role.values());

        name.setText(user.getName());
        email.setText(user.getEmail());
        role.setSelectedItem(user.getRole());

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.add(new JLabel("Name"));
        form.add(name);
        form.add(new JLabel("Email"));
        form.add(email);
        form.add(new JLabel("Role"));
        form.add(role);

        int result = JOptionPane.showConfirmDialog(this, form, "Edit User",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            user.setName(name.getText().trim());
            user.setEmail(email.getText().trim());
            user.setRole((Role) role.getSelectedItem());

            controller.updateUser(activeUser(), user);
            refreshUsers();
            showInfo("User updated successfully.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void deleteSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) {
            showInfo("Select a user first.");
            return;
        }

        User user = userTableModel.getUserAt(row);
        int result = JOptionPane.showConfirmDialog(this,
                "Delete user \"" + user.getName() + "\"?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (result != JOptionPane.YES_OPTION) return;

        try {
            controller.deleteUser(activeUser(), user.getId());
            refreshUsers();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void addCategory() {
        JTextField name = UITheme.createTextField(25);
        if (JOptionPane.showConfirmDialog(this, name, "Add Category",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try {
            controller.createCategory(activeUser(), name.getText());
            refreshCategories();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void editCategory() {
        int row = categoryTable.getSelectedRow();
        if (row < 0) {
            showInfo("Select a category first.");
            return;
        }

        Category category = categoryTableModel.getCategoryAt(row);
        JTextField name = UITheme.createTextField(25);
        name.setText(category.getName());

        if (JOptionPane.showConfirmDialog(this, name, "Edit Category",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try {
            category.setName(name.getText().trim());
            controller.updateCategory(activeUser(), category);
            refreshCategories();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void deleteCategory() {
        int row = categoryTable.getSelectedRow();
        if (row < 0) {
            showInfo("Select a category first.");
            return;
        }

        Category category = categoryTableModel.getCategoryAt(row);
        if (JOptionPane.showConfirmDialog(this,
                "Delete category \"" + category.getName() + "\"?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        try {
            controller.deleteCategory(activeUser(), category.getId());
            refreshCategories();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void addLocation() {
        JTextField campus = UITheme.createTextField(20);
        JTextField building = UITheme.createTextField(20);
        JTextField room = UITheme.createTextField(20);

        JPanel form = locationForm(campus, building, room);

        if (JOptionPane.showConfirmDialog(this, form, "Add Location",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        try {
            controller.createLocation(activeUser(), campus.getText(), building.getText(), room.getText());
            refreshLocations();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void editLocation() {
        int row = locationTable.getSelectedRow();
        if (row < 0) {
            showInfo("Select a location first.");
            return;
        }

        Location location = locationTableModel.getLocationAt(row);

        JTextField campus = UITheme.createTextField(20);
        JTextField building = UITheme.createTextField(20);
        JTextField room = UITheme.createTextField(20);

        campus.setText(location.getCampus());
        building.setText(location.getBuilding());
        room.setText(location.getRoom());

        JPanel form = locationForm(campus, building, room);

        if (JOptionPane.showConfirmDialog(this, form, "Edit Location",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        try {
            location.setCampus(campus.getText().trim());
            location.setBuilding(building.getText().trim());
            location.setRoom(room.getText().trim());

            controller.updateLocation(activeUser(), location);
            refreshLocations();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private JPanel locationForm(JTextField campus, JTextField building, JTextField room) {
        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.add(new JLabel("Campus *"));
        form.add(campus);
        form.add(new JLabel("Building"));
        form.add(building);
        form.add(new JLabel("Room"));
        form.add(room);
        return form;
    }

    private void deleteLocation() {
        int row = locationTable.getSelectedRow();
        if (row < 0) {
            showInfo("Select a location first.");
            return;
        }

        Location location = locationTableModel.getLocationAt(row);
        if (JOptionPane.showConfirmDialog(this,
                "Delete this location?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        try {
            controller.deleteLocation(activeUser(), location.getId());
            refreshLocations();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    @Override
    public void onUserChanged(User newUser) {
        if (newUser == null || newUser.getRole() != Role.ADMIN) {
            dispose();
            return;
        }
        refreshAll();
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "ReFind", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.";
        JOptionPane.showMessageDialog(this, message, "ReFind - Error", JOptionPane.ERROR_MESSAGE);
    }
}
