package com.zutubi.prototype.config.types;

import com.zutubi.config.annotations.SymbolicName;
import com.zutubi.pulse.core.config.AbstractConfiguration;

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
