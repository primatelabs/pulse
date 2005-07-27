package com.cinnamonbob.web.user;

import com.cinnamonbob.web.BaseActionSupport;
import com.cinnamonbob.model.UserManager;
import com.cinnamonbob.model.User;
import com.cinnamonbob.model.AbstractContactPoint;

/**
 *
 *
 */
public class DeleteContactPointAction extends BaseActionSupport
{
    private long id;
    private String name;
    private UserManager userManager;

    public void setUserManager(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public long getId()
    {
        return this.id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String execute()
    {
        User user = userManager.getUser(id);
        if (user == null)
        {
            return INPUT;
        }

        AbstractContactPoint contactPoint = (AbstractContactPoint) user.getContactPoint(name);
        if (contactPoint != null)
        {
            user.remove(contactPoint);
        }
        return SUCCESS;
    }
}
