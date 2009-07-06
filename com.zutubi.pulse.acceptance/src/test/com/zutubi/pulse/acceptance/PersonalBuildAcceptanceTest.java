package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.pages.dashboard.*;
import com.zutubi.pulse.acceptance.support.ProxyServer;
import com.zutubi.pulse.core.engine.api.BuildProperties;
import com.zutubi.pulse.core.engine.api.ResultState;
import com.zutubi.pulse.core.personal.TestPersonalBuildUI;
import com.zutubi.pulse.core.scm.WorkingCopyFactory;
import com.zutubi.pulse.core.scm.api.Revision;
import com.zutubi.pulse.core.scm.api.WorkingCopy;
import com.zutubi.pulse.core.scm.svn.SubversionClient;
import com.zutubi.pulse.core.scm.svn.SubversionWorkingCopy;
import com.zutubi.pulse.dev.personal.PersonalBuildClient;
import com.zutubi.pulse.dev.personal.PersonalBuildCommand;
import com.zutubi.pulse.dev.personal.PersonalBuildConfig;
import com.zutubi.pulse.master.agent.AgentManager;
import com.zutubi.pulse.master.model.ProjectManager;
import com.zutubi.pulse.master.tove.config.MasterConfigurationRegistry;
import com.zutubi.pulse.master.tove.config.project.ProjectConfigurationWizard;
import com.zutubi.pulse.master.tove.config.project.hooks.*;
import com.zutubi.tove.type.record.PathUtils;
import static com.zutubi.util.CollectionUtils.asPair;
import com.zutubi.util.FileSystemUtils;
import com.zutubi.util.Pair;
import com.zutubi.util.StringUtils;
import com.zutubi.util.io.IOUtils;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

/**
 * Simple sanity checks for personal builds.
 */
public class PersonalBuildAcceptanceTest extends SeleniumTestBase
{
    private static final String PROJECT_NAME = "PersonalBuildAcceptanceTest-Project";
    private static final int BUILD_TIMEOUT = 90000;

    private File workingCopyDir;

    protected void setUp() throws Exception
    {
        super.setUp();

        WorkingCopyFactory.registerType("svn", SubversionWorkingCopy.class);
        workingCopyDir = FileSystemUtils.createTempDir("PersonalBuildAcceptanceTest", "");

        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();

        xmlRpcHelper.loginAsAdmin();
    }

    protected void tearDown() throws Exception
    {
        xmlRpcHelper.logout();
        FileSystemUtils.rmdir(workingCopyDir);

        super.tearDown();
    }

    public void testPersonalBuild() throws Exception
    {
        checkout(Constants.TRIVIAL_ANT_REPOSITORY);
        makeChangeToBuildFile();
        createConfigFile(PROJECT_NAME);

        loginAsAdmin();
        ensureProject(PROJECT_NAME);
        editStageToRunOnAgent(AgentManager.MASTER_AGENT_NAME, PROJECT_NAME);
        long buildNumber = runPersonalBuild(ResultState.FAILURE);
        verifyPersonalBuildTabs(buildNumber, AgentManager.MASTER_AGENT_NAME);

        PersonalEnvironmentArtifactPage envPage = browser.openAndWaitFor(PersonalEnvironmentArtifactPage.class, PROJECT_NAME, buildNumber, "default", "build");
        assertTrue(envPage.isPropertyPresentWithValue(BuildProperties.PROPERTY_INCREMENTAL_BOOTSTRAP, Boolean.toString(false)));
        assertTrue(envPage.isPropertyPresentWithValue(BuildProperties.PROPERTY_LOCAL_BUILD, Boolean.toString(false)));
        assertTrue(envPage.isPropertyPresentWithValue(BuildProperties.PROPERTY_PERSONAL_BUILD, Boolean.toString(true)));
        // Make sure this view is not decorated (CIB-1711).
        assertTextNotPresent("logout");
    }

    public void testPersonalBuildViaProxy() throws Exception
    {
        final int PROXY_PORT = 8754;

        ProxyServer proxyServer = new ProxyServer(PROXY_PORT);
        proxyServer.start();

        try
        {
            checkout(Constants.TRIVIAL_ANT_REPOSITORY);
            makeChangeToBuildFile();
            createConfigFile(PROJECT_NAME, asPair(PersonalBuildConfig.PROPERTY_PROXY_HOST, "localhost"), asPair(PersonalBuildConfig.PROPERTY_PROXY_PORT, PROXY_PORT));

            loginAsAdmin();
            ensureProject(PROJECT_NAME);
            editStageToRunOnAgent(AgentManager.MASTER_AGENT_NAME, PROJECT_NAME);
            long buildNumber = runPersonalBuild(ResultState.FAILURE);
            verifyPersonalBuildTabs(buildNumber, AgentManager.MASTER_AGENT_NAME);
        }
        finally
        {
            proxyServer.stop();
        }
    }

