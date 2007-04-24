package com.zutubi.pulse.core.config;

import com.zutubi.config.annotations.ID;

/**
 * Simple interface for the common case of named configuration objects.
 */
public interface NamedConfiguration extends Configuration
{
    @ID
    String getName();
    void setName(String name);
}
