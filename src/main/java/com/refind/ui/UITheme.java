package com.refind.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.refind.model.enums.ItemStatus;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern Dark Navy Blue Theme for ReFind.
 * Features rounded controls, high-contrast typography, and zero color inconsistencies.
 */
public final class UITheme {

    private UITheme() {}

    // ─── Dark Navy Color Palette ─────────────────────────────────────────────
    public static final Color BG_DARK        = new Color(11, 17, 32);     // Deep Navy #0B1120
    public static final Color BG_LIGHT       = BG_DARK;                   // Dark navy alias for backward compatibility
    public static final Color BG_SURFACE     = new Color(15, 23, 42);     // Slate 900 #0F172A
    public static final Color CARD_BG        = new Color(24, 34, 53);     // Rich Navy Card #182235
    public static final Color CARD_HOVER     = new Color(30, 43, 67);     // Elevated Navy #1E2B43
    public static final Color INPUT_BG       = new Color(15, 23, 42);     // Deep Input Field #0F172A
    public static final Color BORDER         = new Color(51, 65, 85);     // Slate 700 #334155
    public static final Color BORDER_FOCUS   = new Color(96, 165, 250);   // Blue 400 #60A5FA

    // Text & Foregrounds (High Contrast - Slate 50 & Slate 400)
    public static final Color TEXT_MAIN      = new Color(248, 250, 252);  // Slate 50 (Crisp Off-White)
    public static final Color TEXT_MUTED     = new Color(148, 163, 184);  // Slate 400 (Readable Light Slate)
    public static final Color TEXT_DIMMED    = new Color(100, 116, 139);  // Slate 500

    // Primary Accents
    public static final Color PRIMARY        = new Color(59, 130, 246);   // Blue 500 #3B82F6
    public static final Color PRIMARY_HOVER  = new Color(37, 99, 235);   // Blue 600 #2563EB
    public static final Color PRIMARY_DARK   = new Color(29, 78, 216);    // Blue 700 #1D4ED8
    public static final Color PRIMARY_LIGHT  = new Color(30, 58, 110);    // Deep Blue Selection #1E3A6E

    // Table Row Alternation
    public static final Color ROW_NORMAL     = new Color(24, 34, 53);     // #182235
    public static final Color ROW_ALT        = new Color(18, 26, 42);     // #121A2A

    // Semantic Status Colors & Badges
    public static final Color SUCCESS        = new Color(52, 211, 153);   // Emerald 400
    public static final Color SUCCESS_BG     = new Color(6, 78, 59);      // Dark Emerald #064E3B
    public static final Color WARNING        = new Color(251, 191, 36);   // Amber 400
    public static final Color WARNING_BG     = new Color(120, 53, 15);    // Dark Amber #78350F
    public static final Color DANGER         = new Color(248, 113, 113);  // Red 400
    public static final Color DANGER_HOVER   = new Color(239, 68, 68);    // Red 500
    public static final Color DANGER_BG      = new Color(127, 29, 29);    // Dark Red #7F1D1D
    public static final Color INFO           = new Color(56, 189, 248);   // Sky 400
    public static final Color INFO_BG        = new Color(12, 74, 110);    // Dark Sky #0C4A6E
    public static final Color PURPLE         = new Color(192, 132, 252);  // Purple 400
    public static final Color PURPLE_BG      = new Color(88, 28, 135);    // Dark Purple