    public void testPersonalBuildChangesImportedFile() throws Exception
    {
        checkout(Constants.VERSIONED_REPOSITORY);
        makeChangeToImportedFile();
        createConfigFile(random);
        loginAsAdmin();

        xmlRpcHelper.insertProject(random, ProjectManager.GLOBAL_PROJECT_NAME, false, xmlRpcHelper.getSubversionConfig(Constants.VERSIONED_REPOSITORY), xmlRpcHelper.createVersionedConfig("pulse/pulse.xml"));
        editStageToRunOnAgent(AgentManager.MASTER_AGENT_NAME, random);
        long buildNumber = runPersonalBuild(ResultState.ERROR);
        browser.openAndWaitFor(PersonalBuildSummaryPage.class, buildNumber);
        assertTextPresent("Unknown child element 'notrecognised'");
    }

    public void testPersonalBuildOnAgent() throws Exception
    {
        checkout(Constants.TRIVIAL_ANT_REPOSITORY);
        makeChangeToBuildFile();
        createConfigFile(PROJECT_NAME);

        loginAsAdmin();
        ensureAgent(AGENT_NAME);
        ensureProject(PROJECT_NAME);
        editStageToRunOnAgent(AGENT_NAME, PROJECT_NAME);
        long buildNumber = runPersonalBuild(ResultState.FAILURE);
        verifyPersonalBuildTabs(buildNumber, AGENT_NAME);
    }

    public void testPersonalBuildWithHooks() throws Exception
    {
        String projectPath = addProject(random, true);
        String hooksPath = PathUtils.getPath(projectPath, "buildHooks");

        // Create two of each type of hook: one that runs for personal builds,
        // and another that doesn't.
        insertHook(hooksPath, PreBuildHookConfiguration.class, "prebuildno", false);
        insertHook(hooksPath, PreBuildHookConfiguration.class, "prebuildyes", true);
        insertHook(hooksPath, PostBuildHookConfiguration.class, "postbuildno", false);
        insertHook(hooksPath, PostBuildHookConfiguration.class, "postbuildyes", true);
        insertHook(hooksPath, PostStageHookConfiguration.class, "poststageno", false);
        insertHook(hooksPath, PostStageHookConfiguration.class, "poststageyes", true);

        // Now make a change and run a personal build.
        checkout(Constants.TRIVIAL_ANT_REPOSITORY);
        makeChangeToBuildFile();
        createConfigFile(random);

        loginAsAdmin();
        editStageToRunOnAgent(AgentManager.MASTER_AGENT_NAME, random);
        long buildNumber = runPersonalBuild(ResultState.FAILURE);

        // Finally check that only the enabled hooks ran.
        String text = getLogText(urls.dashboardMyBuildLog(Long.toString(buildNumber)));
        assertFalse("Pre-build hook not for personal should not have run", text.contains("prebuildno"));
        assertTrue("Pre-build hook for personal should have run", text.contains("prebuildyes"));
        assertFalse("Post-build hook not for personal should not have run", text.contains("postbuildno"));
        assertTrue("Post-build hook for personal should have run", text.contains("postbuildyes"));

        text = getLogText(urls.dashboardMyStageLog(Long.toString(buildNumber), ProjectConfigurationWizard.DEFAULT_STAGE));
        assertFalse("Post-stage hook not for personal should not have run", text.contains("poststageno"));
        assertTrue("Post-stage hook for personal should have run", text.contains("poststageyes"));
    }

    public void testManuallyTriggerHook() throws Exception
    {
        final String HOOK_NAME = "manual-hook";

        String projectPath = addProject(random, true);
        Hashtable<String, Object> hook = xmlRpcHelper.createEmptyConfig(ManualBuildHookConfiguration.class);
        hook.put("name", HOOK_NAME);
        xmlRpcHelper.insertConfig(PathUtils.getPath(projectPath, "buildHooks"), hook);

        // Now make a change and run a personal build.
        checkout(Constants.TRIVIAL_ANT_REPOSITORY);
        makeChangeToBuildFile();
        createConfigFile(random);

        loginAsAdmin();
        editStageToRunOnAgent(AgentManager.MASTER_AGENT_NAME, random);
        long buildNumber = runPersonalBuild(ResultState.FAILURE);

        PersonalBuildSummaryPage page = browser.openAndWaitFor(PersonalBuildSummaryPage.class, buildNumber);
        assertTrue(page.isHookPresent(HOOK_NAME));
        page.clickHook(HOOK_NAME);

        browser.waitForVisible("status-message");
        assertTextPresent("triggered hook '" + HOOK_NAME + "'");
    }

