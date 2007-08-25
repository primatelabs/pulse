package com.zutubi.pulse.core.model;

/**
 * A trivial implementation of the Change interface.
 *
 * @author jsankey
 */
public class Change extends Entity
{
    /**
     * Types of actions that can be performed on an SCM file.
     */
    public enum Action
    {
        /**
         * The file was added to source control.
         */
        ADD
                {
                    public String getPrettyString()
                    {
                        return "added";
                    }
                },
        /**
         * The file was branched somewhere.
         */
        BRANCH
                {
                    public String getPrettyString()
                    {
                        return "branched";
                    }
                },
        /**
         * The file was deleted from source control.
         */
        DELETE
                {
                    public String getPrettyString()
                    {
                        return "deleted";
                    }
                },
        /**
         * The file contents were edited.
         */
        EDIT
                {
                public String getPrettyString()
                    {
                        return "edited";
                    }
                },
        /**
         * Perforce-specific: the file was integrated.
         */
        INTEGRATE
                {
                public String getPrettyString()
                    {
                        return "integrated";
                    }
                },
        /**
         * The file was merged with another.
         */
        MERGE
                {
                    public String getPrettyString()
                    {
                        return "merged";
                    }
                }
        ,
        /**
         * The file was moved.
         */
        MOVE
                {
                    public String getPrettyString()
                    {
                        return "moved";
                    }
                }
        ,
        /**
         * An unrecognised action.
         */
        UNKNOWN
                {
                    public String getPrettyString()
                    {
                        return "unknown";
                    }
                };

        public abstract String getPrettyString();
    }

    private String filename;
    private Action action;
    private boolean directory;
    private String revisionString;

    protected Change()
    {

    }

    public Change(String filename, String revisionString, Action action)
    {
        this(filename, revisionString, action, false);
    }


    public Change(String filename, String revisionString, Action action, boolean directory)
    {
        this.filename = filename;
        this.action = action;
        this.directory = directory;
        this.revisionString = revisionString;
    }

    /**
     * @return the name of the file that was changed as a repository path
     */
    public String getFilename()
    {
        return filename;
    }

    /**
     * @return the action performed on the file
     */
    public Action getAction()
    {
        return action;
    }

    public String getRevisionString()
    {
        return revisionString;
    }

    public boolean isDirectory()
    {
        return directory;
    }

    private void setDirectory(boolean directory)
    {
        this.directory = directory;
    }

    /**
     * Used by hibernate
     *
     * @param filename string
     */
    private void setFilename(String filename)
    {
        this.filename = filename;
    }

    /**
     * Used by hibernate
     *
     * @return action name
     */
    private String getActionName()
    {
        return action.toString();
    }

    /**
     * Used by hibernate.
     *
     * @param action string
     */
    private void setActionName(String action)
    {
        this.action = Action.valueOf(action);
    }

    /**
     * Used by hibernate.
     *
     * @param revisionString string
     */
    private void setRevisionString(String revisionString)
    {
        this.revisionString = revisionString;
    }


    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Change change = (Change) o;

        if (directory != change.directory) return false;
        if (action != change.action) return false;
        if (filename != null ? !filename.equals(change.filename) : change.filename != null) return false;
        if (revisionString != null ? !revisionString.equals(change.revisionString) : change.revisionString != null)
            return false;

        return true;
    }

    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + (filename != null ? filename.hashCode() : 0);
        result = 31 * result + (action != null ? action.hashCode() : 0);
        result = 31 * result + (directory ? 1 : 0);
        result = 31 * result + (revisionString != null ? revisionString.hashCode() : 0);
        return result;
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append(filename);
        if (revisionString != null)
        {
            buffer.append("#").append(revisionString);
        }
        if (action != null)
        {
            buffer.append("-").append(action.toString());
        }
        return buffer.toString();
    }
}
