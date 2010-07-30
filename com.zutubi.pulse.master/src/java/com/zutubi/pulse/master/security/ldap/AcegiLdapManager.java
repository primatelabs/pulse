package com.zutubi.pulse.master.security.ldap;

import com.zutubi.events.Event;
import com.zutubi.events.EventManager;
import com.zutubi.pulse.master.license.LicenseHolder;
import com.zutubi.pulse.master.model.UserManager;
import com.zutubi.pulse.master.security.AcegiUser;
import com.zutubi.pulse.master.tove.config.admin.LDAPConfiguration;
import com.zutubi.pulse.master.tove.config.group.UserGroupConfiguration;
import com.zutubi.pulse.master.tove.config.user.UserConfiguration;
import com.zutubi.pulse.master.tove.config.user.UserPreferencesConfiguration;
import com.zutubi.pulse.master.tove.config.user.contacts.ContactConfiguration;
import com.zutubi.pulse.master.tove.config.user.contacts.EmailContactConfiguration;
import com.zutubi.tove.config.ConfigurationEventListener;
import com.zutubi.tove.config.ConfigurationProvider;
import com.zutubi.tove.config.events.ConfigurationEvent;
import com.zutubi.tove.config.events.PostSaveEvent;
import com.zutubi.tove.events.ConfigurationEventSystemStartedEvent;
import com.zutubi.tove.events.ConfigurationSystemStartedEvent;
import com.zutubi.util.StringUtils;
import com.zutubi.util.logging.Logger;
import org.springframework.security.BadCredentialsException;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.Authentication;
import org.springframework.security.ldap.DefaultInitialDirContextFactory;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.providers.ldap.authenticator.BindAuthenticator;
import org.springframework.security.providers.ldap.LdapAuthenticationProvider;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;
import org.springframework.security.ldap.populator.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.userdetails.ldap.LdapUserDetails;
import org.springframework.security.userdetails.ldap.LdapUserDetailsMapper;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.ldap.core.DirContextOperations;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import java.util.*;

/**
 */
