package com.zutubi.pulse.master.scheduling;

import java.util.Arrays;
import java.util.List;

/**
 * This scheduler strategy is for triggers that do nothing.  These
 * types of triggers exist for there configuration only.
 */
public class NoopSchedulerStrategy implements SchedulerStrategy
{
    public List<String> canHandle()
    {
        return Arrays.asList(NoopTrigger.TYPE);
    }

    public void init(Trigger trigger) throws SchedulingException
    {
    }

    public void pause(Trigger trigger) throws SchedulingException
    {
    }

    public void resume(Trigger trigger) throws SchedulingException
    {
    }

    public void stop(boolean force)
    {
    }

    public void schedule(Trigger trigger) throws SchedulingException
    {
    }

    public void unschedule(Trigger trigger) throws SchedulingException
    {
    }

    public void setTriggerHandler(TriggerHandler handler)
    {
    }
}
