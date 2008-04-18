package com.zutubi.prototype.webwork;

import com.opensymphony.xwork.ActionContext;
import com.zutubi.config.annotations.FieldType;
import com.zutubi.prototype.config.ConfigurationRefactoringManager;
import com.zutubi.prototype.config.TemplateNode;
import com.zutubi.prototype.model.ControllingCheckboxFieldDescriptor;
import com.zutubi.prototype.model.Field;
import com.zutubi.prototype.model.Form;
import com.zutubi.prototype.model.SubmitFieldDescriptor;
import com.zutubi.prototype.type.MapType;
import com.zutubi.prototype.type.Type;
import com.zutubi.prototype.type.record.PathUtils;
import com.zutubi.prototype.type.record.Record;
import com.zutubi.pulse.bootstrap.MasterConfigurationManager;
import com.zutubi.util.TextUtils;
import com.zutubi.util.logging.Logger;
import com.zutubi.validation.ValidationException;
import com.zutubi.validation.i18n.MessagesTextProvider;
import com.zutubi.validation.i18n.TextProvider;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Action to gather new keys and request a clone for map items.
 */
public class CloneAction extends PrototypeSupport
{
    public static final String CHECK_FIELD_PREFIX = "cloneCheck_";
    public static final String KEY_FIELD_PREFIX   = "cloneKey_";

    private static final Logger LOG = Logger.getLogger(CloneAction.class);

    private ConfigurationPanel newPanel;
    private Record record;
    private String parentPath;
    private MapType mapType;
    private boolean templatedCollection;
    private String formSource;
    private String cloneKey;
    private MasterConfigurationManager configurationManager;
    private ConfigurationRefactoringManager configurationRefactoringManager;

    public ConfigurationPanel getNewPanel()
    {
        return newPanel;
    }

    public String getFormSource()
    {
        return formSource;
    }

    public void setCloneKey(String cloneKey)
    {
        this.cloneKey = cloneKey;
    }

    public void doCancel()
    {
        String parentPath = PathUtils.getParentPath(path);
        String newPath = configurationTemplateManager.isTemplatedCollection(parentPath) ? path : parentPath;
        response = new ConfigurationResponse(newPath, configurationTemplateManager.getTemplatePath(newPath));
    }

    public String execute() throws Exception
    {
        validatePath();
        if(isInputSelected())
        {
            renderForm();
            return INPUT;
        }

        MessagesTextProvider textProvider = new MessagesTextProvider(mapType.getTargetType().getClazz());
        Set<String> seenKeys = new HashSet<String>();
        validateCloneKey("cloneKey", cloneKey, seenKeys, textProvider);

        Map<String, String> keyMap = new HashMap<String, String>();
        keyMap.put(PathUtils.getBaseName(path), cloneKey);
        String newPath = PathUtils.getPath(parentPath, cloneKey);

        if(templatedCollection)
        {
             getDescendents(keyMap, seenKeys, textProvider);
        }

        if(hasErrors())
        {
            renderForm();
            return INPUT;
        }

        configurationRefactoringManager.clone(parentPath, keyMap);

        String templatePath = configurationTemplateManager.getTemplatePath(newPath);
        response = new ConfigurationResponse(newPath, templatePath);
        response.registerNewPathAdded(configurationTemplateManager, configurationSecurityManager);

        return SUCCESS;
    }

    private void validateCloneKey(String name, String value, Set<String> seenKeys, TextProvider textProvider)
    {
        if(!TextUtils.stringSet(value))
        {
            addFieldError(name, "clone name is required");
        }
        else
        {
            if(seenKeys.contains(value))
            {
                addFieldError(name, "duplicate clone name, all clones must have unique names");
            }
            else
            {
                try
                {
                    configurationTemplateManager.validateNameIsUnique(parentPath, value, mapType.getKeyProperty(), textProvider);
                }
                catch(ValidationException e)
                {
                    addFieldError(name, e.getMessage());
                }
            }

            seenKeys.add(value);
        }
    }

    private Map<String, String> getDescendents(Map<String, String> selectedDescedents, Set<String> seenKeys, TextProvider textProvider)
    {
        Map parameters = ActionContext.getContext().getParameters();
        for(Object n: parameters.keySet())
        {
            String name = (String) n;
            if(name.startsWith(CHECK_FIELD_PREFIX))
            {
                String descendentKey = name.substring(CHECK_FIELD_PREFIX.length());
                String value = getParameterValue(parameters, KEY_FIELD_PREFIX + descendentKey);

                if (value != null)
                {
                    validateCloneKey(KEY_FIELD_PREFIX + descendentKey, value, seenKeys, textProvider);
                    selectedDescedents.put(descendentKey, value);
                }
            }
        }

        return selectedDescedents;
    }

