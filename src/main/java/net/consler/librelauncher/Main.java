package net.consler.librelauncher;

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

    static void main(String[] args)
    {
        instance = new Main();
        instance.start();
    }

    private void start()
    {
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

            client.getContentPane().setBackground(client.getBackground().darker());

            pageLayout = new CardLayout();
            pageContainer = new JPanel(pageLayout);
            pageContainer.setOpaque(false);
            client.add(pageContainer, BorderLayout.CENTER);

            client.setShape(new RoundRectangle2D.Double(0, 0, 800, 500, 20, 20));
            client.addComponentListener(new ComponentAdapter()
            {
                @Override
                public void componentResized(ComponentEvent e)
                {
                    client.setShape(new RoundRectangle2D.Double(0, 0, client.getWidth(), client.getHeight(), 20, 20));
                }
            });

            titlebar = new Titlebar(this);
            titlebar.show(client);

            home = new Home(this);
            instanceManager = new InstanceManager(this);
            settings = new Settings(this);
            instanceEditor = new InstanceEditor(this);
            sidebar = new Sidebar(this);
            sidebar.show();

            home.show();

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

    public void refreshInstanceViews()
    {
        if (instanceManager.instancePanel != null) instanceManager.instancePanel.populate();
        else home.populate();
    }
}