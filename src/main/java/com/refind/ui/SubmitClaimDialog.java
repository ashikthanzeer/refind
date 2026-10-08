package com.refind.ui;

import com.refind.exception.ValidationException;
import com.refind.model.Claim;
import com.refind.model.Item;
import com.refind.model.User;
import com.refind.model.enums.ClaimStatus;
import com.refind.service.ClaimService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.LocalDateTime;

/**
 * Modern Dark Navy dialog for submitting ownership claims on found items.
 */
public class SubmitClaimDialog extends JDialog {

    private final Item item;
    private final ClaimService claimService;
    private final SessionContext sessionContext;
    private final Runnable onSuccess;

    private JTextArea messageArea;

    public SubmitClaimDialog(Window parent, Item item, ClaimService claimService,
                             SessionContext sessionContext, Runnable onSuccess) {
        super(parent, "Submit Ownership Claim - Item #" + item.getId(), ModalityType.APPLICATION_MODAL);
        this.item = item;
        this.claimService = claimService;
        this.sessionContext = sessionContext;
        this.onSuccess = onSuccess;

        initComponents();
        setSize(520, 480);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UITheme.CARD_BG);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setBackground(UITheme.CARD_BG);

        JLabel title = new JLabel("Claim Item: " + item.getTitle());
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);

        JLabel sub = new JLabel("Provide verifiable identifying details to verify this item belongs to you.");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_MUTED);

        header.add(title, BorderLayout.NORTH);
        header.add(sub, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Body Form
        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBackground(UITheme.CARD_BG);

        JPanel infoCard = new JPanel(new GridLayout(0, 1, 4, 4));
        infoCard.setBackground(UITheme.BG_DARK);
        infoCard.setBorder(new CompoundBorder(new LineBorder(UITheme.BORDER, 1, true), new EmptyBorder(10, 14, 10, 14)));

        JLabel idLbl = new JLabel("Item ID: #" + item.getId() + " (" + (item.getCategory() != null ? item.getCategory().getName() : "Unassigned") + ")");
        idLbl.setFont(UITheme.FONT_BOLD);
        idLbl.setForeground(UITheme.TEXT_MAIN);

        User cur = sessionContext.getCurrentUser();
        JLabel userLbl = new JLabel("Claimant: " + (cur != null ? cur.getName() + " (" + cur.getEmail() + ")" : "Unknown"));
        userLbl.setFont(UITheme.FONT_BODY);
        userLbl.setForeground(UITheme.TEXT_MUTED);

        infoCard.add(idLbl);
        infoCard.add(userLbl);
        body.add(infoCard, BorderLayout.NORTH);

        JPanel msgPanel = new JPanel(new BorderLayout(0, 6));
        msgPanel.setBackground(UITheme.CARD_BG);

        JLabel proofLabel = UITheme.createFieldLabel("Proof of Ownership / Distinctive Features *");
        messageArea = UITheme.createTextArea(6, 25);
        messageArea.setToolTipText("Describe serial numbers, unique marks, lock screen wallpapers, contents, etc.");

        JScrollPane scroll = new JScrollPane(messageArea);
        scroll.setBorder(new LineBorder(UITheme.BORDER, 1, true));

        msgPanel.add(proofLabel, BorderLayout.NORTH);
        msgPanel.add(scroll, BorderLayout.CENTER);
        body.add(msgPanel, BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);

        // Footer Actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(UITheme.CARD_BG);

        JButton submitBtn = UITheme.createPrimaryButton("Submit Claim Request");
        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");

        submitBtn.addActionListener(e -> submitClaim());
        cancelBtn.addActionListener(e -> dispose());

        footer.add(cancelBtn);
        footer.add(submitBtn);
        root.add(footer, BorderLayout.SOUTH);
    }

    private void submitClaim() {
        String msg = messageArea.getText().trim();
        if (msg.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please provide details verifying your ownership.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User claimant = sessionContext.getCurrentUser();
        if (claimant == null) {
            JOptionPane.showMessageDialog(this,
                    "Active user session required.",
                    "Session Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Claim claim = new Claim();
        claim.setClaimant(claimant);
        claim.setItem(item);
        claim.setMessage(msg);
        claim.setStatus(ClaimStatus.PENDING);
        claim.setSubmittedAt(LocalDateTime.now());

        try {
            Claim saved = claimService.submitClaim(claim);
            JOptionPane.showMessageDialog(this,
                    "Your claim request (#" + saved.getId() + ") was submitted successfully!\n" +
                            "A campus administrator will review your claim and reach out.",
                    "Claim Submitted", JOptionPane.INFORMATION_MESSAGE);
            if (onSuccess != null) {
                onSuccess.run();
            }
            dispose();
        } catch (ValidationException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Claim Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to submit claim: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
