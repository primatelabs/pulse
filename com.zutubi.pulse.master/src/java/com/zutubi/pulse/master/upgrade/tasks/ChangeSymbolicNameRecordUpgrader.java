package com.zutubi.pulse.master.upgrade.tasks;

import com.zutubi.tove.type.record.MutableRecord;

/**
 * A record upgrader that changes the symbolic name of the record.
 */
class ChangeSymbolicNameRecordUpgrader implements RecordUpgrader
{
    private String oldSymbolicName;
    private String newSymbolicName;

    /**
     * Creates a new upgrader that will change all occurrences of the given old
     * name to the given new name.
     *
     * @param oldSymbolicName the symbolic name to rename
     * @param newSymbolicName the new symbolic name
     */
    public ChangeSymbolicNameRecordUpgrader(String oldSymbolicName, String newSymbolicName)
    {
        this.oldSymbolicName = oldSymbolicName;
        this.newSymbolicName = newSymbolicName;
    }

    public void upgrade(String path, MutableRecord record)
    {
        if (record.getSymbolicName().equals(oldSymbolicName))
        {
            record.setSymbolicName(newSymbolicName);
        }
    }
}
