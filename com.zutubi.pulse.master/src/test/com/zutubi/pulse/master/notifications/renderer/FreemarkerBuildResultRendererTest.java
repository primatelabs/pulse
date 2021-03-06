package com.zutubi.pulse.master.notifications.renderer;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.zutubi.pulse.core.engine.api.Feature;
import com.zutubi.pulse.core.model.*;
import com.zutubi.pulse.core.scm.api.Revision;
import com.zutubi.pulse.core.test.TestUtils;
import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.pulse.master.model.*;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.tove.config.user.UserConfiguration;
import com.zutubi.pulse.master.webwork.Urls;
import com.zutubi.pulse.servercore.bootstrap.ConfigurableSystemPaths;
import com.zutubi.pulse.servercore.bootstrap.MasterUserPaths;
import com.zutubi.util.Constants;
import com.zutubi.util.io.FileSystemUtils;
import com.zutubi.util.io.IOUtils;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;

import static com.zutubi.pulse.core.postprocessors.api.TestStatus.ERROR;
import static com.zutubi.pulse.core.postprocessors.api.TestStatus.FAILURE;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

public class FreemarkerBuildResultRendererTest extends PulseTestCase
{
    private static final List<PersistentChangelist> NO_CHANGES = Collections.emptyList();
    
    private boolean generate = false;

    private FreemarkerBuildResultRenderer renderer;

    protected void setUp() throws Exception
    {
        super.setUp();
        renderer = new FreemarkerBuildResultRenderer();
        File pulseRoot = new File(TestUtils.getPulseRoot(), "com.zutubi.pulse.master/src/templates");

        Configuration freemarkerConfiguration = new Configuration();
        freemarkerConfiguration.setDirectoryForTemplateLoading(pulseRoot);
        freemarkerConfiguration.setObjectWrapper(new DefaultObjectWrapper());
        freemarkerConfiguration.addAutoInclude("macro.ftl");
        freemarkerConfiguration.setSharedVariable("externalUrls", Urls.getBaselessInstance());
        renderer.setFreemarkerConfiguration(freemarkerConfiguration);
    }

    protected void tearDown() throws Exception
    {
        renderer = null;
        System.clearProperty(FreemarkerBuildResultRenderer.FEATURE_LIMIT_PROPERTY);
        super.tearDown();
    }

    public void testGetUserTemplate() throws IOException
    {
        File tempDir = FileSystemUtils.createTempDir(getName(), "tmp");
        try
        {
            File systemTemplateDir = new File(tempDir, "system");
            ConfigurableSystemPaths systemPaths = new ConfigurableSystemPaths();
            systemPaths.setTemplateRootStrings(asList(systemTemplateDir.getAbsolutePath()));
            renderer.setSystemPaths(systemPaths);

            File userTemplateDir = new File(tempDir, "user");
            MasterUserPaths userPaths = mock(MasterUserPaths.class);
            stub(userPaths.getUserTemplateRoot()).toReturn(userTemplateDir);
            renderer.setUserPaths(userPaths);

            File notificationsDir = new File(userTemplateDir, "notifications");
            File projectNotificationsDir = new File(notificationsDir, "project-builds");
            assertTrue(projectNotificationsDir.mkdirs());

            File customTemplate = new File(projectNotificationsDir, "custom.ftl");
            Files.write("dummy", customTemplate, Charset.defaultCharset());
            File customProperties = new File(projectNotificationsDir, "custom.properties");
            Files.write("display=test display name\ntype=text/html", customProperties, Charset.defaultCharset());

            List<TemplateInfo> available = renderer.getAvailableTemplates(false);
            assertEquals(1, available.size());
            TemplateInfo template = available.get(0);
            assertEquals("custom", template.getTemplate());
            assertEquals("test display name", template.getDisplay());
            assertEquals("text/html", template.getMimeType());
        }
        finally
        {
            removeDirectory(tempDir);
        }
    }
    public void testBasicSuccess() throws Exception
    {
        BuildResult result = createSuccessfulBuild();
        createAndVerify("basic", "http://test.url:8080", result, NO_CHANGES);
    }

