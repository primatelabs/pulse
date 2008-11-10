package com.zutubi.pulse.acceptance.pages.dashboard;

import com.thoughtworks.selenium.Selenium;
import com.zutubi.pulse.acceptance.pages.browse.CommandArtifactPage;
import com.zutubi.pulse.master.webwork.Urls;

/**
 * The personal build decorated artifact page: contains the artifact content
 * with features highlighted.  Only available for plain text artifacts.
 */
public class PersonalCommandArtifactPage extends CommandArtifactPage
{
    public PersonalCommandArtifactPage(Selenium selenium, Urls urls, String projectName, long buildId, String stageName, String commandName, String artifactPath)
    {
        super(selenium, urls, projectName, buildId, stageName, commandName, artifactPath);
    }

    public String getUrl()
    {
        return urls.dashboardMyCommandArtifacts(Long.toString(getBuildId()), getStageName(), getCommandName()) + getArtifactPath();
    }
}
