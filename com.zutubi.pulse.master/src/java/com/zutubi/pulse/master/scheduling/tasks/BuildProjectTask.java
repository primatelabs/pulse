package com.zutubi.pulse.master.scheduling.tasks;

import com.zutubi.pulse.core.resources.api.ResourcePropertyConfiguration;
import com.zutubi.pulse.core.scm.api.Revision;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.pulse.master.model.ProjectManager;
import com.zutubi.pulse.master.model.TriggerBuildReason;
import com.zutubi.pulse.master.model.TriggerOptions;
import com.zutubi.pulse.master.scheduling.Task;
import com.zutubi.pulse.master.scheduling.TaskExecutionContext;
import com.zutubi.pulse.master.scheduling.Trigger;
import com.zutubi.pulse.master.tove.config.project.triggers.TriggerConditionConfiguration;
import com.zutubi.pulse.master.tove.config.project.triggers.TriggerConfiguration;
import com.zutubi.pulse.master.trigger.TriggerCondition;
import com.zutubi.pulse.master.trigger.TriggerConditionFactory;
import com.zutubi.util.logging.Logger;

import java.util.Collection;
import java.util.Collections;

/**
 * A trigger task which triggers a project build.
 */
public class BuildProjectTask implements Task
{
    /**
     * The fixed revision to be built, if not present the revision will float
     * to the latest (Revision, optional).
     */
    public static final String PARAM_REVISION  = "revision";
    /**
     * Indicates if the raised build request should be replaceable by later
     * requests from the same source while queued (Boolean, optional, defaults
     * to true).
     */
    public static final String PARAM_REPLACEABLE = "replaceable";

    /**
     * The status to be used for the build.
     */
    public static final String PARAM_STATUS = "status";

    /**
     * The version to be used for the build.
     */
    public static final String PARAM_VERSION = "version";
    /**
     * Indicates if the associated version has been propagated from
     * a previous build.
     */
    public static final String PARAM_VERSION_PROPAGATED = "version.propagated";

    /**
     * Indicates whether or not the generated build request can jump its queue.
     */
    public static final String PARAM_JUMP_QUEUE_ALLOWED = "jumpQueue";

    /**
     * The source of the build trigger.  Defaults to the trigger name if non is specified.
     */
    public static final String PARAM_SOURCE = "source";

    private static final Logger LOG = Logger.getLogger(BuildProjectTask.class);

    private ProjectManager projectManager;
    private TriggerConditionFactory triggerConditionFactory;

    public void execute(TaskExecutionContext context)
    {
        Trigger trigger = context.getTrigger();
        TriggerConfiguration triggerConfig = trigger.getConfig();
        Project project = projectManager.getProject(trigger.getProjectId(), true);
        if (project == null)
        {
            LOG.warning("Build project task fired for unknown project '" + trigger.getProjectId() + "' (trigger '" + trigger.getName() + "')");
            return;
        }

        if (!conditionsSatisfied(triggerConfig, project))
        {
            return;
        }

        Revision revision = (Revision) context.get(PARAM_REVISION);

        Boolean replaceableValue = (Boolean) context.get(PARAM_REPLACEABLE);
        boolean replaceable = replaceableValue == null || replaceableValue;
        String status = (String) context.get(PARAM_STATUS);

        Boolean versionPropagatedValue = (Boolean) context.get(PARAM_VERSION_PROPAGATED);
        boolean versionPropagated = versionPropagatedValue != null && versionPropagatedValue;

        String version = (String) context.get(PARAM_VERSION);

        Boolean jumpQueueValue = (Boolean) context.get(PARAM_JUMP_QUEUE_ALLOWED);
        boolean jumpQueue = jumpQueueValue == null || jumpQueueValue;

        Collection<ResourcePropertyConfiguration> properties;
        if (triggerConfig == null)
        {
            properties = Collections.emptyList();
        }
        else
        {
            properties = triggerConfig.getProperties().values();
        }

        // generate build request.
        TriggerOptions options = new TriggerOptions(new TriggerBuildReason(trigger.getName()), getSource(context));
        options.setReplaceable(replaceable);
        options.setForce(false);
        options.setProperties(properties);
        options.setStatus(status);
        options.setResolveVersion(!versionPropagated);
        options.setVersion(version);
        options.setJumpQueueAllowed(jumpQueue);
        projectManager.triggerBuild(project.getConfig(), options, revision);
    }

    private boolean conditionsSatisfied(TriggerConfiguration triggerConfig, Project project)
    {
        for (TriggerConditionConfiguration conditionConfig: triggerConfig.getConditions())
        {
            TriggerCondition condition = triggerConditionFactory.create(conditionConfig);
            if (!condition.satisfied(project))
            {
                return false;
            }
        }

        return true;
    }

    private static String getSource(TaskExecutionContext context)
    {
        String source = (String) context.get(PARAM_SOURCE);
        if (source != null)
        {
            return source;
        }
        return "trigger '" + context.getTrigger().getName() + "'";
    }

    public void setProjectManager(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    public void setTriggerConditionFactory(TriggerConditionFactory triggerConditionFactory)
    {
        this.triggerConditionFactory = triggerConditionFactory;
    }
}
