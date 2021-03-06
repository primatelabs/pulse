package com.zutubi.pulse.dev.bootstrap;

import com.zutubi.pulse.core.plugins.PluginManager;
import com.zutubi.pulse.core.spring.SpringComponentContext;

import java.io.File;

/**
 * This manager loads the defined bootstrap spring context.
 *
 * The default bootstrapContext can be overriden by using the -Dbootstrap=<bootstrapContext> option. The
 * specified bootstrap context can be either a fully qualified file name OR a classpath reference.
 */
public class DevBootstrapManager
{
    /**
     * The default bootstrapContext file.
     */
    public static final String DEFAULT_BOOTSTRAP_CONTEXT = "com/zutubi/pulse/dev/bootstrap/context/bootstrapContext.xml";

    /**
     * The bootstrap context property.
     */
    public static final String BOOTSTRAP_CONTEXT_PROPERTY = "bootstrap";

    /**
     * Load the bootstrap context, followed by all contexts passed in.
     *
     * @param contexts additional contexts to load
     */
    public static void startup(String ...contexts)
    {
        // lookup bootstrap context via the system properties.
        loadContext(System.getProperty(BOOTSTRAP_CONTEXT_PROPERTY, DEFAULT_BOOTSTRAP_CONTEXT));

        for(String context: contexts)
        {
            loadContext(context);
        }

        PluginManager pluginManager = SpringComponentContext.getBean("pluginManager");
        pluginManager.initialiseExtensions();
    }

    private static void loadContext(String contextName)
    {
        // lookup the context resource.
        // a) try the file system.
        // b) try the classpath.
        File contextFile = new File(contextName);
        if (contextFile.isFile())
        {
            SpringComponentContext.addFileContextDefinitions(contextName);
        }
        else
        {
            SpringComponentContext.addClassPathContextDefinitions(contextName);
        }
    }

    public static void shutdown()
    {
        SpringComponentContext.closeAll();
    }
}
