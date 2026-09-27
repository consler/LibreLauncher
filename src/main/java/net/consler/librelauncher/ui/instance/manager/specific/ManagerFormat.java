package net.consler.librelauncher.ui.instance.manager.specific;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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

    public static String joinNonBlank(String separator, String... parts)
    {
        StringBuilder result = new StringBuilder();
        for (String part : parts)
        {
            if (part == null || part.isBlank()) continue;
            if (!result.isEmpty()) result.append(separator);
            result.append(part);
        }
        return result.toString();
    }

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