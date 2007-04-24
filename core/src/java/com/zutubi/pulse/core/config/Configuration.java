package com.zutubi.pulse.core.config;

import com.zutubi.config.annotations.Transient;

/**
 * Basic interface that must be implemented by all configuration types.
 */
public interface Configuration
{
    @Transient
    long getID();
    void setID(long id);
}
