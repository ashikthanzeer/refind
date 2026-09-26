package com.refind.ui;

import com.refind.exception.ValidationException;
import com.refind.model.Category;
import com.refind.model.Item;
import com.refind.model.Location;
import com.refind.model.User;
import com.refind.model.enums.ItemStatus;
import com.refind.service.CategoryService;
import com.refind.service.ItemService;
import com.refind.service.LocationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

public class FoundItemEditDialog extends JDialog {

    private final Item item;
    private final ItemService itemService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final SessionContext sessionContext;
    private final Runnable onSuccess;

    private JTextField titleField;
    private JTextArea descArea;
    private JComboBox<CategoryWrapper> categoryCombo;
    private JComboBox<LocationWrapper> locationCombo;
    private JComboBox<ItemStatus> statusCombo;
    private JTextField imagePathField;

    public FoundItemEditDialog(Window parent, Item item, ItemService itemService,
                              CategoryService categoryService, LocationService locationService,
                              SessionContext sessionContext, Runnable onSuccess) {
        super(parent, "Edit Found Item #" + item.getId(), ModalityType.APPLICATION_MODAL);
        this.item = item;
        this.itemService = itemService;
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.sessionContext = sessionContext;
        this.onSuccess = onSuccess;

        initComponents();
        setSize(520, 560);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout(0, 16));
        contentPane.setBackground(UITheme.CARD_BG);
        contentPane.setBorder(new EmptyBorder(20, 24, 20, 24));
        setContentPane(contentPane);

        JLabel headerLabel = new JLabel("Edit Found Item Details");
        headerLabel.setFont(UITheme.FONT_TITLE);
        headerLabel.setForeground(UITheme.TEXT_MAIN);
        contentPane.add(headerLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        int row = 0;

        // Title
        gbc.gridx = 0; gbc.gridy = row++;
        JLabel titleLbl = new JLabel("Title *");
        titleLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(titleLbl, gbc);

        gbc.gridy = row++;
        titleField = UITheme.createTextField(30);
        titleField.setText(item.getTitle());
        formPanel.add(titleField, gbc);

        // Category
        gbc.gridy = row++;
        JLabel catLbl = new JLabel("Category *");
        catLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(catLbl, gbc);

        gbc.gridy = row++;
        categoryCombo = new JComboBox<>();
        categoryCombo.setFont(UITheme.FONT_BODY);
        populateCategories();
        formPanel.add(categoryCombo, gbc);

        // Location
        gbc.gridy = row++;
        JLabel locLbl = new JLabel("Location");
        locLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(locLbl, gbc);

        gbc.gridy = row++;
        locationCombo = new JComboBox<>();
        locationCombo.setFont(UITheme.FONT_BODY);
        populateLocations();
        formPanel.add(locationCombo, gbc);

        // Status
        gbc.gridy = row++;
        JLabel statusLbl = new JLabel("Status");
        statusLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(statusLbl, gbc);

        gbc.gridy = row++;
        statusCombo = new JComboBox<>(ItemStatus.values());
        statusCombo.setFont(UITheme.FONT_BODY);
        statusCombo.setSelectedItem(item.getStatus());
        formPanel.add(statusCombo, gbc);

        // Image Path
        gbc.gridy = row++;
        JLabel imgLbl = new JLabel("Image Path (Optional)");
        imgLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(imgLbl, gbc);

        gbc.gridy = row++;
        JPanel imgRow = new JPanel(new BorderLayout(6, 0));
        imgRow.setBackground(UITheme.CARD_BG);
        imagePathField = UITheme.createTextField(20);
        imagePathField.setText(item.getImagePath() != null ? item.getImagePath() : "");
        JButton browseBtn = UITheme.createSecondaryButton("Browse...");
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                imagePathField.setText(file.getAbsolutePath());
            }
        });
        imgRow.add(imagePathField, BorderLayout.CENTER);
        imgRow.add(browseBtn, BorderLayout.EAST);
        formPanel.add(imgRow, gbc);

        // Description
        gbc.gridy = row++;
        JLabel descLbl = new JLabel("Description");
        descLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(descLbl, gbc);

        gbc.gridy = row++;
        descArea = new JTextArea(4, 30);
        descArea.setFont(UITheme.FONT_BODY);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setText(item.getDescription() != null ? item.getDescription() : "");
        JScrollPane descScroll = new JScrollPane(descArea);
        formPanel.add(descScroll, gbc);

        contentPane.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(UITheme.CARD_BG);

        JButton saveBtn = UITheme.createPrimaryButton("Save Changes");
        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");

        saveBtn.addActionListener(e -> saveChanges());
        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void populateCategories() {
        try {
            List<Category> list = categoryService.getAllCategories();
            CategoryWrapper selected = null;
            for (Category c : list) {
                CategoryWrapper w = new CategoryWrapper(c);
                categoryCombo.addItem(w);
                if (item.getCategory() != null && c.getId().equals(item.getCategory().getId())) {
                    selected = w;
                }
            }
            if (selected != null) {
                categoryCombo.setSelectedItem(selected);
            }
        } catch (Exception ex) {
            categoryCombo.addItem(new CategoryWrapper(new Category("General")));
        }
    }

    private void populateLocations() {
        try {
            List<Location> list = locationService.getAllLocations();
            locationCombo.addItem(new LocationWrapper(null)); // None
            LocationWrapper selected = null;
            for (Location l : list) {
                LocationWrapper w = new LocationWrapper(l);
                locationCombo.addItem(w);
                if (item.getLocation() != null && l.getId().equals(item.getLocation().getId())) {
                    selected = w;
                }
            }
            if (selected != null) {
                locationCombo.setSelectedItem(selected);
            }
        } catch (Exception ex) {
            locationCombo.addItem(new LocationWrapper(null));
        }
    }

    private void saveChanges() {
        String title = titleField.getText().trim();
        if (title.isBlank()) {
            JOptionPane.showMessageDialog(this, "Title is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CategoryWrapper catWrap = (CategoryWrapper) categoryCombo.getSelectedItem();
        if (catWrap == null || catWrap.category == null) {
            JOptionPane.showMessageDialog(this, "Please select a category.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LocationWrapper locWrap = (LocationWrapper) locationCombo.getSelectedItem();
        ItemStatus status = (ItemStatus) statusCombo.getSelectedItem();
        String desc = descArea.getText().trim();
        String img = imagePathField.getText().trim();

        item.setTitle(title);
        item.setDescription(desc);
        item.setCategory(catWrap.category);
        item.setLocation(locWrap != null ? locWrap.location : null);
        item.setStatus(status != null ? status : ItemStatus.OPEN);
        item.setImagePath(img.isEmpty() ? null : img);

        try {
            User currentUser = sessionContext.getCurrentUser();
            itemService.updateItem(item, currentUser);
            JOptionPane.showMessageDialog(this, "Found item #" + item.getId() + " updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            if (onSuccess != null) onSuccess.run();
            dispose();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Permission / Validation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static class CategoryWrapper {
        final Category category;
        CategoryWrapper(Category category) { this.category = category; }
        @Override
        public String toString() { return category != null ? category.getName() : "None"; }
    }

    static class LocationWrapper {
        final Location location;
        LocationWrapper(Location location) { this.location = location; }
        @Override
        public String toString() {
            if (location == null) return "None / Unspecified";
            StringBuilder sb = new StringBuilder();
            if (location.getCampus() != null) sb.append(location.getCampus());
            if (location.getBuilding() != null) sb.append(" - ").append(location.getBuilding());
            if (location.getRoom() != null) sb.append(" (Room ").append(location.getRoom()).append(")");
            return sb.toString();
        }
    }
}
