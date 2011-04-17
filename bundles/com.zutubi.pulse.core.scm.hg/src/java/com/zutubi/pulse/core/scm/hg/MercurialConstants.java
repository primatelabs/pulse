package com.zutubi.pulse.core.scm.hg;

import com.zutubi.util.SystemUtils;

/**
 * Shared constants used by the hg implementation.
 */
public class MercurialConstants
{
    /**
     * Pulse system property used to override the default hg command line executable.
     */
    public static final String PROPERTY_HG_COMMAND = "pulse.hg.command";

    /**
     * The default hg executable, usually "hg" but "hg.exe" on a windows
     * system.  May be overridden with a system property.
     */
    public static final String DEFAULT_HG = SystemUtils.IS_WINDOWS ? "hg.exe" : "hg";

    public static final String COMMAND_CAT = "cat";
    public static final String COMMAND_CLONE = "clone";
    public static final String COMMAND_LOG = "log";
    public static final String COMMAND_PARENTS = "parents";
    public static final String COMMAND_PULL = "pull";
    public static final String COMMAND_PUSH = "push";
    public static final String COMMAND_TAG = "tag";
    public static final String COMMAND_TAGS = "tags";
    public static final String COMMAND_UPDATE = "update";

    public static final String FLAG_BRANCH = "--branch";
    public static final String FLAG_FORCE = "--force";
    public static final String FLAG_LIMIT = "--limit";
    public static final String FLAG_MESSAGE = "--message";
    public static final String FLAG_NO_UPDATE = "--noupdate";
    public static final String FLAG_REVISION = "--rev";
    public static final String FLAG_STYLE = "--style";
    public static final String FLAG_TEMPLATE = "--template";
    public static final String FLAG_VERBOSE = "--verbose";

    public static final String BRANCH_DEFAULT = "default";

    public static final String REVISION_NULL = "null";
    public static final String REVISION_ZERO = "0";
    public static final String REVISION_TIP = "tip";

    public static final String STYLE_XML = "xml";
    public static final String TEMPLATE_NODE = "{node}";
}
