package com.refind.ui;

import com.refind.exception.ValidationException;
import com.refind.model.Category;
import com.refind.model.Item;
import com.refind.model.Location;
import com.refind.model.User;
import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ItemType;
import com.refind.service.CategoryService;
import com.refind.service.ItemService;
import com.refind.service.LocationService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FoundItemFrame extends JFrame implements SessionContext.SessionListener {

    private final ItemService itemService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final SessionContext sessionContext;

    // UI Components
    private JTabbedPane tabbedPane;

    // Browse Tab Components
    private JTextField searchField;
    private JComboBox<CategoryFilterItem> categoryFilterCombo;
    private JComboBox<String> statusFilterCombo;
    private JTable itemTable;
    private FoundItemTableModel tableModel;
    private JLabel countLabel;

    // Report Tab Components
    private JTextField reportTitleField;
    private JComboBox<FoundItemEditDialog.CategoryWrapper> reportCategoryCombo;
    private JComboBox<FoundItemEditDialog.LocationWrapper> reportLocationCombo;
    private JTextField reportImageField;
    private JTextArea reportDescArea;
    private JLabel reporterPreviewLabel;
    private JLabel reportedAtPreviewLabel;
    private JLabel statusPreviewBadge;

    // Header Session Components
    private JLabel currentUserLabel;
    private JComboBox<UserItem> userSwitcherCombo;

    public FoundItemFrame(ItemService itemService, CategoryService categoryService,
                          LocationService locationService, SessionContext sessionContext) {
        super("ReFind - Found Item Management Portal");
        this.itemService = itemService;
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.sessionContext = sessionContext;

        sessionContext.addSessionListener(this);

        initWindow();
        loadFoundItems();
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setMinimumSize(new Dimension(850, 580));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_LIGHT);
        setContentPane(root);

        // Header with Branding & Session Switcher
        root.add(buildHeaderPanel(), BorderLayout.NORTH);

        // Main Content Tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(UITheme.BG_LIGHT);

        tabbedPane.addTab("Browse & Search Found Items", buildBrowsePanel());
        tabbedPane.addTab("Report New Found Item", buildReportPanel());

        root.add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(UITheme.CARD_BG);
        header.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER, 1),
                new EmptyBorder(14, 24, 14, 24)
        ));

        // Brand Info
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandPanel.setBackground(UITheme.CARD_BG);

        JLabel logoBadge = new JLabel("ReFind");
        logoBadge.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoBadge.setForeground(UITheme.PRIMARY);

        JLabel subTitle = new JLabel("|  Found Item Management Module");
        subTitle.setFont(UITheme.FONT_BODY);
        subTitle.setForeground(UITheme.TEXT_MUTED);

        brandPanel.add(logoBadge);
        brandPanel.add(subTitle);
        header.add(brandPanel, BorderLayout.WEST);

        // Session Switcher Panel
        JPanel sessionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        sessionPanel.setBackground(UITheme.CARD_BG);

        JLabel activeUserTag = new JLabel("Logged in as:");
        activeUserTag.setFont(UITheme.FONT_BOLD);
        activeUserTag.setForeground(UITheme.TEXT_MUTED);

        userSwitcherCombo = new JComboBox<>();
        userSwitcherCombo.setFont(UITheme.FONT_BODY);
        populateUserSwitcher();

        userSwitcherCombo.addActionListener(e -> {
            UserItem selected = (UserItem) userSwitcherCombo.getSelectedItem();
            if (selected != null && selected.user != null) {
                sessionContext.setCurrentUser(selected.user);
            }
        });

        currentUserLabel = new JLabel();
        currentUserLabel.setFont(UITheme.FONT_BOLD);
        updateCurrentUserDisplay();

        sessionPanel.add(activeUserTag);
        sessionPanel.add(userSwitcherCombo);
        sessionPanel.add(currentUserLabel);

        header.add(sessionPanel, BorderLayout.EAST);
        return header;
    }

    private void populateUserSwitcher() {
        userSwitcherCombo.removeAllItems();
        List<User> users = sessionContext.getAvailableUsers();
        UserItem currentItem = null;
        for (User u : users) {
            UserItem item = new UserItem(u);
            userSwitcherCombo.addItem(item);
            if (sessionContext.getCurrentUser() != null && u.getId().equals(sessionContext.getCurrentUser().getId())) {
                currentItem = item;
            }
        }
        if (currentItem != null) {
            userSwitcherCombo.setSelectedItem(currentItem);
        }
    }

    private void updateCurrentUserDisplay() {
        User u = sessionContext.getCurrentUser();
        if (u != null) {
            currentUserLabel.setText("[" + u.getRole() + "]");
            currentUserLabel.setForeground(sessionContext.isCurrentUserAdmin() ? UITheme.DANGER : UITheme.PRIMARY);
            if (reporterPreviewLabel != null) {
                reporterPreviewLabel.setText(u.getName() + " (" + u.getEmail() + ")");
            }
        }
    }

    @Override
    public void onUserChanged(User newUser) {
        updateCurrentUserDisplay();
        tableModel.fireTableDataChanged();
    }

    private JPanel buildBrowsePanel() {
        JPanel browsePanel = new JPanel(new BorderLayout(0, 14));
        browsePanel.setBackground(UITheme.BG_LIGHT);
        browsePanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Filter / Search Toolbar
        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        filterCard.setBackground(UITheme.CARD_BG);
        filterCard.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1), new EmptyBorder(4, 8, 4, 8)));

        // Search Keyword
        JLabel searchLbl = new JLabel("Keyword:");
        searchLbl.setFont(UITheme.FONT_BOLD);
        searchField = UITheme.createTextField(15);
        searchField.setToolTipText("Search title or description...");
        searchField.addActionListener(e -> applyFilter());

        // Category Filter
        JLabel catLbl = new JLabel("Category:");
        catLbl.setFont(UITheme.FONT_BOLD);
        categoryFilterCombo = new JComboBox<>();
        categoryFilterCombo.setFont(UITheme.FONT_BODY);
        populateCategoryFilter();
        categoryFilterCombo.addActionListener(e -> applyFilter());

        // Status Filter
        JLabel statusLbl = new JLabel("Status:");
        statusLbl.setFont(UITheme.FONT_BOLD);
        statusFilterCombo = new JComboBox<>(new String[]{"All Statuses", "OPEN", "LOST", "CLAIMED", "RETURNED", "CLOSED"});
        statusFilterCombo.setFont(UITheme.FONT_BODY);
        statusFilterCombo.addActionListener(e -> applyFilter());

        JButton searchBtn = UITheme.createPrimaryButton("Search");
        searchBtn.addActionListener(e -> applyFilter());

        JButton resetBtn = UITheme.createSecondaryButton("Reset");
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            if (categoryFilterCombo.getItemCount() > 0) categoryFilterCombo.setSelectedIndex(0);
            statusFilterCombo.setSelectedIndex(0);
            loadFoundItems();
        });

        filterCard.add(searchLbl);
        filterCard.add(searchField);
        filterCard.add(catLbl);
        filterCard.add(categoryFilterCombo);
        filterCard.add(statusLbl);
        filterCard.add(statusFilterCombo);
        filterCard.add(searchBtn);
        filterCard.add(resetBtn);

        browsePanel.add(filterCard, BorderLayout.NORTH);

        // Table in Center
        tableModel = new FoundItemTableModel();
        itemTable = new JTable(tableModel);
        itemTable.setFont(UITheme.FONT_BODY);
        itemTable.setRowHeight(32);
        itemTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemTable.getTableHeader().setFont(UITheme.FONT_BOLD);
        itemTable.getTableHeader().setBackground(UITheme.BG_LIGHT);
        itemTable.getTableHeader().setBorder(new LineBorder(UITheme.BORDER, 1));
        itemTable.setShowGrid(true);
        itemTable.setGridColor(UITheme.BORDER);

        // Center align and custom renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        itemTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        itemTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(190);
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        itemTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        itemTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        itemTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        itemTable.getColumnModel().getColumn(6).setPreferredWidth(90);

        itemTable.getColumnModel().getColumn(6).setCellRenderer((table, value, isSelected, hasFocus, row, col) -> {
            ItemStatus s = null;
            if (value instanceof String) {
                try { s = ItemStatus.valueOf((String) value); } catch (Exception ignored) {}
            }
            JLabel badge = UITheme.createStatusBadge(s);
            if (isSelected) {
                badge.setBorder(new CompoundBorder(new LineBorder(UITheme.PRIMARY, 1), new EmptyBorder(2, 6, 2, 6)));
            }
            return badge;
        });

        itemTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && itemTable.getSelectedRow() != -1) {
                    viewSelectedDetails();
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(itemTable);
        tableScroll.setBorder(new LineBorder(UITheme.BORDER, 1));
        tableScroll.getViewport().setBackground(Color.WHITE);
        browsePanel.add(tableScroll, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(UITheme.BG_LIGHT);

        countLabel = new JLabel("Loading found items...");
        countLabel.setFont(UITheme.FONT_BOLD);
        countLabel.setForeground(UITheme.TEXT_MUTED);
        bottomBar.add(countLabel, BorderLayout.WEST);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtons.setBackground(UITheme.BG_LIGHT);

        JButton detailsBtn = UITheme.createSecondaryButton("View Details");
        JButton editBtn = UITheme.createSecondaryButton("Edit Item");
        JButton deleteBtn = UITheme.createDangerButton("Delete Item");
        JButton reportNewBtn = UITheme.createPrimaryButton("+ Report Found Item");

        detailsBtn.addActionListener(e -> viewSelectedDetails());
        editBtn.addActionListener(e -> editSelectedItem());
        deleteBtn.addActionListener(e -> deleteSelectedItem());
        reportNewBtn.addActionListener(e -> tabbedPane.setSelectedIndex(1));

        actionButtons.add(detailsBtn);
        actionButtons.add(editBtn);
        actionButtons.add(deleteBtn);
        actionButtons.add(reportNewBtn);

        bottomBar.add(actionButtons, BorderLayout.EAST);
        browsePanel.add(bottomBar, BorderLayout.SOUTH);

        return browsePanel;
    }

    private JPanel buildReportPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(UITheme.BG_LIGHT);
        container.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel formCard = new JPanel(new BorderLayout(0, 16));
        formCard.setBackground(UITheme.CARD_BG);
        formCard.setBorder(UITheme.createCardBorder());

        // Header
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setBackground(UITheme.CARD_BG);
        JLabel title = new JLabel("Report a Found Item");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);
        JLabel subtitle = new JLabel("Fill in the item details. Ownership and timestamps will be automatically recorded.");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);
        cardHeader.add(title, BorderLayout.NORTH);
        cardHeader.add(subtitle, BorderLayout.SOUTH);
        formCard.add(cardHeader, BorderLayout.NORTH);

        // Form Fields Grid
        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        int row = 0;

        // Title
        gbc.gridx = 0; gbc.gridy = row++;
        JLabel tLbl = new JLabel("Item Title *");
        tLbl.setFont(UITheme.FONT_BOLD);
        formGrid.add(tLbl, gbc);

        gbc.gridy = row++;
        reportTitleField = UITheme.createTextField(30);
        formGrid.add(reportTitleField, gbc);

        // Category with "+ Add Category" button
        gbc.gridy = row++;
        JLabel cLbl = new JLabel("Category *");
        cLbl.setFont(UITheme.FONT_BOLD);
        formGrid.add(cLbl, gbc);

        gbc.gridy = row++;
        JPanel catRow = new JPanel(new BorderLayout(8, 0));
        catRow.setBackground(UITheme.CARD_BG);
        reportCategoryCombo = new JComboBox<>();
        reportCategoryCombo.setFont(UITheme.FONT_BODY);
        populateReportCategories();
        JButton addCatBtn = UITheme.createSecondaryButton("+ Add");
        addCatBtn.addActionListener(e -> addNewCategoryDialog());
        catRow.add(reportCategoryCombo, BorderLayout.CENTER);
        catRow.add(addCatBtn, BorderLayout.EAST);
        formGrid.add(catRow, gbc);

        // Location with "+ Add Location" button
        gbc.gridy = row++;
        JLabel lLbl = new JLabel("Location (Where found)");
        lLbl.setFont(UITheme.FONT_BOLD);
        formGrid.add(lLbl, gbc);

        gbc.gridy = row++;
        JPanel locRow = new JPanel(new BorderLayout(8, 0));
        locRow.setBackground(UITheme.CARD_BG);
        reportLocationCombo = new JComboBox<>();
        reportLocationCombo.setFont(UITheme.FONT_BODY);
        populateReportLocations();
        JButton addLocBtn = UITheme.createSecondaryButton("+ Add");
        addLocBtn.addActionListener(e -> addNewLocationDialog());
        locRow.add(reportLocationCombo, BorderLayout.CENTER);
        locRow.add(addLocBtn, BorderLayout.EAST);
        formGrid.add(locRow, gbc);

        // Image Path
        gbc.gridy = row++;
        JLabel imgLbl = new JLabel("Image Path (Optional)");
        imgLbl.setFont(UITheme.FONT_BOLD);
        formGrid.add(imgLbl, gbc);

        gbc.gridy = row++;
        JPanel imgRow = new JPanel(new BorderLayout(8, 0));
        imgRow.setBackground(UITheme.CARD_BG);
        reportImageField = UITheme.createTextField(20);
        JButton browseImgBtn = UITheme.createSecondaryButton("Browse...");
        browseImgBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                reportImageField.setText(f.getAbsolutePath());
            }
        });
        imgRow.add(reportImageField, BorderLayout.CENTER);
        imgRow.add(browseImgBtn, BorderLayout.EAST);
        formGrid.add(imgRow, gbc);

        // Description
        gbc.gridy = row++;
        JLabel dLbl = new JLabel("Description");
        dLbl.setFont(UITheme.FONT_BOLD);
        formGrid.add(dLbl, gbc);

        gbc.gridy = row++;
        reportDescArea = new JTextArea(4, 30);
        reportDescArea.setFont(UITheme.FONT_BODY);
        reportDescArea.setLineWrap(true);
        reportDescArea.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(reportDescArea);
        formGrid.add(descScroll, gbc);

        // Auto-populated Metadata Info Box
        gbc.gridy = row++;
        JPanel metaPanel = new JPanel(new GridLayout(3, 2, 8, 4));
        metaPanel.setBackground(UITheme.BG_LIGHT);
        metaPanel.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1), new EmptyBorder(8, 12, 8, 12)));

        JLabel repTag = new JLabel("Reported By (Auto-populated):");
        repTag.setFont(UITheme.FONT_BOLD);
        reporterPreviewLabel = new JLabel();
        reporterPreviewLabel.setFont(UITheme.FONT_BODY);

        JLabel timeTag = new JLabel("Reported At (Auto-populated):");
        timeTag.setFont(UITheme.FONT_BOLD);
        reportedAtPreviewLabel = new JLabel("Current Timestamp (" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + ")");
        reportedAtPreviewLabel.setFont(UITheme.FONT_BODY);

        JLabel statusTag = new JLabel("Initial Status (Default):");
        statusTag.setFont(UITheme.FONT_BOLD);
        statusPreviewBadge = UITheme.createStatusBadge(ItemStatus.OPEN);

        metaPanel.add(repTag);
        metaPanel.add(reporterPreviewLabel);
        metaPanel.add(timeTag);
        metaPanel.add(reportedAtPreviewLabel);
        metaPanel.add(statusTag);
        metaPanel.add(statusPreviewBadge);

        formGrid.add(metaPanel, gbc);
        formCard.add(formGrid, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(UITheme.CARD_BG);

        JButton submitBtn = UITheme.createPrimaryButton("Submit Found Item Report");
        JButton resetBtn = UITheme.createSecondaryButton("Clear Form");

        submitBtn.addActionListener(e -> submitReport());
        resetBtn.addActionListener(e -> clearReportForm());

        footer.add(submitBtn);
        footer.add(resetBtn);
        formCard.add(footer, BorderLayout.SOUTH);

        JScrollPane formScroll = new JScrollPane(formCard);
        formScroll.setBorder(null);
        formScroll.setBackground(UITheme.BG_LIGHT);
        container.add(formScroll, BorderLayout.CENTER);

        updateCurrentUserDisplay();
        return container;
    }

    private void populateCategoryFilter() {
        categoryFilterCombo.removeAllItems();
        categoryFilterCombo.addItem(new CategoryFilterItem(null, "All Categories"));
        try {
            List<Category> list = categoryService.getAllCategories();
            for (Category c : list) {
                categoryFilterCombo.addItem(new CategoryFilterItem(c.getId(), c.getName()));
            }
        } catch (Exception ignored) {}
    }

    private void populateReportCategories() {
        reportCategoryCombo.removeAllItems();
        try {
            List<Category> list = categoryService.getAllCategories();
            for (Category c : list) {
                reportCategoryCombo.addItem(new FoundItemEditDialog.CategoryWrapper(c));
            }
            if (reportCategoryCombo.getItemCount() > 0) {
                reportCategoryCombo.setSelectedIndex(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to load categories for the Found Item report form.\n" + ex.getMessage(),
                    "Category Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateReportLocations() {
        reportLocationCombo.removeAllItems();
        try {
            reportLocationCombo.addItem(new FoundItemEditDialog.LocationWrapper(null));
            List<Location> list = locationService.getAllLocations();
            for (Location l : list) {
                reportLocationCombo.addItem(new FoundItemEditDialog.LocationWrapper(l));
            }
            if (reportLocationCombo.getItemCount() > 0) {
                reportLocationCombo.setSelectedIndex(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to load locations for the Found Item report form.\n" + ex.getMessage(),
                    "Location Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addNewCategoryDialog() {
        String name = JOptionPane.showInputDialog(this, "Enter New Category Name:", "Add Category", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            try {
                Category c = categoryService.createCategory(name.trim());
                populateCategoryFilter();
                populateReportCategories();
                for (int i = 0; i < reportCategoryCombo.getItemCount(); i++) {
                    if (reportCategoryCombo.getItemAt(i).category != null
                            && c.getId().equals(reportCategoryCombo.getItemAt(i).category.getId())) {
                        reportCategoryCombo.setSelectedIndex(i);
                        break;
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void addNewLocationDialog() {
        JTextField campusField = new JTextField("Main Campus");
        JTextField buildingField = new JTextField();
        JTextField roomField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(3, 2, 6, 6));
        panel.add(new JLabel("Campus:")); panel.add(campusField);
        panel.add(new JLabel("Building:")); panel.add(buildingField);
        panel.add(new JLabel("Room:")); panel.add(roomField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add New Location", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                Location l = locationService.createLocation(campusField.getText().trim(), buildingField.getText().trim(), roomField.getText().trim());
                populateReportLocations();
                for (int i = 0; i < reportLocationCombo.getItemCount(); i++) {
                    if (reportLocationCombo.getItemAt(i).location != null
                            && l.getId().equals(reportLocationCombo.getItemAt(i).location.getId())) {
                        reportLocationCombo.setSelectedIndex(i);
                        break;
                    }
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void submitReport() {
        String title = reportTitleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        FoundItemEditDialog.CategoryWrapper catWrap = (FoundItemEditDialog.CategoryWrapper) reportCategoryCombo.getSelectedItem();
        if (catWrap == null || catWrap.category == null) {
            JOptionPane.showMessageDialog(this, "Category is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        FoundItemEditDialog.LocationWrapper locWrap = (FoundItemEditDialog.LocationWrapper) reportLocationCombo.getSelectedItem();
        String desc = reportDescArea.getText().trim();
        String img = reportImageField.getText().trim();

        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Active user session is required.", "Session Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Construct Item for reporting:
        // - Type is FOUND
        // - Status defaults to OPEN
        // - reportedBy is current user
        // - reportedAt is current timestamp
        Item newItem = new Item();
        newItem.setTitle(title);
        newItem.setDescription(desc.isEmpty() ? null : desc);
        newItem.setType(ItemType.FOUND);
        newItem.setStatus(ItemStatus.OPEN);
        newItem.setCategory(catWrap.category);
        newItem.setLocation(locWrap != null ? locWrap.location : null);
        newItem.setImagePath(img.isEmpty() ? null : img);
        newItem.setReportedBy(currentUser);
        newItem.setReportedAt(LocalDateTime.now());

        try {
            Item saved = itemService.reportItem(newItem);
            JOptionPane.showMessageDialog(this,
                    "Found item reported successfully!\nItem ID: #" + saved.getId() + "\nStatus: " + saved.getStatus(),
                    "Report Submitted", JOptionPane.INFORMATION_MESSAGE);

            clearReportForm();
            loadFoundItems();
            tabbedPane.setSelectedIndex(0); // Switch to browse tab
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearReportForm() {
        reportTitleField.setText("");
        reportDescArea.setText("");
        reportImageField.setText("");
        if (reportCategoryCombo.getItemCount() > 0) reportCategoryCombo.setSelectedIndex(0);
        if (reportLocationCombo.getItemCount() > 0) reportLocationCombo.setSelectedIndex(0);
        reportedAtPreviewLabel.setText("Current Timestamp (" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + ")");
    }

    private void loadFoundItems() {
        try {
            List<Item> foundItems = itemService.getItemsByType(ItemType.FOUND);
            tableModel.setItems(foundItems);
            countLabel.setText("Showing " + foundItems.size() + " found item" + (foundItems.size() == 1 ? "" : "s"));
        } catch (Exception ex) {
            countLabel.setText("Error loading items: " + ex.getMessage());
        }
    }

    private void applyFilter() {
        String keyword = searchField.getText().trim();
        CategoryFilterItem catItem = (CategoryFilterItem) categoryFilterCombo.getSelectedItem();
        Long catId = catItem != null ? catItem.id : null;
        String statusStr = (String) statusFilterCombo.getSelectedItem();

        try {
            List<Item> list;
            if (!keyword.isEmpty()) {
                list = itemService.searchItemsByType(keyword, ItemType.FOUND);
            } else if (catId != null) {
                list = itemService.getItemsByCategoryAndType(catId, ItemType.FOUND);
            } else {
                list = itemService.getItemsByType(ItemType.FOUND);
            }

            // In-memory status filter if selected
            if (statusStr != null && !statusStr.equals("All Statuses")) {
                ItemStatus targetStatus = ItemStatus.valueOf(statusStr);
                list = list.stream().filter(i -> i.getStatus() == targetStatus).toList();
            }

            // In-memory category filter if keyword was searched with category
            if (!keyword.isEmpty() && catId != null) {
                list = list.stream().filter(i -> i.getCategory() != null && catId.equals(i.getCategory().getId())).toList();
            }

            tableModel.setItems(list);
            countLabel.setText("Found " + list.size() + " found item" + (list.size() == 1 ? "" : "s"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Filter error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewSelectedDetails() {
        int row = itemTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a found item from the table first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Item selected = tableModel.getItemAt(row);
        if (selected != null) {
            FoundItemDetailsDialog dialog = new FoundItemDetailsDialog(this, selected, itemService, categoryService, locationService, sessionContext, this::loadFoundItems);
            dialog.setVisible(true);
        }
    }

    private void editSelectedItem() {
        int row = itemTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a found item from the table first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Item selected = tableModel.getItemAt(row);
        if (selected == null) return;

        User currentUser = sessionContext.getCurrentUser();
        boolean isOwner = selected.getReportedBy() != null && currentUser != null
                && currentUser.getId() != null && currentUser.getId().equals(selected.getReportedBy().getId());
        boolean isAdmin = sessionContext.isCurrentUserAdmin();

        if (!isOwner && !isAdmin) {
            String reporter = selected.getReportedBy() != null ? selected.getReportedBy().getName() : "Unknown";
            JOptionPane.showMessageDialog(this,
                    "Permission Denied: You cannot edit this item.\n"
                            + "Current User: " + (currentUser != null ? currentUser.getName() : "None") + "\n"
                            + "Reported By: " + reporter + "\n"
                            + "Only the original reporter or an Admin is permitted to edit.",
                    "Edit Not Permitted", JOptionPane.WARNING_MESSAGE);
            return;
        }

        FoundItemEditDialog dialog = new FoundItemEditDialog(this, selected, itemService, categoryService, locationService, sessionContext, this::loadFoundItems);
        dialog.setVisible(true);
    }

    private void deleteSelectedItem() {
        int row = itemTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a found item from the table first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Item selected = tableModel.getItemAt(row);
        if (selected == null) return;

        User currentUser = sessionContext.getCurrentUser();
        boolean isOwner = selected.getReportedBy() != null && currentUser != null
                && currentUser.getId() != null && currentUser.getId().equals(selected.getReportedBy().getId());
        boolean isAdmin = sessionContext.isCurrentUserAdmin();

        if (!isOwner && !isAdmin) {
            String reporter = selected.getReportedBy() != null ? selected.getReportedBy().getName() : "Unknown";
            JOptionPane.showMessageDialog(this,
                    "Permission Denied: You cannot delete this item.\n"
                            + "Current User: " + (currentUser != null ? currentUser.getName() : "None") + "\n"
                            + "Reported By: " + reporter + "\n"
                            + "Only the original reporter or an Admin is permitted to delete this item.",
                    "Permitted Deletion Violation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete found item #" + selected.getId() + " - \"" + selected.getTitle() + "\"?",
                "Confirm Permitted Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            try {
                boolean success = itemService.deleteItem(selected.getId(), currentUser);
                if (success) {
                    JOptionPane.showMessageDialog(this, "Found item #" + selected.getId() + " successfully deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                    loadFoundItems();
                }
            } catch (ValidationException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Deletion Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static class CategoryFilterItem {
        final Long id;
        final String name;
        CategoryFilterItem(Long id, String name) { this.id = id; this.name = name; }
        @Override
        public String toString() { return name; }
    }

    static class UserItem {
        final User user;
        UserItem(User user) { this.user = user; }
        @Override
        public String toString() {
            return user.getName() + " (" + user.getRole() + ")";
        }
    }
}
