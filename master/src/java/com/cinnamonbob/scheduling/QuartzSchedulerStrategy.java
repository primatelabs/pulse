package com.cinnamonbob.scheduling;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.JobDetail;

/**
 * <class-comment/>
 */
public abstract class QuartzSchedulerStrategy implements SchedulerStrategy
{
    protected static final String CALLBACK_JOB_NAME = "cron.trigger.job.name";

    protected static final String CALLBACK_JOB_GROUP = "cron.trigger.job.group";

    private Scheduler quartzScheduler;

    private TriggerHandler triggerHandler;

    public void setQuartzScheduler(Scheduler quartzScheduler)
    {
        this.quartzScheduler = quartzScheduler;
    }

    public Scheduler getQuartzScheduler()
    {
        return quartzScheduler;
    }

    public void pause(Trigger trigger) throws SchedulingException
    {
        try
        {
            getQuartzScheduler().pauseTrigger(trigger.getName(), trigger.getGroup());
            trigger.setState(TriggerState.PAUSED);
        }
        catch (SchedulerException e)
        {
            throw new SchedulingException(e);
        }
    }

    public void resume(Trigger trigger) throws SchedulingException
    {
        try
        {
            getQuartzScheduler().resumeTrigger(trigger.getName(), trigger.getGroup());
            trigger.setState(TriggerState.ACTIVE);
        }
        catch (SchedulerException e)
        {
            throw new SchedulingException(e);
        }
    }

    public void unschedule(Trigger trigger) throws SchedulingException
    {
        try
        {
            getQuartzScheduler().unscheduleJob(trigger.getName(), trigger.getGroup());
            trigger.setState(TriggerState.NONE);
        }
        catch (SchedulerException e)
        {
            throw new SchedulingException(e);
        }
    }

    protected void ensureCallbackRegistered()
            throws SchedulerException
    {
        JobDetail existingJob = getQuartzScheduler().getJobDetail(CALLBACK_JOB_NAME, CALLBACK_JOB_GROUP);
        if (existingJob == null)
        {
            // register the job detail once only.
            JobDetail detail = new JobDetail(CALLBACK_JOB_NAME, CALLBACK_JOB_GROUP, QuartzTaskCallbackJob.class);
            detail.setDurability(true); // will stay around after the trigger has gone.
            getQuartzScheduler().addJob(detail, true);
        }
    }

    public void schedule(Trigger trigger) throws SchedulingException
    {
        try
        {
            // the quartz trigger equivalent...
            org.quartz.Trigger quartzTrigger = createTrigger(trigger);

            // the callback job
            ensureCallbackRegistered();
            quartzTrigger.setJobName(CALLBACK_JOB_NAME);
            quartzTrigger.setJobGroup(CALLBACK_JOB_GROUP);
            quartzTrigger.getJobDataMap().put(QuartzTaskCallbackJob.TRIGGER_PROP, trigger);

            getQuartzScheduler().scheduleJob(quartzTrigger);

            trigger.setState(TriggerState.ACTIVE);
        }
        catch (SchedulerException e)
        {
            throw new SchedulingException(e);
        }
    }

    public void setTriggerHandler(TriggerHandler handler)
    {
        this.triggerHandler = handler;
    }

    protected abstract org.quartz.Trigger createTrigger(Trigger trigger) throws SchedulingException;
}
