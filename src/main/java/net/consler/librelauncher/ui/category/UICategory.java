package net.consler.librelauncher.ui.category;

import net.consler.librelauncher.Main;

import javax.swing.*;
import java.awt.*;

public class UICategory extends JPanel
{
    public UICategory()
    {
        this.setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Main.client.getBackground());

        int radius = 20;

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius * 2, radius * 2);
        g2.fillRect(radius, 0, getWidth() - radius, radius);
        g2.fillRect(0, radius, radius, getHeight() - radius);
        g2.fillRect(radius, radius, getWidth() - radius, getHeight() - radius);

        g2.dispose();
    }
}
