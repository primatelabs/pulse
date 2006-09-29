package com.zutubi.pulse.web.admin;

import com.zutubi.pulse.agent.AgentManager;
import com.zutubi.pulse.bootstrap.MasterConfiguration;
import com.zutubi.pulse.bootstrap.MasterConfigurationManager;
import com.zutubi.pulse.jabber.JabberManager;
import com.zutubi.pulse.license.License;
import com.zutubi.pulse.license.LicenseHolder;
import com.zutubi.pulse.model.CommitMessageTransformer;
import com.zutubi.pulse.model.ProjectManager;
import com.zutubi.pulse.model.UserManager;
import com.zutubi.pulse.security.ldap.LdapManager;
import com.zutubi.pulse.web.ActionSupport;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

/**
 */
public class ServerSettingsAction extends ActionSupport
{
    private MasterConfigurationManager configurationManager;
    private JabberManager jabberManager;
    private MasterConfiguration config;
    private DateFormat dateFormatter = new SimpleDateFormat("EEEEE, dd MMM yyyy");
    private LdapManager ldapManager;
    private List<CommitMessageTransformer> commitMessageTransformers;
    private ProjectManager projectManager;
    private AgentManager agentManager;
    private UserManager userManager;

    //---( License details )---
    private License license;
    private List<LicenseRestriction> restrictions;

    public MasterConfiguration getConfig()
    {
        return config;
    }

    public String getJabberStatus()
    {
        return jabberManager.getStatusMessage();
    }

    public String getLdapStatus()
    {
        return ldapManager.getStatusMessage();
    }

    public List<CommitMessageTransformer> getCommitMessageTransformers()
    {
        return commitMessageTransformers;
    }

    public boolean isRecipeTimeoutEnabled()
    {
        return config.getUnsatisfiableRecipeTimeout() >= 0;
    }

    public String execute() throws Exception
    {
        config = configurationManager.getAppConfig();
        commitMessageTransformers = projectManager.getCommitMessageTransformers();

        license = LicenseHolder.getLicense();

        restrictions = new LinkedList<LicenseRestriction>();
        restrictions.add(new LicenseRestriction("agents", license.getSupportedAgents(), agentManager.getAgentCount()));
        restrictions.add(new LicenseRestriction("projects", license.getSupportedProjects(), projectManager.getProjectCount()));
        restrictions.add(new LicenseRestriction("users", license.getSupportedUsers(), userManager.getUserCount()));

        return SUCCESS;
    }

    public List<LicenseRestriction> getRestrictions()
    {
        return restrictions;
    }

    public String getExpiryDate()
    {
        if (license.expires())
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(license.getExpiryDate());
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return dateFormatter.format(cal.getTime());
        }
        return "Never";
    }

    public String getUnrestricted()
    {
        return getText("license.unrestricted");
    }

    /**
     * Required resource.
     *
     * @param configurationManager
     */
    public void setConfigurationManager(MasterConfigurationManager configurationManager)
    {
        this.configurationManager = configurationManager;
    }

    /**
     * Required resource.
     *
     * @param jabberManager
     */
    public void setJabberManager(JabberManager jabberManager)
    {
        this.jabberManager = jabberManager;
    }

    /**
     * Required resource.
     *
     * @param ldapManager
     */
    public void setLdapManager(LdapManager ldapManager)
    {
        this.ldapManager = ldapManager;
    }

    /**
     * Required resource.
     *
     * @param agentManager
     */
    public void setAgentManager(AgentManager agentManager)
    {
        this.agentManager = agentManager;
    }

    /**
     * Required resource.
     *
     * @param projectManager
     */
    public void setProjectManager(ProjectManager projectManager)
    {
        this.projectManager = projectManager;
    }

    /**
     * Required resource.
     *
     * @param userManager
     */
    public void setUserManager(UserManager userManager)
    {
        this.userManager = userManager;
    }

    public static class LicenseRestriction
    {
        private String entity;
        private int supported;
        private int inUse;

        public LicenseRestriction(String entity, int supported, int inUse)
        {
            this.entity = entity;
            this.supported = supported;
            this.inUse = inUse;
        }

        public String getEntity()
        {
            return entity;
        }

        public int getSupported()
        {
            return supported;
        }

        public int getInUse()
        {
            return inUse;
        }
    }
}
