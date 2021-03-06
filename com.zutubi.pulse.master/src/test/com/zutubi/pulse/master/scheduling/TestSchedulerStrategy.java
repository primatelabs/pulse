package com.zutubi.pulse.master.scheduling;

import java.util.Arrays;
import java.util.List;

public class TestSchedulerStrategy implements SchedulerStrategy
{
    public List<String> canHandle()
    {
        return Arrays.asList(CronTrigger.TYPE,SimpleTrigger.TYPE,EventTrigger.TYPE,NoopTrigger.TYPE );
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
