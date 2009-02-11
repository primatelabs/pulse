package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.pages.browse.BuildArtifactsPage;
import com.zutubi.pulse.acceptance.pages.browse.BuildSummaryPage;
import com.zutubi.pulse.core.commands.maven2.Maven2CommandConfiguration;
import com.zutubi.pulse.master.model.ProjectManager;

import java.util.Hashtable;
import java.util.Vector;

/**
 * The acceptance tests for the Pulse builtin maven 1.x and 2.x integration.
 */
public class MavenAcceptanceTest extends SeleniumTestBase
{
    private static final int BUILD_TIMEOUT = 90000;

    public void testMavenDefaultTestArtifactConfiguration() throws Exception
    {
        createMavenProject();

        // We expect a artifact called surefire-reports to be configured.
        Hashtable<String, Object> artifact = getArtifactConfiguration(random, "test reports");
        assertArtifactConfiguration(artifact, "test reports", "target/test-reports", "TEXT-*.xml", "junit");
    }

    public void testMaven2DefaultTestArtifactConfiguration() throws Exception
    {
        createMaven2Project();

        // We expect a artifact called surefire-reports to be configured.
        Hashtable<String, Object> artifact = getArtifactConfiguration(random, "test reports");
        assertArtifactConfiguration(artifact, "test reports", "target/surefire-reports", "TEXT-*.xml", "junit");
    }

    public void testMaven2BuildPicksUpTests() throws Exception
    {
        createMaven2Project();

        int buildNumber = runBuild(random);

        loginAsAdmin();

        // We expect the summary page to report that 1 test passed.
        BuildSummaryPage summaryPage = new BuildSummaryPage(selenium, urls, random, buildNumber);
        summaryPage.goTo();
        assertEquals("all 1 passed", summaryPage.getSummaryTestsColumnText());

        // We expect the artifacts page to contain an artifact called test reports.
        BuildArtifactsPage artifactsPage = new BuildArtifactsPage(selenium, urls, random, buildNumber);
        artifactsPage.goTo();
        SeleniumUtils.waitForLocator(selenium, artifactsPage.getArtifactLocator("test reports"));
    }

    @SuppressWarnings({ "unchecked" })
    private void assertArtifactConfiguration(Hashtable<String, Object> artifact, String expectedName, String expectedBase, String expectedIncludes, String... expectedPostProcessors)
    {
        Vector<String> postprocessors = (Vector<String>) artifact.get(Constants.DirectoryArtifact.POSTPROCESSORS);
        assertEquals(expectedPostProcessors.length, postprocessors.size());
        for (int i = 0; i < expectedPostProcessors.length; i++)
        {
            assertEquals(expectedPostProcessors[i], postprocessors.get(i));
        }
        assertEquals(expectedName, artifact.get(Constants.DirectoryArtifact.NAME));
        assertEquals(expectedBase, artifact.get(Constants.DirectoryArtifact.BASE));
        assertEquals(expectedIncludes, artifact.get(Constants.DirectoryArtifact.INCLUDES));
    }

    private void createMavenProject() throws Exception
    {
        // FIXME loader
        Hashtable<String, Object> command = xmlRpcHelper.createEmptyConfig("zutubi.mavenTypeConfig");
        command.put("targets", "install");

        createMavenProject(command);
    }

    private void createMaven2Project() throws Exception
    {
        Hashtable<String, Object> command = xmlRpcHelper.createEmptyConfig(Maven2CommandConfiguration.class);
        command.put("goals", "install");

        createMavenProject(command);
    }

    private void createMavenProject(Hashtable<String, Object> command) throws Exception
    {
        xmlRpcHelper.loginAsAdmin();
        xmlRpcHelper.insertSingleCommandProject(random, ProjectManager.GLOBAL_PROJECT_NAME, false, xmlRpcHelper.getSubversionConfig(Constants.TEST_MAVEN_REPOSITORY), command);
        xmlRpcHelper.logout();
    }

    private int runBuild(String projectName) throws Exception
    {
        try
        {
            xmlRpcHelper.loginAsAdmin();
            return xmlRpcHelper.runBuild(projectName, BUILD_TIMEOUT);
        }
        finally
        {
            xmlRpcHelper.logout();
        }
    }

    private Hashtable<String, Object> getArtifactConfiguration(String projectName, String artifactName) throws Exception
    {
        try
        {
            xmlRpcHelper.loginAsAdmin();
            return xmlRpcHelper.getProjectArtifact(projectName, artifactName);
        }
        finally
        {
            xmlRpcHelper.logout();
        }
    }
}