    public void testPersonalBuildFloatingRevision() throws Exception
    {
        checkout(Constants.TRIVIAL_ANT_REPOSITORY);
        makeChangeToBuildFile();
        createConfigFile(PROJECT_NAME, asPair(PersonalBuildConfig.PROPERTY_REVISION, WorkingCopy.REVISION_FLOATING));

        loginAsAdmin();
        ensureProject(PROJECT_NAME);
        editStageToRunOnAgent(AgentManager.MASTER_AGENT_NAME, PROJECT_NAME);
        long buildNumber = runPersonalBuild(ResultState.FAILURE);

        // Check that we actually built against the latest.
        SubversionClient client = new SubversionClient(Constants.TRIVIAL_ANT_REPOSITORY);
        Revision revision = client.getLatestRevision(null);

        PersonalBuildChangesPage changesPage = browser.openAndWaitFor(PersonalBuildChangesPage.class, buildNumber);
        assertEquals(revision.getRevisionString(), changesPage.getCheckedOutRevision());
    }

    public void testPersonalBuildConflicts() throws Exception
    {
        checkout(Constants.TRIVIAL_ANT_REPOSITORY);
        makeChangeToBuildFile();
        // Set revision to something before the last edit to the build file.
        createConfigFile(PROJECT_NAME, asPair(PersonalBuildConfig.PROPERTY_REVISION, "1"), asPair(PersonalBuildConfig.PROPERTY_UPDATE, false));

        loginAsAdmin();
        ensureProject(PROJECT_NAME);
        editStageToRunOnAgent(AgentManager.MASTER_AGENT_NAME, PROJECT_NAME);
        long buildNumber = runPersonalBuild(ResultState.ERROR);

        browser.openAndWaitFor(PersonalBuildSummaryPage.class, buildNumber);
        assertTextPresent("Patch does not apply cleanly");
    }


    private Hashtable<String, Object> insertHook(String hooksPath, Class<? extends BuildHookConfiguration> hookClass, String name, boolean runForPersonal) throws Exception
    {
        Hashtable<String, Object> hook = xmlRpcHelper.createEmptyConfig(hookClass);
        hook.put("name", name);
        hook.put("runForPersonal", runForPersonal);
        xmlRpcHelper.insertConfig(hooksPath, hook);
        return hook;
    }

    private String getLogText(String url)
    {
        browser.open(url);
        browser.waitForPageToLoad();
        return browser.getText("panel");
    }

