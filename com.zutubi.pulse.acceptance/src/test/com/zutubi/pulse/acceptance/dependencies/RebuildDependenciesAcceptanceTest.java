package com.zutubi.pulse.acceptance.dependencies;

import com.zutubi.pulse.acceptance.AcceptanceTestUtils;
import com.zutubi.pulse.acceptance.BaseXmlRpcAcceptanceTest;
import com.zutubi.pulse.acceptance.XmlRpcHelper;
import static com.zutubi.pulse.core.dependency.ivy.IvyManager.STATUS_MILESTONE;
import com.zutubi.pulse.core.engine.api.ResultState;
import static com.zutubi.pulse.master.model.Project.State.IDLE;
import static com.zutubi.pulse.master.tove.config.project.DependencyConfiguration.*;
import static com.zutubi.util.CollectionUtils.asPair;
import com.zutubi.util.Condition;
import com.zutubi.util.FileSystemUtils;

import java.io.File;

public class RebuildDependenciesAcceptanceTest extends BaseXmlRpcAcceptanceTest
{
    private File tmpDir;
    private String projectName;
    private BuildRunner buildRunner;
    private ConfigurationHelper configurationHelper;
    private ProjectConfigurations projects;

    protected void setUp() throws Exception
    {
        super.setUp();

        loginAsAdmin();

        Repository repository = new Repository();
        repository.clear();

        tmpDir = FileSystemUtils.createTempDir(randomName());

        projectName = randomName();

        buildRunner = new BuildRunner(xmlRpcHelper);
        configurationHelper = new ConfigurationHelper();
        configurationHelper.setXmlRpcHelper(xmlRpcHelper);
        configurationHelper.init();

        projects = new ProjectConfigurations(configurationHelper);
    }

    @Override
    protected void tearDown() throws Exception
    {
        removeDirectory(tmpDir);

        logout();

        super.tearDown();
    }

    private void insertProject(ProjectConfigurationHelper project) throws Exception
    {
        configurationHelper.insertProject(project.getConfig());
    }

    public void testRebuildSingleDependency() throws Exception
    {
        WaitAntProject projectA = projects.createWaitAntProject(tmpDir, projectName + "A");
        insertProject(projectA);

        WaitAntProject projectB = projects.createWaitAntProject(tmpDir, projectName + "B");
        projectB.addDependency(projectA);
        insertProject(projectB);

        buildRunner.triggerRebuild(projectB.getConfig());

        // expect projectA to be building, projectB to be pending_dependency.
        xmlRpcHelper.waitForBuildInProgress(projectA.getName(), 1);

        assertEquals(ResultState.PENDING_DEPENDENCY, buildRunner.getBuildStatus(projectB, 1));

        projectA.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(projectA.getName(), 1);

        assertEquals(ResultState.SUCCESS, buildRunner.getBuildStatus(projectA, 1));

        // expect projectB to be building.
        xmlRpcHelper.waitForBuildInProgress(projectB.getName(), 1);
        projectB.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(projectB.getName(), 1);

        assertEquals(ResultState.SUCCESS, buildRunner.getBuildStatus(projectB, 1));
    }

