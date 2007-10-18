package com.zutubi.pulse.acceptance;

import com.zutubi.prototype.config.ConfigurationRegistry;
import com.zutubi.prototype.security.AccessManager;
import com.zutubi.prototype.type.record.PathUtils;
import com.zutubi.pulse.acceptance.pages.admin.AgentHierarchyPage;
import com.zutubi.pulse.acceptance.pages.admin.DeleteConfirmPage;
import com.zutubi.pulse.acceptance.pages.admin.ListPage;
import com.zutubi.pulse.acceptance.pages.admin.ProjectHierarchyPage;
import com.zutubi.pulse.acceptance.pages.browse.ProjectsPage;
import com.zutubi.pulse.agent.AgentManager;
import com.zutubi.pulse.model.ProjectManager;
import com.zutubi.pulse.prototype.config.LabelConfiguration;
import com.zutubi.pulse.prototype.config.group.ServerPermission;
import com.zutubi.pulse.prototype.config.project.BuildStageConfiguration;
import com.zutubi.pulse.prototype.config.project.triggers.BuildCompletedTriggerConfiguration;
import com.zutubi.pulse.prototype.config.user.UserConfigurationActions;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Tests for deletion of various things: an area that is notorious for bugs!
 */
public class DeleteAcceptanceTest extends SeleniumTestBase
{
    private static final String ACTION_DELETE_RECORD = "delete record";
    private static final String ACTION_DELETE_BUILDS = "delete all build results";
    private static final String ACTION_HIDE_RECORD   = "hide record";

    private static final String ACTION_HIDE          = "hide";
    private static final String ACTION_RESTORE       = "restore";

    protected void setUp() throws Exception
    {
        super.setUp();
        xmlRpcHelper.loginAsAdmin();
    }

    protected void tearDown() throws Exception
    {
        xmlRpcHelper.logout();
        super.tearDown();
    }

    public void testDeleteListItem() throws Exception
    {
        String projectPath = xmlRpcHelper.insertTrivialProject(random, false);
        String labelPath = insertLabel(projectPath);

        loginAsAdmin();
        ListPage labelList = new ListPage(selenium, urls, PathUtils.getParentPath(labelPath));
        labelList.goTo();
        String baseName = PathUtils.getBaseName(labelPath);
        labelList.assertItemPresent(baseName, null, "view", "delete");
        DeleteConfirmPage confirmPage = labelList.clickDelete(baseName);
        confirmPage.waitFor();
        confirmPage.assertTasks(labelPath, ACTION_DELETE_RECORD);
        confirmPage.clickDelete();

        labelList.waitFor();
        labelList.assertItemNotPresent(baseName);
    }

    public void testCancelDelete() throws Exception
    {
        String projectPath = xmlRpcHelper.insertTrivialProject(random, false);
        String labelPath = insertLabel(projectPath);

        loginAsAdmin();
        ListPage labelList = new ListPage(selenium, urls, PathUtils.getParentPath(labelPath));
        labelList.goTo();
        String baseName = PathUtils.getBaseName(labelPath);
        labelList.assertItemPresent(baseName, null, "view", "delete");
        DeleteConfirmPage confirmPage = labelList.clickDelete(baseName);
        confirmPage.waitFor();
        confirmPage.clickCancel();

        labelList.waitFor();
        labelList.assertItemPresent(baseName, null);
    }

    public void testCancelDeleteProject() throws Exception
    {
        xmlRpcHelper.insertTrivialProject(random, false);
        loginAsAdmin();
        ProjectHierarchyPage hierarchyPage = new ProjectHierarchyPage(selenium, urls, random, false);
        hierarchyPage.goTo();
        DeleteConfirmPage confirmPage = hierarchyPage.clickDelete();
        confirmPage.waitFor();
        confirmPage.clickCancel();
        hierarchyPage.waitFor();
    }

    public void testDeleteProject() throws Exception
    {
        String projectPath = xmlRpcHelper.insertTrivialProject(random, false);

        loginAsAdmin();
        ProjectHierarchyPage hierarchyPage = new ProjectHierarchyPage(selenium, urls, random, false);
        hierarchyPage.goTo();
        DeleteConfirmPage confirmPage = hierarchyPage.clickDelete();
        confirmPage.waitFor();
        confirmPage.assertTasks(projectPath, ACTION_DELETE_RECORD, projectPath, ACTION_DELETE_BUILDS);
        confirmPage.clickDelete();
        ProjectHierarchyPage global = new ProjectHierarchyPage(selenium, urls, ProjectManager.GLOBAL_PROJECT_NAME, true);
        global.waitFor();
        assertElementNotPresent("link=" + random);

        ProjectsPage projectsPage = new ProjectsPage(selenium, urls);
        projectsPage.goTo();
        projectsPage.assertProjectNotPresent(null, random);
    }

