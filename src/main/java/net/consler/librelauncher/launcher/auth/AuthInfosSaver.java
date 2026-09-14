package net.consler.librelauncher.launcher.auth;

import fr.theshark34.openlauncherlib.minecraft.AuthInfos;
import fr.theshark34.openlauncherlib.util.Saver;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static net.consler.librelauncher.Main.APPDATA_DIR;

public class AuthInfosSaver
{

    private static final Path CONFIG_FILE = new File(APPDATA_DIR, "authinfos.txt").toPath();
    private static final Saver SAVER = new Saver(CONFIG_FILE);

    public static void saveAuthInfos(AuthInfos authInfos)
    {
        saveAuthInfos(authInfos.getUsername(), authInfos);
    }

    public static void saveOfflineAccount(String username)
    {
        AuthInfos offlineAuth = new AuthInfos(username, "0", String.valueOf(UUID.nameUUIDFromBytes(username.getBytes())));
        saveAuthInfos(username, offlineAuth);
    }

    public static void saveAuthInfos(String accountName, AuthInfos authInfos)
    {
        if (!APPDATA_DIR.exists()) APPDATA_DIR.mkdirs();

        SAVER.set("account." + accountName + ".name", authInfos.getUsername());
        SAVER.set("account." + accountName + ".token", authInfos.getAccessToken() == null ? "" : authInfos.getAccessToken());
        SAVER.set("account." + accountName + ".uuid", authInfos.getUuid() == null ? "" : authInfos.getUuid());

        List<String> accounts = new ArrayList<>(listAccountNames());
        if (!accounts.contains(accountName)) accounts.add(accountName);

        SAVER.set("accounts", String.join(";", accounts));
        SAVER.set("active_account", accountName);
    }

    public static AuthInfos getAuthInfos()
    {
        return getAuthInfos(getActiveAccount());
    }

    public static AuthInfos getAuthInfos(String accountName)
    {
        String name = SAVER.get("account." + accountName + ".name");
        String token = SAVER.get("account." + accountName + ".token");
        String uuid = SAVER.get("account." + accountName + ".uuid");
        
        return new AuthInfos(name, token, uuid);

    }

    public static List<String> listAccountNames()
    {
        String rawAccounts = SAVER.get("accounts");
        if (rawAccounts == null || rawAccounts.isBlank()) return new ArrayList<>();

        return Arrays.stream(rawAccounts.split(";")).map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    public static String getActiveAccount()
    {
        String activeAccount = SAVER.get("active_account");
        if (activeAccount != null && !activeAccount.isBlank()) return activeAccount;

        List<String> accounts = listAccountNames();
        if (!accounts.isEmpty()) return accounts.getFirst();

        return null;
    }

    public static void setActiveAccount(String accountName)
    {
        if (accountName == null || accountName.isBlank()) return;
        if (!listAccountNames().contains(accountName)) return;

        SAVER.set("active_account", accountName);
    }

    public static void deleteAccount(String accountName)
    {
        if (accountName == null || accountName.isBlank()) return;

        SAVER.remove("account." + accountName + ".name");
        SAVER.remove("account." + accountName + ".token");
        SAVER.remove("account." + accountName + ".uuid");

        List<String> accounts = new ArrayList<>(listAccountNames());
        accounts.remove(accountName);
        SAVER.set("accounts", String.join(";", accounts));

        if (accountName.equals(getActiveAccount())) SAVER.set("active_account", accounts.isEmpty() ? "" : accounts.getFirst());
    }
}
