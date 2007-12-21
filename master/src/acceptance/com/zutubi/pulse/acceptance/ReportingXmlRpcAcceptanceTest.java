package com.zutubi.pulse.acceptance;

import com.zutubi.prototype.type.record.PathUtils;
import com.zutubi.pulse.agent.AgentManager;
import com.zutubi.pulse.model.ProjectManager;
import com.zutubi.pulse.prototype.config.LabelConfiguration;
import com.zutubi.util.Sort;

import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Tests for the remote API, primarily the reporting functionality.
 * Configration functions are tested in {@link ConfigXmlRpcAcceptanceTest}.
 */
public class ReportingXmlRpcAcceptanceTest extends BaseXmlRpcAcceptanceTest
{
    protected void setUp() throws Exception
    {
        super.setUp();
        loginAsAdmin();
    }

    protected void tearDown() throws Exception
    {
        logout();
        super.tearDown();
    }

    public void testGetAllUserLogins() throws Exception
    {
        getAllHelper(new GetAllHelper()
        {
            public Vector<String> get() throws Exception
            {
                return xmlRpcHelper.getAllUserLogins();
            }

            public void add(String name) throws Exception
            {
                xmlRpcHelper.insertTrivialUser(name);
            }
        });
    }

    public void testGetAllProjectNames() throws Exception
    {
        getAllHelper(new GetAllHelper()
        {
            public Vector<String> get() throws Exception
            {
                return xmlRpcHelper.getAllProjectNames();
            }

            public void add(String name) throws Exception
            {
                xmlRpcHelper.insertTrivialProject(name, false);
            }
        });
    }

    public void testGetAllAgentNamesDoesNotIncludeTemplates() throws Exception
    {
        Vector<String> allAgents = xmlRpcHelper.getAllAgentNames();
        assertFalse(allAgents.contains(AgentManager.GLOBAL_AGENT_NAME));
    }

    public void testGetAllAgentNames() throws Exception
    {
        getAllHelper(new GetAllHelper()
        {
            public Vector<String> get() throws Exception
            {
                return xmlRpcHelper.getAllAgentNames();
            }

            public void add(String name) throws Exception
            {
                xmlRpcHelper.insertSimpleAgent(name);
            }
        });
    }

    public void testGetAllProjectNamesDoesNotIncludeTemplates() throws Exception
    {
        Vector<String> allProjects = xmlRpcHelper.getAllProjectNames();
        assertFalse(allProjects.contains(ProjectManager.GLOBAL_PROJECT_NAME));
    }

    public void testGetMyProjectNamesAllProjects() throws Exception
    {
        Vector<String> allProjects = xmlRpcHelper.getAllProjectNames();
        Vector<String> myProjects = xmlRpcHelper.getMyProjectNames();
        Sort.StringComparator c = new Sort.StringComparator();
        Collections.sort(allProjects, c);
        Collections.sort(myProjects, c);
        assertEquals(allProjects, myProjects);
    }

    public void testGetMyProjectNames() throws Exception
    {
        String random = randomName();
        String login = random + "-user";
        String project = random + "-project";

        String userPath = xmlRpcHelper.insertTrivialUser(login);
        String projectPath = xmlRpcHelper.insertSimpleProject(project, false);
        
        xmlRpcHelper.logout();
        xmlRpcHelper.login(login, "");
        String dashboardPath = PathUtils.getPath(userPath, "preferences", "dashboard");
        Hashtable<String, Object> dashboardSettings = xmlRpcHelper.getConfig(dashboardPath);
        dashboardSettings.put("showAllProjects", false);
        dashboardSettings.put("shownProjects", new Vector<String>(Arrays.asList(projectPath)));
        xmlRpcHelper.saveConfig(dashboardPath, dashboardSettings, true);

        Vector<String> myProjects = xmlRpcHelper.getMyProjectNames();
        assertEquals(1, myProjects.size());
        assertEquals(project, myProjects.get(0));
    }

