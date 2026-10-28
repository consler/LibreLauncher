package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.ErrorDialog;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.ui.category.settings.Settings;
import net.consler.librelauncherlib.instance.InstanceProfile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public abstract class InstanceItem
{
    protected final Main main;
    public JPanel panel;
    protected JPanel listPanel;

    private final String title;
    private final String type;

    protected InstanceItem(Main main, String title, String type)
    {
        this.main = main;
        this.title = title;
        this.type = type;
    }

    public final JPanel createPanel(InstanceProfile instance)
    {
        panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(Settings.createHeader(title), BorderLayout.NORTH);

        listPanel = new ItemListPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPopupMenu popupMenu = createPopup(instance);
        panel.setComponentPopupMenu(popupMenu);
        scrollPane.setComponentPopupMenu(popupMenu);

        if (Boolean.parseBoolean(new SettingsSaver().get("show_side_panel", "false")))
            panel.add(new SidePanel(main, type, instance).show(), BorderLayout.EAST);

        populate(instance);

        return panel;
    }

    public abstract void populate(InstanceProfile instance);

    public abstract JPopupMenu createPopup(InstanceProfile instance);

    protected final JPanel createItemRow(String name, Image icon, boolean reserveIconSpace)
    {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rowPanel.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, ThemeManager.outlineColor(panel.getBackground())));
        rowPanel.setPreferredSize(new Dimension(0, 65));
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowPanel.setBackground(panel.getBackground());

        if (icon != null) rowPanel.add(new JLabel(new ImageIcon(icon)));
        else if (reserveIconSpace)
        {
            JLabel iconLabel = new JLabel();
            iconLabel.setPreferredSize(new Dimension(50, 50));
            rowPanel.add(iconLabel);
        }
        rowPanel.add(new JLabel(name));
        return rowPanel;
    }

    protected final void addItemRow(JPanel rowPanel)
    {
        rowPanel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                Arrays.stream(listPanel.getComponents()).forEach(component -> {
                    component.setBackground(panel.getBackground());
                });
                rowPanel.setBackground(panel.getBackground().brighter());
            }
        });

        listPanel.add(rowPanel);
        listPanel.add(Box.createVerticalStrut(5));
    }

    protected final void showListingError(ExecutionException exception, String activity)
    {
        new ErrorDialog(main.client, new Exception(exception.getCause()), activity);
    }

    private static class ItemListPanel extends JPanel implements Scrollable
    {
        @Override
        public Dimension getPreferredScrollableViewportSize()
        {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return Math.max(visibleRect.height - 16, 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return false;
        }
    }
}
