package com.zutubi.pulse.master.build.control;

import com.zutubi.events.DefaultEventManager;
import com.zutubi.pulse.core.Bootstrapper;
import com.zutubi.pulse.core.PulseExecutionContext;
import com.zutubi.pulse.core.RecipeRequest;
import com.zutubi.pulse.core.engine.api.Feature;
import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.core.events.*;
import com.zutubi.pulse.core.model.CommandResult;
import com.zutubi.pulse.core.model.RecipeResult;
import com.zutubi.pulse.core.scm.TestScmClient;
import com.zutubi.pulse.core.scm.api.Revision;
import com.zutubi.pulse.core.scm.config.api.ScmConfiguration;
import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.pulse.master.agent.Agent;
import com.zutubi.pulse.master.agent.AgentService;
import com.zutubi.pulse.master.agent.AgentStatus;
import com.zutubi.pulse.master.agent.Host;
import com.zutubi.pulse.master.bootstrap.Data;
import com.zutubi.pulse.master.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.master.bootstrap.SimpleMasterConfigurationManager;
import com.zutubi.pulse.master.build.log.RecipeLogger;
import com.zutubi.pulse.master.build.queue.RecipeAssignmentRequest;
import com.zutubi.pulse.master.build.queue.RecipeQueue;
import com.zutubi.pulse.master.events.build.RecipeAssignedEvent;
import com.zutubi.pulse.master.model.*;
import com.zutubi.pulse.master.scm.MasterScmClientFactory;
import com.zutubi.pulse.master.tove.config.agent.AgentConfiguration;
import com.zutubi.pulse.master.tove.config.project.AnyCapableAgentRequirements;
import com.zutubi.pulse.master.tove.config.project.BuildStageConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.tove.config.project.hooks.BuildHookManager;
import com.zutubi.pulse.master.tove.config.project.types.CustomTypeConfiguration;
import com.zutubi.pulse.servercore.CheckoutBootstrapper;
import com.zutubi.pulse.servercore.bootstrap.MasterUserPaths;
import com.zutubi.util.io.FileSystemUtils;
import org.mockito.Matchers;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.zutubi.pulse.core.engine.api.BuildProperties.*;
import static org.mockito.Mockito.*;

public class RecipeControllerTest extends PulseTestCase
{
    private static final String STAGE_NAME = "root stage";

    private File recipeDir;

    private RecordingRecipeQueue recipeQueue;
    private BuildManager buildManager;
    private AgentService buildService;

    private RecipeResult recipeResult;
    private RecipeResultNode stageResult;
    private RecipeAssignmentRequest assignmentRequest;
    private RecipeController recipeController;
    private RecipeDispatchService recipeDispatchService;

    protected void setUp() throws Exception
    {
        super.setUp();
        recipeDir = FileSystemUtils.createTempDir(RecipeControllerTest.class.getName(), "");

        RecipeResultCollector resultCollector = mock(RecipeResultCollector.class);
        stub(resultCollector.getRecipeDir(Matchers.<BuildResult>anyObject(), anyLong())).toReturn(recipeDir);
        recipeQueue = new RecordingRecipeQueue();
        buildManager = mock(BuildManager.class);
        buildService = mock(AgentService.class);
        RecipeLogger logger = mock(RecipeLogger.class);

        recipeResult = new RecipeResult("root recipe");
        recipeResult.setId(100);
        stageResult = new RecipeResultNode(STAGE_NAME, 1, recipeResult);
        stageResult.setId(101);

        RecipeRequest recipeRequest = new RecipeRequest(makeContext("project", recipeResult.getId(), recipeResult.getRecipeName()));
        Project project = new Project();
        ProjectConfiguration projectConfig = new ProjectConfiguration();
        projectConfig.getStages().put(STAGE_NAME, new BuildStageConfiguration(STAGE_NAME));
        projectConfig.setType(new CustomTypeConfiguration());
        project.setConfig(projectConfig);
        BuildResult build = new BuildResult(new ManualTriggerBuildReason("user"), project, 1, false);
        build.setRevision(new Revision(1));
        assignmentRequest = new RecipeAssignmentRequest(project, new AnyCapableAgentRequirements(), null, recipeRequest, null);
        MasterConfigurationManager configurationManager = new SimpleMasterConfigurationManager()
        {
            public File getDataDirectory()
            {
                return new File("test");
            }

            public MasterUserPaths getUserPaths()
            {
                return new Data(getDataDirectory());
            }
        };

        recipeDispatchService = mock(RecipeDispatchService.class);

        MasterScmClientFactory scmClientFactory = mock(MasterScmClientFactory.class);
        stub(scmClientFactory.createClient((ProjectConfiguration) anyObject(), (ScmConfiguration) anyObject())).toReturn(new TestScmClient());
        recipeController = new RecipeController(projectConfig, build, stageResult, assignmentRequest, null, logger, resultCollector, 0);
        recipeController.setRecipeQueue(recipeQueue);
        recipeController.setBuildManager(buildManager);
        recipeController.setEventManager(new DefaultEventManager());
        recipeController.setConfigurationManager(configurationManager);
        recipeController.setResourceManager(new DefaultResourceManager());
        recipeController.setRecipeDispatchService(recipeDispatchService);
        recipeController.setScmClientFactory(scmClientFactory);
        recipeController.setBuildHookManager(mock(BuildHookManager.class));
    }