    public void testWithChanges() throws Exception
    {
        List<PersistentChangelist> changes = getChanges();
        BuildResult result = createBuildWithChanges(changes);

        createAndVerify("changes", "http://another.url", result, changes);
    }

    public void testWithErrors() throws Exception
    {
        errorsHelper("plain-text-email");
    }

    public void testWithErrorLimit() throws Exception
    {
        List<PersistentChangelist> changes = getChanges();
        BuildResult result = createBuildWithErrors(changes);
        System.setProperty(FreemarkerBuildResultRenderer.FEATURE_LIMIT_PROPERTY, "7");
        createAndVerify("excesserrors", "plain-text-email", "http://another.url", result, changes);
    }

    public void testSimpleInstantBasic() throws Exception
    {
        BuildResult result = createSuccessfulBuild();
        createAndVerify("basic", "simple-instant-message", "http://test.url:8080", result, NO_CHANGES);
    }

    public void testSimpleInstantError() throws Exception
    {
        errorsHelper("simple-instant-message");
    }

    public void testDetailedInstantBasic() throws Exception
    {
        BuildResult result = createSuccessfulBuild();
        createAndVerify("basic", "detailed-instant-message", "http://test.url:8080", result, NO_CHANGES);
    }

    public void testDetailedInstantError() throws Exception
    {
        errorsHelper("detailed-instant-message");
    }

    public void testDetailedInstantSingleStageFailure() throws Exception
    {
        singleStageFailureHelper("detailed-instant-message");
    }

    public void testDetailedInstantFailures() throws Exception
    {
        failuresHelper("detailed-instant-message", false);
    }

    public void testHTMLWithChanges() throws Exception
    {
        List<PersistentChangelist> changes = getChanges();
        BuildResult result = createBuildWithChanges(changes);

        createAndVerify("changes", "html-email", "http://another.url", result, changes);
    }

    public void testHTMLWithErrors() throws Exception
    {
        errorsHelper("html-email");
    }

    public void testHTMLWithErrorLimit() throws Exception
    {
        List<PersistentChangelist> changes = getChanges();
        BuildResult result = createBuildWithErrors(changes);
        System.setProperty(FreemarkerBuildResultRenderer.FEATURE_LIMIT_PROPERTY, "7");
        createAndVerify("excesserrors", "html-email", "http://another.url", result, changes);
    }


    public void testWithFailures() throws Exception
    {
        failuresHelper("plain-text-email", false);
    }

    public void testHTMLWithFailures() throws Exception
    {
        failuresHelper("html-email", false);
    }

    public void testWithSingleStageFailure() throws Exception
    {
        singleStageFailureHelper("plain-text-email");
    }

    public void testHTMLWithSingleStageFailure() throws Exception
    {
        singleStageFailureHelper("html-email");
    }

    public void testWithExcessFailures() throws Exception
    {
        failuresHelper("plain-text-email", true);
    }

    public void testHTMLWithExcessFailures() throws Exception
    {
        failuresHelper("html-email", true);
    }

    public void testPersonalSimpleInstant() throws Exception
    {
        personalBuildHelper("simple-instant-message");
    }

    public void testPersonalDetailedInstant() throws Exception
    {
        personalBuildHelper("detailed-instant-message");
    }

    public void testPersonalPlainTextEmail() throws Exception
    {
        personalBuildHelper("plain-text-email");
    }

    public void testPersonalHTMLEmail() throws Exception
    {
        personalBuildHelper("html-email");
    }

    public void testProjectOverviewSuccess() throws Exception
    {
        BuildResult result = createSuccessfulBuild();
        createAndVerify("basic", "html-project-overview", "http://test.url:8080", result, NO_CHANGES);
    }

