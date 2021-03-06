package com.zutubi.pulse.acceptance.forms;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.zutubi.pulse.acceptance.SeleniumBrowser;
import com.zutubi.tove.annotations.Field;
import com.zutubi.tove.annotations.FieldType;
import com.zutubi.tove.annotations.Form;
import com.zutubi.tove.annotations.Wizard;
import com.zutubi.tove.config.api.Configuration;
import com.zutubi.util.logging.Logger;
import com.zutubi.util.reflection.AnnotationUtils;
import com.zutubi.util.reflection.ReflectionUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.find;
import static java.util.Arrays.asList;

/**
 * Base for forms that are based off a configuration class, where
 * the class is available (i.e. not a plugin).  The form and field names are
 * automatically determined from the class.
 */
public class ConfigurationForm extends SeleniumForm
{
    public static final String ANNOTATION_INHERITED = "inherited";

    private static final Logger LOG = Logger.getLogger(ConfigurationForm.class);

    private Class<? extends Configuration> configurationClass;
    private List<FieldInfo> fields = new LinkedList<FieldInfo>();

    public ConfigurationForm(SeleniumBrowser browser, Class<? extends Configuration> configurationClass)
    {
        this(browser, configurationClass, true);
    }

    public ConfigurationForm(SeleniumBrowser browser, Class<? extends Configuration> configurationClass, boolean ajax)
    {
        this(browser, configurationClass, ajax, false);
    }

    public ConfigurationForm(SeleniumBrowser browser, Class<? extends Configuration> configurationClass, boolean ajax, boolean inherited)
    {
        this(browser, configurationClass, ajax, inherited, false);
    }

    public ConfigurationForm(SeleniumBrowser browser, Class<? extends Configuration> configurationClass, boolean ajax, boolean inherited, boolean wizard)
    {
        super(browser, ajax, inherited);
        this.configurationClass = configurationClass;
        analyzeFields(wizard);
    }

    public String getFormName()
    {
        return configurationClass.getName();
    }

    public String[] getFieldNames()
    {
        return transform(fields, new Function<FieldInfo, String>()
        {
            public String apply(FieldInfo fieldInfo)
            {
                return fieldInfo.name;
            }
        }).toArray(new String[fields.size()]);
    }

    public int[] getFieldTypes()
    {
        int[] types = new int[fields.size()];
        int i = 0;
        for (FieldInfo fieldInfo: fields)
        {
            types[i++] = fieldInfo.type;
        }

        return types;
    }

    private void analyzeFields(boolean ignoreWizard)
    {
        try
        {
            Form formAnnotation = null;
            for (Class<?> clazz: ReflectionUtils.getSuperclasses(configurationClass, Configuration.class, false))
            {
                formAnnotation = clazz.getAnnotation(Form.class);
                if (formAnnotation != null)
                {
                    break;
                }
            }
            
            final String[] fieldNames = formAnnotation == null ? getFieldNames() : formAnnotation.fieldOrder();
            BeanInfo beanInfo = Introspector.getBeanInfo(configurationClass);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (final String fieldName : fieldNames)
            {
                PropertyDescriptor property = find(asList(propertyDescriptors), new Predicate<PropertyDescriptor>()
                {
                    public boolean apply(PropertyDescriptor propertyDescriptor)
                    {
                        return propertyDescriptor.getName().equals(fieldName);
                    }
                }, null);

                int fieldType = TEXTFIELD;
                if (property != null)
                {
                    List<Annotation> annotations = AnnotationUtils.annotationsFromProperty(property, true);
                    if (ignoreWizard && any(annotations, instanceOf(Wizard.Ignore.class)))
                    {
                        continue;
                    }

                    fieldType = determineFieldType(property, annotations);
                }

                fields.add(new FieldInfo(fieldName, fieldType));
            }
        }
        catch (IntrospectionException e)
        {
            LOG.severe(e);
            throw new RuntimeException(e);
        }
    }

    private int determineFieldType(PropertyDescriptor property, List<Annotation> annotations)
    {
        int fieldType = TEXTFIELD;

        Field field = (Field) find(annotations, new Predicate<Annotation>()
        {
            public boolean apply(Annotation annotation)
            {
                return annotation instanceof Field;
            }
        }, null);

        Class<?> returnType = property.getReadMethod().getReturnType();
        boolean collection = List.class.isAssignableFrom(returnType) || Map.class.isAssignableFrom(returnType);
        if (field == null)
        {
            if (returnType == Boolean.class || returnType == Boolean.TYPE)
            {
                fieldType = CHECKBOX;
            }
            else if (returnType.isEnum())
            {
                fieldType = COMBOBOX;
            }
            else if (List.class.isAssignableFrom(returnType))
            {
                fieldType = MULTI_SELECT;
            }
        }
        else
        {
            fieldType = convertFieldType(field.type(), collection);
        }

        return fieldType;
    }

    private int convertFieldType(String name, boolean collection)
    {
        if(name.equals(FieldType.CHECKBOX) || name.equals(FieldType.CONTROLLING_CHECKBOX))
        {
            return CHECKBOX;
        }
        else if(name.equals(FieldType.ITEM_PICKER))
        {
            return ITEM_PICKER;
        }
        else if(name.equals(FieldType.SELECT) || name.equals(FieldType.CONTROLLING_SELECT))
        {
            return collection ? COMBOBOX : MULTI_SELECT;
        }

        return TEXTFIELD;
    }

    /**
     * Indicates if a field is marked as inherited.
     *
     * @param fieldName name of the field to check
     * @return true iff the given field is marked inherited
     */
    public boolean isInherited(String fieldName)
    {
        return isAnnotationPresent(fieldName, ANNOTATION_INHERITED);
    }

    private static class FieldInfo
    {
        String name;
        int type;

        private FieldInfo(String name, int type)
        {
            this.name = name;
            this.type = type;
        }
    }
}
