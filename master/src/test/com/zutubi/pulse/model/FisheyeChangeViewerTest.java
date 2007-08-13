package com.zutubi.pulse.model;

import com.zutubi.prototype.config.MockConfigurationProvider;
import com.zutubi.pulse.core.config.Configuration;
import com.zutubi.pulse.core.model.NumericalFileRevision;
import com.zutubi.pulse.core.model.Revision;
import com.zutubi.pulse.prototype.config.project.ProjectConfiguration;
import com.zutubi.pulse.prototype.config.project.changeviewer.FisheyeConfiguration;
import com.zutubi.pulse.scm.config.ScmConfiguration;
import com.zutubi.pulse.test.PulseTestCase;

/**
 */
public class FisheyeChangeViewerTest extends PulseTestCase
{
    private static final String BASE = "http://fisheye.cinnamonbob.com";
    private static final String PATH = "Zutubi";

    private FisheyeConfiguration viewer;

    protected void setUp() throws Exception
    {
        // it is a bit of work to inject a working configuration provider this way.  Need to find a better way.
        final ProjectConfiguration project = new ProjectConfiguration();
        project.setScm(new ScmConfiguration()
        {
            public String getType()
            {
                return "mock";
            }
        });
        viewer = new FisheyeConfiguration(BASE, PATH);
        viewer.setConfigurationProvider(new MockConfigurationProvider()
        {
            public <T extends Configuration> T getAncestorOfType(Configuration c, Class<T> clazz)
            {
                return (T)project;
            }
        });
    }

    public void testGetChangesetURL()
    {
        assertEquals("http://fisheye.cinnamonbob.com/changelog/Zutubi/?cs=2508", viewer.getChangesetURL(new Revision(null, null, null, "2508")));   
    }

    public void testGetFileViewURL()
    {
        assertEquals("http://fisheye.cinnamonbob.com/browse/Zutubi/pulse/trunk/master/src/java/com/zutubi/pulse/api/RemoteApi.java?r=2508", viewer.getFileViewURL("/pulse/trunk/master/src/java/com/zutubi/pulse/api/RemoteApi.java", new NumericalFileRevision(2508)));   
    }

    public void testGetFileDownloadURL()
    {
        assertEquals("http://fisheye.cinnamonbob.com/browse/~raw,r=2508/Zutubi/pulse/trunk/master/src/java/com/zutubi/pulse/api/RemoteApi.java", viewer.getFileDownloadURL("/pulse/trunk/master/src/java/com/zutubi/pulse/api/RemoteApi.java", new NumericalFileRevision(2508)));
    }

    public void testGetFileDiffURL()
    {
        assertEquals("http://fisheye.cinnamonbob.com/browse/Zutubi/pulse/trunk/master/src/java/com/zutubi/pulse/api/RemoteApi.java?r1=2507&r2=2508", viewer.getFileDiffURL("/pulse/trunk/master/src/java/com/zutubi/pulse/api/RemoteApi.java", new NumericalFileRevision(2508)));
    }
}
