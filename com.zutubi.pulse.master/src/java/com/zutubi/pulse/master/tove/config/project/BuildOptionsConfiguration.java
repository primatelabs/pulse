package com.zutubi.pulse.master.tove.config.project;

import com.zutubi.tove.annotations.ControllingCheckbox;
import com.zutubi.tove.annotations.Form;
import com.zutubi.tove.annotations.Reference;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.config.Undefined;
import com.zutubi.tove.config.api.AbstractConfiguration;
import com.zutubi.validation.annotations.Numeric;

/**
 * Generic build options that don't warrant their own category.
 */
@SymbolicName("zutubi.buildOptionsConfig")
@Form(fieldOrder = {"isolateChangelists", "maxChangesPerBuild", "timeout", "stageFailureLimit", "stageRetriesOnAgentProblem", "concurrentBuilds", "priority", "autoClearResponsibility", "idLeader", "logCompressionEnabled", "liveLogsEnabled"})
public class BuildOptionsConfiguration extends AbstractConfiguration
{
    public static final int TIMEOUT_NEVER = 0;

    @ControllingCheckbox(checkedFields = {"maxChangesPerBuild"})
    private boolean isolateChangelists = false;
    @Numeric(min = 1)
    private int maxChangesPerBuild = 1;
    @Numeric(min = 0)
    private int timeout = TIMEOUT_NEVER;
    private int stageFailureLimit = 0;
    @Numeric(min = 0)
    private int stageRetriesOnAgentProblem = 0;
    private boolean autoClearResponsibility = true;
    @Reference
    private ProjectConfiguration idLeader = null;
    private boolean logCompressionEnabled = true;
    private boolean liveLogsEnabled = true;

    private int priority = Undefined.INTEGER;

    /**
     * The number of concurrent builds that are allowed for this
     * project.
     */
    @Numeric(min = 1)
    private int concurrentBuilds = 1;

    public BuildOptionsConfiguration()
    {
        setPermanent(true);
    }

    public boolean getIsolateChangelists()
    {
        return isolateChangelists;
    }

    public void setIsolateChangelists(boolean b)
    {
        this.isolateChangelists = b;
    }

    public int getMaxChangesPerBuild()
    {
        return maxChangesPerBuild;
    }

    public void setMaxChangesPerBuild(int maxChangesPerBuild)
    {
        this.maxChangesPerBuild = maxChangesPerBuild;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public int getStageFailureLimit()
    {
        return stageFailureLimit;
    }

    public void setStageFailureLimit(int stageFailureLimit)
    {
        this.stageFailureLimit = stageFailureLimit;
    }

    public int getStageRetriesOnAgentProblem()
    {
        return stageRetriesOnAgentProblem;
    }

    public void setStageRetriesOnAgentProblem(int stageRetriesOnAgentProblem)
    {
        this.stageRetriesOnAgentProblem = stageRetriesOnAgentProblem;
    }

    public boolean isAutoClearResponsibility()
    {
        return autoClearResponsibility;
    }

    public void setAutoClearResponsibility(boolean autoClearResponsibility)
    {
        this.autoClearResponsibility = autoClearResponsibility;
    }

    public ProjectConfiguration getIdLeader()
    {
        return idLeader;
    }

    public void setIdLeader(ProjectConfiguration idLeader)
    {
        this.idLeader = idLeader;
    }

    public boolean isLogCompressionEnabled()
    {
        return logCompressionEnabled;
    }

    public void setLogCompressionEnabled(boolean logCompressionEnabled)
    {
        this.logCompressionEnabled = logCompressionEnabled;
    }

    public boolean hasPriority()
    {
        return priority != Undefined.INTEGER;
    }

    public int getPriority()
    {
        return priority;
    }

    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    public void setConcurrentBuilds(int concurrentBuilds)
    {
        this.concurrentBuilds = concurrentBuilds;
    }

    public int getConcurrentBuilds()
    {
        return concurrentBuilds;
    }

    public boolean isLiveLogsEnabled()
    {
        return liveLogsEnabled;
    }

    public void setLiveLogsEnabled(boolean liveLogsEnabled)
    {
        this.liveLogsEnabled = liveLogsEnabled;
    }
}
