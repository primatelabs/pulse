package com.zutubi.pulse.master.license;

import com.zutubi.validation.ValidationException;
import com.zutubi.validation.validators.FieldValidatorSupport;

/**
 * Checks if a license key may be decoded.
 */
public class LicenseKeyValidator extends FieldValidatorSupport
{
    public void validateField(Object obj) throws ValidationException
    {
        if (obj == null || !(obj instanceof String))
        {
            addError();
            return;
        }

        try
        {
            String licenseKey = (String) obj;

            // remove any '\n' characters from the license key.
            licenseKey = licenseKey.replaceAll("\n", "");

            // check that the decoder is able to decode the license.
            LicenseDecoder decoder = new LicenseDecoder();
            if (decoder.decode(licenseKey.getBytes()) == null)
            {
                // validation of the license key has failed.
                addError();
            }
        }
        catch (LicenseException e)
        {
            // determining whether or not the license key is valid has failed.
            // the license key may in fact be valid.. lets ignore this problem
            // for now since we will deal with it as an action error later.
        }
    }
}
