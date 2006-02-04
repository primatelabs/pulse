package com.cinnamonbob.xwork.validator.validators;

import com.opensymphony.xwork.validator.validators.FieldValidatorSupport;
import com.opensymphony.xwork.validator.ValidationException;
import org.netbeans.lib.cvsclient.CVSRoot;

/**
 *
 *
 */
public class CvsRootValidator extends FieldValidatorSupport
{
    public void validate(Object object) throws ValidationException
    {
        Object obj = getFieldValue(getFieldName(), object);
        if (obj == null)
        {
            addFieldError(getFieldName(), getMessageKey());
        }
        try
        {
            CVSRoot.parse((String)obj);
        }
        catch (IllegalArgumentException iae)
        {
            addFieldError(getFieldName(), iae.getMessage());
        }
    }
}
