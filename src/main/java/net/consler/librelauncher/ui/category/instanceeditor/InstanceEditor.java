package net.consler.librelauncher.ui.category.instanceeditor;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.InstancesConfig;
import net.consler.librelauncher.ui.category.RoundCornerPanel;
import net.consler.librelauncherlib.instance.InstanceProfile;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class InstanceEditor
{
    private final Main main;
    public JPanel panel;
    public JPanel contentPanel;
    public CardLayout cardLayout;

    private JPanel categoryPanel;
    public final List<JButton> categoryButtons = new ArrayList<>();
    public JButton settingsButton;
    public JButton logsButton;
    public JButton modsButton;
    public JButton resourcePacksButton;

    public InstancesConfig instancesConfig;
    private final Map<String, Supplier<JPanel>> categoryPages = new HashMap<>();
    private final Set<String> loadedPages = new HashSet<>();

    public InstanceEditor(Main main)
    {
        this.main = main;
    }

    public void show(InstanceProfile instance)
    {
        instancesConfig = new InstancesConfig();

        if (panel == null) createLayout();
        categoryPanel.removeAll();
        contentPanel.removeAll();
        categoryButtons.clear();
        categoryPages.clear();
        loadedPages.clear();

        settingsButton = new JButton("Settings");
        modsButton = new JButton("Mods");
        resourcePacksButton = new JButton("Resource Packs");
        logsButton = new JButton("Logs");

        addCategory(settingsButton, () -> new InstanceSettings(main, instancesConfig).createPanel(instance));
        addCategory(modsButton, () -> new InstanceMods(main).createPanel(instance));

        selectCategory(settingsButton);
        categoryPanel.revalidate();
        categoryPanel.repaint();
        contentPanel.revalidate();
        contentPanel.repaint();
        main.showPage("instanceeditor");
    }

    private void createLayout()
    {
        panel = new RoundCornerPanel(main.client.getBackground());
        panel.setLayout(new BorderLayout());

        categoryPanel = new JPanel();
        categoryPanel.setLayout(new BoxLayout(categoryPanel, BoxLayout.Y_AXIS));
        categoryPanel.setOpaque(false);
        categoryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(255, 255, 255, 20)),
                BorderFactory.createEmptyBorder(20, 15, 20, 15)
        ));
        categoryPanel.setPreferredSize(new Dimension(170, 0));

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        panel.add(categoryPanel, BorderLayout.WEST);
        panel.add(contentPanel, BorderLayout.CENTER);
        main.pageContainer.add(panel, "instanceeditor");
    }

    private void addCategory(JButton button, Supplier<JPanel> pageFactory)
    {
        categoryPages.put(button.getText(), pageFactory);

        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        button.setPreferredSize(new Dimension(Integer.MAX_VALUE, 35));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> selectCategory(button));

        categoryButtons.add(button);
        categoryPanel.add(button);
        categoryPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    }

    public void selectCategory(JButton selectedButton)
    {
        String pageName = selectedButton.getText();
        if (!categoryPages.containsKey(pageName)) return;

        if (!loadedPages.contains(pageName))
        {
            contentPanel.add(categoryPages.get(pageName).get(), pageName);
            loadedPages.add(pageName);
        }
        cardLayout.show(contentPanel, selectedButton.getText());

        for (JButton button : categoryButtons)
        {
            if (button == selectedButton) button.setBackground(new Color(255, 255, 255, 30));
            else button.setBackground(null);
        }
    }

    public static JLabel createHeader(String text)
    {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        return label;
    }
}
