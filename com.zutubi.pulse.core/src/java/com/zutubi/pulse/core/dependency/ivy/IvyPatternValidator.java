package com.zutubi.pulse.core.dependency.ivy;

import com.zutubi.tove.variables.VariableResolver;
import com.zutubi.tove.variables.api.ResolutionException;
import com.zutubi.validation.ValidationException;
import com.zutubi.validation.validators.StringFieldValidatorSupport;
import org.apache.ivy.core.IvyPatternHelper;

import java.util.Collections;

/**
 * A validator that checks the format of ivy patterns.
 */
public class IvyPatternValidator extends StringFieldValidatorSupport
{
    public IvyPatternValidator()
    {
        super(false);
    }

    protected void validateStringField(String value) throws ValidationException
    {
        try
        {
            if (VariableResolver.containsVariable(value))
            {
                // do not validate strings that contain references.
                return;
            }
        }
        catch (ResolutionException e)
        {
            // noop.
        }

        try
        {
            IvyPatternHelper.substituteTokens(value, Collections.emptyMap());
        }
        catch (Exception e)
        {
            addErrorMessage(e.getMessage());
        }
    }
}
