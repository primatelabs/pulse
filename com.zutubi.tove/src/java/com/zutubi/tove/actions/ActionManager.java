package com.zutubi.tove.actions;

import com.zutubi.tove.ConventionSupport;
import com.zutubi.tove.config.ConfigurationRefactoringManager;
import com.zutubi.tove.config.ConfigurationSecurityManager;
import com.zutubi.tove.config.ConfigurationTemplateManager;
import com.zutubi.tove.config.api.ActionResult;
import com.zutubi.tove.config.api.Configuration;
import com.zutubi.tove.security.AccessManager;
import com.zutubi.tove.type.CompositeType;
import com.zutubi.tove.type.TypeRegistry;
import com.zutubi.tove.type.record.PathUtils;
import com.zutubi.util.UnaryFunctionE;
import com.zutubi.util.bean.ObjectFactory;
import com.zutubi.util.logging.Logger;

import java.util.*;

/**
 * Provides support for executing actions on configuration instances.  For
 * example, triggering a project build is an action on a ProjectConfiguration
 * instance.  Actions are presented as links in the web UI, and a form may
 * optionally be displayed to capture an action argument.
 */
public class ActionManager
{
    private static final Logger LOG = Logger.getLogger(ActionManager.class);

    public static final String I18N_KEY_SUFFIX_FEEDACK = ".feedback";
    public static final Set<String> COMMON_ACTIONS = new HashSet<String>();
    static
    {
        COMMON_ACTIONS.add(AccessManager.ACTION_VIEW);
        COMMON_ACTIONS.add(ConfigurationRefactoringManager.ACTION_CLONE);
        COMMON_ACTIONS.add(ConfigurationRefactoringManager.ACTION_PULL_UP);
        COMMON_ACTIONS.add(ConfigurationRefactoringManager.ACTION_PUSH_DOWN);
        COMMON_ACTIONS.add(AccessManager.ACTION_DELETE);
    }

    private Map<CompositeType, ConfigurationActions> actionsByType = new HashMap<CompositeType, ConfigurationActions>();
    private ObjectFactory objectFactory;
    private TypeRegistry typeRegistry;
    private ConfigurationSecurityManager configurationSecurityManager;
    private ConfigurationRefactoringManager configurationRefactoringManager;
    private ConfigurationTemplateManager configurationTemplateManager;

    public List<String> getActions(Configuration configurationInstance, boolean includeDefault, boolean includeNonSimple)
    {
        List<String> result = new LinkedList<String>();

        if (configurationInstance != null)
        {
            String path = configurationInstance.getConfigurationPath();
            if (includeDefault)
            {
                if (configurationSecurityManager.hasPermission(path, AccessManager.ACTION_VIEW))
                {
                    result.add(AccessManager.ACTION_VIEW);
                }

                if (includeNonSimple)
                {
                    if (configurationRefactoringManager.canClone(path) && configurationSecurityManager.hasPermission(PathUtils.getParentPath(path), AccessManager.ACTION_CREATE))
                    {
                        result.add(ConfigurationRefactoringManager.ACTION_CLONE);
                    }

                    if (configurationRefactoringManager.canPullUp(path))
                    {
                        result.add(ConfigurationRefactoringManager.ACTION_PULL_UP);
                    }

                    if (configurationRefactoringManager.canPushDown(path))
                    {
                        result.add(ConfigurationRefactoringManager.ACTION_PUSH_DOWN);
                    }
                }

                if (configurationTemplateManager.canDelete(path) && configurationSecurityManager.hasPermission(path, AccessManager.ACTION_DELETE))
                {
                    result.add(AccessManager.ACTION_DELETE);
                }
            }

            CompositeType type = getType(configurationInstance);
            ConfigurationActions configurationActions = getConfigurationActions(type);

            if (configurationActions.actionsEnabled(configurationInstance, configurationTemplateManager.isDeeplyValid(path)))
            {
                try
                {
                    List<ConfigurationAction> actions = configurationActions.getActions(configurationInstance);
                    for (ConfigurationAction action : actions)
                    {
                        if ((includeNonSimple || isSimple(action)) && configurationSecurityManager.hasPermission(path, action.getPermissionName()))
                        {
                            result.add(action.getName());
                        }
                    }
                }
                catch (Exception e)
                {
                    LOG.severe(e);
                }
            }
        }

        return result;
    }

    private boolean isSimple(ConfigurationAction action)
    {
        return !action.hasArgument() && !action.hasVariants();
    }

