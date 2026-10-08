package com.refind.ui;

import com.refind.controller.AdminController;
import com.refind.dao.impl.*;
import com.refind.exception.ValidationException;
import com.refind.model.Category;
import com.refind.model.Item;
import com.refind.model.Location;
import com.refind.model.User;
import com.refind.model.enums.ItemStatus;
import com.refind.model.enums.ItemType;
import com.refind.model.enums.Role;
import com.refind.service.*;
import com.refind.service.impl.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Unified Modern Full-Stack Desktop Portal for ReFind.
 * Brings together Lost Items, Found Items, Claiming, Reporting,
 * Authentication, and Administrator Management in a Dark Navy Blue theme.
 */
public class ReFindApp extends JFrame implements SessionContext.SessionListener {

    private final UserService userService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final ItemService itemService;
    private final AuthService authService;
    private final ClaimService claimService;
    private final ModerationService moderationService;
    private final NotificationService notificationService;
    private final SessionContext sessionContext;

    // Main Navigation Tabs
    private JTabbedPane mainTabbedPane;

    // Header Components
    private JLabel userDisplayLabel;
    private JLabel roleBadgeLabel;
    private JButton adminDashboardNavBtn;
    private JButton authActionBtn;

    // Lost Tab Components
    private JTextField lostSearchField;
    private JComboBox<CategoryWrapper> lostCatFilterCombo;
    private JComboBox<String> lostStatusFilterCombo;
    private JTable lostTable;
    private LostItemTableModel lostTableModel;
    private JLabel lostCountLabel;

    // Found Tab Components
    private JTextField foundSearchField;
    private JComboBox<CategoryWrapper> foundCatFilterCombo;
    private JComboBox<String> foundStatusFilterCombo;
    private JTable foundTable;
    private FoundItemTableModel foundTableModel;
    private JLabel foundCountLabel;

    // Report Tab Components
    private JRadioButton reportLostRadio;
    private JRadioButton reportFoundRadio;
    private JTextField reportTitleField;
    private JComboBox<CategoryWrapper> reportCategoryCombo;
    private JComboBox<LocationWrapper> reportLocationCombo;
    private JTextField reportImageField;
    private JTextArea reportDescArea;
    private JLabel reportUserLabel;

    public static void main(String[] args) {
        UITheme.setupLookAndFeel();

        SwingUtilities.invokeLater(() -> {
            try {
                // Initialize DAOs
                var userDAO = new JdbcUserDAO();
                var categoryDAO = new JdbcCategoryDAO();
                var locationDAO = new JdbcLocationDAO();
                var itemDAO = new JdbcItemDAO();
                var claimDAO = new JdbcClaimDAO();
                var modDAO = new JdbcModerationDAO();
                var notifDAO = new JdbcNotificationDAO();

                // Initialize Services
                var userService = new JdbcUserService(userDAO);
                var categoryService = new JdbcCategoryService(categoryDAO);
                var locationService = new JdbcLocationService(locationDAO);
                var itemService = new JdbcItemService(itemDAO);
                var authService = new JdbcAuthService(userDAO);
                var claimService = new JdbcClaimService(claimDAO);
                var notifService = new JdbcNotificationService(notifDAO);
                var moderationService = new JdbcModerationService(claimDAO, modDAO, itemDAO, notifDAO);

                // Auto-seed sample test records
                seedInitialData(userService, categoryService, locationService, itemService, authService);

                // Initialize Session
                var sessionContext = new SessionContext(userService);

                // Launch Unified Window
                var app = new ReFindApp(
                        userService, categoryService, locationService, itemService,
                        authService, claimService, moderationService, notifService, sessionContext
                );
                app.setVisible(true);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to launch ReFind Portal:\n" + ex.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public ReFindApp(UserService userService, CategoryService categoryService,
                     LocationService locationService, ItemService itemService,
                     AuthService authService, ClaimService claimService,
                     ModerationService moderationService, NotificationService notificationService,
                     SessionContext sessionContext) {
        super("ReFind - Campus Lost & Found Management System");
        this.userService = userService;
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.itemService = itemService;
        this.authService = authService;
        this.claimService = claimService;
        this.moderationService = moderationService;
        this.notificationService = notificationService;
        this.sessionContext = sessionContext;

        sessionContext.addSessionListener(this);

        initWindow();
        loadAllData();
    }

    private void initWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setMinimumSize(new Dimension(950, 650));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_DARK);
        setContentPane(root);

        // Header
        root.add(buildHeader(), BorderLayout.NORTH);

        // Tab Navigation
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(UITheme.FONT_BOLD);
        mainTabbedPane.setBackground(UITheme.BG_DARK);
        mainTabbedPane.setForeground(UITheme.TEXT_MUTED);

        mainTabbedPane.addTab("🔍 Lost Items Portal", buildLostPanel());
        mainTabbedPane.addTab("📦 Found Items Portal", buildFoundPanel());
        mainTabbedPane.addTab("➕ Report an Item", buildReportPanel());

        root.add(mainTabbedPane, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(UITheme.CARD_BG);
        header.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER, 1),
                new EmptyBorder(14, 24, 14, 24)
        ));

