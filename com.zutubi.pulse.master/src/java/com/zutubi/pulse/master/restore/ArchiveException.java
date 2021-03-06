package com.zutubi.pulse.master.restore;

import com.zutubi.pulse.core.api.PulseException;

/**
 * The base exception for the exceptions generated by the Archive (backup/restore)
 * processing.
 */
public class ArchiveException extends PulseException
{
    public ArchiveException()
    {
    }

    public ArchiveException(String errorMessage)
    {
        super(errorMessage);
    }

    public ArchiveException(Throwable cause)
    {
        super(cause);
    }

    public ArchiveException(String errorMessage, Throwable cause)
    {
        super(errorMessage, cause);
    }
}
