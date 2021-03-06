package com.zutubi.pulse.core.scm.cvs.config;

import com.zutubi.pulse.core.scm.config.api.PollableScmConfiguration;
import com.zutubi.pulse.core.scm.cvs.validation.annotation.CvsRoot;
import com.zutubi.tove.annotations.*;
import com.zutubi.validation.annotations.Required;

/**
 *
 *
 */
@Form(fieldOrder = {"root", "password", "module", "branch", "monitor", "customPollingInterval", "pollingInterval", "quietPeriodEnabled", "quietPeriod", "includedPaths", "excludedPaths"})
@ConfigurationCheck("CvsConfigurationCheckHandler")
@SymbolicName("zutubi.cvsConfig")
public class CvsConfiguration extends PollableScmConfiguration
{
    @Required @CvsRoot
    @Text
    private String root;
    @Required
    private String module;
    
    @Password
    private String password;
    private String branch;

    public CvsConfiguration()
    {
        setQuietPeriodEnabled(true);
        setQuietPeriod(5);
    }

    public String getRoot()
    {
        return root;
    }

    public void setRoot(String root)
    {
        this.root = root;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getBranch()
    {
        return branch;
    }

    public void setBranch(String branch)
    {
        this.branch = branch;
    }

    public String getModule()
    {
        return module;
    }

    public void setModule(String module)
    {
        this.module = module;
    }

    public String getType()
    {
        return "cvs";
    }
}
