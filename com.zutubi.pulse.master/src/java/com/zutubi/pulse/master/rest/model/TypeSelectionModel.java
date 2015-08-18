package com.zutubi.pulse.master.rest.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.zutubi.util.adt.Pair;

import java.util.List;

/**
 * Models an unconfigured property that has an undetermined type -- i.e. property type itself is
 * abstract with multiple possible extensions.
 */
@JsonTypeName("type-selection")
public class TypeSelectionModel extends ConfigModel
{
    private List<Pair<Integer, String>> configuredDescendants;

    public TypeSelectionModel(String key, String label)
    {
        super(null, key, label);
    }

    public List<Pair<Integer, String>> getConfiguredDescendants()
    {
        return configuredDescendants;
    }

    public void setConfiguredDescendants(List<Pair<Integer, String>> configuredDescendants)
    {
        this.configuredDescendants = configuredDescendants;
    }
}
