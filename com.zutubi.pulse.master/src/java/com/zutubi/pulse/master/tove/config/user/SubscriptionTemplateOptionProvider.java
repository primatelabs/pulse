package com.zutubi.pulse.master.tove.config.user;

import com.zutubi.pulse.master.notifications.renderer.BuildResultRenderer;
import com.zutubi.pulse.master.notifications.renderer.TemplateInfo;
import com.zutubi.pulse.master.tove.handler.MapOption;
import com.zutubi.pulse.master.tove.handler.MapOptionProvider;
import com.zutubi.tove.type.TypeProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides the list of available templates for build subscriptions.
 */
public class SubscriptionTemplateOptionProvider extends MapOptionProvider
{
    private BuildResultRenderer buildResultRenderer;

    public MapOption getEmptyOption(Object instance, String parentPath, TypeProperty property)
    {
        return new MapOption("", "");
    }

    protected Map<String, String> getMap(Object instance, String parentPath, TypeProperty property)
    {
        Map<String, String> options = new HashMap<String, String>();
        for (TemplateInfo template : buildResultRenderer.getAvailableTemplates(false))
        {
            options.put(template.getTemplate(), template.getDisplay());
        }
        return options;
    }

    public void setBuildResultRenderer(BuildResultRenderer buildResultRenderer)
    {
        this.buildResultRenderer = buildResultRenderer;
    }

}