public class AcegiLdapManager implements LdapManager, ConfigurationEventListener //, com.zutubi.events.EventListener
{
    public void connect()
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public UserConfiguration authenticate(String username, String password, boolean addContact)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addLdapRoles(AcegiUser user)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean canAutoAdd()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getStatusMessage()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<UserGroupConfiguration> testAuthenticate(LDAPConfiguration configuration, String testLogin, String testPassword)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void handleConfigurationEvent(ConfigurationEvent event)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void handleEvent(Event event)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Class[] getHandledEvents()
    {
        return new Class[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
    /*
    private static final Logger LOG = Logger.getLogger(AcegiLdapManager.class);

    private static final String EMAIL_CONTACT_NAME = "LDAP email";
    private static final String PROPERTY_START_TLS = "pulse.ldap.use.starttls";

    private boolean initialised = false;
    private ConfigurationProvider configurationProvider;
    private boolean enabled = false;
    private DefaultInitialDirContextFactory contextFactory;
    private LdapAuthenticationProvider authenticator;
    private List<DefaultLdapAuthoritiesPopulator> populators = new LinkedList<DefaultLdapAuthoritiesPopulator>();
    private boolean autoAdd = false;
    private String statusMessage = null;
    private String emailAttribute = null;
    private UserManager userManager;
    private Map<String, DirContextOperations> detailsMap = new HashMap<String, DirContextOperations>();

    private void registerConfigListeners(ConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
        configurationProvider.registerEventListener(this, false, false, LDAPConfiguration.class);
    }

    private synchronized void init(LDAPConfiguration ldapConfiguration)
    {
        initialised = false;
        statusMessage = null;

        enabled = ldapConfiguration != null && ldapConfiguration.isEnabled();

        if (enabled)
        {
            autoAdd = ldapConfiguration.getAutoAddUsers();
            emailAttribute = ldapConfiguration.getEmailAttribute();

            String hostUrl = ldapConfiguration.getLdapUrl();
            String baseDn = ldapConfiguration.getBaseDn();
            String managerDn = ldapConfiguration.getManagerDn();
            String managerPassword = ldapConfiguration.getManagerPassword();
            boolean followReferrals = ldapConfiguration.getFollowReferrals();
            boolean escapeSpaces = ldapConfiguration.getEscapeSpaceCharacters();

            contextFactory = createContextFactory(hostUrl, baseDn, managerDn, managerPassword, followReferrals, escapeSpaces);
            authenticator = createAuthenticator(ldapConfiguration.getUserBaseDn(), ldapConfiguration.getUserFilter(), ldapConfiguration.getPasswordAttribute(), contextFactory);

            if (!ldapConfiguration.getGroupBaseDns().isEmpty())
            {
                populators = createPopulators(ldapConfiguration.getGroupBaseDns(), ldapConfiguration.getGroupSearchFilter(), ldapConfiguration.getGroupRoleAttribute(), ldapConfiguration.getSearchGroupSubtree(), escapeSpaces, contextFactory);
            }
        }
        else
        {
            contextFactory = null;
            authenticator = null;
            populators.clear();
        }

        initialised = true;
    }

    private DefaultInitialDirContextFactory createContextFactory(String hostUrl, String baseDn, String managerDn, String managerPassword, boolean followReferrals, boolean escapeSpaces)
    {
        if (escapeSpaces)
        {
            baseDn = escapeSpaces(baseDn);
            managerDn = escapeSpaces(managerDn);
        }


        if (!hostUrl.endsWith("/"))
        {
            hostUrl += '/';
        }

        DefaultInitialDirContextFactory result = new DefaultInitialDirContextFactory(hostUrl + baseDn);

        if (StringUtils.stringSet(managerDn))
        {
            result.setManagerDn(managerDn);

            if (StringUtils.stringSet(managerPassword))
            {
                result.setManagerPassword(managerPassword);
            }
        }

        if(followReferrals)
        {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("java.naming.referral", "follow");
            result.setExtraEnvVars(vars);
        }

        return result;
    }

    private LdapAuthenticationProvider createAuthenticator(String userBase, String userFilter, String passwordAttribute, DefaultInitialDirContextFactory contextFactory)
    {
        FilterBasedLdapUserSearch search = new FilterBasedLdapUserSearch(userBase, convertUserFilter(userFilter), contextFactory);
        search.setSearchSubtree(true);

        BindAuthenticator authenticator = new BindAuthenticator(contextFactory);
        authenticator.setUserSearch(search);

        LdapAuthenticationProvider provider = new LdapAuthenticationProvider(authenticator);

        if (StringUtils.stringSet(passwordAttribute))
        {
            LdapUserDetailsMapper mapper = new LdapUserDetailsMapper();
            mapper.setPasswordAttributeName(passwordAttribute);
            provider.setUserDetailsContextMapper(mapper);
        }

        return provider;
    }

    private List<DefaultLdapAuthoritiesPopulator> createPopulators(List<String> groupDns, String groupFilter, String groupRoleAttribute, boolean searchSubtree, boolean escapeSpaces, DefaultInitialDirContextFactory contextFactory)
    {
        List<DefaultLdapAuthoritiesPopulator> populators = new LinkedList<DefaultLdapAuthoritiesPopulator>();
        for (String groupDn: groupDns)
        {
            if (escapeSpaces)
            {
                groupDn = escapeSpaces(groupDn);
            }

            DefaultLdapAuthoritiesPopulator populator = new DefaultLdapAuthoritiesPopulator(contextFactory, groupDn);
            if (StringUtils.stringSet(groupFilter))
            {
                populator.setGroupSearchFilter(convertGroupFilter(groupFilter));
            }

            if (StringUtils.stringSet(groupRoleAttribute))
            {
                populator.setGroupRoleAttribute(groupRoleAttribute);
            }

            populator.setSearchSubtree(searchSubtree);
            populator.setRolePrefix("");
            populator.setConvertToUpperCase(false);
            populators.add(populator);
        }

        return populators;
    }

    private String convertUserFilter(String userFilter)
    {
        return userFilter.replace("${login}", "{0}");
    }

    private String convertGroupFilter(String groupFilter)
    {
        return groupFilter.replace("${user.dn}", "{0}").replace("${login}", "{1}");
    }

    public synchronized void connect()
    {
        statusMessage = null;
        try
        {
            DirContext dirContext = contextFactory.newInitialDirContext();
            if (Boolean.getBoolean(PROPERTY_START_TLS))
            {
                LdapContext ldapContext = (LdapContext) dirContext;
                StartTlsResponse tlsResponse = (StartTlsResponse) ldapContext.extendedOperation(new StartTlsRequest());
                tlsResponse.negotiate();
            }
        }
        catch (Exception e)
        {
            LOG.warning("Unable to connect to LDAP server: " + e.getMessage(), e);
            statusMessage = e.getMessage();
        }
    }

    public synchronized UserConfiguration authenticate(String username, String password, boolean addContact)
    {
        if (enabled && initialised)
        {
            try
            {
                Authentication authentication = ldapAuthenticate(authenticator, username, password);
                UserDetails details = (UserDetails) authentication.getPrincipal();

                String name = getStringAttribute(details, "cn", username);
                if (name == null)
                {
                    name = username;
                }

                UserConfiguration user = userManager.getUserConfig(username);
                if(user == null)
                {
                    user = new UserConfiguration(username, name);
                    user.setAuthenticatedViaLdap(true);
                }

                if (addContact && StringUtils.stringSet(emailAttribute))
                {
                    addContact(user, details);
                }

                detailsMap.put(username, details);
                return user;
            }
            catch (BadCredentialsException e)
            {
                LOG.info("LDAP login failure: user: " + username + " : " + e.getMessage(), e);
            }
            catch (Exception e)
            {
                LOG.warning("Error contacting LDAP server: " + e.getMessage(), e);
                statusMessage = e.getMessage();
            }
        }

        return null;
    }

    private Authentication ldapAuthenticate(LdapAuthenticationProvider authenticator, String username, String password)
    {
        if(!StringUtils.stringSet(password))
        {
            throw new BadCredentialsException("LDAP users cannot have an empty password");
        }

        return authenticator.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }

    public synchronized void addLdapRoles(AcegiUser user)
    {
        DirContextOperations details = detailsMap.get(user.getUsername());
        if (details != null)
        {
            try
            {
                List<UserGroupConfiguration> groups = getLdapGroups(details, populators);
                for(UserGroupConfiguration group: groups)
                {
                    LOG.debug("Adding user '" + user.getUsername() + "' to group '" + group.getName() + "' via LDAP");
                    user.addGroup(group);
                }
            }
            catch (Exception e)
            {
                LOG.severe("Error retrieving group roles from LDAP server: " + e.getMessage(), e);
            }
        }
    }

    private List<UserGroupConfiguration> getLdapGroups(DirContextOperations details, List<DefaultLdapAuthoritiesPopulator> populators)
    {
        List<UserGroupConfiguration> groups = new LinkedList<UserGroupConfiguration>();
        for (DefaultLdapAuthoritiesPopulator populator: populators)
        {
            GrantedAuthority[] ldapAuthorities = populator.getGrantedAuthorities(details, details.getDn().);
            for (GrantedAuthority authority : ldapAuthorities)
            {
                UserGroupConfiguration group = userManager.getGroupConfig(authority.getAuthority());
                if (group != null)
                {
                    groups.add(group);
                }
            }
        }

        return groups;
    }

    private void addContact(UserConfiguration user, DirContextOperations details)
    {
        UserPreferencesConfiguration prefs = user.getPreferences();
        Map<String, ContactConfiguration> contacts = prefs.getContacts();
        if (!contacts.containsKey(EMAIL_CONTACT_NAME))
        {
            String email = getStringAttribute(details, emailAttribute, user.getLogin());
            if (email != null)
            {
                try
                {
                    new InternetAddress(email);
                    EmailContactConfiguration contact = new EmailContactConfiguration();
                    contact.setName(EMAIL_CONTACT_NAME);
                    contact.setAddress(email);
                    contact.setPrimary(contacts.isEmpty());
                    contacts.put(EMAIL_CONTACT_NAME, contact);
                }
                catch (AddressException e)
                {
                    LOG.warning("Ignoring invalid email address '" + email + "' for user '" + user.getLogin() + "'");
                }
            }
        }
    }

    private String getStringAttribute(Authentication details, String attribute, String username)
    {
        Attribute att = details.getAttributes().get(attribute);
        if (att != null)
        {
            try
            {
                Object value = att.get();
                if (value instanceof String)
                {
                    return (String) value;
                }
            }
            catch (NamingException e)
            {
                LOG.debug("Unable to get attribute '" + attribute + "' for user '" + username + "': " + e.getMessage(), e);
            }
        }
        else
        {
            LOG.debug("User '" + username + "' has no '" + attribute + "' attribute");
        }

        return null;
    }

    public synchronized boolean canAutoAdd()
    {
        return enabled && initialised && autoAdd && LicenseHolder.hasAuthorization("canAddUser");
    }

    public List<UserGroupConfiguration> testAuthenticate(LDAPConfiguration configuration, String testLogin, String testPassword)
    {
        DefaultInitialDirContextFactory contextFactory = createContextFactory(configuration.getLdapUrl(), configuration.getBaseDn(), configuration.getManagerDn(), configuration.getManagerPassword(), configuration.getFollowReferrals(), configuration.getEscapeSpaceCharacters());
        contextFactory.newInitialDirContext();

        BindAuthenticator authenticator = createAuthenticator(configuration.getUserBaseDn(), configuration.getUserFilter(), configuration.getPasswordAttribute(), contextFactory);
        DirContextOperations details = ldapAuthenticate(authenticator, testLogin, testPassword);

        if (!configuration.getGroupBaseDns().isEmpty())
        {
            List<DefaultLdapAuthoritiesPopulator> populators = createPopulators(configuration.getGroupBaseDns(), configuration.getGroupSearchFilter(), configuration.getGroupRoleAttribute(), configuration.getSearchGroupSubtree(), configuration.getEscapeSpaceCharacters(), contextFactory);
            return getLdapGroups(details, populators);
        }
        else
        {
            return Collections.emptyList();
        }
    }

    private String escapeSpaces(String dn)
    {
        return dn.replaceAll(" ", "\\\\20");
    }

    public String getStatusMessage()
    {
        return statusMessage;
    }

    public synchronized void handleConfigurationEvent(ConfigurationEvent event)
    {
        if(event instanceof PostSaveEvent)
        {
            init((LDAPConfiguration) event.getInstance());
        }
    }

    public void handleEvent(Event event)
    {
        if (event instanceof ConfigurationEventSystemStartedEvent)
        {
            registerConfigListeners(((ConfigurationEventSystemStartedEvent)event).getConfigurationProvider());
        }
        else
        {
            init(configurationProvider.get(LDAPConfiguration.class));
        }
    }

    public Class[] getHandledEvents()
    {
        return new Class[]{ ConfigurationEventSystemStartedEvent.class, ConfigurationSystemStartedEvent.class };
    }
*/
    public void setUserManager(UserManager userManager)
    {

    }

    public void setEventManager(EventManager eventManager)
    {

    }
}