    public void testDeleteProjectWithReference() throws Exception
    {
        String refererName = random + "-er";
        String refereeName = random + "-ee";
        String refererPath = xmlRpcHelper.insertTrivialProject(refererName, false);
        String refereePath = xmlRpcHelper.insertTrivialProject(refereeName, false);

        String triggerPath = insertBuildCompletedTrigger(refereePath, refererPath);

        loginAsAdmin();
        ListPage triggersPage = new ListPage(selenium, urls, PathUtils.getParentPath(triggerPath));
        triggersPage.goTo();
        triggersPage.assertItemPresent("test", null);

        ProjectHierarchyPage hierarchyPage = new ProjectHierarchyPage(selenium, urls, refereeName, false);
        hierarchyPage.goTo();
        DeleteConfirmPage confirmPage = hierarchyPage.clickDelete();
        confirmPage.waitFor();
        confirmPage.assertTasks(refereePath, ACTION_DELETE_RECORD, refereePath, ACTION_DELETE_BUILDS, triggerPath, ACTION_DELETE_RECORD);
        confirmPage.clickDelete();

        ProjectHierarchyPage globalPage = new ProjectHierarchyPage(selenium, urls, ProjectManager.GLOBAL_PROJECT_NAME, true);
        globalPage.waitFor();

        triggersPage.goTo();
        triggersPage.assertItemNotPresent("test");
    }

    public void testDeleteWithInvisibleReference() throws Exception
    {
        // We need a user that is part of a group we can assign privileges to
        String authLogin = "u1:" + random;
        String authPath = xmlRpcHelper.insertTrivialUser(authLogin);

        String noAuthLogin = "u2:" + random;
        String noAuthPath = xmlRpcHelper.insertTrivialUser(noAuthLogin);

        String groupName = "group:" + random;
        xmlRpcHelper.insertGroup(groupName, Arrays.asList(authPath, noAuthPath), ServerPermission.DELETE_PROJECT.toString());

        String projectPath = xmlRpcHelper.insertTrivialProject(random, false);

        String dashboardPath = PathUtils.getPath(authPath, "preferences", "dashboard");
        Hashtable<String, Object> dashboard = xmlRpcHelper.getConfig(dashboardPath);
        dashboard.put("showAllProjects", false);
        dashboard.put("shownProjects", new Vector<String>(Arrays.asList(projectPath)));
        xmlRpcHelper.saveConfig(dashboardPath, dashboard, false);

        login(authLogin, "");
        ProjectHierarchyPage hierarchyPage = new ProjectHierarchyPage(selenium, urls, random, false);
        hierarchyPage.goTo();
        DeleteConfirmPage confirmPage = hierarchyPage.clickDelete();
        confirmPage.waitFor();
        confirmPage.assertTasks(projectPath, ACTION_DELETE_RECORD, projectPath, ACTION_DELETE_BUILDS, PathUtils.getPath(dashboardPath, "shownProjects", "0"), "remove reference");
        assertTextNotPresent("A further");
        confirmPage.clickCancel();
        hierarchyPage.waitFor();
        logout();
        
        login(noAuthLogin, "");
        hierarchyPage.goTo();
        confirmPage = hierarchyPage.clickDelete();
        confirmPage.waitFor();
        confirmPage.assertTasks(projectPath, ACTION_DELETE_RECORD, projectPath, ACTION_DELETE_BUILDS);
        assertTextPresent("A further task is required that is not visible to you with your current permissions");
        confirmPage.clickDelete();

        ProjectHierarchyPage globalPage = new ProjectHierarchyPage(selenium, urls, ProjectManager.GLOBAL_PROJECT_NAME, true);
        globalPage.waitFor();

        dashboard = xmlRpcHelper.getConfig(dashboardPath);
        assertEquals(0, ((Vector) dashboard.get("shownProjects")).size());

        // Make sure that it is not missing just because the reference is now
        // broken
        assertTrue(xmlRpcHelper.isConfigValid(dashboardPath));
    }

    public void testDeleteAgent() throws Exception
    {
        String path = xmlRpcHelper.insertSimpleAgent(random);
        loginAsAdmin();
        AgentHierarchyPage hierarchyPage = new AgentHierarchyPage(selenium, urls, random, false);
        hierarchyPage.goTo();
        DeleteConfirmPage confirmPage = hierarchyPage.clickDelete();
        confirmPage.waitFor();
        confirmPage.assertTasks(path, ACTION_DELETE_RECORD, path, "delete agent state");
        confirmPage.clickDelete();

        AgentHierarchyPage globalPage = new AgentHierarchyPage(selenium, urls, AgentManager.GLOBAL_AGENT_NAME, true);
        globalPage.waitFor();

        assertFalse(xmlRpcHelper.configPathExists(path));
    }

