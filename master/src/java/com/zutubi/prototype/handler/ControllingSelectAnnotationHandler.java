package com.zutubi.prototype.handler;

import com.zutubi.config.annotations.Select;
import com.zutubi.config.annotations.ControllingSelect;

import java.lang.annotation.Annotation;

/**
 *
 */
public class ControllingSelectAnnotationHandler extends OptionAnnotationHandler
{
    protected String getOptionProviderClass(Annotation annotation)
    {
        return ((ControllingSelect)annotation).optionProvider();
    }
}
