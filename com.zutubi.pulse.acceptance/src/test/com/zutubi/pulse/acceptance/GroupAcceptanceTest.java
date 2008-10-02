package com.zutubi.pulse.acceptance;

import com.zutubi.pulse.acceptance.forms.admin.GroupForm;
import com.zutubi.pulse.acceptance.pages.admin.HierarchyPage;
import com.zutubi.pulse.acceptance.pages.admin.ListPage;
import com.zutubi.pulse.acceptance.pages.admin.ProjectHierarchyPage;
import com.zutubi.pulse.master.model.ProjectManager;
import com.zutubi.pulse.master.tove.config.group.ServerPermission;
import com.zutubi.tove.config.ConfigurationRegistry;
import com.zutubi.tove.type.record.PathUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test for user groups.
 */
@Test(dependsOnGroups = {"init.*"})
public class GroupAcceptanceTest extends SeleniumTestBase
{

    @BeforeMethod
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    @AfterMethod
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testCreateEmptyGroup()
    {
        loginAsAdmin();
        
        ListPage groupsPage = new ListPage(selenium, urls, ConfigurationRegistry.GROUPS_SCOPE);
        groupsPage.goTo();
        groupsPage.clickAdd();

        GroupForm form = new GroupForm(selenium);
        form.waitFor();
        form.finishFormElements(random, null, null);

        waitForElement(getGroupPath(random));
        form.assertFormPresent();
        form.assertFormElements(random, "", "");
    }

    public void testAddUserToGroup() throws Exception
    {
        String login = "u:" + random;
        String userHandle;

        xmlRpcHelper.loginAsAdmin();
        try
        {
            String userPath = xmlRpcHelper.insertTrivialUser(login);
            userHandle = xmlRpcHelper.getConfigHandle(userPath);
        }
        finally
        {
            xmlRpcHelper.logout();
        }

        login(login, "");
        ProjectHierarchyPage globalPage = new ProjectHierarchyPage(selenium, urls, ProjectManager.GLOBAL_PROJECT_NAME, true);
        globalPage.goTo();
        assertFalse(globalPage.isAddPresent());
        logout();

        loginAsAdmin();
        ListPage groupsPage = new ListPage(selenium, urls, ConfigurationRegistry.GROUPS_SCOPE);
        groupsPage.goTo();
        groupsPage.clickAdd();

        GroupForm form = new GroupForm(selenium);
        form.waitFor();
        form.finishFormElements(random, null, ServerPermission.CREATE_PROJECT.toString());
        waitForElement(getGroupPath(random));
        form.waitFor();
        form.applyFormElements(null, userHandle, null);
        form.waitFor();
        logout();

        login(login, "");
        globalPage.goTo();
        SeleniumUtils.waitForElementId(selenium, HierarchyPage.LINK_ADD);
        logout();
    }

    public void testAddPermissionToGroup() throws Exception
    {
        String login = "u:" + random;
        String userHandle;

        xmlRpcHelper.loginAsAdmin();
        try
        {
            String userPath = xmlRpcHelper.insertTrivialUser(login);
            userHandle = xmlRpcHelper.getConfigHandle(userPath);
        }
        finally
        {
            xmlRpcHelper.logout();
        }

        loginAsAdmin();
        ListPage groupsPage = new ListPage(selenium, urls, ConfigurationRegistry.GROUPS_SCOPE);
        groupsPage.goTo();
        groupsPage.clickAdd();

        GroupForm form = new GroupForm(selenium);
        form.waitFor();
        form.finishFormElements(random, userHandle, null);
        waitForElement(getGroupPath(random));
        logout();

        login(login, "");
        ProjectHierarchyPage globalPage = new ProjectHierarchyPage(selenium, urls, ProjectManager.GLOBAL_PROJECT_NAME, true);
        globalPage.goTo();
        assertFalse(globalPage.isAddPresent());
        logout();

        loginAsAdmin();
        goTo(urls.adminGroup(random));
        form.waitFor();
        form.applyFormElements(null, null, ServerPermission.CREATE_PROJECT.toString());
        form.waitFor();
        logout();

        login(login, "");
        globalPage.goTo();
        assertTrue(globalPage.isAddPresent());
        logout();
    }

    private String getGroupPath(String group)
    {
        return PathUtils.getPath(ConfigurationRegistry.GROUPS_SCOPE, group);
    }
}
