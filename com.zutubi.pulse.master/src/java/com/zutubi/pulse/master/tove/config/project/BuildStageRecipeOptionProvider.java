package com.zutubi.pulse.master.tove.config.project;

import com.zutubi.pulse.core.FileResolver;
import com.zutubi.pulse.core.PulseFileLoader;
import com.zutubi.pulse.core.PulseFileLoaderFactory;
import com.zutubi.pulse.core.RelativeFileResolver;
import com.zutubi.pulse.core.engine.PulseFileSource;
import com.zutubi.pulse.core.scm.api.Revision;
import com.zutubi.pulse.master.scm.ScmFileResolver;
import com.zutubi.pulse.master.scm.ScmManager;
import com.zutubi.pulse.master.tove.handler.ListOptionProvider;
import com.zutubi.tove.config.ConfigurationProvider;
import com.zutubi.tove.config.api.Configuration;
import com.zutubi.tove.type.TypeProperty;
import com.zutubi.util.ConcurrentUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * An option provider for the recipe field of a build stage.  Designed to be
 * used lazily as it could take a long time to load the Pulse file from the
 * SCM.  This may even time out.
 */
public class BuildStageRecipeOptionProvider extends ListOptionProvider
{
    private static final int TIMEOUT_SECONDS = 30;
    
    private ConfigurationProvider configurationProvider;
    private PulseFileLoaderFactory fileLoaderFactory;
    private ScmManager scmManager;

    public String getEmptyOption(Object instance, String parentPath, TypeProperty property)
    {
        return null;
    }

    public List<String> getOptions(Object instance, final String parentPath, TypeProperty property)
    {
        List<String> recipes = new LinkedList<String>();
        recipes.add("");

        List<String> recipeNames;
        try
        {
            recipeNames = ConcurrentUtils.runWithTimeout(new Callable<List<String>>()
            {
                public List<String> call() throws Exception
                {
                    Configuration stages = configurationProvider.get(parentPath, Configuration.class);
                    ProjectConfiguration projectConfig = configurationProvider.getAncestorOfType(stages, ProjectConfiguration.class);
                    if (projectConfig != null)
                    {
                        PulseFileSource pulseFileSource = projectConfig.getType().getPulseFile(projectConfig, Revision.HEAD, null);
                        PulseFileLoader pulseFileLoader = fileLoaderFactory.createLoader();
                        FileResolver fileResolver = new RelativeFileResolver(pulseFileSource.getPath(), new ScmFileResolver(projectConfig, Revision.HEAD, scmManager));
                        return pulseFileLoader.loadAvailableRecipes(pulseFileSource.getFileContent(), fileResolver);
                    }

                    return Collections.emptyList();
                }
            }, TIMEOUT_SECONDS, TimeUnit.SECONDS, null);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Unable to load Pulse file to load available recipes: " + e.getMessage(), e);
        }

        if (recipeNames == null)
        {
            throw new RuntimeException("Timed out listing recipes in Pulse file.");
        }

        for (String recipe: recipeNames)
        {
            recipes.add(recipe);
        }

        return recipes;
    }

    public void setConfigurationProvider(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    public void setFileLoaderFactory(PulseFileLoaderFactory fileLoaderFactory)
    {
        this.fileLoaderFactory = fileLoaderFactory;
    }

    public void setScmManager(ScmManager scmManager)
    {
        this.scmManager = scmManager;
    }
}