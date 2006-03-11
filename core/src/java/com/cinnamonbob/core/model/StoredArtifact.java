package com.cinnamonbob.core.model;

import java.util.*;
import java.io.File;

/**
 */
public class StoredArtifact extends Entity
{
    private String name;
    /**
     * Files stored as part of this artifact.  A common special case is just
     * a single file.
     */
    List<StoredFileArtifact> children = new LinkedList<StoredFileArtifact>();

    public StoredArtifact()
    {
    }

    public StoredArtifact(String name)
    {
        this.name = name;
    }

    public StoredArtifact(String name, StoredFileArtifact file)
    {
        this.name = name;
        this.children.add(file);
    }

    public String getName()
    {
        return name;
    }

    private void setName(String name)
    {
        this.name = name;
    }

    public void add(StoredFileArtifact child)
    {
        children.add(child);
    }

    public List<StoredFileArtifact> getChildren()
    {
        return children;
    }

    private void setChildren(List<StoredFileArtifact> children)
    {
        this.children = children;
    }

    public boolean isSingleFile()
    {
        return children.size() == 1;
    }

    public StoredFileArtifact getFile()
    {
        return children.get(0);
    }

    public boolean hasFeatures()
    {
        for (StoredFileArtifact child : children)
        {
            if (child.hasFeatures())
            {
                return true;
            }
        }

        return false;
    }

    public Iterable<Feature.Level> getLevels()
    {
        Set<Feature.Level> result = new TreeSet<Feature.Level>();
        for (StoredFileArtifact child : children)
        {
            for (Feature.Level level : child.getLevels())
            {
                result.add(level);
            }
        }

        return result;
    }

    public boolean hasMessages(Feature.Level level)
    {
        for (StoredFileArtifact child : children)
        {
            if (child.hasMessages(level))
            {
                return true;
            }
        }

        return false;
    }

    public List<Feature> getFeatures(Feature.Level level)
    {
        List<Feature> result = new LinkedList<Feature>();
        for(StoredFileArtifact child: children)
        {
            result.addAll(child.getFeatures(level));
        }

        return result;
    }

    public String trimmedPath(StoredFileArtifact artifact)
    {
        String path = artifact.getPath();
        if(path.startsWith(name))
        {
            path = path.substring(name.length());
        }

        if(path.startsWith(File.separator))
        {
            path = path.substring(1);
        }

        if(path.startsWith("/"))
        {
            path = path.substring(1);
        }

        if(path.startsWith("\\"))
        {
            path = path.substring(1);
        }

        return path;
    }
}
