package com.refind.ui;

import com.refind.model.enums.ItemStatus;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public final class UITheme {

    private UITheme() {}

    // Color Palette
    public static final Color PRIMARY = new Color(79, 70, 229);      // Indigo 600
    public static final Color PRIMARY_DARK = new Color(67, 56, 202); // Indigo 700
    public static final Color PRIMARY_LIGHT = new Color(238, 242, 255); // Indigo 50

    public static final Color BG_LIGHT = new Color(248, 250, 252);   // Slate 50
    public static final Color CARD_BG = Color.WHITE;
    public static final Color TEXT_MAIN = new Color(15, 23, 42);      // Slate 900
    public static final Color TEXT_MUTED = new Color(100, 116, 139);  // Slate 500
    public static final Color BORDER = new Color(226, 232, 240);      // Slate 200

    public static final Color SUCCESS = new Color(16, 185, 129);     // Emerald 500
    public static final Color SUCCESS_LIGHT = new Color(209, 250, 229);
    public static final Color WARNING = new Color(245, 158, 11);     // Amber 500
    public static final Color WARNING_LIGHT = new Color(254, 243, 199);
    public static final Color DANGER = new Color(239, 68, 68);        // Red 500
    public static final Color DANGER_LIGHT = new Color(254, 226, 226);
    public static final Color INFO = new Color(14, 165, 233);         // Sky 500
    public static final Color INFO_LIGHT = new Color(224, 242, 254);

    // Typography
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(Color.WHITE);
        btn.setForeground(TEXT_MAIN);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(7, 14, 7, 14)));
        return btn;
    }

    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(DANGER);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    public static JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(FONT_BODY);
        field.setForeground(TEXT_MAIN);
        field.setBackground(Color.WHITE);
        field.setCaretColor(PRIMARY);
        field.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(6, 10, 6, 10)));
        return field;
    }

    public static Border createCardBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(16, 16, 16, 16)
        );
    }

    public static JLabel createStatusBadge(ItemStatus status) {
        String text = status != null ? status.name() : "UNKNOWN";
        JLabel badge = new JLabel(text, SwingConstants.CENTER);
        badge.setFont(FONT_SMALL);
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(3, 8, 3, 8));

        if (status == null) {
            badge.setBackground(BG_LIGHT);
            badge.setForeground(TEXT_MUTED);
            return badge;
        }

        switch (status) {
            case OPEN:
                badge.setBackground(INFO_LIGHT);
                badge.setForeground(INFO);
                break;
            case LOST:
                badge.setBackground(WARNING_LIGHT);
                badge.setForeground(WARNING);
                break;
            case CLAIMED:
                badge.setBackground(PRIMARY_LIGHT);
                badge.setForeground(PRIMARY);
                break;
            case RETURNED:
                badge.setBackground(SUCCESS_LIGHT);
                badge.setForeground(SUCCESS);
                break;
            case CLOSED:
            default:
                badge.setBackground(BG_LIGHT);
                badge.setForeground(TEXT_MUTED);
                break;
        }
        return badge;
    }
}
