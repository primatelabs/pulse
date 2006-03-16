package com.cinnamonbob.web.user;

import com.cinnamonbob.model.SubscriptionManager;
import com.cinnamonbob.model.UserManager;
import com.cinnamonbob.model.User;
import com.cinnamonbob.web.ActionSupport;
import com.opensymphony.util.TextUtils;

/**
 *
 * 
 */
public class UserActionSupport extends ActionSupport
{
    /**
     * Useful resources commonly used by User related Actions.
     */
    private UserManager userManager;

    private SubscriptionManager subscriptionManager;

    /**
     * static long indicating that a value has not been specified.
     */
    public static final long NONE_SPECIFIED = -1;

    /**
     * User id.
     */
    private long userId = NONE_SPECIFIED;

    /**
     * User login.
     */
    private String userLogin = null;

    /**
     * Required resource.
     *
     * @param userManager
     */
    public void setUserManager(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public UserManager getUserManager()
    {
        return userManager;
    }

    public SubscriptionManager getSubscriptionManager()
    {
        return subscriptionManager;
    }

    public void setSubscriptionManager(SubscriptionManager subscriptionManager)
    {
        this.subscriptionManager = subscriptionManager;
    }

    /**
     *
     * @return user id, or -1 if none specified.
     */
    public long getUserId()
    {
        return userId;
    }

    /**
     * Set the user id
     *
     * @param user
     */
    public void setUserId(long user)
    {
        this.userId = user;
    }

    public String getUserLogin()
    {
        return userLogin;
    }

    public void setUserLogin(String userLogin)
    {
        this.userLogin = userLogin;
    }

    public User getUser()
    {
        if (userId != NONE_SPECIFIED)
        {
            return getUser(userId);
        }
        else if (TextUtils.stringSet(userLogin))
        {
            return getUser(userLogin);
        }
        return null;
    }

    protected User getUser(String userLogin)
    {
        return userManager.getUser(userLogin);
    }

    protected User getUser(long userId)
    {
        return userManager.getUser(userId);
    }

    protected void addUnknownUserActionError()
    {
        if (userId != NONE_SPECIFIED)
        {
            addActionError("Unknown user '" + userId + "'");
        }
        else if (TextUtils.stringSet(userLogin))
        {
            addActionError("Unknown user '" + userLogin + "'");
        }
    }

    protected void addUnknownUserFieldError()
    {
        if (userId != NONE_SPECIFIED)
        {
            addFieldError("userId", "Unknown user '" + userId + "'");
        }
        else if (TextUtils.stringSet(userLogin))
        {
            addFieldError("userLogin", "Unknown user '" + userLogin + "'");
        }
    }
}
