package net.consler.librelauncher.ui.category.settings;

import net.consler.librelauncher.settings.AuthManager;
import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.auth.MicrosoftAuthenticator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AccountsSettings
{
    public static JPanel panel;
    public static JPanel accountsListPanel;

    public static AuthManager authManager = new AuthManager();

    public static JPanel createPanel()
    {
        panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.add(Settings.createHeader("Accounts Settings"));
        panel.add(headerPanel, BorderLayout.NORTH);

        accountsListPanel = new JPanel();
        accountsListPanel.setOpaque(false);
        accountsListPanel.setLayout(new BoxLayout(accountsListPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(accountsListPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);

        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomPanel.setOpaque(false);

        JButton addOnlineBtn = new JButton("Add Online Account");
        JButton addOfflineBtn = new JButton("Add Offline Account");

        addOnlineBtn.addActionListener(e -> addOnlineAccount());
        addOfflineBtn.addActionListener(e -> addOfflineAccount());

        bottomPanel.add(addOnlineBtn);
        bottomPanel.add(addOfflineBtn);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        refreshAccountList();

        return panel;
    }

    private static void refreshAccountList()
    {
        accountsListPanel.removeAll();

        if (authManager.getAccounts().isEmpty())
        {
            JLabel emptyLabel = new JLabel("No accounts added yet.");
            emptyLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            accountsListPanel.add(emptyLabel);
        }
        else
        {
            for (Account account : authManager.getAccounts())
            {
                accountsListPanel.add(createAccountCard(account));
                accountsListPanel.add(Box.createVerticalStrut(8));
            }
        }

        accountsListPanel.revalidate();
        accountsListPanel.repaint();
    }

    private static JPanel createAccountCard(Account account)
    {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        card.setPreferredSize(new Dimension(400, 46));

        Color borderColor = UIManager.getColor("Component.borderColor");

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                new EmptyBorder(8, 14, 8, 14)
        ));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        JLabel usernameLabel = new JLabel(account.getUsername());

        if (account.isActive()) usernameLabel.setFont(usernameLabel.getFont().deriveFont(Font.BOLD));

        leftPanel.add(usernameLabel);

        if (account.isActive())
        {
            JLabel activeLabel = new JLabel("• Active");
            Color accentColor = UIManager.getColor("Component.accentColor");
            if (accentColor == null) accentColor = UIManager.getColor("ProgressBar.foreground");
            if (accentColor != null) activeLabel.setForeground(accentColor);
            activeLabel.setFont(activeLabel.getFont().deriveFont(Font.BOLD, 11f));
            leftPanel.add(activeLabel);
        }

        JLabel typeLabel = new JLabel(account.getType() == Account.AccountType.ONLINE ? "Online" : "Offline");
        Color disabledFg = UIManager.getColor("Label.disabledForeground");
        if (disabledFg != null) typeLabel.setForeground(disabledFg);
        typeLabel.setFont(typeLabel.getFont().deriveFont(11f));

        card.add(leftPanel, BorderLayout.WEST);
        card.add(typeLabel, BorderLayout.EAST);

        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem setActiveItem = new JMenuItem("Set Active");
        setActiveItem.setEnabled(!account.isActive());
        setActiveItem.addActionListener(e ->
        {
            authManager.setActiveAccount(account);
            refreshAccountList();
        });

        JMenuItem removeItem = new JMenuItem("Remove");
        removeItem.addActionListener(e ->
        {
            boolean wasActive = account.isActive();
            authManager.removeAccount(account);
            if (wasActive && !authManager.getAccounts().isEmpty())
            {
                authManager.setActiveAccount(authManager.getAccounts().getFirst());
            }
            refreshAccountList();
        });

        contextMenu.add(setActiveItem);
        contextMenu.addSeparator();
        contextMenu.add(removeItem);

        card.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                if(e.isPopupTrigger())
                {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        return card;
    }

    private static void addOfflineAccount()
    {
        String username = JOptionPane.showInputDialog(
                panel,
                "Enter offline username:",
                "Add Offline Account",
                JOptionPane.PLAIN_MESSAGE
        );

        if (username != null && !username.trim().isEmpty())
        {
            boolean setAsActive = authManager.getAccounts().isEmpty();
            authManager.addAccount(new Account(username.trim(), Account.AccountType.OFFLINE, setAsActive, AuthProfile.Offline(username.trim())));
            refreshAccountList();
        }
    }

    private static void addOnlineAccount()
    {
        new MicrosoftAuthenticator().loginWithNativeWebView()
                .thenAccept(profile -> SwingUtilities.invokeLater(() ->
                {
                    boolean setAsActive = authManager.getAccounts().isEmpty();
                    authManager.addAccount(new Account(profile.username(), Account.AccountType.ONLINE, setAsActive, profile));
                    refreshAccountList();
                }))
                .exceptionally(e ->
                {
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(
                                panel,
                                "Failed to authenticate online account:\n" + e.getLocalizedMessage(),
                                "Authentication Error",
                                JOptionPane.ERROR_MESSAGE
                        ));
                    return null;
                });
    }
}