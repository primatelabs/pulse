package com.zutubi.pulse.core.commands.bjam;

import com.zutubi.pulse.core.commands.core.RegexPostProcessorConfiguration;
import com.zutubi.tove.annotations.SymbolicName;

/**
 * A pre-cannoed regex post-processor configuration that looks for error
 * messages from Boost Jam (bjam).
 */
@SymbolicName("zutubi.bjamPostProcessorConfig")
public class BJamPostProcessorConfiguration extends RegexPostProcessorConfiguration
{
    public BJamPostProcessorConfiguration()
    {
        addErrorRegexes("^error:",
                       "^rule [a-zA-Z0-9_-]+ unknown",
                       "^\\.\\.\\.failed",
                       "^\\*\\*\\* argument error",
                       "^don't know how to make",
                       "^syntax error");

        addWarningRegexes("^warning:");

        setFailOnError(false);
        setLeadingContext(1);
        setTrailingContext(3);
    }

    public BJamPostProcessorConfiguration(String name)
    {
        this();
        setName(name);
    }
}
