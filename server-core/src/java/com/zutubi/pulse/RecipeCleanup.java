package com.zutubi.pulse;

import com.zutubi.pulse.events.EventManager;
import com.zutubi.pulse.events.build.RecipeStatusEvent;
import com.zutubi.pulse.util.FileSystem;
import com.zutubi.pulse.util.logging.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Class to remove stale recipe files from recipe directory.
 */
public class RecipeCleanup
{
    private static final Logger LOG = Logger.getLogger(RecipeCleanup.class);

    static final String DELETING_DIRECTORY = "Found stale recipe directory '%s', deleting...";
    static final String DELETING_FILE = "Found unexpected file '%s', deleting...";
    static final String DELETED = "Deleted.";
    static final String UNABLE_TO_DELETE_FILE = "Unable to delete file '%s'";

    private FileSystem fileSystem;

    public RecipeCleanup(FileSystem fileSystem)
    {
        this.fileSystem = fileSystem;
    }

    public void cleanup(EventManager eventManager, File directoryToClean, long recipeId)
    {
        File[] files = directoryToClean.listFiles();
        if (files == null)
        {
            LOG.warning("unable to list contents of recipe directory '" + directoryToClean.getAbsolutePath() + "'");
            return;
        }

        for (File node : directoryToClean.listFiles())
        {
            String statusMessage;
            if (node.isDirectory())
            {
                statusMessage = DELETING_DIRECTORY;
            }
            else
            {
                statusMessage = DELETING_FILE;
            }
            eventManager.publish(new RecipeStatusEvent(this, recipeId, String.format(statusMessage, node.getName())));
            try
            {
                fileSystem.delete(node);
                eventManager.publish(new RecipeStatusEvent(this, recipeId, DELETED));
            }
            catch (IOException e)
            {
                eventManager.publish(new RecipeStatusEvent(this, recipeId, String.format(UNABLE_TO_DELETE_FILE, node.getName())));
            }
        }
    }
}

