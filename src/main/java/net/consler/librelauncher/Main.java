package net.consler.librelauncher;

import net.consler.librelauncher.settings.SettingsSaver;
import net.consler.librelauncher.ui.ThemeManager;
import net.consler.librelauncher.ui.category.instanceeditor.InstanceEditor;
import net.consler.librelauncher.ui.category.home.Home;
import net.consler.librelauncher.ui.category.instancemanager.InstanceManager;
import net.consler.librelauncher.ui.category.settings.Settings;
import net.consler.librelauncher.ui.Sidebar;
import net.consler.librelauncher.ui.Titlebar;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;

public class Main
{
    private static Main instance;

    public JFrame client;
    public JPanel pageContainer;
    public CardLayout pageLayout;
    public ThemeManager themeManager;
    public Sidebar sidebar;
    public Titlebar titlebar;
    public Home home;
    public InstanceManager instanceManager;
    public Settings settings;
    public InstanceEditor instanceEditor;
    private boolean roundWindowCorners = true;

    static void main(String[] args)
    {
        instance = new Main();
        instance.start();
    }

    private void start()
    {
        roundWindowCorners = Boolean.parseBoolean(new SettingsSaver().get("round_window_corners", "true"));
        themeManager = new ThemeManager(this);
        themeManager.init();
        SwingUtilities.invokeLater(() ->
        {
            client = new JFrame("LibreLauncher");
            client.setIconImage(Toolkit.getDefaultToolkit().getImage(Main.class.getResource("icon.png")));
            client.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.setSize(800, 500);
            client.setLocationRelativeTo(null);
            client.setLayout(new BorderLayout());
            client.setUndecorated(true);

            client.addComponentListener(new ComponentAdapter()
            {
                @Override
                public void componentResized(ComponentEvent e)
                {
                    refreshWindowShape();
                }
            });
            refreshWindowShape();

            reloadThemeUI(false);

            client.setVisible(true);
        });
    }

    public static Main getInstance()
    {
        return instance;
    }

    public void showPage(String name)
    {
        pageLayout.show(pageContainer, name);
    }

    public void reloadThemeUI()
    {
        reloadThemeUI(true);
    }

    private void reloadThemeUI(boolean showAppearanceSettings)
    {
        client.getContentPane().removeAll();
        client.getContentPane().setBackground(ThemeManager.surfaceColor(client.getBackground()));

        pageLayout = new CardLayout();
        pageContainer = new JPanel(pageLayout);
        pageContainer.setOpaque(false);
        client.add(pageContainer, BorderLayout.CENTER);

        titlebar = new Titlebar(this);
        titlebar.show(client);

        home = new Home(this);
        instanceManager = new InstanceManager(this);
        settings = new Settings(this);
        instanceEditor = new InstanceEditor(this);
        sidebar = new Sidebar(this);
        sidebar.show();

        home.show();
        if (showAppearanceSettings)
        {
            sidebar.setUICategory("settings");
            settings.selectCategory(settings.appearanceButton);
        }

        client.revalidate();
        client.repaint();
    }

    public void refreshInstanceViews()
    {
        if (instanceManager.instancePanel != null) instanceManager.instancePanel.populate();
        else home.populate();
    }

    public void setRoundWindowCorners(boolean rounded)
    {
        roundWindowCorners = rounded;
        refreshWindowShape();
    }

    private void refreshWindowShape()
    {
        if (client == null) return;

        if (roundWindowCorners)
            client.setShape(new RoundRectangle2D.Double(0, 0, client.getWidth(), client.getHeight(), 20, 20));
        else
            client.setShape(null);
    }
}