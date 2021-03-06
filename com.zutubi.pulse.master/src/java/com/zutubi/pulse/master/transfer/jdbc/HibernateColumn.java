package com.zutubi.pulse.master.transfer.jdbc;

import com.zutubi.pulse.master.transfer.Column;

/**
 *
 *
 */
public class HibernateColumn implements Column
{
    private org.hibernate.mapping.Column delegate;

    public HibernateColumn(org.hibernate.mapping.Column delegate)
    {
        this.delegate = delegate;
    }

    public String getName()
    {
        return delegate.getName();
    }

    public int getSqlTypeCode()
    {
        return delegate.getSqlTypeCode();
    }
}
