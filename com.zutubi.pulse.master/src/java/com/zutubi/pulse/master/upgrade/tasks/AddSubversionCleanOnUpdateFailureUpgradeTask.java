package com.zutubi.pulse.master.upgrade.tasks;

import com.zutubi.tove.type.record.PathUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Adds a new option to SubversionConfiguration to allow clean up on update
 * failure.
 */
public class AddSubversionCleanOnUpdateFailureUpgradeTask extends AbstractRecordPropertiesUpgradeTask
{
    private static final String SCOPE_PROJECTS = "projects";
    private static final String TYPE_SUBVERSION = "zutubi.subversionConfig";
    private static final String PROPERTY_SCM = "scm";
    private static final String PROPERTY_CLEAN_ON_UPDATE_FAILURE = "cleanOnUpdateFailure";

    public boolean haltOnFailure()
    {
        return true;
    }

    protected RecordLocator getRecordLocator()
    {
        return RecordLocators.newTypeFilter(
                RecordLocators.newPathPattern(PathUtils.getPath(SCOPE_PROJECTS, PathUtils.WILDCARD_ANY_ELEMENT, PROPERTY_SCM)),
                TYPE_SUBVERSION
        );
    }

    protected List<? extends RecordUpgrader> getRecordUpgraders()
    {
        // Set to false in existing installs to preserve current behaviour.
        return Arrays.asList(RecordUpgraders.newAddProperty(PROPERTY_CLEAN_ON_UPDATE_FAILURE, Boolean.toString(false)));
    }
}