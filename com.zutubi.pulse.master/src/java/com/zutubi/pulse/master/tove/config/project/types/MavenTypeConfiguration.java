package com.zutubi.pulse.master.tove.config.project.types;

import com.zutubi.tove.annotations.Form;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.pulse.master.tove.config.project.BrowseScmDirAction;
import com.zutubi.util.TextUtils;
import org.apache.velocity.VelocityContext;

/**
 * The UI configuration for the maven 1.x project template.
 */
@SymbolicName("zutubi.mavenTypeConfig")
@Form(fieldOrder = {"workingDir", "targets", "arguments", "postProcessors"})
public class MavenTypeConfiguration extends TemplateTypeConfiguration
{
    private String targets;
    @BrowseScmDirAction
    private String workingDir;

    private String arguments;

    public MavenTypeConfiguration()
    {
        // setup the default surefire-reports test artifact
        DirectoryArtifactConfiguration artifact = new DirectoryArtifactConfiguration();
        artifact.setName("test reports");
        artifact.setBase("target/test-reports");
        artifact.setIncludes("TEST-*.xml");
        artifact.addPostprocessor("junit");
        addArtifact(artifact);
    }

    public String getTargets()
    {
        return targets;
    }

    public void setTargets(String targets)
    {
        this.targets = targets;
    }

    public String getWorkingDir()
    {
        return workingDir;
    }

    public void setWorkingDir(String workingDir)
    {
        this.workingDir = workingDir;
    }

    public String getArguments()
    {
        return arguments;
    }

    public void setArguments(String arguments)
    {
        this.arguments = arguments;
    }

    protected String getTemplateName()
    {
        return "maven.template.vm";
    }

    protected void setupContext(VelocityContext context)
    {
        if (TextUtils.stringSet(targets))
        {
            context.put("targets", targets);
        }
        if (TextUtils.stringSet(workingDir))
        {
            context.put("workingDir", workingDir.trim());
        }
        if (TextUtils.stringSet(arguments))
        {
            context.put("arguments", arguments);
        }
    }
}
