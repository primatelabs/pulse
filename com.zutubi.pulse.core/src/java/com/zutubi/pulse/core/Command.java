package com.zutubi.pulse.core;

import com.zutubi.pulse.core.commands.api.CommandContext;

/**
 * 
 *
 */
public interface Command
{
    /**
     * Execute the command.
     *
     * @param context defines the context in which the command is being executed.
     * @param result defines the command result instance used by the command to record its execution details.
     */
    void execute(CommandContext commandContext);

    /**
     *
     * @return a list of artifacts generated by this command implementation.
     */
    // FIXME loader
//    List<Artifact> getArtifacts();

    /**
     * The name of the command is used to identify it.
     *
     * @return name of the command.
     */
    // FIXME loader
//    @Required String getName();

    /**
     * Set the name of the command.
     *
     * @param name value. 
     */
    // FIXME loader
//    void setName(String name);

    /**
     * @return true if this command should run even if a prior command has failed
     */
    // FIXME loader redundant
//    boolean isForce();

    /**
     * The terminate method allows the commands execution to be interupted.
     */
    void terminate();
}
