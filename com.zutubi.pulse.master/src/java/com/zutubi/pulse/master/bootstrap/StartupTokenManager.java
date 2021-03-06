package com.zutubi.pulse.master.bootstrap;

import com.zutubi.pulse.master.api.TokenManager;
import com.zutubi.pulse.master.model.User;
import com.zutubi.pulse.servercore.api.AdminTokenManager;
import com.zutubi.pulse.servercore.api.AuthenticationException;

/**
 */
public class StartupTokenManager implements TokenManager
{
    private AdminTokenManager adminTokenManager;

    public String login(String username, String password) throws AuthenticationException
    {
        throw new AuthenticationException("System startup in progress");
    }

    public String login(String username, String password, long expiry) throws AuthenticationException
    {
        throw new AuthenticationException("System startup in progress");
    }

    public boolean logout(String token)
    {
        return false;
    }

    public void verifyAdmin(String token) throws AuthenticationException
    {
        if (!adminTokenManager.checkAdminToken(token))
        {
            throw new AuthenticationException("Invalid token");
        }
    }

    public void verifyUser(String token) throws AuthenticationException
    {
        throw new AuthenticationException("System startup in progress");
    }

    public void verifyRoleIn(String token, String... allowedAuthorities) throws AuthenticationException
    {
        throw new AuthenticationException("System startup in progress");
    }

    public void loginUser(String token) throws AuthenticationException
    {
        throw new AuthenticationException("System startup in progress");
    }

    public User loginAndReturnUser(String token) throws AuthenticationException
    {
        throw new AuthenticationException("System startup in progress");
    }

    public void logoutUser()
    {
    }

    public void setAdminTokenManager(AdminTokenManager adminTokenManager)
    {
        this.adminTokenManager = adminTokenManager;
    }
}
