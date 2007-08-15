package com.zutubi.pulse.model;

import com.zutubi.pulse.core.model.Revision;
import com.zutubi.pulse.prototype.config.project.ProjectConfiguration;
import com.zutubi.pulse.core.scm.ScmClient;
import com.zutubi.pulse.core.scm.ScmClientFactory;
import com.zutubi.pulse.core.scm.ScmException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 */
public class ChangelistIsolator
{
    private Map<Long, Revision> latestRequestedRevisions = new TreeMap<Long, Revision>();
    private BuildManager buildManager;
    private ScmClientFactory scmClientFactory;

    public ChangelistIsolator(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }

    public synchronized List<Revision> getRevisionsToRequest(ProjectConfiguration projectConfig, Project project, boolean force) throws ScmException
    {
        List<Revision> result;
        Revision latestBuiltRevision;

        if (latestRequestedRevisions.containsKey(projectConfig.getHandle()))
        {
            latestBuiltRevision = latestRequestedRevisions.get(projectConfig.getHandle());
        }
        else
        {
            // We have not yet seen a request for this project. Find out what
            // the last revision built of this project was (if any).
            latestBuiltRevision = getLatestBuiltRevision(project);
            if (latestBuiltRevision != null)
            {
                latestRequestedRevisions.put(projectConfig.getHandle(), latestBuiltRevision);
            }
        }

        if (latestBuiltRevision == null)
        {
            // The spec has never been built or even requested.  Just build
            // the latest (we need to start somewhere!).
            ScmClient client = scmClientFactory.createClient(projectConfig.getScm());
            result = Arrays.asList(client.getLatestRevision());
        }
        else
        {
            // We now have the last requested revision, return every revision
            // since then.
            ScmClient client = scmClientFactory.createClient(projectConfig.getScm());
            result = client.getRevisions(latestBuiltRevision, null);
        }

        if (result.size() > 0)
        {
            // Remember the new latest
            latestRequestedRevisions.put(projectConfig.getHandle(), result.get(result.size() - 1));
        }
        else if (force)
        {
            // Force a build of the latest revision anyway
            // We need to copy the revision as they are unique in the BUILD_SCM table!
            assert latestBuiltRevision != null;
            result = Arrays.asList(latestBuiltRevision.copy());
        }

        return result;
    }

    private Revision getLatestBuiltRevision(Project project)
    {
        for (int first = 0; /* forever */ ; first++)
        {
            List<BuildResult> latest = buildManager.queryBuilds(new Project[]{project}, null, -1, -1, null, first, 1, true);
            if (latest.size() > 0)
            {
                Revision revision = latest.get(0).getRevision();
                if (revision != null)
                {
                    return revision;
                }
            }
            else
            {
                return null;
            }
        }
    }

    public void setBuildManager(BuildManager buildManager)
    {
        this.buildManager = buildManager;
    }

    public void setScmClientFactory(ScmClientFactory scmClientFactory)
    {
        this.scmClientFactory = scmClientFactory;
    }
}
