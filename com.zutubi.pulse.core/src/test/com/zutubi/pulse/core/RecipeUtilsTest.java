package com.zutubi.pulse.core;

import com.zutubi.pulse.core.engine.api.ExecutionContext;
import com.zutubi.pulse.core.resources.ResourceRequirement;
import com.zutubi.pulse.core.resources.api.ResourceConfiguration;
import com.zutubi.pulse.core.resources.api.ResourcePropertyConfiguration;
import com.zutubi.pulse.core.resources.api.ResourceVersionConfiguration;
import com.zutubi.pulse.core.test.api.PulseTestCase;
import com.zutubi.util.SystemUtils;

import java.util.Set;

import static java.util.Arrays.asList;

public class RecipeUtilsTest extends PulseTestCase
{
    private static final String PROPERTY_SUFFIX = "-prop";
    private static final String VALUE_SUFFIX = "-val";

    private static final String RESOURCE_WITH_DEFAULT = "with-default";
    private static final String VERSION_DEFAULT = "default";
    private static final String VERSION_NON_DEFAULT = "non-default";

    private static final String RESOURCE_NO_DEFAULT = "no-default";
    private static final String VERSION_ONLY = "oly";

    private InMemoryResourceRepository resourceRepository;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        resourceRepository = new InMemoryResourceRepository();

        ResourceConfiguration withDefaultResource = new ResourceConfiguration(RESOURCE_WITH_DEFAULT);
        withDefaultResource.addProperty(property(RESOURCE_WITH_DEFAULT));

        ResourceVersionConfiguration defaultVersion = new ResourceVersionConfiguration(VERSION_DEFAULT);
        defaultVersion.addProperty(property(VERSION_DEFAULT));
        withDefaultResource.addVersion(defaultVersion);

        ResourceVersionConfiguration nonDefaultVersion = new ResourceVersionConfiguration(VERSION_NON_DEFAULT);
        nonDefaultVersion.addProperty(property(VERSION_NON_DEFAULT));
        withDefaultResource.addVersion(nonDefaultVersion);

        withDefaultResource.setDefaultVersion(VERSION_DEFAULT);
        resourceRepository.addResource(withDefaultResource);

        ResourceConfiguration noDefaultResource = new ResourceConfiguration(RESOURCE_NO_DEFAULT);
        noDefaultResource.addProperty(property(RESOURCE_NO_DEFAULT));

