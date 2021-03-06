package com.zutubi.pulse.core.scm.git;

import com.zutubi.util.SystemUtils;

import java.io.File;

/**
 * Shared constants used by the git implementation.
 */
public class GitConstants
{
    /**
     * Pulse system property used to override the default git command line executable.
     */
    public static final String PROPERTY_GIT_COMMAND = "pulse.git.command";

    /**
     * The default git executable, usually "git" except on Windows where it can be
     * named variably.  May be overridden with a system property.
     */
    public static final String DEFAULT_GIT;
    static 
    {
        if (SystemUtils.IS_WINDOWS)
        {
            File inPath = SystemUtils.findInPath("git");
            if (inPath == null)
            {
                DEFAULT_GIT = "git.exe";
            }
            else
            {
                DEFAULT_GIT = inPath.getName();
            }
        }
        else
        {
            DEFAULT_GIT = "git";
        }
    }
    public static final String RESOURCE_NAME = "git";

    // Top level commands.

    public static final String COMMAND_PULL = "pull";
    public static final String COMMAND_LOG = "log";
    public static final String COMMAND_CLONE = "clone";
    public static final String COMMAND_CHECKOUT = "checkout";
    public static final String COMMAND_BRANCH = "branch";
    public static final String COMMAND_SHOW = "show";
    public static final String COMMAND_DIFF = "diff";
    public static final String COMMAND_LS_REMOTE = "ls-remote";
    public static final String COMMAND_LS_TREE = "ls-tree";
    public static final String COMMAND_INIT = "init";
    public static final String COMMAND_REMOTE = "remote";
    public static final String COMMAND_MERGE = "merge";
    public static final String COMMAND_TAG = "tag";
    public static final String COMMAND_PUSH = "push";
    public static final String COMMAND_CONFIG = "config";
    public static final String COMMAND_REVISION_PARSE = "rev-parse";
    public static final String COMMAND_COMMIT = "commit";
    public static final String COMMAND_REBASE = "rebase";
    public static final String COMMAND_RESET = "reset";
    public static final String COMMAND_FETCH = "fetch";
    public static final String COMMAND_MERGE_BASE = "merge-base";
    public static final String COMMAND_APPLY = "apply";
    public static final String COMMAND_ADD = "add";
    public static final String COMMAND_SUBMODULE = "submodule";

    // Command flags.

    public static final String FLAG_AUTHOR = "--author";
    public static final String FLAG_BRANCH = "-b";
    public static final String FLAG_DEPTH = "--depth";
    public static final String FLAG_FORCE = "-f";
    public static final String FLAG_MIRROR = "--mirror";
    public static final String FLAG_NAME_STATUS = "--name-status";
    public static final String FLAG_PRETTY = "--pretty";
    public static final String FLAG_CHANGES = "-n";
    public static final String FLAG_NO_CHECKOUT = "--no-checkout";
    public static final String FLAG_REVERSE = "--reverse";
    public static final String FLAG_DELETE = "-D";
    public static final String FLAG_SHOW_MERGE_FILES = "-c";
    public static final String FLAG_FETCH = "-f";
    public static final String FLAG_TRACK = "-t";
    public static final String FLAG_SET_HEAD = "-m";
    public static final String FLAG_MESSAGE = "-m";
    public static final String FLAG_ALL = "-a";
    public static final String FLAG_ONTO = "--onto";
    public static final String FLAG_HARD = "--hard";
    public static final String FLAG_ADD = "-a";
    public static final String FLAG_BINARY = "--binary";
    public static final String FLAG_FIND_COPIES = "-C";
    public static final String FLAG_CACHED = "--cached";
    public static final String FLAG_VERBOSE = "--verbose";
    public static final String FLAG_INIT = "--init";
    public static final String FLAG_RECURSIVE = "--recursive";
    public static final String FLAG_SKIP = "--skip";
    public static final String FLAG_SINGLE_BRANCH = "--single-branch";
    public static final String FLAG_IGNORE_SPACE_CHANGE = "--ignore-space-change";
    
    public static final String FLAG_SEPARATOR = "--";

    // Non-flag command arguments (subcommands).

    public static final String ARG_ADD = "add";
    public static final String ARG_UPDATE = "update";

    // Misc.
    
    public static final String REMOTE_ORIGIN = "origin";

    public static final String REVISION_HEAD = "HEAD";

    public static final String TYPE_BLOB = "blob";
    public static final String TYPE_TREE = "tree";

    /**
     * File has conflicts after a merge
     */
    public static final String ACTION_UNMERGED =   "U";
    /**
     * File has been copied and modified
     */
    public static final String ACTION_COPY_MODIFIED =   "C";
    /**
     * File has been renamed and modified
     */
    public static final String ACTION_RENAME_MODIFIED =   "R";
    /**
     * File has been modified
     */
    public static final String ACTION_MODIFIED =   "M";
    /**
     * File has been added
     */
    public static final String ACTION_ADDED =    "A";
    /**
     * File has been deleted
     */
    public static final String ACTION_DELETED =  "D";

    public static final String CONFIG_TYPE_BOOLEAN = "--bool";
    public static final String CONFIG_BARE = "core.bare";
}
