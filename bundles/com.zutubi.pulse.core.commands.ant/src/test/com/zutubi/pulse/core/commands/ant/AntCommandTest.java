package com.zutubi.pulse.core.commands.ant;

import com.zutubi.pulse.core.commands.core.EnvironmentConfiguration;
import com.zutubi.pulse.core.commands.core.ExecutableCommandTestCase;
import com.zutubi.pulse.core.commands.core.NamedArgumentCommand;
import com.zutubi.pulse.core.commands.api.TestCommandContext;
import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.util.SystemUtils;

import java.io.File;
import java.io.IOException;

public class AntCommandTest extends ExecutableCommandTestCase
{
    private static final String EXTENSION_XML = "xml";

    public void testBasicDefault() throws Exception
    {
        copyBuildFile("basic");

        NamedArgumentCommand command = new NamedArgumentCommand(new AntCommandConfiguration());
        successRun(command, "build target");
    }

    public void testBasicTargets() throws Exception
    {
        copyBuildFile("basic");

        AntCommandConfiguration config = new AntCommandConfiguration();
        config.setTargets("build test");

        NamedArgumentCommand command = new NamedArgumentCommand(config);
        successRun(command, "build target", "test target");
    }

    public void testDoubleSpaceTargets() throws Exception
    {
        copyBuildFile("basic");

        AntCommandConfiguration config = new AntCommandConfiguration();
        config.setTargets("build  test");

        NamedArgumentCommand command = new NamedArgumentCommand(config);
        successRun(command, "build target", "test target");
    }

    public void testEnvironment() throws Exception
    {
        copyBuildFile("basic");

        AntCommandConfiguration config = new AntCommandConfiguration();
        config.setTargets("environment");
        config.getEnvironments().add(new EnvironmentConfiguration("TEST_ENV_VAR", "test variable value"));

        NamedArgumentCommand command = new NamedArgumentCommand(config);
        successRun(command, "test variable value");
    }

    public void testExplicitBuildfile() throws Exception
    {
        copyBuildFile("basic",  "custom.xml");

        AntCommandConfiguration config = new AntCommandConfiguration();
        config.setBuildFile("custom.xml");

        NamedArgumentCommand command = new NamedArgumentCommand(config);
        successRun(command, "build target");
    }

    public void testExplicitArguments() throws Exception
    {
        copyBuildFile("basic",  "custom.xml");

        AntCommandConfiguration config = new AntCommandConfiguration();
        config.setBuildFile("custom.xml");
        config.setTargets("build");
        config.setArgs("test");

        NamedArgumentCommand command = new NamedArgumentCommand(config);
        successRun(command, "build target", "test target");
    }

    public void testRunNoBuildFile() throws Exception
    {
        NamedArgumentCommand command = new NamedArgumentCommand(new AntCommandConfiguration());
        TestCommandContext context = runCommand(command);
        // Windows batch files have unreliable exit codes, so we don't confirm
        // on Windows (where the post-processor fixes this).
        if (!SystemUtils.IS_WINDOWS)
        {
            assertEquals(ResultState.FAILURE, context.getResultState());
        }

        assertDefaultOutputContains("Buildfile: build.xml does not exist!");
    }

    public void testRunNonExistantBuildFile() throws Exception
    {
        AntCommandConfiguration config = new AntCommandConfiguration();
        config.setBuildFile("nope.xml");

        NamedArgumentCommand command = new NamedArgumentCommand(config);
        TestCommandContext context = runCommand(command);
        // Windows batch files have unreliable exit codes, so we don't confirm
        // on Windows (where the post-processor fixes this).
        if (!SystemUtils.IS_WINDOWS)
        {
            assertEquals(ResultState.FAILURE, context.getResultState());
        }

        assertDefaultOutputContains("Buildfile: nope.xml does not exist!");
    }

    private File copyBuildFile(String name) throws IOException
    {
        return copyBuildFile(name, "build.xml");
    }

    private File copyBuildFile(String name, String toName) throws IOException
    {
        return copyBuildFile(name, EXTENSION_XML, toName);
    }
}
