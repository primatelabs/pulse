package com.zutubi.pulse.acceptance.pages.dashboard;

import com.thoughtworks.selenium.Selenium;
import com.zutubi.pulse.acceptance.SeleniumUtils;
import com.zutubi.pulse.acceptance.pages.ProjectsSummaryPage;
import com.zutubi.pulse.master.webwork.Urls;
import com.zutubi.pulse.master.xwork.actions.user.ResponsibilityModel;
import com.zutubi.util.StringUtils;
import com.zutubi.util.SystemUtils;
import static com.zutubi.util.Constants.SECOND;

/**
 * The dashboard page shows a user's configurable dashboard.
 */
public class DashboardPage extends ProjectsSummaryPage
{
    private static final String ID_RESPONSIBILITIES = "responsibilities";

    public DashboardPage(Selenium selenium, Urls urls)
    {
        super(selenium, urls, "dashboard-content", "dashboard");
    }

    public String getUrl()
    {
        return urls.dashboard();
    }

    @Override
    public void waitFor()
    {
        super.waitFor();
        SeleniumUtils.waitForVariable(selenium, "view.initialised", 30 * SECOND);
    }

    public void hideGroupAndWait(String group)
    {
        // Under IE for some unknown reason selenium cannot find the group
        // hide element.  I have verified it exists with the expected id, and
        // that it is not a waiting issue.  So I work around this case, but
        // continue to test the link itself on non-Windows systems.
        if (SystemUtils.IS_WINDOWS)
        {
            selenium.open(urls.base() + "user/hideDashboardGroup.action?groupName=" + StringUtils.formUrlEncode(group));
        }
        else
        {
            clickGroupAction(group, ACTION_HIDE);
        }
        selenium.waitForPageToLoad("30000");
        waitFor();
    }

    public void hideProjectAndWait(String group, String project)
    {
        // As above, a workaround for Selenium/IE issues.
        if (SystemUtils.IS_WINDOWS)
        {
            selenium.open(urls.base() + "user/hideDashboardProject.action?projectName=" + StringUtils.formUrlEncode(project));
        }
        else
        {
            clickProjectAction(group, project, ACTION_HIDE);
        }
        selenium.waitForPageToLoad("30000");
        waitFor();
    }

    public boolean hasResponsibilities()
    {
        return selenium.isElementPresent(ID_RESPONSIBILITIES);
    }

    public boolean hasResponsibility(String project)
    {
        return selenium.isElementPresent(ResponsibilityModel.getResponsibilityId(project));
    }

    public void clearResponsibility(String project)
    {
        selenium.click(getClearResponsibilityId(project));
    }

    public boolean isClearResponsibilityPresent(String project)
    {
        return selenium.isElementPresent(getClearResponsibilityId(project));
    }

    private String getClearResponsibilityId(String project)
    {
        return "clear-" + ResponsibilityModel.getResponsibilityId(project);
    }
}
