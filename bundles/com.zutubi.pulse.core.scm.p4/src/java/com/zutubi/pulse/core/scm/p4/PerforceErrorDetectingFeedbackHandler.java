package com.zutubi.pulse.core.scm.p4;

import com.zutubi.pulse.core.scm.api.ScmCancelledException;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.pulse.core.scm.process.api.ScmLineHandler;

/**
 * Perforce output handler that detects and reports errors.
 */
public abstract class PerforceErrorDetectingFeedbackHandler implements ScmLineHandler
{
    /**
     * Perforce may report this error during a sync but it does not affect the
     * actual sync itself, so we can continue regardless.
     */
    private static final String ERROR_PROXY_CACHE = "Proxy could not update its cache";

    private boolean throwOnStderr = false;
    private String commandLine;
    private boolean haveSignificantError = false;
    private StringBuilder stderr;

    public PerforceErrorDetectingFeedbackHandler(boolean throwOnStderr)
    {
        this.throwOnStderr = throwOnStderr;
        stderr = new StringBuilder();
    }

    public void handleCommandLine(String line)
    {
        this.commandLine = line;
        haveSignificantError = false;
        stderr.delete(0, stderr.length());
    }

    public void handleStderr(String line)
    {
        stderr.append(line);
        stderr.append('\n');

        if (!line.contains(ERROR_PROXY_CACHE))
        {
            haveSignificantError = true;
        }
    }

    public void handleExitCode(int code) throws ScmException
    {
        String prefix = commandLine == null ? "p4 process" : "'" + commandLine + "'";
        if (code != 0)
        {
            String message = prefix + " returned non-zero exit code: " + Integer.toString(code);

            if (stderr.length() > 0)
            {
                message += ", error '" + stderr.toString().trim() + "'";
            }

            throw new ScmException(message);
        }

        if (haveSignificantError && throwOnStderr)
        {
            throw new ScmException(prefix + " returned error '" + stderr.toString().trim() + "'");
        }
    }

    public void checkCancelled() throws ScmCancelledException
    {
    }

    public StringBuilder getStderr()
    {
        return stderr;
    }
}
