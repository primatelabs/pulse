package com.zutubi.pulse.master.build.queue;

import com.google.common.base.Predicate;

/**
 * A predicate that matches a request holder containing a
 * build request event with a specified owner
 *
 * @param <T> the specific subclass of RequestHolder that is being searched.
 */
public class HasOwnerPredicate<T extends RequestHolder> implements Predicate<T>
{
    private Object owner;

    public HasOwnerPredicate(Object owner)
    {
        this.owner = owner;
    }

    public boolean apply(T holder)
    {
        return holder.getOwner().equals(owner);
    }
}
