package com.zutubi.pulse.master.xwork.actions.project;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.zutubi.pulse.core.model.PersistentTestSuiteResult;
import com.zutubi.pulse.core.model.RecipeResult;
import com.zutubi.pulse.core.model.TestSuitePersister;
import com.zutubi.pulse.master.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.master.model.RecipeResultNode;
import com.zutubi.util.StringUtils;
import com.zutubi.util.WebUtils;
import com.zutubi.util.io.FileSystemUtils;
import com.zutubi.util.logging.Logger;

import java.io.File;
import java.util.List;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;

/**
 * The action that allows for the viewing of a test suite.  The input for this
 * action is the path that defines the suite of interest.
 * <ul>
 * <li>path: the path of the suite in question</li>
 * </ul>
 */
public class ViewTestSuiteAction extends StageActionBase
{
    private static final Logger LOG = Logger.getLogger(ViewTestSuiteAction.class);

    private String path;
    private PersistentTestSuiteResult suite;
    private List<String> paths;
    private boolean filterTests;
    private MasterConfigurationManager configurationManager;

    /**
     * The path of the suite to be viewed by this action.
     *
     * @param path  of the suite
     */
    public void setPath(String path)
    {
        this.path = path;
    }

    /**
     * Get the path of the suite that is being viewed by this action.
     *
     * @return the suites path
     */
    public String getPath()
    {
        return path;
    }

    /**
     * Get the suite being viewed by this action
     * 
     * @return  the suite or null if the suite does not exist.
     */
    public PersistentTestSuiteResult getSuite()
    {
        return suite;
    }

    /**
     * Returns true if a suite exists at the specified path.
     * @return  true if a suite exists, false otherwise.
     */
    public boolean getSuiteExists()
    {
        return suite != null;
    }

    public List<String> getPaths()
    {
        return paths;
    }

    public boolean isFilterTests()
    {
        return filterTests;
    }

    /**
     * Get the path to a test that is a child of this test suite.
     *
     * @param childName     name of the child entity.
     *
     * @return the path to the child
     */
    public String getChildUriPath(String childName)
    {
        String uriChildName = WebUtils.uriComponentEncode(childName);
        if (paths == null)
        {
            return uriChildName;
        }
        else
        {
            String uriPath = StringUtils.join("/", transform(paths, new Function<String, String>()
            {
                public String apply(String s)
                {
                    return WebUtils.uriComponentEncode(s);
                }
            }));

            return uriPath + "/" + uriChildName;
        }
    }

    public void validate()
    {

    }

    public String execute()
    {
        RecipeResultNode node = getRequiredRecipeResultNode();
        if(node.getResult() == null)
        {
            addActionError("Invalid stage [" + getStageName() + "]");
            return ERROR;
        }

        File testDir = new File(node.getResult().getAbsoluteOutputDir(configurationManager.getDataDirectory()), RecipeResult.TEST_DIR);

        path = StringUtils.stripPrefix(path, "/");
        if(StringUtils.stringSet(path))
        {
            String[] elements = path.split("/");
            paths = newArrayList(transform(asList(elements), new Function<String, String>()
            {
                public String apply(String s)
                {
                    return WebUtils.uriComponentDecode(s);
                }
            }));

            String[] encodedElements = Collections2.transform(paths, new Function<String, String>()
            {
                public String apply(String s)
                {
                    return urlEncode(s);
                }
            }).toArray(new String[elements.length]);

            testDir = new File(testDir, FileSystemUtils.composeFilename(encodedElements));
        }

        if (testDir.isDirectory())
        {
            TestSuitePersister persister = new TestSuitePersister();
            try
            {
                suite = persister.read(null, testDir, false, false, -1);
            }
            catch (Exception e)
            {
                LOG.warning(e);
                addActionError("Unable to read test suite from directory '" + testDir.getAbsolutePath() + "': " + e.getMessage());
                return ERROR;
            }
        }

        filterTests = TestFilterContext.isFilterEnabledForBuild(getBuildResult().getId());

        return SUCCESS;
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }
}
