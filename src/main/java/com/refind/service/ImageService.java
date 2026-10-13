package com.refind.service;

import com.refind.exception.ValidationException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared image storage for ReFind.
 *
 * This project persists item image paths in MySQL, so a centralized filesystem directory is the most
 * appropriate option here: it preserves the existing schema, keeps images available across restarts,
 * and avoids storing large binary payloads directly in the database for a desktop application.
 */
public class ImageService {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Path LEGACY_UPLOADS_DIR = Paths.get("uploads", "items");
    private static final Path SHARED_UPLOADS_DIR = resolveStorageRoot();

    // In-memory cache for scaled thumbnails to optimize list scrolling performance
    private final Map<String, ImageIcon> thumbnailCache = new ConcurrentHashMap<>();

    public ImageService() {
        ensureStorageDirectoryExists();
    }

    private static Path resolveStorageRoot() {
        String configured = System.getProperty("refind.image.dir");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("REFIND_IMAGE_DIR");
        }
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured).normalize();
        }
        return Paths.get(System.getProperty("user.home", "."), ".refind", "uploads", "items").normalize();
    }

    private void ensureStorageDirectoryExists() {
        try {
            if (!Files.exists(SHARED_UPLOADS_DIR)) {
                Files.createDirectories(SHARED_UPLOADS_DIR);
            }
            if (Files.exists(LEGACY_UPLOADS_DIR) && !Files.exists(SHARED_UPLOADS_DIR.resolve(".migration-complete"))) {
                migrateLegacyImages();
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not create shared image storage directory: " + e.getMessage());
        }
    }

    private void migrateLegacyImages() throws IOException {
        if (!Files.exists(LEGACY_UPLOADS_DIR)) {
            return;
        }
        try (var paths = Files.list(LEGACY_UPLOADS_DIR)) {
            for (Path legacyFile : paths.filter(Files::isRegularFile).toList()) {
                Path target = SHARED_UPLOADS_DIR.resolve(legacyFile.getFileName());
                if (!Files.exists(target)) {
                    Files.copy(legacyFile, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        Files.createFile(SHARED_UPLOADS_DIR.resolve(".migration-complete"));
    }

    /**
     * Validates and copies a user-selected image into managed shared storage.
     * Returns the absolute storage path to be persisted in MySQL.
     */
    public String storeImage(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            throw new ValidationException("Selected image file does not exist.");
        }

        if (sourceFile.length() > MAX_FILE_SIZE_BYTES) {
            throw new ValidationException("Image file size exceeds the 5 MB maximum limit.");
        }

        BufferedImage img;
        try {
            img = ImageIO.read(sourceFile);
            if (img == null) {
                throw new ValidationException("Unsupported or corrupted image file. Please choose a valid JPEG, PNG, or GIF file.");
            }
        } catch (IOException e) {
            throw new ValidationException("Failed to read image file: " + e.getMessage());
        }

        String origName = sourceFile.getName();
        String ext = "jpg";
        int dotIndex = origName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < origName.length() - 1) {
            String candExt = origName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
            if (candExt.equals("png") || candExt.equals("jpeg") || candExt.equals("jpg") || candExt.equals("gif")) {
                ext = candExt;
            }
        }

        ensureStorageDirectoryExists();
        String safeFileName = "item_" + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path targetPath = SHARED_UPLOADS_DIR.resolve(safeFileName);

        try {
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toAbsolutePath().normalize().toString().replace('\\', '/');
        } catch (IOException e) {
            throw new ValidationException("Failed to save image to storage: " + e.getMessage());
        }
    }

    /**
     * Resolves and loads an image from shared storage, legacy project-local storage, or a direct file path.
     */
    public BufferedImage loadImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        String normalized = imagePath.trim().replace('\\', '/');
        File file = null;

        Path candidate = Paths.get(normalized);
        if (candidate.isAbsolute()) {
            file = candidate.toFile();
        } else {
            file = Paths.get(normalized).toFile();
            if (!file.exists()) {
                file = SHARED_UPLOADS_DIR.resolve(candidate.getFileName() == null ? normalized : candidate.getFileName().toString()).toFile();
            }
            if (!file.exists()) {
                file = LEGACY_UPLOADS_DIR.resolve(candidate.getFileName() == null ? normalized : candidate.getFileName().toString()).toFile();
            }
        }

        if (file == null || !file.exists()) {
            return null;
        }

        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            return null;
        }
    }

    public ImageIcon getScaledThumbnail(String imagePath, int maxWidth, int maxHeight) {
        if (imagePath == null || imagePath.isBlank()) {
            return createPlaceholderIcon(maxWidth, maxHeight, "No Photo Attached");
        }

        String cacheKey = imagePath + "_" + maxWidth + "x" + maxHeight;
        ImageIcon cached = thumbnailCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        BufferedImage original = loadImage(imagePath);
        if (original == null) {
            return createPlaceholderIcon(maxWidth, maxHeight, "Photo Unavailable");
        }

        int origW = original.getWidth();
        int origH = original.getHeight();

        double scale = Math.min((double) maxWidth / origW, (double) maxHeight / origH);
        int targetW = Math.max(1, (int) Math.round(origW * scale));
        int targetH = Math.max(1, (int) Math.round(origH * scale));

        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(original, 0, 0, targetW, targetH, null);
        g2.dispose();

        ImageIcon icon = new ImageIcon(scaled);
        thumbnailCache.put(cacheKey, icon);
        return icon;
    }

    public static ImageIcon createPlaceholderIcon(int width, int height, String label) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(240, 243, 246));
        g2.fillRoundRect(0, 0, width, height, 8, 8);

        g2.setColor(new Color(210, 218, 226));
        g2.drawRoundRect(0, 0, width - 1, height - 1, 8, 8);

        int iconSize = Math.min(32, Math.min(width / 3, height / 3));
        int iconX = (width - iconSize) / 2;
        int iconY = (height - iconSize) / 2 - 8;

        g2.setColor(new Color(160, 174, 192));
        g2.drawRoundRect(iconX, iconY, iconSize, (int) (iconSize * 0.75), 6, 6);
        g2.drawOval(iconX + iconSize / 3, iconY + (int) (iconSize * 0.2), iconSize / 3, iconSize / 3);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(label);
        int textX = Math.max(4, (width - textW) / 2);
        int textY = iconY + (int) (iconSize * 0.75) + 16;
        g2.setColor(new Color(113, 128, 150));
        g2.drawString(label, textX, textY);

        g2.dispose();
        return new ImageIcon(img);
    }
}