    // Typography
    public static final Font FONT_BRAND   = new Font("Segoe UI", Font.BOLD,  20);
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  17);
    public static final Font FONT_HEADER  = new Font("Segoe UI", Font.BOLD,  14);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BADGE   = new Font("Segoe UI", Font.BOLD,  11);

    // ─── Look & Feel Setup ───────────────────────────────────────────────────

    public static void setupLookAndFeel() {
        try {
            FlatDarkLaf.setup();
        } catch (Exception ex) {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Apply dark navy UI defaults to guarantee zero color inconsistencies
        UIManager.put("Button.arc", 16);
        UIManager.put("Component.arc", 12);
        UIManager.put("ProgressBar.arc", 12);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.thumbArc", 8);

        UIManager.put("control",                      BG_DARK);
        UIManager.put("Panel.background",             BG_DARK);
        UIManager.put("OptionPane.background",        CARD_BG);
        UIManager.put("OptionPane.messageForeground", TEXT_MAIN);
        UIManager.put("Label.foreground",             TEXT_MAIN);
        UIManager.put("Label.font",                   FONT_BODY);

        UIManager.put("TextField.background",         INPUT_BG);
        UIManager.put("TextField.foreground",         TEXT_MAIN);
        UIManager.put("TextField.caretForeground",    PRIMARY);
        UIManager.put("TextField.selectionBackground",PRIMARY_LIGHT);
        UIManager.put("TextField.selectionForeground",TEXT_MAIN);

        UIManager.put("PasswordField.background",     INPUT_BG);
        UIManager.put("PasswordField.foreground",     TEXT_MAIN);
        UIManager.put("PasswordField.caretForeground",PRIMARY);
        UIManager.put("PasswordField.selectionBackground", PRIMARY_LIGHT);
        UIManager.put("PasswordField.selectionForeground", TEXT_MAIN);

        UIManager.put("TextArea.background",          INPUT_BG);
        UIManager.put("TextArea.foreground",          TEXT_MAIN);
        UIManager.put("TextArea.caretForeground",     PRIMARY);
        UIManager.put("TextArea.selectionBackground", PRIMARY_LIGHT);
        UIManager.put("TextArea.selectionForeground", TEXT_MAIN);

        UIManager.put("ComboBox.background",          INPUT_BG);
        UIManager.put("ComboBox.foreground",          TEXT_MAIN);
        UIManager.put("ComboBox.selectionBackground", PRIMARY_LIGHT);
        UIManager.put("ComboBox.selectionForeground", TEXT_MAIN);

        UIManager.put("Table.background",             CARD_BG);
        UIManager.put("Table.foreground",             TEXT_MAIN);
        UIManager.put("Table.selectionBackground",    PRIMARY_LIGHT);
        UIManager.put("Table.selectionForeground",    TEXT_MAIN);
        UIManager.put("Table.gridColor",              BORDER);

        UIManager.put("TableHeader.background",       CARD_BG);
        UIManager.put("TableHeader.foreground",       TEXT_MAIN);
        UIManager.put("TableHeader.font",             FONT_BOLD);

        UIManager.put("TabbedPane.background",        BG_DARK);
        UIManager.put("TabbedPane.foreground",        TEXT_MUTED);
        UIManager.put("TabbedPane.selectedForeground",TEXT_MAIN);
        UIManager.put("TabbedPane.selected",          CARD_BG);
        UIManager.put("TabbedPane.tabAreaBackground", BG_DARK);

        UIManager.put("ScrollPane.background",        CARD_BG);
        UIManager.put("Viewport.background",          CARD_BG);
    }

    // ─── Custom Rounded Button Class ─────────────────────────────────────────

    public static class RoundedButton extends JButton {
        private Color normalBg;
        private Color hoverBg;
        private Color pressedBg;
        private Color borderColor;
        private int cornerRadius = 18;
        private boolean isHovered = false;
        private boolean isPressed = false;

        public RoundedButton(String text, Color bg, Color fg, Color hover, Color border) {
            super(text);
            this.normalBg = bg;
            this.hoverBg = hover;
            this.pressedBg = hover.darker();
            this.borderColor = border;

            setFont(FONT_BOLD);
            setForeground(fg);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 18, 9, 18));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    isPressed = true;
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        public void setCornerRadius(int radius) {
            this.cornerRadius = radius;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color currentBg = normalBg;
            if (!isEnabled()) {
                currentBg = BORDER;
            } else if (isPressed) {
                currentBg = pressedBg;
            } else if (isHovered) {
                currentBg = hoverBg;
            }

            int w = getWidth();
            int h = getHeight();

            g2.setColor(currentBg);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.6f, 0.6f, w - 1.2f, h - 1.2f, cornerRadius, cornerRadius));
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ─── Button Factories ─────────────────────────────────────────────────────

    public static JButton createPrimaryButton(String text) {
        return new RoundedButton(text, PRIMARY, Color.WHITE, PRIMARY_HOVER, null);
    }

    public static JButton createSecondaryButton(String text) {
        return new RoundedButton(text, CARD_BG, TEXT_MAIN, CARD_HOVER, BORDER);
    }

    public static JButton createDangerButton(String text) {
        return new RoundedButton(text, DANGER, Color.WHITE, DANGER_HOVER, null);
    }

    public static JButton createSuccessButton(String text) {
        return new RoundedButton(text, new Color(16, 185, 129), Color.WHITE, new Color(5, 150, 105), null);
    }

    public static JButton createOutlineButton(String text) {
        RoundedButton btn = new RoundedButton(text, new Color(15, 23, 42, 0), PRIMARY, new Color(59, 130, 246, 35), PRIMARY);
        return btn;
    }

    // ─── Input Field Factories ────────────────────────────────────────────────

    public static JTextField createTextField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(FONT_BODY);
        field.setForeground(TEXT_MAIN);
        field.setBackground(INPUT_BG);
        field.setCaretColor(PRIMARY);
        field.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(7, 12, 7, 12)));
        return field;
    }

    public static JPasswordField createPasswordField(int columns) {
        JPasswordField field = new JPasswordField(columns);
        field.setFont(FONT_BODY);
        field.setForeground(TEXT_MAIN);
        field.setBackground(INPUT_BG);
        field.setCaretColor(PRIMARY);
        field.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(7, 12, 7, 12)));
        return field;
    }

    public static JTextArea createTextArea(int rows, int columns) {
        JTextArea area = new JTextArea(rows, columns);
        area.setFont(FONT_BODY);
        area.setForeground(TEXT_MAIN);
        area.setBackground(INPUT_BG);
        area.setCaretColor(PRIMARY);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
        return area;
    }

    public static JTextArea createReadOnlyTextArea(String text) {
        JTextArea area = new JTextArea(text);
        area.setFont(FONT_BODY);
        area.setForeground(TEXT_MAIN);
        area.setBackground(INPUT_BG);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(new EmptyBorder(8, 10, 8, 10));
        return area;
    }

    // ─── Label Factories ──────────────────────────────────────────────────────

    public static JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY);
        label.setForeground(TEXT_MAIN);
        return label;
    }

    public static JLabel createMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BODY);
        label.setForeground(TEXT_MUTED);
        return label;
    }

    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_BOLD);
        label.setForeground(TEXT_MAIN);
        return label;
    }

    // ─── Table Styling ────────────────────────────────────────────────────────

    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setForeground(TEXT_MAIN);
        table.setBackground(CARD_BG);
        table.setRowHeight(36);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(PRIMARY_LIGHT);
        table.setSelectionForeground(TEXT_MAIN);
        table.setShowGrid(true);
        table.setGridColor(new Color(30, 41, 59));
        table.setIntercellSpacing(new Dimension(0, 1));

        // Header Styling
        table.getTableHeader().setFont(FONT_BOLD);
        table.getTableHeader().setBackground(new Color(18, 26, 42));
        table.getTableHeader().setForeground(TEXT_MAIN);
        table.getTableHeader().setOpaque(true);
        table.getTableHeader().setBorder(new LineBorder(BORDER, 1));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(table.getWidth(), 38));

        // Alternating row renderer with high contrast text
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    setBackground(PRIMARY_LIGHT);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(row % 2 == 0 ? ROW_NORMAL : ROW_ALT);
                    setForeground(TEXT_MAIN);
                }
                setBorder(new EmptyBorder(0, 12, 0, 12));
                return this;
            }
        });
    }

    // ─── Borders & Badges ─────────────────────────────────────────────────────

    public static Border createCardBorder() {
        return new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(16, 20, 16, 20)
        );
    }

    public static JLabel createStatusBadge(ItemStatus status) {
        String text = status != null ? status.name() : "UNKNOWN";
        return createRoundedBadge(text, getStatusBg(status), getStatusFg(status));
    }

    public static JLabel createRoleBadge(String role) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        Color bg = isAdmin ? PURPLE_BG : INFO_BG;
        Color fg = isAdmin ? PURPLE : INFO;
        return createRoundedBadge(role, bg, fg);
    }

    public static JLabel createRoundedBadge(String text, Color bg, Color fg) {
        JLabel badge = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(FONT_BADGE);
        badge.setForeground(fg);
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(3, 10, 3, 10));
        return badge;
    }

    private static Color getStatusBg(ItemStatus status) {
        if (status == null) return BORDER;
        return switch (status) {
            case OPEN -> INFO_BG;
            case LOST -> WARNING_BG;
            case FOUND -> new Color(30, 58, 138); // Deep Blue
            case CLAIMED -> PURPLE_BG;
            case RETURNED -> SUCCESS_BG;
            case CLOSED -> new Color(30, 41, 59);
        };
    }

    private static Color getStatusFg(ItemStatus status) {
        if (status == null) return TEXT_MUTED;
        return switch (status) {
            case OPEN -> INFO;
            case LOST -> WARNING;
            case FOUND -> new Color(96, 165, 250);
            case CLAIMED -> PURPLE;
            case RETURNED -> SUCCESS;
            case CLOSED -> TEXT_MUTED;
        };
    }
}