    private String getParameterValue(Map parameters, String name)
    {
        Object cloneKeyObject = parameters.get(name);
        String value = null;
        if(cloneKeyObject instanceof String)
        {
            value = (String) cloneKeyObject;
        }
        else if(cloneKeyObject instanceof String[])
        {
            value = ((String[]) cloneKeyObject)[0];
        }
        return value;
    }

    private void validatePath()
    {
        parentPath = PathUtils.getParentPath(path);
        if(parentPath == null)
        {
            throw new IllegalArgumentException("Invalid path '" + path + "': no parent path");
        }

        Type parentType = configurationTemplateManager.getType(parentPath);
        if(!(parentType instanceof MapType))
        {
            throw new IllegalArgumentException("Invalid path '" + path + "': parent is not a map (only map elements may be cloned)");
        }

        mapType = (MapType) parentType;
        type = parentType.getTargetType();
        templatedCollection = configurationTemplateManager.isTemplatedCollection(parentPath);

        record = configurationTemplateManager.getRecord(path);
    }

    private void renderForm() throws IOException, TemplateException
    {
        Form form = new Form("form", "clone", PrototypeUtils.getConfigURL(path, "clone", null, "aconfig"));
        Field field = new Field(FieldType.TEXT, "cloneKey");
        field.setLabel("clone name");
        field.setValue(getValue("cloneKey", getKey(record), ActionContext.getContext().getParameters()));
        form.add(field);

        if(templatedCollection)
        {
            addDescendentFields(form);
        }

        addSubmit(form, "clone", true);
        addSubmit(form, "cancel", false);

        StringWriter writer = new StringWriter();
        PrototypeUtils.renderForm(new HashMap<String, Object>(), form, getClass(), writer, configurationManager);
        formSource = writer.toString();

        newPanel = new ConfigurationPanel("aconfig/clone.vm");
    }

    private void addDescendentFields(final Form form)
    {
        final Map parameters = ActionContext.getContext().getParameters();
        TemplateNode templateNode = configurationTemplateManager.getTemplateNode(path);
        templateNode.forEachDescendent(new TemplateNode.NodeHandler()
        {
            public boolean handle(TemplateNode node)
            {
                Record record = configurationTemplateManager.getRecord(node.getPath());
                String key = getKey(record);
                String nameField = KEY_FIELD_PREFIX + key;

                Field field = new Field(FieldType.CONTROLLING_CHECKBOX, CHECK_FIELD_PREFIX + key);
                field.addParameter(ControllingCheckboxFieldDescriptor.PARAM_INVERT, false);
                field.addParameter(ControllingCheckboxFieldDescriptor.PARAM_DEPENDENT_FIELDS, getDependentFields(nameField, node));
                field.setLabel("clone descendent '" + key + "'");
                if(parameters.containsKey(CHECK_FIELD_PREFIX + key))
                {
                    field.setValue("true");
                }
                form.add(field);

                field = new Field(FieldType.TEXT, nameField);
                field.setLabel("clone name");
                field.setValue(getValue(KEY_FIELD_PREFIX + key, key, parameters));
                form.add(field);

                return true;
            }
        }, true);
    }

    private String[] getDependentFields(String textField, TemplateNode node)
    {
        String[] result = new String[node.getChildren().size() + 1];
        result[0] = textField;
        int i = 1;
        for(TemplateNode child: node.getChildren())
        {
            result[i++] = CHECK_FIELD_PREFIX + child.getId();
        }

        return result;
    }

    private void addSubmit(Form form, String name, boolean isDefault)
    {
        Field field = new Field(FieldType.SUBMIT, name);
        field.setValue(name);
        if(isDefault)
        {
            field.addParameter(SubmitFieldDescriptor.PARAM_DEFAULT, true);
        }
        
        form.add(field);
    }

    private String getValue(String fieldName, String key, Map parameters)
    {
        if (isInputSelected())
        {
            return "clone of " + key;
        }
        else
        {
            return getParameterValue(parameters, fieldName);
        }
    }

    private String getKey(Record record)
    {
        return (String) record.get(mapType.getKeyProperty());
    }

    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    public void setConfigurationRefactoringManager(ConfigurationRefactoringManager configurationRefactoringManager)
    {
        this.configurationRefactoringManager = configurationRefactoringManager;
    }
}
