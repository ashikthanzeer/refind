package com.refind.ui;

import com.refind.exception.ValidationException;
import com.refind.model.User;
import com.refind.model.enums.Role;
import com.refind.service.AuthService;
import com.refind.service.UserService;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.List;

/**
 * Modern Dark Navy Blue Login and Registration Dialog for ReFind.
 */
public class LoginDialog extends JDialog {

    private final AuthService authService;
    private final UserService userService;
    private final SessionContext sessionContext;
    private final Runnable onLoginSuccess;

    private JTextField loginEmailField;
    private JPasswordField loginPasswordField;
    private JLabel loginErrorLabel;

    private JTextField regNameField;
    private JTextField regEmailField;
    private JPasswordField regPasswordField;
    private JPasswordField regConfirmPasswordField;
    private JLabel regErrorLabel;

    public LoginDialog(Window parent, AuthService authService, UserService userService,
                       SessionContext sessionContext, Runnable onLoginSuccess) {
        super(parent, "ReFind - Account Authentication", ModalityType.APPLICATION_MODAL);
        this.authService = authService;
        this.userService = userService;
        this.sessionContext = sessionContext;
        this.onLoginSuccess = onLoginSuccess;

        initComponents();
        setSize(520, 560);
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_DARK);
        setContentPane(root);

