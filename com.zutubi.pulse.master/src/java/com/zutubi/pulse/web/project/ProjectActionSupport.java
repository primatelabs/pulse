package com.zutubi.pulse.web.project;

import com.zutubi.pulse.master.model.*;
import com.zutubi.pulse.scheduling.Scheduler;
import com.zutubi.pulse.web.ActionSupport;
import com.zutubi.util.TextUtils;
import org.acegisecurity.AccessDeniedException;

/**
 * 
 *
 */
public class ProjectActionSupport extends ActionSupport
{
    protected BuildManager buildManager;
    protected ScmManager scmManager;
    protected Scheduler scheduler;

    private static final long NONE_SPECIFIED = -1;

    protected long projectId = NONE_SPECIFIED;
    protected String projectName = null;

    public BuildManager getBuildManager()
    {
        return buildManager;
    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }

    public void setScmManager(ScmManager scmManager)
    {
        this.scmManager = scmManager;
    }

    public ScmManager getScmManager()
    {
        return scmManager;
    }

    public Scheduler getScheduler()
    {
        return this.scheduler;
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }


    public long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(long projectId)
    {
        this.projectId = projectId;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public Project getProject()
    {
        if (projectId != NONE_SPECIFIED)
        {
            return getProject(projectId);
        }
        else if (TextUtils.stringSet(projectName))
        {
            return getProject(projectName);
        }
        return null;
    }

    protected Project getProject(long id)
    {
        return projectManager.getProject(id, false);
    }

    protected Project getProject(String projectName)
    {
        return getProjectManager().getProject(projectName, false);
    }

    public void addUnknownProjectActionError()
    {
        if (projectId != NONE_SPECIFIED)
        {
            addActionError("Unknown project [" + projectId + "]");
        }
        else if (TextUtils.stringSet(projectName))
        {
            addActionError("Unknown project [" + projectName + "]");
        }
        else
        {
            addActionError("Require either a project name or id.");
        }
    }

    public void addUnknownProjectFieldError()
    {
        if (projectId != NONE_SPECIFIED)
        {
            addFieldError("projectId", "Unknown project [" + projectId + "]");
        }
        else if (TextUtils.stringSet(projectName))
        {
            addFieldError("projectName", "Unknown project [" + projectName + "]");
        }
        else
        {
            addActionError("Require either a project name or id.");
        }
    }

    public Project lookupProject(long id)
    {
        Project p = projectManager.getProject(id, false);
        if(p == null)
        {
            addActionError("Unknown project [" + id + "]");
        }

        return p;
    }

    public void checkPermissions(BuildResult result)
    {
        if(result.isPersonal())
        {
            User user = getLoggedInUser();
            if(!result.getUser().equals(user))
            {
                throw new AccessDeniedException("Only the owner can view a personal build");
            }
        }
    }
}
