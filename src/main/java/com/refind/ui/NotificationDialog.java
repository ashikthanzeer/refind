package com.refind.ui;

import com.refind.model.Notification;
import com.refind.model.User;
import com.refind.service.NotificationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationDialog extends JDialog {

    private final NotificationService notificationService;
    private final SessionContext sessionContext;
    private final Runnable onUpdated;

    private JPanel listPanel;

    public NotificationDialog(Window parent, NotificationService notificationService, SessionContext sessionContext, Runnable onUpdated) {
        super(parent, "Notifications - ReFind", ModalityType.APPLICATION_MODAL);
        this.notificationService = notificationService;
        this.sessionContext = sessionContext;
        this.onUpdated = onUpdated;

        initComponents();
        setSize(520, 480);
        setMinimumSize(new Dimension(450, 380));
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

        JLabel titleLabel = new JLabel("Your Notifications");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_MAIN);

        JButton markAllBtn = UITheme.createSecondaryButton("Mark All as Read");
        markAllBtn.addActionListener(e -> markAllAsRead());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(markAllBtn, BorderLayout.EAST);
        root.add(headerPanel, BorderLayout.NORTH);

        // Scrollable notification list
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(UITheme.CARD_BG);

        loadNotifications();

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_LIGHT));
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        root.add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(UITheme.CARD_BG);
        JButton closeBtn = UITheme.createPrimaryButton("Close");
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);
        root.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadNotifications() {
        listPanel.removeAll();
        User user = sessionContext.getCurrentUser();
        if (user == null || user.getId() == null) {
            JLabel emptyLabel = new JLabel("Please sign in to view your notifications.");
            emptyLabel.setFont(UITheme.FONT_BODY);
            emptyLabel.setForeground(UITheme.TEXT_MUTED);
            emptyLabel.setBorder(new EmptyBorder(30, 20, 30, 20));
            listPanel.add(emptyLabel);
            listPanel.revalidate();
            listPanel.repaint();
            return;
        }

        List<Notification> notifications = notificationService.getNotificationsByUser(user.getId());
        if (notifications.isEmpty()) {
            JLabel emptyLabel = new JLabel("No notifications yet. Updates on your reports and claims will appear here.");
            emptyLabel.setFont(UITheme.FONT_BODY);
            emptyLabel.setForeground(UITheme.TEXT_MUTED);
            emptyLabel.setBorder(new EmptyBorder(30, 20, 30, 20));
            listPanel.add(emptyLabel);
        } else {
            for (Notification n : notifications) {
                listPanel.add(createNotificationCard(n));
                listPanel.add(Box.createVerticalStrut(8));
            }
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createNotificationCard(Notification n) {
        JPanel card = new JPanel(new BorderLayout(10, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(n.isReadFlag() ? UITheme.BORDER_LIGHT : UITheme.PRIMARY, n.isReadFlag() ? 1 : 2),
                new EmptyBorder(10, 12, 10, 12)
        ));
        card.setBackground(n.isReadFlag() ? Color.WHITE : new Color(245, 249, 255));

        JLabel textLabel = new JLabel("<html><body style='width: 300px;'>" + n.getText() + "</body></html>");
        textLabel.setFont(n.isReadFlag() ? UITheme.FONT_BODY : UITheme.FONT_BOLD);
        textLabel.setForeground(UITheme.TEXT_MAIN);

        String timeStr = n.getCreatedAt() != null
                ? n.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
                : "";
        JLabel timeLabel = new JLabel(timeStr);
        timeLabel.setFont(UITheme.FONT_SMALL);
        timeLabel.setForeground(UITheme.TEXT_MUTED);

        card.add(textLabel, BorderLayout.CENTER);
        card.add(timeLabel, BorderLayout.NORTH);

        if (!n.isReadFlag()) {
            JButton readBtn = new JButton("Mark Read");
            readBtn.setFont(UITheme.FONT_SMALL);
            readBtn.addActionListener(e -> {
                notificationService.markAsRead(n.getId(), sessionContext.requireAuthenticated());
                loadNotifications();
                if (onUpdated != null) onUpdated.run();
            });
            card.add(readBtn, BorderLayout.EAST);
        }

        return card;
    }

    private void markAllAsRead() {
        User user = sessionContext.getCurrentUser();
        if (user != null && user.getId() != null) {
            List<Notification> unread = notificationService.getUnreadNotifications(user.getId());
            for (Notification n : unread) {
                notificationService.markAsRead(n.getId(), user);
            }
            loadNotifications();
            if (onUpdated != null) onUpdated.run();
        }
    }
}
