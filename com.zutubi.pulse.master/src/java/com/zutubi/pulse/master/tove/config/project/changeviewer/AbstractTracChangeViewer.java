package com.zutubi.pulse.master.tove.config.project.changeviewer;

import com.zutubi.pulse.core.scm.api.FileChange;
import com.zutubi.pulse.core.scm.api.Revision;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.util.StringUtils;

/**
 * Base class shared by implementations for different trac versions.
 */
@SymbolicName("zutubi.abstractTracChangeViewerConfig")
public abstract class AbstractTracChangeViewer extends BasePathChangeViewer
{
    public AbstractTracChangeViewer(String baseURL, String projectPath)
    {
        super(baseURL, projectPath);
    }

    public String getRevisionURL(Revision revision)
    {
        return StringUtils.join("/", true, true, getBaseURL(), "changeset", revision.getRevisionString());
    }

    public String getFileViewURL(ChangeContext context, FileChange fileChange)
    {
        return StringUtils.join("/", true, true, getBaseURL(), "browser", StringUtils.urlEncodePath(fileChange.getPath()) + "?rev=" + fileChange.getRevision());
    }

    public String getFileDownloadURL(ChangeContext context, FileChange fileChange)
    {
        return getFileViewURL(context, fileChange) + "&format=raw";
    }
}