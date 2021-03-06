package com.zutubi.pulse.master.bootstrap.tasks;

import com.zutubi.pulse.master.webwork.views.freemarker.CustomFreemarkerManager;
import com.zutubi.pulse.servercore.bootstrap.StartupTask;

/**
 * Sets up Freemarker logging during the startup process.
 */
public class FreemarkerLoggingStartupTask implements StartupTask
{
    public void execute()
    {
        CustomFreemarkerManager.initialiseLogging();
    }

    public boolean haltOnFailure()
    {
        return true;
    }
}
