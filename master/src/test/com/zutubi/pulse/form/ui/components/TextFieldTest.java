package com.zutubi.pulse.form.ui.components;

/**
 * <class-comment/>
 */
public class TextFieldTest extends ComponentTestCase
{
    public void testComponentRendering() throws Exception
    {
        TextField field = new TextField();
        renderer.render(field);

        String content = writer.toString();
    }
}
