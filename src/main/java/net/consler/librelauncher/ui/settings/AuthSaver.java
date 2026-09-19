package net.consler.librelauncher.ui.settings;

import net.consler.librelauncherlib.auth.AuthProfile;
import net.consler.librelauncherlib.auth.AuthSession;
import net.consler.librelauncherlib.auth.MicrosoftAuthenticator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.consler.librelauncher.Main.APPDATA_DIR;

public class AuthSaver
{
    private static final Path ACCOUNTS_FILE = new File(APPDATA_DIR, "accounts.txt").toPath();
    private static final Saver SAVER;

    static
    {
        if (!APPDATA_DIR.exists()) APPDATA_DIR.mkdirs();

        try
        {
            if (!ACCOUNTS_FILE.toFile().exists()) Files.createFile(ACCOUNTS_FILE);
        }
        catch (IOException ignored){}

        SAVER = new Saver(ACCOUNTS_FILE);
    }

    public static List<String> listAccountNames()
    {
        String accountsStr = SAVER.get("accounts");
        if (accountsStr == null || accountsStr.isBlank()) return new ArrayList<>();

        return new ArrayList<>(Arrays.asList(accountsStr.split(",")));
    }

    public static String getActiveAccount()
    {
        return SAVER.get("active_account");
    }

    public static void setActiveAccount(String username)
    {
        SAVER.set("active_account", username);
    }

    public static void saveAuthProfile(String username, AuthProfile profile)
    {
        List<String> accounts = listAccountNames();
        if (!accounts.contains(username))
        {
            accounts.add(username);
            SAVER.set("accounts", String.join(",", accounts));
        }

        SAVER.set("account." + username + ".username", profile.username() != null ? profile.username() : "");
        SAVER.set("account." + username + ".uuid", profile.uuid() != null ? profile.uuid() : "");
        SAVER.set("account." + username + ".accessToken", profile.accessToken() != null ? profile.accessToken() : "");
        SAVER.set("account." + username + ".refreshToken", profile.refreshToken() != null ? profile.refreshToken() : "");
        SAVER.set("account." + username + ".expiresAt", profile.expiresAt() != null ? profile.expiresAt().toString() : "");

        if (getActiveAccount() == null || getActiveAccount().isBlank()) setActiveAccount(username);
    }

    public static AuthProfile getAuthProfile(String username)
    {

        List<String> accounts = listAccountNames();
        if (!accounts.contains(username)) return null;

        String pUsername = SAVER.get("account." + username + ".username");
        String pUuid = SAVER.get("account." + username + ".uuid");
        String pAccessToken = SAVER.get("account." + username + ".accessToken");
        String pRefreshToken = SAVER.get("account." + username + ".refreshToken");
        Instant pExpiresAt = Instant.parse(SAVER.get("account." + username + ".expiresAt"));

        AuthSession authSession = new AuthSession(new AuthProfile(pUsername, pUuid, pAccessToken, pRefreshToken, pExpiresAt));

        return authSession.ensureFreshProfile(new MicrosoftAuthenticator()).join();
    }

    public static AuthProfile getActiveAuthProfile()
    {
        String active = getActiveAccount();
        if (active != null && !active.isBlank()) return getAuthProfile(active);

        return null;
    }

    public static void deleteAccount(String username)
    {
        List<String> accounts = listAccountNames();
        if (accounts.contains(username))
        {
            accounts.remove(username);
            SAVER.set("accounts", String.join(",", accounts));

            SAVER.remove("account." + username + ".username");
            SAVER.remove("account." + username + ".uuid");
            SAVER.remove("account." + username + ".accessToken");
            SAVER.remove("account." + username + ".refreshToken");
            SAVER.remove("account." + username + ".expiresAt");

            if (username.equals(getActiveAccount()))
            {
                if (!accounts.isEmpty()) setActiveAccount(accounts.getFirst());
                else SAVER.set("active_account", null);
            }
        }
    }
}