    public void testGetAllProjectGroups() throws Exception
    {
        String projectName = randomName() + "-project";
        final String projectPath = xmlRpcHelper.insertTrivialProject(projectName, false);

        getAllHelper(new GetAllHelper()
        {
            public Vector<String> get() throws Exception
            {
                return xmlRpcHelper.getAllProjectGroups();
            }

            public void add(String name) throws Exception
            {
                String labelsPath = PathUtils.getPath(projectPath, "labels");
                Hashtable<String, Object> label = xmlRpcHelper.createDefaultConfig(LabelConfiguration.class);
                label.put("label", name);
                xmlRpcHelper.insertConfig(labelsPath, label);
            }
        });
    }

    public void testGetProjectGroup() throws Exception
    {
        String random = randomName();
        String projectName = random + "-project";
        String labelName = random + "-label";

        String projectPath = xmlRpcHelper.insertTrivialProject(projectName, false);
        String labelsPath = PathUtils.getPath(projectPath, "labels");
        Hashtable<String, Object> label = xmlRpcHelper.createDefaultConfig(LabelConfiguration.class);
        label.put("label", labelName);
        xmlRpcHelper.insertConfig(labelsPath, label);

        Hashtable<String, Object> group = xmlRpcHelper.getProjectGroup(labelName);
        assertEquals(labelName, group.get("name"));
        Vector<String> projects = (Vector<String>) group.get("projects");
        assertEquals(1, projects.size());
        assertEquals(projectName, projects.get(0));
    }

    public void testGetProjectGroupNonExistant() throws Exception
    {
        // Groups are virtual: if you ask for a non-existant label, it is
        // just an empty group
        String testName = "something that does not exist";
        Hashtable<String, Object> group = xmlRpcHelper.getProjectGroup(testName);
        assertEquals(testName, group.get("name"));
        Vector<String> projects = (Vector<String>) group.get("projects");
        assertEquals(0, projects.size());
    }

    public void testGetBuild() throws Exception
    {
        // A bit of a sanity check: in reality we use this method for other
        // tests that run builds so it is exercised in a few ways.
        String projectName = randomName();
        insertSimpleProject(projectName);

        xmlRpcHelper.triggerBuild(projectName);

        Hashtable<String, Object> build;
        do
        {
            build = xmlRpcHelper.getBuild(projectName, 1);
        }
        while(build == null || !Boolean.TRUE.equals(build.get("completed")));

        assertEquals(1, build.get("id"));
        assertEquals(projectName, build.get("project"));
        assertEquals("success", build.get("status"));
    }

    public void testGetBuildUnknownProject()
    {
        try
        {
            xmlRpcHelper.getBuild("this is a made up project", 1);
            fail();
        }
        catch(Exception e)
        {
            assertTrue(e.getMessage().contains("Unknown project 'this is a made up project'"));
        }
    }

    public void testGetBuildUnknownBuild() throws Exception
    {
        String projectName = randomName();
        insertSimpleProject(projectName);
        assertNull(xmlRpcHelper.getBuild(projectName, 1));
    }

    public void testDeleteBuild() throws Exception
    {
        String projectName = randomName();
        insertSimpleProject(projectName);
        xmlRpcHelper.runBuild(projectName, 60000);
        
        assertTrue(xmlRpcHelper.deleteBuild(projectName, 1));
        assertNull(xmlRpcHelper.getBuild(projectName, 1));
    }

    public void testDeleteBuildUknownProject() throws Exception
    {
        try
        {
            xmlRpcHelper.deleteBuild("this is a made up project", 1);
            fail();
        }
        catch(Exception e)
        {
            assertTrue(e.getMessage().contains("Unknown project 'this is a made up project'"));
        }
    }

    public void testDeleteBuildUnknownBuild() throws Exception
    {
        String projectName = randomName();
        insertSimpleProject(projectName);
        assertFalse(xmlRpcHelper.deleteBuild(projectName, 1));
    }

    private void getAllHelper(GetAllHelper helper) throws Exception
    {
        String name = randomName();
        Vector<String> all = helper.get();
        int sizeBefore = all.size();
        assertFalse(all.contains(name));
        helper.add(name);
        all = helper.get();
        assertEquals(sizeBefore + 1, all.size());
        assertTrue(all.contains(name));
    }

    private interface GetAllHelper
    {
        Vector<String> get() throws Exception;
        void add(String name) throws Exception;
    }
}