    public void ensurePermission(String path, String actionName)
    {
        CompositeType type = configurationTemplateManager.getType(path, CompositeType.class);
        ConfigurationActions actions = getConfigurationActions(type);
        ConfigurationAction action = actions.getAction(actionName);

        if (action != null)
        {
            configurationSecurityManager.ensurePermission(path, action.getPermissionName());
        }
        else
        {
            LOG.warning("Permission check for unrecognised action '" + actionName + "' on path '" + path + "'");
        }
    }

    public List<String> getVariants(final String actionName, final Configuration configurationInstance)
    {
        if (COMMON_ACTIONS.contains(actionName))
        {
            return null;
        }

        return processAction(actionName, configurationInstance, new UnaryFunctionE<ConfigurationActions,List<String>,Exception>()
        {
            public List<String> process(ConfigurationActions actions) throws Exception
            {
                return actions.getVariants(actionName, configurationInstance);
            }
        });
    }

    public Configuration prepare(final String actionName, final Configuration configurationInstance)
    {
        return processAction(actionName, configurationInstance, new UnaryFunctionE<ConfigurationActions, Configuration, Exception>()
        {
            public Configuration process(ConfigurationActions actions) throws Exception
            {
                return actions.prepare(actionName, configurationInstance);
            }
        });
    }

    public ActionResult execute(final String actionName, final Configuration configurationInstance, final Configuration argumentInstance)
    {
        return processAction(actionName, configurationInstance, new UnaryFunctionE<ConfigurationActions, ActionResult, Exception>()
        {
            public ActionResult process(ConfigurationActions actions) throws Exception
            {
                return actions.execute(actionName, configurationInstance, argumentInstance);
            }
        });
    }

    /**
     * Executes the given action on all concrete descendants of the given path
     * that have the action currently avaialable.  The action must be simple -
     * i.e. it cannot require an argument or use a custom UI.
     *
     * @param actionName name of the action to execute
     * @param path       path to process the concrete descendants of
     * @return a mapping from path to result for all concrete descendants on
     *         which the action was performed
     */
    public Map<String, ActionResult> executeOnDescendants(String actionName, String path)
    {
        Map<String, ActionResult> results = new HashMap<String, ActionResult>();
        List<String> concreteDescendantPaths = configurationTemplateManager.getDescendantPaths(path, true, true, false);
        for (String descendantPath: concreteDescendantPaths)
        {
            Configuration descendant = configurationTemplateManager.getInstance(descendantPath);
            if (descendant != null && getActions(descendant, false, false).contains(actionName))
            {
                results.put(descendantPath, execute(actionName, descendant, null));
            }
        }

        return results;
    }

    private <T> T processAction(String actionName, Configuration configurationInstance, UnaryFunctionE<ConfigurationActions, T, Exception> f)
    {
        CompositeType type = getType(configurationInstance);
        ConfigurationActions actions = getConfigurationActions(type);
        ConfigurationAction action = actions.getAction(actionName);

        if (action != null)
        {
            configurationSecurityManager.ensurePermission(configurationInstance.getConfigurationPath(), action.getPermissionName());

            try
            {
                return f.process(actions);
            }
            catch (Exception e)
            {
                LOG.severe(e);
                throw new RuntimeException(e);
            }
        }
        else
        {
            throw new IllegalArgumentException("Request for unrecognised action '" + actionName + "' on path '" + configurationInstance.getConfigurationPath() + "'");
        }
    }

    private CompositeType getType(Object configurationInstance)
    {
        CompositeType type = typeRegistry.getType(configurationInstance.getClass());
        if (type == null)
        {
            throw new IllegalArgumentException("Invalid instance: not of configuration type");
        }
        return type;
    }

    public synchronized ConfigurationActions getConfigurationActions(CompositeType type)
    {
        ConfigurationActions actions = actionsByType.get(type);
        if (actions == null)
        {
            Class<? extends Configuration> configurationClass = type.getClazz();
            actions = new ConfigurationActions(configurationClass, ConventionSupport.getActions(configurationClass), objectFactory);
            actionsByType.put(type, actions);
        }

        return actions;
    }

    public void setObjectFactory(ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }

    public void setTypeRegistry(TypeRegistry typeRegistry)
    {
        this.typeRegistry = typeRegistry;
    }

    public void setConfigurationSecurityManager(ConfigurationSecurityManager configurationSecurityManager)
    {
        this.configurationSecurityManager = configurationSecurityManager;
    }

    public void setConfigurationRefactoringManager(ConfigurationRefactoringManager configurationRefactoringManager)
    {
        this.configurationRefactoringManager = configurationRefactoringManager;
    }

    public void setConfigurationTemplateManager(ConfigurationTemplateManager configurationTemplateManager)
    {
        this.configurationTemplateManager = configurationTemplateManager;
    }
}