    public void testDeleteAgentWithBuildStageReference() throws Exception
    {
        String agentName = "agent-" + random;
        String agentPath = xmlRpcHelper.insertSimpleAgent(agentName);
        String projectName = "project-" + random;
        String projectPath = xmlRpcHelper.insertSimpleProject(projectName, false);

        Hashtable <String, Object> stage = xmlRpcHelper.createEmptyConfig(BuildStageConfiguration.class);
        stage.put("name", "agent");
        stage.put("agent", agentPath);
        stage.put("recipe", "");
        String stagePath = xmlRpcHelper.insertConfig(PathUtils.getPath(projectPath, "stages"), stage);

        loginAsAdmin();
        AgentHierarchyPage hierarchyPage = new AgentHierarchyPage(selenium, urls, agentName, false);
        hierarchyPage.goTo();
        DeleteConfirmPage confirmPage = hierarchyPage.clickDelete();
        confirmPage.waitFor();
        confirmPage.assertTasks(agentPath, ACTION_DELETE_RECORD, agentPath, "delete agent state", PathUtils.getPath(stagePath, "agent"), "null out reference");
        confirmPage.clickDelete();

        AgentHierarchyPage globalPage = new AgentHierarchyPage(selenium, urls, AgentManager.GLOBAL_AGENT_NAME, true);
        globalPage.waitFor();
        assertFalse(xmlRpcHelper.configPathExists(agentPath));

        assertTrue(xmlRpcHelper.isConfigValid(stagePath));
        stage = xmlRpcHelper.getConfig(stagePath);
        assertNull(stage.get("agent"));
    }

    public void testDeleteUser() throws Exception
    {
        String path = xmlRpcHelper.insertTrivialUser(random);
        loginAsAdmin();
        
        ListPage usersPage = new ListPage(selenium, urls, ConfigurationRegistry.USERS_SCOPE);
        usersPage.goTo();
        usersPage.assertItemPresent(random, null, AccessManager.ACTION_VIEW, AccessManager.ACTION_DELETE, UserConfigurationActions.ACTION_SET_PASSWORD);
        DeleteConfirmPage confirmPage = usersPage.clickDelete(random);
        confirmPage.waitFor();
        confirmPage.assertTasks(path, ACTION_DELETE_RECORD, path, "delete user state");
        confirmPage.clickDelete();

        usersPage.waitFor();
        usersPage.assertItemNotPresent(random);
    }

    public void testHideMapItem() throws Exception
    {
        String projectPath = xmlRpcHelper.insertSimpleProject(random, false);

        loginAsAdmin();
        String stagesPath = PathUtils.getPath(projectPath, "stages");
        String stagePath = PathUtils.getPath(stagesPath, "default");

        ListPage stagesPage = new ListPage(selenium, urls, stagesPath);
        stagesPage.goTo();
        stagesPage.expandTreeNode(stagesPath);
        stagesPage.assertItemPresent("default", ListPage.ANNOTATION_INHERITED, AccessManager.ACTION_VIEW, AccessManager.ACTION_DELETE);
        stagesPage.assertTreeLinkPresent("default");

        DeleteConfirmPage confirmPage = stagesPage.clickDelete("default");
        confirmPage.waitFor();
        assertTextPresent("Are you sure you wish to hide this record?");
        confirmPage.assertTasks(stagePath, ACTION_HIDE_RECORD);
        confirmPage.clickDelete();

        stagesPage.waitFor();
        stagesPage.assertItemPresent("default", ListPage.ANNOTATION_HIDDEN, ACTION_RESTORE);
        stagesPage.assertTreeLinkNotPresent("default");
    }

    public void testCancelHide() throws Exception
    {
        String projectPath = xmlRpcHelper.insertSimpleProject(random, false);

        loginAsAdmin();
        String stagesPath = PathUtils.getPath(projectPath, "stages");

        ListPage stagesPage = new ListPage(selenium, urls, stagesPath);
        stagesPage.goTo();
        stagesPage.assertItemPresent("default", ListPage.ANNOTATION_INHERITED, AccessManager.ACTION_VIEW, AccessManager.ACTION_DELETE);

        DeleteConfirmPage confirmPage = stagesPage.clickDelete("default");
        confirmPage.waitFor();
        confirmPage.clickCancel();

        stagesPage.waitFor();
        stagesPage.assertItemPresent("default", ListPage.ANNOTATION_INHERITED, AccessManager.ACTION_VIEW, AccessManager.ACTION_DELETE);
    }

