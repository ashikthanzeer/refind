package com.refind.ui;

import com.refind.model.User;
import com.refind.service.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

public class LoginDialog extends JDialog {

    private final AuthService authService;
    private final SessionContext sessionContext;
    private final Runnable onLoginSuccess;

    private JTextField emailField;
    private JPasswordField passwordField;
    private JLabel errorLabel;

    public LoginDialog(Window parent, AuthService authService, SessionContext sessionContext, Runnable onLoginSuccess) {
        super(parent, "Sign In - ReFind", ModalityType.APPLICATION_MODAL);
        this.authService = authService;
        this.sessionContext = sessionContext;
        this.onLoginSuccess = onLoginSuccess;

        initComponents();
        setSize(420, 360);
        setMinimumSize(new Dimension(380, 320));
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

        JLabel titleLabel = new JLabel("Welcome to ReFind");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Sign in with your campus account");
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
        gbc.insets = new Insets(4, 0, 4, 0);

        int row = 0;

        gbc.gridy = row++;
        JLabel emailLbl = new JLabel("Email Address");
        emailLbl.setFont(UITheme.FONT_BOLD);
        emailLbl.setForeground(UITheme.TEXT_MAIN);
        formPanel.add(emailLbl, gbc);

        gbc.gridy = row++;
        emailField = UITheme.createTextField(20);
        formPanel.add(emailField, gbc);

        gbc.gridy = row++;
        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(UITheme.FONT_BOLD);
        passLbl.setForeground(UITheme.TEXT_MAIN);
        formPanel.add(passLbl, gbc);

        gbc.gridy = row++;
        passwordField = new JPasswordField(20);
        passwordField.setFont(UITheme.FONT_BODY);
        passwordField.setPreferredSize(new Dimension(passwordField.getPreferredSize().width, 32));
        formPanel.add(passwordField, gbc);

        gbc.gridy = row++;
        errorLabel = new JLabel(" ");
        errorLabel.setFont(UITheme.FONT_SMALL);
        errorLabel.setForeground(UITheme.DANGER);
        formPanel.add(errorLabel, gbc);

        root.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new BorderLayout(8, 0));
        buttonPanel.setBackground(UITheme.CARD_BG);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActions.setBackground(UITheme.CARD_BG);

        JButton registerLinkBtn = new JButton("Register");
        registerLinkBtn.setFont(UITheme.FONT_BODY);
        registerLinkBtn.setForeground(UITheme.PRIMARY);
        registerLinkBtn.setBorderPainted(false);
        registerLinkBtn.setContentAreaFilled(false);
        registerLinkBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerLinkBtn.addActionListener(e -> openRegisterDialog());
        leftActions.add(registerLinkBtn);

        if (!authService.hasAdministrator()) {
            JButton firstAdminBtn = new JButton("Create First Administrator");
            firstAdminBtn.setFont(UITheme.FONT_BODY);
            firstAdminBtn.setForeground(UITheme.DANGER);
            firstAdminBtn.setBorderPainted(false);
            firstAdminBtn.setContentAreaFilled(false);
            firstAdminBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            firstAdminBtn.addActionListener(e -> openFirstAdminSetup());
            leftActions.add(firstAdminBtn);
        }

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionButtons.setBackground(UITheme.CARD_BG);

        JButton cancelBtn = UITheme.createSecondaryButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JButton loginBtn = UITheme.createPrimaryButton("Sign In");
        loginBtn.addActionListener(e -> performLogin());

        // Enter key submits form
        getRootPane().setDefaultButton(loginBtn);

        actionButtons.add(cancelBtn);
        actionButtons.add(loginBtn);

        buttonPanel.add(leftActions, BorderLayout.WEST);
        buttonPanel.add(actionButtons, BorderLayout.EAST);
        root.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void performLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both email and password.");
            return;
        }

        Optional<User> userOpt = authService.login(email, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            sessionContext.setCurrentUser(user);
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            }
            dispose();
        } else {
            errorLabel.setText("Invalid email address or password.");
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }

    private void openRegisterDialog() {
        dispose();
        RegisterDialog regDialog = new RegisterDialog(getOwner(), authService, sessionContext, onLoginSuccess);
        regDialog.setVisible(true);
    }

    private void openFirstAdminSetup() {
        dispose();
        FirstAdminSetupDialog setupDialog = new FirstAdminSetupDialog(getOwner(), authService, () -> {
            LoginDialog loginDialog = new LoginDialog(getOwner(), authService, sessionContext, onLoginSuccess);
            loginDialog.setVisible(true);
        });
        setupDialog.setVisible(true);
    }
}
