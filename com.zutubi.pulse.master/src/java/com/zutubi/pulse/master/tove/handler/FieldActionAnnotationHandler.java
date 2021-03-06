package com.zutubi.pulse.master.tove.handler;

import com.zutubi.pulse.master.tove.model.Descriptor;
import com.zutubi.pulse.master.tove.model.FieldDescriptor;
import com.zutubi.tove.annotations.FieldAction;
import com.zutubi.tove.type.CompositeType;
import com.zutubi.util.ClassLoaderUtils;
import com.zutubi.util.StringUtils;
import com.zutubi.util.bean.ObjectFactory;
import com.zutubi.util.logging.Logger;

import java.lang.annotation.Annotation;

/**
 * Handler for the {@link com.zutubi.tove.annotations.FieldAction} annotation.
 */
public class FieldActionAnnotationHandler implements AnnotationHandler
{
    private static final Logger LOG = Logger.getLogger(FieldActionAnnotationHandler.class);

    private ObjectFactory objectFactory;

    public void process(CompositeType annotatedType, Annotation annotation, Descriptor descriptor) throws Exception
    {
        FieldDescriptor fieldDescriptor = (FieldDescriptor) descriptor;
        FieldAction fieldAction = (FieldAction) annotation;
        if (StringUtils.stringSet(fieldAction.filterClass()))
        {
            Class<Object> filterClass = ClassLoaderUtils.loadAssociatedClass(annotatedType.getClazz(), fieldAction.filterClass());
            if(!satisfied(filterClass, fieldDescriptor, fieldAction))
            {
                return;
            }
        }

        fieldDescriptor.addAction(fieldAction.actionKey());
        if (StringUtils.stringSet(fieldAction.template()))
        {
            fieldDescriptor.addScript(fieldAction.template());
        }
    }

    private boolean satisfied(Class<Object> filterClass, FieldDescriptor fieldDescriptor, FieldAction fieldAction)
    {
        if(!FieldActionPredicate.class.isAssignableFrom(filterClass))
        {
            LOG.warning("Field action filter class '" + filterClass.getName() + "' does not implement FieldActionPredicate: ignoring");
            return true;
        }

        FieldActionPredicate predicate = (FieldActionPredicate) objectFactory.buildBean(filterClass);
        return predicate.satisfied(fieldDescriptor, fieldAction);
    }

    public void setObjectFactory(ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }
}
