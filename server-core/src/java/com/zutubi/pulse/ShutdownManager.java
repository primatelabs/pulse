package com.zutubi.pulse;

import com.zutubi.pulse.core.Stoppable;
import com.zutubi.pulse.bootstrap.ComponentContext;

import java.util.List;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Manages orderly shutdown of the system.  The order is determined by a list
 * configured externally (e.g. via Spring).
 */
public class ShutdownManager
{
    private List<Stoppable> stoppables;

    /**
     * Performs the shutdown sequence.
     *
     * @param force if true, forcibly terminate active tasks
     */
    public void shutdown(boolean force, boolean exitJvm)
    {
        if(checkWrapper(111))
        {
            return;
        }

        stop(force);

        if (exitJvm)
        {
            // why exit? because some external packages do not clean up all of their threads... 
            System.exit(0);
        }
        else
        {
            // cleanout the component context.
            ComponentContext.closeAll();
        }
    }

    public void stop(boolean force)
    {
        for (Stoppable stoppable : stoppables)
        {
            stoppable.stop(force);
        }
    }

    public boolean checkWrapper(int exitCode)
    {
        try
        {
            // If we are running under the Java Service Wrapper, stop via it.
            Class wrapperManagerClass = Class.forName("org.tanukisoftware.wrapper.WrapperManager");

            try
            {
                Method stopMethod = wrapperManagerClass.getMethod("stop", int.class);
                stopMethod.invoke(null, exitCode);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw e;
            }
        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    public void delayedShutdown(boolean force, boolean exitJvm)
    {
        ShutdownRunner runner = new ShutdownRunner(force, exitJvm);
        new Thread(runner).start();
    }

    public void delayedStop()
    {
        StopRunner runner = new StopRunner();
        new Thread(runner).start();
    }

    public void setStoppables(List<Stoppable> stoppables)
    {
        this.stoppables = stoppables;
    }

    private class ShutdownRunner implements Runnable
    {
        private boolean force;
        private boolean exitJvm;

        public ShutdownRunner(boolean force, boolean exitJvm)
        {
            this.force = force;
            this.exitJvm = exitJvm;
        }

        public void run()
        {
            // Oh my, is this ever dodgy...
            try
            {
                //TODO: work out a way to shutdown jetty where by it stops accepting inbound requests
                // and then shuts itself down after it has finished processing the final active request.
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                // Empty
            }
            shutdown(force, exitJvm);
        }
    }

    private class StopRunner implements Runnable
    {
        public void run()
        {
            try
            {
                Thread.sleep(500);
            }
            catch(InterruptedException e)
            {
                // Nothing
            }

            stop(true);
        }
    }
}
