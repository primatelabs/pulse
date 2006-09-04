package com.zutubi.pulse.model;

import com.zutubi.pulse.core.model.Entity;
import com.zutubi.pulse.web.DefaultAction;
import org.acegisecurity.userdetails.UserDetails;

import java.util.*;

/**
 * 
 *
 */
public class User extends Entity implements UserDetails
{
    public static int REFRESH_DISABLED = 0;

    private static final String PROPERTY_DASHBOARD_BUILD_COUNT = "user.dashboardBuildCount";
    private static final String PROPERTY_DEFAULT_ACTION = "user.defaultAction";
    private static final String PROPERTY_LDAP_AUTHENTICATION = "user.ldapAuthentication";
    private static final String PROPERTY_SHOW_MY_CHANGES = "show.my.changes";
    private static final String PROPERTY_MY_CHANGES_COUNT = "my.changes.count";
    private static final String PROPERTY_SHOW_PROJECT_CHANGES = "show.project.changes";
    private static final String PROPERTY_PROJECT_CHANGES_COUNT = "project.changes.count";
    private static final String PROPERTY_REFRESH_INTERVAL = "user.refreshInterval";
    private static final String PROPERTY_TAIL_LINES = "tail.lines";
    private static final String PROPERTY_TAIL_REFRESH_INTERVAL = "tail.refresh.interval";

    /**
     * The login name is used to identify this user.
     */
    private String login;

    /**
     * The name of the user is the users full name.
     */
    private String name;

    /**
     * Indicates whether or not the user is enabled. Only enabled users can
     * log in to the system.
     */
    private boolean enabled;

    /**
     * The users password.
     */
    private String password;
    /**
     * Contact points configured by the user for notifications.
     */
    private List<ContactPoint> contactPoints;

    /**
     * List of projects the user wants to hide from their dashboard.
     */
    private Set<Project> hiddenProjects = new HashSet<Project>();

    /**
     * Authorities granted directly to this user.  Authorities may also be
     * granted via the user's groups.
     */
    private List<String> authorities;

    /**
     * Groups this user is a member of.
     */
    private Set<Group> groups = new HashSet<Group>();

    private Map<String, String> properties;

    /**
     * A list of login aliases for the user: logins in other tools that pulse
     * may be interacting with (e.g. SCMs).
     */
    private List<String> aliases;
    private long nextBuildNumber;

    public User()
    {
        aliases = new LinkedList<String>();
    }

    public User(String login, String name)
    {
        this();
        this.login = login;
        this.name = name;
    }

    public User(String login, String name, String password, String... authorities)
    {
        this(login, name);
        this.password = password;
        this.authorities = Arrays.asList(authorities);
    }

    public String getLogin()
    {
        return login;
    }

