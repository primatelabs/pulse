package com.zutubi.util;

import java.util.Date;

/**
 * A namespace for constant values.
 */
public class Constants
{
    public static final long MEGABYTE = 1048576;

    public static final long MILLISECOND = 1;
    public static final long SECOND      = 1000 * MILLISECOND;
    public static final long MINUTE      = 60 * SECOND;
    public static final long HOUR        = 60 * MINUTE;
    public static final long DAY         = 24 * HOUR;
    public static final long WEEK        = 7 * DAY;
    public static final long YEAR        = 365 * DAY;

    public static final Date DAY_0 = new Date(0);
}
