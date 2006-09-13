package com.zutubi.validation;

import com.zutubi.validation.providers.AnnotationValidatorProvider;
import com.zutubi.validation.providers.ReflectionValidatorProvider;

import java.util.List;
import java.util.LinkedList;

/**
 * <class-comment/>
 */
public class DefaultValidationManager implements ValidationManager
{
    private List<ValidatorProvider> providers = new LinkedList<ValidatorProvider>();

    public DefaultValidationManager()
    {
        // configure the default validation provider.
        providers.add(new AnnotationValidatorProvider());
        providers.add(new ReflectionValidatorProvider());
    }

    public void validate(Object o) throws ValidationException
    {
        validate(o, new DelegatingValidationContext(o));
    }

    public void validate(Object o, ValidationContext context) throws ValidationException
    {
        // get validators
        List<Validator> validators = new LinkedList<Validator>();
        for (ValidatorProvider provider : providers)
        {
            validators.addAll(provider.getValidators(o));
        }

        // run them.
        for (Validator v : validators)
        {
            v.setValidationContext(context);
            v.validate(o);
        }
    }

    public void setProviders(List<ValidatorProvider> providers)
    {
        this.providers = providers;
    }

    public void addValidatorProvider(ValidatorProvider provider)
    {
        this.providers.add(provider);
    }
}
