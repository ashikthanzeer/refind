package com.refind.ui;

import com.refind.model.Claim;
import com.refind.model.Item;
import com.refind.model.User;
import com.refind.model.enums.ClaimStatus;
import com.refind.service.ClaimService;
import com.refind.service.ImageService;
import com.refind.service.ItemService;
import com.refind.service.ModerationService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClaimsPanel extends JPanel implements SessionContext.SessionListener {

    private final ClaimService claimService;
    private final ModerationService moderationService;
    private final ItemService itemService;
    private final SessionContext sessionContext;
    private final ImageService imageService;

    private JTabbedPane claimsTabs;
    private MyClaimsTableModel myClaimsModel;
    private JTable myClaimsTable;

    private PendingClaimsTableModel pendingClaimsModel;
    private JTable pendingClaimsTable;
    private JTextArea pendingClaimProofArea;
    private JButton reviewClaimBtn;

    public ClaimsPanel(ClaimService claimService, ModerationService moderationService,
                       ItemService itemService, SessionContext sessionContext,
                       ImageService imageService) {
        this.claimService = claimService;
        this.moderationService = moderationService;
        this.itemService = itemService;
        this.sessionContext = sessionContext;
        this.imageService = imageService;

        sessionContext.addSessionListener(this);

        setLayout(new BorderLayout());
        setBackground(UITheme.BG_LIGHT);
        initComponents();
        refreshClaims();
    }

    private void initComponents() {
        claimsTabs = new JTabbedPane();
        claimsTabs.setFont(UITheme.FONT_BOLD);
        claimsTabs.setBackground(UITheme.BG_LIGHT);

        claimsTabs.addTab("My Submitted Claims", buildMyClaimsTab());

        if (sessionContext.isCurrentUserAdmin()) {
            claimsTabs.addTab("Review Pending Claims", buildReviewTab());
        }

        add(claimsTabs, BorderLayout.CENTER);
    }

    private JPanel buildMyClaimsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(UITheme.BG_LIGHT);

        JLabel titleLbl = new JLabel("Claims You Have Submitted");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_MAIN);
        toolbar.add(titleLbl, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(UITheme.BG_LIGHT);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshClaims());

        JButton withdrawBtn = UITheme.createDangerButton("Withdraw Claim");
        withdrawBtn.addActionListener(e -> withdrawSelectedClaim());

        actions.add(refreshBtn);
        actions.add(withdrawBtn);
        toolbar.add(actions, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);

        // Table
        myClaimsModel = new MyClaimsTableModel(itemService);
        myClaimsTable = new JTable(myClaimsModel);
        UITheme.styleTable(myClaimsTable);
        myClaimsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Status badge renderer
        myClaimsTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof ClaimStatus) {
                    JLabel badge = UITheme.createStatusBadge((ClaimStatus) value);
                    JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
                    p.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
                    p.add(badge);
                    return p;
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        JScrollPane scrollPane = new JScrollPane(myClaimsTable);
        scrollPane.setBorder(new LineBorder(UITheme.BORDER, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildReviewTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.BG_LIGHT);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(UITheme.BG_LIGHT);

        JLabel titleLbl = new JLabel("Pending Claims Awaiting Moderation");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_MAIN);
        toolbar.add(titleLbl, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setBackground(UITheme.BG_LIGHT);

        JButton refreshBtn = UITheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshClaims());

        reviewClaimBtn = UITheme.createPrimaryButton("Review & Decide Claim");
        reviewClaimBtn.addActionListener(e -> reviewSelectedClaim());

        actions.add(refreshBtn);
        actions.add(reviewClaimBtn);
        toolbar.add(actions, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);

        // Split view: Table on top/left, proof details below
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.65);
        splitPane.setBorder(null);

        pendingClaimsModel = new PendingClaimsTableModel(itemService);
        pendingClaimsTable = new JTable(pendingClaimsModel);
        UITheme.styleTable(pendingClaimsTable);
        pendingClaimsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        pendingClaimsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = pendingClaimsTable.getSelectedRow();
                if (row >= 0) {
                    Claim c = pendingClaimsModel.getClaimAt(row);
                    pendingClaimProofArea.setText(c != null && c.getMessage() != null ? c.getMessage() : "No message.");
                } else {
                    pendingClaimProofArea.setText("");
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(pendingClaimsTable);
        tableScroll.setBorder(new LineBorder(UITheme.BORDER, 1));
        splitPane.setTopComponent(tableScroll);

        // Details preview
        JPanel proofPanel = new JPanel(new BorderLayout(0, 6));
        proofPanel.setBackground(UITheme.CARD_BG);
        proofPanel.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1), new EmptyBorder(10, 14, 10, 14)));

        JLabel proofLbl = new JLabel("Claimant Ownership Proof Statement:");
        proofLbl.setFont(UITheme.FONT_BOLD);
        proofLbl.setForeground(UITheme.TEXT_MAIN);
        proofPanel.add(proofLbl, BorderLayout.NORTH);

        pendingClaimProofArea = UITheme.createReadOnlyTextArea("Select a claim row above to read the submitted ownership proof.");
        pendingClaimProofArea.setRows(3);
        JScrollPane proofScroll = new JScrollPane(pendingClaimProofArea);
        proofPanel.add(proofScroll, BorderLayout.CENTER);

        splitPane.setBottomComponent(proofPanel);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    public void refreshClaims() {
        User user = sessionContext.getCurrentUser();
        if (user != null && user.getId() != null) {
            List<Claim> myClaims = claimService.getClaimsByClaimant(user.getId());
            myClaimsModel.setClaims(myClaims);
        } else {
            myClaimsModel.setClaims(new ArrayList<>());
        }

        if (sessionContext.isCurrentUserAdmin()) {
            List<Claim> pending = moderationService.getPendingClaims();
            if (pendingClaimsModel != null) {
                pendingClaimsModel.setClaims(pending);
            }
            if (claimsTabs.indexOfTab("Review Pending Claims") == -1) {
                claimsTabs.addTab("Review Pending Claims", buildReviewTab());
            }
        } else {
            int idx = claimsTabs.indexOfTab("Review Pending Claims");
            if (idx != -1) {
                claimsTabs.removeTabAt(idx);
            }
        }
    }

    private void withdrawSelectedClaim() {
        int row = myClaimsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a claim from the table to withdraw.", "Select Claim", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Claim claim = myClaimsModel.getClaimAt(row);
        if (claim == null) return;

        if (claim.getStatus() != ClaimStatus.PENDING) {
            JOptionPane.showMessageDialog(this, "Only pending claims can be withdrawn. This claim has already been " + claim.getStatus() + ".", "Cannot Withdraw", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to withdraw your claim #" + claim.getId() + "?",
                "Confirm Withdrawal", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                claimService.withdrawClaim(claim.getId(), sessionContext.requireAuthenticated());
                JOptionPane.showMessageDialog(this, "Claim withdrawn successfully.", "Withdrawn", JOptionPane.INFORMATION_MESSAGE);
                refreshClaims();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to withdraw claim: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void reviewSelectedClaim() {
        int row = pendingClaimsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a pending claim from the table to review.", "Select Claim", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Claim claim = pendingClaimsModel.getClaimAt(row);
        if (claim == null) return;

        Window ancestor = SwingUtilities.getWindowAncestor(this);
        ClaimReviewDialog dialog = new ClaimReviewDialog(ancestor, claim, moderationService, itemService, sessionContext, imageService, this::refreshClaims);
        dialog.setVisible(true);
    }

    @Override
    public void onUserChanged(User newUser) {
        refreshClaims();
    }

    private static class MyClaimsTableModel extends AbstractTableModel {
        private final String[] columns = {"Claim ID", "Item Title", "Submitted At", "Status", "Decided At"};
        private final ItemService itemService;
        private List<Claim> claims = new ArrayList<>();

        MyClaimsTableModel(ItemService itemService) {
            this.itemService = itemService;
        }

        void setClaims(List<Claim> claims) {
            this.claims = claims != null ? claims : new ArrayList<>();
            fireTableDataChanged();
        }

        Claim getClaimAt(int row) {
            return (row >= 0 && row < claims.size()) ? claims.get(row) : null;
        }

        @Override public int getRowCount() { return claims.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int c) { return columns[c]; }

        @Override
        public Object getValueAt(int row, int col) {
            Claim claim = claims.get(row);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            switch (col) {
                case 0: return "#" + claim.getId();
                case 1:
                    if (claim.getItem() != null && claim.getItem().getId() != null) {
                        return itemService.getItemById(claim.getItem().getId())
                                .map(Item::getTitle)
                                .orElse("Item #" + claim.getItem().getId());
                    }
                    return "Item #" + (claim.getItem() != null ? claim.getItem().getId() : "?");
                case 2: return claim.getSubmittedAt() != null ? claim.getSubmittedAt().format(dtf) : "-";
                case 3: return claim.getStatus();
                case 4: return claim.getDecidedAt() != null ? claim.getDecidedAt().format(dtf) : "Pending";
                default: return "";
            }
        }
    }

    private static class PendingClaimsTableModel extends AbstractTableModel {
        private final String[] columns = {"Claim ID", "Item Title", "Claimant ID", "Submitted At", "Status"};
        private final ItemService itemService;
        private List<Claim> claims = new ArrayList<>();

        PendingClaimsTableModel(ItemService itemService) {
            this.itemService = itemService;
        }

        void setClaims(List<Claim> claims) {
            this.claims = claims != null ? claims : new ArrayList<>();
            fireTableDataChanged();
        }

        Claim getClaimAt(int row) {
            return (row >= 0 && row < claims.size()) ? claims.get(row) : null;
        }

        @Override public int getRowCount() { return claims.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int c) { return columns[c]; }

        @Override
        public Object getValueAt(int row, int col) {
            Claim claim = claims.get(row);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            switch (col) {
                case 0: return "#" + claim.getId();
                case 1:
                    if (claim.getItem() != null && claim.getItem().getId() != null) {
                        return itemService.getItemById(claim.getItem().getId())
                                .map(Item::getTitle)
                                .orElse("Item #" + claim.getItem().getId());
                    }
                    return "Item #" + (claim.getItem() != null ? claim.getItem().getId() : "?");
                case 2:
                    return claim.getClaimant() != null
                            ? (claim.getClaimant().getName() != null ? claim.getClaimant().getName() : "User #" + claim.getClaimant().getId())
                            : "-";
                case 3: return claim.getSubmittedAt() != null ? claim.getSubmittedAt().format(dtf) : "-";
                case 4: return claim.getStatus();
                default: return "";
            }
        }
    }
}
