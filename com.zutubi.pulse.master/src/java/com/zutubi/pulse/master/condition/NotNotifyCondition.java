package com.zutubi.pulse.master.condition;

import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.tove.config.user.UserConfiguration;

public class NotNotifyCondition implements NotifyCondition
{
    private NotifyCondition condition;

    public NotNotifyCondition(NotifyCondition condition)
    {
        this.condition = condition;
    }

    public boolean satisfied(BuildResult result, UserConfiguration user)
    {
        return !condition.satisfied(result, user);
    }

    public NotifyCondition getCondition()
    {
        return condition;
    }
}