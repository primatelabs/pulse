package com.zutubi.tove.config.health;

import com.zutubi.tove.type.record.MutableRecord;
import com.zutubi.tove.type.record.Record;
import com.zutubi.tove.type.record.RecordManager;

/**
 * Identifies an unexpected simple value in a record.
 */
public class UnexpectedSimpleValueProblem extends HealthProblemSupport
{
    private String key;

    /**
     * Creates a new problem for the given unexpected key at the given path.
     * 
     * @param path    path of the record the key is in
     * @param message message describing this problem
     * @param key     unexpected key found
     */
    protected UnexpectedSimpleValueProblem(String path, String message, String key)
    {
        super(path, message);
        this.key = key;
    }

    public void solve(RecordManager recordManager)
    {
        // If the value is still there, just remove it and update.
        Record record = recordManager.select(getPath());
        if (record != null && record.containsKey(key))
        {
            MutableRecord mutable = record.copy(false, true);
            mutable.remove(key);
            recordManager.update(getPath(), mutable);
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        if (!super.equals(o))
        {
            return false;
        }

        UnexpectedSimpleValueProblem that = (UnexpectedSimpleValueProblem) o;

        if (key != null ? !key.equals(that.key) : that.key != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + (key != null ? key.hashCode() : 0);
        return result;
    }
}