    public void testProjectOverviewFailureNoPreviousSuccess() throws Exception
    {
        BuildResult result = createSuccessfulBuild();
        result.failure("i failed");
        createAndVerify("failednosuccess", "html-project-overview", "http://test.url:8080", result, NO_CHANGES, null, 0, 0);
    }

    public void testProjectOverviewFailurePreviousSuccess() throws Exception
    {
        Project project = createProject();
        BuildResult previous = new BuildResult(new TriggerBuildReason("scm trigger"), project, 90, false);
        initialiseResult(previous);
        previous.setId(previous.getId() - 1);
        previous.getStamps().setStartTime(System.currentTimeMillis() - Constants.DAY * 3);
        BuildResult result = createSuccessfulBuild();
        result.failure("i failed");
        createAndVerify("failedsuccess", "html-project-overview", "http://test.url:8080", result, NO_CHANGES, previous, 33, 10);
    }

    public void testRecipeWithFailedBuildsMissingDetails() throws Exception
    {
        // CIB-2991
        BuildResult result = createSuccessfulBuild();
        final RecipeResult node = result.getStages().get(0).getResult();
        node.setTestSummary(new TestResultSummary(0, 1, 1, 0, 5));
        createAndVerify("missingtests", "plain-text-email", "http://test.url:8080", result, NO_CHANGES);
    }

    private void errorsHelper(String type) throws Exception
    {
        List<PersistentChangelist> changes = getChanges();
        BuildResult result = createBuildWithErrors(changes);
        createAndVerify("errors", type, "http://another.url", result, changes);
    }

    private BuildResult createBuildWithErrors(List<PersistentChangelist> changes)
    {
        BuildResult result = createBuildWithChanges(changes);
        result.error("test error message");
        result.addFeature(Feature.Level.WARNING, "warning message on result");
        RecipeResultNode firstStage = result.getStages().get(0);
        firstStage.getResult().error("test recipe error message");
        RecipeResultNode secondStage = result.getStages().get(1);
        secondStage.getResult().failure("test recipe failure message with the unfortunate need to wrap because it is really quite ridiculously long");

        RecipeResultNode thirdStages = result.getStages().get(2);
        RecipeResult thirdResult = thirdStages.getResult();

        CommandResult command = new CommandResult("test command");
        command.setId(200);
        command.error("bad stuff happened, so wrap this: 000000000000000000000000000000000000000000000000000000000000000000000");
        command.complete();
        thirdResult.add(command);

        command = new CommandResult("artifact command");
        command.setId(101);
        command.failure("artifacts let me down");

        StoredFileArtifact artifact = new StoredFileArtifact("first-artifact/testpath");
        artifact.addFeature(new PersistentFeature(Feature.Level.INFO, "info message"));
        artifact.addFeature(new PersistentFeature(Feature.Level.ERROR, "error message"));
        artifact.addFeature(new PersistentPlainFeature(Feature.Level.WARNING, "warning message", 19));
        command.addArtifact(new StoredArtifact("first-artifact", artifact));

        artifact = new StoredFileArtifact("second-artifact/this/time/a/very/very/very/very/long/pathname/which/will/look/ugly/i/have/no/doubt");
        artifact.addFeature(new PersistentPlainFeature(Feature.Level.ERROR, "error 1", 1000000));
        artifact.addFeature(new PersistentFeature(Feature.Level.ERROR, "error 2"));
        artifact.addFeature(new PersistentFeature(Feature.Level.ERROR, "error 3: in this case a longer error message so i can see how the wrapping works on the artifact messages"));
        command.addArtifact(new StoredArtifact("second-artifact", artifact));

        command.complete();
        thirdResult.add(command);
        return result;
    }

