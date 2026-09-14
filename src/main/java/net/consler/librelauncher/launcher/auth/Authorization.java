package net.consler.librelauncher.launcher.auth;

import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.theshark34.openlauncherlib.minecraft.AuthInfos;

import java.util.UUID;

public class Authorization
{
    public static AuthInfos authorize()
    {
        String activeAccount = AuthInfosSaver.getActiveAccount();
        if (activeAccount != null) return AuthInfosSaver.getAuthInfos(activeAccount);

        try
        {
            return authorizeWithWebView();
        }
        catch (Exception e)
        {
            throw new net.consler.librelauncher.exceptions.AuthException(e.getMessage(), e);
        }
    }

    public static AuthInfos authorize(String username)
    {
        return new AuthInfos(username, "0", UUID.fromString(username).toString());
    }

    public static AuthInfos authorizeWithWebView() throws MicrosoftAuthenticationException
    {
        MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
        MicrosoftAuthResult authResult = authenticator.loginWithWebview();

        String displayName = authResult.getProfile().getName();

        return new AuthInfos(displayName, authResult.getAccessToken(), authResult.getProfile().getId());
    }
}
