package com.zutubi.pulse.master.tove.config.project.hooks;

import com.zutubi.events.Event;
import com.zutubi.pulse.core.PulseExecutionContext;
import com.zutubi.pulse.core.engine.api.BuildProperties;
import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.engine.api.Feature;
import com.zutubi.pulse.core.model.Result;
import com.zutubi.pulse.core.spring.SpringComponentContext;
import com.zutubi.pulse.master.MasterBuildPaths;
import com.zutubi.pulse.master.MasterBuildProperties;
import com.zutubi.pulse.master.agent.MasterLocationProvider;
import com.zutubi.pulse.master.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.master.build.log.*;
import com.zutubi.pulse.master.events.build.BuildEvent;
import com.zutubi.pulse.master.events.build.StageEvent;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.pulse.master.model.RecipeResultNode;
import com.zutubi.pulse.master.model.persistence.hibernate.HibernateBuildResultDao;
import com.zutubi.pulse.master.security.SecurityUtils;
import com.zutubi.pulse.master.tove.config.admin.GlobalConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfigurationActions;
import com.zutubi.tove.config.ConfigurationProvider;
import com.zutubi.tove.security.AccessManager;
import com.zutubi.util.UnaryProcedure;
import com.zutubi.util.io.IOUtils;
import com.zutubi.util.logging.Logger;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Manages the execution of build hooks at various points in the build
 * process.
 */
public class BuildHookManager
{
    private static final Logger LOG = Logger.getLogger(BuildHookManager.class);

    private static final String PROPERTY_TRIGGER_USER = "hook.trigger.user";

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private AccessManager accessManager;
    private MasterConfigurationManager configurationManager;
    private MasterLocationProvider     masterLocationProvider;
    private ConfigurationProvider configurationProvider;

    public void handleEvent(Event event, HookLogger logger)
    {
        final BuildEvent be = (BuildEvent) event;
        BuildResult buildResult = be.getBuildResult();

        PulseExecutionContext context = new PulseExecutionContext(be.getContext());
        Project project = buildResult.getProject();
        for (BuildHookConfiguration hook : project.getConfig().getBuildHooks().values())
        {
            if (hook.enabled() && hook.triggeredBy(be))
            {
                RecipeResultNode resultNode = null;
                if (be instanceof StageEvent)
                {
                    resultNode = ((StageEvent) be).getStageNode();
                }

                logAndExecuteTask(hook, context, logger, buildResult, resultNode, false);
            }
        }
    }

    public void manualTrigger(final BuildHookConfiguration hook, final BuildResult result)
    {
        if (hook.canManuallyTriggerFor(result))
        {
            accessManager.ensurePermission(ProjectConfigurationActions.ACTION_TRIGGER_HOOK, result);
            final String username = SecurityUtils.getLoggedInUsername();

            HibernateBuildResultDao.initialise(result);
            executor.execute(new Runnable()
            {
                public void run()
                {
                    final PulseExecutionContext context = new PulseExecutionContext();
                    MasterBuildProperties.addAllBuildProperties(context, result, masterLocationProvider, configurationManager, configurationProvider.get(GlobalConfiguration.class).getBaseUrl());
                    context.addString(BuildProperties.NAMESPACE_INTERNAL, PROPERTY_TRIGGER_USER, username);

                    if (hook.appliesTo(result))
                    {
                        HookLogger buildLogger = createBuildLogger(result);
                        try
                        {
                            logAndExecuteTask(hook, context, buildLogger, result, null, true);
                        }
                        finally
                        {
                            IOUtils.close(buildLogger);
                        }
                    }

                    result.forEachNode(new UnaryProcedure<RecipeResultNode>()
                    {
                        public void run(RecipeResultNode recipeResultNode)
                        {
                            if (hook.appliesTo(recipeResultNode))
                            {
                                context.push();
                                HookLogger recipeLogger = createRecipeLogger(result, recipeResultNode);
                                try
                                {
                                    MasterBuildProperties.addStageProperties(context, result, recipeResultNode, configurationManager, false);
                                    MasterBuildProperties.addCompletedStageProperties(context, result, recipeResultNode, configurationManager, false);
                                    logAndExecuteTask(hook, context, recipeLogger, result, recipeResultNode, true);
                                }
                                finally
                                {
                                    IOUtils.close(recipeLogger);
                                    context.pop();
                                }
                            }
                        }
                    });
                }
            });
        }
    }

    private DefaultBuildLogger createBuildLogger(BuildResult result)
    {
        MasterBuildPaths paths = new MasterBuildPaths(configurationManager);
        DefaultBuildLogger logger = new DefaultBuildLogger(new BuildLogFile(result, paths));
        logger.prepare();
        return logger;
    }

    private DefaultRecipeLogger createRecipeLogger(BuildResult buildResult, RecipeResultNode recipeResultNode)
    {
        MasterBuildPaths paths = new MasterBuildPaths(configurationManager);
        DefaultRecipeLogger logger = new DefaultRecipeLogger(new RecipeLogFile(buildResult, recipeResultNode.getResult().getId(), paths), true);
        logger.prepare();
        return logger;
    }

    private void logAndExecuteTask(BuildHookConfiguration hook, PulseExecutionContext context, HookLogger logger, BuildResult buildResult, RecipeResultNode resultNode, boolean manual)
    {
        logger.hookCommenced(hook.getName());
        OutputStream out = null;
        try
        {
            // stream the output to whoever is listening.
            out = new OutputLoggerOutputStream(logger);
            context.setOutputStream(out);
            executeTask(hook, context, buildResult, resultNode, manual);
        }
        finally
        {
            IOUtils.close(out);
        }
        logger.hookCompleted(hook.getName());
    }

    private void executeTask(BuildHookConfiguration hook, ExecutionContext context, BuildResult buildResult, RecipeResultNode resultNode, boolean manual)
    {
        BuildHookTaskConfiguration task = hook.getTask();
        if (task != null)
        {
            try
            {
                SpringComponentContext.autowire(task);
                task.execute(context, buildResult, resultNode, hook.runsOnAgent());
            }
            catch (Exception e)
            {
                String message = "Error executing task for hook '" + hook.getName() + "': " + e.getMessage();
                if (manual)
                {
                    OutputStream outputStream = context.getOutputStream();
                    if (outputStream != null)
                    {
                        PrintWriter writer = new PrintWriter(outputStream, true);
                        writer.println(message);
                    }
                }
                else
                {
                    Result result = resultNode == null ? buildResult : resultNode.getResult();
                    result.addFeature(Feature.Level.ERROR, message);
                    if (hook.failOnError())
                    {
                        result.error();
                    }
                }

                LOG.info(message, e);
            }
        }
    }

    public void setAccessManager(AccessManager accessManager)
    {
        this.accessManager = accessManager;
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void setMasterLocationProvider(MasterLocationProvider masterLocationProvider)
    {
        this.masterLocationProvider = masterLocationProvider;
    }

    public void setThreadFactory(final ThreadFactory threadFactory)
    {
        executor = Executors.newSingleThreadExecutor(new ThreadFactory()
        {
            public Thread newThread(Runnable r)
            {
                Thread thread = threadFactory.newThread(r);
                thread.setName("Build Hook Executor");
                return thread;
            }
        });
    }

    public void setConfigurationProvider(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }
}
