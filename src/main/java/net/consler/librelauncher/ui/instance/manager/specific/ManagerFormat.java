package net.consler.librelauncher.ui.instance.manager.specific;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Small formatting and icon-building helpers shared across the instance-specific
 * manager categories, so byte sizes, dates and BufferedImage icons (as returned by
 * librelauncherlib's Mod/World/ResourcePack/Datapack/ServerEntry classes) are all
 * rendered consistently.
 */
public final class ManagerFormat
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    private ManagerFormat()
    {
    }

    public static String size(long bytes)
    {
        if (bytes < 1024) return bytes + " B";

        int exponent = (int) (Math.log(bytes) / Math.log(1024));
        exponent = Math.min(exponent, 4);
        char unit = "KMGT".charAt(exponent - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exponent), unit);
    }

    public static String date(long epochMillis)
    {
        if (epochMillis <= 0) return "Unknown";
        return DATE_FORMAT.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()));
    }

    /**
     * Joins the given parts with the separator, skipping any that are null/blank.
     */
    public static String joinNonBlank(String separator, String... parts)
    {
        StringBuilder result = new StringBuilder();
        for (String part : parts)
        {
            if (part == null || part.isBlank()) continue;
            if (result.length() > 0) result.append(separator);
            result.append(part);
        }
        return result.toString();
    }

    /**
     * Builds an icon Node from a BufferedImage (as returned by e.g. Mod.getIcon(),
     * World.getIcon(), ResourcePack.getIcon(), Datapack.getIcon() or
     * Servers.ServerEntry.getIconImage()), falling back to a glyph label when no
     * image is available.
     */
    public static Node imageIcon(BufferedImage image, boolean available, String fallbackGlyph)
    {
        if (available && image != null)
        {
            ImageView imageView = new ImageView(SwingFXUtils.toFXImage(image, null));
            imageView.setFitWidth(48);
            imageView.setFitHeight(48);
            imageView.setPreserveRatio(true);
            imageView.getStyleClass().add("manager-item-image");
            return imageView;
        }

        Label label = new Label(fallbackGlyph);
        label.getStyleClass().add("manager-item-icon");
        return label;
    }
}