    protected void tearDown() throws Exception
    {
        removeDirectory(recipeDir);
        super.tearDown();
    }

    public void testIgnoresOtherRecipes()
    {
        assertFalse(recipeController.matchesRecipeEvent(new RecipeCommencedEvent(this, 1, recipeResult.getId() + 1, "yay", Collections.<String, String>emptyMap(), 0)));
    }

    public void testDispatchRequest()
    {
        // Initialising should cause a dispatch request, and should initialise the bootstrapper
        Bootstrapper bootstrapper = new CheckoutBootstrapper("project");
        recipeController.initialise(bootstrapper);
        assertTrue(recipeQueue.hasDispatched(recipeResult.getId()));
        RecipeAssignmentRequest dispatched = recipeQueue.getRequest(recipeResult.getId());
        assertSame(assignmentRequest, dispatched);
        assertSame(assignmentRequest.getRequest().getBootstrapper(), bootstrapper);
    }

    public void testAssignedEvent()
    {
        testDispatchRequest();

        Host host = mock(Host.class);

        Agent agent = mock(Agent.class);
        stub(agent.getService()).toReturn(buildService);
        stub(agent.getName()).toReturn("testagent");
        stub(agent.isOnline()).toReturn(true);
        stub(agent.getStatus()).toReturn(AgentStatus.IDLE);
        stub(agent.getConfig()).toReturn(new AgentConfiguration());
        stub(agent.getHost()).toReturn(host);

        // After dispatching, the controller should handle a dispatched event
        // by recording the build service on the result node.
        RecipeAssignedEvent event = new RecipeAssignedEvent(this, new RecipeRequest(makeContext("project", recipeResult.getId(), "test")), agent);
        assertTrue(recipeController.matchesRecipeEvent(event));
        recipeController.handleRecipeEvent(event);
        assertEquals(agent.getName(), stageResult.getAgentName());

        verify(buildManager, times(1)).save(stageResult);
        verify(recipeDispatchService, times(1)).dispatch(event);
    }

    public void testCommencedEvent()
    {
        testAssignedEvent();

        // A recipe commence event should change the result state, and record
        // the start time.
        RecipeCommencedEvent event = new RecipeCommencedEvent(this, 1, recipeResult.getId(), recipeResult.getRecipeName(), Collections.<String, String>emptyMap(), 10101);
        assertTrue(recipeController.matchesRecipeEvent(event));
        recipeController.handleRecipeEvent(event);
        assertEquals(ResultState.IN_PROGRESS, recipeResult.getState());
    }

    public void testCommandCommencedEvent()
    {
        testCommencedEvent();

        // A command commenced event should result in a new command result
        // with the correct name, state and start time.
        CommandCommencedEvent event = new CommandCommencedEvent(this, 1, recipeResult.getId(), "test command", 555);
        assertTrue(recipeController.matchesRecipeEvent(event));
        recipeController.handleRecipeEvent(event);

        List<CommandResult> commandResults = recipeResult.getCommandResults();
        assertTrue(commandResults.size() > 0);
        CommandResult result = commandResults.get(commandResults.size() - 1);
        result.setOutputDir("dummy");
        assertEquals(ResultState.IN_PROGRESS, result.getState());
        assertEquals(event.getName(), result.getCommandName());
    }

    public void testCommandCompletedEvent()
    {
        testCommandCommencedEvent();

        // A command completed event should result in the command result
        // in the event being applied to the recipe result.
        CommandResult commandResult = new CommandResult("test command");
        commandResult.commence();
        commandResult.setOutputDir("dummy");
        commandResult.complete();
        CommandCompletedEvent event = new CommandCompletedEvent(this, 1, recipeResult.getId(), commandResult);

        assertTrue(recipeController.matchesRecipeEvent(event));
        recipeController.handleRecipeEvent(event);
        List<CommandResult> commandResults = recipeResult.getCommandResults();
        assertTrue(commandResults.size() > 0);
        CommandResult result = commandResults.get(commandResults.size() - 1);
        assertTrue(result.completed());
        assertEquals("dummy", result.getOutputDir());
    }

