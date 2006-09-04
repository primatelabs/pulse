package com.zutubi.pulse.events.build;

import com.zutubi.pulse.core.BuildRevision;
import com.zutubi.pulse.core.model.Entity;
import com.zutubi.pulse.model.*;

/**
 */
public class BuildRequestEvent extends AbstractBuildRequestEvent
{
    private BuildReason reason;

    public BuildRequestEvent(Object source, BuildReason reason, Project project, String specification, BuildRevision revision)
    {
        super(source, revision, project, specification);
        this.reason = reason;
    }

    public Entity getOwner()
    {
        return getProject();
    }

    public boolean isPersonal()
    {
        return false;
    }

    public BuildReason getReason()
    {
        return reason;
    }

    public BuildResult createResult(ProjectManager projectManager, UserManager userManager)
    {
        return new BuildResult(reason, getProject(), getSpecification(), projectManager.getNextBuildNumber(getProject()));
    }
}
