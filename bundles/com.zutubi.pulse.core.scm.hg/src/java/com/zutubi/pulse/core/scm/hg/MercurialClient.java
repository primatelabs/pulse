package com.zutubi.pulse.core.scm.hg;

import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.engine.api.ResourceProperty;
import com.zutubi.pulse.core.scm.api.*;
import com.zutubi.pulse.core.scm.hg.config.MercurialConfiguration;
import com.zutubi.pulse.core.scm.process.api.ScmOutputCapturingHandler;
import com.zutubi.pulse.core.scm.process.api.ScmOutputHandler;
import com.zutubi.pulse.core.scm.process.api.ScmOutputHandlerSupport;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.FileSystemUtils;
import com.zutubi.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.zutubi.pulse.core.scm.hg.MercurialConstants.*;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Implementation of the {@link com.zutubi.pulse.core.scm.api.ScmClient} interface for the
 * Mercurial source control system (http://git.or.cz/).
 */
public class MercurialClient implements ScmClient
{
    public static final String TYPE = "hg";
    /**
     * Timeout for acquiring the ScmContext lock.
     */
    private static final int DEFAULT_TIMEOUT = 120;
    private static final Set<ScmCapability> CAPABILITIES = EnumSet.complementOf(EnumSet.of(ScmCapability.BROWSE, ScmCapability.EMAIL));

    private MercurialConfiguration config;
    private MercurialCore hg;

    public MercurialClient(MercurialConfiguration config)
    {
        this.config = config;
        hg = new MercurialCore(config.isInactivityTimeoutEnabled() ? config.getInactivityTimeoutSeconds() : 0);
    }

    /**
     * Prepare the local clone of the remote repository.  This local clone will subsequently
     * be used for browsing, checking for changes, determining changelists etc etc.
     *
     * @param context the scm context in which this client will be operating.
     * @throws ScmException if we encounter a problem
     */
    public void init(ScmContext context, ScmFeedbackHandler handler) throws ScmException
    {
        File workingDir = context.getPersistentWorkingDir();
        if (workingDir.exists() && !FileSystemUtils.rmdir(workingDir))
        {
            throw new ScmException("Init failed. Could not delete directory: " + workingDir.getAbsolutePath());
        }

        hg.setWorkingDirectory(workingDir.getParentFile());
        handler.status("Initialising clone of repository '" + config.getRepository() + "'...");
        ScmOutputHandlerSupport outputHandler = new ScmOutputHandlerSupport(handler);
        hg.clone(outputHandler, config.getRepository(), config.getBranch(), null, workingDir.getName());
        handler.status("Repository cloned.");
        hg.setWorkingDirectory(workingDir);
        hg.update(outputHandler, null);
    }

    public void destroy(ScmContext context, ScmFeedbackHandler handler) throws ScmException
    {
        // Nothing to do - the directory is deleted for us.
    }

    public void close()
    {
        // noop.  We do not keep any processes active, and the persistent directory
        // remains for the duration of the scm configuration.
    }

    public Set<ScmCapability> getCapabilities(ScmContext context)
    {
        if (context != null)
        {
            return CAPABILITIES;
        }

        EnumSet<ScmCapability> capabilities = EnumSet.copyOf(CAPABILITIES);
        capabilities.remove(ScmCapability.BROWSE);
        return capabilities;
    }

    public String getUid() throws ScmException
    {
        return config.getRepository();
    }

    public String getLocation() throws ScmException
    {
        return getUid();
    }

    public List<ResourceProperty> getProperties(ExecutionContext context) throws ScmException
    {
        return Arrays.asList(
                new ResourceProperty("hg.repository", config.getRepository())
        );
    }

    public Revision checkout(ExecutionContext context, Revision revision, ScmFeedbackHandler handler) throws ScmException
    {
        File workingDir = context.getWorkingDir();
        if (workingDir.exists() && !FileSystemUtils.rmdir(workingDir))
        {
            throw new ScmException("Could not delete directory '" + workingDir.getAbsolutePath() + "'");
        }

        // hg clone --noupdate [--branch <branch>] <repository> <dir>
        // cd <dir>
        // hg update --rev <revision>
        ScmOutputHandler outputHandler = new ScmOutputHandlerSupport(handler);
        hg.setWorkingDirectory(workingDir.getParentFile());
        hg.clone(outputHandler, config.getRepository(), config.getBranch(), null, workingDir.getName());

        hg.setWorkingDirectory(workingDir);
        hg.update(outputHandler, getRevisionString(revision));

        return new Revision(hg.parents());
    }

    public Revision update(ExecutionContext context, Revision revision, ScmFeedbackHandler handler) throws ScmException
    {
        File workingDir = context.getWorkingDir();
        if (!isMercurialRepository(workingDir))
        {
            return checkout(context, revision, handler);
        }

        ScmOutputHandlerSupport outputHandler = new ScmOutputHandlerSupport(handler);
        hg.setWorkingDirectory(workingDir);
        hg.pull(outputHandler, config.getBranch());
        hg.update(outputHandler, safeRevisionString(revision));
        return new Revision(hg.parents());
    }

    public InputStream retrieve(ScmContext context, String path, Revision revision) throws ScmException
    {
        context.tryLock(DEFAULT_TIMEOUT, SECONDS);
        try
        {
            preparePersistentDirectory(null, context.getPersistentWorkingDir());
            return hg.cat(path, getRevisionString(revision));
        }
        finally
        {
            context.unlock();
        }
    }

    public void storeConnectionDetails(ExecutionContext context, File outputDir) throws ScmException, IOException
    {

    }

    public EOLStyle getEOLPolicy(ExecutionContext context) throws ScmException
    {
        return EOLStyle.BINARY;
    }

    public Revision getLatestRevision(ScmContext context) throws ScmException
    {
        context.tryLock(DEFAULT_TIMEOUT, SECONDS);
        try
        {
            preparePersistentDirectory(null, context.getPersistentWorkingDir());
            String symbolicRevision = config.getBranch();
            if (!StringUtils.stringSet(symbolicRevision))
            {
                symbolicRevision = REVISION_TIP;
            }

            List<Changelist> logs = hg.log(false, null, symbolicRevision, symbolicRevision, 1);
            return logs.get(0).getRevision();
        }
        finally
        {
            context.unlock();
        }
    }

    private void preparePersistentDirectory(ScmOutputHandler handler, File workingDir) throws ScmException
    {
        if (!isMercurialRepository(workingDir))
        {
            String path;
            try
            {
                path = workingDir.getCanonicalPath();
            }
            catch (IOException e)
            {
                path = workingDir.getAbsolutePath();
            }

            throw new ScmException("Mercurial repository not found: " + path);
        }
        else
        {
            hg.setWorkingDirectory(workingDir);
            hg.pull(handler, config.getBranch());
            hg.update(handler, null);
        }
    }

    private boolean isMercurialRepository(File dir)
    {
        return new File(dir, ".hg").isDirectory();
    }

    public List<Revision> getRevisions(ScmContext context, Revision from, Revision to) throws ScmException
    {
        context.tryLock(DEFAULT_TIMEOUT, SECONDS);
        try
        {
            preparePersistentDirectory(null, context.getPersistentWorkingDir());

            List<Changelist> changelists = hg.log(false, safeBranch(), safeRevisionString(from), safeRevisionString(to), -1);
            if (changelists.size() > 0)
            {
                changelists = changelists.subList(1, changelists.size());
            }

            List<Revision> revisions = new LinkedList<Revision>();
            for (Changelist changelist : changelists)
            {
                revisions.add(changelist.getRevision());
            }
            return revisions;
        }
        finally
        {
            context.unlock();
        }
    }

    private String safeBranch()
    {
        String branch = config.getBranch();
        if (!StringUtils.stringSet(branch))
        {
            branch = BRANCH_DEFAULT;
        }

        return branch;
    }

    private String safeRevisionString(Revision rev)
    {
        return rev == null ? null : rev.getRevisionString();
    }

    public List<Changelist> getChanges(ScmContext context, Revision from, Revision to) throws ScmException
    {
        context.tryLock(DEFAULT_TIMEOUT, SECONDS);
        try
        {
            preparePersistentDirectory(null, context.getPersistentWorkingDir());
            String safeFromRevision = safeRevisionString(from);
            List<Changelist> changelists = hg.log(true, safeBranch(), safeFromRevision, safeRevisionString(to), -1);
            if (changelists.size() > 0 && !REVISION_ZERO.equals(safeFromRevision))
            {
                // Mercurial ranges are inclusive, we exclude the lower bound.
                changelists = changelists.subList(1, changelists.size());
            }

            return CollectionUtils.filter(changelists, new ChangelistPathsPredicate(config.getFilterPaths()));
        }
        finally
        {
            context.unlock();
        }
    }

    public List<ScmFile> browse(ScmContext context, String path, Revision revision) throws ScmException
    {
        throw new ScmException("Operation not supported");
    }

    public void tag(ScmContext scmContext, ExecutionContext context, Revision revision, String name, boolean moveExisting) throws ScmException
    {
        scmContext.tryLock(DEFAULT_TIMEOUT, SECONDS);
        try
        {
            preparePersistentDirectory(null, scmContext.getPersistentWorkingDir());
            hg.tag(null, revision, name, "[pulse] applying tag", moveExisting);
            hg.push(null, config.getBranch());
        }
        finally
        {
            scmContext.unlock();
        }
    }

    public Revision parseRevision(ScmContext context, String revision) throws ScmException
    {
        // FIXME this is not validating anything as it should.
        if (!StringUtils.stringSet(revision))
        {
            throw new ScmException("Unexpected revision format: '" + revision + "'");
        }
        return new Revision(revision);
    }

    public Revision getPreviousRevision(ScmContext context, Revision fileRevision, boolean isFile) throws ScmException
    {
        return null;
    }

    public String getEmailAddress(ScmContext context, String user) throws ScmException
    {
        throw new ScmException("Operation not supported");
    }

    private String getRevisionString(Revision revision)
    {
        if (revision == null)
        {
            return REVISION_TIP;
        }
        else
        {
            return revision.getRevisionString();
        }
    }
    
    public void testConnection() throws ScmException
    {
        File tempDir = null;
        try
        {
            tempDir = FileSystemUtils.createTempDir(getClass().getName());
            ScmOutputCapturingHandler handler = new ScmOutputCapturingHandler();
            hg.clone(handler, config.getRepository(), config.getBranch(), REVISION_NULL, tempDir.getAbsolutePath());

            String stderr = handler.getError().trim();
            if (StringUtils.stringSet(stderr))
            {
                throw new ScmException("Command '" + handler.getCommandLine() + "' output error: " + stderr);
            }
        }
        catch (IOException e)
        {
            throw new ScmException(e);
        }
        finally
        {
            if (tempDir != null)
            {
                FileSystemUtils.rmdir(tempDir);
            }
        }
    }
}
