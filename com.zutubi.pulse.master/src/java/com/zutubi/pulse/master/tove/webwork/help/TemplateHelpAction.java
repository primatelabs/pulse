package com.zutubi.pulse.master.tove.webwork.help;

import com.zutubi.pulse.master.xwork.actions.ActionSupport;

/**
 * Displays documentation for template operations.
 */
public class TemplateHelpAction extends ActionSupport
{
    public String execute() throws Exception
    {
        // Currently we show generic help.
        return SUCCESS;
    }
}
