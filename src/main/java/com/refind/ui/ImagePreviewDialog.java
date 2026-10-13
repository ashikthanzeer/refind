package com.refind.ui;

import com.refind.service.ImageService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePreviewDialog extends JDialog {

    public ImagePreviewDialog(Window parent, String title, String imagePath, ImageService imageService) {
        super(parent, title != null ? title + " - Photo Preview" : "Photo Preview", ModalityType.APPLICATION_MODAL);

        initComponents(imagePath, imageService);
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents(String imagePath, ImageService imageService) {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Color.BLACK);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        setContentPane(root);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxW = Math.max(400, (int) (screenSize.width * 0.75));
        int maxH = Math.max(300, (int) (screenSize.height * 0.75));

        BufferedImage original = imageService.loadImage(imagePath);
        JLabel imageLabel;

        if (original != null) {
            int origW = original.getWidth();
            int origH = original.getHeight();

            double scale = Math.min((double) maxW / origW, (double) maxH / origH);
            if (scale > 1.0) {
                scale = 1.0; // Don't upscale small images beyond original dimensions
            }
            int targetW = Math.max(1, (int) Math.round(origW * scale));
            int targetH = Math.max(1, (int) Math.round(origH * scale));

            Image scaled = original.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);
            imageLabel = new JLabel(new ImageIcon(scaled));
        } else {
            imageLabel = new JLabel("Image not available", SwingConstants.CENTER);
            imageLabel.setForeground(Color.LIGHT_GRAY);
            imageLabel.setPreferredSize(new Dimension(360, 240));
        }

        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(new JScrollPane(imageLabel), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Color.BLACK);
        JButton closeBtn = new JButton("Close Preview");
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);
        root.add(bottomPanel, BorderLayout.SOUTH);
    }
}
