package com.zutubi.pulse.core;

import com.opensymphony.util.TextUtils;
import com.zutubi.pulse.core.model.CommandResult;
import com.zutubi.validation.Validateable;
import com.zutubi.validation.ValidationContext;
import com.zutubi.validation.annotations.Required;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 * A command group represents a command and a set of artifact definitions.
 *
 */
public class CommandGroup implements Command, Validateable
{
    private String name;

    private Command command = null;

    private List<Artifact> artifacts = new LinkedList<Artifact>();

    public void add(Command cmd) throws FileLoadException
    {
        if (this.command != null)
        {
            throw new FileLoadException("A 'command' tag may only contain a single nested command.");
        }
        this.command = cmd;

        if (!TextUtils.stringSet(this.command.getName()))
        {
            this.command.setName(name);
        }
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void terminate()
    {
        command.terminate();
    }

    public String getName()
    {
        return name;
    }

    public FileArtifact createArtifact()
    {
        FileArtifact customArtifact = new FileArtifact();
        artifacts.add(customArtifact);
        return customArtifact;
    }

    public DirectoryArtifact createDirArtifact()
    {
        DirectoryArtifact customArtifact = new DirectoryArtifact();
        artifacts.add(customArtifact);
        return customArtifact;
    }

    public LinkArtifact createLinkArtifact()
    {
        LinkArtifact linkArtifact = new LinkArtifact();
        artifacts.add(linkArtifact);
        return linkArtifact;
    }

    public void execute(CommandContext context, CommandResult result)
    {
        try
        {
            command.execute(context, result);
        }
        finally
        {
            processArtifacts(context, result);
        }
    }

    //TODO: move this out of the command group and into the recipe processing.
    private void processArtifacts(CommandContext context, CommandResult result)
    {
        for(Artifact artifact: getArtifacts())
        {
            try
            {
                artifact.capture(result, context);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public List<Artifact> getArtifacts()
    {
        List<Artifact> allArtifacts = new LinkedList<Artifact>();
        allArtifacts.addAll(artifacts);
        allArtifacts.addAll(getCommand().getArtifacts());
        return allArtifacts;
    }

    public List<String> getArtifactNames()
    {
        List<String> names = new LinkedList<String>();
        for (Artifact artifact : getArtifacts())
        {
            names.add(artifact.getName());
        }
        return names;
    }

    @Required public Command getCommand()
    {
        return command;
    }

    public void validate(ValidationContext context)
    {
        // ensure that our artifacts have unique names.
        List<String> artifactNames = getArtifactNames();
        Set<String> names = new TreeSet<String>();
        for (String name : artifactNames)
        {
            if (names.contains(name))
            {
                context.addFieldError("name", "A duplicate artifact name '" + name + "' has been detected. Please only " +
                        "use unique names for artifacts within a command group.");
            }
            names.add(name);
        }
    }
}
