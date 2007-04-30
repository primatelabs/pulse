package com.zutubi.prototype;

import com.zutubi.prototype.model.HiddenFieldDescriptor;
import com.zutubi.prototype.model.Wizard;
import com.zutubi.prototype.type.record.Record;
import com.zutubi.prototype.wizard.WizardState;
import com.zutubi.prototype.wizard.WizardTransition;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Mapping;

import java.util.List;

/**
 *
 *
 */
public class WizardDescriptor extends AbstractDescriptor
{
    private com.zutubi.prototype.wizard.Wizard wizardInstance;

    private boolean decorate;

    private FormDescriptorFactory formDescriptorFactory;

    public WizardDescriptor(com.zutubi.prototype.wizard.Wizard wizardInstance)
    {
        this.wizardInstance = wizardInstance;
    }

    public Wizard instantiate(String path, Record record)
    {
        Wizard wizard = new Wizard();
        wizard.setDecorate(decorate);

        // create the form wizard for the wizard.
        WizardState state = wizardInstance.getCurrentState();

        FormDescriptor formDescriptor = state.createFormDescriptor(formDescriptorFactory, path);
        formDescriptor.setAction("wizard");

        // decorate the form so that it fits into the wizard.
        decorate(formDescriptor);

        wizard.setForm(formDescriptor.instantiate(path, record));

        wizard.setStepCount(wizardInstance.getStateCount());
        wizard.setCurrentStep(wizardInstance.getCurrentStateIndex() + 1);

        return wizard;
    }

    private void decorate(FormDescriptor descriptor)
    {
        List<String> actions = CollectionUtils.map(wizardInstance.getAvailableActions(), new Mapping<WizardTransition, String>()
        {
            public String map(WizardTransition o)
            {
                return o.name().toLowerCase();
            }
        });

        descriptor.setActions(actions);

        HiddenFieldDescriptor hidden = new HiddenFieldDescriptor();
        hidden.setName("state");
        hidden.setValue(wizardInstance.getCurrentStateIndex());

        descriptor.add(hidden);
    }

    public void setDecorate(boolean decorate)
    {
        this.decorate = decorate;
    }

    public void setFormDescriptorFactory(FormDescriptorFactory formDescriptorFactory)
    {
        this.formDescriptorFactory = formDescriptorFactory;
    }
}
