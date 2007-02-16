package com.zutubi.prototype;

import com.zutubi.prototype.model.Field;
import ognl.Ognl;
import ognl.OgnlException;

import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 */
public class FieldDescriptor implements Descriptor
{
    private String name;

    private Map<String, Object> parameters = new HashMap<String, Object>();

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void addParameter(String key, Object value)
    {
        parameters.put(key, value);
    }

    public Object getParameter(String key)
    {
        return parameters.get(key);
    }

    public Map<String, Object> getParameters()
    {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters)
    {
        this.parameters = parameters;
    }

    public Field instantiate(Object instance)
    {
        Field field = new Field();
        field.getParameters().put("name", getName());
        field.getParameters().put("id", getName());
        field.getParameters().put("label", getName() + ".label");
        field.getParameters().putAll(getParameters());

        try
        {
            if (!field.getParameters().containsKey("value"))
            {
                if (instance != null)
                {
                    Map context = Ognl.createDefaultContext(instance);
                    field.getParameters().put("value", Ognl.getValue(getName(), context, instance));

                    if (getParameter("type").equals("select"))
                    {
                        field.getParameters().put("list", Ognl.getValue(getName() + "Options", context, instance));
                    }
                }
            }
        }
        catch (OgnlException e)
        {
            field.getParameters().put("value", e.getMessage());
        }

        return field;
    }
}
