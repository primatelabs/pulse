package com.zutubi.pulse.master.vfs.provider.pulse;

import com.google.common.base.Predicate;
import com.zutubi.pulse.core.model.CommandResult;
import com.zutubi.pulse.core.model.StoredArtifact;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.provider.AbstractFileSystem;

import java.util.LinkedList;
import java.util.List;

/**
 * File representing a single command in an artifacts tree.
 */
public class CommandResultFileObject extends AbstractPulseFileObject implements CommandResultProvider
{
    private final long commandResultId;

    private String displayName;

    public CommandResultFileObject(final FileName name, final long commandResultId, final AbstractFileSystem fs)
    {
        super(name, fs);

        this.commandResultId = commandResultId;
    }

    public AbstractPulseFileObject createFile(final FileName fileName)
    {
        long artifactId = Long.parseLong(fileName.getBaseName());

        return objectFactory.buildBean(ArtifactFileObject.class, fileName, artifactId, pfs);
    }

    protected FileType doGetType() throws Exception
    {
        return FileType.FOLDER;
    }

    protected String[] doListChildren() throws Exception
    {
        List<String> children = new LinkedList<String>();

        for (StoredArtifact artifact : getCommandResult().getArtifacts())
        {
            children.add(Long.toString(artifact.getId()));
        }
        return children.toArray(new String[children.size()]);
    }

    public String getDisplayName()
    {
        if (displayName == null)
        {
            CommandResult commandResult = getCommandResult();
            displayName = String.format("command :: %s", commandResult.getCommandName());
        }
        return displayName;
    }

    public CommandResult getCommandResult()
    {
        return buildManager.getCommandResult(getCommandResultId());
    }

    public long getCommandResultId()
    {
        return commandResultId;
    }

    public boolean isExplicit() throws FileSystemException
    {
        return isAnyChild(new Predicate<ArtifactFileObject>()
        {
            public boolean apply(ArtifactFileObject file)
            {
                return file.isExplicit();
            }
        });
    }

    public boolean isFeatured() throws FileSystemException
    {
        return isAnyChild(new Predicate<ArtifactFileObject>()
        {
            public boolean apply(ArtifactFileObject file)
            {
                return file.isFeatured();
            }
        });
    }

    private boolean isAnyChild(Predicate<ArtifactFileObject> p) throws FileSystemException
    {
        FileObject[] children = getChildren();
        for (FileObject child: children)
        {
            if (p.apply((ArtifactFileObject) child))
            {
                return true;
            }
        }

        return false;
    }
}
