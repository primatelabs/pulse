package com.zutubi.pulse.model;

import com.zutubi.pulse.core.model.Resource;
import com.zutubi.pulse.core.model.ResourceVersion;
import com.zutubi.pulse.core.model.Property;
import com.zutubi.pulse.core.FileLoadException;

/**
 */
public class PersistentResource extends Resource
{
    /**
     * The slave that owns this resource, or null if it is owned by the master.
     */
    private Slave slave;

    public PersistentResource()
    {
    }

    public PersistentResource(String name)
    {
        super(name);
    }

    public PersistentResource(String name, Slave slave)
    {
        super(name);
        this.slave = slave;
    }

    public PersistentResource(Resource resource, Slave slave)
    {
        // Shallow copy: we take ownership
        this.setName(resource.getName());
        this.setProperties(resource.getProperties());
        this.setVersions(resource.getVersions());

        this.slave = slave;
    }

    public Resource asResource()
    {
        // Deep copy the other way: don't want hibernate proxies in result.
        Resource resource = new Resource(getName());
        for(Property p: getProperties().values())
        {
            resource.addProperty(new Property(p.getName(), p.getValue()));
        }

        for(ResourceVersion v: getVersions().values())
        {
            ResourceVersion copy = new ResourceVersion(v.getValue());
            for(Property p: v.getProperties().values())
            {
                try
                {
                    copy.addProperty(new Property(p.getName(), p.getValue()));
                }
                catch (FileLoadException e)
                {
                    // Impossible
                }
            }

            resource.add(copy);
        }

        return resource;
    }

    public Slave getSlave()
    {
        return slave;
    }

    public void setSlave(Slave slave)
    {
        this.slave = slave;
    }

}
