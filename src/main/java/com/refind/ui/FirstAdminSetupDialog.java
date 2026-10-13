package com.refind.ui;

import com.refind.exception.ValidationException;
import com.refind.model.User;
import com.refind.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class FirstAdminSetupDialog extends JDialog {

    private final AuthService authService;
    private final Runnable onSetupComplete;

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JLabel errorLabel;

    public FirstAdminSetupDialog(Window parent, AuthService authService, Runnable onSetupComplete) {
        super(parent, "Create First Administrator - ReFind", ModalityType.APPLICATION_MODAL);
        this.authService = authService;
        this.onSetupComplete = onSetupComplete;

        initComponents();
        setSize(480, 500);
        setMinimumSize(new Dimension(420, 440));
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(UITheme.CARD_BG);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));
        setContentPane(root);

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.CARD_BG);

        JLabel titleLabel = new JLabel("Create the first administrator");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("This setup is only available when no administrator exists.");
        subtitleLabel.setFont(UITheme.FONT_BODY);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        root.add(headerPanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(4, 0, 4, 0);

        int row = 0;

        gbc.gridy = row++;
        JLabel nameLbl = new JLabel("Full Name");
        nameLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(nameLbl, gbc);

        gbc.gridy = row++;
        nameField = UITheme.createTextField(20);
        formPanel.add(nameField, gbc);

        gbc.gridy = row++;
        JLabel emailLbl = new JLabel("Email Address");
        emailLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(emailLbl, gbc);

        gbc.gridy = row++;
        emailField = UITheme.createTextField(20);
        formPanel.add(emailField, gbc);

        gbc.gridy = row++;
        JLabel passLbl = new JLabel("Password (min 6 characters)");
        passLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(passLbl, gbc);

        gbc.gridy = row++;
        passwordField = new JPasswordField(20);
        passwordField.setFont(UITheme.FONT_BODY);
        passwordField.setPreferredSize(new Dimension(passwordField.getPreferredSize().width, 32));
        formPanel.add(passwordField, gbc);

        gbc.gridy = row++;
        JLabel confirmPassLbl = new JLabel("Confirm Password");
        confirmPassLbl.setFont(UITheme.FONT_BOLD);
        formPanel.add(confirmPassLbl, gbc);

        gbc.gridy = row++;
        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setFont(UITheme.FONT_BODY);
        confirmPasswordField.setPreferredSize(new Dimension(confirmPasswordField.getPreferredSize().width, 32));
        formPanel.add(confirmPasswordField, gbc);

        gbc.gridy = row++;
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER);
        formPanel.add(errorLabel, gbc);

        root.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new BorderLayout(8, 0));
        buttonPanel.setBackground(UITheme.CARD_BG);

        JButton backBtn = new JButton("Back to Sign In");
        backBtn.setFont(UITheme.FONT_BODY);
        backBtn.setForeground(UITheme.PRIMARY);
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> {
            dispose();
            if (onSetupComplete != null) {
                onSetupComplete.run();
            }
        });

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setBackground(UITheme.CARD_BG);

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton createBtn = UITheme.createPrimaryButton("Create Administrator");
        createBtn.addActionListener(e -> performSetup());

        getRootPane().setDefaultButton(createBtn);

        actionButtons.add(cancelBtn);
        actionButtons.add(createBtn);

        buttonPanel.add(backBtn, BorderLayout.WEST);
        buttonPanel.add(actionButtons, BorderLayout.EAST);
        root.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void performSetup() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        try {
            User created = authService.bootstrapFirstAdmin(name, email, password, confirmPassword);
            JOptionPane.showMessageDialog(this,
                    "Administrator account created for " + created.getName() + ". Please sign in with your new administrator credentials.",
                    "Administrator Ready", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            if (onSetupComplete != null) {
                onSetupComplete.run();
            }
        } catch (ValidationException ve) {
            errorLabel.setText(ve.getMessage());
        } catch (Exception ex) {
            errorLabel.setText("Administrator setup error: " + ex.getMessage());
        }
    }
}
