package com.zutubi.tove.config.types;

import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.config.api.AbstractConfiguration;

/**
 */
@SymbolicName("Circular")
public class CircularObject extends AbstractConfiguration
{
    private CircularObject nested;

    public CircularObject getNested()
    {
        return nested;
    }

    public void setNested(CircularObject nested)
    {
        this.nested = nested;
    }
}
