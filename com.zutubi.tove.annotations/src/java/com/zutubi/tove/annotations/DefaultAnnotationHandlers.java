package com.zutubi.tove.annotations;

/**
 *
 *
 */
public interface DefaultAnnotationHandlers
{
    public static final String HANDLER_PACKAGE    = "com.zutubi.pulse.master.tove.handler";

    public static final String CONTROLLING_SELECT = HANDLER_PACKAGE + ".ControllingSelectAnnotationHandler";
    public static final String FIELD_ACTION       = HANDLER_PACKAGE + ".FieldActionAnnotationHandler";
    public static final String FIELD_PARAMETER    = HANDLER_PACKAGE + ".FieldParameterAnnotationHandler";
    public static final String FIELD_SCRIPT       = HANDLER_PACKAGE + ".FieldScriptAnnotationHandler";
    public static final String FIELD              = HANDLER_PACKAGE + ".FieldAnnotationHandler";
    public static final String FORM               = HANDLER_PACKAGE + ".FormAnnotationHandler";
    public static final String ITEM_PICKER        = HANDLER_PACKAGE + ".ItemPickerAnnotationHandler";
    public static final String REFERENCE          = HANDLER_PACKAGE + ".ReferenceAnnotationHandler";
    public static final String SELECT             = HANDLER_PACKAGE + ".SelectAnnotationHandler";
}
