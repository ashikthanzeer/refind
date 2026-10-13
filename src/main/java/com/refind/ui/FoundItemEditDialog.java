package com.refind.ui;

import com.refind.exception.ValidationException;
import com.refind.model.Category;
import com.refind.model.Item;
import com.refind.model.Location;
import com.refind.model.enums.ItemStatus;
import com.refind.service.CategoryService;
import com.refind.service.ImageService;
import com.refind.service.ItemService;
import com.refind.service.LocationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.List;

public class FoundItemEditDialog extends JDialog {

    private final Item item;
    private final ItemService itemService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final SessionContext sessionContext;
    private final ImageService imageService;
    private final Runnable onSuccess;

    private JTextField titleField;
    private JTextArea descArea;
    private JComboBox<CategoryWrapper> categoryCombo;
    private JComboBox<LocationWrapper> locationCombo;
    private JComboBox<ItemStatus> statusCombo;
    private JTextField imagePathField;
    private JLabel imagePreviewLabel;

    public FoundItemEditDialog(Window parent, Item item, ItemService itemService,
                               CategoryService categoryService, LocationService locationService,
                               SessionContext sessionContext, Runnable onSuccess) {
        super(parent, "Edit Found Item #" + item.getId(), ModalityType.APPLICATION_MODAL);
        this.item = item;
        this.itemService = itemService;
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.sessionContext = sessionContext;
        this.imageService = new ImageService();
        this.onSuccess = onSuccess;

        initComponents();
        setSize(540, 600);
        setMinimumSize(new Dimension(480, 500));
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout(0, 14));
        contentPane.setBackground(UITheme.CARD_BG);
        contentPane.setBorder(new EmptyBorder(20, 24, 20, 24));
        setContentPane(contentPane);

        JLabel headerLabel = new JLabel("Edit Found Item Details");
        headerLabel.setFont(UITheme.FONT_TITLE);
        headerLabel.setForeground(UITheme.TEXT_MAIN);
        contentPane.add(headerLabel, BorderLayout.NORTH);

        // Form Grid
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(3, 0, 3, 0);

        int row = 0;

        // Title
        gbc.gridy = row++;
        JLabel titleLbl = new JLabel("Item Title *");
        titleLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(titleLbl, gbc);

        gbc.gridy = row++;
        titleField = UITheme.createTextField(25);
        titleField.setText(item.getTitle() != null ? item.getTitle() : "");
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

        // Image Path / Upload
        gbc.gridy = row++;
        JLabel imgLbl = new JLabel("Photograph (Upload or Path)");
        imgLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(imgLbl, gbc);

        gbc.gridy = row++;
        JPanel imgRow = new JPanel(new BorderLayout(8, 0));
        imgRow.setBackground(UITheme.CARD_BG);

        imagePreviewLabel = new JLabel();
        imagePreviewLabel.setPreferredSize(new Dimension(80, 56));
        imagePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        imagePreviewLabel.setIcon(imageService.getScaledThumbnail(item.getImagePath(), 80, 56));
        imgRow.add(imagePreviewLabel, BorderLayout.WEST);

        imagePathField = UITheme.createTextField(20);
        imagePathField.setText(item.getImagePath() != null ? item.getImagePath() : "");
        imgRow.add(imagePathField, BorderLayout.CENTER);

        JButton browseBtn = UITheme.createSecondaryButton("Browse...");
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Image Files (JPEG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    String stored = imageService.storeImage(file);
                    imagePathField.setText(stored);
                    imagePreviewLabel.setIcon(imageService.getScaledThumbnail(stored, 80, 56));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to upload image: " + ex.getMessage(), "Upload Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        imgRow.add(browseBtn, BorderLayout.EAST);
        formPanel.add(imgRow, gbc);

        // Description
        gbc.gridy = row++;
        JLabel descLbl = new JLabel("Description");
        descLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(descLbl, gbc);

        gbc.gridy = row++;
        descArea = UITheme.createTextArea(4, 30);
        descArea.setText(item.getDescription() != null ? item.getDescription() : "");
        JScrollPane descScroll = new JScrollPane(descArea);
        formPanel.add(descScroll, gbc);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(UITheme.CARD_BG);

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = UITheme.createPrimaryButton("Save Changes");
        saveBtn.addActionListener(e -> saveChanges());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        contentPane.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void populateCategories() {
        categoryCombo.removeAllItems();
        try {
            List<Category> categories = categoryService.getAllCategories();
            int selectedIndex = 0;
            for (int i = 0; i < categories.size(); i++) {
                Category c = categories.get(i);
                categoryCombo.addItem(new CategoryWrapper(c));
                if (item.getCategory() != null && c.getId().equals(item.getCategory().getId())) {
                    selectedIndex = i;
                }
            }
            if (categoryCombo.getItemCount() > 0) {
                categoryCombo.setSelectedIndex(selectedIndex);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void populateLocations() {
        locationCombo.removeAllItems();
        locationCombo.addItem(new LocationWrapper(null)); // "None / Unspecified"
        try {
            List<Location> locations = locationService.getAllLocations();
            int selectedIndex = 0;
            for (int i = 0; i < locations.size(); i++) {
                Location loc = locations.get(i);
                locationCombo.addItem(new LocationWrapper(loc));
                if (item.getLocation() != null && loc.getId().equals(item.getLocation().getId())) {
                    selectedIndex = i + 1;
                }
            }
            locationCombo.setSelectedIndex(selectedIndex);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void saveChanges() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Item title cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        CategoryWrapper selectedCat = (CategoryWrapper) categoryCombo.getSelectedItem();
        if (selectedCat == null || selectedCat.category == null) {
            JOptionPane.showMessageDialog(this, "Please select a category.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocationWrapper selectedLoc = (LocationWrapper) locationCombo.getSelectedItem();
        ItemStatus selectedStatus = (ItemStatus) statusCombo.getSelectedItem();
        String imgPath = imagePathField.getText().trim();
        String desc = descArea.getText().trim();

        item.setTitle(title);
        item.setCategory(selectedCat.category);
        item.setLocation(selectedLoc != null ? selectedLoc.location : null);
        item.setStatus(selectedStatus != null ? selectedStatus : ItemStatus.OPEN);
        item.setImagePath(imgPath.isEmpty() ? null : imgPath);
        item.setDescription(desc.isEmpty() ? null : desc);

        try {
            itemService.updateItem(item, sessionContext.getCurrentUser());
            JOptionPane.showMessageDialog(this, "Found item updated successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            if (onSuccess != null) onSuccess.run();
            dispose();
        } catch (ValidationException ve) {
            JOptionPane.showMessageDialog(this, ve.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to update item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static class CategoryWrapper {
        public final Category category;
        public CategoryWrapper(Category category) { this.category = category; }
        @Override
        public String toString() {
            return category != null ? category.getName() : "Select Category";
        }
    }

    public static class LocationWrapper {
        public final Location location;
        public LocationWrapper(Location location) { this.location = location; }
        @Override
        public String toString() {
            if (location == null) return "None / Unspecified";
            return location.getCampus() + " - " + location.getBuilding() + " (Room " + location.getRoom() + ")";
        }
    }
}
