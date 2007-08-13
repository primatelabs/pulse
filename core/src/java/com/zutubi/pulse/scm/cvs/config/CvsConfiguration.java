package com.zutubi.pulse.scm.cvs.config;

import com.zutubi.config.annotations.ConfigurationCheck;
import com.zutubi.config.annotations.Form;
import com.zutubi.config.annotations.Password;
import com.zutubi.config.annotations.SymbolicName;
import com.zutubi.config.annotations.Text;
import com.zutubi.pulse.scm.config.ScmConfiguration;
import com.zutubi.pulse.scm.cvs.validation.annotation.CvsRoot;
import com.zutubi.pulse.scm.cvs.CvsClient;
import com.zutubi.validation.annotations.Required;

/**
 *
 *
 */
@Form(fieldOrder = {"root", "password", "module", "branch", "monitor", "checkoutScheme", "customPollingInterval", "pollingInterval", "quietPeriodEnabled", "quietPeriod"})
@ConfigurationCheck("CvsConfigurationCheckHandler")
@SymbolicName("zutubi.cvsConfig")
public class CvsConfiguration extends ScmConfiguration
{
    @Required @CvsRoot
    @Text
    private String root;

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
        return CvsClient.TYPE;
    }
}
