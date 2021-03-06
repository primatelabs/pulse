package com.zutubi.pulse.master.tove.webwork;

import com.opensymphony.util.TextUtils;
import com.zutubi.i18n.Messages;
import com.zutubi.pulse.master.tove.format.StateDisplayManager;
import com.zutubi.tove.config.api.Configuration;
import com.zutubi.tove.type.CompositeType;
import com.zutubi.util.WebUtils;
import com.zutubi.util.adt.TreeNode;

import java.util.Collection;

/**
 * A layer on top of the {@link com.zutubi.pulse.master.tove.format.StateDisplayManager}
 * which returns state as rendered fragments of HTML for the web interface.
 */
public class StateDisplayRenderer
{
    private static final Messages I18N = Messages.getInstance(StateDisplayRenderer.class);
    
    private static final int COLLECTION_LIMIT = 3;

    private StateDisplayManager stateDisplayManager;

    /**
     * Calls {@link com.zutubi.pulse.master.tove.format.StateDisplayManager#format(String, com.zutubi.tove.config.api.Configuration)}
     * to format the specified field of the given instance, then renders the
     * returned result as an HTML fragment.
     *
     * @param fieldName name of the state field to render
     * @param instance  instance to render the field for
     * @return an HTML fragment suitable for inclusion in a state table value
     *         cell
     */
    public String render(String fieldName, Configuration instance)
    {
        return renderFormatted(fieldName, stateDisplayManager.format(fieldName, instance));
    }

    /**
     * Calls {@link com.zutubi.pulse.master.tove.format.StateDisplayManager#formatCollection(String, com.zutubi.tove.type.CompositeType, java.util.Collection, com.zutubi.tove.config.api.Configuration)}
     * to format the specified field of the given collection, then renders the
     * returned result as an HTML fragment.
     *
     * @param fieldName      name of the state field to render
     * @param type           target type of the collection
     * @param collection     collection to render the field for
     * @param parentInstance instance that owns the collection
     * @return an HTML fragment suitable for inclusion in a state table value
     *         cell
     */
    public String renderCollection(String fieldName, CompositeType type, Collection<? extends Configuration> collection, Configuration parentInstance)
    {
        return renderFormatted(fieldName, stateDisplayManager.formatCollection(fieldName, type, collection, parentInstance));
    }

    private String renderFormatted(String fieldName, Object formatted)
    {
        if (formatted instanceof TreeNode)
        {
            return renderTree(fieldName, (TreeNode) formatted);
        }
        else if (formatted instanceof Collection)
        {
            return renderCollection(fieldName, (Collection) formatted);
        }
        else
        {
            return TextUtils.htmlEncode(formatted.toString());
        }
    }

    private String renderTree(String fieldName, TreeNode treeNode)
    {
        StringBuilder result = new StringBuilder();
        // Exclude the root from the count - it is not rendered.
        boolean hasExcess = treeNode.size() - 1 - COLLECTION_LIMIT > 1;
        renderChildren(fieldName, treeNode, true, hasExcess, new int[]{0}, result);
        return result.toString();
    }

    private void renderChildren(String fieldName, TreeNode<?> treeNode, boolean isRoot, boolean hasExcess, int[] count, StringBuilder result)
    {
        if (!treeNode.isLeaf())
        {
            startList(result, isRoot && hasExcess);
            for (TreeNode<?> child: treeNode)
            {
                count[0]++;
                startItem(result, hasExcess, count[0]);
                renderSimple(result, child.getData());
                renderChildren(fieldName, child, false, hasExcess, count, result);
                endItem(result);
            }
            endList(fieldName, result, treeNode.size() - 1, isRoot && hasExcess);
        }
    }

    private String renderCollection(String fieldName, Collection collection)
    {
        StringBuilder result = new StringBuilder();
        boolean hasExcess = collection.size() - COLLECTION_LIMIT > 1;

        startList(result, hasExcess);

        int count = 0;
        for (Object o: collection)
        {
            count++;
            startItem(result, hasExcess, count);
            renderSimple(result, o);
            endItem(result);
        }

        endList(fieldName, result, collection.size(), hasExcess);
        return result.toString();
    }

    private void renderSimple(StringBuilder result, Object value)
    {
        result.append(TextUtils.htmlEncode(value.toString()));
    }

    private void startList(StringBuilder result, boolean enableToggle)
    {
        result.append("<ul");
        if (enableToggle)
        {
            result.append(" class='top-level' onclick='toggleStateList(event);'");
        }
        result.append(">");
    }

    private void endList(String fieldName, StringBuilder result, int size, boolean showToggleItems)
    {
        if (showToggleItems)
        {
            result.append("<li class='details expansion' id='state.");
            result.append(WebUtils.toValidHtmlName(fieldName));
            result.append(".expand'/>");
            result.append(I18N.format("collection.more", size - COLLECTION_LIMIT));
            result.append("</li>");
            result.append("<li class='details excess' id='state.");
            result.append(WebUtils.toValidHtmlName(fieldName));
            result.append(".collapse'/>");
            result.append(I18N.format("collection.all", size));
            result.append("</li>");
        }

        result.append("</ul>");
    }

    private void startItem(StringBuilder result, boolean hasExcess, int count)
    {
        result.append("<li");
        if (hasExcess && count > COLLECTION_LIMIT)
        {
            result.append(" class='excess'");
        }
        result.append(">");
    }

    private void endItem(StringBuilder result)
    {
        result.append("</li>");
    }

    public void setStateDisplayManager(StateDisplayManager stateDisplayManager)
    {
        this.stateDisplayManager = stateDisplayManager;
    }
}