    public void testRecipeCompletedEvent()
    {
        testCommandCompletedEvent();

        // A recipe completed event should result in the recipe result
        // details being applied, and the controller should then be
        // finished
        RecipeResult result = new RecipeResult(recipeResult.getRecipeName());
        result.setId(recipeResult.getId());
        result.commence(1234);
        result.complete();
        RecipeCompletedEvent event = new RecipeCompletedEvent(this, 1, result);

        assertTrue(recipeController.matchesRecipeEvent(event));
        recipeController.handleRecipeEvent(event);
        assertEquals(ResultState.SUCCESS, recipeResult.getState());
        assertTrue(recipeController.isFinished());
    }

    public void testErrorBeforeDispatched()
    {
        testDispatchRequest();

        sendError();

        assertNull(stageResult.getAgentName());
        assertTrue(recipeResult.getStamps().started());
        assertTrue(recipeResult.getStamps().ended());
    }

    public void testErrorBeforeCommenced()
    {
        testAssignedEvent();

        sendError();

        assertTrue(recipeResult.getStamps().started());
        assertTrue(recipeResult.getStamps().ended());
    }

    public void testErrorAfterCommenced()
    {
        testCommencedEvent();

        sendError();

        // Should have start and end times.
        assertTrue(recipeResult.getStamps().started());
        assertTrue(recipeResult.getStamps().ended());
    }

    public void testErrorMidCommand()
    {
        testCommandCommencedEvent();

        sendError();

        // Command should be aborted
        List<CommandResult> results = recipeResult.getCommandResults();
        assertTrue(results.size() > 0);
        CommandResult lastResult = results.get(results.size() - 1);
        assertEquals(ResultState.ERROR, lastResult.getState());
    }

    public void testErrorBetweenCommands()
    {
        testCommandCompletedEvent();

        sendError();

        // Command should not be affected
        List<CommandResult> results = recipeResult.getCommandResults();
        assertTrue(results.size() > 0);
        CommandResult lastResult = results.get(results.size() - 1);
        assertEquals(ResultState.SUCCESS, lastResult.getState());
    }

    private PulseExecutionContext makeContext(String project, long id, String recipeName)
    {
        PulseExecutionContext context = new PulseExecutionContext();
        context.addString(NAMESPACE_INTERNAL, PROPERTY_PROJECT, project);
        context.addString(NAMESPACE_INTERNAL, PROPERTY_RECIPE_ID, Long.toString(id));
        context.addString(NAMESPACE_INTERNAL, PROPERTY_RECIPE, recipeName);
        return context;
    }

    private RecipeErrorEvent sendError()
    {
        RecipeErrorEvent error = new RecipeErrorEvent(this, 1, recipeResult.getId(), "test error message", false);
        assertTrue(recipeController.matchesRecipeEvent(error));
        recipeController.handleRecipeEvent(error);
        assertErrorDetailsSaved(error);
        return error;
    }

    private void assertErrorDetailsSaved(RecipeErrorEvent error)
    {
        assertEquals(ResultState.ERROR, recipeResult.getState());
        assertEquals(error.getErrorMessage(), recipeResult.getFeatures(Feature.Level.ERROR).get(0).getSummary());
        verify(buildManager, atLeastOnce()).save(recipeResult);
    }

    class RecordingRecipeQueue implements RecipeQueue
    {
        private Map<Long, RecipeAssignmentRequest> dispatched = new TreeMap<Long, RecipeAssignmentRequest>();

        public void enqueue(RecipeAssignmentRequest request)
        {
            dispatched.put(request.getRequest().getId(), request);
        }

        public List<RecipeAssignmentRequest> takeSnapshot()
        {
            throw new RuntimeException("Method not implemented.");
        }

        public boolean cancelRequest(long id)
        {
            throw new RuntimeException("Method not implemented.");
        }

        public void start()
        {
            throw new RuntimeException("Method not implemented.");
        }

        public void stop()
        {
            throw new RuntimeException("Method not implemented.");
        }

        public boolean isRunning()
        {
            throw new RuntimeException("Method not implemented.");
        }

        public boolean hasDispatched(long recipeId)
        {
            return dispatched.containsKey(recipeId);
        }

        public RecipeAssignmentRequest getRequest(long recipeId)
        {
            return dispatched.get(recipeId);
        }
    }
}
