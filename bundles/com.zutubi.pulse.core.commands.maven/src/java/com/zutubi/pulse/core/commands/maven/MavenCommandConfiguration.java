package com.zutubi.pulse.core.commands.maven;

import com.zutubi.pulse.core.commands.core.NamedArgumentCommandConfiguration;
import com.zutubi.tove.annotations.Form;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.util.SystemUtils;
import com.zutubi.util.TextUtils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Configuration for instances of {@link MavenCommand}.
 */
@SymbolicName("zutubi.mavenCommandConfig")
@Form(fieldOrder = {"name", "workingDir", "targets", "args", "extraArguments", "postProcessors", "exe", "inputFile", "outputFile", "force"})
public class MavenCommandConfiguration extends NamedArgumentCommandConfiguration
{
    private String targets;

    public MavenCommandConfiguration()
    {
        super(MavenCommand.class, "maven.bin", SystemUtils.IS_WINDOWS ? "maven.bat" : "maven");
    }

    protected List<NamedArgument> getNamedArguments()
    {
        List<NamedArgument> result = new LinkedList<NamedArgument>();
        if (TextUtils.stringSet(targets))
        {
            result.add(new NamedArgument("targets", targets, Arrays.asList(targets.split("\\s+"))));
        }
        return result;
    }

    public String getTargets()
    {
        return targets;
    }

    public void setTargets(String targets)
    {
        this.targets = targets;
    }
}
