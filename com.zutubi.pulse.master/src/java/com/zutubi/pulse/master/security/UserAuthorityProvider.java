package com.zutubi.pulse.master.security;

import com.zutubi.pulse.master.model.User;
import com.zutubi.tove.security.AuthorityProvider;
import com.zutubi.tove.security.DefaultAccessManager;

import java.util.Set;

/**
 * Provides authorities for users by delegating to
 * {@link com.zutubi.pulse.master.security.UserConfigurationAuthorityProvider}.
 */
public class UserAuthorityProvider implements AuthorityProvider<User>
{
    private UserConfigurationAuthorityProvider userConfigurationAuthorityProvider;

    public Set<String> getAllowedAuthorities(String action, User resource)
    {
        return userConfigurationAuthorityProvider.getAllowedAuthorities(action, resource.getConfig());
    }

    public void setAccessManager(DefaultAccessManager accessManager)
    {
        accessManager.registerAuthorityProvider(User.class, this);
    }

    public void setUserConfigurationAuthorityProvider(UserConfigurationAuthorityProvider userConfigurationAuthorityProvider)
    {
        this.userConfigurationAuthorityProvider = userConfigurationAuthorityProvider;
    }
}