    private void failuresHelper(String type, boolean excessFailures) throws Exception
    {
        BuildResult result = createSuccessfulBuild();
        result.failure("test failed tests");

        RecipeResultNode firstStage = result.getStages().get(0);
        firstStage.getResult().failure("tests failed dude");

        RecipeResultNode secondStage = result.getStages().get(1);
        secondStage.getResult().failure("tests failed second dude");

        RecipeResultNode thirdStage = result.getStages().get(2);
        RecipeResult thirdResult = thirdStage.getResult();

        CommandResult command = new CommandResult("failing tests");
        command.setId(100);
        command.failure("tests let me down");

        PersistentTestSuiteResult tests = new PersistentTestSuiteResult();

        StoredFileArtifact artifact = new StoredFileArtifact("first-artifact/testpath");
        PersistentTestSuiteResult rootSuite = new PersistentTestSuiteResult("root test suite");
        rootSuite.add(new PersistentTestCaseResult("2 failed", 0, FAILURE, "a failure message which is bound to be detailed, potentially to the extreme but in this case just to wrap a bit"));
        rootSuite.add(new PersistentTestCaseResult("3 error", 0, ERROR, "short error"));

        PersistentTestSuiteResult nestedSuite = new PersistentTestSuiteResult("nested suite");
        nestedSuite.add(new PersistentTestCaseResult("n1 failed", 0, FAILURE, "a failure message which is bound to be detailed, potentially to the extreme but in this case just to wrap a bit"));
        nestedSuite.add(new PersistentTestCaseResult("n2 error", 0, ERROR, "short error"));
        rootSuite.add(nestedSuite);

        tests.add(rootSuite);
        command.addArtifact(new StoredArtifact("first-artifact", artifact));

        artifact = new StoredFileArtifact("second-artifact/this/time/a/very/very/very/very/long/pathname/which/will/look/ugly/i/have/no/doubt");
        tests.add(new PersistentTestCaseResult("test case at top level", 0, FAILURE, "and i failed"));
        command.addArtifact(new StoredArtifact("second-artifact", artifact));

        command.complete();
        thirdResult.add(command);
        thirdResult.setFailedTestResults(tests);
        TestResultSummary summary = tests.getSummary();
        if(excessFailures)
        {
            summary.setFailures(summary.getFailures() + 123);
        }

        thirdResult.setTestSummary(summary);
        createAndVerify((excessFailures ? "excess" : "") + "failures", type, "http://host.url", result);
    }

    private void singleStageFailureHelper(String type) throws Exception
    {
        BuildResult result = createSuccessfulBuild();
        result.failure("a stage failed");

        RecipeResultNode secondStage = result.getStages().get(1);
        RecipeResult secondResult = secondStage.getResult();

        CommandResult command = new CommandResult("failing tests");
        command.setId(23);
        command.failure("i failed");
        command.complete();
        secondResult.add(command);
        createAndVerify("singelstagefailure", type, "http://host.url", result);
    }

