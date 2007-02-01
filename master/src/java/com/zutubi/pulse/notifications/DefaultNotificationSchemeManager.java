package com.zutubi.pulse.notifications;

import java.util.*;

/**
 * <class-comment/>
 */
public class DefaultNotificationSchemeManager implements NotificationSchemeManager
{
    // Use a tree map to order by key
    private Map<String, Class<? extends NotificationHandler>> handlers = new TreeMap<String, Class<? extends NotificationHandler>>();

    public DefaultNotificationSchemeManager()
    {
    }

    public void init()
    {
        // initialise the default handlers
        handlers.put("email", EmailNotificationHandler.class);
        handlers.put("jabber", JabberNotificationHandler.class);
    }

    public List<String> getNotificationSchemes()
    {
        return new LinkedList(handlers.keySet());
    }

    public Class<? extends NotificationHandler> getNotificationHandler(String scheme)
    {
        return handlers.get(scheme);
    }
}
