package com.zutubi.pulse.master.xwork.actions.project;

import com.zutubi.i18n.Messages;
import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.core.model.PersistentChangelist;
import com.zutubi.pulse.master.model.BuildColumns;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.pulse.master.model.User;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfigurationActions;
import com.zutubi.pulse.master.tove.config.user.UserPreferencesConfiguration;
import com.zutubi.pulse.master.tove.model.ActionLink;
import com.zutubi.pulse.master.tove.webwork.ToveUtils;
import com.zutubi.pulse.servercore.bootstrap.SystemPaths;
import com.zutubi.tove.actions.ActionManager;
import com.zutubi.tove.actions.ConfigurationAction;
import com.zutubi.tove.actions.ConfigurationActions;
import com.zutubi.tove.security.AccessManager;
import com.zutubi.tove.type.TypeRegistry;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Action to display project home page - the latest project status.
 */
public class ProjectHomeAction extends ProjectActionBase
{
    private int totalBuilds;
    private int successfulBuilds;
    private int failedBuilds;
    private boolean paused;
    private boolean pausable;
    private boolean resumable;
    private BuildResult currentBuild;
    private List<PersistentChangelist> latestChanges;
    private List<BuildResult> recentBuilds;
    private BuildColumns summaryColumns;
    private BuildColumns recentColumns;
    private List<ActionLink> actions = new LinkedList<ActionLink>();

    private ActionManager actionManager;
    private SystemPaths systemPaths;
    private TypeRegistry typeRegistry;

    public int getTotalBuilds()
    {
        return totalBuilds;
    }

    public int getSuccessfulBuilds()
    {
        return successfulBuilds;
    }

    public int getFailedBuilds()
    {
        return failedBuilds;
    }

    public int getErrorBuilds()
    {
        return totalBuilds - successfulBuilds - failedBuilds;
    }

    public boolean isPaused()
    {
        return paused;
    }

    public boolean isPausable()
    {
        return pausable;
    }

    public boolean isResumable()
    {
        return resumable;
    }

    public int getPercent(int quotient, int divisor)
    {
        if (divisor > 0)
        {
            return (int) Math.round(quotient * 100.0 / divisor);
        }
        else
        {
            return 0;
        }
    }

    public int getPercentSuccessful()
    {
        return getPercent(successfulBuilds, totalBuilds);
    }

    public int getPercentSuccessNoErrors()
    {
        return getPercent(successfulBuilds, totalBuilds - getErrorBuilds());
    }

    public int getPercentFailed()
    {
        return getPercent(failedBuilds, totalBuilds);
    }

    public int getPercentError()
    {
        return getPercent(getErrorBuilds(), totalBuilds);
    }

    public boolean getProjectNotBuilding()
    {
        Project project = getRequiredProject();
        return project.getState() == Project.State.PAUSED || project.getState() == Project.State.IDLE;
    }

    public BuildResult getCurrentBuild()
    {
        return currentBuild;
    }

    public List<PersistentChangelist> getLatestChanges()
    {
        return latestChanges;
    }

    public List<BuildResult> getRecentBuilds()
    {
        return recentBuilds;
    }

    public BuildColumns getSummaryColumns()
    {
        return summaryColumns;
    }

    public BuildColumns getRecentColumns()
    {
        return recentColumns;
    }

    public List<ActionLink> getActions()
    {
        return actions;
    }

    public String execute()
    {
        Project project = getRequiredProject();
        
        paused = project.getState() == Project.State.PAUSED;
        pausable = project.isTransitionValid(Project.Transition.PAUSE);
        resumable = project.isTransitionValid(Project.Transition.RESUME);

        totalBuilds = buildManager.getBuildCount(project, ResultState.getCompletedStates());
        successfulBuilds = buildManager.getBuildCount(project, new ResultState[]{ResultState.SUCCESS});
        failedBuilds = buildManager.getBuildCount(project, new ResultState[]{ResultState.FAILURE});
        currentBuild = buildManager.getLatestBuildResult(project);
        latestChanges = buildManager.getLatestChangesForProject(project, 10);
        recentBuilds = buildManager.getLatestBuildResultsForProject(project, 11);
        if(!recentBuilds.isEmpty())
        {
            recentBuilds.remove(0);
        }

        User user = getLoggedInUser();
        summaryColumns = new BuildColumns(user == null ? UserPreferencesConfiguration.defaultProjectColumns() : user.getPreferences().getProjectSummaryColumns(), projectManager);
        recentColumns = new BuildColumns(user == null ? UserPreferencesConfiguration.defaultProjectColumns() : user.getPreferences().getProjectRecentColumns(), projectManager);

        File contentRoot = systemPaths.getContentRoot();
        ConfigurationActions configurationActions = actionManager.getConfigurationActions(typeRegistry.getType(ProjectConfiguration.class));
        for (String candidateAction: Arrays.asList(AccessManager.ACTION_WRITE, ProjectConfigurationActions.ACTION_MARK_CLEAN, ProjectConfigurationActions.ACTION_TRIGGER))
        {
            String permission = candidateAction;
            ConfigurationAction configurationAction = configurationActions.getAction(candidateAction);
            if (configurationAction != null)
            {
                permission = configurationAction.getPermissionName();
            }
            
            if (accessManager.hasPermission(permission,  project))
            {
                actions.add(ToveUtils.getActionLink(candidateAction, Messages.getInstance(ProjectConfiguration.class), contentRoot));
            }
        }

        return SUCCESS;
    }

    public void setActionManager(ActionManager actionManager)
    {
        this.actionManager = actionManager;
    }

    public void setSystemPaths(SystemPaths systemPaths)
    {
        this.systemPaths = systemPaths;
    }

    public void setTypeRegistry(TypeRegistry typeRegistry)
    {
        this.typeRegistry = typeRegistry;
    }
}
