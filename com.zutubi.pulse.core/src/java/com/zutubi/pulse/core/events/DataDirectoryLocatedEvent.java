package com.zutubi.pulse.core.events;

import com.zutubi.events.Event;

/**
 * An event raised just after the data directory is discovered, before it is
 * used.  A brand new data directory will be initialised prior to this event,
 * but otherwise will be unused.
 */
public class DataDirectoryLocatedEvent extends Event
{
    public DataDirectoryLocatedEvent(Object source)
    {
        super(source);
    }

    public String toString()
    {
        return "Data Directory Set Event";
    }
}
