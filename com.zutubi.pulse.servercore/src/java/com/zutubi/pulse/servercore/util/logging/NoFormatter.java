package com.zutubi.pulse.servercore.util.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * An implementation of the formatter that applies no extra formatting to log messages.
 */
public class NoFormatter extends Formatter
{
    public String format(LogRecord record)
    {
        return record.getMessage();
    }
}
