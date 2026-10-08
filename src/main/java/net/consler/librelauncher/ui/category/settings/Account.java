package net.consler.librelauncher.ui.category.settings;

import net.consler.librelauncherlib.auth.AuthProfile;

public class Account
{
    public enum AccountType
    {
        ONLINE, OFFLINE
    }

    private final String username;
    private final AccountType type;
    private boolean active;
    private final AuthProfile authProfile;

    public Account(String username, AccountType type, boolean active, AuthProfile authProfile)
    {
        this.username = username;
        this.type = type;
        this.active = active;
        this.authProfile = authProfile;
    }

    public String getUsername()
    {
        return username;
    }

    public AccountType getType()
    {
        return type;
    }

    public AuthProfile getAuthProfile()
    {
        return authProfile;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }
}
