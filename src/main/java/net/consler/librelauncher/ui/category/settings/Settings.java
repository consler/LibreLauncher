package net.consler.librelauncher.ui.category.settings;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.category.RoundCornerPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class Settings
{
    private final Main main;
    public JPanel panel;
    public JPanel contentPanel;
    public CardLayout cardLayout;

    public final List<JButton> categoryButtons = new ArrayList<>();
    public JButton generalButton;
    public JButton appearanceButton;
    public JButton behaviourButton;
    public JButton accountsButton;

    public SettingsSaver settingsSaver;
    private final Map<String, Supplier<JPanel>> categoryPages = new HashMap<>();
    private final Set<String> loadedPages = new HashSet<>();

    public Settings(Main main)
    {
        this.main = main;
    }

    public void show()
    {
        if (panel != null)
        {
            main.showPage("settings");
            return;
        }

        settingsSaver = new SettingsSaver();

        panel = new RoundCornerPanel(main.client.getBackground());
        panel.setLayout(new BorderLayout());

        JPanel categoryPanel = new JPanel();
        categoryPanel.setLayout(new BoxLayout(categoryPanel, BoxLayout.Y_AXIS));
        categoryPanel.setOpaque(false);

        categoryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, ThemeManager.outlineColor(main.client.getBackground())),
                BorderFactory.createEmptyBorder(20, 15, 20, 15)
        ));
        categoryPanel.setPreferredSize(new Dimension(170, 0));

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 30));

        generalButton = new JButton("Minecraft");
        appearanceButton = new JButton("Appearance");
        behaviourButton = new JButton("Behaviour");
        accountsButton = new JButton("Accounts");

        addCategory(generalButton, () -> new MinecraftSettings(settingsSaver).createPanel(), categoryPanel);
        addCategory(appearanceButton, () -> new AppearanceSettings(main, settingsSaver).createPanel(), categoryPanel);
        addCategory(behaviourButton, () -> new BehaviourSettings(settingsSaver).createPanel(), categoryPanel);
        addCategory(accountsButton, () -> new AccountsSettings(main).createPanel(), categoryPanel);

        if (!categoryButtons.isEmpty()) selectCategory(generalButton);

        panel.add(categoryPanel, BorderLayout.WEST);
        panel.add(contentPanel, BorderLayout.CENTER);

        main.pageContainer.add(panel, "settings");
        main.showPage("settings");
    }

    private void addCategory(JButton button, Supplier<JPanel> pageFactory, JPanel categoryPanel)
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
            if (button == selectedButton) button.setBackground(ThemeManager.surfaceColor(main.client.getBackground()));
            else button.setBackground(null);
        }
    }

    public static JLabel createHeader(String text)
    {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}