        // Header Branding
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.CARD_BG);
        header.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER, 1),
                new EmptyBorder(20, 24, 20, 24)
        ));

        JLabel logo = new JLabel("ReFind");
        logo.setFont(UITheme.FONT_BRAND);
        logo.setForeground(UITheme.PRIMARY);

        JLabel subtitle = new JLabel("Lost & Found Management Portal - Authentication");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_MUTED);

        header.add(logo, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Tabbed Pane for Login / Register
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(UITheme.BG_DARK);
        tabbedPane.setForeground(UITheme.TEXT_MUTED);

        tabbedPane.addTab("Sign In", buildSignInPanel());
        tabbedPane.addTab("Create Account", buildRegisterPanel());

        root.add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel buildSignInPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(20, 28, 20, 28));

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 8));
        form.setBackground(UITheme.BG_DARK);

        JLabel emailLbl = UITheme.createFieldLabel("Email Address");
        loginEmailField = UITheme.createTextField(25);
        loginEmailField.setText("admin@campus.edu");

        JLabel passLbl = UITheme.createFieldLabel("Password");
        loginPasswordField = UITheme.createPasswordField(25);
        loginPasswordField.setText("admin123");

        loginErrorLabel = new JLabel(" ");
        loginErrorLabel.setFont(UITheme.FONT_SMALL);
        loginErrorLabel.setForeground(UITheme.DANGER);

        form.add(emailLbl);
        form.add(loginEmailField);
        form.add(passLbl);
        form.add(loginPasswordField);
        form.add(loginErrorLabel);

        JButton signInBtn = UITheme.createPrimaryButton("Sign In");
        signInBtn.addActionListener(e -> performSignIn());
        loginPasswordField.addActionListener(e -> performSignIn());
        loginEmailField.addActionListener(e -> loginPasswordField.requestFocusInWindow());

        JPanel actions = new JPanel(new BorderLayout(0, 12));
        actions.setBackground(UITheme.BG_DARK);
        actions.add(signInBtn, BorderLayout.NORTH);

        // Quick Demo Shortcuts
        JPanel demoCard = new JPanel(new GridLayout(0, 1, 4, 6));
        demoCard.setBackground(UITheme.CARD_BG);
        demoCard.setBorder(new CompoundBorder(
                new LineBorder(UITheme.BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel demoTag = new JLabel("Quick Demo Sign-In (Click to Test):");
        demoTag.setFont(UITheme.FONT_BADGE);
        demoTag.setForeground(UITheme.TEXT_MUTED);
        demoCard.add(demoTag);

        JButton demoAdminBtn = UITheme.createSecondaryButton("👑 Sign in as Security Officer (Admin)");
        JButton demoUser1Btn = UITheme.createSecondaryButton("👤 Sign in as John Doe (User)");
        JButton demoUser2Btn = UITheme.createSecondaryButton("👤 Sign in as Alice Smith (User)");

        demoAdminBtn.addActionListener(e -> quickLogin("admin@campus.edu", "admin123", Role.ADMIN));
        demoUser1Btn.addActionListener(e -> quickLogin("john@campus.edu", "password123", Role.USER));
        demoUser2Btn.addActionListener(e -> quickLogin("alice@campus.edu", "password123", Role.USER));

        demoCard.add(demoAdminBtn);
        demoCard.add(demoUser1Btn);
        demoCard.add(demoUser2Btn);

        actions.add(demoCard, BorderLayout.CENTER);

        panel.add(form, BorderLayout.NORTH);
        panel.add(actions, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildRegisterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(18, 28, 18, 28));

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 6));
        form.setBackground(UITheme.BG_DARK);

        JLabel nameLbl = UITheme.createFieldLabel("Full Name *");
        regNameField = UITheme.createTextField(25);

        JLabel emailLbl = UITheme.createFieldLabel("Campus Email Address *");
        regEmailField = UITheme.createTextField(25);

        JLabel passLbl = UITheme.createFieldLabel("Password *");
        regPasswordField = UITheme.createPasswordField(25);

        JLabel confirmLbl = UITheme.createFieldLabel("Confirm Password *");
        regConfirmPasswordField = UITheme.createPasswordField(25);

        regErrorLabel = new JLabel(" ");
        regErrorLabel.setFont(UITheme.FONT_SMALL);
        regErrorLabel.setForeground(UITheme.DANGER);

        form.add(nameLbl);
        form.add(regNameField);
        form.add(emailLbl);
        form.add(regEmailField);
        form.add(passLbl);
        form.add(regPasswordField);
        form.add(confirmLbl);
        form.add(regConfirmPasswordField);
        form.add(regErrorLabel);

        JButton registerBtn = UITheme.createSuccessButton("Create Account & Sign In");
        registerBtn.addActionListener(e -> performRegister());

        panel.add(form, BorderLayout.CENTER);
        panel.add(registerBtn, BorderLayout.SOUTH);

        return panel;
    }

    private void performSignIn() {
        String email = loginEmailField.getText().trim();
        String password = new String(loginPasswordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            loginErrorLabel.setText("Please enter both email and password.");
            return;
        }

        loginErrorLabel.setText("Authenticating...");

        boolean ok = sessionContext.login(email, password, authService);
        if (ok) {
            dispose();
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            }
        } else {
            loginErrorLabel.setText("Invalid email or password. Please try again.");
        }
    }

    private void quickLogin(String email, String password, Role defaultRole) {
        boolean ok = sessionContext.login(email, password, authService);
        if (!ok) {
            // Find in available users
            List<User> list = sessionContext.getAvailableUsers();
            for (User u : list) {
                if (u.getEmail() != null && u.getEmail().equalsIgnoreCase(email)) {
                    sessionContext.setCurrentUser(u);
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                User demo = new User(email.split("@")[0], email, password, defaultRole, null);
                demo.setId(100L);
                sessionContext.setCurrentUser(demo);
                ok = true;
            }
        }

        dispose();
        if (onLoginSuccess != null) {
            onLoginSuccess.run();
        }
    }

    private void performRegister() {
        String name = regNameField.getText().trim();
        String email = regEmailField.getText().trim();
        String password = new String(regPasswordField.getPassword());
        String confirm = new String(regConfirmPasswordField.getPassword());

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            regErrorLabel.setText("All fields are required.");
            return;
        }

        if (!password.equals(confirm)) {
            regErrorLabel.setText("Passwords do not match.");
            return;
        }

        if (password.length() < 4) {
            regErrorLabel.setText("Password must be at least 4 characters.");
            return;
        }

        try {
            User registered = authService.register(name, email, password);
            sessionContext.setCurrentUser(registered);
            sessionContext.refreshUsers();
            JOptionPane.showMessageDialog(this,
                    "Account registered successfully! Welcome, " + registered.getName() + "!",
                    "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            }
        } catch (ValidationException ex) {
            regErrorLabel.setText(ex.getMessage());
        } catch (Exception ex) {
            regErrorLabel.setText("Registration failed: " + ex.getMessage());
        }
    }
}
