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
    public static final Color PRIMARY = new Color(79, 70, 229);       // Indigo 600
    public static final Color PRIMARY_DARK = new Color(67, 56, 202);  // Indigo 700
    public static final Color PRIMARY_LIGHT = new Color(238, 242, 255); // Indigo 50

    public static final Color BG_LIGHT = new Color(248, 250, 252);    // Slate 50
    public static final Color CARD_BG = Color.WHITE;
    public static final Color TEXT_MAIN = new Color(15, 23, 42);       // Slate 900
    public static final Color TEXT_MUTED = new Color(100, 116, 139);   // Slate 500
    public static final Color BORDER = new Color(226, 232, 240);       // Slate 200
    public static final Color ROW_ALT = new Color(241, 245, 249);      // Slate 100 – alternating table rows

    public static final Color SUCCESS = new Color(16, 185, 129);      // Emerald 500
    public static final Color SUCCESS_LIGHT = new Color(209, 250, 229);
    public static final Color WARNING = new Color(245, 158, 11);      // Amber 500
    public static final Color WARNING_LIGHT = new Color(254, 243, 199);
    public static final Color DANGER = new Color(239, 68, 68);         // Red 500
    public static final Color DANGER_LIGHT = new Color(254, 226, 226);
    public static final Color INFO = new Color(14, 165, 233);          // Sky 500
    public static final Color INFO_LIGHT = new Color(224, 242, 254);

    // Typography
    public static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  18);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);

    // ─── Look & Feel ─────────────────────────────────────────────────────────

    /**
     * Configures Nimbus (cross-platform) Look & Feel with app color overrides.
     * Must be called ONCE before any Swing component is created, on the EDT.
     * Nimbus respects setBackground/setForeground reliably on all platforms,
     * unlike the Linux GTK L&F which causes white-on-white rendering issues.
     */
    public static void setupLookAndFeel() {
        // 1. Install Nimbus (falls back to cross-platform Metal if unavailable)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e2) { /* ignore – use whatever default is available */ }
        }

        // 2. Override global UI defaults so every widget inherits correct colors
        UIManager.put("control",                   BG_LIGHT);
        UIManager.put("Panel.background",          BG_LIGHT);
        UIManager.put("OptionPane.background",     CARD_BG);
        UIManager.put("OptionPane.messageForeground", TEXT_MAIN);
        UIManager.put("Label.foreground",          TEXT_MAIN);
        UIManager.put("Label.font",                FONT_BODY);
        UIManager.put("TextField.background",      Color.WHITE);
        UIManager.put("TextField.foreground",      TEXT_MAIN);
        UIManager.put("TextField.caretForeground", PRIMARY);
        UIManager.put("TextArea.background",       Color.WHITE);
        UIManager.put("TextArea.foreground",       TEXT_MAIN);
        UIManager.put("TextArea.caretForeground",  PRIMARY);
        UIManager.put("ComboBox.background",       Color.WHITE);
        UIManager.put("ComboBox.foreground",       TEXT_MAIN);
        UIManager.put("Table.background",          Color.WHITE);
        UIManager.put("Table.foreground",          TEXT_MAIN);
        UIManager.put("Table.selectionBackground", PRIMARY_LIGHT);
        UIManager.put("Table.selectionForeground", TEXT_MAIN);
        UIManager.put("Table.gridColor",           BORDER);
        UIManager.put("TableHeader.background",    BG_LIGHT);
        UIManager.put("TableHeader.foreground",    TEXT_MAIN);
        UIManager.put("TabbedPane.background",     BG_LIGHT);
        UIManager.put("TabbedPane.foreground",     TEXT_MAIN);
        UIManager.put("TabbedPane.selected",       CARD_BG);
        UIManager.put("TabbedPane.tabAreaBackground", BG_LIGHT);
        UIManager.put("ScrollPane.background",     CARD_BG);
        UIManager.put("Viewport.background",       CARD_BG);
        UIManager.put("Button.font",       FONT_BOLD);
        UIManager.put("ComboBox.font",     FONT_BODY);
        UIManager.put("TextField.font",    FONT_BODY);
        UIManager.put("TextArea.font",     FONT_BODY);
        UIManager.put("Table.font",        FONT_BODY);
        UIManager.put("TableHeader.font",  FONT_BOLD);
        UIManager.put("TabbedPane.font",   FONT_BOLD);
    }

    // ─── Button Factories ─────────────────────────────────────────────────────

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

    // ─── Input Field Factories ────────────────────────────────────────────────

    public static JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(FONT_BODY);
        field.setForeground(TEXT_MAIN);
        field.setBackground(Color.WHITE);
        field.setOpaque(true);
        field.setCaretColor(PRIMARY);
        field.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(6, 10, 6, 10)));
        return field;
    }

    /** Creates a styled editable JTextArea (e.g. for forms). */
    public static JTextArea createTextArea(int rows, int columns) {
        JTextArea area = new JTextArea(rows, columns);
        area.setFont(FONT_BODY);
        area.setForeground(TEXT_MAIN);
        area.setBackground(Color.WHITE);
        area.setOpaque(true);
        area.setCaretColor(PRIMARY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(6, 8, 6, 8));
        return area;
    }

    /** Creates a read-only styled JTextArea (e.g. for detail views). */
    public static JTextArea createReadOnlyTextArea(String text) {
        JTextArea area = new JTextArea(text);
        area.setFont(FONT_BODY);
        area.setForeground(TEXT_MAIN);
        area.setBackground(BG_LIGHT);
        area.setOpaque(true);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(8, 8, 8, 8));
        return area;
    }

    // ─── Label Factories ──────────────────────────────────────────────────────

    /** Plain body-text label. */
    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY);
        label.setForeground(TEXT_MAIN);
        return label;
    }

    /** Bold form-field label. */
    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BOLD);
        label.setForeground(TEXT_MAIN);
        return label;
    }

    // ─── Table Styling ────────────────────────────────────────────────────────

    /**
     * Applies consistent font, color, header, and alternating-row styling to a JTable.
     * Call this after creating any JTable instead of setting properties individually.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setForeground(TEXT_MAIN);
        table.setBackground(Color.WHITE);
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(TEXT_MAIN);
        table.setShowGrid(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));

        // Header
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setBackground(BG_LIGHT);
        table.getTableHeader().setForeground(TEXT_MAIN);
        table.getTableHeader().setOpaque(true);
        table.getTableHeader().setBorder(new LineBorder(BORDER, 1));
        table.getTableHeader().setReorderingAllowed(false);

        // Alternating-row default renderer with guaranteed foreground
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    setBackground(PRIMARY_LIGHT);
                    setForeground(TEXT_MAIN);
                } else {
                    setBackground(row % 2 == 0 ? Color.WHITE : ROW_ALT);
                    setForeground(TEXT_MAIN);
                }
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
    }

    // ─── Borders ─────────────────────────────────────────────────────────────

    public static Border createCardBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(16, 16, 16, 16)
        );
    }

    // ─── Status Badge ─────────────────────────────────────────────────────────

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
