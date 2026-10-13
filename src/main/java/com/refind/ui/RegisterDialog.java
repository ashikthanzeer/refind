package com.refind.ui;

import com.refind.exception.ValidationException;
import com.refind.model.User;
import com.refind.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RegisterDialog extends JDialog {

    private final AuthService authService;
    private final SessionContext sessionContext;
    private final Runnable onRegisterSuccess;

    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JLabel errorLabel;

    public RegisterDialog(Window parent, AuthService authService, SessionContext sessionContext, Runnable onRegisterSuccess) {
        super(parent, "Register Account - ReFind", ModalityType.APPLICATION_MODAL);
        this.authService = authService;
        this.sessionContext = sessionContext;
        this.onRegisterSuccess = onRegisterSuccess;

        initComponents();
        setSize(460, 480);
        setMinimumSize(new Dimension(420, 420));
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UITheme.CARD_BG);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));
        setContentPane(root);

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(UITheme.CARD_BG);

        JLabel titleLabel = new JLabel("Create a New Account");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Join ReFind to report lost items and submit ownership claims");
        subtitleLabel.setFont(UITheme.FONT_BODY);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        root.add(headerPanel, BorderLayout.NORTH);

        // Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(3, 0, 3, 0);

        int row = 0;

        gbc.gridy = row++;
        JLabel nameLbl = new JLabel("Full Name");
        nameLbl.setFont(UITheme.FONT_BOLD);
        nameLbl.setForeground(UITheme.TEXT_MAIN);
        formPanel.add(nameLbl, gbc);

        gbc.gridy = row++;
        nameField = UITheme.createTextField(20);
        formPanel.add(nameField, gbc);

        gbc.gridy = row++;
        JLabel emailLbl = new JLabel("Campus Email Address");
        emailLbl.setFont(UITheme.FONT_BOLD);
        emailLbl.setForeground(UITheme.TEXT_MAIN);
        formPanel.add(emailLbl, gbc);

        gbc.gridy = row++;
        emailField = UITheme.createTextField(20);
        formPanel.add(emailField, gbc);

        gbc.gridy = row++;
        JLabel passLbl = new JLabel("Password (min 6 characters)");
        passLbl.setFont(UITheme.FONT_BOLD);
        passLbl.setForeground(UITheme.TEXT_MAIN);
        formPanel.add(passLbl, gbc);

        gbc.gridy = row++;
        passwordField = new JPasswordField(20);
        passwordField.setFont(UITheme.FONT_BODY);
        passwordField.setPreferredSize(new Dimension(passwordField.getPreferredSize().width, 32));
        formPanel.add(passwordField, gbc);

        gbc.gridy = row++;
        JLabel confirmPassLbl = new JLabel("Confirm Password");
        confirmPassLbl.setFont(UITheme.FONT_BOLD);
        confirmPassLbl.setForeground(UITheme.TEXT_MAIN);
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

        // Buttons
        JPanel buttonPanel = new JPanel(new BorderLayout(8, 0));
        buttonPanel.setBackground(UITheme.CARD_BG);

        JButton loginLinkBtn = new JButton("Already registered? Sign in");
        loginLinkBtn.setFont(UITheme.FONT_BODY);
        loginLinkBtn.setForeground(UITheme.PRIMARY);
        loginLinkBtn.setBorderPainted(false);
        loginLinkBtn.setContentAreaFilled(false);
        loginLinkBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginLinkBtn.addActionListener(e -> {
            dispose();
            LoginDialog logDialog = new LoginDialog(getOwner(), authService, sessionContext, onRegisterSuccess);
            logDialog.setVisible(true);
        });

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setBackground(UITheme.CARD_BG);

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton registerBtn = UITheme.createPrimaryButton("Create Account");
        registerBtn.addActionListener(e -> performRegister());

        getRootPane().setDefaultButton(registerBtn);

        actionButtons.add(cancelBtn);
        actionButtons.add(registerBtn);

        buttonPanel.add(loginLinkBtn, BorderLayout.WEST);
        buttonPanel.add(actionButtons, BorderLayout.EAST);
        root.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void performRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        try {
            User newUser = authService.register(name, email, password, confirmPassword);
            sessionContext.setCurrentUser(newUser);

            JOptionPane.showMessageDialog(this,
                    "Account successfully created! Welcome, " + newUser.getName() + ".",
                    "Registration Successful", JOptionPane.INFORMATION_MESSAGE);

            if (onRegisterSuccess != null) {
                onRegisterSuccess.run();
            }
            dispose();
        } catch (ValidationException ve) {
            errorLabel.setText(ve.getMessage());
        } catch (Exception ex) {
            errorLabel.setText("Registration error: " + ex.getMessage());
        }
    }
}
