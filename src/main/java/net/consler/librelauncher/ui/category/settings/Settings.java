package net.consler.librelauncher.ui.category.settings;

import net.consler.librelauncher.Main;
import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.category.RoundCornerPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Settings
{
    public static JPanel panel;
    public static JPanel contentPanel;
    public static CardLayout cardLayout;

    public static List<JButton> categoryButtons;
    public static JButton generalButton;
    public static JButton appearanceButton;
    public static JButton behaviourButton;
    public static JButton accountsButton;

    public static SettingsSaver settingsSaver;

    public static void show()
    {
        settingsSaver = new SettingsSaver();

        panel = new RoundCornerPanel();
        panel.setLayout(new BorderLayout());

        categoryButtons = new ArrayList<>();

        JPanel categoryPanel = new JPanel();
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

        generalButton = new JButton("Minecraft");
        appearanceButton = new JButton("Appearance");
        behaviourButton = new JButton("Behaviour");
        accountsButton = new JButton("Accounts");

        addCategory(generalButton, "Minecraft", MinecraftSettings.createPanel(), categoryPanel);
        addCategory(appearanceButton, "Appearance", AppearanceSettings.createPanel(), categoryPanel);
        addCategory(behaviourButton, "Behaviour", BehaviourSettings.createPanel(), categoryPanel);
        addCategory(accountsButton, "Accounts", AccountsSettings.createPanel(), categoryPanel);

        if (!categoryButtons.isEmpty()) selectCategory(generalButton, "Minecraft");

        panel.add(categoryPanel, BorderLayout.WEST);
        panel.add(contentPanel, BorderLayout.CENTER);

        Main.client.add(panel, BorderLayout.CENTER);
    }

    private static void addCategory(JButton button, String name, JPanel content, JPanel categoryPanel)
    {
        contentPanel.add(content, name);

        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        button.setPreferredSize(new Dimension(Integer.MAX_VALUE, 35));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addActionListener(e -> selectCategory(button, name));

        categoryButtons.add(button);
        categoryPanel.add(button);
        categoryPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    }

    public static void selectCategory(JButton selectedButton, String cardName)
    {
        cardLayout.show(contentPanel, cardName);

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

    public static void hide()
    {
        if (panel != null) Main.client.remove(panel);
    }
}