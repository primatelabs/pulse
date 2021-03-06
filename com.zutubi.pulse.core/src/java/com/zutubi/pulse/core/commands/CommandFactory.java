package com.zutubi.pulse.core.commands;

import com.zutubi.pulse.core.commands.api.Command;
import com.zutubi.pulse.core.commands.api.CommandConfiguration;

/**
 * Factory for creating commands from configuration.
 */
public interface CommandFactory
{
    /**
     * Create a new command from the given configuration.  The configuration
     * identifies the type of command to create, and that type should have a
     * single-parameter constructor which will accept the configuration as an
     * argument.
     *
     * @param configuration configuration used to build the command
     * @return the created command
     * @throws com.zutubi.pulse.core.engine.api.BuildException on any error
     */
    Command create(CommandConfiguration configuration);
}