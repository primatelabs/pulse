package com.zutubi.pulse.master.vfs.provider.pulse.reference;

import com.google.common.base.Function;
import com.zutubi.pulse.core.marshal.doc.ChildNodeDocs;
import com.zutubi.pulse.core.marshal.doc.ElementDocs;
import com.zutubi.pulse.core.marshal.doc.ExtensibleDocs;
import com.zutubi.pulse.core.marshal.doc.NodeDocs;
import com.zutubi.pulse.master.vfs.provider.pulse.AbstractPulseFileObject;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.provider.AbstractFileSystem;

import java.util.Collection;

import static com.google.common.collect.Collections2.transform;

/**
 * A file object representing an {@link com.zutubi.pulse.core.marshal.doc.ElementDocs}
 * node in the tove file doc tree.
 */
public class ElementFileObject extends AbstractReferenceFileObject
{
    private ElementDocs elementDocs;

    public ElementFileObject(final FileName name, final AbstractFileSystem fs, ElementDocs elementDocs)
    {
        super(name, fs);
        this.elementDocs = elementDocs;
    }

    protected String[] getDynamicChildren()
    {
        Collection<ChildNodeDocs> children = elementDocs.getChildren();
        return transform(children, new Function<ChildNodeDocs, String>()
        {
            public String apply(ChildNodeDocs childNodeDocs)
            {
                return childNodeDocs.getName();
            }
        }).toArray(new String[children.size()]);
    }

    public AbstractPulseFileObject createDynamicFile(FileName fileName)
    {
        NodeDocs nodeDocs = elementDocs.getNode(fileName.getBaseName());
        if (nodeDocs == null)
        {
            return null;
        }

        if (nodeDocs instanceof ElementDocs)
        {
            return objectFactory.buildBean(ElementFileObject.class, fileName, getFileSystem(), nodeDocs);
        }
        else if (nodeDocs instanceof ExtensibleDocs)
        {
            return objectFactory.buildBean(ExtensibleFileObject.class, fileName, getFileSystem(), nodeDocs);
        }
        else
        {
            return objectFactory.buildBean(BuiltinElementFileObject.class, fileName, getFileSystem(), nodeDocs);
        }
    }

    @Override
    public String getIconCls()
    {
        return "reference-element-icon";
    }

    public ElementDocs getElementDocs()
    {
        return elementDocs;
    }
}