    public void testHideMapItemWithSkeletonDescendent() throws Exception
    {
        String parentName = random + "-parent";
        String childName = random + "-child";

        String parentPath = xmlRpcHelper.insertSimpleProject(parentName, true);
        String parentStagesPath = PathUtils.getPath(parentPath, "stages");
        String parentStagePath = PathUtils.getPath(parentStagesPath, "default");
        String childPath = xmlRpcHelper.insertSimpleProject(childName, parentName, false);
        String childStagePath = PathUtils.getPath(childPath, "stages", "default");

        loginAsAdmin();

        ListPage stagesPage = new ListPage(selenium, urls, parentStagesPath);
        stagesPage.goTo();
        stagesPage.assertItemPresent("default", ListPage.ANNOTATION_INHERITED, AccessManager.ACTION_VIEW, AccessManager.ACTION_DELETE);

        DeleteConfirmPage confirmPage = stagesPage.clickDelete("default");
        confirmPage.waitFor();
        assertTextPresent("Are you sure you wish to hide this record?");
        confirmPage.assertTasks(parentStagePath, ACTION_HIDE_RECORD);
        confirmPage.clickDelete();

        stagesPage.waitFor();
        stagesPage.assertItemPresent("default", ListPage.ANNOTATION_HIDDEN, ACTION_RESTORE);
        assertFalse(xmlRpcHelper.configPathExists(childStagePath));
    }

    public void testHideMapItemWithDescendentOverride() throws Exception
    {
        String parentName = random + "-parent";
        String childName = random + "-child";

        String parentPath = xmlRpcHelper.insertSimpleProject(parentName, true);
        String parentStagesPath = PathUtils.getPath(parentPath, "stages");
        String parentStagePath = PathUtils.getPath(parentStagesPath, "default");
        String childPath = xmlRpcHelper.insertSimpleProject(childName, parentName, false);
        String childStagePath = PathUtils.getPath(childPath, "stages", "default");
        Hashtable<String, Object> childStage = xmlRpcHelper.getConfig(childStagePath);
        childStage.put("recipe", "edited");
        xmlRpcHelper.saveConfig(childStagePath, childStage, false);

        loginAsAdmin();

        ListPage stagesPage = new ListPage(selenium, urls, parentStagesPath);
        stagesPage.goTo();
        stagesPage.assertItemPresent("default", ListPage.ANNOTATION_INHERITED, AccessManager.ACTION_VIEW, AccessManager.ACTION_DELETE);

        DeleteConfirmPage confirmPage = stagesPage.clickDelete("default");
        confirmPage.waitFor();
        assertTextPresent("Are you sure you wish to hide this record?");
        confirmPage.assertTasks(parentStagePath, ACTION_HIDE_RECORD, childStagePath, ACTION_DELETE_RECORD);
        confirmPage.clickDelete();

        stagesPage.waitFor();
        stagesPage.assertItemPresent("default", ListPage.ANNOTATION_HIDDEN, ACTION_RESTORE);
        assertFalse(xmlRpcHelper.configPathExists(childStagePath));
    }

    public void testRestoreMapItem() throws Exception
    {
        String projectPath = xmlRpcHelper.insertSimpleProject(random, false);
        String stagesPath = PathUtils.getPath(projectPath, "stages");
        String stagePath = PathUtils.getPath(stagesPath, "default");
        xmlRpcHelper.deleteConfig(stagePath);

        loginAsAdmin();

        ListPage stagesPage = new ListPage(selenium, urls, stagesPath);
        stagesPage.goTo();
        stagesPage.assertItemPresent("default", ListPage.ANNOTATION_HIDDEN, ACTION_RESTORE);
        stagesPage.expandTreeNode(stagesPath);
        stagesPage.assertTreeLinkNotPresent("default");

        stagesPage.clickRestore("default");
        stagesPage.waitFor();
        stagesPage.assertItemPresent("default", ListPage.ANNOTATION_INHERITED, AccessManager.ACTION_VIEW, AccessManager.ACTION_DELETE);
        stagesPage.expandTreeNode(stagesPath);
        stagesPage.assertTreeLinkPresent("default");
    }

    private String insertBuildCompletedTrigger(String refereePath, String refererPath) throws Exception
    {
        Hashtable<String, Object> trigger = xmlRpcHelper.createEmptyConfig(BuildCompletedTriggerConfiguration.class);
        trigger.put("name", "test");
        trigger.put("project", refereePath);
        return xmlRpcHelper.insertConfig(PathUtils.getPath(refererPath, "triggers"), trigger);
    }

    private String insertLabel(String projectPath) throws Exception
    {
        Hashtable<String, Object> label = xmlRpcHelper.createEmptyConfig(LabelConfiguration.class);
        label.put("label", "test");
        return xmlRpcHelper.insertConfig(PathUtils.getPath(projectPath, "labels"), label);
    }
}
