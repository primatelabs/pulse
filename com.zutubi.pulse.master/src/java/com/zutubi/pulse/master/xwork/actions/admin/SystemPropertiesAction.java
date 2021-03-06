package com.zutubi.pulse.master.xwork.actions.admin;

import com.opensymphony.xwork.ActionContext;
import com.zutubi.pulse.master.xwork.actions.ActionSupport;

import java.util.Map;
import java.util.Properties;

/**
 * A simple helper action that helps administrators to tweak the configuration of a running pulse
 * installation by allowing them to change the system properties.
 *
 * 
 */
public class SystemPropertiesAction extends ActionSupport
{
    public Properties getSystemProperties()
    {
        return System.getProperties();
    }

    public String doAdd()
    {
        // apply the parameters to the system properties.
        Map<String, String[]> params = ActionContext.getContext().getParameters();

        for (String key : params.keySet())
        {
            String[] values = params.get(key);
            if (values.length > 0)
            {
                System.setProperty(key, values[0]);
            }
        }

        return SUCCESS;
    }

    public String doRemove()
    {
        // apply the parameters to the system properties.
        Map<String, String[]> params = ActionContext.getContext().getParameters();

        for (String key : params.keySet())
        {
            System.clearProperty(key);
        }
        
        return SUCCESS;
    }
}
