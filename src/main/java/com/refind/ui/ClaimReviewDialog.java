package com.refind.ui;

import com.refind.exception.ValidationException;
import com.refind.model.Claim;
import com.refind.model.Item;
import com.refind.model.User;
import com.refind.service.ImageService;
import com.refind.service.ItemService;
import com.refind.service.ModerationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class ClaimReviewDialog extends JDialog {

    private final Claim claim;
    private final ModerationService moderationService;
    private final ItemService itemService;
    private final SessionContext sessionContext;
    private final ImageService imageService;
    private final Runnable onDecided;

    private JTextArea commentArea;
    private JLabel errorLabel;

    public ClaimReviewDialog(Window parent, Claim claim, ModerationService moderationService,
                             ItemService itemService, SessionContext sessionContext,
                             ImageService imageService, Runnable onDecided) {
        super(parent, "Review Claim #" + claim.getId(), ModalityType.APPLICATION_MODAL);
        this.claim = claim;
        this.moderationService = moderationService;
        this.itemService = itemService;
        this.sessionContext = sessionContext;
        this.imageService = imageService;
        this.onDecided = onDecided;

        initComponents();
        setSize(580, 620);
        setMinimumSize(new Dimension(500, 520));
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(UITheme.CARD_BG);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        setContentPane(root);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.CARD_BG);

        JLabel titleLabel = new JLabel("Claim Verification & Decision");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);

        JLabel statusBadge = UITheme.createStatusBadge(claim.getStatus());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(statusBadge, BorderLayout.EAST);
        root.add(headerPanel, BorderLayout.NORTH);

        // Center Panel
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(UITheme.CARD_BG);

        // Claimant Info
        String claimantStr = claim.getClaimant() != null
                ? (claim.getClaimant().getName() != null ? claim.getClaimant().getName() : "User #" + claim.getClaimant().getId())
                + (claim.getClaimant().getEmail() != null ? " (" + claim.getClaimant().getEmail() + ")" : "")
                : "Unknown Claimant";

        centerPanel.add(createFieldRow("Claimant:", claimantStr));

        String submittedStr = claim.getSubmittedAt() != null
                ? claim.getSubmittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "Unknown";
        centerPanel.add(createFieldRow("Submitted At:", submittedStr));

        // Item Card
        Item item = null;
        if (claim.getItem() != null && claim.getItem().getId() != null) {
            item = itemService.getItemById(claim.getItem().getId()).orElse(claim.getItem());
        }

        if (item != null) {
            JPanel itemCard = new JPanel(new BorderLayout(10, 0));
            itemCard.setBackground(new Color(248, 250, 252));
            itemCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BORDER_LIGHT),
                    new EmptyBorder(10, 10, 10, 10)
            ));

            if (imageService != null && item.getImagePath() != null) {
                JLabel thumb = new JLabel(imageService.getScaledThumbnail(item.getImagePath(), 80, 60));
                itemCard.add(thumb, BorderLayout.WEST);
            }

            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setBackground(new Color(248, 250, 252));

            JLabel title = new JLabel(item.getTitle() != null ? item.getTitle() : "Item #" + item.getId());
            title.setFont(UITheme.FONT_BOLD);
            JLabel typeLbl = new JLabel("Type: " + item.getType() + " | Status: " + item.getStatus());
            typeLbl.setFont(UITheme.FONT_SMALL);
            typeLbl.setForeground(UITheme.TEXT_MUTED);

            info.add(title);
            info.add(Box.createVerticalStrut(2));
            info.add(typeLbl);
            itemCard.add(info, BorderLayout.CENTER);

            centerPanel.add(Box.createVerticalStrut(8));
            centerPanel.add(itemCard);
        }

        // Claimant Message
        centerPanel.add(Box.createVerticalStrut(10));
        JLabel msgHeader = new JLabel("Claimant's Proof of Ownership:");
        msgHeader.setFont(UITheme.FONT_BOLD);
        msgHeader.setForeground(UITheme.TEXT_MAIN);
        msgHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(msgHeader);
        centerPanel.add(Box.createVerticalStrut(4));

        JTextArea messageArea = UITheme.createReadOnlyTextArea(claim.getMessage() != null ? claim.getMessage() : "No details provided.");
        messageArea.setFont(UITheme.FONT_BODY);
        messageArea.setRows(4);
        JScrollPane msgScroll = new JScrollPane(messageArea);
        msgScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(msgScroll);

        // Moderator Decision Comment
        centerPanel.add(Box.createVerticalStrut(12));
        JLabel commentHeader = new JLabel("Reviewer Decision Note / Feedback:");
        commentHeader.setFont(UITheme.FONT_BOLD);
        commentHeader.setForeground(UITheme.TEXT_MAIN);
        commentHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(commentHeader);
        centerPanel.add(Box.createVerticalStrut(4));

        commentArea = new JTextArea(3, 30);
        commentArea.setFont(UITheme.FONT_BODY);
        commentArea.setLineWrap(true);
        commentArea.setWrapStyleWord(true);
        commentArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT),
                new EmptyBorder(6, 6, 6, 6)
        ));
        JScrollPane commentScroll = new JScrollPane(commentArea);
        commentScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(commentScroll);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(Box.createVerticalStrut(6));
        centerPanel.add(errorLabel);

        JScrollPane mainScroll = new JScrollPane(centerPanel);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(14);
        root.add(mainScroll, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setBackground(UITheme.CARD_BG);

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton rejectBtn = UITheme.createDangerButton("Reject Claim");
        rejectBtn.addActionListener(e -> performDecision(false));

        JButton approveBtn = UITheme.createPrimaryButton("Approve Claim");
        approveBtn.addActionListener(e -> performDecision(true));

        buttonPanel.add(cancelBtn);
        buttonPanel.add(rejectBtn);
        buttonPanel.add(approveBtn);
        root.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void performDecision(boolean approve) {
        User moderator = sessionContext.getCurrentUser();
        if (moderator == null || moderator.getId() == null) {
            errorLabel.setText("Administrator authentication required.");
            return;
        }

        String comment = commentArea.getText().trim();
        if (!approve && comment.isEmpty()) {
            errorLabel.setText("Please enter an explanation note when rejecting a claim.");
            return;
        }

        try {
            if (approve) {
                moderationService.approveClaim(claim.getId(), moderator.getId(),
                        comment.isEmpty() ? "Claim approved by campus administrator." : comment);
                JOptionPane.showMessageDialog(this,
                        "Claim #" + claim.getId() + " has been APPROVED!\nItem status updated to CLAIMED.",
                        "Decision Saved", JOptionPane.INFORMATION_MESSAGE);
            } else {
                moderationService.rejectClaim(claim.getId(), moderator.getId(), comment);
                JOptionPane.showMessageDialog(this,
                        "Claim #" + claim.getId() + " has been REJECTED.\nNotification sent to claimant.",
                        "Decision Saved", JOptionPane.INFORMATION_MESSAGE);
            }

            if (onDecided != null) {
                onDecided.run();
            }
            dispose();
        } catch (ValidationException ve) {
            errorLabel.setText(ve.getMessage());
        } catch (Exception ex) {
            errorLabel.setText("Moderation error: " + ex.getMessage());
        }
    }

    private JPanel createFieldRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(UITheme.CARD_BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_BOLD);
        lbl.setForeground(UITheme.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(110, 22));

        JLabel val = new JLabel(value);
        val.setFont(UITheme.FONT_BODY);
        val.setForeground(UITheme.TEXT_MAIN);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }
}
