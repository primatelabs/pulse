package com.zutubi.pulse.core.scm;

import com.zutubi.pulse.util.FileSystemUtils;

/**
 * Represents a file or directory stored in an SCM.
 */
public class ScmFile implements Comparable
{
    public static String SEPARATOR = "/";

    private String path;
    private int prefixLength = 0;

    private boolean directory = false;

    private String type = "text/plain";

    private ScmFile(String path, int prefixLength, boolean dir)
    {
        this.path = path;
        this.prefixLength = prefixLength;
        this.directory = dir;
    }

    public ScmFile(String path)
    {
        this(path, false);
    }

    public ScmFile(String path, boolean dir)
    {
        this.path = normalizePath(path);
        this.directory = dir;
    }

    public ScmFile(String parent, String child)
    {
        this(parent, child, false);
    }

    public ScmFile(String parent, String child, boolean dir)
    {
        this(normalizePath(parent) + SEPARATOR + normalizePath(child), dir);
    }

    public ScmFile(ScmFile parent, String child)
    {
        this(parent, child, false);
    }

    public ScmFile(ScmFile parent, String child, boolean dir)
    {
        this(parent.getPath() + SEPARATOR + normalizePath(child), dir);
    }

    public String getPath()
    {
        return path;
    }

    public boolean isDirectory()
    {
        return directory;
    }

    public boolean isFile()
    {
        return !isDirectory();
    }

    public String getMimeType()
    {
        return type;
    }

    public void setMimeType(String type)
    {
        this.type = type;
    }

    public String getName()
    {
        int lastIndex = path.lastIndexOf(SEPARATOR);
        if (lastIndex != -1)
        {
            return path.substring(lastIndex + 1);
        }
        return this.path;
    }

    public String getParent()
    {
        int index = path.lastIndexOf(SEPARATOR);
        if (index < prefixLength)
        {
            if ((prefixLength > 0) && (path.length() > prefixLength))
            {
                return path.substring(0, prefixLength);
            }
            return null;
        }
        return path.substring(0, index);
    }

    public ScmFile getParentFile()
    {
        String p = this.getParent();
        if (p == null)
        {
            return null;
        }
        return new ScmFile(p, this.prefixLength, true);
    }


    public static String normalizePath(String path)
    {
        path = FileSystemUtils.normaliseSeparators(path);
        if (path.startsWith(SEPARATOR))
        {
            path = path.substring(1);
        }
        if (path.endsWith(SEPARATOR))
        {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    // auto-generated.

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScmFile file = (ScmFile) o;

        if (directory != file.directory) return false;
        if (prefixLength != file.prefixLength) return false;
        if (path != null ? !path.equals(file.path) : file.path != null) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = (path != null ? path.hashCode() : 0);
        result = 31 * result + prefixLength;
        result = 31 * result + (directory ? 1 : 0);
        return result;
    }

    public int compareTo(Object o)
    {
        ScmFile other = (ScmFile) o;
        return getName().compareTo(other.getName());
    }

/*

    private static final String SEPARATOR = "/";
    
    private String name;
    private String path;
    private boolean isDir;
    private ScmFile parent;
    private String type = "text/plain";

    public ScmFile(String name, boolean isDirectory, String path)
    {
        this.name = name;
        this.isDir = isDirectory;

        if (path.endsWith(SEPARATOR))
        {
            path = path.substring(0, path.length() - 1);
        }
        this.path = path;
    }

    public ScmFile(boolean isDirectory, String path)
    {
        this(null, isDirectory, path);

        int index = path.lastIndexOf('/');
        if (index == -1)
        {
            name = path;
        }
        else if (index == path.length())
        {
            name = "";
        }
        else
        {
            name = path.substring(index + 1);
        }
    }

    public ScmFile(String name, boolean isDirectory, ScmFile parent, String path)
    {
        this(name, isDirectory, path);
        this.parent = parent;
    }

    public ScmFile(boolean isDirectory, ScmFile parent, String path)
    {
        this(isDirectory, path);
        this.parent = parent;
    }

    public boolean isDirectory()
    {
        return isDir;
    }

    public boolean isFile()
    {
        return !isDirectory();
    }

    public ScmFile getParentFile()
    {
        if (parent == null)
        {
            if (path.contains(SEPARATOR))
            {
                parent = new ScmFile(true, getParentPath(path));
            }
            else
            {
                if (path.length() > 0)
                {
                    parent = new ScmFile(true, "");
                }
            }
        }

        return parent;
    }

    private String getParentPath(String path)
    {
        int index = path.lastIndexOf('/');
        assert(index >= 0);
        return path.substring(0, index);
    }

    public String getMimeType()
    {
        return type;
    }

    public void setMimeType(String type)
    {
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public String getPath()
    {
        return path;
    }

    public int compareTo(Object o)
    {
        ScmFile other = (ScmFile) o;
        return name.compareTo(other.name);
    }

    public String toString()
    {
        return name + (isDir ? SEPARATOR : "");
    }
*/
}
