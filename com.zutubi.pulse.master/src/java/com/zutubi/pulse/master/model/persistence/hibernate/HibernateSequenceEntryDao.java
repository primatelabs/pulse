package com.zutubi.pulse.master.model.persistence.hibernate;

import com.zutubi.pulse.master.model.SequenceEntry;
import com.zutubi.pulse.master.model.persistence.SequenceEntryDao;

/**
 * The hibernate implementation of the {@link SequenceEntryDao} interface.
 */
public class HibernateSequenceEntryDao extends HibernateEntityDao<SequenceEntry> implements SequenceEntryDao 
{
    public Class<SequenceEntry> persistentClass()
    {
        return SequenceEntry.class;
    }

    public SequenceEntry findByName(String name)
    {
        return (SequenceEntry) findUniqueByNamedQuery("findByName", "name", name);
    }
}
