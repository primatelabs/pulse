package com.zutubi.pulse.master.trigger;

import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.pulse.master.model.BuildManager;
import com.zutubi.pulse.master.model.BuildResult;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.pulse.master.model.ProjectManager;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.tove.config.project.triggers.ProjectStateTriggerConditionConfiguration;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ProjectStateTriggerConditionTest extends PulseTestCase
{
    private static final int PROJECT_ID = 1;
    
    private Project project;
    private BuildManager buildManager;
    private ProjectManager projectManager;
    private ProjectStateTriggerConditionConfiguration config;
    private ProjectStateTriggerCondition condition;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        project = new Project();
        project.setId(PROJECT_ID);
        ProjectConfiguration projectConfig = new ProjectConfiguration("test");
        projectConfig.setProjectId(PROJECT_ID);
        project.setConfig(projectConfig);

        buildManager = mock(BuildManager.class);

        projectManager = mock(ProjectManager.class);
        stub(projectManager.getProject(PROJECT_ID, true)).toReturn(project);

        config = new ProjectStateTriggerConditionConfiguration();
        config.setProject(project.getConfig());
        config.setStates(Arrays.asList(ResultState.SUCCESS));

        condition = new ProjectStateTriggerCondition(config);
        condition.setBuildManager(buildManager);
        condition.setProjectManager(projectManager);
    }

    public void testNoBuilds()
    {
        assertFalse(condition.satisfied(new Project()));
    }

    public void testBuildInAnotherState()
    {
        BuildResult result = new BuildResult();
        result.setState(ResultState.ERROR);
        stub(buildManager.getLatestCompletedBuildResult(project)).toReturn(result);
        assertFalse(condition.satisfied(new Project()));
    }

    public void testBuildInRequiredState()
    {
        BuildResult result = new BuildResult();
        result.setState(ResultState.SUCCESS);
        stub(buildManager.getLatestCompletedBuildResult(project)).toReturn(result);
        assertTrue(condition.satisfied(new Project()));
    }
}