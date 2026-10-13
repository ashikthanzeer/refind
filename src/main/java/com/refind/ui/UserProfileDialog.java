package com.refind.ui;

import com.refind.exception.ValidationException;
import com.refind.model.User;
import com.refind.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class UserProfileDialog extends JDialog {

    private final AuthService authService;
    private final SessionContext sessionContext;
    private final Runnable onProfileUpdated;

    private JTextField nameField;
    private JPasswordField currentPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;
    private JLabel errorLabel;

    public UserProfileDialog(Window parent, AuthService authService, SessionContext sessionContext, Runnable onProfileUpdated) {
        super(parent, "My Profile - ReFind", ModalityType.APPLICATION_MODAL);
        this.authService = authService;
        this.sessionContext = sessionContext;
        this.onProfileUpdated = onProfileUpdated;

        initComponents();
        setSize(480, 520);
        setMinimumSize(new Dimension(420, 460));
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        User user = sessionContext.getCurrentUser();
        if (user == null) {
            dispose();
            return;
        }

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UITheme.CARD_BG);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));
        setContentPane(root);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UITheme.CARD_BG);

        JLabel titleLabel = new JLabel("User Account Profile");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);

        JLabel roleBadge = UITheme.createRoleBadge(user.getRole());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(roleBadge, BorderLayout.EAST);
        root.add(headerPanel, BorderLayout.NORTH);

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 0, 4, 0);

        int row = 0;

        gbc.gridy = row++;
        JLabel emailLbl = new JLabel("Email (Account Identifier)");
        emailLbl.setFont(UITheme.FONT_BOLD);
        emailLbl.setForeground(UITheme.TEXT_MUTED);
        formPanel.add(emailLbl, gbc);

        gbc.gridy = row++;
        JTextField emailField = UITheme.createTextField(20);
        emailField.setText(user.getEmail());
        emailField.setEditable(false);
        emailField.setBackground(new Color(245, 245, 245));
        formPanel.add(emailField, gbc);

        gbc.gridy = row++;
        JLabel memberSinceLbl = new JLabel("Member Since");
        memberSinceLbl.setFont(UITheme.FONT_BOLD);
        memberSinceLbl.setForeground(UITheme.TEXT_MUTED);
        formPanel.add(memberSinceLbl, gbc);

        gbc.gridy = row++;
        String createdStr = user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "Active User";
        JTextField createdField = UITheme.createTextField(20);
        createdField.setText(createdStr);
        createdField.setEditable(false);
        createdField.setBackground(new Color(245, 245, 245));
        formPanel.add(createdField, gbc);

        gbc.gridy = row++;
        JLabel nameLbl = new JLabel("Full Name");
        nameLbl.setFont(UITheme.FONT_BOLD);
        nameLbl.setForeground(UITheme.TEXT_MAIN);
        formPanel.add(nameLbl, gbc);

        gbc.gridy = row++;
        nameField = UITheme.createTextField(20);
        nameField.setText(user.getName());
        formPanel.add(nameField, gbc);

        gbc.gridy = row++;
        formPanel.add(Box.createVerticalStrut(8), gbc);

        gbc.gridy = row++;
        JLabel passSectionLbl = new JLabel("Change Password (Leave blank to keep current)");
        passSectionLbl.setFont(UITheme.FONT_BOLD);
        passSectionLbl.setForeground(UITheme.PRIMARY);
        formPanel.add(passSectionLbl, gbc);

        gbc.gridy = row++;
        JLabel currPassLbl = new JLabel("Current Password");
        currPassLbl.setFont(UITheme.FONT_SMALL);
        formPanel.add(currPassLbl, gbc);

        gbc.gridy = row++;
        currentPasswordField = new JPasswordField(20);
        currentPasswordField.setPreferredSize(new Dimension(currentPasswordField.getPreferredSize().width, 30));
        formPanel.add(currentPasswordField, gbc);

        gbc.gridy = row++;
        JLabel newPassLbl = new JLabel("New Password (min 6 characters)");
        newPassLbl.setFont(UITheme.FONT_SMALL);
        formPanel.add(newPassLbl, gbc);

        gbc.gridy = row++;
        newPasswordField = new JPasswordField(20);
        newPasswordField.setPreferredSize(new Dimension(newPasswordField.getPreferredSize().width, 30));
        formPanel.add(newPasswordField, gbc);

        gbc.gridy = row++;
        JLabel confPassLbl = new JLabel("Confirm New Password");
        confPassLbl.setFont(UITheme.FONT_SMALL);
        formPanel.add(confPassLbl, gbc);

        gbc.gridy = row++;
        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setPreferredSize(new Dimension(confirmPasswordField.getPreferredSize().width, 30));
        formPanel.add(confirmPasswordField, gbc);

        gbc.gridy = row++;
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER);
        formPanel.add(errorLabel, gbc);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        root.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setBackground(UITheme.CARD_BG);

        JButton closeBtn = UITheme.createSecondaryButton("Close");
        closeBtn.addActionListener(e -> dispose());

        JButton saveBtn = UITheme.createPrimaryButton("Save Profile");
        saveBtn.addActionListener(e -> saveProfile());

        buttonPanel.add(closeBtn);
        buttonPanel.add(saveBtn);
        root.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void saveProfile() {
        User user = sessionContext.getCurrentUser();
        if (user == null) {
            dispose();
            return;
        }

        String newName = nameField.getText().trim();
        String currentPass = new String(currentPasswordField.getPassword());
        String newPass = new String(newPasswordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());

        if (!newPass.isEmpty() && !newPass.equals(confirmPass)) {
            errorLabel.setText("New password and confirmation do not match.");
            return;
        }

        try {
            User updated = authService.updateProfile(user.getId(), newName, currentPass, newPass.isEmpty() ? null : newPass);
            sessionContext.setCurrentUser(updated);

            JOptionPane.showMessageDialog(this,
                    "Profile successfully updated.",
                    "Profile Updated", JOptionPane.INFORMATION_MESSAGE);

            if (onProfileUpdated != null) {
                onProfileUpdated.run();
            }
            dispose();
        } catch (ValidationException ve) {
            errorLabel.setText(ve.getMessage());
        } catch (Exception ex) {
            errorLabel.setText("Update error: " + ex.getMessage());
        }
    }
}
