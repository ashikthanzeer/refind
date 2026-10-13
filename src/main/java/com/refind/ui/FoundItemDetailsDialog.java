package com.refind.ui;

import com.refind.dao.impl.JdbcClaimDAO;
import com.refind.dao.impl.JdbcItemDAO;
import com.refind.dao.impl.JdbcNotificationDAO;
import com.refind.model.Item;
import com.refind.model.User;
import com.refind.model.enums.ItemStatus;
import com.refind.service.CategoryService;
import com.refind.service.ClaimService;
import com.refind.service.ImageService;
import com.refind.service.ItemService;
import com.refind.service.LocationService;
import com.refind.service.impl.JdbcClaimService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class FoundItemDetailsDialog extends JDialog {

    private final ItemService itemService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final SessionContext sessionContext;
    private final ClaimService claimService;
    private final ImageService imageService;
    private final Runnable onDataChanged;
    private Item item;

    public FoundItemDetailsDialog(Window parent, Item item, ItemService itemService,
                                  CategoryService categoryService, LocationService locationService,
                                  SessionContext sessionContext, Runnable onDataChanged) {
        this(parent, item, itemService, categoryService, locationService, sessionContext,
                new JdbcClaimService(new JdbcClaimDAO(), new JdbcItemDAO(), new JdbcNotificationDAO()),
                new ImageService(), onDataChanged);
    }

    public FoundItemDetailsDialog(Window parent, Item item, ItemService itemService,
                                  CategoryService categoryService, LocationService locationService,
                                  SessionContext sessionContext, ClaimService claimService,
                                  ImageService imageService, Runnable onDataChanged) {
        super(parent, "Found Item Details - #" + item.getId(), ModalityType.APPLICATION_MODAL);
        this.item = item;
        this.itemService = itemService;
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.sessionContext = sessionContext;
        this.claimService = claimService != null ? claimService
                : new JdbcClaimService(new JdbcClaimDAO(), new JdbcItemDAO(), new JdbcNotificationDAO());
        this.imageService = imageService != null ? imageService : new ImageService();
        this.onDataChanged = onDataChanged;

        initComponents();
        setSize(580, 620);
        setMinimumSize(new Dimension(500, 500));
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout(0, 14));
        contentPane.setBackground(UITheme.CARD_BG);
        contentPane.setBorder(new EmptyBorder(20, 24, 20, 24));
        setContentPane(contentPane);

        // Header Panel: Title + Status Badge
        JPanel headerPanel = new JPanel(new BorderLayout(12, 0));
        headerPanel.setBackground(UITheme.CARD_BG);

        JLabel titleLabel = new JLabel(item.getTitle());
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);

        JLabel statusBadge = UITheme.createStatusBadge(item.getStatus());

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(statusBadge, BorderLayout.EAST);
        contentPane.add(headerPanel, BorderLayout.NORTH);

        // Details Grid
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(UITheme.CARD_BG);

        bodyPanel.add(createFieldRow("Item ID:", "#" + item.getId() + " (Type: " + item.getType() + ")"));

        String catName = (item.getCategory() != null && item.getCategory().getName() != null)
                ? item.getCategory().getName()
                : (item.getCategory() != null ? "Category #" + item.getCategory().getId() : "Unassigned");
        bodyPanel.add(createFieldRow("Category:", catName));

        String locationStr = "Not specified";
        if (item.getLocation() != null) {
            StringBuilder sb = new StringBuilder();
            if (item.getLocation().getCampus() != null) sb.append(item.getLocation().getCampus());
            if (item.getLocation().getBuilding() != null) sb.append(" - ").append(item.getLocation().getBuilding());
            if (item.getLocation().getRoom() != null) sb.append(", Room ").append(item.getLocation().getRoom());
            locationStr = sb.toString();
        }
        bodyPanel.add(createFieldRow("Location:", locationStr));

        final String reporterStr;
        if (item.getReportedBy() != null) {
            String name = item.getReportedBy().getName() != null ? item.getReportedBy().getName() : "User #" + item.getReportedBy().getId();
            String email = item.getReportedBy().getEmail() != null ? " (" + item.getReportedBy().getEmail() + ")" : "";
            reporterStr = name + email;
        } else {
            reporterStr = "Unknown";
        }
        bodyPanel.add(createFieldRow("Reported By:", reporterStr));

        String dateStr = item.getReportedAt() != null
                ? item.getReportedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : "Unknown";
        bodyPanel.add(createFieldRow("Reported At:", dateStr));

        // Photograph Display with Aspect-Preserving Scaling and Click Preview
        bodyPanel.add(Box.createVerticalStrut(8));
        JLabel photoHeader = new JLabel("Photograph:");
        photoHeader.setFont(UITheme.FONT_BOLD);
        photoHeader.setForeground(UITheme.TEXT_MAIN);
        bodyPanel.add(photoHeader);
        bodyPanel.add(Box.createVerticalStrut(4));

        JLabel imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setIcon(imageService.getScaledThumbnail(item.getImagePath(), 340, 180));
        imgLabel.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        if (item.getImagePath() != null && !item.getImagePath().isBlank()) {
            imgLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            imgLabel.setToolTipText("Click to view full size photograph");
            imgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    new ImagePreviewDialog(FoundItemDetailsDialog.this, item.getTitle(), item.getImagePath(), imageService).setVisible(true);
                }
            });
        }
        bodyPanel.add(imgLabel);

        bodyPanel.add(Box.createVerticalStrut(10));

        JLabel descHeader = new JLabel("Description:");
        descHeader.setFont(UITheme.FONT_BOLD);
        descHeader.setForeground(UITheme.TEXT_MAIN);
        bodyPanel.add(descHeader);
        bodyPanel.add(Box.createVerticalStrut(6));

        JTextArea descArea = UITheme.createReadOnlyTextArea(
                item.getDescription() != null && !item.getDescription().isBlank()
                        ? item.getDescription()
                        : "No description provided."
        );
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setPreferredSize(new Dimension(500, 90));
        descScroll.setBorder(new LineBorder(UITheme.BORDER, 1));
        bodyPanel.add(descScroll);

        // Wrap scrollable body
        JScrollPane mainScroll = new JScrollPane(bodyPanel);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(14);
        contentPane.add(mainScroll, BorderLayout.CENTER);

        // Buttons Footer
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setBackground(UITheme.CARD_BG);

        User currentUser = sessionContext.getCurrentUser();
        boolean isOwner = item.getReportedBy() != null && currentUser != null && currentUser.getId() != null
                && currentUser.getId().equals(item.getReportedBy().getId());
        boolean isAdmin = sessionContext.isCurrentUserAdmin();
        boolean isPermitted = isOwner || isAdmin;
        boolean isClaimable = !isOwner && (item.getStatus() == ItemStatus.OPEN || item.getStatus() == ItemStatus.FOUND);

        JButton claimBtn = UITheme.createPrimaryButton("Claim this Found Item");
        JButton editBtn = UITheme.createSecondaryButton("Edit Item");
        JButton deleteBtn = UITheme.createDangerButton("Delete Item");
        JButton closeBtn = UITheme.createSecondaryButton("Close");

        if (isClaimable) {
            claimBtn.addActionListener(e -> {
                if (currentUser == null) {
                    JOptionPane.showMessageDialog(this, "Please sign in to submit an ownership claim.", "Sign In Required", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                new ClaimSubmissionDialog(this, item, claimService, sessionContext, imageService, () -> {
                    if (onDataChanged != null) onDataChanged.run();
                }).setVisible(true);
            });
            buttonPanel.add(claimBtn);
        }

        if (isPermitted) {
            buttonPanel.add(editBtn);
            buttonPanel.add(deleteBtn);
        }

        editBtn.addActionListener(e -> {
            FoundItemEditDialog editDialog = new FoundItemEditDialog(this, item, itemService, categoryService, locationService, sessionContext, () -> {
                item = itemService.getItemById(item.getId()).orElse(item);
                if (onDataChanged != null) onDataChanged.run();
                dispose();
            });
            editDialog.setVisible(true);
        });

        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to permanently delete this found item report:\n\"" + item.getTitle() + "\"?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    itemService.deleteItem(item.getId(), currentUser);
                    JOptionPane.showMessageDialog(this, "Item successfully deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                    if (onDataChanged != null) onDataChanged.run();
                    dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to delete item: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        closeBtn.addActionListener(e -> dispose());
        buttonPanel.add(closeBtn);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createFieldRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(UITheme.CARD_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(100, 22));

        JLabel val = new JLabel(value);
        val.setFont(UITheme.FONT_BODY);
        val.setForeground(UITheme.TEXT_MAIN);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }
}
