package com.zutubi.pulse;

import com.zutubi.pulse.bootstrap.ConfigurationManager;
import com.zutubi.pulse.core.RecipeProcessor;
import com.zutubi.pulse.core.Stoppable;
import com.zutubi.pulse.core.RecipeRequest;
import com.zutubi.pulse.core.ResourceRepository;
import com.zutubi.pulse.events.EventManager;
import com.zutubi.pulse.util.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class MasterRecipeProcessor implements Stoppable
{
    private static final Logger LOG = Logger.getLogger(MasterRecipeProcessor.class);

    private ExecutorService executor;
    private RecipeProcessor recipeProcessor;
    private ConfigurationManager configurationManager;
    private EventManager eventManager;
    private ResourceRepository resourceRepository;

    public MasterRecipeProcessor()
    {
        executor = Executors.newSingleThreadExecutor();
    }

    public void processRecipe(RecipeRequest request)
    {
        executor.execute(new MasterRecipeRunner(request, recipeProcessor, eventManager, configurationManager, resourceRepository));
    }

    public void setRecipeProcessor(RecipeProcessor recipeProcessor)
    {
        this.recipeProcessor = recipeProcessor;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void setEventManager(EventManager eventManager)
    {
        this.eventManager = eventManager;
    }

    public void terminateRecipe(long id)
    {
        try
        {
            recipeProcessor.terminateRecipe(id);
        }
        catch (InterruptedException e)
        {
            LOG.warning("Interrupted while terminating recipe", e);
        }
    }

    public void stop(boolean force)
    {
        // We do not take responsibility for shutting down the running
        // recipe, that is controlled at a higher level
        executor.shutdownNow();
    }

    public void setResourceRepository(ResourceRepository resourceRepository)
    {
        this.resourceRepository = resourceRepository;
    }
}
