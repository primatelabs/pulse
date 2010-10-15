package com.zutubi.pulse.acceptance;

import com.zutubi.i18n.Messages;
import com.zutubi.pulse.acceptance.forms.admin.CustomTypeForm;
import com.zutubi.pulse.acceptance.forms.admin.MultiRecipeTypeForm;
import com.zutubi.pulse.acceptance.forms.admin.SetPasswordForm;
import com.zutubi.pulse.acceptance.forms.admin.UserForm;
import com.zutubi.pulse.acceptance.pages.admin.AgentConfigPage;
import com.zutubi.pulse.acceptance.pages.admin.ListPage;
import com.zutubi.pulse.acceptance.pages.admin.ProjectConfigPage;
import com.zutubi.pulse.master.agent.AgentManager;
import com.zutubi.pulse.master.model.Project;
import com.zutubi.pulse.master.tove.config.MasterConfigurationRegistry;
import com.zutubi.pulse.master.tove.config.agent.AgentConfigurationActions;
import com.zutubi.pulse.master.tove.config.project.ProjectConfiguration;
import com.zutubi.pulse.master.tove.config.project.ProjectConfigurationActions;
import com.zutubi.tove.config.ConfigurationRefactoringManager;
import com.zutubi.tove.security.AccessManager;
import com.zutubi.tove.type.record.PathUtils;

import java.util.Hashtable;

/**
 * Acceptance tests for actions that may be executed on configuration
 * instances.
 */
public class ConfigActionsAcceptanceTest extends SeleniumTestBase
{
    private static final String EXPECTED_FILE_CONTENT = "build.xml";

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

    public void testActionFeedbackSimple() throws Exception
    {
        xmlRpcHelper.insertSimpleProject(random, false);
        browser.loginAsAdmin();
        ProjectConfigPage projectConfigPage = browser.openAndWaitFor(ProjectConfigPage.class, random, false);
        projectConfigPage.clickAction("clean");
        waitForStatus(Messages.getInstance(ProjectConfiguration.class).format("clean.feedback"));
    }

    public void testCustomActionWithArgument() throws Exception
    {
        ListPage usersPage = customActionWithArgumentPrelude();

        SetPasswordForm form = browser.createForm(SetPasswordForm.class);
        form.waitFor();
        form.saveFormElements("testpw", "testpw");

        usersPage.waitFor();
        browser.logout();

        // Login with the new password
        assertTrue(browser.login(random, "testpw"));
    }

    public void testCustomActionWithArgumentValidation() throws Exception
    {
        customActionWithArgumentPrelude();
        SetPasswordForm form = browser.createForm(SetPasswordForm.class);
        form.waitFor();
        form.saveFormElements("one", "two");
        form.waitFor();
        assertTextPresent("passwords do not match");
    }

    public void testCustomActionWithArgumentCancel() throws Exception
    {
        customActionWithArgumentPrelude();

        SetPasswordForm setPasswordForm = browser.createForm(SetPasswordForm.class);
        setPasswordForm.waitFor();
        setPasswordForm.cancelFormElements("testpw", "testpw");

        UserForm userForm = browser.createForm(UserForm.class, random);
        userForm.waitFor();
        browser.logout();

        // Check the password is unchanged
        assertTrue(browser.login(random, ""));
    }

    private ListPage customActionWithArgumentPrelude() throws Exception
    {
        xmlRpcHelper.insertTrivialUser(random);

        browser.loginAsAdmin();
        ListPage usersPage = browser.openAndWaitFor(ListPage.class, MasterConfigurationRegistry.USERS_SCOPE);
        usersPage.clickAction(random, "setPassword");
        return usersPage;
    }

    public void testPrepareAction() throws Exception
    {
        ProjectConfigPage projectPage = prepareActionPrelude();

        CustomTypeForm form = browser.createForm(CustomTypeForm.class);
        form.waitFor();

        // Make sure the arg was prepared from the current project config
        assertTrue(form.getFieldValue("pulseFileString").contains(EXPECTED_FILE_CONTENT));
        form.saveFormElements(new String[]{null});

        projectPage.waitFor();
        browser.waitForLocator(projectPage.getTreeLinkLocator("custom pulse file"));
        projectPage.clickComposite("type", "custom pulse file");

        form.waitFor();
        assertTrue(form.getFieldValue("pulseFileString").contains(EXPECTED_FILE_CONTENT));
    }