    public void testRebuildMultipleDependencies() throws Exception
    {
        final WaitAntProject projectA = projects.createWaitAntProject(tmpDir, projectName + "A");
        insertProject(projectA);

        final WaitAntProject projectB = projects.createWaitAntProject(tmpDir, projectName + "B");
        insertProject(projectB);

        WaitAntProject projectC = projects.createWaitAntProject(tmpDir, projectName + "C");
        projectC.addDependency(projectA);
        projectC.addDependency(projectB);
        insertProject(projectC);

        buildRunner.triggerRebuild(projectC.getConfig());

        WaitAntProject firstDependency;
        WaitAntProject secondDependency;
        AcceptanceTestUtils.waitForCondition(new Condition()
        {
            public boolean satisfied()
            {
                try
                {
                    return buildRunner.getBuildStatus(projectA, 1) == ResultState.IN_PROGRESS || buildRunner.getBuildStatus(projectB, 1) == ResultState.IN_PROGRESS;
                }
                catch (Exception e)
                {
                    return false;
                }
            }
        }, XmlRpcHelper.BUILD_TIMEOUT, "a dependency to start building");

        if (buildRunner.getBuildStatus(projectA, 1) == ResultState.IN_PROGRESS)
        {
            firstDependency = projectA;
            secondDependency = projectB;
        }
        else
        {
            firstDependency = projectB;
            secondDependency = projectA;
        }

        assertEquals(ResultState.PENDING, buildRunner.getBuildStatus(secondDependency, 1));
        assertEquals(ResultState.PENDING_DEPENDENCY, buildRunner.getBuildStatus(projectC, 1));
        firstDependency.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(firstDependency.getName(), 1);

        xmlRpcHelper.waitForBuildInProgress(secondDependency.getName(), 1);
        assertEquals(ResultState.SUCCESS, buildRunner.getBuildStatus(firstDependency, 1));
        assertEquals(ResultState.IN_PROGRESS, buildRunner.getBuildStatus(secondDependency, 1));
        assertEquals(ResultState.PENDING_DEPENDENCY, buildRunner.getBuildStatus(projectC, 1));
        secondDependency.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(secondDependency.getName(), 1);

        xmlRpcHelper.waitForBuildInProgress(projectC.getName(), 1);
        assertEquals(ResultState.SUCCESS, buildRunner.getBuildStatus(projectA, 1));
        assertEquals(ResultState.SUCCESS, buildRunner.getBuildStatus(projectB, 1));
        assertEquals(ResultState.IN_PROGRESS, buildRunner.getBuildStatus(projectC, 1));
        projectC.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(projectC.getName(), 1);
    }

    public void testRebuildTransitiveDependency() throws Exception
    {
        final WaitAntProject projectA = projects.createWaitAntProject(tmpDir, projectName + "A");
        insertProject(projectA);

        final WaitAntProject projectB = projects.createWaitAntProject(tmpDir, projectName + "B");
        projectB.addDependency(projectA);
        insertProject(projectB);

        final WaitAntProject projectC = projects.createWaitAntProject(tmpDir, projectName + "C");
        projectC.addDependency(projectB);
        insertProject(projectC);

        buildRunner.triggerRebuild(projectC.getConfig());

        xmlRpcHelper.waitForBuildInProgress(projectA.getName(), 1);

        assertEquals(ResultState.IN_PROGRESS, buildRunner.getBuildStatus(projectA, 1));
        assertEquals(ResultState.PENDING_DEPENDENCY, buildRunner.getBuildStatus(projectB, 1));
        assertEquals(ResultState.PENDING_DEPENDENCY, buildRunner.getBuildStatus(projectC, 1));

        projectA.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(projectA.getName(), 1);
        xmlRpcHelper.waitForBuildInProgress(projectB.getName(), 1);

        assertEquals(ResultState.SUCCESS, buildRunner.getBuildStatus(projectA, 1));
        assertEquals(ResultState.IN_PROGRESS, buildRunner.getBuildStatus(projectB, 1));
        assertEquals(ResultState.PENDING_DEPENDENCY, buildRunner.getBuildStatus(projectC, 1));

        projectB.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(projectB.getName(), 1);
        xmlRpcHelper.waitForBuildInProgress(projectC.getName(), 1);

        assertEquals(ResultState.SUCCESS, buildRunner.getBuildStatus(projectA, 1));
        assertEquals(ResultState.SUCCESS, buildRunner.getBuildStatus(projectB, 1));
        assertEquals(ResultState.IN_PROGRESS, buildRunner.getBuildStatus(projectC, 1));

        projectC.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(projectC.getName(), 1);
    }

