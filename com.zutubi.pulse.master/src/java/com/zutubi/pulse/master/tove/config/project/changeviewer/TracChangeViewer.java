package com.zutubi.pulse.master.tove.config.project.changeviewer;

import com.zutubi.pulse.core.scm.api.FileChange;
import com.zutubi.pulse.core.scm.api.Revision;
import com.zutubi.pulse.core.scm.api.ScmException;
import com.zutubi.tove.annotations.Form;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * A change viwer implementation for linking to a Trac instance.
 */
@Form(fieldOrder = {"baseURL", "projectPath"})
@SymbolicName("zutubi.tracChangeViewerConfig")
public class TracChangeViewer extends AbstractTracChangeViewer
{
    public TracChangeViewer()
    {
        super(null, null);
    }

    public TracChangeViewer(String baseURL, String projectPath)
    {
        super(baseURL, projectPath);
    }

    public String getFileDiffURL(ChangeContext context, FileChange fileChange) throws ScmException
    {
        Revision previous = context.getPreviousFileRevision(fileChange);
        if(previous == null)
        {
            return null;
        }

        return StringUtils.join("/", true, true, getBaseURL(), "changeset?new=" + getDiffPath(fileChange.getPath(), fileChange.getRevision()) + "&old=" + getDiffPath(fileChange.getPath(), previous));
    }

    private String getDiffPath(String path, Revision revision)
    {
        String result = StringUtils.join("/", path + "@" + revision);
        if(result.startsWith("/"))
        {
            result = result.substring(1);
        }
        
        try
        {
            return URLEncoder.encode(result, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            // Programmer error!
            return result;
        }
    }
}
