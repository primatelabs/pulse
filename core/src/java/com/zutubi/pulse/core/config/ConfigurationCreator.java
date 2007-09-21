package com.zutubi.pulse.core.config;

/**
 * Creators are used in place of the an actual Configuration type when a new
 * instance of that configuration type is being created.  This is useful when
 * the wizard form for a type needs to significantly differ from the edit
 * page.  Often the {@link com.zutubi.config.annotations.Wizard.Ignore} can
 * be a simpler alternative, so creators should only be used when there are
 * non-trivial differences.  One example is in the creation of a new user,
 * where a password and confirm field are shown.
 */
public interface ConfigurationCreator<T extends Configuration> extends Configuration
{
    T create();
}