    public void testRebuildUsesTransitiveProperty() throws Exception
    {
        final WaitAntProject projectA = projects.createWaitAntProject(tmpDir, projectName + "A");
        insertProject(projectA);

        final WaitAntProject projectB = projects.createWaitAntProject(tmpDir, projectName + "B");
        projectB.addDependency(projectA).setTransitive(false);
        insertProject(projectB);

        final WaitAntProject projectC = projects.createWaitAntProject(tmpDir, projectName + "C");
        projectC.addDependency(projectB);
        insertProject(projectC);

        buildRunner.triggerRebuild(projectC.getConfig());

        xmlRpcHelper.waitForBuildInProgress(projectB.getName(), 1);

        assertEquals(IDLE, xmlRpcHelper.getProjectState(projectA.getName()));
        assertEquals(ResultState.IN_PROGRESS, buildRunner.getBuildStatus(projectB, 1));
        assertEquals(ResultState.PENDING_DEPENDENCY, buildRunner.getBuildStatus(projectC, 1));

        projectB.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(projectB.getName(), 1);
        xmlRpcHelper.waitForBuildInProgress(projectC.getName(), 1);

        assertEquals(ResultState.SUCCESS, buildRunner.getBuildStatus(projectB, 1));
        assertEquals(ResultState.IN_PROGRESS, buildRunner.getBuildStatus(projectC, 1));

        projectC.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(projectC.getName(), 1);
    }

    public void testRebuildUsesStatusProperty() throws Exception
    {
        final WaitAntProject projectA = projects.createWaitAntProject(tmpDir, projectName + "A");
        insertProject(projectA);

        final WaitAntProject projectB = projects.createWaitAntProject(tmpDir, projectName + "B");
        projectB.addDependency(projectA).setRevision(REVISION_LATEST_RELEASE);
        insertProject(projectB);

        final WaitAntProject projectC = projects.createWaitAntProject(tmpDir, projectName + "C");
        projectC.addDependency(projectB).setRevision(REVISION_LATEST_MILESTONE);
        insertProject(projectC);

        final WaitAntProject projectD = projects.createWaitAntProject(tmpDir, projectName + "D");
        projectD.addDependency(projectC).setRevision(REVISION_LATEST_INTEGRATION);
        insertProject(projectD);

        buildRunner.triggerRebuild(projectD.getConfig(), asPair("status", (Object) STATUS_MILESTONE));

        xmlRpcHelper.waitForBuildInProgress(projectB.getName(), 1);

        assertEquals(IDLE, xmlRpcHelper.getProjectState(projectA.getName()));
        assertEquals(ResultState.IN_PROGRESS, buildRunner.getBuildStatus(projectB, 1));
        assertEquals(ResultState.PENDING_DEPENDENCY, buildRunner.getBuildStatus(projectC, 1));
        assertEquals(ResultState.PENDING_DEPENDENCY, buildRunner.getBuildStatus(projectD, 1));

        projectB.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(projectB.getName(), 1);
        xmlRpcHelper.waitForBuildInProgress(projectC.getName(), 1);

        assertEquals(ResultState.SUCCESS, buildRunner.getBuildStatus(projectB, 1));
        assertEquals(ResultState.IN_PROGRESS, buildRunner.getBuildStatus(projectC, 1));
        assertEquals(ResultState.PENDING_DEPENDENCY, buildRunner.getBuildStatus(projectD, 1));

        projectC.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(projectC.getName(), 1);
        xmlRpcHelper.waitForBuildInProgress(projectD.getName(), 1);

        assertEquals(ResultState.SUCCESS, buildRunner.getBuildStatus(projectB, 1));
        assertEquals(ResultState.SUCCESS, buildRunner.getBuildStatus(projectC, 1));
        assertEquals(ResultState.IN_PROGRESS, buildRunner.getBuildStatus(projectD, 1));

        projectD.releaseBuild();
        xmlRpcHelper.waitForBuildToComplete(projectD.getName(), 1);
    }

    public void testRebuildStopsOnFailure() throws Exception
    {
        ProjectConfigurationHelper projectA = projects.createFailAntProject(projectName + "A");
        insertProject(projectA);

        WaitAntProject projectB = projects.createWaitAntProject(tmpDir, projectName + "B");
        projectB.addDependency(projectA);
        insertProject(projectB);

        buildRunner.triggerRebuild(projectB.getConfig());

        xmlRpcHelper.waitForBuildToComplete(projectA.getName(), 1);
        assertEquals(ResultState.FAILURE, buildRunner.getBuildStatus(projectA, 1));

        xmlRpcHelper.waitForBuildToComplete(projectB.getName(), 1);
        assertEquals(ResultState.ERROR, buildRunner.getBuildStatus(projectB, 1));

        // We would normally have to release projectBs' build.  However, it did not run,
        // because projectA failed. 
    }
}