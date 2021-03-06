package com.zutubi.tove.config;

import com.zutubi.tove.config.events.ConfigurationEvent;

/**
 * An event listener that listens for configuration events.  This is the
 * preferred way to be notified of configuration changes, as it is a
 * simplification of the more general event handling mechanism.  Where more
 * flexibility is required, implement
 * {@link com.zutubi.events.EventListener}.
 */
public interface ConfigurationEventListener
{
    public void handleConfigurationEvent(ConfigurationEvent event);
}
