package com.refind.ui;

import com.refind.exception.ValidationException;
import com.refind.model.Item;
import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.service.CategoryService;
import com.refind.service.ItemService;
import com.refind.service.LocationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class LostItemDetailsDialog extends JDialog {

    private final ItemService itemService;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final SessionContext sessionContext;
    private final Runnable onDataChanged;
    private Item item;

    public LostItemDetailsDialog(Window parent, Item item, ItemService itemService,
                                 CategoryService categoryService, LocationService locationService,
                                 SessionContext sessionContext, Runnable onDataChanged) {
        super(parent, "Lost Item Details - #" + item.getId(), ModalityType.APPLICATION_MODAL);
        this.item = item;
        this.itemService = itemService;
        this.categoryService = categoryService;
        this.locationService = locationService;
        this.sessionContext = sessionContext;
        this.onDataChanged = onDataChanged;

        initComponents();
        setSize(560, 520);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout(0, 16));
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

        if (item.getImagePath() != null && !item.getImagePath().isBlank()) {
            bodyPanel.add(createFieldRow("Image Path:", item.getImagePath()));
        }

        bodyPanel.add(Box.createVerticalStrut(10));

        JLabel descHeader = new JLabel("Description:");
        descHeader.setFont(UITheme.FONT_BOLD);
        descHeader.setForeground(UITheme.TEXT_MAIN);
        bodyPanel.add(descHeader);
        bodyPanel.add(Box.createVerticalStrut(6));

        JTextArea descArea = new JTextArea(item.getDescription() != null ? item.getDescription() : "No description provided.");
        descArea.setFont(UITheme.FONT_BODY);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setEditable(false);
        descArea.setBackground(UITheme.BG_LIGHT);
        descArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setPreferredSize(new Dimension(500, 110));
        descScroll.setBorder(new LineBorder(UITheme.BORDER, 1));
        bodyPanel.add(descScroll);

        contentPane.add(bodyPanel, BorderLayout.CENTER);

        // Buttons Footer
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(UITheme.CARD_BG);

        User currentUser = sessionContext.getCurrentUser();
        boolean isOwner = item.getReportedBy() != null && currentUser != null && currentUser.getId() != null
                && currentUser.getId().equals(item.getReportedBy().getId());
        boolean isAdmin = sessionContext.isCurrentUserAdmin();
        boolean isPermitted = isOwner || isAdmin;

        JButton editBtn = UITheme.createSecondaryButton("Edit Item");
        JButton deleteBtn = UITheme.createDangerButton("Delete Item");
        JButton closeBtn = UITheme.createSecondaryButton("Close");

        if (!isPermitted) {
            editBtn.setToolTipText("Only the owner (" + reporterStr + ") or an Admin can edit this item.");
            deleteBtn.setToolTipText("Only the owner (" + reporterStr + ") or an Admin can delete this item.");
        }

        editBtn.addActionListener(e -> {
            if (!isPermitted) {
                JOptionPane.showMessageDialog(this,
                        "Permission Denied: You can only edit items you reported (unless you are an Admin).\n"
                                + "Current User: " + (currentUser != null ? currentUser.getName() : "None") + "\n"
                                + "Reported By: " + reporterStr,
                        "Edit Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            LostItemEditDialog editDialog = new LostItemEditDialog(this, item, itemService, categoryService, locationService, sessionContext, () -> {
                item = itemService.getItemById(item.getId()).orElse(item);
                if (onDataChanged != null) onDataChanged.run();
                dispose();
            });
            editDialog.setVisible(true);
        });

        deleteBtn.addActionListener(e -> {
            if (!isPermitted) {
                JOptionPane.showMessageDialog(this,
                        "Permission Denied: You can only delete items you reported (unless you are an Admin).\n"
                                + "Current User: " + (currentUser != null ? currentUser.getName() : "None") + "\n"
                                + "Reported By: " + reporterStr,
                        "Deletion Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete lost item #" + item.getId() + " - \"" + item.getTitle() + "\"?",
                    "Confirm Permitted Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                try {
                    boolean success = itemService.deleteItem(item.getId(), currentUser);
                    if (success) {
                        JOptionPane.showMessageDialog(this, "Lost item #" + item.getId() + " has been successfully deleted.", "Item Deleted", JOptionPane.INFORMATION_MESSAGE);
                        if (onDataChanged != null) onDataChanged.run();
                        dispose();
                    }
                } catch (ValidationException ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Deletion Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        closeBtn.addActionListener(e -> dispose());

        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(closeBtn);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createFieldRow(String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        row.setBackground(UITheme.CARD_BG);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(110, 20));

        JLabel val = new JLabel(value);
        val.setFont(UITheme.FONT_BODY);
        val.setForeground(UITheme.TEXT_MAIN);

        row.add(lbl);
        row.add(val);
        return row;
    }
}
