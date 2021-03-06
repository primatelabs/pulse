package com.zutubi.pulse.master.agent;

import com.zutubi.pulse.core.PulseExecutionContext;
import com.zutubi.pulse.core.RecipeRequest;
import com.zutubi.pulse.master.tove.config.agent.AgentConfiguration;
import com.zutubi.pulse.servercore.AgentRecipeDetails;
import com.zutubi.pulse.servercore.agent.SynchronisationMessage;
import com.zutubi.pulse.servercore.agent.SynchronisationMessageResult;
import com.zutubi.pulse.servercore.filesystem.FileInfo;

import java.io.File;
import java.util.List;

/**
 * Service for communication with agents.
 */
public interface AgentService
{
    /**
     * Returns the configuration of the agent underlying this service.
     *
     * @return the configuration of our agent
     */
    AgentConfiguration getAgentConfig();

    boolean build(RecipeRequest request);

    /**
     * Collects files produced by the recipe execution to the given local
     * directories.
     *
     * @param recipeDetails details of the recipe used to find the results
     * @param outputDest    local directory to receive the output files
     *                      (artifacts)
     */
    void collectResults(AgentRecipeDetails recipeDetails, File outputDest);

    void cleanup(AgentRecipeDetails recipeDetails);

    /**
     * Terminates the given recipe if it is still running.  This method may
     * only be called *after* receiving the recipe commenced event for the
     * recipe.
     *
     * @param recipeId the recipe to terminate
     */
    void terminateRecipe(long recipeId);

    /**
     * Synchronises the agent by processing all of the given messages.
     * Messages are converted to tasks, the tasks executed and the results
     * returned.
     *
     * @param messages messages to process
     * @return results corresponding results for each of the messages
     */
    List<SynchronisationMessageResult> synchronise(List<SynchronisationMessage> messages);

    /**
     * List the path relative to the base directory of the defined recipe.
     *
     * @param recipeDetails details used to identify the base directory
     * @param path          path relative to the base directory
     *
     * @return a list of file info instances representing the requested listing.
     */
    List<FileInfo> getFileListing(AgentRecipeDetails recipeDetails, String path);

    /**
     * Retrieve the file info for the path relative to the base directory of the
     * defined recipe.
     *
     * @param recipeDetails     the recipe details
     * @param path              the path relative to the recipes base directory
     *
     * @return a file info object for the requested path.
     */
    FileInfo getFile(AgentRecipeDetails recipeDetails, String path);

    /**
     * Executes a command on the agent.
     *
     * @param context context providing an output stream, and properties to resolve the workingDir
     *                (once the base directory property is added, which must be done on the agent)
     * @param commandLine command (and arguments) to run
     * @param workingDir if not null, directory in which to run the command, may contain properties
     *                   to be resolved using the context
     * @param timeout if non-zero, a time limit in seconds to apply to the command
     */
    void executeCommand(PulseExecutionContext context, List<String> commandLine, String workingDir, int timeout);
}
