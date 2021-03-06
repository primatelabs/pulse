package com.zutubi.pulse.master.xwork.actions.project;

import com.google.common.base.Function;
import com.zutubi.i18n.Messages;
import com.zutubi.pulse.master.MasterBuildPaths;
import com.zutubi.pulse.master.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.master.build.log.BuildLogFile;
import com.zutubi.pulse.master.build.log.LogFile;
import com.zutubi.pulse.master.build.log.RecipeLogFile;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.RecipeResultNode;
import com.zutubi.pulse.master.model.User;
import com.zutubi.pulse.master.tove.config.user.UserPreferencesConfiguration;
import com.zutubi.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Iterables.transform;

/**
 * Action to get a build or recipe log.  If a stage is specified, a recipe log
 * is returned.  Otherwise, the build log is returned.
 */
public class TailBuildLogAction extends StageActionBase
{
    private static final Messages I18N = Messages.getInstance(TailBuildLogAction.class);

    private static final int DEFAULT_MAX_LINES = 30;
    private static final int DEFAULT_REFRESH_INTERVAL = 60;

    protected boolean raw = false;
    protected boolean buildSelected = false;
    protected int maxLines = DEFAULT_MAX_LINES;
    private int tailRefreshInterval = DEFAULT_REFRESH_INTERVAL;

    protected String tail = "";
    protected boolean logExists;
    protected InputStream inputStream;
    protected Map<String, String> stages;

    protected MasterConfigurationManager configurationManager;

    public void setRaw(boolean raw)
    {
        this.raw = raw;
    }

    public void setBuildSelected(boolean buildSelected)
    {
        this.buildSelected = buildSelected;
    }

    public String getTail()
    {
        return tail;
    }

    public int getMaxLines()
    {
        return maxLines;
    }

    public int getTailRefreshInterval()
    {
        return tailRefreshInterval;
    }

    public boolean getLogExists()
    {
        return logExists;
    }

    public InputStream getInputStream()
    {
        return inputStream;
    }

    public Map<String, String> getStages()
    {
        return stages;
    }

    public String execute() throws Exception
    {
        initialiseProperties();

        BuildResult buildResult = getRequiredBuildResult();
        MasterBuildPaths paths = new MasterBuildPaths(configurationManager);
        LogFile logFile = null;
        if (StringUtils.stringSet(getStageName()))
        {
            RecipeResultNode resultNode = getRequiredRecipeResultNode();
            logFile = new RecipeLogFile(buildResult, resultNode.getResult().getId(), paths);
        }
        else if (buildSelected)
        {
            logFile = new BuildLogFile(buildResult, paths);
        }
        else
        {
            // By default, go to the first stage with a log (if any) as this is generally
            // more useful than the overall build log.
            List<RecipeResultNode> recipeResultNodes = buildResult.getStages();
            for (RecipeResultNode resultNode: recipeResultNodes)
            {
                logFile = new RecipeLogFile(buildResult, resultNode.getResult().getId(), paths);
                if (logFile.exists())
                {
                    setStageName(resultNode.getStageName());
                    break;
                }
            }

            if (logFile == null || !logFile.exists())
            {
                logFile = new BuildLogFile(buildResult, paths);
            }
        }

        Iterable<String> stageNames = transform(buildResult.getStages(), new Function<RecipeResultNode, String>()
        {
            public String apply(RecipeResultNode recipeResultNode)
            {
                return recipeResultNode.getStageName();
            }
        });

        stages = new LinkedHashMap<String, String>();
        stages.put("", I18N.format("build.log"));
        for (String stageName: stageNames)
        {
            stages.put(stageName, I18N.format("stage.log", stageName));
        }

        if (logFile.exists())
        {
            logExists = true;
            if (raw)
            {
                return getRaw(logFile);
            }
            else
            {
                return getTail(logFile);
            }
        }
        else
        {
            logExists = false;
        }

        return "tail";
    }

    protected String getRaw(LogFile logFile)
    {
        try
        {
            inputStream = logFile.openStream();
        }
        catch (IOException e)
        {
            addActionError("Unable to open recipe log: " + e.getMessage());
            return ERROR;
        }

        return "raw";
    }

    protected String getTail(LogFile logFile) throws IOException
    {
        try
        {
            this.tail = logFile.getTail(maxLines);
        }
        catch (IOException e)
        {
            addActionError("Error tailing log: " + e.getMessage());
            return ERROR;
        }

        return "tail";
    }

    protected void initialiseProperties()
    {
        Object principle = getPrinciple();
        if (principle != null)
        {
            User user = userManager.getUser((String) principle);
            if (user != null)
            {
                UserPreferencesConfiguration preferences = user.getPreferences();
                tailRefreshInterval = preferences.getTailRefreshInterval();
                maxLines = preferences.getTailLines();
            }
        }
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }
}
