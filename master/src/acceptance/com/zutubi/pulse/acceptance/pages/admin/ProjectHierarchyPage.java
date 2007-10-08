package com.zutubi.pulse.acceptance.pages.admin;

import com.thoughtworks.selenium.Selenium;
import com.zutubi.pulse.acceptance.pages.SeleniumPage;
import com.zutubi.pulse.webwork.mapping.Urls;
import com.zutubi.util.CollectionUtils;
import junit.framework.Assert;

/**
 * The page shown when looking at a project in the heirarchy view.
 */
public class ProjectHierarchyPage extends SeleniumPage
{
    public static final String LINK_ADD = "add.new";
    public static final String LINK_ADD_TEMPLATE = "add.template";
    public static final String LINK_CONFIGURE = "configure";
    public static final String LINK_DELETE = "delete";

    private String project;
    private boolean template;

    public ProjectHierarchyPage(Selenium selenium, Urls urls, String project, boolean template)
    {
        super(selenium, urls, "projects/" + project);
        this.project = project;
        this.template = template;
    }

    public void assertPresent()
    {
        super.assertPresent();
        
        String[] links = selenium.getAllLinks();
        if (template)
        {
            Assert.assertTrue(CollectionUtils.contains(links, LINK_ADD));
            Assert.assertTrue(CollectionUtils.contains(links, LINK_ADD_TEMPLATE));
        }

        Assert.assertTrue(CollectionUtils.contains(links, LINK_CONFIGURE));
    }

    public String getUrl()
    {
        return urls.adminProjects();
    }

    public boolean isAddPresent()
    {
        return selenium.isElementPresent(LINK_ADD);
    }
    
    public void clickAdd()
    {
        selenium.click(LINK_ADD);
    }

    public void clickAddTemplate()
    {
        selenium.click(LINK_ADD_TEMPLATE);
    }

    public void setTemplate(boolean template)
    {
        this.template = template;
    }

    public ProjectConfigPage clickConfigure()
    {
        selenium.click(LINK_CONFIGURE);
        ProjectConfigPage configPage = new ProjectConfigPage(selenium, urls, project, template);
        configPage.waitFor();
        return configPage;
    }
}
