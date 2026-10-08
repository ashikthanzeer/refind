package com.refind.ui;

import com.refind.controller.AdminController;
import com.refind.database.DatabaseConnection;
import com.refind.model.Category;
import com.refind.model.Claim;
import com.refind.model.Item;
import com.refind.model.Location;
import com.refind.model.User;
import com.refind.model.enums.ClaimStatus;
import com.refind.model.enums.ItemType;
import com.refind.model.enums.Role;
import com.refind.service.CategoryService;
import com.refind.service.ClaimService;
import com.refind.service.ItemService;
import com.refind.service.LocationService;
import com.refind.service.ModerationService;
import com.refind.service.UserService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

/**
 * Modern Dark Navy Blue Administrator Dashboard for ReFind.
 * Features KPI analytics, claim moderation, all-item oversight,
 * user role management, categories, and location configuration.
 */
public class AdminFrame extends JFrame implements SessionContext.SessionListener {

    private final AdminController controller;
    private final SessionContext sessionContext;
    private final ItemService itemService;
    private final ClaimService claimService;
    private final ModerationService moderationService;

    // KPI Display Labels
    private JLabel kpiLostCountLabel;
    private JLabel kpiFoundCountLabel;
    private JLabel kpiClaimsCountLabel;
    private JLabel kpiUsersCountLabel;
    private JLabel kpiDbStatusLabel;

    // Tables & Models
    private JTable userTable;
    private JTable categoryTable;
    private JTable locationTable;
    private JTable claimTable;
    private JTable allItemsTable;

    private UserTableModel userTableModel;
    private CategoryTableModel categoryTableModel;
    private LocationTableModel locationTableModel;
    private ClaimTableModel claimTableModel;
    private LostItemTableModel allItemsTableModel;

    private JComboBox<String> claimFilterCombo;
    private JTabbedPane mainTabs;

    public AdminFrame(UserService userService,
                      CategoryService categoryService,
                      LocationService locationService,
                      SessionContext sessionContext) {
        this(userService, categoryService, locationService, null, null, null, sessionContext);
    }

