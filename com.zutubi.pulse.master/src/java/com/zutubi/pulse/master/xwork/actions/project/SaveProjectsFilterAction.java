package com.zutubi.pulse.master.xwork.actions.project;

import com.google.common.base.Objects;
import com.zutubi.pulse.master.model.User;
import com.zutubi.pulse.master.xwork.actions.ActionSupport;

/**
 * An action to save the value of the filter field on the dashboard/browse views.
 */
public class SaveProjectsFilterAction extends ActionSupport
{
    private String filter;
    private boolean dashboard;

    public void setFilter(String filter)
    {
        this.filter = filter;
    }

    public void setDashboard(boolean dashboard)
    {
        this.dashboard = dashboard;
    }

    @Override
    public String execute() throws Exception
    {
        Object principle = getPrinciple();
        if (principle != null)
        {
            User user = userManager.getUser((String) principle);
            if (user != null)
            {
                boolean changed = false;
                if (dashboard)
                {
                    if (!Objects.equal(user.getDashboardFilter(), filter))
                    {
                        user.setDashboardFilter(filter);
                        changed = true;
                    }
                }
                else if (!Objects.equal(user.getBrowseViewFilter(), filter))
                {
                    user.setBrowseViewFilter(filter);
                    changed = true;
                }

                if (changed)
                {
                    userManager.save(user);
                }
            }
        }

        return SUCCESS;
    }
}
