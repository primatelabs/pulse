package com.zutubi.pulse.prototype.config.user;

import com.zutubi.prototype.MapOption;
import com.zutubi.prototype.MapOptionProvider;
import com.zutubi.prototype.type.TypeProperty;
import com.zutubi.pulse.model.BuildColumns;
import static com.zutubi.util.CollectionUtils.asOrderedMap;
import static com.zutubi.util.CollectionUtils.asPair;

import java.util.Map;

/**
 * Provides available build information columns for the browse view.
 */
public class BrowseViewColumnsOptionProvider extends MapOptionProvider
{
    private static final Map<String, String> COLUMNS = asOrderedMap(asPair(BuildColumns.KEY_VERSION, "build version"),
                                                                    asPair(BuildColumns.KEY_ERRORS, "error count"),
                                                                    asPair(BuildColumns.KEY_REASON, "reason"),
                                                                    asPair(BuildColumns.KEY_REVISION, "revision"),
                                                                    asPair(BuildColumns.KEY_ELAPSED, "running/remaining time"),
                                                                    asPair(BuildColumns.KEY_WHEN, "start time"),
                                                                    asPair(BuildColumns.KEY_TESTS, "test summary"),
                                                                    asPair(BuildColumns.KEY_WARNINGS, "warning count"));

    public MapOption getEmptyOption(Object instance, String parentPath, TypeProperty property)
    {
        return null;
    }

    protected Map<String, String> getMap(Object instance, String parentPath, TypeProperty property)
    {
        return COLUMNS;
    }
}