    public void setLogin(String login)
    {
        this.login = login;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getPassword()
    {
        return password;
    }

    /**
     * NOTE: Do not use this  method to update the users password. Please use the
     * userManager instead.
     *
     * @param encodedPassword
     *
     * @see com.zutubi.pulse.model.UserManager#setPassword(User, String)
     */
    public void setPassword(String encodedPassword)
    {
        this.password = encodedPassword;
    }

    public void add(ContactPoint point)
    {
        getContactPoints().add(point);
        point.setUser(this);
    }

    public boolean remove(ContactPoint point)
    {
        if (contactPoints.remove(point))
        {
            point.setUser(null);
            return true;
        }
        return false;
    }

    private void setContactPoints(List<ContactPoint> l)
    {
        this.contactPoints = l;
    }

    public List<ContactPoint> getContactPoints()
    {
        if (contactPoints == null)
        {
            contactPoints = new LinkedList<ContactPoint>();
        }
        return contactPoints;
    }

    public ContactPoint getContactPoint(String name)
    {
        for (ContactPoint cp : contactPoints)
        {
            if (cp.getName().compareTo(name) == 0)
            {
                return cp;
            }
        }
        return null;
    }

    public ContactPoint getContactPoint(long contactPointId)
    {
        for (ContactPoint cp : contactPoints)
        {
            if (cp.getId() == contactPointId)
            {
                return cp;
            }
        }
        return null;
    }

    public List<Subscription> getSubscriptions()
    {
        List<Subscription> subscriptions = new LinkedList<Subscription>();
        for (ContactPoint cp : contactPoints)
        {
            subscriptions.addAll(cp.getSubscriptions());
        }
        return subscriptions;
    }

    public Subscription getSubscription(long id)
    {
        for(Subscription s: getSubscriptions())
        {
            if(s.getId() == id)
            {
                return s;
            }
        }

        return null;
    }

    public GrantedAuthority[] getAuthorities()
    {
        List<String> directAuthorities = getGrantedAuthorities();
        int total = directAuthorities.size();

        for(Group g: groups)
        {
            total += g.getAuthorityCount();
        }

        GrantedAuthority[] result = new GrantedAuthority[total];
        int i = 0;
        for(String authority: directAuthorities)
        {
            result[i++] = new GrantedAuthority(authority);
        }

        for(Group g: groups)
        {
            for(GrantedAuthority authority: g.getAuthorities())
            {
                result[i++] = authority;
            }
        }

        return result;
    }

    public List<String> getGrantedAuthorities()
    {
        if (authorities == null)
        {
            authorities = new ArrayList<String>();
        }
        return authorities;
    }

    private void setGrantedAuthorities(List<String> authorities)
    {
        this.authorities = authorities;
    }

    public void add(String authority)
    {
        if (!hasAuthority(authority))
        {
            getGrantedAuthorities().add(authority);
        }
    }

    public void remove(String authority)
    {
        // for this to actually delete the authority, we require cascade delete-orphan.
        // otherwise the reference from the authority to the user will keep it alive.
        getGrantedAuthorities().remove(authority);
    }

    public boolean hasAuthority(String authority)
    {
        return getGrantedAuthorities().contains(authority);
    }

    public String getUsername()
    {
        return getLogin();
    }

    /**
     * @see org.acegisecurity.userdetails.UserDetails#isAccountNonExpired()
     */
    public boolean isAccountNonExpired()
    {
        return true;
    }

    /**
     * @see org.acegisecurity.userdetails.UserDetails#isAccountNonLocked()
     */
    public boolean isAccountNonLocked()
    {
        return true;
    }

    /**
     * @see org.acegisecurity.userdetails.UserDetails#isCredentialsNonExpired()
     */
    public boolean isCredentialsNonExpired()
    {
        return true;
    }

    public List<String> getAliases()
    {
        return aliases;
    }

    private void setAliases(List<String> aliases)
    {
        this.aliases = aliases;
    }

    public void addAlias(String alias)
    {
        aliases.add(alias);
    }

    public boolean removeAlias(String alias)
    {
        return aliases.remove(alias);
    }

    public void removeAlias(int index)
    {
        if (index <= aliases.size())
        {
            aliases.remove(index);
        }
    }

    public boolean hasAlias(String alias)
    {
        return aliases.contains(alias);
    }

    public Set<Project> getHiddenProjects()
    {
        return hiddenProjects;
    }

    public void setHiddenProjects(Set<Project> hiddenProjects)
    {
        this.hiddenProjects = hiddenProjects;
    }

    public void addHiddenProject(Project p)
    {
        hiddenProjects.add(p);
    }

    public void clearProjects()
    {
        hiddenProjects.clear();
    }

    public void setProperties(Map<String, String> props)
    {
        this.properties = props;
    }

    public Map<String, String> getProperties()
    {
        if (properties == null)
        {
            properties = new HashMap<String, String>();
        }
        return properties;
    }

    public void setProperty(String key, String value)
    {
        getProperties().put(key, value);
    }

    public String getProperty(String key)
    {
        return getProperties().get(key);
    }

    public boolean hasProperty(String key)
    {
        return getProperties().containsKey(key);
    }

    // ---( for backward compatibility and ease of migration. )---
    /**
     * The action to take the user to when they log in.  Usually their
     * dashboard, but can be a welcome page when they first sign up.
     */
    public String getDefaultAction()
    {
        if (hasProperty(PROPERTY_DEFAULT_ACTION))
        {
            return getProperty(PROPERTY_DEFAULT_ACTION);
        }
        return DefaultAction.DASHBOARD_ACTION;
    }

    public void setDefaultAction(String defaultAction)
    {
        setProperty(PROPERTY_DEFAULT_ACTION, defaultAction);
    }

    /**
     * Number of seconds between refreshes of "live" content, or 0 if the
     * user disables refreshing.
     */
    public int getRefreshInterval()
    {
        if (hasProperty(PROPERTY_REFRESH_INTERVAL))
        {
            return Integer.valueOf(getProperty(PROPERTY_REFRESH_INTERVAL));
        }
        return 60;
    }

    public void setRefreshInterval(int refreshInterval)
    {
        setProperty(PROPERTY_REFRESH_INTERVAL, Integer.toString(refreshInterval));
    }

    public int getTailRefreshInterval()
    {
        return getIntProperty(PROPERTY_TAIL_REFRESH_INTERVAL, 60);
    }

    public void setTailRefreshInterval(int interval)
    {
        setIntProperty(PROPERTY_TAIL_REFRESH_INTERVAL, interval);
    }

    public int getTailLines()
    {
        return getIntProperty(PROPERTY_TAIL_LINES, 30);
    }

    public void setTailLines(int lines)
    {
        setIntProperty(PROPERTY_TAIL_LINES, lines);
    }

    public int getDashboardBuildCount()
    {
        if (hasProperty(PROPERTY_DASHBOARD_BUILD_COUNT))
        {
            return Integer.valueOf(getProperty(PROPERTY_DASHBOARD_BUILD_COUNT));
        }
        return 2;
    }

    public void setDashboardBuildCount(int buildCount)
    {
        setProperty(PROPERTY_DASHBOARD_BUILD_COUNT, Integer.toString(buildCount));
    }

    public boolean getLdapAuthentication()
    {
        return getBooleanProperty(PROPERTY_LDAP_AUTHENTICATION, false);
    }

    public void setLdapAuthentication(boolean useLdap)
    {
        setBooleanProperty(PROPERTY_LDAP_AUTHENTICATION, useLdap);
    }

    public boolean getShowMyChanges()
    {
        return getBooleanProperty(PROPERTY_SHOW_MY_CHANGES, true);
    }

    public void setShowMyChanges(boolean show)
    {
        setBooleanProperty(PROPERTY_SHOW_MY_CHANGES, show);
    }

    public int getMyChangesCount()
    {
        return getIntProperty(PROPERTY_MY_CHANGES_COUNT, 10);
    }

    public void setMyChangesCount(int count)
    {
        setIntProperty(PROPERTY_MY_CHANGES_COUNT, count);
    }

    public boolean getShowProjectChanges()
    {
        return getBooleanProperty(PROPERTY_SHOW_PROJECT_CHANGES, false);
    }

    public void setShowProjectChanges(boolean show)
    {
        setBooleanProperty(PROPERTY_SHOW_PROJECT_CHANGES, show);
    }

    public int getProjectChangesCount()
    {
        return getIntProperty(PROPERTY_PROJECT_CHANGES_COUNT, 10);
    }

    public void setProjectChangesCount(int count)
    {
        setIntProperty(PROPERTY_PROJECT_CHANGES_COUNT, count);
    }

    public boolean equals(Object other)
    {
        if(other == null || !(other instanceof User))
        {
            return false;
        }

        return login.equals(((User)other).login);
    }

    public int hashCode()
    {
        return login.hashCode();
    }

    private boolean getBooleanProperty(String property, boolean defaultValue)
    {
        if(hasProperty(property))
        {
            return Boolean.valueOf(getProperty(property));
        }

        return defaultValue;
    }

    private void setBooleanProperty(String property, boolean value)
    {
        setProperty(property, Boolean.toString(value));
    }

    private int getIntProperty(String property, int defaultValue)
    {
        if(hasProperty(property))
        {
            return Integer.parseInt(getProperty(property));
        }

        return defaultValue;
    }

    private void setIntProperty(String property, int value)
    {
        setProperty(property, Integer.toString(value));
    }

    public Set<Group> getGroups()
    {
        return groups;
    }

    private void setGroups(Set<Group> groups)
    {
        this.groups = groups;
    }

    /**
     * Add a group that this user has become a member of.  Package local:
     * edit this association from the group side!
     *
     * @param group the group to add
     */
    void addGroup(Group group)
    {
        groups.add(group);
    }

    /**
     * Remove a group that this user is no longer a member of.  Package local:
     * edit this association from the group side!
     *
     * @param group the group to remove
     */
    void removeGroup(Group group)
    {
        groups.remove(group);
    }

    public long getNextBuildNumber()
    {
        return nextBuildNumber;
    }

    public void setNextBuildNumber(long nextBuildNumber)
    {
        this.nextBuildNumber = nextBuildNumber;
    }
}