    public void testPrepareActionCancel() throws Exception
    {
        ProjectConfigPage projectPage = prepareActionPrelude();

        CustomTypeForm customForm = browser.createForm(CustomTypeForm.class);
        customForm.waitFor();
        customForm.cancelFormElements(new String[]{null});

        projectPage.waitFor();
        assertFalse(projectPage.isTreeLinkPresent("pulse file"));
        assertTrue(projectPage.isTreeLinkPresent("recipes, commands and artifacts"));

        projectPage.clickComposite("type", "recipes, commands and artifacts");
        MultiRecipeTypeForm typeForm = browser.createForm(MultiRecipeTypeForm.class);
        typeForm.waitFor();
    }

    public void testPrepareActionValidation() throws Exception
    {
        prepareActionPrelude();

        CustomTypeForm form = browser.createForm(CustomTypeForm.class);
        form.waitFor();

        // Make sure the arg was prepared from the current project config
        assertTrue(form.getFieldValue("pulseFileString").contains(EXPECTED_FILE_CONTENT));
        form.saveFormElements("<?xml version=\"1.0\"?><project><nosuchtag/></project>");
        form.waitFor();

        assertTextPresent("Unknown child element 'nosuchtag'");
    }

    private ProjectConfigPage prepareActionPrelude() throws Exception
    {
        xmlRpcHelper.insertSimpleProject(random, false);

        browser.loginAsAdmin();
        ProjectConfigPage projectPage = browser.openAndWaitFor(ProjectConfigPage.class, random, false);
        projectPage.clickAction(ProjectConfigurationActions.ACTION_CONVERT_TO_CUSTOM);
        return projectPage;
    }

    public void testCannotConvertProjectWhenTypeIsInherited() throws Exception
    {
        String parentProject = random + "-parent";
        String childProject = random + "-child";
        xmlRpcHelper.insertSimpleProject(parentProject, true);
        xmlRpcHelper.insertTrivialProject(childProject, parentProject, false);

        browser.loginAsAdmin();
        ProjectConfigPage projectPage = browser.openAndWaitFor(ProjectConfigPage.class, childProject, false);
        assertFalse(projectPage.isActionPresent(ProjectConfigurationActions.ACTION_CONVERT_TO_CUSTOM));
        assertFalse(projectPage.isActionPresent(ProjectConfigurationActions.ACTION_CONVERT_TO_VERSIONED));
    }

    public void testCannotConvertProjectWhenTypeIsOverridden() throws Exception
    {
        String parentProject = random + "-parent";
        String childProject = random + "-child";
        xmlRpcHelper.insertSimpleProject(parentProject, true);
        String childPath = xmlRpcHelper.insertTrivialProject(childProject, parentProject, false);

        browser.loginAsAdmin();
        ProjectConfigPage projectPage = browser.openAndWaitFor(ProjectConfigPage.class, parentProject, false);
        assertTrue(projectPage.isActionPresent(ProjectConfigurationActions.ACTION_CONVERT_TO_CUSTOM));
        assertTrue(projectPage.isActionPresent(ProjectConfigurationActions.ACTION_CONVERT_TO_VERSIONED));

        String childTypePath = PathUtils.getPath(childPath, "type");
        Hashtable<String, Object> childType = xmlRpcHelper.getConfig(childTypePath);
        childType.put("defaultRecipe", "meoverridenow");
        xmlRpcHelper.saveConfig(childTypePath, childType, false);

        projectPage.openAndWaitFor();
        assertFalse(projectPage.isActionPresent(ProjectConfigurationActions.ACTION_CONVERT_TO_CUSTOM));
        assertFalse(projectPage.isActionPresent(ProjectConfigurationActions.ACTION_CONVERT_TO_VERSIONED));
    }

    public void testActionsListedForAgent() throws Exception
    {
        browser.loginAsAdmin();
        AgentConfigPage agentPage = browser.openAndWaitFor(AgentConfigPage.class, AgentManager.MASTER_AGENT_NAME, false);
        assertTrue(agentPage.isActionPresent(AgentConfigurationActions.ACTION_PING));
    }

    public void testActionsNotListedForTemplateAgent() throws Exception
    {
        browser.loginAsAdmin();
        AgentConfigPage agentPage = browser.openAndWaitFor(AgentConfigPage.class, AgentManager.GLOBAL_AGENT_NAME, false);
        assertFalse(agentPage.isActionPresent(AgentConfigurationActions.ACTION_PING));
    }

