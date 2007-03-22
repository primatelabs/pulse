package com.zutubi.pulse.condition;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.FullConstraintMatcher;
import com.mockobjects.dynamic.Mock;
import com.zutubi.pulse.core.model.ResultState;
import com.zutubi.pulse.model.*;
import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.util.Constants;

import java.util.Arrays;

/**
 */
public class UnsuccessfulCountDaysValueTest extends PulseTestCase
{
    private Mock mockBuildManager;
    private BuildSpecification spec;
    private UnsuccessfulCountDaysValue value;
    private Project project;

    protected void setUp() throws Exception
    {
        mockBuildManager = new Mock(BuildManager.class);
        project = new Project("test", "test");
        project.setId(99);
        spec = new BuildSpecification("hooray");
        spec.setId(1234);
        spec.getPname().setId(2);
        project.addBuildSpecification(spec);
        value = new UnsuccessfulCountDaysValue();
    }

    public void testNullBuild()
    {
        setBuildManager();
        assertEquals(0, value.getValue(null, null));
    }

    public void testSuccessfulBuild()
    {
        setBuildManager();
        BuildResult result = createBuild(1);
        result.setState(ResultState.SUCCESS);
        assertEquals(0, value.getValue(result, null));
    }

    public void testNoPreviousSuccessFirstFailure()
    {
        BuildResult failure = createBuild(20);
        setupCalls(null, failure);
        setBuildManager();
        assertEquals(0, value.getValue(failure, null));
    }

    public void testNoPreviousSuccess()
    {
        BuildResult firstFailure = createBuild(2);
        firstFailure.getStamps().setEndTime(System.currentTimeMillis() - Constants.DAY * 3 - 7200000);
        BuildResult failure = createBuild(20);
        setupCalls(null, firstFailure);
        setBuildManager();
        assertEquals(3, value.getValue(failure, null));
    }

    public void testPreviousSuccessFirstFailure()
    {
        BuildResult success = createBuild(3);
        success.setState(ResultState.SUCCESS);
        BuildResult failure = createBuild(20);
        setupCalls(success, failure);
        setBuildManager();
        assertEquals(0, value.getValue(failure, null));
    }

    public void testPreviousSuccess()
    {
        BuildResult success = createBuild(3);
        success.setState(ResultState.SUCCESS);
        BuildResult firstFailure = createBuild(5);
        firstFailure.getStamps().setEndTime(System.currentTimeMillis() - Constants.DAY * 4);
        setupCalls(success, firstFailure);
        setBuildManager();
        assertEquals(4, value.getValue(createBuild(20), null));
    }

    private BuildResult createBuild(long number)
    {
        BuildResult buildResult = new BuildResult(new ManualTriggerBuildReason("w00t"), project, spec, number, false);
        buildResult.complete(System.currentTimeMillis());
        buildResult.setState(ResultState.FAILURE);
        return buildResult;
    }

    private void setupCalls(BuildResult lastSuccess, BuildResult... firstFailure)
    {
        mockBuildManager.expectAndReturn("getLatestSuccessfulBuildResult", spec, lastSuccess);
        // project, spec.getPname(), null, lastSuccess == null ? 1 : lastSuccess.getNumber() + 1, -1, 1, 1, false, false
        long number = lastSuccess == null ? 1 : lastSuccess.getNumber() + 1;
        mockBuildManager.expectAndReturn("querySpecificationBuilds", new FullConstraintMatcher(new Constraint[]{ C.eq(project), C.eq(spec.getPname()), C.IS_NULL, C.eq(number), C.eq(-1L), C.eq(0), C.eq(1), C.eq(false), C.eq(false) }), Arrays.asList(firstFailure));
    }

    private void setBuildManager()
    {
        value.setBuildManager((BuildManager) mockBuildManager.proxy());
    }
}
