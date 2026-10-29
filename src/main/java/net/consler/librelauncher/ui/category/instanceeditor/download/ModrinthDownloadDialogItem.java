package net.consler.librelauncher.ui.category.instanceeditor.download;

import net.consler.librelauncherlib.integration.modrinth.ModrinthProject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class ModrinthDownloadDialogItem extends JPanel
{
    private final AbstractButton selectionButton;
    private final JLabel iconLabel;
    private final boolean installed;

    ModrinthDownloadDialogItem(
            ModrinthProject project,
            boolean installed,
            boolean allowMultipleSelection,
            Color background,
            Color outline,
            Consumer<AbstractButton> onSelectionChanged,
            Consumer<AbstractButton> onDoubleClick,
            Consumer<ModrinthProject> onProjectClicked,
            Consumer<ModrinthDownloadDialogItem> onPressed)
    {
        super(new BorderLayout(8, 0));
        this.installed = installed;

        String title = project.title() == null || project.title().isBlank() ? project.slug() : project.title();
        selectionButton = allowMultipleSelection ? new JCheckBox() : new JRadioButton();
        selectionButton.setOpaque(false);
        selectionButton.setEnabled(!installed);
        selectionButton.addActionListener(event ->
        {
            onSelectionChanged.accept(selectionButton);
            onProjectClicked.accept(project);
        });

        iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(50, 50));
        iconLabel.setOpaque(true);
        iconLabel.setBackground(background);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setToolTipText(installed ? title + " is already installed" : title);
        if (installed) titleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        DescriptionLabel descriptionLabel = new DescriptionLabel(project.description());
        descriptionLabel.setFont(titleLabel.getFont().deriveFont(10f));
        descriptionLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        setPreferredSize(new Dimension(100, 74));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setBackground(background);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, outline),
                new EmptyBorder(5, 6, 5, 6)
        ));

        JPanel leading = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        leading.setOpaque(false);
        leading.add(selectionButton);
        leading.add(iconLabel);

        JPanel details = new JPanel(new BorderLayout(0, 2));
        details.setOpaque(false);
        details.add(titleLabel, BorderLayout.NORTH);
        details.add(descriptionLabel, BorderLayout.CENTER);
        add(leading, BorderLayout.WEST);
        add(details, BorderLayout.CENTER);

        MouseAdapter mouseAdapter = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent event)
            {
                onPressed.accept(ModrinthDownloadDialogItem.this);
                onProjectClicked.accept(project);
                setBackground(background.brighter());
                if (!ModrinthDownloadDialogItem.this.installed && event.getClickCount() == 2)
                    onDoubleClick.accept(selectionButton);
            }
        };
        addMouseListener(mouseAdapter);
        titleLabel.addMouseListener(mouseAdapter);
        iconLabel.addMouseListener(mouseAdapter);
        descriptionLabel.addMouseListener(mouseAdapter);
    }

    AbstractButton selectionButton()
    {
        return selectionButton;
    }

    JLabel iconLabel()
    {
        return iconLabel;
    }

    private static final class DescriptionLabel extends JComponent
    {
        private final String description;

        private DescriptionLabel(String description)
        {
            this.description = description == null ? "" : description;
            setPreferredSize(new Dimension(100, 30));
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            super.paintComponent(graphics);
            if (description.isBlank()) return;

            Graphics2D g = (Graphics2D) graphics.create();
            g.setFont(getFont());
            g.setColor(getForeground());
            FontMetrics metrics = g.getFontMetrics();
            List<String> lines = wrappedLines(metrics, getWidth());
            int y = metrics.getAscent();
            for (String line : lines)
            {
                g.drawString(line, 0, y);
                y += metrics.getHeight();
            }
            g.dispose();
        }

        private List<String> wrappedLines(FontMetrics metrics, int width)
        {
            int availableWidth = Math.max(1, width);
            String[] words = description.trim().split("\\s+");
            List<String> lines = new ArrayList<>(2);
            StringBuilder line = new StringBuilder();

            for (String word : words)
            {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (metrics.stringWidth(candidate) <= availableWidth)
                {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }

                if (!line.isEmpty())
                {
                    if (lines.size() == 1)
                    {
                        lines.add(ellipsize(line.toString(), metrics, availableWidth));
                        return lines;
                    }
                    lines.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                }
                else
                {
                    line.append(word);
                }

                if (metrics.stringWidth(line.toString()) > availableWidth)
                {
                    lines.add(ellipsize(line.toString(), metrics, availableWidth));
                    return lines;
                }
            }

            if (!line.isEmpty()) lines.add(line.toString());
            return lines;
        }

        private static String ellipsize(String text, FontMetrics metrics, int width)
        {
            String ellipsis = "...";
            StringBuilder shortened = new StringBuilder(text);
            while (!shortened.isEmpty() && metrics.stringWidth(shortened + ellipsis) > width)
                shortened.setLength(shortened.length() - 1);
            return shortened + ellipsis;
        }
    }
}
