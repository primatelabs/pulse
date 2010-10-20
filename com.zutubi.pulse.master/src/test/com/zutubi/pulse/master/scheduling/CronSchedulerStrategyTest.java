package com.zutubi.pulse.master.scheduling;

import com.zutubi.pulse.master.scheduling.quartz.TriggerAdapter;
import static com.zutubi.pulse.master.scheduling.QuartzSchedulerStrategy.CALLBACK_JOB_NAME;
import static com.zutubi.pulse.master.scheduling.QuartzSchedulerStrategy.CALLBACK_JOB_GROUP;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

public class CronSchedulerStrategyTest extends SchedulerStrategyTestBase
{
    private org.quartz.Scheduler quartzScheduler = null;

    public void setUp() throws Exception
    {
        super.setUp();

        SchedulerFactory schedFact = new StdSchedulerFactory();
        quartzScheduler = schedFact.getScheduler();
        quartzScheduler.setJobFactory(new QuartzTaskJobFactory(triggerHandler));
        quartzScheduler.start();
        scheduler = new CronSchedulerStrategy();
        ((QuartzSchedulerStrategy)scheduler).setQuartzScheduler(quartzScheduler);
    }

    public void tearDown() throws Exception
    {
        quartzScheduler.shutdown();
        super.tearDown();
    }

    protected void activateTrigger(Trigger trigger) throws SchedulingException
    {
        try
        {
            String name = trigger.getName();
            String group = trigger.getGroup();
            org.quartz.Trigger t = quartzScheduler.getTrigger(name, group);
            if (t != null)
            {
                // if the quartz trigger is currently paused, then we should not trigger since this
                // is not what quartz itself would do. Remember, we are just trying to imitate quartz
                // in a controlled fashion.
                int state = quartzScheduler.getTriggerState(name, group);
                if (state == org.quartz.Trigger.STATE_PAUSED)
                {
                    return;
                }

                // we need to ensure that the quartz threads have had a chance to trigger
                // the job, so register a callback and then yeild until it is received.
                final boolean[] triggered = new boolean[]{false};
                TriggerAdapter globalTriggerListener = new TriggerAdapter()
                {
                    public void triggerComplete(org.quartz.Trigger trigger, JobExecutionContext context, int triggerInstructionCode)
                    {
                        triggered[0] = true;
                    }
                };
                
                quartzScheduler.addGlobalTriggerListener(globalTriggerListener);

                // manually trigger the quartz callback job with the triggers details.
                quartzScheduler.triggerJob(CALLBACK_JOB_NAME, CALLBACK_JOB_GROUP, t.getJobDataMap());

                while (!triggered[0])
                {
                    Thread.yield();
                }
            }
        }
        catch (SchedulerException e)
        {
            // trying to activate a job that is not registered? well, thats because its not scheduled....
            throw new SchedulingException(e);
        }
    }

    protected Trigger createTrigger()
    {
        // create a new quartz trigger. Ideally, this trigger would not possibly trigger
        // during the course of this test case since we are 'manually' handling the triggering
        // via the activateTrigger method.
        return new CronTrigger("0 0 0 ? * WED", getName());
    }
}