    private void checkout(String url) throws SVNException
    {
        SVNUpdateClient client = new SVNUpdateClient(SVNWCUtil.createDefaultAuthenticationManager(), null);
        client.doCheckout(SVNURL.parseURIDecoded(url), workingCopyDir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
    }

    private void makeChangeToBuildFile() throws IOException
    {
        // Edit the build.xml file so we have an outstanding change
        File buildFile = new File(workingCopyDir, "build.xml");
        FileSystemUtils.createFile(buildFile, "<?xml version=\"1.0\"?>\n" +
                "<project default=\"" + random + "\">\n" +
                "    <target name=\"" + random + "\">\n" +
                "        <nosuchcommand/>\n" +
                "    </target>\n" +
                "</project>");
    }

    private void makeChangeToImportedFile() throws IOException
    {
        File includedFile = new File(workingCopyDir, "properties.xml");
        FileSystemUtils.createFile(includedFile, "<?xml version=\"1.0\"?>\n" +
                "<project><notrecognised/></project>\n");
    }

    private void editStageToRunOnAgent(String agent, String projectName) throws Exception
    {
        String stagePath = PathUtils.getPath(MasterConfigurationRegistry.PROJECTS_SCOPE, projectName, "stages", "default");
        Hashtable<String, Object> stage = xmlRpcHelper.getConfig(stagePath);
        stage.put("agent", PathUtils.getPath(MasterConfigurationRegistry.AGENTS_SCOPE, agent));
        xmlRpcHelper.saveConfig(stagePath, stage, false);
    }

    private long runPersonalBuild(ResultState expectedStatus)
    {
        // Request the build and wait for it to complete
        AcceptancePersonalBuildUI ui = requestPersonalBuild();

        List<String> warnings = ui.getWarningMessages();
        assertTrue("Got warnings: " + StringUtils.join("\n", warnings), warnings.isEmpty());
        List<String> errors = ui.getErrorMessages();
        assertTrue("Got errors: " + StringUtils.join("\n", errors), errors.isEmpty());

        List<String> statuses = ui.getStatusMessages();
        assertTrue(statuses.size() > 0);
        assertTrue("Patch not accepted given status:\n" + StringUtils.join("\n", statuses), ui.isPatchAccepted());

        long buildNumber = ui.getBuildNumber();
        browser.openAndWaitFor(MyBuildsPage.class);
        browser.refreshUntilElement(MyBuildsPage.getBuildNumberId(buildNumber));
        assertElementNotPresent(MyBuildsPage.getBuildNumberId(buildNumber + 1));
        browser.refreshUntilText(MyBuildsPage.getBuildStatusId(buildNumber), expectedStatus.getPrettyString(), BUILD_TIMEOUT);
        return buildNumber;
    }

    private void createConfigFile(String projectName, Pair<String, ?>... extraProperties) throws IOException
    {
        File configFile = new File(workingCopyDir, PersonalBuildConfig.PROPERTIES_FILENAME);
        Properties config = new Properties();
        config.put(PersonalBuildConfig.PROPERTY_PULSE_URL, browser.getBaseUrl());
        config.put(PersonalBuildConfig.PROPERTY_PULSE_USER, "admin");
        config.put(PersonalBuildConfig.PROPERTY_PULSE_PASSWORD, "admin");
        config.put(PersonalBuildConfig.PROPERTY_PROJECT, projectName);

        for (Pair<String, ?> extra: extraProperties)
        {
            config.put(extra.first, extra.second.toString());
        }

        FileOutputStream os = null;
        try
        {
            os = new FileOutputStream(configFile);
            config.store(os, null);
        }
        finally
        {
            IOUtils.close(os);
        }
    }

    private AcceptancePersonalBuildUI requestPersonalBuild()
    {
        PersonalBuildConfig config = new PersonalBuildConfig(workingCopyDir);
        AcceptancePersonalBuildUI ui = new AcceptancePersonalBuildUI();
        PersonalBuildClient client = new PersonalBuildClient(config, ui);

        PersonalBuildCommand command = new PersonalBuildCommand();
        command.execute(client);

        return ui;
    }

    private void verifyPersonalBuildTabs(long buildNumber, String agent)
    {
        // Verify each tab in turn
        browser.openAndWaitFor(PersonalBuildSummaryPage.class, buildNumber);
        assertTextPresent("nosuchcommand");
        assertEquals(agent, browser.getText(IDs.stageAgentCell(PROJECT_NAME, buildNumber, "default")));

        browser.click(IDs.buildDetailsTab());
        PersonalBuildDetailedViewPage detailedViewPage = browser.createPage(PersonalBuildDetailedViewPage.class, buildNumber);
        detailedViewPage.waitFor();
        detailedViewPage.clickCommand("default", "build");
        assertTextPresent("nosuchcommand");

        browser.click(IDs.buildChangesTab());
        PersonalBuildChangesPage changesPage = browser.createPage(PersonalBuildChangesPage.class, buildNumber);
        changesPage.waitFor();
        // Just parse to make sure it's a number: asserting the revision has
        // proven too fragile.
        Long.parseLong(changesPage.getCheckedOutRevision());
        assertEquals("build.xml", changesPage.getChangedFile(0));

        browser.click(IDs.buildTestsTab());
        PersonalBuildTestsPage testsPage = browser.createPage(PersonalBuildTestsPage.class, buildNumber);
        testsPage.waitFor();
        assertTrue(testsPage.isBuildComplete());
        assertFalse(testsPage.hasTests());

        browser.click(IDs.buildFileTab());
        PersonalBuildFilePage filePage = browser.createPage(PersonalBuildFilePage.class, buildNumber);
        filePage.waitFor();
        assertTrue(filePage.isHighlightedFilePresent());
        assertTextPresent("<ant");

        PersonalBuildArtifactsPage artifactsPage = browser.openAndWaitFor(PersonalBuildArtifactsPage.class, buildNumber);
        browser.waitForLocator(artifactsPage.getCommandLocator("build"));

        browser.click(IDs.buildWorkingCopyTab());
        PersonalBuildWorkingCopyPage wcPage = browser.createPage(PersonalBuildWorkingCopyPage.class, buildNumber);
        wcPage.waitFor();
        assertTrue(wcPage.isWorkingCopyNotPresent());
    }

    private static class AcceptancePersonalBuildUI extends TestPersonalBuildUI
    {
        private long buildNumber = -1;

        public boolean isPatchAccepted()
        {
            return buildNumber > 0;
        }

        public long getBuildNumber()
        {
            return buildNumber;
        }

        public void status(String message)
        {
            super.status(message);

            if (message.startsWith("Patch accepted"))
            {
                String[] pieces = message.split(" ");
                String number = pieces[pieces.length - 1];
                number = number.substring(0, number.length() - 1);
                buildNumber = Long.parseLong(number);
            }
        }

        public String inputPrompt(String question)
        {
            return "";
        }

        public String passwordPrompt(String question)
        {
            return "";
        }
    }
}
