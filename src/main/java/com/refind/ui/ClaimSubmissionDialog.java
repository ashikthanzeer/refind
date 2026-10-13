package com.refind.ui;

import com.refind.exception.ValidationException;
import com.refind.model.Claim;
import com.refind.model.Item;
import com.refind.model.User;
import com.refind.model.enums.ClaimStatus;
import com.refind.service.ClaimService;
import com.refind.service.ImageService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;

public class ClaimSubmissionDialog extends JDialog {

    private final Item item;
    private final ClaimService claimService;
    private final SessionContext sessionContext;
    private final ImageService imageService;
    private final Runnable onClaimSubmitted;

    private JTextArea messageArea;
    private JLabel errorLabel;

    public ClaimSubmissionDialog(Window parent, Item item, ClaimService claimService,
                                 SessionContext sessionContext, ImageService imageService,
                                 Runnable onClaimSubmitted) {
        super(parent, "Submit Claim - " + item.getTitle(), ModalityType.APPLICATION_MODAL);
        this.item = item;
        this.claimService = claimService;
        this.sessionContext = sessionContext;
        this.imageService = imageService;
        this.onClaimSubmitted = onClaimSubmitted;

        initComponents();
        setSize(520, 560);
        setMinimumSize(new Dimension(460, 480));
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UITheme.CARD_BG);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        setContentPane(root);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(UITheme.CARD_BG);

        JLabel titleLabel = new JLabel("Ownership Claim");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);

        JLabel statusBadge = UITheme.createStatusBadge(item.getStatus());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(statusBadge, BorderLayout.EAST);
        root.add(headerPanel, BorderLayout.NORTH);

        // Center Content
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(UITheme.CARD_BG);

        // Item Summary Card
        JPanel itemCard = new JPanel(new BorderLayout(12, 0));
        itemCard.setBackground(new Color(248, 250, 252));
        itemCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT),
                new EmptyBorder(12, 12, 12, 12)
        ));

        // Thumbnail
        if (imageService != null) {
            JLabel thumbLabel = new JLabel(imageService.getScaledThumbnail(item.getImagePath(), 90, 70));
            thumbLabel.setPreferredSize(new Dimension(90, 70));
            itemCard.add(thumbLabel, BorderLayout.WEST);
        }

        JPanel itemInfo = new JPanel();
        itemInfo.setLayout(new BoxLayout(itemInfo, BoxLayout.Y_AXIS));
        itemInfo.setBackground(new Color(248, 250, 252));

        JLabel itemName = new JLabel(item.getTitle());
        itemName.setFont(UITheme.FONT_BOLD);
        itemName.setForeground(UITheme.TEXT_MAIN);

        String catName = item.getCategory() != null ? item.getCategory().getName() : "Unassigned";
        JLabel itemCategory = new JLabel("Category: " + catName + " (" + item.getType() + ")");
        itemCategory.setFont(UITheme.FONT_SMALL);
        itemCategory.setForeground(UITheme.TEXT_MUTED);

        itemInfo.add(itemName);
        itemInfo.add(Box.createVerticalStrut(4));
        itemInfo.add(itemCategory);
        itemCard.add(itemInfo, BorderLayout.CENTER);

        centerPanel.add(itemCard);
        centerPanel.add(Box.createVerticalStrut(14));

        // Instructions
        JLabel promptLbl = new JLabel("Explain why this item belongs to you:");
        promptLbl.setFont(UITheme.FONT_BOLD);
        promptLbl.setForeground(UITheme.TEXT_MAIN);
        promptLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(promptLbl);

        JLabel hintLbl = new JLabel("<html><body style='width: 400px; color: #718096;'>" +
                "Provide distinguishing marks, unique characteristics, exact contents, serial numbers, " +
                "or proof of purchase to assist administrators in verifying your claim.</body></html>");
        hintLbl.setFont(UITheme.FONT_SMALL);
        hintLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(Box.createVerticalStrut(4));
        centerPanel.add(hintLbl);
        centerPanel.add(Box.createVerticalStrut(8));

        // Text area
        messageArea = new JTextArea(6, 30);
        messageArea.setFont(UITheme.FONT_BODY);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_LIGHT),
                new EmptyBorder(8, 8, 8, 8)
        ));
        JScrollPane textScroll = new JScrollPane(messageArea);
        textScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(textScroll);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(Box.createVerticalStrut(6));
        centerPanel.add(errorLabel);

        root.add(centerPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setBackground(UITheme.CARD_BG);

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton submitBtn = UITheme.createPrimaryButton("Submit Claim");
        submitBtn.addActionListener(e -> submitClaim());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(submitBtn);
        root.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void submitClaim() {
        User user = sessionContext.getCurrentUser();
        if (user == null || user.getId() == null) {
            errorLabel.setText("Please sign in before submitting a claim.");
            return;
        }

        String message = messageArea.getText().trim();
        if (message.isEmpty()) {
            errorLabel.setText("Please enter ownership proof details.");
            return;
        }

        try {
            Claim claim = new Claim();
            claim.setClaimant(user);
            claim.setItem(item);
            claim.setMessage(message);
            claim.setStatus(ClaimStatus.PENDING);
            claim.setSubmittedAt(LocalDateTime.now());

            Claim saved = claimService.submitClaim(claim);

            JOptionPane.showMessageDialog(this,
                    "Claim #" + saved.getId() + " submitted successfully!\n" +
                            "An administrator will review your claim and notify you of the outcome.",
                    "Claim Submitted", JOptionPane.INFORMATION_MESSAGE);

            if (onClaimSubmitted != null) {
                onClaimSubmitted.run();
            }
            dispose();
        } catch (ValidationException ve) {
            errorLabel.setText(ve.getMessage());
        } catch (Exception ex) {
            errorLabel.setText("Error submitting claim: " + ex.getMessage());
        }
    }
}