    private void personalBuildHelper(String type) throws Exception
    {
        UserConfiguration userConfig = new UserConfiguration("jason", "Jason Sankey");
        User user = new User();
        user.setConfig(userConfig);
        Project project = createProject();
        BuildResult result = new BuildResult(new PersonalBuildReason(user.getLogin()), user, project, 12);
        initialiseResult(result);

        result.failure("test failed tests");

        RecipeResultNode firstStage = result.getStages().get(0);
        firstStage.getResult().failure("tests failed dude");

        RecipeResultNode secondStage = result.getStages().get(1);
        secondStage.getResult().failure("tests failed second dude");

        RecipeResultNode thirdStage = result.getStages().get(2);
        RecipeResult thirdResult = thirdStage.getResult();

        CommandResult command = new CommandResult("failing tests");
        command.setId(22);
        command.failure("tests let me down");

        PersistentTestSuiteResult tests = new PersistentTestSuiteResult();

        StoredFileArtifact artifact = new StoredFileArtifact("first-artifact/testpath");
        PersistentTestSuiteResult rootSuite = new PersistentTestSuiteResult("root test suite");
        rootSuite.add(new PersistentTestCaseResult("2 failed", 0, FAILURE, "a failure message which is bound to be detailed, potentially to the extreme but in this case just to wrap a bit"));
        rootSuite.add(new PersistentTestCaseResult("3 error", 0, ERROR, "short error"));

        PersistentTestSuiteResult nestedSuite = new PersistentTestSuiteResult("nested suite");
        nestedSuite.add(new PersistentTestCaseResult("n1 failed", 0, FAILURE, "a failure message which is bound to be detailed, potentially to the extreme but in this case just to wrap a bit"));
        nestedSuite.add(new PersistentTestCaseResult("n2 error", 0, ERROR, "short error"));
        rootSuite.add(nestedSuite);

        tests.add(rootSuite);
        command.addArtifact(new StoredArtifact("first-artifact", artifact));

        artifact = new StoredFileArtifact("second-artifact/this/time/a/very/very/very/very/long/pathname/which/will/look/ugly/i/have/no/doubt");
        tests.add(new PersistentTestCaseResult("test case at top level", 0, FAILURE, "and i failed"));
        command.addArtifact(new StoredArtifact("second-artifact", artifact));

        command.complete();
        thirdResult.add(command);
        thirdResult.setFailedTestResults(tests);
        TestResultSummary summary = tests.getSummary();
        thirdResult.setTestSummary(summary);
        createAndVerify("personal", type, "http://host.url", result);
    }

    private Project createProject()
    {
        Project project = new Project();
        ProjectConfiguration config = new ProjectConfiguration();
        config.setName("test project");
        project.setConfig(config);
        return project;
    }

    private BuildResult createBuildWithChanges(List<PersistentChangelist> changes)
    {
        BuildResult result = createSuccessfulBuild();
        result.setRevision(new Revision("656"));
        for(PersistentChangelist change: changes)
        {
            change.setResultId(result.getId());
        }

        return result;
    }

    private List<PersistentChangelist> getChanges()
    {
        List<PersistentChangelist> changes = new LinkedList<PersistentChangelist>();
        PersistentChangelist list = new PersistentChangelist(new Revision("655"), 324252, "test author", "CIB-1: short comment", Collections.<PersistentFileChange>emptyList());
        changes.add(list);
        list = new PersistentChangelist(new Revision("656"), 310000, "author2", "this time we will use a longer comment to make sure that the renderer is applying some sort of trimming to the resulting output dadada da dadad ad ad adadad ad ad ada d adada dad ad ad d ad ada da d", Collections.<PersistentFileChange>emptyList());
        changes.add(list);
        return changes;
    }

    private BuildResult createSuccessfulBuild()
    {
        Project project = createProject();
        BuildResult result = new BuildResult(new TriggerBuildReason("scm trigger"), project, 101, false);
        initialiseResult(result);
        return result;
    }

    private void initialiseResult(BuildResult result)
    {
        result.setId(11);
        result.commence(10000);

        RecipeResult recipeResult = new RecipeResult("first recipe");
        recipeResult.commence(10001);
        recipeResult.complete();
        recipeResult.getStamps().setEndTime(99998);
        RecipeResultNode node = new RecipeResultNode("first stage", 1, recipeResult);
        result.addStage(node);

        recipeResult = new RecipeResult("second recipe");
        recipeResult.commence(10001);
        recipeResult.complete();
        recipeResult.getStamps().setEndTime(99998);
        node = new RecipeResultNode("second stage", 2, recipeResult);
        result.addStage(node);

        recipeResult = new RecipeResult("third recipe");
        recipeResult.commence(10002);
        recipeResult.complete();
        node = new RecipeResultNode("third stage", 3, recipeResult);
        recipeResult.getStamps().setEndTime(99999);
        result.addStage(node);

        result.complete();
        result.getStamps().setEndTime(100000);
    }