    public AdminFrame(UserService userService,
                      CategoryService categoryService,
                      LocationService locationService,
                      ItemService itemService,
                      ClaimService claimService,
                      ModerationService moderationService,
                      SessionContext sessionContext) {
        super("ReFind - Administrator Command Center");
        this.itemService = itemService;
        this.claimService = claimService;
        this.moderationService = moderationService;
        this.controller = new AdminController(
                userService, categoryService, locationService, itemService, claimService, moderationService
        );
        this.sessionContext = sessionContext;

        sessionContext.addSessionListener(this);

        initWindow();
        refreshAll();
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 750);
        setMinimumSize(new Dimension(950, 650));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_DARK);
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);

        mainTabs = new JTabbedPane();
        mainTabs.setFont(UITheme.FONT_BOLD);
        mainTabs.setBackground(UITheme.BG_DARK);
        mainTabs.setForeground(UITheme.TEXT_MUTED);

        mainTabs.addTab("📊 Dashboard Overview", buildDashboardOverviewPanel());
        mainTabs.addTab("⚖️ Claims Moderation", buildClaimsPanel());
        mainTabs.addTab("📦 All Items Oversight", buildAllItemsPanel());
        mainTabs.addTab("👥 User Accounts", buildUsersPanel());
        mainTabs.addTab("🏷️ Categories", buildCategoriesPanel());
        mainTabs.addTab("📍 Locations", buildLocationsPanel());

        root.add(mainTabs, BorderLayout.CENTER);
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
        title.setFont(UITheme.FONT_BRAND);
        title.setForeground(UITheme.PRIMARY);

        JLabel subtitle = new JLabel("|  Administrator Command Center");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        brand.add(title);
        brand.add(subtitle);
        header.add(brand, BorderLayout.WEST);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        userPanel.setBackground(UITheme.CARD_BG);

        User user = sessionContext.getCurrentUser();
        JLabel userLabel = new JLabel(user != null ? user.getName() : "Administrator");
        userLabel.setFont(UITheme.FONT_BOLD);
        userLabel.setForeground(UITheme.TEXT_MAIN);

        JLabel roleBadge = UITheme.createRoleBadge("ADMIN");

        JButton refreshBtn = UITheme.createSecondaryButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> refreshAll());

        JButton close = UITheme.createSecondaryButton("Close Window");
        close.addActionListener(e -> dispose());

        userPanel.add(userLabel);
        userPanel.add(roleBadge);
        userPanel.add(refreshBtn);
        userPanel.add(close);
        header.add(userPanel, BorderLayout.EAST);

        return header;
    }

    // ─── 1. Dashboard Overview Tab ───────────────────────────────────────────

    private JPanel buildDashboardOverviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 18));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Top Section: Title & Subtitle
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(UITheme.BG_DARK);
        JLabel title = new JLabel("System Analytics & Health Overview");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);
        JLabel sub = new JLabel("Real-time telemetry and management controls across all campus locations.");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_MUTED);
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(sub, BorderLayout.SOUTH);
        panel.add(titlePanel, BorderLayout.NORTH);

        // Center: KPI Metric Cards Grid
        JPanel kpiGrid = new JPanel(new GridLayout(2, 3, 16, 16));
        kpiGrid.setBackground(UITheme.BG_DARK);

        kpiLostCountLabel = new JLabel("0", SwingConstants.CENTER);
        kpiFoundCountLabel = new JLabel("0", SwingConstants.CENTER);
        kpiClaimsCountLabel = new JLabel("0", SwingConstants.CENTER);
        kpiUsersCountLabel = new JLabel("0", SwingConstants.CENTER);
        kpiDbStatusLabel = new JLabel("Checking...", SwingConstants.CENTER);

        kpiGrid.add(createKpiCard("🔍 Total Lost Items Reported", kpiLostCountLabel, UITheme.WARNING, "Items missing by owners"));
        kpiGrid.add(createKpiCard("📦 Total Found Items Registered", kpiFoundCountLabel, UITheme.PRIMARY, "Recovered items awaiting owner"));
        kpiGrid.add(createKpiCard("⚖️ Pending Claims for Review", kpiClaimsCountLabel, UITheme.PURPLE, "Awaiting moderator review"));
        kpiGrid.add(createKpiCard("👥 Registered User Accounts", kpiUsersCountLabel, UITheme.INFO, "Active campus accounts"));
        kpiGrid.add(createKpiCard("🗄️ Database Connection", kpiDbStatusLabel, UITheme.SUCCESS, "Engine status & connectivity"));

        // Sixth Card: Quick Action Center
        JPanel actionsCard = new JPanel(new GridLayout(0, 1, 6, 8));
        actionsCard.setBackground(UITheme.CARD_BG);
        actionsCard.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true), new EmptyBorder(14, 16, 14, 16)));
        JLabel quickTag = new JLabel("⚡ Quick Administrator Actions");
        quickTag.setFont(UITheme.FONT_BOLD);
        quickTag.setForeground(UITheme.TEXT_MAIN);
        actionsCard.add(quickTag);

        JButton gotoClaims = UITheme.createPrimaryButton("Review Pending Claims →");
        gotoClaims.addActionListener(e -> mainTabs.setSelectedIndex(1));
        JButton gotoUsers = UITheme.createSecondaryButton("Manage Users & Roles →");
        gotoUsers.addActionListener(e -> mainTabs.setSelectedIndex(3));

        actionsCard.add(gotoClaims);
        actionsCard.add(gotoUsers);
        kpiGrid.add(actionsCard);

        panel.add(kpiGrid, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, Color accentColor, String footerText) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(UITheme.CARD_BG);
        card.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(14, 18, 14, 18)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_BOLD);
        titleLbl.setForeground(UITheme.TEXT_MUTED);
        card.add(titleLbl, BorderLayout.NORTH);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(accentColor);
        card.add(valueLabel, BorderLayout.CENTER);

        JLabel foot = new JLabel(footerText);
        foot.setFont(UITheme.FONT_SMALL);
        foot.setForeground(UITheme.TEXT_DIMMED);
        card.add(foot, BorderLayout.SOUTH);

        return card;
    }

    // ─── 2. Claims Moderation Tab ────────────────────────────────────────────

    private JPanel buildClaimsPanel() {
        JPanel panel = createManagementPanel("Claim Review & Moderation",
                "Review proof messages from claimants. Approve or reject claims with moderator notes.");

        // Toolbar Filter
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        toolBar.setBackground(UITheme.CARD_BG);
        toolBar.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true), new EmptyBorder(4, 8, 4, 8)));

        JLabel filterLbl = UITheme.createFieldLabel("Filter Status:");
        claimFilterCombo = new JComboBox<>(new String[]{"All Claims", "PENDING", "APPROVED", "REJECTED"});
        claimFilterCombo.setFont(UITheme.FONT_BODY);
        claimFilterCombo.addActionListener(e -> refreshClaims());

        toolBar.add(filterLbl);
        toolBar.add(claimFilterCombo);

        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setBackground(UITheme.BG_DARK);
        centerContainer.add(toolBar, BorderLayout.NORTH);

        claimTableModel = new ClaimTableModel();
        claimTable = createTable(claimTableModel);
        claimTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        claimTable.getColumnModel().getColumn(1).setPreferredWidth(170);
        claimTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        claimTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        claimTable.getColumnModel().getColumn(4).setPreferredWidth(210);
        claimTable.getColumnModel().getColumn(5).setPreferredWidth(95);
        claimTable.getColumnModel().getColumn(6).setPreferredWidth(120);

        centerContainer.add(new JScrollPane(claimTable), BorderLayout.CENTER);
        panel.add(centerContainer, BorderLayout.CENTER);

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(UITheme.BG_DARK);

        JButton approveBtn = UITheme.createSuccessButton("✓ Approve Claim");
        JButton rejectBtn = UITheme.createDangerButton("✕ Reject Claim");
        JButton refreshBtn = UITheme.createSecondaryButton("Refresh Claims");

        approveBtn.addActionListener(e -> approveSelectedClaim());
        rejectBtn.addActionListener(e -> rejectSelectedClaim());
        refreshBtn.addActionListener(e -> refreshClaims());

        actions.add(refreshBtn);
        actions.add(rejectBtn);
        actions.add(approveBtn);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private void approveSelectedClaim() {
        int row = claimTable.getSelectedRow();
        if (row < 0) {
            showInfo("Please select a claim from the table first.");
            return;
        }

        Claim claim = claimTableModel.getClaimAt(row);
        if (claim == null) return;

        if (claim.getStatus() != ClaimStatus.PENDING) {
            showInfo("This claim has already been decided (" + claim.getStatus() + ").");
            return;
        }

        String comment = JOptionPane.showInputDialog(this,
                "Enter approval comment/instructions for claimant (optional):",
                "Approve Claim #" + claim.getId(),
                JOptionPane.PLAIN_MESSAGE);

        if (comment == null) return; // User cancelled

        try {
            controller.approveClaim(activeUser(), claim.getId(), comment);
            showInfo("Claim #" + claim.getId() + " has been APPROVED.\nItem status updated to CLAIMED and notification sent.");
            refreshClaims();
            refreshOverviewKpis();
            refreshAllItems();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void rejectSelectedClaim() {
        int row = claimTable.getSelectedRow();
        if (row < 0) {
            showInfo("Please select a claim from the table first.");
            return;
        }

        Claim claim = claimTableModel.getClaimAt(row);
        if (claim == null) return;

        if (claim.getStatus() != ClaimStatus.PENDING) {
            showInfo("This claim has already been decided (" + claim.getStatus() + ").");
            return;
        }

        String comment = JOptionPane.showInputDialog(this,
                "Enter reason for claim rejection (required):",
                "Reject Claim #" + claim.getId(),
                JOptionPane.WARNING_MESSAGE);

        if (comment == null || comment.trim().isEmpty()) {
            showInfo("Rejection reason is required.");
            return;
        }

        try {
            controller.rejectClaim(activeUser(), claim.getId(), comment);
            showInfo("Claim #" + claim.getId() + " has been REJECTED.\nNotification sent to claimant.");
            refreshClaims();
            refreshOverviewKpis();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    // ─── 3. All Items Oversight Tab ──────────────────────────────────────────

    private JPanel buildAllItemsPanel() {
        JPanel panel = createManagementPanel("Item Inventory Oversight",
                "Full inventory of all items (Lost & Found) reported across campus.");

        allItemsTableModel = new LostItemTableModel();
        allItemsTable = createTable(allItemsTableModel);

        panel.add(new JScrollPane(allItemsTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(UITheme.BG_DARK);

        JButton refresh = UITheme.createSecondaryButton("Refresh Inventory");
        JButton delete = UITheme.createDangerButton("Delete Selected Item");

        refresh.addActionListener(e -> refreshAllItems());
        delete.addActionListener(e -> deleteSelectedItemAsAdmin());

        actions.add(refresh);
        actions.add(delete);
        panel.add(actions, BorderLayout.SOUTH);

        return panel;
    }

    private void deleteSelectedItemAsAdmin() {
        int row = allItemsTable.getSelectedRow();
        if (row < 0) {
            showInfo("Please select an item first.");
            return;
        }

        Item item = allItemsTableModel.getItemAt(row);
        if (item == null) return;

        int res = JOptionPane.showConfirmDialog(this,
                "Administrator Action: Permanently delete item #" + item.getId() + " - \"" + item.getTitle() + "\"?",
                "Confirm Administrator Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (res == JOptionPane.YES_OPTION) {
            try {
                controller.deleteItem(activeUser(), item.getId());
                showInfo("Item #" + item.getId() + " was deleted.");
                refreshAllItems();
                refreshOverviewKpis();
            } catch (Exception ex) {
                showError(ex);
            }
        }
    }

    // ─── 4. User Accounts Tab ────────────────────────────────────────────────

    private JPanel buildUsersPanel() {
        JPanel panel = createManagementPanel("User Account Management",
                "Manage user accounts, assign Administrator privileges, or deactivate users.");

        userTableModel = new UserTableModel();
        userTable = createTable(userTableModel);
        userTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(180);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(240);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(100);

        panel.add(new JScrollPane(userTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(UITheme.BG_DARK);

        JButton refresh = UITheme.createSecondaryButton("Refresh");
        JButton edit = UITheme.createPrimaryButton("Edit User / Role");
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

    // ─── 5. Categories Tab ───────────────────────────────────────────────────

    private JPanel buildCategoriesPanel() {
        JPanel panel = createManagementPanel("Category Management",
                "Add, rename or remove item classification categories.");

        categoryTableModel = new CategoryTableModel();
        categoryTable = createTable(categoryTableModel);

        panel.add(new JScrollPane(categoryTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(UITheme.BG_DARK);

        JButton add = UITheme.createPrimaryButton("+ Add Category");
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

    // ─── 6. Locations Tab ────────────────────────────────────────────────────

    private JPanel buildLocationsPanel() {
        JPanel panel = createManagementPanel("Campus Location Management",
                "Define buildings, rooms, and campuses where items are found or lost.");

        locationTableModel = new LocationTableModel();
        locationTable = createTable(locationTableModel);

        panel.add(new JScrollPane(locationTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(UITheme.BG_DARK);

        JButton add = UITheme.createPrimaryButton("+ Add Location");
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
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UITheme.BG_DARK);

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
        UITheme.styleTable(table);
        return table;
    }

    private User activeUser() {
        return sessionContext.getCurrentUser();
    }

    public void refreshAll() {
        try {
            controller.requireAdmin(activeUser());
            refreshOverviewKpis();
            refreshClaims();
            refreshAllItems();
            refreshUsers();
            refreshCategories();
            refreshLocations();
        } catch (Exception ex) {
            showError(ex);
            dispose();
        }
    }

    private void refreshOverviewKpis() {
        try {
            AdminController.DashboardMetrics metrics = controller.getDashboardMetrics(activeUser());
            kpiLostCountLabel.setText(String.valueOf(metrics.totalLostItems()));
            kpiFoundCountLabel.setText(String.valueOf(metrics.totalFoundItems()));
            kpiClaimsCountLabel.setText(String.valueOf(metrics.pendingClaims()));
            kpiUsersCountLabel.setText(String.valueOf(metrics.totalUsers()));

            boolean fallback = DatabaseConnection.isUsingFallback();
            kpiDbStatusLabel.setText(fallback ? "In-Memory Resilient Mode" : "MySQL Connected");
            kpiDbStatusLabel.setForeground(fallback ? UITheme.WARNING : UITheme.SUCCESS);
        } catch (Exception ex) {
            kpiDbStatusLabel.setText("Offline");
            kpiDbStatusLabel.setForeground(UITheme.DANGER);
        }
    }

    private void refreshClaims() {
        try {
            String filter = claimFilterCombo != null ? (String) claimFilterCombo.getSelectedItem() : "All Claims";
            List<Claim> list;
            if ("PENDING".equalsIgnoreCase(filter)) {
                list = controller.getPendingClaims(activeUser());
            } else {
                list = controller.getClaims(activeUser());
                if (filter != null && !filter.startsWith("All")) {
                    ClaimStatus st = ClaimStatus.valueOf(filter);
                    list = list.stream().filter(c -> c.getStatus() == st).toList();
                }
            }
            claimTableModel.setClaims(list);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void refreshAllItems() {
        try {
            List<Item> list = controller.getAllItems(activeUser());
            allItemsTableModel.setItems(list);
        } catch (Exception ex) {
            showError(ex);
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
        form.setBackground(UITheme.CARD_BG);
        form.add(UITheme.createFieldLabel("Name"));
        form.add(name);
        form.add(UITheme.createFieldLabel("Email"));
        form.add(email);
        form.add(UITheme.createFieldLabel("Role"));
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
            refreshOverviewKpis();
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
            refreshOverviewKpis();
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
        form.setBackground(UITheme.CARD_BG);
        form.add(UITheme.createFieldLabel("Campus *"));
        form.add(campus);
        form.add(UITheme.createFieldLabel("Building"));
        form.add(building);
        form.add(UITheme.createFieldLabel("Room"));
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
        JOptionPane.showMessageDialog(this, message, "ReFind Admin", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.";
        JOptionPane.showMessageDialog(this, message, "ReFind - Error", JOptionPane.ERROR_MESSAGE);
    }
}
