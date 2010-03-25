package com.zutubi.pulse.servercore.services;

import com.zutubi.pulse.core.RecipeRequest;
import com.zutubi.pulse.core.config.ResourceConfiguration;
import com.zutubi.pulse.servercore.AgentRecipeDetails;
import com.zutubi.pulse.servercore.SystemInfo;
import com.zutubi.pulse.servercore.filesystem.FileInfo;
import com.zutubi.pulse.servercore.util.logging.CustomLogRecord;

import java.util.List;

/**
 */
public interface SlaveService
{
    /**
     * Most primitive communication, do *not* change the signature of this
     * method.
     *
     * @return the build number of the slave (we will only continue to talk
     *         if the build number matches ours)
     */
    int ping();

    /**
     * The update mechanism needs to be stable.  Any changes to the way this
     * works requires knowledge in new code (master and slave side) that
     * knows to veto impossible updates.
     *
     * @param token      secure token for inter-agent communication
     * @param build      the build number to update to
     * @param master     url of the master requesting the update
     * @param hostId     the slave host's id, for when it calls us back
     * @param packageUrl URL from which a zip containing the given build can
     *                   be obtained
     * @param packageSize size, in bytes, of the package
     * @return true if the agent wishes to proceed with the update
     */
    boolean updateVersion(String token, String build, String master, long hostId, String packageUrl, long packageSize);

    HostStatus getStatus(String token, String master);

    /**
     * A request to build a recipe on the slave, if the slave is currently idle.
     *
     * @param token   secure token for inter-agent communication
     * @param master  location of the master for return messages
     * @param agentHandle  handle of the agent, used in returned messages
     * @param request details of the recipe to build
     * @return true if the request was accepted, false of the slave was busy
     *
     * @throws InvalidTokenException if the given token does not match the
     * slave's
     */
    boolean build(String token, String master, long agentHandle, RecipeRequest request) throws InvalidTokenException;

    void cleanupRecipe(String token, AgentRecipeDetails recipeDetails) throws InvalidTokenException;

    void terminateRecipe(String token, long agentHandle, long recipeId) throws InvalidTokenException;

    SystemInfo getSystemInfo(String token) throws InvalidTokenException;

    List<CustomLogRecord> getRecentMessages(String token) throws InvalidTokenException;

    List<ResourceConfiguration> discoverResources(String token);

    void garbageCollect();

    /**
     * List the path relative to the base directory of the defined recipe.
     *
     * @param token     secure token for inter-agent communication
     * @param details   used to identify the base directory
     * @param path      path relative to the base directory
     *
     * @return a list of file info instances representing the requested listing.
     */
    List<FileInfo> getFileInfos(String token, AgentRecipeDetails details, String path);

    /**
     * Retrieve the file info for the path relative to the base directory of the
     * defined recipe.
     *
     * @param token     secure token for inter-agent communication
     * @param details   the recipe details
     * @param path      the path relative to the recipes base directory
     *
     * @return a file info object for the requested path.
     */
    FileInfo getFileInfo(String token, AgentRecipeDetails details, String path);
}