    protected void createAndVerify(String expectedName, String baseUrl, BuildResult result, List<PersistentChangelist> changelists) throws IOException
    {
        createAndVerify(expectedName, "plain-text-email", baseUrl, result, changelists);
    }

    protected void createAndVerify(String expectedName, String type, String baseUrl, BuildResult result) throws IOException, URISyntaxException
    {
        createAndVerify(expectedName, type, baseUrl, result, new LinkedList<PersistentChangelist>());
    }

    protected void createAndVerify(String expectedName, String type, String baseUrl, BuildResult result, List<PersistentChangelist> changelists) throws IOException
    {
        createAndVerify(expectedName, type, baseUrl, result, changelists, null, 0, 0);
    }

    protected void createAndVerify(String expectedName, String type, String baseUrl, BuildResult result, List<PersistentChangelist> changelists, BuildResult lastSuccess, int unsuccessfulBuilds, int unsuccessfulDays) throws IOException
    {
        result.calculateFeatureCounts();
        
        Map<String, Object> dataMap = getDataMap(baseUrl, result, changelists, lastSuccess, unsuccessfulBuilds, unsuccessfulDays);

        String extension = "txt";

        // Just a hack that makes it easier to view expected output in a
        // browser during development.
        if (type.equals("html-email"))
        {
            extension = "html";
        }
        else if(type.equals("html-project-overview"))
        {
            extension = "overview.html";
        }
        else if(type.equals("simple-instant-message"))
        {
            extension = "si.txt";
        }
        else if(type.equals("detailed-instant-message"))
        {
            extension = "di.txt";
        }

        if (generate)
        {
            File expected = new File(getClass().getSimpleName() + "." + expectedName + "." + extension);
            OutputStream outStream = null;
            Writer writer = null;

            try
            {
                outStream = new FileOutputStream(expected);
                writer = new OutputStreamWriter(outStream);
                renderer.render(result, dataMap, type, writer);
            }
            finally
            {
                IOUtils.close(outStream);
                IOUtils.close(writer);
            }

            System.out.println("Generated expected output in '" + expected.getAbsolutePath() + "'");
        }
        else
        {
            StringWriter writer = new StringWriter();
            renderer.render(result, dataMap, type, writer);
            String got = removeCR(replaceTimestamps(writer.getBuffer().toString()));
            final String raw = Resources.asCharSource(getInputURL(expectedName, extension), Charset.defaultCharset()).read();
            String expected = removeCR(replaceTimestamps(raw));
            assertEquals(expected, got);
        }
    }

    private String removeCR(String s)
    {
        return s.replace("\r", "");
    }

    private Map<String, Object> getDataMap(String baseUrl, BuildResult result, List<PersistentChangelist> changelists, BuildResult lastHealthy, int unsuccessfulBuilds, int unsuccessfulDays)
    {

        Map<String, Object> dataMap = new HashMap<String, Object>();
        dataMap.put("renderer", renderer);
        dataMap.put("baseUrl", baseUrl);
        dataMap.put("project", result.getProject());
        dataMap.put("status", result.healthy() ? "healthy" : "broken");
        dataMap.put("result", result);
        dataMap.put("model", result);
        dataMap.put("changelists", changelists);
        dataMap.put("errorLevel", Feature.Level.ERROR);
        dataMap.put("warningLevel", Feature.Level.WARNING);

        if(lastHealthy != null)
        {
            dataMap.put("lastHealthy", lastHealthy);
            dataMap.put("unsuccessfulBuilds", unsuccessfulBuilds);
            dataMap.put("unsuccessfulDays", unsuccessfulDays);
        }
        
        return dataMap;
    }

    private String replaceTimestamps(String str)
    {
        return str.replaceAll("\n.*ago<", "@@@@").replaceAll("\n[0-9]+ ms", "@@@@").replaceAll("\n.*70.*<", "@@@@");
    }
}
