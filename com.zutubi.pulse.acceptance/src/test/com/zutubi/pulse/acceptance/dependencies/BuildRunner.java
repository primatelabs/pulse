package com.zutubi.pulse.acceptance.dependencies;

import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.acceptance.XmlRpcHelper;
import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.util.Pair;
import static com.zutubi.util.CollectionUtils.asPair;

import java.util.Hashtable;
import java.lang.reflect.Array;

/**
 * The build runner is an acceptance test support class that caters specifically
 * to the triggering and monitoring of builds. 
 */
public class BuildRunner
{
    private XmlRpcHelper xmlRpcHelper;

    /**
     * Create a new instance of the build runner.
     *
     * @param xmlRpcHelper  the xml rpc helper configured to connect to the pulse server
     * on which builds will be run.
     */
    public BuildRunner(XmlRpcHelper xmlRpcHelper)
    {
        this.xmlRpcHelper = xmlRpcHelper;
    }

    /**
     * Trigger a build for the specified project and asserting that it is successful.
     *
     * @param project   the project for which a build is being triggered.
     * @param options   the trigger options
     * @return  the build number
     * 
     * @throws Exception on error or if the build was not successful.
     */
    public int triggerSuccessfulBuild(ProjectConfiguration project, Pair<String, Object>... options) throws Exception
    {
        int buildNumber = triggerCompleteBuild(project, options);

        ResultState buildStatus = getBuildStatus(project, buildNumber);
        if (!ResultState.SUCCESS.equals(buildStatus))
        {
            throw new RuntimeException("Expected success, had " + buildStatus + " instead.");
        }
        return buildNumber;
    }

    /**
     * Trigger a build for the specified project and wait for it to complete.
     *
     * @param project   the project for which a build is being triggered.
     * @param options   the trigger options.
     * @return the build number
     *
     * @throws Exception on error or if the we timed out waiting for the build to complete.
     */
    public int triggerCompleteBuild(ProjectConfiguration project, Pair<String, Object>... options) throws Exception
    {
        int number = triggerBuild(project, options);
        xmlRpcHelper.waitForBuildToComplete(project.getName(), number);
        return number;
    }

    /**
     * Trigger a build of the specified project.
     *
     * @param project   the project for which the build is being triggered.
     * @param options   the build options.
     * @return  the build number
     * 
     * @throws Exception on error.
     */
    public int triggerBuild(ProjectConfiguration project, Pair<String, Object>... options) throws Exception
    {
        // projects that are not initialised will 'drop' the trigger request. 
        xmlRpcHelper.waitForProjectToInitialise(project.getName());
        
        Hashtable<String, Object> triggerOptions = new Hashtable<String, Object>();
        if (options != null)
        {
            for (Pair<String, Object> option : options)
            {
                triggerOptions.put(option.getFirst(), option.getSecond());
            }
        }

        int number = xmlRpcHelper.getNextBuildNumber(project.getName());
        xmlRpcHelper.call("triggerBuild", project.getName(), triggerOptions);
        return number;
    }

    /**
     * Trigger a build of the specified project, with the 'rebuild' trigger option set to
     * true.
     *
     * @param project   the project to be built.
     * @param options   the build options.
     * @return  the build number.
     * @throws Exception on error.
     */
    @SuppressWarnings({"unchecked"})
    public int triggerRebuild(ProjectConfiguration project, Pair<String, Object>... options) throws Exception
    {
        Pair<String, Object>[] args = (Pair<String, Object>[]) Array.newInstance(Pair.class, options.length + 1);
        System.arraycopy(options, 0, args, 0, options.length);
        args[args.length - 1] = asPair("rebuild", (Object)"true");
        return triggerBuild(project, args);
    }

    /**
     * Get the status of the specific build.
     *
     * @param project       the project that the build belongs to.
     * @param buildNumber   the build number uniquely identifying the build.
     * @return  the result state of the build or null if the build was not found.
     *
     * @throws Exception thrown on error.
     */
    public ResultState getBuildStatus(ProjectConfigurationHelper project, int buildNumber) throws Exception
    {
        return getBuildStatus(project.getConfig(), buildNumber);
    }

    /**
     * Get the status of the specific build.
     *
     * @param project       the project that the build belongs to.
     * @param buildNumber   the build number uniquely identifying the build.
     * @return the result state of the build, or null if the build was not found.
     *
     * @throws Exception thrown on error.
     */
    public ResultState getBuildStatus(ProjectConfiguration project, int buildNumber) throws Exception
    {
        Hashtable<String, Object> build = xmlRpcHelper.getBuild(project.getName(), buildNumber);
        if (build != null)
        {
            return ResultState.fromPrettyString((String) build.get("status"));
        }
        return null;
    }

}