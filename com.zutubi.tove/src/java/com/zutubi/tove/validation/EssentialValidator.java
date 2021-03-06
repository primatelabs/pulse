package com.zutubi.tove.validation;

import com.zutubi.tove.config.ConfigurationValidationContext;
import com.zutubi.validation.ValidationException;
import com.zutubi.validation.validators.FieldValidatorSupport;

/**
 * Checks that essential (see {@link com.zutubi.tove.annotations.Essential})
 * fields are set up when the context indicates they should be checked (for
 * concrete instances when full validation takes place).
 */
public class EssentialValidator extends FieldValidatorSupport
{
    public EssentialValidator()
    {
        super("essential");
    }

    protected void validateField(Object value) throws ValidationException
    {
        if(validationContext instanceof ConfigurationValidationContext)
        {
            ConfigurationValidationContext configurationValidationContext = (ConfigurationValidationContext) validationContext;
            if(configurationValidationContext.isCheckEssential() && !configurationValidationContext.isTemplate() && value == null)
            {
                // Add as an instance rather than a field error as we are
                // applied to complex subfields.
                validationContext.addActionError(getErrorMessage());
            }
        }
    }
}
