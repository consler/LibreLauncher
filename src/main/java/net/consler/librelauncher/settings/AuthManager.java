package net.consler.librelauncher.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncher.ui.category.settings.Account;
import net.consler.librelauncherlib.auth.AuthProfile;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

import static net.consler.librelauncher.settings.SettingsSaver.configDir;

public class AuthManager
{
    public static final File accountsFile = new File(configDir.toFile(), "accounts.json");

    public ArrayList<Account> accounts = new ArrayList<>();

    public AuthManager()
    {
        if (!configDir.toFile().exists())
        {
            configDir.toFile().mkdirs();
        }

        if (accountsFile.isFile())
        {
            try (FileReader reader = new FileReader(accountsFile))
            {
                JsonElement loaded = JsonParser.parseReader(reader);
                if (loaded != null && loaded.isJsonArray())
                {
                    loaded.getAsJsonArray().forEach(account -> accounts.add(parseAccount(account)));
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to load accounts.json", e);
            }
        }
    }

    public void addAccount(Account account)
    {
        accounts.forEach(existingAccount -> existingAccount.setActive(false));

        account.setActive(true);
        accounts.add(account);

        save();
    }

    public Account parseAccount(JsonElement accountElement)
    {
        JsonObject accountObject = accountElement.getAsJsonObject();

        String username = accountObject.get("username").getAsString();
        Account.AccountType type = Account.AccountType.valueOf(accountObject.get("type").getAsString());
        boolean isActive = accountObject.get("isActive").getAsBoolean();

        JsonObject profileObject = accountObject.get("profile").getAsJsonObject();
        String profileUsername = profileObject.get("username").getAsString();
        String uuid = profileObject.get("uuid").getAsString();
        String accessToken = profileObject.get("accessToken").getAsString();
        String refreshToken = profileObject.get("refreshToken").getAsString();
        Instant expiresAt = Instant.parse(profileObject.get("expiresAt").getAsString());

        AuthProfile authProfile = new AuthProfile(profileUsername, uuid, accessToken, refreshToken, expiresAt);

        return new Account(username, type, isActive, authProfile);
    }

    public JsonElement parseJsonElement(Account account)
    {
        JsonObject accountObject = new JsonObject();
        accountObject.addProperty("type", account.getType().toString());
        accountObject.addProperty("isActive", account.isActive());
        accountObject.addProperty("username", account.getUsername());

        JsonObject profileObject = new JsonObject();
        AuthProfile authProfile = account.getAuthProfile();
        profileObject.addProperty("username", authProfile.username());
        profileObject.addProperty("uuid", authProfile.uuid());
        profileObject.addProperty("accessToken", authProfile.accessToken());
        profileObject.addProperty("refreshToken", authProfile.refreshToken());
        profileObject.addProperty("expiresAt", authProfile.expiresAt().toString());

        accountObject.add("profile", profileObject);

        return accountObject;
    }

    public ArrayList<Account> getAccounts()
    {
        return accounts;
    }

    public void removeAccount(Account account)
    {
        accounts.remove(account);

        save();
    }

    public void save()
    {
        JsonArray jsonArray = new JsonArray();
        accounts.forEach(account -> jsonArray.add(parseJsonElement(account)));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(accountsFile))
        {
            gson.toJson(jsonArray, writer);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to save accounts.json", e);
        }
    }

    public Account getActiveAccount()
    {
        return accounts.stream().filter(Account::isActive).findFirst().orElse(null);
    }

    public void setActiveAccount(Account account)
    {
        accounts.forEach(a -> a.setActive(a == account));
        save();
    }
}