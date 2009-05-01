package com.zutubi.pulse.acceptance.forms;

import com.thoughtworks.selenium.Selenium;
import com.zutubi.pulse.acceptance.SeleniumUtils;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Mapping;
import com.zutubi.util.Pair;
import junit.framework.Assert;

import java.util.List;

/**
 * Base for form classes: supports methods for reading and writing fields and
 * submitting forms.
 */
public abstract class SeleniumForm
{
    public static final int TEXTFIELD      = 3;
    public static final int CHECKBOX       = 4;
    public static final int COMBOBOX       = 6;
    public static final int MULTI_CHECKBOX = 7;
    public static final int MULTI_SELECT   = 8;
    public static final int ITEM_PICKER    = 9;

    protected Selenium selenium;
    protected boolean inherited = false;
    protected boolean ajax = true;

    protected SeleniumForm(Selenium selenium)
    {
        this.selenium = selenium;
    }

    protected SeleniumForm(Selenium selenium, boolean ajax)
    {
        this.selenium = selenium;
        this.ajax = ajax;
    }

    public SeleniumForm(Selenium selenium, boolean ajax, boolean inherited)
    {
        this.selenium = selenium;
        this.ajax = ajax;
        this.inherited = inherited;
    }

    public Selenium getSelenium()
    {
        return selenium;
    }

    public boolean isAjax()
    {
        return ajax;
    }

    public void waitFor()
    {
        // Wait for the last field as the forms are lazily rendered
        String[] fields = getActualFieldNames();
        SeleniumUtils.waitForElementId(selenium, getFieldId(fields[fields.length - 1]));
    }

    public boolean isFormPresent()
    {
        return selenium.isElementPresent(getFormName());
    }

    public String getFieldId(String name)
    {
        return "zfid." + name;
    }

    public boolean isAnnotationPresent(String fieldName, String annotation)
    {
        return selenium.isElementPresent(getFieldId(fieldName) + "." + annotation);
    }

    public boolean isEditable(String fieldName)
    {
        return selenium.isEditable(getFieldId(fieldName));
    }

    public boolean isMarkedRequired(String fieldName)
    {
        return isAnnotationPresent(fieldName, "required");
    }

    /**
     * Returns the type identifiers for the form fields. The default implementation
     * returns an array of TEXTFIELD identifiers.
     *
     * @return an array of form field identifiers.
     */
    public int[] getFieldTypes()
    {
        int[] types = new int[getFieldNames().length];
        for (int i = 0; i < types.length; i++)
        {
            types[i] = TEXTFIELD;
        }
        return types;
    }

    public String[] getSelectOptions(String name)
    {
        return selenium.getSelectOptions(getFieldId(name));
    }

    public String[] getComboBoxOptions(String name)
    {
        String js = "var result = function() { " +
                        "var combo = selenium.browserbot.getCurrentWindow().Ext.getCmp('zfid." + name + "'); " +
                        "var values = []; " +
                        "combo.store.each(function(r) { values.push(r.get(combo.valueField)); }); " +
                        "return values; " +
                    "}(); " +
                    "result";
        return selenium.getEval(js).split(",");
    }

    public void submitNamedFormElements(String submitValue, Pair<String, String>... fieldValues)
    {
        if (fieldValues != null)
        {
            for (Pair<String, String> fv: fieldValues)
            {
                setFieldValue(fv.first, fv.second);
            }
        }

        submit(submitValue);
    }

    public void submitFormElements(String submitValue, String... fieldValues)
    {
        setFormElements(fieldValues);
        submit(submitValue);
    }

    public void nextNamedFormElements(Pair<String, String>... fieldValues)
    {
        submitNamedFormElements("next", fieldValues);
    }

    public void nextFormElements(String... args)
    {
        submitFormElements("next", args);
    }

    public void applyNamedFormElements(Pair<String, String>... fieldValues)
    {
        submitNamedFormElements("apply", fieldValues);
    }

    public void applyFormElements(String... args)
    {
        submitFormElements("apply", args);
    }

    public void saveNamedFormElements(Pair<String, String>... fieldValues)
    {
        submitNamedFormElements("save", fieldValues);
    }

    public void saveFormElements(String... args)
    {
        submitFormElements("save", args);
    }

    public void finishNamedFormElements(Pair<String, String>... fieldValues)
    {
        submitNamedFormElements("finish", fieldValues);
    }

    public void finishFormElements(String... args)
    {
        submitFormElements("finish", args);
    }

    public void cancelNamedFormElements(Pair<String, String>... fieldValues)
    {
        submitNamedFormElements("cancel", fieldValues);
    }

    public void cancelFormElements(String... args)
    {
        submitFormElements("cancel", args);
    }

    public void resetFormElements(String... args)
    {
        submitFormElements("reset", args);
    }

