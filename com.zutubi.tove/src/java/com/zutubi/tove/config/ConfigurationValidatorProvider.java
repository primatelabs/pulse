package com.zutubi.tove.config;

import com.google.common.base.Predicate;
import static com.google.common.collect.Iterables.addAll;
import static com.google.common.collect.Iterables.filter;
import com.zutubi.validation.ValidationContext;
import com.zutubi.validation.Validator;
import com.zutubi.validation.ValidatorProvider;
import com.zutubi.validation.validators.RequiredValidator;

import java.util.LinkedList;
import java.util.List;

/**
 * A validator provider that adds capabilities customised to the
 * configuration subsystem.
 */
public class ConfigurationValidatorProvider implements ValidatorProvider
{
    private List<ValidatorProvider> delegates = new LinkedList<ValidatorProvider>();

    public List<Validator> getValidators(Object obj, ValidationContext context)
    {
        List<Validator> validators = new LinkedList<Validator>();
        final boolean includeRequired = !(context instanceof ConfigurationValidationContext) || !((ConfigurationValidationContext)context).isTemplate();
        for(ValidatorProvider delegate: delegates)
        {
            addAll(validators, filter(delegate.getValidators(obj, context), new Predicate<Validator>()
            {
                public boolean apply(Validator validator)
                {
                    return !(!includeRequired && validator instanceof RequiredValidator) || !((RequiredValidator) validator).isIgnorable();
                }
            }));
        }

        return validators;
    }

    public void setDelegates(List<ValidatorProvider> delegates)
    {
        this.delegates = delegates;
    }
}