        ResourceVersionConfiguration onlyVersion = new ResourceVersionConfiguration(VERSION_ONLY);
        onlyVersion.addProperty(property(VERSION_ONLY));
        noDefaultResource.addVersion(onlyVersion);
        resourceRepository.addResource(noDefaultResource);
    }

    private ResourcePropertyConfiguration property(String name)
    {
        return new ResourcePropertyConfiguration(propertyName(name), propertyValue(name));
    }

    private String propertyName(String name)
    {
        return name + PROPERTY_SUFFIX;
    }

    private String propertyValue(String name)
    {
        return propertyName(name) + VALUE_SUFFIX;
    }

    public void testAcceptableNamesOnWindows()
    {
        if (!SystemUtils.IS_WINDOWS)
        {
            return;
        }

        Set<String> suppressedEnvironment = RecipeUtils.getSuppressedEnvironment();
        assertTrue(RecipeUtils.acceptableName("^", suppressedEnvironment));
        assertTrue(RecipeUtils.acceptableName("<", suppressedEnvironment));
        assertTrue(RecipeUtils.acceptableName(">", suppressedEnvironment));
        assertTrue(RecipeUtils.acceptableName("|", suppressedEnvironment));
        assertTrue(RecipeUtils.acceptableName("&", suppressedEnvironment));
        assertTrue(RecipeUtils.acceptableName(" ", suppressedEnvironment));

        assertFalse(RecipeUtils.acceptableName("=", suppressedEnvironment));
    }

    public void testAcceptableNames()
    {
        Set<String> suppressedEnvironment = RecipeUtils.getSuppressedEnvironment();
        assertTrue(RecipeUtils.acceptableName("a", suppressedEnvironment));
        assertTrue(RecipeUtils.acceptableName("Z", suppressedEnvironment));
        assertTrue(RecipeUtils.acceptableName("2", suppressedEnvironment));

        assertFalse(RecipeUtils.acceptableName("env.something", suppressedEnvironment));
    }

    public void testConvertNames()
    {
        assertEquals("PULSE_A", RecipeUtils.convertName("a"));
        assertEquals("PULSE_1", RecipeUtils.convertName("1"));
    }

    public void testImportDefaultVersion()
    {
        ExecutionContext context = populateContext(new ResourceRequirement(RESOURCE_WITH_DEFAULT, false, false));
        assertPropertyAdded(context, RESOURCE_WITH_DEFAULT);
        assertPropertyAdded(context, VERSION_DEFAULT);
        assertPropertyNotAdded(context, VERSION_NON_DEFAULT);
    }

    public void testImportSpecificVersion()
    {
        ExecutionContext context = populateContext(new ResourceRequirement(RESOURCE_WITH_DEFAULT, VERSION_NON_DEFAULT, false, false));
        assertPropertyAdded(context, RESOURCE_WITH_DEFAULT);
        assertPropertyNotAdded(context, VERSION_DEFAULT);
        assertPropertyAdded(context, VERSION_NON_DEFAULT);
    }

    public void testImportDefaultVersionNoDefault()
    {
        ExecutionContext context = populateContext(new ResourceRequirement(RESOURCE_NO_DEFAULT, false, false));
        assertPropertyAdded(context, RESOURCE_NO_DEFAULT);
        assertPropertyNotAdded(context, VERSION_ONLY);
    }

    public void testImportUnknownResource()
    {
        ExecutionContext context = populateContext(new ResourceRequirement("unknown", false, false));
        assertPropertyNotAdded(context, RESOURCE_WITH_DEFAULT);
        assertPropertyNotAdded(context, RESOURCE_NO_DEFAULT);
    }

    public void testImportUnknownVersion()
    {
        ExecutionContext context = populateContext(new ResourceRequirement(RESOURCE_WITH_DEFAULT, "unknown", false, false));
        assertPropertyAdded(context, RESOURCE_WITH_DEFAULT);
        assertPropertyNotAdded(context, VERSION_DEFAULT);
        assertPropertyNotAdded(context, VERSION_NON_DEFAULT);
    }

    public void testImportMultipleResources()
    {
        ExecutionContext context = populateContext(new ResourceRequirement(RESOURCE_WITH_DEFAULT, false, false), new ResourceRequirement(RESOURCE_NO_DEFAULT, false, false));
        assertPropertyAdded(context, RESOURCE_WITH_DEFAULT);
        assertPropertyAdded(context, VERSION_DEFAULT);
        assertPropertyNotAdded(context, VERSION_NON_DEFAULT);
        assertPropertyAdded(context, RESOURCE_NO_DEFAULT);
        assertPropertyNotAdded(context, VERSION_ONLY);
    }

    public void testImportResourceExistingAfterUnknown()
    {
        ExecutionContext context = populateContext(new ResourceRequirement("unknown", false, false), new ResourceRequirement(RESOURCE_WITH_DEFAULT, false, false));
        assertPropertyAdded(context, RESOURCE_WITH_DEFAULT);
        assertPropertyAdded(context, VERSION_DEFAULT);
        assertPropertyNotAdded(context, RESOURCE_NO_DEFAULT);
    }

    public void testImportResourceExistingAfterUnknownVerison()
    {
        ExecutionContext context = populateContext(new ResourceRequirement(RESOURCE_WITH_DEFAULT, "unknown", false, false), new ResourceRequirement(RESOURCE_NO_DEFAULT, false, false));
        assertPropertyAdded(context, RESOURCE_WITH_DEFAULT);
        assertPropertyAdded(context, RESOURCE_NO_DEFAULT);
    }
    
    private ExecutionContext populateContext(ResourceRequirement... requirements)
    {
        ExecutionContext context = new PulseExecutionContext();
        RecipeUtils.addResourceProperties(context, asList(requirements), resourceRepository);
        return context;
    }

    private void assertPropertyAdded(ExecutionContext context, String name)
    {
        assertEquals(propertyValue(name), context.getString(propertyName(name)));
    }

    private void assertPropertyNotAdded(ExecutionContext context, String name)
    {
        name = propertyName(name);
        assertNull("Property '" + name + "' should not have been added to the context", context.getString(name));
    }
}