    private void submit(String id)
    {
        selenium.click("zfid." + id);
        if (ajax)
        {
            SeleniumUtils.waitForVariable(selenium, "formSubmitting", SeleniumUtils.DEFAULT_TIMEOUT, true);
        }
        else
        {
            selenium.waitForPageToLoad("60000");
        }
    }

    /**
     * Returns an array of values of the same size as the form which is null-
     * filled.  When used to set or submit the form, no values in the form will
     * be changed.
     *
     * @return a value array that will leave the form unchanged
     */
    public String[] getUnchangedValues()
    {
        return new String[getFieldNames().length];
    }

    public String getFieldValue(String name)
    {
        int type = getFieldType(name);
        switch(type)
        {
            case ITEM_PICKER:
            case MULTI_SELECT:
                return selenium.getEval("selenium.browserbot.getCurrentWindow().Ext.getCmp('" + getFieldId(name) + "').getValue();");
            default:
                return selenium.getValue(getFieldId(name));
        }
    }

    public void setFieldValue(String name, String value)
    {
        int type = getFieldType(name);
        setFormElement(name, value, type);
    }

    private void setFormElement(String name, String value, int type)
    {
        if (value != null)
        {
            String id = getFieldId(name);
            switch (type)
            {
                case TEXTFIELD:
                    selenium.type(id, value);
                    break;
                case COMBOBOX:
                    setComponentValue(id, "'" + value + "'");
                    break;
                case ITEM_PICKER:
                case MULTI_SELECT:
                    String[] values = convertMultiValue(value);
                    List<String> quotedValues = CollectionUtils.map(values, new Mapping<String, String>()
                    {
                        public String map(String s)
                        {
                            return "'" + s + "'";
                        }
                    });
                    setComponentValue(id, quotedValues.toString());
                    break;
                case CHECKBOX:
                    setComponentValue(id, value);
                    break;
                case MULTI_CHECKBOX:
                    setMultiValues(name, value);
                    break;
                default:
                    break;
            }
        }
    }

    private void setComponentValue(String id, String value)
    {
        // Custom Ext widgets are tricky to manage.  Since we are
        // not testing the widgets themselves, just go direct to
        // the setValue method.
        selenium.getEval("var field = selenium.browserbot.getCurrentWindow().Ext.getCmp('" + id + "'); field.setValue(" + value + "); field.form.updateButtons()");
    }

    private int getFieldType(String name)
    {
        int i;
        String[] names = getActualFieldNames();
        for(i = 0; i < names.length; i++)
        {
            if(names[i].equals(name))
            {
                break;
            }
        }

        return getActualFieldTypes()[i];
    }

    public void setFormElements(String... values)
    {
        int[] types = getActualFieldTypes();
        Assert.assertEquals(values.length, types.length);

        String[] names = getActualFieldNames();
        for (int i = 0; i < types.length; i++)
        {
            String name = names[i];
            String value = values[i];
            setFormElement(name, value, types[i]);
        }

        // Hack: make sure buttons are updated.  Typing something empty in
        // selenium doesn't work :|.
        if (names.length > 0)
        {
            selenium.getEval("var field = selenium.browserbot.getCurrentWindow().Ext.getCmp('" + getFieldId(names[0]) + "'); field.form.updateButtons()");
        }
    }

    public String[] getFormValues()
    {
        String[] fieldNames = getActualFieldNames();
        String[] formValues = new String[fieldNames.length];
        CollectionUtils.mapToArray(fieldNames, new Mapping<String, String>()
        {
            public String map(String fieldName)
            {
                return getFieldValue(fieldName);
            }
        }, formValues);
        return formValues;
    }

    public void setMultiValues(String name, String values)
    {
        String[] set = convertMultiValue(values);

        String fieldLocator = getFieldId(name);
        for(String value: set)
        {
            selenium.addSelection(fieldLocator, "value=" + value);
        }
    }

    public String[] convertMultiValue(String values)
    {
        String[] set;
        if (values.length() > 0)
        {
            set = values.split(",");
        }
        else
        {
            set = new String[0];
        }
        return set;
    }

    public String[] getActualFieldNames()
    {
        String[] fieldNames = getFieldNames();
        if(inherited)
        {
            String[] temp = fieldNames;
            fieldNames = new String[fieldNames.length - 1];
            System.arraycopy(temp, 1, fieldNames, 0, temp.length - 1);
        }
        return fieldNames;
    }

    public int[] getActualFieldTypes()
    {
        int[] fieldTypes = getFieldTypes();
        if(inherited)
        {
            int[] temp = fieldTypes;
            fieldTypes = new int[fieldTypes.length - 1];
            System.arraycopy(temp, 1, fieldTypes, 0, temp.length - 1);
        }
        return fieldTypes;
    }

    public abstract String getFormName();
    public abstract String[] getFieldNames();
}
