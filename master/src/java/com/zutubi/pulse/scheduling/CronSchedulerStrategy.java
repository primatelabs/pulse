package com.zutubi.pulse.scheduling;

import org.hibernate.proxy.HibernateProxy;

import java.text.ParseException;

/**
 * <class-comment/>
 */
public class CronSchedulerStrategy extends QuartzSchedulerStrategy
{
    public String canHandle()
    {
        return CronTrigger.TYPE;
    }

    public boolean dependsOnProject(Trigger trigger, long projectId)
    {
        return false;
    }

    protected org.quartz.Trigger createTrigger(Trigger trigger) throws SchedulingException
    {
        CronTrigger cronTrigger = (CronTrigger) trigger;
        try
        {
            return new org.quartz.CronTrigger(Long.toString(cronTrigger.getId()), QUARTZ_GROUP, cronTrigger.getCron());
        }
        catch (ParseException e)
        {
            throw new SchedulingException(e);
        }
    }
}
