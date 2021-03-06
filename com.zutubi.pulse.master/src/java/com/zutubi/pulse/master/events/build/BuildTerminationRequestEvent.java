package com.zutubi.pulse.master.events.build;

import com.zutubi.events.Event;
import com.zutubi.util.time.TimeStamps;

import java.util.Locale;

/**
 * Raised to request that running builds be forcefully terminated.
 */
public class BuildTerminationRequestEvent extends Event
{
    private static final int ALL_BUILDS = -1;

    /**
     * ID of the build to terminate, or -1 to terminate all builds.
     */
    private long buildId;

    /**
     * If not null, gives a reason for this termination, e.g. "requested by admin".
     */
    private String reason;
    /**
     * If true, kill the build as quickly as possible without any graceful cleanup.
     */
    private boolean kill;
    /**
     * Milliseconds since the epoch at the time of this request.
     */
    private long timestamp;

    /**
     * Request that all builds be terminated.
     *
     * @param source the source of the termination request
     * @param reason a human readable message describing why the request has been made
     * @param kill   true to forcefully kill the builds with no graceful cleanup
     */
    public BuildTerminationRequestEvent(Object source, String reason, boolean kill)
    {
        this(source, ALL_BUILDS, reason, kill);
    }

    public BuildTerminationRequestEvent(Object source, long buildId, String reason, boolean kill)
    {
        super(source);
        this.buildId = buildId;
        this.reason = reason;
        this.kill = kill;
        timestamp = System.currentTimeMillis();
    }

    /**
     * Returns true if the specified build id should be terminated, false otherwise.
     * @param buildId   the id of the build being checked.
     * @return true if the specified build should be terminated, false otherwise.
     */
    public boolean isTerminationRequested(long buildId)
    {
        return isTerminateAllBuilds() || this.buildId == buildId;
    }

    public boolean isTerminateAllBuilds()
    {
        return this.buildId == ALL_BUILDS;
    }

    public long getBuildId()
    {
        return buildId;
    }

    public String getReason()
    {
        return reason;
    }

    public boolean isKill()
    {
        return kill;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public String getMessage()
    {
        String message = "Forceful termination ";
        if (reason != null)
        {
            message += reason + " ";
        }

        message += "at " + TimeStamps.getPrettyDate(timestamp, Locale.getDefault());
        return message;
    }

    public String toString()
    {
        return String.format("Build Termination Request Event[build: %s, message: %s, kill: %s]", (isTerminateAllBuilds() ? "all" : buildId), getMessage(), kill ? "yes" : "no");
    }
}