        // Brand
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setBackground(UITheme.CARD_BG);

        JLabel logo = new JLabel("ReFind");
        logo.setFont(UITheme.FONT_BRAND);
        logo.setForeground(UITheme.PRIMARY);

        JLabel sub = new JLabel("|  Campus Lost & Found Portal");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_MUTED);

        brand.add(logo);
        brand.add(sub);
        header.add(brand, BorderLayout.WEST);

        // User & Actions
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        userPanel.setBackground(UITheme.CARD_BG);

        JLabel userPrefix = new JLabel("Active Account:");
        userPrefix.setFont(UITheme.FONT_SMALL);
        userPrefix.setForeground(UITheme.TEXT_MUTED);

        userDisplayLabel = new JLabel("Checking session...");
        userDisplayLabel.setFont(UITheme.FONT_BOLD);
        userDisplayLabel.setForeground(UITheme.TEXT_MAIN);

        roleBadgeLabel = UITheme.createRoleBadge("USER");

        adminDashboardNavBtn = UITheme.createPrimaryButton("🛡️ Admin Dashboard");
        adminDashboardNavBtn.addActionListener(e -> openAdminDashboard());

        authActionBtn = UITheme.createSecondaryButton("🔑 Sign In / Switch");
        authActionBtn.addActionListener(e -> openLoginDialog());

        JButton logoutBtn = UITheme.createSecondaryButton("Sign Out");
        logoutBtn.addActionListener(e -> performLogout());

        userPanel.add(userPrefix);
        userPanel.add(userDisplayLabel);
        userPanel.add(roleBadgeLabel);
        userPanel.add(adminDashboardNavBtn);
        userPanel.add(authActionBtn);
        userPanel.add(logoutBtn);

        header.add(userPanel, BorderLayout.EAST);
        updateUserHeader();

        return header;
    }

    private void updateUserHeader() {
        User cur = sessionContext.getCurrentUser();
        if (cur != null) {
            userDisplayLabel.setText(cur.getName());
            boolean isAdmin = cur.getRole() == Role.ADMIN;
            roleBadgeLabel.setText(cur.getRole().name());
            roleBadgeLabel.setForeground(isAdmin ? UITheme.PURPLE : UITheme.INFO);
            adminDashboardNavBtn.setVisible(isAdmin);
            authActionBtn.setText("Switch Account");
            if (reportUserLabel != null) {
                reportUserLabel.setText(cur.getName() + " (" + cur.getEmail() + ")");
            }
        } else {
            userDisplayLabel.setText("Not signed in");
            roleBadgeLabel.setText("GUEST");
            roleBadgeLabel.setForeground(UITheme.TEXT_MUTED);
            adminDashboardNavBtn.setVisible(false);
            authActionBtn.setText("🔑 Sign In");
            if (reportUserLabel != null) {
                reportUserLabel.setText("Please sign in first");
            }
        }
    }

    private void openLoginDialog() {
        LoginDialog dialog = new LoginDialog(this, authService, userService, sessionContext, () -> {
            updateUserHeader();
            loadAllData();
        });
        dialog.setVisible(true);
    }

    private void performLogout() {
        sessionContext.logout();
        updateUserHeader();
        openLoginDialog();
    }

    private void openAdminDashboard() {
        if (!sessionContext.isCurrentUserAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "Access Denied: Administrator privileges required.",
                    "Admin Authorization", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AdminFrame adminFrame = new AdminFrame(
                userService, categoryService, locationService, itemService,
                claimService, moderationService, sessionContext
        );
        adminFrame.setVisible(true);
    }

    // ─── Lost Items Portal ───────────────────────────────────────────────────

    private JPanel buildLostPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Filter Bar
        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterCard.setBackground(UITheme.CARD_BG);
        filterCard.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true), new EmptyBorder(4, 8, 4, 8)));

        JLabel searchLbl = UITheme.createFieldLabel("Keyword:");
        lostSearchField = UITheme.createTextField(14);
        lostSearchField.setToolTipText("Search title or description...");
        lostSearchField.addActionListener(e -> filterLostItems());

        JLabel catLbl = UITheme.createFieldLabel("Category:");
        lostCatFilterCombo = new JComboBox<>();
        lostCatFilterCombo.setFont(UITheme.FONT_BODY);
        lostCatFilterCombo.addActionListener(e -> filterLostItems());

        JLabel statusLbl = UITheme.createFieldLabel("Status:");
        lostStatusFilterCombo = new JComboBox<>(new String[]{"All Statuses", "OPEN", "LOST", "CLAIMED", "RETURNED", "CLOSED"});
        lostStatusFilterCombo.setFont(UITheme.FONT_BODY);
        lostStatusFilterCombo.addActionListener(e -> filterLostItems());

        JButton searchBtn = UITheme.createPrimaryButton("Search");
        searchBtn.addActionListener(e -> filterLostItems());

        JButton resetBtn = UITheme.createSecondaryButton("Reset");
        resetBtn.addActionListener(e -> {
            lostSearchField.setText("");
            if (lostCatFilterCombo.getItemCount() > 0) lostCatFilterCombo.setSelectedIndex(0);
            lostStatusFilterCombo.setSelectedIndex(0);
            loadLostItems();
        });

        filterCard.add(searchLbl);
        filterCard.add(lostSearchField);
        filterCard.add(catLbl);
        filterCard.add(lostCatFilterCombo);
        filterCard.add(statusLbl);
        filterCard.add(lostStatusFilterCombo);
        filterCard.add(searchBtn);
        filterCard.add(resetBtn);

        panel.add(filterCard, BorderLayout.NORTH);

        // Table
        lostTableModel = new LostItemTableModel();
        lostTable = new JTable(lostTableModel);
        UITheme.styleTable(lostTable);

        lostTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        lostTable.getColumnModel().getColumn(1).setPreferredWidth(190);
        lostTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        lostTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        lostTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        lostTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        lostTable.getColumnModel().getColumn(6).setPreferredWidth(90);

        lostTable.getColumnModel().getColumn(6).setCellRenderer((t, val, isSel, hasFoc, row, col) -> {
            ItemStatus s = null;
            if (val instanceof String str) {
                try { s = ItemStatus.valueOf(str); } catch (Exception ignored) {}
            }
            return UITheme.createStatusBadge(s);
        });

        lostTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && lostTable.getSelectedRow() != -1) {
                    viewSelectedLostDetails();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(lostTable);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1));
        panel.add(scroll, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG_DARK);

        lostCountLabel = new JLabel("Loading items...");
        lostCountLabel.setFont(UITheme.FONT_BOLD);
        lostCountLabel.setForeground(UITheme.TEXT_MUTED);
        footer.add(lostCountLabel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(UITheme.BG_DARK);

        JButton viewBtn = UITheme.createSecondaryButton("View Details");
        JButton editBtn = UITheme.createSecondaryButton("Edit Item");
        JButton deleteBtn = UITheme.createDangerButton("Delete Item");
        JButton reportNewBtn = UITheme.createPrimaryButton("+ Report Lost Item");

        viewBtn.addActionListener(e -> viewSelectedLostDetails());
        editBtn.addActionListener(e -> editSelectedLostItem());
        deleteBtn.addActionListener(e -> deleteSelectedLostItem());
        reportNewBtn.addActionListener(e -> {
            reportLostRadio.setSelected(true);
            mainTabbedPane.setSelectedIndex(2);
        });

        actions.add(viewBtn);
        actions.add(editBtn);
        actions.add(deleteBtn);
        actions.add(reportNewBtn);

        footer.add(actions, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    // ─── Found Items Portal ──────────────────────────────────────────────────

    private JPanel buildFoundPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Filter Bar
        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterCard.setBackground(UITheme.CARD_BG);
        filterCard.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true), new EmptyBorder(4, 8, 4, 8)));

        JLabel searchLbl = UITheme.createFieldLabel("Keyword:");
        foundSearchField = UITheme.createTextField(14);
        foundSearchField.setToolTipText("Search title or description...");
        foundSearchField.addActionListener(e -> filterFoundItems());

        JLabel catLbl = UITheme.createFieldLabel("Category:");
        foundCatFilterCombo = new JComboBox<>();
        foundCatFilterCombo.setFont(UITheme.FONT_BODY);
        foundCatFilterCombo.addActionListener(e -> filterFoundItems());

        JLabel statusLbl = UITheme.createFieldLabel("Status:");
        foundStatusFilterCombo = new JComboBox<>(new String[]{"All Statuses", "FOUND", "OPEN", "CLAIMED", "RETURNED", "CLOSED"});
        foundStatusFilterCombo.setFont(UITheme.FONT_BODY);
        foundStatusFilterCombo.addActionListener(e -> filterFoundItems());

        JButton searchBtn = UITheme.createPrimaryButton("Search");
        searchBtn.addActionListener(e -> filterFoundItems());

        JButton resetBtn = UITheme.createSecondaryButton("Reset");
        resetBtn.addActionListener(e -> {
            foundSearchField.setText("");
            if (foundCatFilterCombo.getItemCount() > 0) foundCatFilterCombo.setSelectedIndex(0);
            foundStatusFilterCombo.setSelectedIndex(0);
            loadFoundItems();
        });

        filterCard.add(searchLbl);
        filterCard.add(foundSearchField);
        filterCard.add(catLbl);
        filterCard.add(foundCatFilterCombo);
        filterCard.add(statusLbl);
        filterCard.add(foundStatusFilterCombo);
        filterCard.add(searchBtn);
        filterCard.add(resetBtn);

        panel.add(filterCard, BorderLayout.NORTH);

        // Table
        foundTableModel = new FoundItemTableModel();
        foundTable = new JTable(foundTableModel);
        UITheme.styleTable(foundTable);

        foundTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        foundTable.getColumnModel().getColumn(1).setPreferredWidth(190);
        foundTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        foundTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        foundTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        foundTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        foundTable.getColumnModel().getColumn(6).setPreferredWidth(90);

        foundTable.getColumnModel().getColumn(6).setCellRenderer((t, val, isSel, hasFoc, row, col) -> {
            ItemStatus s = null;
            if (val instanceof String str) {
                try { s = ItemStatus.valueOf(str); } catch (Exception ignored) {}
            }
            return UITheme.createStatusBadge(s);
        });

        foundTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && foundTable.getSelectedRow() != -1) {
                    viewSelectedFoundDetails();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(foundTable);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1));
        panel.add(scroll, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(UITheme.BG_DARK);

        foundCountLabel = new JLabel("Loading items...");
        foundCountLabel.setFont(UITheme.FONT_BOLD);
        foundCountLabel.setForeground(UITheme.TEXT_MUTED);
        footer.add(foundCountLabel, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(UITheme.BG_DARK);

        JButton claimBtn = UITheme.createPrimaryButton("✋ Claim This Item");
        JButton viewBtn = UITheme.createSecondaryButton("View Details");
        JButton editBtn = UITheme.createSecondaryButton("Edit Item");
        JButton deleteBtn = UITheme.createDangerButton("Delete Item");
        JButton reportNewBtn = UITheme.createSecondaryButton("+ Report Found Item");

        claimBtn.addActionListener(e -> claimSelectedFoundItem());
        viewBtn.addActionListener(e -> viewSelectedFoundDetails());
        editBtn.addActionListener(e -> editSelectedFoundItem());
        deleteBtn.addActionListener(e -> deleteSelectedFoundItem());
        reportNewBtn.addActionListener(e -> {
            reportFoundRadio.setSelected(true);
            mainTabbedPane.setSelectedIndex(2);
        });

        actions.add(claimBtn);
        actions.add(viewBtn);
        actions.add(editBtn);
        actions.add(deleteBtn);
        actions.add(reportNewBtn);

        footer.add(actions, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    // ─── Report an Item Form Tab ─────────────────────────────────────────────

    private JPanel buildReportPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(UITheme.BG_DARK);
        container.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel formCard = new JPanel(new BorderLayout(0, 16));
        formCard.setBackground(UITheme.CARD_BG);
        formCard.setBorder(UITheme.createCardBorder());

        // Header
        JPanel cardHeader = new JPanel(new BorderLayout());
        cardHeader.setBackground(UITheme.CARD_BG);
        JLabel title = new JLabel("Report a Lost or Found Item");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);
        JLabel subtitle = new JLabel("Choose the report type, fill in item attributes, and publish to the campus registry.");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);
        cardHeader.add(title, BorderLayout.NORTH);
        cardHeader.add(subtitle, BorderLayout.SOUTH);
        formCard.add(cardHeader, BorderLayout.NORTH);

        // Grid
        JPanel formGrid = new JPanel(new GridBagLayout());
        formGrid.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        int row = 0;

        // Type Radios
        gbc.gridx = 0; gbc.gridy = row++;
        JLabel typeLbl = UITheme.createFieldLabel("Item Type *");
        formGrid.add(typeLbl, gbc);

        gbc.gridy = row++;
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        radioPanel.setBackground(UITheme.CARD_BG);
        reportLostRadio = new JRadioButton("I Lost Something (LOST)", true);
        reportFoundRadio = new JRadioButton("I Found Something (FOUND)");
        reportLostRadio.setFont(UITheme.FONT_BOLD);
        reportLostRadio.setForeground(UITheme.WARNING);
        reportFoundRadio.setFont(UITheme.FONT_BOLD);
        reportFoundRadio.setForeground(UITheme.PRIMARY);

        ButtonGroup grp = new ButtonGroup();
        grp.add(reportLostRadio);
        grp.add(reportFoundRadio);
        radioPanel.add(reportLostRadio);
        radioPanel.add(reportFoundRadio);
        formGrid.add(radioPanel, gbc);

        // Title
        gbc.gridy = row++;
        JLabel tLbl = UITheme.createFieldLabel("Item Title *");
        formGrid.add(tLbl, gbc);

        gbc.gridy = row++;
        reportTitleField = UITheme.createTextField(30);
        formGrid.add(reportTitleField, gbc);

        // Category
        gbc.gridy = row++;
        JLabel cLbl = UITheme.createFieldLabel("Category *");
        formGrid.add(cLbl, gbc);

        gbc.gridy = row++;
        reportCategoryCombo = new JComboBox<>();
        reportCategoryCombo.setFont(UITheme.FONT_BODY);
        formGrid.add(reportCategoryCombo, gbc);

        // Location
        gbc.gridy = row++;
        JLabel lLbl = UITheme.createFieldLabel("Location (Optional)");
        formGrid.add(lLbl, gbc);

        gbc.gridy = row++;
        reportLocationCombo = new JComboBox<>();
        reportLocationCombo.setFont(UITheme.FONT_BODY);
        formGrid.add(reportLocationCombo, gbc);

        // Description
        gbc.gridy = row++;
        JLabel dLbl = UITheme.createFieldLabel("Detailed Description");
        formGrid.add(dLbl, gbc);

        gbc.gridy = row++;
        reportDescArea = UITheme.createTextArea(4, 30);
        JScrollPane descScroll = new JScrollPane(reportDescArea);
        descScroll.setBorder(new LineBorder(UITheme.BORDER, 1, true));
        formGrid.add(descScroll, gbc);

        // Image Path
        gbc.gridy = row++;
        JLabel iLbl = UITheme.createFieldLabel("Image File / URL (Optional)");
        formGrid.add(iLbl, gbc);

        gbc.gridy = row++;
        reportImageField = UITheme.createTextField(30);
        formGrid.add(reportImageField, gbc);

        // Meta Box
        gbc.gridy = row++;
        JPanel metaBox = new JPanel(new GridLayout(1, 2, 10, 4));
        metaBox.setBackground(UITheme.BG_DARK);
        metaBox.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true), new EmptyBorder(8, 12, 8, 12)));

        JLabel repTag = new JLabel("Reporting User: ");
        repTag.setFont(UITheme.FONT_SMALL);
        repTag.setForeground(UITheme.TEXT_MUTED);
        reportUserLabel = new JLabel("Checking...");
        reportUserLabel.setFont(UITheme.FONT_BOLD);
        reportUserLabel.setForeground(UITheme.TEXT_MAIN);

        metaBox.add(repTag);
        metaBox.add(reportUserLabel);
        formGrid.add(metaBox, gbc);

        formCard.add(formGrid, BorderLayout.CENTER);

        // Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(UITheme.CARD_BG);

        JButton submitBtn = UITheme.createPrimaryButton("Submit Item Report");
        JButton clearBtn = UITheme.createSecondaryButton("Clear Form");

        submitBtn.addActionListener(e -> submitNewReport());
        clearBtn.addActionListener(e -> clearReportForm());

        footer.add(clearBtn);
        footer.add(submitBtn);
        formCard.add(footer, BorderLayout.SOUTH);

        JScrollPane formScroll = new JScrollPane(formCard);
        formScroll.setBorder(null);
        formScroll.setBackground(UITheme.BG_DARK);
        container.add(formScroll, BorderLayout.CENTER);

        return container;
    }

    private void submitNewReport() {
        String title = reportTitleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        CategoryWrapper catWrap = (CategoryWrapper) reportCategoryCombo.getSelectedItem();
        if (catWrap == null || catWrap.category == null) {
            JOptionPane.showMessageDialog(this, "Category is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User currentUser = sessionContext.getCurrentUser();
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Please sign in to submit reports.", "Authentication Required", JOptionPane.WARNING_MESSAGE);
            openLoginDialog();
            return;
        }

        LocationWrapper locWrap = (LocationWrapper) reportLocationCombo.getSelectedItem();
        String desc = reportDescArea.getText().trim();
        String img = reportImageField.getText().trim();
        boolean isLost = reportLostRadio.isSelected();

        Item newItem = new Item();
        newItem.setTitle(title);
        newItem.setDescription(desc.isEmpty() ? null : desc);
        newItem.setType(isLost ? ItemType.LOST : ItemType.FOUND);
        newItem.setStatus(isLost ? ItemStatus.OPEN : ItemStatus.FOUND);
        newItem.setCategory(catWrap.category);
        newItem.setLocation(locWrap != null ? locWrap.location : null);
        newItem.setImagePath(img.isEmpty() ? null : img);
        newItem.setReportedBy(currentUser);
        newItem.setReportedAt(LocalDateTime.now());

        try {
            Item saved = itemService.reportItem(newItem);
            JOptionPane.showMessageDialog(this,
                    (isLost ? "Lost" : "Found") + " item reported successfully!\nItem ID: #" + saved.getId(),
                    "Report Submitted", JOptionPane.INFORMATION_MESSAGE);
            clearReportForm();
            loadAllData();
            mainTabbedPane.setSelectedIndex(isLost ? 0 : 1);
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to submit: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearReportForm() {
        reportTitleField.setText("");
        reportDescArea.setText("");
        reportImageField.setText("");
        if (reportCategoryCombo.getItemCount() > 0) reportCategoryCombo.setSelectedIndex(0);
        if (reportLocationCombo.getItemCount() > 0) reportLocationCombo.setSelectedIndex(0);
    }

    // ─── Data Loading & Filtering ────────────────────────────────────────────

    private void loadAllData() {
        loadLostItems();
        loadFoundItems();
        populateCategoriesAndLocations();
        updateUserHeader();
    }

    private void loadLostItems() {
        try {
            List<Item> lostItems = itemService.getItemsByType(ItemType.LOST);
            lostTableModel.setItems(lostItems);
            lostCountLabel.setText("Showing " + lostItems.size() + " lost item" + (lostItems.size() == 1 ? "" : "s"));
        } catch (Exception ex) {
            lostCountLabel.setText("Error loading items: " + ex.getMessage());
        }
    }

    private void loadFoundItems() {
        try {
            List<Item> foundItems = itemService.getItemsByType(ItemType.FOUND);
            foundTableModel.setItems(foundItems);
            foundCountLabel.setText("Showing " + foundItems.size() + " found item" + (foundItems.size() == 1 ? "" : "s"));
        } catch (Exception ex) {
            foundCountLabel.setText("Error loading items: " + ex.getMessage());
        }
    }

    private void populateCategoriesAndLocations() {
        try {
            List<Category> cats = categoryService.getAllCategories();
            lostCatFilterCombo.removeAllItems();
            lostCatFilterCombo.addItem(new CategoryWrapper(null, "All Categories"));

            foundCatFilterCombo.removeAllItems();
            foundCatFilterCombo.addItem(new CategoryWrapper(null, "All Categories"));

            reportCategoryCombo.removeAllItems();

            for (Category c : cats) {
                CategoryWrapper w = new CategoryWrapper(c, c.getName());
                lostCatFilterCombo.addItem(w);
                foundCatFilterCombo.addItem(w);
                reportCategoryCombo.addItem(w);
            }

            List<Location> locs = locationService.getAllLocations();
            reportLocationCombo.removeAllItems();
            reportLocationCombo.addItem(new LocationWrapper(null, "No Location Assigned"));
            for (Location l : locs) {
                reportLocationCombo.addItem(new LocationWrapper(l, l.getCampus() + (l.getBuilding() != null ? " - " + l.getBuilding() : "")));
            }
        } catch (Exception ignored) {}
    }

    private void filterLostItems() {
        String keyword = lostSearchField.getText().trim();
        CategoryWrapper catItem = (CategoryWrapper) lostCatFilterCombo.getSelectedItem();
        Long catId = catItem != null && catItem.category != null ? catItem.category.getId() : null;
        String statusStr = (String) lostStatusFilterCombo.getSelectedItem();

        try {
            List<Item> list;
            if (!keyword.isEmpty()) {
                list = itemService.searchItemsByType(keyword, ItemType.LOST);
            } else if (catId != null) {
                list = itemService.getItemsByCategoryAndType(catId, ItemType.LOST);
            } else {
                list = itemService.getItemsByType(ItemType.LOST);
            }

            if (statusStr != null && !statusStr.equals("All Statuses")) {
                ItemStatus targetStatus = ItemStatus.valueOf(statusStr);
                list = list.stream().filter(i -> i.getStatus() == targetStatus).toList();
            }

            lostTableModel.setItems(list);
            lostCountLabel.setText("Found " + list.size() + " lost item" + (list.size() == 1 ? "" : "s"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Filter error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterFoundItems() {
        String keyword = foundSearchField.getText().trim();
        CategoryWrapper catItem = (CategoryWrapper) foundCatFilterCombo.getSelectedItem();
        Long catId = catItem != null && catItem.category != null ? catItem.category.getId() : null;
        String statusStr = (String) foundStatusFilterCombo.getSelectedItem();

        try {
            List<Item> list;
            if (!keyword.isEmpty()) {
                list = itemService.searchItemsByType(keyword, ItemType.FOUND);
            } else if (catId != null) {
                list = itemService.getItemsByCategoryAndType(catId, ItemType.FOUND);
            } else {
                list = itemService.getItemsByType(ItemType.FOUND);
            }

            if (statusStr != null && !statusStr.equals("All Statuses")) {
                ItemStatus targetStatus = ItemStatus.valueOf(statusStr);
                list = list.stream().filter(i -> i.getStatus() == targetStatus).toList();
            }

            foundTableModel.setItems(list);
            foundCountLabel.setText("Found " + list.size() + " found item" + (list.size() == 1 ? "" : "s"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Filter error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Actions for Selected Items ──────────────────────────────────────────

    private void viewSelectedLostDetails() {
        int row = lostTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a lost item first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Item item = lostTableModel.getItemAt(row);
        if (item != null) {
            new LostItemDetailsDialog(this, item, itemService, categoryService, locationService, sessionContext, this::loadLostItems).setVisible(true);
        }
    }

    private void editSelectedLostItem() {
        int row = lostTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a lost item first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Item item = lostTableModel.getItemAt(row);
        if (item != null) {
            if (!canEditOrDelete(item)) {
                JOptionPane.showMessageDialog(this, "Permission Denied: Only the original reporter or an Admin can edit this item.", "Edit Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new LostItemEditDialog(this, item, itemService, categoryService, locationService, sessionContext, this::loadLostItems).setVisible(true);
        }
    }

    private void deleteSelectedLostItem() {
        int row = lostTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a lost item first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Item item = lostTableModel.getItemAt(row);
        if (item != null) {
            if (!canEditOrDelete(item)) {
                JOptionPane.showMessageDialog(this, "Permission Denied: Only the original reporter or an Admin can delete this item.", "Deletion Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int opt = JOptionPane.showConfirmDialog(this, "Delete lost item #" + item.getId() + " - \"" + item.getTitle() + "\"?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                itemService.deleteItem(item.getId(), sessionContext.getCurrentUser());
                loadLostItems();
            }
        }
    }

    private void claimSelectedFoundItem() {
        int row = foundTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a found item from the table to claim.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Item item = foundTableModel.getItemAt(row);
        if (item != null) {
            new SubmitClaimDialog(this, item, claimService, sessionContext, this::loadFoundItems).setVisible(true);
        }
    }

    private void viewSelectedFoundDetails() {
        int row = foundTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a found item first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Item item = foundTableModel.getItemAt(row);
        if (item != null) {
            new FoundItemDetailsDialog(this, item, itemService, categoryService, locationService, sessionContext, this::loadFoundItems).setVisible(true);
        }
    }

    private void editSelectedFoundItem() {
        int row = foundTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a found item first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Item item = foundTableModel.getItemAt(row);
        if (item != null) {
            if (!canEditOrDelete(item)) {
                JOptionPane.showMessageDialog(this, "Permission Denied: Only the original reporter or an Admin can edit this item.", "Edit Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new FoundItemEditDialog(this, item, itemService, categoryService, locationService, sessionContext, this::loadFoundItems).setVisible(true);
        }
    }

    private void deleteSelectedFoundItem() {
        int row = foundTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a found item first.", "Selection Required", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Item item = foundTableModel.getItemAt(row);
        if (item != null) {
            if (!canEditOrDelete(item)) {
                JOptionPane.showMessageDialog(this, "Permission Denied: Only the original reporter or an Admin can delete this item.", "Deletion Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int opt = JOptionPane.showConfirmDialog(this, "Delete found item #" + item.getId() + " - \"" + item.getTitle() + "\"?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                itemService.deleteItem(item.getId(), sessionContext.getCurrentUser());
                loadFoundItems();
            }
        }
    }

    private boolean canEditOrDelete(Item item) {
        User cur = sessionContext.getCurrentUser();
        if (cur == null) return false;
        if (sessionContext.isCurrentUserAdmin()) return true;
        return item.getReportedBy() != null && cur.getId() != null && cur.getId().equals(item.getReportedBy().getId());
    }

    @Override
    public void onUserChanged(User newUser) {
        updateUserHeader();
    }

    private static void seedInitialData(UserService userService, CategoryService categoryService,
                                       LocationService locationService, ItemService itemService,
                                       AuthService authService) {
        try {
            // Seed Categories
            if (categoryService.getAllCategories().isEmpty()) {
                categoryService.createCategory("Electronics & Gadgets");
                categoryService.createCategory("Wallets, Bags & Purses");
                categoryService.createCategory("Student IDs & Documents");
                categoryService.createCategory("Keys & Keychains");
                categoryService.createCategory("Books & Stationery");
            }

            // Seed Locations
            if (locationService.getAllLocations().isEmpty()) {
                locationService.createLocation("Main Campus", "Computer Science Building", "Lab 201");
                locationService.createLocation("Main Campus", "Central Library", "Main Reading Hall");
                locationService.createLocation("North Campus", "Student Center Cafeteria", "Booth 4");
            }

            // Seed Users with real hashed passwords
            List<User> users = userService.getAllUsers();
            if (users.isEmpty()) {
                authService.register("Campus Security Officer", "admin@campus.edu", "admin123");
                authService.register("John Doe", "john@campus.edu", "password123");
                authService.register("Alice Smith", "alice@campus.edu", "password123");

                // Elevate Security Officer to ADMIN
                userService.getAllUsers().stream()
                        .filter(u -> "admin@campus.edu".equalsIgnoreCase(u.getEmail()))
                        .findFirst()
                        .ifPresent(u -> {
                            u.setRole(Role.ADMIN);
                            userService.updateUser(u);
                        });
            }

            // Seed items if empty
            List<Item> lostItems = itemService.getItemsByType(ItemType.LOST);
            List<Category> cats = categoryService.getAllCategories();
            List<Location> locs = locationService.getAllLocations();
            List<User> currentUsers = userService.getAllUsers();

            if (lostItems.isEmpty() && !currentUsers.isEmpty() && !cats.isEmpty() && !locs.isEmpty()) {
                User john = currentUsers.stream().filter(u -> "john@campus.edu".equals(u.getEmail())).findFirst().orElse(currentUsers.get(0));
                User alice = currentUsers.stream().filter(u -> "alice@campus.edu".equals(u.getEmail())).findFirst().orElse(currentUsers.get(0));

                Item item1 = new Item();
                item1.setTitle("Lost Space Grey MacBook Charger 96W");
                item1.setDescription("Left on desk 14 in computer science lab 201 with USB-C braided cable.");
                item1.setType(ItemType.LOST);
                item1.setStatus(ItemStatus.OPEN);
                item1.setCategory(cats.get(0));
                item1.setLocation(locs.get(0));
                item1.setReportedBy(john);
                item1.setReportedAt(LocalDateTime.now().minusHours(4));
                itemService.reportItem(item1);

                Item item2 = new Item();
                item2.setTitle("Brown Leather Bi-fold Wallet");
                item2.setDescription("Contains campus meal card, driver license, and transit pass.");
                item2.setType(ItemType.LOST);
                item2.setStatus(ItemStatus.OPEN);
                item2.setCategory(cats.size() > 1 ? cats.get(1) : cats.get(0));
                item2.setLocation(locs.size() > 1 ? locs.get(1) : locs.get(0));
                item2.setReportedBy(alice);
                item2.setReportedAt(LocalDateTime.now().minusHours(8));
                itemService.reportItem(item2);
            }

            List<Item> foundItems = itemService.getItemsByType(ItemType.FOUND);
            if (foundItems.isEmpty() && !currentUsers.isEmpty() && !cats.isEmpty() && !locs.isEmpty()) {
                User admin = currentUsers.stream().filter(u -> u.getRole() == Role.ADMIN).findFirst().orElse(currentUsers.get(0));

                Item found1 = new Item();
                found1.setTitle("Found Sony WH-1000XM4 Headphones");
                found1.setDescription("Black over-ear wireless headphones in protective zipper case found on 2nd floor library table.");
                found1.setType(ItemType.FOUND);
                found1.setStatus(ItemStatus.FOUND);
                found1.setCategory(cats.get(0));
                found1.setLocation(locs.size() > 1 ? locs.get(1) : locs.get(0));
                found1.setReportedBy(admin);
                found1.setReportedAt(LocalDateTime.now().minusHours(2));
                itemService.reportItem(found1);
            }
        } catch (Exception ex) {
            System.err.println("Notice during data seeding: " + ex.getMessage());
        }
    }

    public static class CategoryWrapper {
        public final Category category;
        public final String label;
        public CategoryWrapper(Category category, String label) { this.category = category; this.label = label; }
        @Override public String toString() { return label; }
    }

    public static class LocationWrapper {
        public final Location location;
        public final String label;
        public LocationWrapper(Location location, String label) { this.location = location; this.label = label; }
        @Override public String toString() { return label; }
    }
}