    public void testActionsNotListedForInvalidProject() throws Exception
    {
        xmlRpcHelper.insertTrivialProject(random, false);

        browser.loginAsAdmin();
        ProjectConfigPage projectPage = browser.openAndWaitFor(ProjectConfigPage.class, random, false);
        assertFalse(projectPage.isActionPresent(ProjectConfigurationActions.ACTION_TRIGGER));
        assertFalse(projectPage.isActionPresent(ProjectConfigurationActions.ACTION_CONVERT_TO_CUSTOM));
    }

    public void testActionsListedForTemplateProject() throws Exception
    {
        xmlRpcHelper.insertSimpleProject(random, true);

        browser.loginAsAdmin();
        ProjectConfigPage projectPage = browser.openAndWaitFor(ProjectConfigPage.class, random, false);
        assertFalse(projectPage.isActionPresent(ProjectConfigurationActions.ACTION_TRIGGER));
        assertTrue(projectPage.isActionPresent(ProjectConfigurationActions.ACTION_CONVERT_TO_CUSTOM));
        assertTrue(projectPage.isActionPresent(ProjectConfigurationActions.ACTION_CONVERT_TO_VERSIONED));
    }

    public void testDescendantActions() throws Exception
    {
        String parentName = random + "-parent";
        String child1Name = random + "-child1";
        String child2Name = random + "-child2";

        xmlRpcHelper.insertSimpleProject(parentName, true);

        browser.loginAsAdmin();
        ProjectConfigPage projectPage = browser.openAndWaitFor(ProjectConfigPage.class, parentName, true);
        assertFalse(projectPage.isDescendantActionsPresent());

        String child1Path = xmlRpcHelper.insertTrivialProject(child1Name, parentName, false);
        xmlRpcHelper.insertTrivialProject(child2Name, parentName, false);

        projectPage.openAndWaitFor();
        assertTrue(projectPage.isDescendantActionsPresent());
        assertTrue(projectPage.isDescendantActionPresent(ProjectConfigurationActions.ACTION_PAUSE));
        assertFalse(projectPage.isDescendantActionPresent(ProjectConfigurationActions.ACTION_RESUME));
        assertDefaultAndComplexActionsNotShown(projectPage);

        projectPage.clickDescendantActionAndWait(ProjectConfigurationActions.ACTION_PAUSE);
        assertTextPresent("action 'pause' triggered on 2 descendants");
        assertFalse(projectPage.isDescendantActionPresent(ProjectConfigurationActions.ACTION_PAUSE));
        assertTrue(projectPage.isDescendantActionPresent(ProjectConfigurationActions.ACTION_RESUME));

        assertEquals(Project.State.PAUSED, xmlRpcHelper.getProjectState(child1Name));
        assertEquals(Project.State.PAUSED, xmlRpcHelper.getProjectState(child2Name));

        xmlRpcHelper.doConfigAction(child1Path, ProjectConfigurationActions.ACTION_RESUME);
        projectPage.openAndWaitFor();
        assertTrue(projectPage.isDescendantActionPresent(ProjectConfigurationActions.ACTION_PAUSE));
        assertTrue(projectPage.isDescendantActionPresent(ProjectConfigurationActions.ACTION_RESUME));

        projectPage.clickDescendantActionAndWait(ProjectConfigurationActions.ACTION_RESUME);
        assertTextPresent("action 'resume' triggered on 1 descendant");
        assertTrue(projectPage.isDescendantActionPresent(ProjectConfigurationActions.ACTION_PAUSE));
        assertFalse(projectPage.isDescendantActionPresent(ProjectConfigurationActions.ACTION_RESUME));
    }

    private void assertDefaultAndComplexActionsNotShown(ProjectConfigPage projectPage)
    {
        assertFalse(projectPage.isDescendantActionPresent(ProjectConfigurationActions.ACTION_TRIGGER));
        assertFalse(projectPage.isDescendantActionPresent(ProjectConfigurationActions.ACTION_CONVERT_TO_CUSTOM));
        assertFalse(projectPage.isDescendantActionPresent(ConfigurationRefactoringManager.ACTION_CLONE));
        assertFalse(projectPage.isDescendantActionPresent(ConfigurationRefactoringManager.ACTION_PULL_UP));
        assertFalse(projectPage.isDescendantActionPresent(ConfigurationRefactoringManager.ACTION_PUSH_DOWN));
        assertFalse(projectPage.isDescendantActionPresent(AccessManager.ACTION_DELETE));
        assertFalse(projectPage.isDescendantActionPresent(AccessManager.ACTION_VIEW));
    }
}
