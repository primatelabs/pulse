package com.zutubi.tove.config;

import com.zutubi.i18n.Messages;
import com.zutubi.tove.annotations.Ordered;
import com.zutubi.tove.annotations.Reference;
import com.zutubi.tove.annotations.SymbolicName;
import com.zutubi.tove.config.api.AbstractConfiguration;
import com.zutubi.tove.config.api.AbstractNamedConfiguration;
import com.zutubi.tove.config.api.NamedConfiguration;
import com.zutubi.tove.type.*;
import com.zutubi.tove.type.record.MutableRecord;
import com.zutubi.tove.type.record.Record;
import com.zutubi.tove.type.record.TemplateRecord;
import com.zutubi.util.CollectionUtils;
import com.zutubi.util.Mapping;
import com.zutubi.validation.ValidationException;
import org.acegisecurity.AccessDeniedException;

import java.util.*;

import static com.zutubi.tove.type.record.PathUtils.getBaseName;
import static com.zutubi.tove.type.record.PathUtils.getPath;
import static com.zutubi.util.CollectionUtils.*;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class ConfigurationRefactoringManagerTest extends AbstractConfigurationSystemTestCase
{
    private static final Messages I18N = Messages.getInstance(ConfigurationRefactoringManager.class);

    private static final String SAMPLE_SCOPE = "sample";
    private static final String TEMPLATE_SCOPE = "template";
    private static final String NAME_ROOT = "root";

    private ConfigurationRefactoringManager configurationRefactoringManager;
    private String rootPath;
    private long rootHandle;

    protected void setUp() throws Exception
    {
        super.setUp();
        configurationRefactoringManager = new ConfigurationRefactoringManager();
        configurationRefactoringManager.setTypeRegistry(typeRegistry);
        configurationRefactoringManager.setConfigurationTemplateManager(configurationTemplateManager);
        configurationRefactoringManager.setConfigurationReferenceManager(configurationReferenceManager);
        configurationRefactoringManager.setConfigurationSecurityManager(configurationSecurityManager);
        configurationRefactoringManager.setRecordManager(recordManager);
        configurationRefactoringManager.setConfigurationHealthChecker(configurationHealthChecker);

        CompositeType typeA = typeRegistry.register(ConfigA.class);
        MapType mapA = new MapType(typeA, typeRegistry);

        MapType templatedMap = new TemplatedMapType(typeA, typeRegistry);

        configurationPersistenceManager.register(SAMPLE_SCOPE, mapA);
        configurationPersistenceManager.register(TEMPLATE_SCOPE, templatedMap);

        MutableRecord root = unstantiate(new ConfigA(NAME_ROOT));
        configurationTemplateManager.markAsTemplate(root);
        rootPath = configurationTemplateManager.insertRecord(TEMPLATE_SCOPE, root);
        rootHandle = configurationTemplateManager.getRecord(rootPath).getHandle();
        
        typeRegistry.register(ConfigAntCommand.class);
        typeRegistry.register(ConfigMavenCommand.class);
    }

    public void testCanCloneEmptyPath()
    {
        assertFalse(configurationRefactoringManager.canClone(""));
    }

    public void testCanCloneSingleElementPath()
    {
        assertFalse(configurationRefactoringManager.canClone(SAMPLE_SCOPE));
    }

    public void testCanCloneNonexistantPath()
    {
        assertFalse(configurationRefactoringManager.canClone("sample/fu"));
    }

    public void testCanCloneTemplateRoot()
    {
        assertFalse(configurationRefactoringManager.canClone(rootPath));
    }

    public void testCanCloneParentPathNotACollection()
    {
        configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        assertFalse(configurationRefactoringManager.canClone("sample/a/b"));
    }

    public void testCanCloneParentPathAList()
    {
        String aPath = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        String listPath = getPath(aPath, "blist");
        String clonePath = getPath(listPath, configurationTemplateManager.getRecord(listPath).keySet().iterator().next());
        assertFalse(configurationRefactoringManager.canClone(clonePath));
    }

    public void testCanClonePermanent()
    {
        ConfigA a = createAInstance("a");
        a.setPermanent(true);
        String aPath = configurationTemplateManager.insert(SAMPLE_SCOPE, a);
        assertFalse(configurationRefactoringManager.canClone(aPath));
    }

    public void testCanCloneTemplateItem()
    {
        configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        assertTrue(configurationRefactoringManager.canClone("sample/a"));
    }

    /**
     * CIB-2307, NPE generated by nested type cloning because base type of
     * map is used rather than actual type.
     *
     * @throws Exception on error.
     */
    public void testCanCloneCollectionItems() throws Exception
    {
        ConfigRecipe recipe = new ConfigRecipe("recipe");
        recipe.getCommands().put("ant", new ConfigAntCommand("ant", new ConfigEnvironment("key", "value")));

        MapType map = new MapType(typeRegistry.getType(ConfigRecipe.class), typeRegistry);
        configurationPersistenceManager.register("recipes", map);

        configurationTemplateManager.insert("recipes", recipe);

        assertTrue(configurationRefactoringManager.canClone("recipes/recipe/commands/ant"));
        configurationRefactoringManager.clone("recipes/recipe/commands/ant", "ant Clone");
    }

    public void testCanCloneItemBelowTopLevel()
    {
        configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        assertTrue(configurationRefactoringManager.canClone("sample/a/bmap/colby"));
    }

    public void testCloneEmptyPath()
    {
        illegalClonePathHelper("", "Invalid path '': no parent");
    }

    public void testCloneSingleElementPath()
    {
        illegalClonePathHelper(SAMPLE_SCOPE, "Invalid path 'sample': no parent");
    }

    public void testCloneInvalidParentPath()
    {
        illegalClonePathHelper("huh/instance", "Invalid path 'huh': references non-existant root scope 'huh'");
    }

    public void testCloneInvalidPath()
    {
        illegalClonePathHelper("sample/nosuchinstance", "Invalid path 'sample/nosuchinstance': path does not exist");
    }

    public void testCloneParentPathNotACollection()
    {
        configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        illegalClonePathHelper("sample/a/b", "Invalid parent path 'sample/a': only elements of a map collection may be cloned (parent has type com.zutubi.tove.type.CompositeType)");
    }

    public void testCloneParentPathAList()
    {
        String aPath = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        String listPath = getPath(aPath, "blist");
        String clonePath = getPath(listPath, configurationTemplateManager.getRecord(listPath).keySet().iterator().next());
        illegalClonePathHelper(clonePath, "Invalid parent path '" + listPath + "': only elements of a map collection may be cloned (parent has type com.zutubi.tove.type.ListType)");
    }

    public void testCloneTemplateRoot()
    {
        illegalClonePathHelper(rootPath, "Invalid path '" + rootPath + "': cannot clone root of a template hierarchy");
    }

    public void testClonePermanent() throws TypeException
    {
        ConfigA a = createAInstance("a");
        a.setPermanent(true);
        String aPath = configurationTemplateManager.insert(SAMPLE_SCOPE, a);
        invalidCloneNameHelper(aPath, "clone", "Invalid path '" + aPath + "': refers to a permanent record");
    }

    private void illegalClonePathHelper(String path, String expectedError)
    {
        try
        {
            configurationRefactoringManager.clone(path, "clone");
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertEquals(expectedError, e.getMessage());
        }
        catch (ToveRuntimeException e)
        {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalArgumentException);
            assertEquals(expectedError, cause.getMessage());
        }
    }

    public void testCloneEmptyCloneName() throws TypeException
    {
        invalidCloneNameHelper(insertTemplateA(rootPath, "a", false), "", "Invalid empty clone key");
    }

    public void testCloneDuplicateCloneName() throws TypeException
    {
        insertTemplateA(rootPath, "existing", false);
        invalidCloneNameHelper(insertTemplateA(rootPath, "a", false), "existing", "name is already in use, please select another name");
    }

    public void testCloneCloneNameInAncestor() throws TypeException
    {
        addB(rootPath, "parentB");
        String childPath = insertTemplateA(rootPath, "child", false);
        configurationTemplateManager.delete(getPath(childPath, "bmap", "parentB"));
        String childBPath = addB(childPath, "childB");
        invalidCloneNameHelper(childBPath, "parentB", "name is already in use in ancestor \"root\", please select another name");
    }

    public void testCloneCloneNameInDescendant() throws TypeException
    {
        String parentBPath = addB(rootPath, "parentB");
        String childPath = insertTemplateA(rootPath, "child", false);
        addB(childPath, "childB");
        invalidCloneNameHelper(parentBPath, "childB", "name is already in use in descendant \"child\", please select another name");
    }

    public void testSimpleClone()
    {
        String path = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        String clonePath = configurationRefactoringManager.clone(path, "clone of a");
        assertEquals("sample/clone of a", clonePath);
        assertClone(configurationTemplateManager.getInstance(clonePath, ConfigA.class), "a");
    }

    public void testSimpleCloneInTemplateScope() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a"), false);
        String clonePath = configurationRefactoringManager.clone(path, "clone of a");
        assertEquals("template/clone of a", clonePath);
        ConfigA clone = configurationTemplateManager.getInstance(clonePath, ConfigA.class);
        assertTrue(clone.isConcrete());
        assertEquals(rootHandle, configurationTemplateManager.getTemplateParentRecord(clonePath).getHandle());
        assertClone(clone, "a");
    }

    public void testCloneOfTemplate() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a"), true);
        String clonePath = configurationRefactoringManager.clone(path, "clone of a");
        assertEquals("template/clone of a", clonePath);
        ConfigA clone = configurationTemplateManager.getInstance(clonePath, ConfigA.class);
        assertFalse(clone.isConcrete());
        assertEquals(rootHandle, configurationTemplateManager.getTemplateParentRecord(clonePath).getHandle());
        assertClone(clone, "a");
    }

    public void testCloneBelowTopLevel()
    {
        String path = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        ConfigB colby = configurationTemplateManager.getInstance(path, ConfigA.class).getBmap().values().iterator().next();
        String clonePath = configurationRefactoringManager.clone(colby.getConfigurationPath(), "clone");
        assertEquals("sample/a/bmap/clone", clonePath);
        ConfigB clone = configurationTemplateManager.getInstance(clonePath, ConfigB.class);
        
        assertNotSame(colby, clone);
        assertEquals("clone", clone.getName());
        assertEquals(1, clone.getY());
    }

    public void testCloneCreatesSkeletons() throws TypeException
    {
        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        String child1Path = insertTemplateAInstance(parentPath, createAInstance("child1"), false);
        String child2Path = insertTemplateAInstance(parentPath, createAInstance("child2"), false);

        assertNotNull(recordManager.select(getPath(child1Path, "bmap")));
        Listener listener = registerListener();

        configurationRefactoringManager.clone(getPath(parentPath, "bmap", "colby"), "clone");
        // Data assertions on teardown will detect any missing skeletons.

        listener.assertEvents(
                new InsertEventSpec(getPath(child1Path, "bmap", "clone"), false),
                new PostInsertEventSpec(getPath(child1Path, "bmap", "clone"), false),
                new InsertEventSpec(getPath(child2Path, "bmap", "clone"), false),
                new PostInsertEventSpec(getPath(child2Path, "bmap", "clone"), false)
        );
    }

    public void testMultipleClone()
    {
        configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("1"));
        configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("2"));

        configurationRefactoringManager.clone(SAMPLE_SCOPE, asMap(asPair("1", "clone of 1"), asPair("2", "clone of 2")));

        assertClone(configurationTemplateManager.getInstance(getPath(SAMPLE_SCOPE, "clone of 1"), ConfigA.class), "1");
        assertClone(configurationTemplateManager.getInstance(getPath(SAMPLE_SCOPE, "clone of 2"), ConfigA.class), "2");
    }

    public void testCloneWithInternalReference()
    {
        cloneWithInternalReferenceHelper(configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a")));
    }

    public void testCloneWithInternalReferenceInTemplatedScope() throws TypeException
    {
        cloneWithInternalReferenceHelper(insertTemplateAInstance(rootPath, createAInstance("a"), false));
    }

    private void cloneWithInternalReferenceHelper(String path)
    {
        ConfigA instance = configurationTemplateManager.getInstance(path, ConfigA.class);
        instance.setRefToRef(instance.getRef());
        instance.getListOfRefs().add(instance.getRef());
        configurationTemplateManager.save(instance);

        String clonePath = configurationRefactoringManager.clone(path, "clone");
        instance = configurationTemplateManager.getInstance(path, ConfigA.class);
        assertNotNull(instance.getRef());
        assertSame(instance.getRef(), instance.getRefToRef());
        assertEquals(1, instance.getListOfRefs().size());
        assertSame(instance.getRef(), instance.getListOfRefs().get(0));

        ConfigA cloneInstance = configurationTemplateManager.getInstance(clonePath, ConfigA.class);
        assertNotSame(instance.getRef(), cloneInstance.getRef());
        assertNotSame(instance.getRefToRef(), cloneInstance.getRefToRef());
        assertSame(cloneInstance.getRef(), cloneInstance.getRefToRef());
        assertEquals(1, cloneInstance.getListOfRefs().size());
        assertSame(cloneInstance.getRef(), cloneInstance.getListOfRefs().get(0));
    }

    public void testMultipleCloneWithReferenceBetween()
    {
        String path1 = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("1"));
        ConfigA instance1 = configurationTemplateManager.getInstance(path1, ConfigA.class);
        ConfigA instance2 = createAInstance("2");
        instance2.setRefToRef(instance1.getRef());
        String path2 = configurationTemplateManager.insert(SAMPLE_SCOPE, instance2);

        configurationRefactoringManager.clone(SAMPLE_SCOPE, asMap(asPair("1", "clone of 1"), asPair("2", "clone of 2")));

        instance1 = configurationTemplateManager.getInstance(path1, ConfigA.class);
        instance2 = configurationTemplateManager.getInstance(path2, ConfigA.class);
        ConfigA clone1 = configurationTemplateManager.getInstance(getPath(SAMPLE_SCOPE, "clone of 1"), ConfigA.class);
        ConfigA clone2 = configurationTemplateManager.getInstance(getPath(SAMPLE_SCOPE, "clone of 2"), ConfigA.class);

        assertClone(clone1, "1");
        assertClone(clone2, "2");
        assertSame(instance1.getRef(), instance2.getRefToRef());
        assertNotSame(instance1.getRef(), clone2.getRefToRef());
        assertSame(clone1.getRef(), clone2.getRefToRef());
    }

    public void testMultipleCloneOfTemplateHierarchy() throws TypeException
    {
        templateHierarchyHelper(asMap(asPair("parent", "clone of parent"), asPair("child", "clone of child")));
    }

    public void testMultipleCloneOfTemplateHierarchyChildFirst() throws TypeException
    {
        templateHierarchyHelper(asMap(asPair("child", "clone of child"), asPair("parent", "clone of parent")));
    }

    public void testMultipleCloneWithReferenceBetweenInInheritedMapItem() throws TypeException
    {
        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        configurationTemplateManager.getInstance(parentPath, ConfigA.class);
        String child1Path = insertTemplateAInstance(parentPath, createAInstance("child1"), false);
        String child2Path = insertTemplateAInstance(parentPath, createAInstance("child2"), false);

        ConfigA child1Instance = configurationTemplateManager.getInstance(child1Path, ConfigA.class);
        ConfigA child2Instance = configurationTemplateManager.getInstance(child2Path, ConfigA.class);

        // Set up a reference from map item in child1 to child2
        ConfigB mapItem = child1Instance.getBmap().get("colby");
        mapItem.setRefToA(child2Instance);
        configurationTemplateManager.save(mapItem);

        configurationRefactoringManager.clone(TEMPLATE_SCOPE, asMap(asPair("child1", "clone1"), asPair("child2", "clone2")));

        child1Instance = configurationTemplateManager.getInstance(child1Path, ConfigA.class);
        child2Instance = configurationTemplateManager.getInstance(child2Path, ConfigA.class);
        assertSame(child2Instance, child1Instance.getBmap().get("colby").getRefToA());

        ConfigA clone1Instance = configurationTemplateManager.getInstance(getPath(TEMPLATE_SCOPE, "clone1"), ConfigA.class);
        ConfigA clone2Instance = configurationTemplateManager.getInstance(getPath(TEMPLATE_SCOPE, "clone2"), ConfigA.class);
        assertNotSame(child1Instance.getBmap().get("colby").getRefToA(), clone1Instance.getBmap().get("colby").getRefToA());
        assertSame(clone2Instance, clone1Instance.getBmap().get("colby").getRefToA());
    }

    public void testCloneWithInheritedItem() throws TypeException
    {
        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        insertTemplateAInstance(parentPath, new ConfigA("child"), false);

        String clonePath = configurationRefactoringManager.clone("template/child/bmap/colby", "clone of colby");
        ConfigB clone = configurationTemplateManager.getInstance(clonePath, ConfigB.class);
        assertEquals(1, clone.getY());
    }

    public void testCloneWithOveriddenItem() throws TypeException
    {
        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        ConfigA childInstance = createAInstance("child");
        childInstance.getBmap().get("colby").setY(111222333);
        insertTemplateAInstance(parentPath, childInstance, false);

        String clonePath = configurationRefactoringManager.clone("template/child/bmap/colby", "clone of colby");
        ConfigB clone = configurationTemplateManager.getInstance(clonePath, ConfigB.class);
        assertEquals(111222333, clone.getY());
    }

    public void testClonePreservesImplicitCollectionOrders() throws TypeException
    {
        final List<String> LIST_NAMES = asList("zingyl", "angryl", "snowyl", "jauntyl", "ickyl", "yuckyl");
        final List<String> MAP_NAMES = asList("zingym", "angrym", "snowym", "jauntym", "ickym", "yuckym");

        // We can't just add collection items to this list and unstantiate as
        // then the collection records will have explicit orders set.
        ConfigA instance = createAInstance("source");
        String sourcePath = configurationTemplateManager.insert(SAMPLE_SCOPE, instance);

        String listPath = getPath(sourcePath, "orderedBlist");
        for (String name: LIST_NAMES)
        {
            configurationTemplateManager.insert(listPath, new ConfigB(name));
        }

        String mapPath = getPath(sourcePath, "orderedBmap");
        for (String name: MAP_NAMES)
        {
            configurationTemplateManager.insert(mapPath, new ConfigB(name));
        }

        String clonePath = configurationRefactoringManager.clone(sourcePath, "clone");

        instance = configurationTemplateManager.getInstance(clonePath, ConfigA.class);
        Mapping<ConfigB, String> configToNameFn = new Mapping<ConfigB, String>()
        {
            public String map(ConfigB configB)
            {
                return configB.getName();
            }
        };
        
        assertEquals(LIST_NAMES, map(instance.getOrderedBlist(), configToNameFn));
        assertEquals(MAP_NAMES, map(instance.getOrderedBmap().values(), configToNameFn));
    }

    private void templateHierarchyHelper(Map<String, String> originalKeyToCloneKey) throws TypeException
    {
        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        insertTemplateAInstance(parentPath, createAInstance("child"), false);

        configurationRefactoringManager.clone(TEMPLATE_SCOPE, originalKeyToCloneKey);

        String parentClonePath = getPath(TEMPLATE_SCOPE, "clone of parent");
        ConfigA parentClone = configurationTemplateManager.getInstance(parentClonePath, ConfigA.class);
        assertFalse(parentClone.isConcrete());
        assertClone(parentClone, "parent");
        assertEquals(rootHandle, configurationTemplateManager.getTemplateParentRecord(parentClonePath).getHandle());

        String childClonePath = getPath(TEMPLATE_SCOPE, "clone of child");
        ConfigA childClone = configurationTemplateManager.getInstance(childClonePath, ConfigA.class);
        assertTrue(childClone.isConcrete());
        assertEquals(parentClone.getHandle(), configurationTemplateManager.getTemplateParentRecord(childClonePath).getHandle());
    }

    public void testCanExtractParentTemplateInvalidParentPath()
    {
        assertFalse(configurationRefactoringManager.canExtractParentTemplate("nosuchscope/foo"));
    }

    public void testCanExtractParentTemplateParentPathNotAMap()
    {
        assertFalse(configurationRefactoringManager.canExtractParentTemplate(rootPath + "/foo"));
    }

    public void testCanExtractParentTemplateParentPathNotATemplatedScope()
    {
        assertFalse(configurationRefactoringManager.canExtractParentTemplate(SAMPLE_SCOPE + "/foo"));
    }

    public void testCanExtractParentTemplateInvalidItem()
    {
        assertFalse(configurationRefactoringManager.canExtractParentTemplate(TEMPLATE_SCOPE + "/nope"));
    }

    public void testCanExtractParentTemplateRootTemplate()
    {
        assertFalse(configurationRefactoringManager.canExtractParentTemplate(rootPath));
    }

    public void testCanExtractParentTemplate() throws TypeException
    {
        String aPath = insertTemplateAInstance(rootPath, createAInstance("a"), false);
        assertTrue(configurationRefactoringManager.canExtractParentTemplate(aPath));
    }

    public void testExtractParentTemplateInvalidParentPath()
    {
        extractParentTemplateErrorHelper("nosuchscope", Collections.<String>emptyList(), "foo", "Invalid parent path 'nosuchscope': does not refer to a templated collection");
    }

    public void testExtractParentTemplateParentPathNotAMap()
    {
        extractParentTemplateErrorHelper(rootPath, Collections.<String>emptyList(), "foo", "Invalid parent path '" + rootPath + "': does not refer to a templated collection");
    }

    public void testExtractParentTemplateParentPathNotATemplatedScope()
    {
        extractParentTemplateErrorHelper(SAMPLE_SCOPE, Collections.<String>emptyList(), "foo", "Invalid parent path '" + SAMPLE_SCOPE + "': does not refer to a templated collection");
    }

    public void testExtractParentTemplateInvalidChildKey()
    {
        extractParentTemplateErrorHelper(TEMPLATE_SCOPE, asList("nope"), "foo", "Invalid child key 'nope': does not refer to an element of the templated collection");
    }

    public void testExtractParentTemplateRootTemplate()
    {
        extractParentTemplateErrorHelper(TEMPLATE_SCOPE, asList(getBaseName(rootPath)), "foo", "Invalid child key 'root': cannot extract parent from the root of a template hierarchy");
    }

    public void testExtractParentTemplateChildKeysNotSiblings() throws TypeException
    {
        String parentPath = insertTemplateA(rootPath, "parent", true);
        insertTemplateA(parentPath, "child", false);
        extractParentTemplateErrorHelper(TEMPLATE_SCOPE, asList("parent", "child"), "foo", "Invalid child keys: all child keys must refer to siblings in the template hierarchy");
    }

    public void testExtractParentTemplateParentNameEmpty() throws TypeException
    {
        insertTemplateA(rootPath, "a", true);
        extractParentTemplateErrorHelper(TEMPLATE_SCOPE, asList("a"), "", "Parent template name is required");
    }

    public void testExtractParentTemplateParentNameNotUnique() throws TypeException
    {
        insertTemplateA(rootPath, "a", true);
        extractParentTemplateErrorHelper(TEMPLATE_SCOPE, asList("a"), "a", "name is already in use, please select another name");
    }

    private void extractParentTemplateErrorHelper(String parentPath, List<String> childKeys, String parentTemplateName, String expectedError)
    {
        try
        {
            configurationRefactoringManager.extractParentTemplate(parentPath, childKeys, parentTemplateName);
            fail();
        }
        catch (Exception e)
        {
            assertEquals(expectedError, e.getMessage());
        }
    }

    public void testExtractParentTemplateSimple() throws TypeException
    {
        String aPath = insertTemplateAInstance(rootPath, createAInstance("a"), false);
        String extractedPath = configurationRefactoringManager.extractParentTemplate(TEMPLATE_SCOPE, asList("a"), "extracted");

        // Ensure the instances both look as expected
        ConfigA extractedInstance = configurationTemplateManager.getInstance(extractedPath, ConfigA.class);
        assertAInstance(extractedInstance, "extracted");
        assertFalse(extractedInstance.isConcrete());

        ConfigA aInstance = configurationTemplateManager.getInstance(aPath, ConfigA.class);
        assertAInstance(aInstance, "a");
        assertTrue(aInstance.isConcrete());

        // Assert the expected shape of the hierarchy
        TemplateNode node = configurationTemplateManager.getTemplateNode(aPath);
        assertNotNull(node);
        assertEquals("root/extracted/a", node.getTemplatePath());

        node = configurationTemplateManager.getTemplateNode(extractedPath);
        assertNotNull(node);
        assertEquals("root/extracted", node.getTemplatePath());

        // Now assert that the fields have really been pulled up.
        assertAllValuesExtracted("a");
    }

    public void testExtractParentTemplateIdenticalSiblings() throws TypeException
    {
        String path1 = insertTemplateAInstance(rootPath, createAInstance("1"), false);
        String path2 = insertTemplateAInstance(rootPath, createAInstance("2"), false);
        String extractedPath = configurationRefactoringManager.extractParentTemplate(TEMPLATE_SCOPE, asList("1", "2"), "extracted");

        // Ensure parent has all fields pulled up
        ConfigA extractedInstance = configurationTemplateManager.getInstance(extractedPath, ConfigA.class);
        assertAInstance(extractedInstance, "extracted");
        assertFalse(extractedInstance.isConcrete());

        // And both children look good too
        ConfigA instance1 = configurationTemplateManager.getInstance(path1, ConfigA.class);
        assertAInstance(instance1, "1");
        assertTrue(instance1.isConcrete());

        ConfigA instance2 = configurationTemplateManager.getInstance(path2, ConfigA.class);
        assertAInstance(instance2, "2");
        assertTrue(instance2.isConcrete());

        // Assert the expected shape of the hierarchy
        TemplateNode node = configurationTemplateManager.getTemplateNode(extractedPath);
        assertNotNull(node);
        assertEquals("root/extracted", node.getTemplatePath());

        assertEquals(2, node.getChildren().size());
        List<String> children = map(node.getChildren(), new Mapping<TemplateNode, String>()
        {
            public String map(TemplateNode templateNode)
            {
                return templateNode.getId();
            }
        });
        Collections.sort(children);
        assertEquals("1", children.get(0));
        assertEquals("2", children.get(1));

        // Now assert that the fields have really been pulled up.
        assertAllValuesExtracted("1");
        assertAllValuesExtracted("2");
    }

    public void testExtractParentTemplateReferenceToExtracted() throws TypeException
    {
        externalReferenceHelper("referee");
    }

    public void testExtractParentTemplateReferenceFromExtracted() throws TypeException
    {
        externalReferenceHelper("referer");
    }

    private void externalReferenceHelper(String extractKey) throws TypeException
    {
        String refereePath = insertTemplateAInstance(rootPath, createAInstance("referee"), false);
        ConfigA referee = configurationTemplateManager.getInstance(refereePath, ConfigA.class);
        ConfigA referer = new ConfigA("referer");
        referer.setRefToRef(referee.getRef());
        referer.getListOfRefs().add(referee.getRef());
        String refererPath = insertTemplateAInstance(rootPath, referer, false);

        configurationRefactoringManager.extractParentTemplate(TEMPLATE_SCOPE, Arrays.asList(extractKey), "extracted");

        referee = configurationTemplateManager.getInstance(refereePath, ConfigA.class);
        referer = configurationTemplateManager.getInstance(refererPath, ConfigA.class);
        assertSame(referee.getRef(), referer.getRefToRef());
        assertSame(referee.getRef(), referer.getListOfRefs().get(0));
    }

    public void testExtractParentTemplateInternalReference() throws TypeException
    {
        final String EXTRACTED_NAME = "extracted";

        String aPath = insertAInstanceWithInternalReference("a");
        String extractedPath = configurationRefactoringManager.extractParentTemplate(TEMPLATE_SCOPE, Arrays.asList("a"), EXTRACTED_NAME);

        assertExtractedInternalReference(aPath, extractedPath);
        assertExtractedInternalReference(extractedPath, extractedPath);
    }

    public void testExtractParentTemplateInternalReferenceList() throws TypeException
    {
        String aPath = insertAInstanceWithInternalReferenceList("a");
        String extractedPath = configurationRefactoringManager.extractParentTemplate(TEMPLATE_SCOPE, Arrays.asList("a"), "extracted");

        assertExtractedInternalReferenceList(aPath, extractedPath);
        assertExtractedInternalReferenceList(extractedPath, extractedPath);
    }

    public void testExtractParentTemplateCommonExternalReferences() throws TypeException
    {
        String refereePath = insertTemplateAInstance(rootPath, createAInstance("referee"), false);
        String path1 = insertTemplateAInstance(rootPath, createAInstance("one"), false);
        String path2 = insertTemplateAInstance(rootPath, createAInstance("two"), false);

        ConfigA referee = configurationTemplateManager.getInstance(refereePath, ConfigA.class);
        ConfigA a1 = configurationTemplateManager.getInstance(path1, ConfigA.class);
        a1.setRefToRef(referee.getRef());
        configurationTemplateManager.save(a1);

        ConfigA a2 = configurationTemplateManager.getInstance(path2, ConfigA.class);
        a2.setRefToRef(referee.getRef());
        configurationTemplateManager.save(a2);

        String extractedPath = configurationRefactoringManager.extractParentTemplate(TEMPLATE_SCOPE, Arrays.asList("one", "two"), "extracted");

        referee = configurationTemplateManager.getInstance(refereePath, ConfigA.class);
        a1 = configurationTemplateManager.getInstance(path1, ConfigA.class);
        a2 = configurationTemplateManager.getInstance(path2, ConfigA.class);
        ConfigA extracted = configurationTemplateManager.getInstance(extractedPath, ConfigA.class);
        
        assertSame(referee.getRef(), a1.getRefToRef());
        assertSame(referee.getRef(), a2.getRefToRef());
        assertSame(referee.getRef(), extracted.getRefToRef());
    }

    public void testExtractParentTemplateCommonInternalReferences() throws TypeException
    {
        String path1 = insertAInstanceWithInternalReference("one");
        String path2 = insertAInstanceWithInternalReference("two");
        String extractedPath = configurationRefactoringManager.extractParentTemplate(TEMPLATE_SCOPE, Arrays.asList("one", "two"), "extracted");

        assertExtractedInternalReference(path1, extractedPath);
        assertExtractedInternalReference(path2, extractedPath);
        assertExtractedInternalReference(extractedPath, extractedPath);
    }

    public void testExtractParentTemplateCommonInternalReferenceLists() throws TypeException
    {
        String path1 = insertAInstanceWithInternalReferenceList("one");
        String path2 = insertAInstanceWithInternalReferenceList("two");
        String extractedPath = configurationRefactoringManager.extractParentTemplate(TEMPLATE_SCOPE, Arrays.asList("one", "two"), "extracted");

        assertExtractedInternalReferenceList(path1, extractedPath);
        assertExtractedInternalReferenceList(path2, extractedPath);
        assertExtractedInternalReferenceList(extractedPath, extractedPath);
    }

    private String insertAInstanceWithInternalReference(String name) throws TypeException
    {
        ConfigA configA = createAInstance(name);
        String aPath = insertTemplateAInstance(rootPath, configA, false);

        configA = configurationTemplateManager.getInstance(aPath, ConfigA.class);
        configA.setRefToRef(configA.getRef());
        return configurationTemplateManager.save(configA);
    }

    private void assertExtractedInternalReference(String originalPath, String extractedPath)
    {
        ConfigA configA = configurationTemplateManager.getInstance(originalPath, ConfigA.class);
        assertSame(configA.getRef(), configA.getRefToRef());
        // Check that the value is owned at the right level (scrubbing was
        // applied correctly).
        assertEquals(getBaseName(extractedPath), ((TemplateRecord) configurationTemplateManager.getRecord(extractedPath)).getOwner("refToRef"));
    }

    private String insertAInstanceWithInternalReferenceList(String name) throws TypeException
    {
        ConfigA configA = createAInstance(name);
        String aPath = insertTemplateAInstance(rootPath, configA, false);

        configA = configurationTemplateManager.getInstance(aPath, ConfigA.class);
        configA.getListOfRefs().add(configA.getRef());
        configurationTemplateManager.save(configA);
        return aPath;
    }

    private void assertExtractedInternalReferenceList(String originalPath, String extractedPath)
    {
        ConfigA configA;
        configA = configurationTemplateManager.getInstance(originalPath, ConfigA.class);
        assertSame(configA.getRef(), configA.getListOfRefs().get(0));
        assertEquals(getBaseName(extractedPath), ((TemplateRecord) configurationTemplateManager.getRecord(extractedPath)).getOwner("listOfRefs"));
    }

    public void testExtractParentTemplateNullReference() throws TypeException
    {
        ConfigA a = createAInstance("a");
        a.getBmap().put("b", new ConfigB("b"));
        insertTemplateAInstance(rootPath, a, false);
        String extractedPath = configurationRefactoringManager.extractParentTemplate(TEMPLATE_SCOPE, Arrays.asList("a"), "extracted");

        ConfigA extracted = configurationTemplateManager.getInstance(extractedPath, ConfigA.class);
        assertNull(extracted.getBmap().get("b").getRefToRef());
    }

    private void assertAllValuesExtracted(String key)
    {
        String path = getPath(TEMPLATE_SCOPE, key);
        TemplateRecord record = (TemplateRecord) configurationTemplateManager.getRecord(path);
        assertEquals(key, record.getOwner("name"));
        assertEquals("10", record.get("x"));
        assertEquals("extracted", record.getOwner("x"));

        TemplateRecord bRecord = (TemplateRecord) record.get("b");
        assertEquals("44", bRecord.get("y"));
        assertEquals("extracted", bRecord.getOwner("y"));

        TemplateRecord blistRecord = (TemplateRecord) record.get("blist");
        TemplateRecord lisbyRecord = (TemplateRecord) blistRecord.get(blistRecord.nestedKeySet().iterator().next());
        assertEquals("lisby", lisbyRecord.get("name"));

        TemplateRecord bmapRecord = (TemplateRecord) record.get("bmap");
        TemplateRecord colbyRecord = (TemplateRecord) bmapRecord.get("colby");
        assertEquals("1", colbyRecord.get("y"));
        assertEquals("extracted", colbyRecord.getOwner("y"));
    }

    public void testGetPullUpAncestorsInvalidPath()
    {
        assertEquals(Collections.<String>emptyList(), configurationRefactoringManager.getPullUpAncestors("invalid/path/here"));
    }

    public void testGetPullUpAncestorsTemplateCollectionItem() throws TypeException
    {
        String path = insertTemplateA(rootPath, "a", false);
        assertEquals(Collections.<String>emptyList(), configurationRefactoringManager.getPullUpAncestors(path));
    }

    public void testGetPullUpAncenstorsNotTemplate()
    {
        String path = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        assertEquals(Collections.<String>emptyList(), configurationRefactoringManager.getPullUpAncestors(path));
    }

    public void testGetPullUpAncenstorsNoAncestor() throws TypeException
    {
        assertEquals(Collections.<String>emptyList(), configurationRefactoringManager.getPullUpAncestors(getPath(rootPath, "b")));
    }

    public void testGetPullUpAncestorsNoValidAncestor() throws TypeException
    {
        String templateParentPath = insertTemplateAInstance(rootPath, createAInstance("a1"), true);
        String path = insertTemplateAInstance(templateParentPath, createAInstance("a2"), false);
        assertEquals(Collections.<String>emptyList(), configurationRefactoringManager.getPullUpAncestors(getPath(path, "b")));
    }

    public void testGetPullUpAncestors() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a1"), false);
        assertEquals(asList(NAME_ROOT), configurationRefactoringManager.getPullUpAncestors(getPath(path, "b")));
    }

    public void testCanPullUpAnyInvalidPath()
    {
        assertFalse(configurationRefactoringManager.canPullUp("invalid/path/here"));
    }

    public void testCanPullUpAnyTemplateCollectionItem() throws TypeException
    {
        String path = insertTemplateA(rootPath, "a", false);
        assertFalse(configurationRefactoringManager.canPullUp(path));
    }

    public void testCanPullUpAnyNotTemplate()
    {
        String path = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        assertFalse(configurationRefactoringManager.canPullUp(path));
    }

    public void testCanPullUpAnyNoAncestor() throws TypeException
    {
        assertFalse(configurationRefactoringManager.canPullUp(getPath(rootPath, "b")));
    }

    public void testCanPullUpAnyNoValidAncestor() throws TypeException
    {
        String templateParentPath = insertTemplateAInstance(rootPath, createAInstance("a1"), true);
        String path = insertTemplateAInstance(templateParentPath, createAInstance("a2"), false);
        assertFalse(configurationRefactoringManager.canPullUp(getPath(path, "b")));
    }

    public void testCanPullUpAnyPermanent() throws TypeException
    {
        ConfigA configA = createAInstance("a1");
        configA.getB().setPermanent(true);
        String path = insertTemplateAInstance(rootPath, configA, false);
        assertFalse(configurationRefactoringManager.canPullUp(getPath(path, "b")));
    }

    public void testCanPullUpAny() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a1"), false);
        assertTrue(configurationRefactoringManager.canPullUp(getPath(path, "b")));
    }
    
    public void testCanPullUpInvalidPath()
    {
        assertFalse(configurationRefactoringManager.canPullUp("invalid/path/here", "anything"));
    }

    public void testCanPullUpTemplateCollectionItem() throws TypeException
    {
        String path = insertTemplateA(rootPath, "a", false);
        assertFalse(configurationRefactoringManager.canPullUp(path, NAME_ROOT));
    }

    public void testCanPullUpNonTemplatedItem() throws TypeException
    {
        String path = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        assertFalse(configurationRefactoringManager.canPullUp(path, NAME_ROOT));
    }

    public void testCanPullUpNoAncestor() throws TypeException
    {
        assertFalse(configurationRefactoringManager.canPullUp(getPath(rootPath, "b"), NAME_ROOT));
    }

    public void testCanPullUpBadAncestor() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a1"), false);
        insertTemplateAInstance(rootPath, createAInstance("a2"), false);
        assertFalse(configurationRefactoringManager.canPullUp(getPath(path, "b"), "a2"));
    }

    public void testCanPullUpAncestorDefinesPath() throws TypeException
    {
        String templateParentPath = insertTemplateAInstance(rootPath, createAInstance("a1"), true);
        String path = insertTemplateAInstance(templateParentPath, createAInstance("a2"), false);
        assertFalse(configurationRefactoringManager.canPullUp(getPath(path, "b"), NAME_ROOT));
    }

    public void testCanPullUpAncestorDoesNotDefineParentPath() throws TypeException
    {
        ConfigA configA = createAInstance("child");
        configA.getBmap().get("colby").getCmap().put("colcy", new ConfigC("colcy"));
        String path = insertTemplateAInstance(rootPath, configA, false);
        assertFalse(configurationRefactoringManager.canPullUp(getPath(path, "bmap", "colby", "cmap", "colcy"), NAME_ROOT));
    }

    public void testCanPullUpSiblingDefinesPath() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a1"), false);
        insertTemplateAInstance(rootPath, createAInstance("a2"), false);
        assertFalse(configurationRefactoringManager.canPullUp(getPath(path, "b"), NAME_ROOT));
    }

    public void testCanPullUpSimple() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a1"), false);
        assertFalse(configurationRefactoringManager.canPullUp(getPath(path, "b", "y"), NAME_ROOT));
    }

    public void testCanPullUpInternalReference() throws TypeException
    {
        final String NAME_PARENT = "parent";

        String templateParentPath = insertTemplateAInstance(rootPath, new ConfigA(NAME_PARENT), true);
        String childPath = insertTemplateAInstance(templateParentPath, createAInstance("child"), false);

        ConfigA child = configurationTemplateManager.getInstance(childPath, ConfigA.class);
        child.getB().setRefToRef(child.getRef());
        configurationTemplateManager.save(child);

        String pullPath = getPath(childPath, "b");
        assertFalse(configurationRefactoringManager.canPullUp(pullPath, NAME_PARENT));
    }

    public void testCanPullUpComposite() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a1"), false);
        assertTrue(configurationRefactoringManager.canPullUp(getPath(path, "b"), NAME_ROOT));
    }

    public void testCanPullUpCollectionItem() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a1"), false);
        assertTrue(configurationRefactoringManager.canPullUp(getPath(path, "bmap", "colby"), NAME_ROOT));
    }

    public void testCanPullUpInternalReferenceDefinedInAncestor() throws TypeException
    {
        final String NAME_PARENT = "parent";

        ConfigA parent = new ConfigA(NAME_PARENT);
        parent.setRef(new Referee("ee"));
        String templateParentPath = insertTemplateAInstance(rootPath, parent, true);

        parent = configurationTemplateManager.getInstance(templateParentPath, ConfigA.class);
        ConfigA child = new ConfigA("child");
        ConfigB childB = new ConfigB("b");
        childB.setRefToRef(parent.getRef());
        child.setB(childB);
        String childPath = insertTemplateAInstance(templateParentPath, child, false);

        String pullPath = getPath(childPath, "b");
        assertTrue(configurationRefactoringManager.canPullUp(pullPath, NAME_PARENT));
    }

    public void testPullUpUnableToWriteToAncestor() throws TypeException
    {
        final String ERROR_MESSAGE = "No such luck, bozo";

        ConfigurationSecurityManager securityManager = mock(ConfigurationSecurityManager.class);
        doThrow(new AccessDeniedException(ERROR_MESSAGE)).when(securityManager).ensurePermission(startsWith(rootPath), anyString());
        configurationRefactoringManager.setConfigurationSecurityManager(securityManager);

        String path = insertTemplateAInstance(rootPath, createAInstance("a1"), false);
        try
        {
            configurationRefactoringManager.pullUp(getPath(path, "b"), NAME_ROOT);
            fail("Should not be able to pull up with no permission");
        }
        catch (Exception e)
        {
            assertThat(e.getMessage(), containsString(ERROR_MESSAGE));
        }
    }

    public void testPullUpSiblingDefinesPath() throws TypeException
    {
        // a1
        //   b <-- pulled up
        // a2
        //   b
        String path = insertTemplateAInstance(rootPath, createAInstance("a1"), false);
        insertTemplateAInstance(rootPath, createAInstance("a2"), false);
        try
        {
            configurationRefactoringManager.pullUp(getPath(path, "b"), NAME_ROOT);
            fail("Should not be able to pull up when sibling defines path");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("Unable to pull up path 'template/a1/b': Ancestor 'root' already has descendants [template/a2/b] that define this path", e.getMessage());
        }
    }

    public void testPullUpInternalReference() throws TypeException
    {
        final String NAME_PARENT = "parent";

        // child
        //   ref: defines ee
        //   b <-- pulled up
        //     refToRef: refers to ee
        String templateParentPath = insertTemplateAInstance(rootPath, new ConfigA(NAME_PARENT), true);
        String childPath = insertTemplateAInstance(templateParentPath, createAInstance("child"), false);

        ConfigA child = configurationTemplateManager.getInstance(childPath, ConfigA.class);
        child.getB().setRefToRef(child.getRef());
        configurationTemplateManager.save(child);

        try
        {
            String pullPath = getPath(childPath, "b");
            configurationRefactoringManager.pullUp(pullPath, NAME_PARENT);
            fail("Should not be able to pull up when path contains reference not defined at ancestor level");
        }
        catch (IllegalArgumentException e)
        {
            assertEquals("Unable to pull up path 'template/child/b': Path contains reference to 'template/child/ref' which does not exist in ancestor 'parent'", e.getMessage());
        }
    }

    public void testPullUpComposite() throws TypeException
    {
        final String NAME_PARENT = "parent";

        // child
        //   b <-- pulled up
        String templateParentPath = insertTemplateAInstance(rootPath, new ConfigA(NAME_PARENT), true);
        String childPath = insertTemplateAInstance(templateParentPath, createAInstance("child"), false);
        String pullPath = getPath(childPath, "b");

        String path = configurationRefactoringManager.pullUp(pullPath, NAME_PARENT);

        assertEquals(getPath(templateParentPath, "b"), path);
        TemplateRecord pulledUp = (TemplateRecord) configurationTemplateManager.getRecord(path);
        assertEquals("44", pulledUp.get("y"));
        assertEquals(NAME_PARENT, pulledUp.getOwner("y"));
        
        TemplateRecord inherited = (TemplateRecord) configurationTemplateManager.getRecord(pullPath);
        assertEquals("44", inherited.get("y"));
        assertEquals(NAME_PARENT, inherited.getOwner("y"));
    }

    public void testPullUpCollectionItem() throws TypeException
    {
        final String NAME_PARENT = "parent";

        // child
        //   bmap
        //     colby <-- pulled up
        String templateParentPath = insertTemplateAInstance(rootPath, new ConfigA(NAME_PARENT), true);
        String childPath = insertTemplateAInstance(templateParentPath, createAInstance("child"), false);
        String pullPath = getPath(childPath, "bmap", "colby");

        String path = configurationRefactoringManager.pullUp(pullPath, NAME_PARENT);

        assertEquals(getPath(templateParentPath, "bmap", "colby"), path);
        TemplateRecord pulledUp = (TemplateRecord) configurationTemplateManager.getRecord(path);
        assertEquals("1", pulledUp.get("y"));
        assertEquals(NAME_PARENT, pulledUp.getOwner("y"));

        TemplateRecord inherited = (TemplateRecord) configurationTemplateManager.getRecord(pullPath);
        assertEquals("1", inherited.get("y"));
        assertEquals(NAME_PARENT, inherited.getOwner("y"));
    }

    public void testPullUpEvents() throws TypeException
    {
        final String NAME_PARENT = "parent";

        String templateParentPath = insertTemplateAInstance(rootPath, new ConfigA(NAME_PARENT), true);
        String childPath = insertTemplateAInstance(templateParentPath, createAInstance("child"), false);
        String siblingPath = insertTemplateAInstance(templateParentPath, new ConfigA("sibling"), false);
        String pullPath = getPath(childPath, "b");

        Listener listener = registerListener();

        configurationRefactoringManager.pullUp(pullPath, NAME_PARENT);

        String siblingInsertPath = getPath(siblingPath, "b");
        listener.assertEvents(new InsertEventSpec(siblingInsertPath, false), new PostInsertEventSpec(siblingInsertPath, false));
    }

    public void testPullUpInternalReferenceDefinedInAncestor() throws TypeException
    {
        final String NAME_PARENT = "parent";

        // parent
        //   ref: defines ee
        //
        // child
        //   b <-- pulled up
        //     refToRef: refers to ee
        ConfigA parent = new ConfigA(NAME_PARENT);
        parent.setRef(new Referee("ee"));
        String templateParentPath = insertTemplateAInstance(rootPath, parent, true);

        parent = configurationTemplateManager.getInstance(templateParentPath, ConfigA.class);
        ConfigA child = new ConfigA("child");
        ConfigB childB = new ConfigB("b");
        childB.setRefToRef(parent.getRef());
        child.setB(childB);
        String childPath = insertTemplateAInstance(templateParentPath, child, false);


        String pullPath = getPath(childPath, "b");
        String path = configurationRefactoringManager.pullUp(pullPath, NAME_PARENT);


        parent = configurationTemplateManager.getInstance(templateParentPath, ConfigA.class);
        child = configurationTemplateManager.getInstance(childPath, ConfigA.class);

        assertSame(parent.getRef(), parent.getB().getRefToRef());
        assertSame(child.getRef(), child.getB().getRefToRef());
        
        TemplateRecord templateParentRecord = (TemplateRecord) configurationTemplateManager.getRecord(templateParentPath);
        assertEquals(NAME_PARENT, templateParentRecord.getOwner("b"));
        TemplateRecord pulledUpB = (TemplateRecord) templateParentRecord.get("b");
        assertEquals(NAME_PARENT, pulledUpB.getOwner("refToRef"));

        String handle = Long.toString(((Record) templateParentRecord.get("ref")).getHandle());
        assertEquals(handle, pulledUpB.get("refToRef"));

        TemplateRecord inherited = (TemplateRecord) configurationTemplateManager.getRecord(path);
        assertEquals(NAME_PARENT, inherited.getOwner("refToRef"));
        assertEquals(handle, inherited.get("refToRef"));
    }

    public void testPullUpReferenceWithinPulledUp() throws TypeException
    {
        final String NAME_PARENT = "parent";

        // child
        //   b <-- pulled up
        //     ref: defines ee
        //     refToRef: refers to ee
        String templateParentPath = insertTemplateAInstance(rootPath, new ConfigA(NAME_PARENT), true);

        ConfigA child = new ConfigA("child");
        ConfigB childB = new ConfigB("b");
        childB.setRef(new Referee("ee"));
        child.setB(childB);
        String childPath = insertTemplateAInstance(templateParentPath, child, false);

        child = configurationTemplateManager.getInstance(childPath, ConfigA.class);
        child.getB().setRefToRef(child.getB().getRef());
        configurationTemplateManager.save(child.getB());


        String pullPath = getPath(childPath, "b");
        configurationRefactoringManager.pullUp(pullPath, NAME_PARENT);


        ConfigA parent = configurationTemplateManager.getInstance(templateParentPath, ConfigA.class);
        child = configurationTemplateManager.getInstance(childPath, ConfigA.class);

        assertNotNull(parent.getB());
        assertSame(parent.getB().getRef(), parent.getB().getRef());
        assertSame(child.getB().getRef(), child.getB().getRefToRef());
    }

    public void testPullUpInternalReferenceToWithinPulledUp() throws TypeException
    {
        final String NAME_PARENT = "parent";

        // child
        //   refToRef: refers to ee
        //   b <-- pulled up
        //     ref: defines ee
        String templateParentPath = insertTemplateAInstance(rootPath, new ConfigA(NAME_PARENT), true);

        ConfigA child = new ConfigA("child");
        ConfigB childB = new ConfigB("b");
        childB.setRef(new Referee("ee"));
        child.setB(childB);
        String childPath = insertTemplateAInstance(templateParentPath, child, false);

        child = configurationTemplateManager.getInstance(childPath, ConfigA.class);
        child.setRefToRef(child.getB().getRef());
        configurationTemplateManager.save(child);


        String pullPath = getPath(childPath, "b");
        configurationRefactoringManager.pullUp(pullPath, NAME_PARENT);


        ConfigA parent = configurationTemplateManager.getInstance(templateParentPath, ConfigA.class);
        child = configurationTemplateManager.getInstance(childPath, ConfigA.class);

        assertNotNull(parent.getB());
        assertNotNull(parent.getB().getRef());
        assertNull(parent.getRefToRef());
        assertSame(child.getB().getRef(), child.getRefToRef());
    }

    public void testGetPushDownChildrenInvalidPath()
    {
        assertEquals(Collections.<String>emptyList(), configurationRefactoringManager.getPushDownChildren("invalid/path/here"));
    }

    public void testGetPushDownChildrenTemplateCollectionItem() throws TypeException
    {
        String path = insertTemplateA(rootPath, "a", false);
        assertEquals(Collections.<String>emptyList(), configurationRefactoringManager.getPushDownChildren(path));
    }

    public void testGetPushDownChildrenNotTemplate()
    {
        String path = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        assertEquals(Collections.<String>emptyList(), configurationRefactoringManager.getPushDownChildren(path));
    }

    public void testGetPushDownChildrenNoChildren() throws TypeException
    {
        assertEquals(Collections.<String>emptyList(), configurationRefactoringManager.getPushDownChildren(getPath(rootPath, "b")));
    }

    public void testGetPushDownChildrenHiddenInChild() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a1"), true);
        String childPath = insertTemplateAInstance(path, createAInstance("a2"), false);
        // Hide the path in the child
        configurationTemplateManager.delete(getPath(childPath, "bmap", "colby"));
        assertEquals(Collections.<String>emptyList(), configurationRefactoringManager.getPushDownChildren(getPath(path, "bmap", "colby")));
    }

    public void testGetPushDownChildren() throws TypeException
    {
        final String NAME_CHILD = "child";

        String path = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        insertTemplateAInstance(path, createAInstance(NAME_CHILD), false);
        assertEquals(asList(NAME_CHILD), configurationRefactoringManager.getPushDownChildren(getPath(path, "b")));
    }

    public void testCanPushDownAnyInvalidPath()
    {
        assertFalse(configurationRefactoringManager.canPushDown("invalid/path/here"));
    }

    public void testCanPushDownAnyTemplateCollectionItem() throws TypeException
    {
        String path = insertTemplateA(rootPath, "a", false);
        assertFalse(configurationRefactoringManager.canPushDown(path));
    }

    public void testCanPushDownAnyNotTemplate()
    {
        String path = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        assertFalse(configurationRefactoringManager.canPushDown(path));
    }

    public void testCanPushDownAnyNoChildren() throws TypeException
    {
        assertFalse(configurationRefactoringManager.canPushDown(getPath(rootPath, "b")));
    }

    public void testCanPushDownAnyNoValidChild() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a1"), true);
        String childPath = insertTemplateAInstance(path, createAInstance("a2"), false);
        // Hide the path in the child
        configurationTemplateManager.delete(getPath(childPath, "bmap", "colby"));
        assertFalse(configurationRefactoringManager.canPushDown(getPath(path, "bmap", "colby")));
    }

    public void testCanPushDownAnyPermanent() throws TypeException
    {
        ConfigA configA = createAInstance("parent");
        configA.getB().setPermanent(true);
        String path = insertTemplateAInstance(rootPath, configA, true);
        insertTemplateAInstance(path, createAInstance("child"), false);
        assertFalse(configurationRefactoringManager.canPushDown(getPath(path, "b")));
    }

    public void testCanPushDownInherited() throws TypeException
    {
        String grandParentPath = insertTemplateAInstance(rootPath, createAInstance("gp"), true);
        String parentPath = insertTemplateAInstance(grandParentPath, createAInstance("p"), true);
        insertTemplateAInstance(parentPath, createAInstance("c"), false);
        assertFalse(configurationRefactoringManager.canPushDown(getPath(parentPath, "b")));
    }

    public void testCanPushDownAny() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        insertTemplateAInstance(path, createAInstance("child"), false);
        assertTrue(configurationRefactoringManager.canPushDown(getPath(path, "b")));
    }

    public void testCanPushDownInvalidPath()
    {
        assertFalse(configurationRefactoringManager.canPushDown("invalid/path/here", "anything"));
    }

    public void testCanPushDownTemplateCollectionItem() throws TypeException
    {
        String path = insertTemplateA(rootPath, "a", false);
        assertFalse(configurationRefactoringManager.canPushDown(path, NAME_ROOT));
    }

    public void testCanPushDownNonTemplatedItem() throws TypeException
    {
        String path = configurationTemplateManager.insert(SAMPLE_SCOPE, createAInstance("a"));
        assertFalse(configurationRefactoringManager.canPushDown(path, NAME_ROOT));
    }

    public void testCanPushDownNoChild() throws TypeException
    {
        assertFalse(configurationRefactoringManager.canPushDown(getPath(rootPath, "b"), NAME_ROOT));
    }

    public void testCanPushDownBadChild() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a"), false);
        assertFalse(configurationRefactoringManager.canPushDown(getPath(path, "b"), NAME_ROOT));
    }

    public void testCanPushDownChildHidesPath() throws TypeException
    {
        final String NAME_CHILD = "child";

        String path = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        String childPath = insertTemplateAInstance(path, createAInstance(NAME_CHILD), false);
        // Hide the path in the child
        configurationTemplateManager.delete(getPath(childPath, "bmap", "colby"));
        assertFalse(configurationRefactoringManager.canPushDown(getPath(path, "bmap", "colby"), NAME_CHILD));
    }

    public void testCanPushDownSimple() throws TypeException
    {
        final String NAME_CHILD = "child";

        String path = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        insertTemplateAInstance(path, createAInstance(NAME_CHILD), false);
        assertFalse(configurationRefactoringManager.canPushDown(getPath(path, "b", "y"), NAME_CHILD));
    }

    public void testCanPushDownComposite() throws TypeException
    {
        final String NAME_CHILD = "child";

        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD), false);
        assertTrue(configurationRefactoringManager.canPushDown(getPath(parentPath, "b"), NAME_CHILD));
    }

    public void testCanPushDownCollectionItem() throws TypeException
    {
        final String NAME_CHILD = "child";

        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD), false);
        assertTrue(configurationRefactoringManager.canPushDown(getPath(parentPath, "bmap", "colby"), NAME_CHILD));
    }

    public void testPushDownInvalidPath()
    {
        try
        {
            configurationRefactoringManager.pushDown("invalid/path", asSet("dummy"));
            fail("Should not be able to push down invalid path");
        }
        catch (IllegalArgumentException e)
        {
            assertThat(e.getMessage(), containsString("Path 'invalid/path' does not exist"));
        }
    }

    public void testPushDownNoChildren() throws TypeException
    {
        String path = insertTemplateA(rootPath, "a", true);
        try
        {
            configurationRefactoringManager.pushDown(getPath(path, "b"), Collections.<String>emptySet());
            fail("Should not be able to push down to no children");
        }
        catch (IllegalArgumentException e)
        {
            assertThat(e.getMessage(), containsString("At least one child must be specified"));
        }
    }

    public void testPushDownInvalidChild() throws TypeException
    {
        String path = insertTemplateAInstance(rootPath, createAInstance("a"), true);
        try
        {
            configurationRefactoringManager.pushDown(getPath(path, "b"), asSet("invalid"));
            fail("Should not be able to push down to an invalid child");
        }
        catch (IllegalArgumentException e)
        {
            assertThat(e.getMessage(), containsString("Specified child 'invalid' is not a direct child of this path's template owner"));
        }
    }

    public void testPushDownPathHiddenInChild() throws TypeException
    {
        // parent
        //   bmap
        //     colby <-- push down(child)
        // child
        //   bmap
        //   [colby] (hidden)
        final String NAME_CHILD = "child";

        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        String childPath = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD), false);
        configurationTemplateManager.delete(getPath(childPath, "bmap", "colby"));
        try
        {
            configurationRefactoringManager.pushDown(getPath(parentPath, "bmap", "colby"), asSet(NAME_CHILD));
            fail("Should not be able to push down to child that hides path");
        }
        catch (IllegalArgumentException e)
        {
            assertThat(e.getMessage(), containsString("Path is hidden in specified child 'child'"));
        }
    }

    public void testPushDownPathHiddenInOtherChild() throws TypeException
    {
        // parent
        //   bmap
        //     colby <-- push down(child)
        // child
        //   bmap
        //   colby
        // child-hidden
        //   bmap
        //   [colby] (hidden)
        final String NAME_CHILD = "child";
        final String NAME_CHILD_HIDDEN = "child-hidden";

        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        String childPath = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD), false);
        String childHiddenPath = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD_HIDDEN), false);
        configurationTemplateManager.delete(getPath(childHiddenPath, "bmap", "colby"));
        String pushPath = getPath(parentPath, "bmap", "colby");
        configurationRefactoringManager.pushDown(pushPath, asSet(NAME_CHILD));

        String toPath = getPath(childPath, "bmap", "colby");
        String toHiddenPath = getPath(childHiddenPath, "bmap", "colby");
        assertFalse(configurationTemplateManager.pathExists(pushPath));
        assertTrue(configurationTemplateManager.pathExists(toPath));
        assertFalse(configurationTemplateManager.pathExists(toHiddenPath));
    }
    
    public void testPushDownComposite() throws TypeException
    {
        // parent
        //   b <-- push down(child)
        // child
        //   b
        final String NAME_CHILD = "child";

        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        String childPath = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD), false);

        String pushPath = getPath(parentPath, "b");
        Set<String> pushedToPaths = configurationRefactoringManager.pushDown(pushPath, asSet(NAME_CHILD));

        String toPath = getPath(childPath, "b");
        assertEquals(asSet(toPath), pushedToPaths);
        assertFalse(configurationTemplateManager.pathExists(pushPath));
        assertTrue(configurationTemplateManager.pathExists(toPath));
        Record record = configurationTemplateManager.getRecord(toPath);
        assertEquals("44", record.get("y"));
    }

    public void testPushDownCollectionItem() throws TypeException
    {
        // parent
        //   bmap
        //     colby <-- push down(child)
        // child
        //   bmap
        //     colby
        final String NAME_CHILD = "child";

        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        String childPath = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD), false);

        String pushPath = getPath(parentPath, "bmap", "colby");
        Set<String> pushedToPaths = configurationRefactoringManager.pushDown(pushPath, asSet(NAME_CHILD));

        String toPath = getPath(childPath, "bmap", "colby");
        assertEquals(asSet(toPath), pushedToPaths);
        assertFalse(configurationTemplateManager.pathExists(pushPath));
        assertTrue(configurationTemplateManager.pathExists(toPath));
        Record record = configurationTemplateManager.getRecord(toPath);
        assertEquals("1", record.get("y"));
    }

    public void testPushDownPreservesOverride() throws TypeException
    {
        // parent
        //   b <-- push down(child)
        // child
        //   b
        final String NAME_CHILD = "child";

        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        ConfigA child = createAInstance(NAME_CHILD);
        child.getB().setY(88);
        String childPath = insertTemplateAInstance(parentPath, child, false);

        String pushPath = getPath(parentPath, "b");
        configurationRefactoringManager.pushDown(pushPath, asSet(NAME_CHILD));

        Record record = configurationTemplateManager.getRecord(getPath(childPath, "b"));
        assertEquals("88", record.get("y"));
    }

    public void testPushDownMultipleChildren() throws TypeException
    {
        // parent
        //   b <-- push down(child1, child2)
        // child1
        //   b
        // child2
        //   b
        final String NAME_CHILD1 = "child1";
        final String NAME_CHILD2 = "child2";

        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        String child1Path = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD1), false);
        String child2Path = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD2), false);

        String pushPath = getPath(parentPath, "b");
        Set<String> pushedToPaths = configurationRefactoringManager.pushDown(pushPath, asSet(NAME_CHILD1, NAME_CHILD2));

        String toPath1 = getPath(child1Path, "b");
        String toPath2 = getPath(child2Path, "b");
        assertEquals(asSet(toPath1, toPath2), pushedToPaths);
        assertFalse(configurationTemplateManager.pathExists(pushPath));
        assertTrue(configurationTemplateManager.pathExists(toPath1));
        assertTrue(configurationTemplateManager.pathExists(toPath2));
        Record record = configurationTemplateManager.getRecord(toPath1);
        assertEquals("44", record.get("y"));
        record = configurationTemplateManager.getRecord(toPath2);
        assertEquals("44", record.get("y"));
    }

    public void testPushDownSubsetOfChildren() throws TypeException
    {
        // parent
        //   b <-- push down(child1)
        // child1
        //   b
        // child2
        //   b
        final String NAME_CHILD1 = "child1";
        final String NAME_CHILD2 = "child2";

        String parentPath = insertTemplateAInstance(rootPath, createAInstance("parent"), true);
        String child1Path = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD1), false);
        String child2Path = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD2), false);
        Listener listener = registerListener();

        String pushPath = getPath(parentPath, "b");
        Set<String> pushedToPaths = configurationRefactoringManager.pushDown(pushPath, asSet(NAME_CHILD1));

        String toPath = getPath(child1Path, "b");
        String deletedPath = getPath(child2Path, "b");
        assertEquals(asSet(toPath), pushedToPaths);
        assertFalse(configurationTemplateManager.pathExists(pushPath));
        assertTrue(configurationTemplateManager.pathExists(toPath));
        assertFalse(configurationTemplateManager.pathExists(deletedPath));
        Record record = configurationTemplateManager.getRecord(toPath);
        assertEquals("44", record.get("y"));
        
        listener.assertEvents(new DeleteEventSpec(deletedPath, false), new PostDeleteEventSpec(deletedPath, false));
    }

    public void testPushDownWalksNestedPaths() throws TypeException
    {
        // parent
        //   b <-- pushed down
        //     cmap
        //       colc
        // child
        //   b
        //     cmap
        //       colc
        final String NAME_CHILD = "child";
        final String NAME_C = "colc";

        ConfigA parentA = createAInstance("parent");
        parentA.getB().getCmap().put(NAME_C, new ConfigC(NAME_C));
        String parentPath = insertTemplateAInstance(rootPath, parentA, true);
        String childPath = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD), false);

        String pushPath = getPath(parentPath, "b");
        configurationRefactoringManager.pushDown(pushPath, asSet(NAME_CHILD));

        Record record = configurationTemplateManager.getRecord(getPath(childPath, "b", "cmap", "colc"));
        assertEquals(ConfigC.DEFAULT_VALUE, record.get("value"));
    }

    public void testPushDownHiddenPathNestedInChild() throws TypeException
    {
        // parent
        //   b <-- pushed down
        //     cmap
        //       colc
        // child
        //   b
        //     cmap
        //       [colc] (hidden)
        final String NAME_CHILD = "child";
        final String NAME_C = "colc";

        ConfigA parentA = createAInstance("parent");
        parentA.getB().getCmap().put(NAME_C, new ConfigC(NAME_C));
        String parentPath = insertTemplateAInstance(rootPath, parentA, true);
        String childPath = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD), false);
        String hiddenPath = getPath(childPath, "b", "cmap", "colc");
        configurationTemplateManager.delete(hiddenPath);

        String pushPath = getPath(parentPath, "b");
        configurationRefactoringManager.pushDown(pushPath, asSet(NAME_CHILD));

        Record record = configurationTemplateManager.getRecord(getPath(childPath, "b"));
        assertEquals("44", record.get("y"));
        assertFalse(configurationTemplateManager.pathExists(hiddenPath));
    }

    public void testPushDownReferenceWithin() throws TypeException
    {
        // parent
        //   b <-- pushed down
        //     ref: ee
        //     refToRef: refers to ee
        // child
        //   b
        final String NAME_CHILD = "child";

        ConfigA parentA = createAInstance("parent");
        parentA.getB().setRef(new Referee("ee"));
        String parentPath = insertTemplateAInstance(rootPath, parentA, true);
        String childPath = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD), false);
        parentA = configurationTemplateManager.getInstance(parentPath, ConfigA.class);
        parentA.getB().setRefToRef(parentA.getB().getRef());
        configurationTemplateManager.save(parentA);

        String pushPath = getPath(parentPath, "b");
        configurationRefactoringManager.pushDown(pushPath, asSet(NAME_CHILD));

        ConfigA childA = configurationTemplateManager.getInstance(childPath, ConfigA.class);
        assertNotNull(childA.getB().getRef());
        assertSame(childA.getB().getRef(), childA.getB().getRefToRef());
    }

    public void testPushDownInternalReferenceToPushedDown() throws TypeException
    {
        // parent
        //   refToRef: refers to ee
        //   b <-- pushed down
        //     ref: ee
        // child
        //   b
        final String NAME_CHILD = "child";

        ConfigA parentA = createAInstance("parent");
        parentA.getB().setRef(new Referee("ee"));
        String parentPath = insertTemplateAInstance(rootPath, parentA, true);
        insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD), false);
        parentA = configurationTemplateManager.getInstance(parentPath, ConfigA.class);
        parentA.setRefToRef(parentA.getB().getRef());
        configurationTemplateManager.save(parentA);

        try
        {
            configurationRefactoringManager.pushDown(getPath(parentPath, "b"), asSet(NAME_CHILD));
            fail("Should not be able to push down when there are internal references to the item to push down");
        }
        catch (IllegalArgumentException e)
        {
            assertThat(e.getMessage(), containsString("Reference from 'template/parent' to within item to pull down that would become invalid"));
        }
    }

    public void testPushDownInternalReferenceFromPushedDown() throws TypeException
    {
        // parent
        //   ref: ee
        //   b <-- pushed down
        //     refToRef: refers to ee
        // child
        //   b
        final String NAME_CHILD = "child";

        ConfigA parentA = createAInstance("parent");
        parentA.setRef(new Referee("ee"));
        String parentPath = insertTemplateAInstance(rootPath, parentA, true);
        String childPath = insertTemplateAInstance(parentPath, new ConfigA(NAME_CHILD), false);
        parentA = configurationTemplateManager.getInstance(parentPath, ConfigA.class);
        parentA.getB().setRefToRef(parentA.getRef());
        configurationTemplateManager.save(parentA);

        configurationRefactoringManager.pushDown(getPath(parentPath, "b"), asSet(NAME_CHILD));

        ConfigA childA = configurationTemplateManager.getInstance(childPath, ConfigA.class);
        assertNotNull(childA.getRef());
        assertSame(childA.getRef(), childA.getB().getRefToRef());
    }

    public void testMoveEmptyPath()
    {
        failedMoveHelper("", rootPath, "path.required", "Path");
    }

    public void testMoveInvalidPath()
    {
        failedMoveHelper("invalid", rootPath, "path.nonexistant", "Path", "invalid");
    }

    public void testMoveScope()
    {
        failedMoveHelper(TEMPLATE_SCOPE, rootPath, "path.not.templated.collection.item", "Path", TEMPLATE_SCOPE);
    }
    
    public void testMoveNonTemplatedPath()
    {
        String path = configurationTemplateManager.insert(SAMPLE_SCOPE, new ConfigA("non templated"));
        failedMoveHelper(path, rootPath, "path.not.templated", "Path", path);
    }

    public void testMoveUnderTemplatedItem()
    {
        String itemPath = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, new ConfigA("templated"), rootPath, false);
        String path = getPath(itemPath, "recipes");
        failedMoveHelper(path, rootPath, "path.not.templated.collection.item", "Path", path);
    }

    public void testMoveRoot()
    {
        failedMoveHelper(rootPath, rootPath, "path.is.root", "Path", rootPath);
    }
    
    public void testMoveNewTemplateParentPathEmpty()
    {
        String path = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, new ConfigA("templated"), rootPath, false);
        failedMoveHelper(path, "", "path.required", "New template parent path");
    }

    public void testMoveNewTemplateParentPathInvalid()
    {
        String path = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, new ConfigA("templated"), rootPath, false);
        failedMoveHelper(path, "invalid", "path.nonexistant", "New template parent path", "invalid");
    }

    public void testMoveNewTemplateParentPathAScope()
    {
        String path = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, new ConfigA("templated"), rootPath, false);
        failedMoveHelper(path, TEMPLATE_SCOPE, "path.not.templated.collection.item", "New template parent path", TEMPLATE_SCOPE);
    }

    public void testMoveNewTemplateParentPathNotATemplate()
    {
        String path = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, new ConfigA("templated"), rootPath, false);
        failedMoveHelper(path, path, "path.is.concrete", "New template parent path", path);
    }

    public void testMoveNewTemplateParentInDifferentScope() throws TypeException
    {
        final String ANOTHER_SCOPE = "another";

        CompositeType typeA = typeRegistry.getType(ConfigA.class);
        MapType templatedMap = new TemplatedMapType(typeA, typeRegistry);
        configurationPersistenceManager.register(ANOTHER_SCOPE, templatedMap);

        MutableRecord root = unstantiate(new ConfigA("r2"));
        configurationTemplateManager.markAsTemplate(root);
        String anotherRootPath = configurationTemplateManager.insertRecord(ANOTHER_SCOPE, root);

        String path = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, new ConfigA("templated"), rootPath, false);
        failedMoveHelper(path, anotherRootPath, "move.new.template.parent.different.scope", anotherRootPath, path);
    }

    private void failedMoveHelper(String path, String newTemplateParentPath, String expectedMessageKey, Object... expectedMessageArgs)
    {
        try
        {
            configurationRefactoringManager.move(path, newTemplateParentPath);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertThat(e.getMessage(), containsString(I18N.format(expectedMessageKey, expectedMessageArgs)));
        }
    }

    public void testMoveTrivial()
    {
        MoveHierarchy hierarchy = setupSimpleMoveHierarchy();

        Listener listener = registerListener();
        ConfigurationRefactoringManager.MoveResult result = configurationRefactoringManager.move(hierarchy.childPath, hierarchy.originalTemplateParentPath);
        assertEquals(Collections.<String>emptyList(), result.getDeletedPaths());
        listener.assertEvents();
    }
    
    public void testMoveComplexConcrete()
    {
        // original parent
        //   x: 1
        //   blist: [original b]
        //   bmap: {}
        //   ref: original referee
        //
        // new parent
        //   x: 2
        //   blist: [new b]
        //   bmap: {map new b, map both b}
        //   ref: null
        //
        // child
        //   x: 1 (scrubbed before move, should be preserved)
        //   blist: [original b] (inherited before move, should be preserved)
        //   bmap: {map child b, map both b}
        //   ref: original referee (inherited before move, should be preserved)
        ConfigA originalTemplateParent = new ConfigA("original parent");
        originalTemplateParent.setX(1);
        originalTemplateParent.getBlist().add(new ConfigB("original b"));
        originalTemplateParent.setRef(new Referee("original referee"));
        String originalTemplateParentPath = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, originalTemplateParent, rootPath, true);

        ConfigA newTemplateParent = new ConfigA("new template parent");
        newTemplateParent.setX(2);
        newTemplateParent.getBlist().add(new ConfigB("new b"));
        newTemplateParent.getBmap().put("map new b", new ConfigB("map new b"));
        newTemplateParent.getBmap().put("map both b", new ConfigB("map both b"));
        String newTemplateParentPath = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, newTemplateParent, rootPath, true);

        String childPath = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, new ConfigA("child"), originalTemplateParentPath, false);
        ConfigA child = configurationTemplateManager.deepClone(configurationTemplateManager.getInstance(childPath, ConfigA.class));
        child.setX(1);
        child.getBmap().put("map child b", new ConfigB("map child b"));
        child.getBmap().put("map both b", new ConfigB("map both b"));
        configurationTemplateManager.save(child);

        Listener listener = registerListener();

        ConfigurationRefactoringManager.MoveResult result = configurationRefactoringManager.move(childPath, newTemplateParentPath);
        assertEquals(Collections.<String>emptyList(), result.getDeletedPaths());
        
        // expected child
        //   x: 1
        //   blist: [original b, new b]
        //   bmap: {map child b, map both b, map new b}
        //   ref: original referee
        child = configurationTemplateManager.getInstance(childPath, ConfigA.class);
        assertEquals(1, child.getX());
        Referee referee = child.getRef();
        assertNotNull(referee);
        assertEquals("original referee", referee.getName());
        List<ConfigB> blist = child.getBlist();
        assertEquals(2, blist.size());
        assertEquals("original b", blist.get(0).getName());
        assertEquals("new b", blist.get(1).getName());
        Map<String, ConfigB> bmap = child.getBmap();
        assertEquals(3, bmap.size());
        assertEquals(CollectionUtils.asSet("map new b", "map both b", "map child b"), bmap.keySet());

        String newBPath = blist.get(1).getConfigurationPath();
        String newMapBPath = getPath(childPath, "bmap", "map new b");
        listener.assertEvents(
                new InsertEventSpec(newBPath, false),
                new PostInsertEventSpec(newBPath, false),
                new InsertEventSpec(newMapBPath, false),
                new PostInsertEventSpec(newMapBPath, false)
        );
    }

    public void testMoveSubtree()
    {
        MoveHierarchy moveHierarchy = setupSubtreeMoveHierarchy();
        
        ConfigA newTemplateParent = moveHierarchy.cloneNewTemplateParent();
        newTemplateParent.setX(-1);
        newTemplateParent.addRecipe(new ConfigRecipe("newp recipe"));
        configurationTemplateManager.save(newTemplateParent);
        
        ConfigA originalTemplateParent = moveHierarchy.cloneOriginalTemplateParent();
        originalTemplateParent.setX(1);
        originalTemplateParent.addRecipe(new ConfigRecipe("originalp recipe"));
        configurationTemplateManager.save(originalTemplateParent);
        
        ConfigA child = moveHierarchy.cloneChild();
        child.setX(2);
        child.addRecipe(new ConfigRecipe("child recipe"));
        configurationTemplateManager.save(child);
        
        ConfigA grandchild1 = moveHierarchy.cloneGrandchild1();
        grandchild1.setX(3);
        grandchild1.addRecipe(new ConfigRecipe("grandchild1 recipe"));
        configurationTemplateManager.save(grandchild1);
        
        Listener listener = registerListener();

        ConfigurationRefactoringManager.MoveResult result = configurationRefactoringManager.move(moveHierarchy.childPath, moveHierarchy.newTemplateParentPath);
        assertEquals(Collections.<String>emptyList(), result.getDeletedPaths());
        
        child = configurationTemplateManager.getInstance(moveHierarchy.childPath, ConfigA.class);
        assertEquals(2, child.getX());
        assertEquals(asSet("child recipe", "originalp recipe", "newp recipe"), child.getRecipes().keySet());

        grandchild1 = configurationTemplateManager.getInstance(moveHierarchy.grandchild1Path, ConfigA.class);
        assertEquals(3, grandchild1.getX());
        assertEquals(asSet("grandchild1 recipe", "child recipe", "originalp recipe", "newp recipe"), grandchild1.getRecipes().keySet());

        ConfigA grandchild2 = configurationTemplateManager.getInstance(moveHierarchy.grandchild2Path, ConfigA.class);
        assertEquals(2, grandchild2.getX());
        assertEquals(asSet("child recipe", "originalp recipe", "newp recipe"), grandchild2.getRecipes().keySet());
        
        String newRecipeRemainderPath = getPath("recipes", "newp recipe");
        listener.assertEvents(
                new InsertEventSpec(getPath(moveHierarchy.grandchild1Path, newRecipeRemainderPath), false),
                new PostInsertEventSpec(getPath(moveHierarchy.grandchild1Path, newRecipeRemainderPath), false),
                new InsertEventSpec(getPath(moveHierarchy.grandchild2Path, newRecipeRemainderPath), false),
                new PostInsertEventSpec(getPath(moveHierarchy.grandchild2Path, newRecipeRemainderPath), false)
        );
    }

    public void testMoveDeleteIncompatible()
    {
        final String RECIPE_NAME = "recipe";
        final String COMMAND_NAME = "command";

        // New template parent and child both have commands at the same path,
        // one is an ant command, the other a maven command. 
        MoveHierarchy moveHierarchy = setupSimpleMoveHierarchy();

        ConfigA newTemplateParent = moveHierarchy.cloneNewTemplateParent();
        ConfigRecipe newTemplateParentRecipe = new ConfigRecipe(RECIPE_NAME);
        newTemplateParentRecipe.addCommand(new ConfigMavenCommand(COMMAND_NAME));
        newTemplateParent.addRecipe(newTemplateParentRecipe);
        configurationTemplateManager.save(newTemplateParent);

        ConfigA child = moveHierarchy.cloneChild();
        ConfigRecipe childRecipe = new ConfigRecipe(RECIPE_NAME);
        childRecipe.addCommand(new ConfigAntCommand(COMMAND_NAME, new ConfigEnvironment()));
        child.addRecipe(childRecipe);
        configurationTemplateManager.save(child);

        child = moveHierarchy.cloneChild();
        ConfigAntCommand childCommand = (ConfigAntCommand) child.getRecipes().get(RECIPE_NAME).getCommands().get(COMMAND_NAME);
        String deletedEnvironmentPath = childCommand.getEnvironments().get(0).getConfigurationPath();

        Listener listener = registerListener();

        ConfigurationRefactoringManager.MoveResult result = configurationRefactoringManager.move(moveHierarchy.childPath, moveHierarchy.newTemplateParentPath);
        String deletedCommandPath = getPath(moveHierarchy.childPath, "recipes", RECIPE_NAME, "commands", COMMAND_NAME);
        assertEquals(asList(deletedCommandPath), result.getDeletedPaths());

        child = configurationTemplateManager.getInstance(moveHierarchy.childPath, ConfigA.class);
        assertEquals(asSet(RECIPE_NAME), child.getRecipes().keySet());
        ConfigRecipe recipe = child.getRecipes().get(RECIPE_NAME);
        assertEquals(asSet(COMMAND_NAME), recipe.getCommands().keySet());
        assertTrue(recipe.getCommands().get(COMMAND_NAME) instanceof ConfigMavenCommand);

        listener.assertEvents(
                new DeleteEventSpec(deletedCommandPath, false),
                new PostDeleteEventSpec(deletedCommandPath, false),
                new DeleteEventSpec(deletedEnvironmentPath, true),
                new PostDeleteEventSpec(deletedEnvironmentPath, true)
        );
    }

    public void testMoveDeleteIncompatibleInSubtree()
    {
        final String RECIPE_NAME = "recipe";
        final String COMMAND_NAME = "command";

        // New template parent and grandchild1 both have commands at the same
        // path, one is an ant command, the other a maven command.  The recipe
        // does not exist at all in grandchild2, where it will be added.
        MoveHierarchy moveHierarchy = setupSubtreeMoveHierarchy();

        ConfigA newTemplateParent = moveHierarchy.cloneNewTemplateParent();
        ConfigRecipe newTemplateParentRecipe = new ConfigRecipe(RECIPE_NAME);
        newTemplateParentRecipe.addCommand(new ConfigMavenCommand(COMMAND_NAME));
        newTemplateParent.addRecipe(newTemplateParentRecipe);
        configurationTemplateManager.save(newTemplateParent);

        ConfigA grandchild1 = moveHierarchy.cloneGrandchild1();
        ConfigRecipe grandchild1Recipe = new ConfigRecipe(RECIPE_NAME);
        grandchild1Recipe.addCommand(new ConfigAntCommand(COMMAND_NAME, new ConfigEnvironment()));
        grandchild1.addRecipe(grandchild1Recipe);
        configurationTemplateManager.save(grandchild1);

        grandchild1 = moveHierarchy.cloneGrandchild1();
        ConfigAntCommand grandchild1Command = (ConfigAntCommand) grandchild1.getRecipes().get(RECIPE_NAME).getCommands().get(COMMAND_NAME);
        String deletedEnvironmentPath = grandchild1Command.getEnvironments().get(0).getConfigurationPath();

        Listener listener = registerListener();

        ConfigurationRefactoringManager.MoveResult result = configurationRefactoringManager.move(moveHierarchy.childPath, moveHierarchy.newTemplateParentPath);
        String deletedCommandPath = getPath(moveHierarchy.grandchild1Path, "recipes", RECIPE_NAME, "commands", COMMAND_NAME);
        assertEquals(asList(deletedCommandPath), result.getDeletedPaths());

        grandchild1 = configurationTemplateManager.getInstance(moveHierarchy.grandchild1Path, ConfigA.class);
        assertEquals(asSet(RECIPE_NAME), grandchild1.getRecipes().keySet());
        ConfigRecipe recipe = grandchild1.getRecipes().get(RECIPE_NAME);
        assertEquals(asSet(COMMAND_NAME), recipe.getCommands().keySet());
        assertTrue(recipe.getCommands().get(COMMAND_NAME) instanceof ConfigMavenCommand);

        ConfigA grandchild2 = configurationTemplateManager.getInstance(moveHierarchy.grandchild2Path, ConfigA.class);
        ConfigRecipe grandchild2Recipe = grandchild2.getRecipes().get(RECIPE_NAME);
        String grandchild2RecipePath = grandchild2Recipe.getConfigurationPath();
        String grandchild2CommandPath = grandchild2Recipe.getCommands().get(COMMAND_NAME).getConfigurationPath();
        listener.assertEvents(
                new DeleteEventSpec(deletedCommandPath, false),
                new PostDeleteEventSpec(deletedCommandPath, false),
                new DeleteEventSpec(deletedEnvironmentPath, true),
                new PostDeleteEventSpec(deletedEnvironmentPath, true),
                new InsertEventSpec(grandchild2RecipePath, false),
                new PostInsertEventSpec(grandchild2RecipePath, false),
                new InsertEventSpec(grandchild2CommandPath, true),
                new InsertEventSpec(grandchild2CommandPath, true)
        );
    }
    
    public void testMoveInternalReference()
    {
        MoveHierarchy moveHierarchy = setupSimpleMoveHierarchy();
        
        ConfigA child = moveHierarchy.cloneChild();
        child.setRef(new Referee("ee"));
        configurationTemplateManager.save(child);

        moveInternalReferenceHelper(moveHierarchy);
    }

    public void testMoveReferenceToItemDefinedInOriginalParent()
    {
        MoveHierarchy moveHierarchy = setupSimpleMoveHierarchy();
        
        ConfigA originalTemplateParent = moveHierarchy.cloneOriginalTemplateParent();
        originalTemplateParent.setRef(new Referee("ee"));
        configurationTemplateManager.save(originalTemplateParent);

        moveInternalReferenceHelper(moveHierarchy);
    }

    public void testMoveReferenceToItemDefinedInCommonAncestor()
    {
        MoveHierarchy moveHierarchy = setupSimpleMoveHierarchy();
        
        ConfigA root = configurationTemplateManager.deepClone(configurationTemplateManager.getInstance(rootPath, ConfigA.class));
        root.setRef(new Referee("ee"));
        configurationTemplateManager.save(root);

        moveInternalReferenceHelper(moveHierarchy);
    }

    private void moveInternalReferenceHelper(MoveHierarchy moveHierarchy)
    {
        ConfigA child = moveHierarchy.cloneChild();
        child.setRefToRef(child.getRef());
        child.getListOfRefs().add(child.getRef());
        configurationTemplateManager.save(child);

        doMoveWithNoExpectedEvents(moveHierarchy);

        child = configurationTemplateManager.getInstance(moveHierarchy.childPath, ConfigA.class);
        assertSame(child.getRef(), child.getRefToRef());
        List<Referee> refs = child.getListOfRefs();
        assertEquals(1, refs.size());
        assertSame(child.getRef(), refs.get(0));
    }

    public void testMoveWithHiddenMapItem()
    {
        // The child hides a recipe defined in its template parent.  Ensure it
        // is not pushed down, and the bad hidden key is healed post-move. 
        MoveHierarchy moveHierarchy = setupSimpleMoveHierarchy();
        
        ConfigA originalTemplateParent = moveHierarchy.cloneOriginalTemplateParent();
        originalTemplateParent.addRecipe(new ConfigRecipe("recipe"));
        configurationTemplateManager.save(originalTemplateParent);
        
        configurationTemplateManager.delete(getPath(moveHierarchy.childPath, "recipes", "recipe"));

        doMoveWithNoExpectedEvents(moveHierarchy);

        ConfigA child = configurationTemplateManager.getInstance(moveHierarchy.childPath, ConfigA.class);
        assertEquals(Collections.<String>emptySet(), child.getRecipes().keySet());
    }

    public void testMoveWithHiddenMapItemDefinedInCommonAncestor()
    {
        // The child hides a recipe defined in the root.  Ensure it is still
        // hidden after the move. 
        MoveHierarchy moveHierarchy = setupSimpleMoveHierarchy();
        
        ConfigA root = configurationTemplateManager.deepClone(configurationTemplateManager.getInstance(rootPath, ConfigA.class));
        root.addRecipe(new ConfigRecipe("recipe"));
        configurationTemplateManager.save(root);
        
        configurationTemplateManager.delete(getPath(moveHierarchy.childPath, "recipes", "recipe"));

        doMoveWithNoExpectedEvents(moveHierarchy);
        
        ConfigA child = configurationTemplateManager.getInstance(moveHierarchy.childPath, ConfigA.class);
        assertEquals(Collections.<String>emptySet(), child.getRecipes().keySet());
    }

    public void testMoveWithHiddenMapItemInOneSubtree()
    {
        // grandchild1 hides a recipe defined in the child.  Ensure it is still
        // hidden after the move.  grandchild2 doesn't hide it - ensure it is
        // still there.
        MoveHierarchy moveHierarchy = setupSubtreeMoveHierarchy();
        
        ConfigA child = moveHierarchy.cloneChild();
        child.addRecipe(new ConfigRecipe("recipe"));
        configurationTemplateManager.save(child);
        
        configurationTemplateManager.delete(getPath(moveHierarchy.grandchild1Path, "recipes", "recipe"));

        doMoveWithNoExpectedEvents(moveHierarchy);
        
        ConfigA grandchild1 = configurationTemplateManager.getInstance(moveHierarchy.grandchild1Path, ConfigA.class);
        assertEquals(Collections.<String>emptySet(), grandchild1.getRecipes().keySet());

        ConfigA grandchild2 = configurationTemplateManager.getInstance(moveHierarchy.grandchild2Path, ConfigA.class);
        assertEquals(asSet("recipe"), grandchild2.getRecipes().keySet());
    }

    public void testMoveWithHiddenListItem()
    {
        // The child hides a blist item defined in its template parent.  Ensure
        // it is not pushed down, and the bad hidden key is healed post-move. 
        MoveHierarchy moveHierarchy = setupSimpleMoveHierarchy();
        
        ConfigA originalTemplateParent = moveHierarchy.cloneOriginalTemplateParent();
        originalTemplateParent.getBlist().add(new ConfigB("b"));
        configurationTemplateManager.save(originalTemplateParent);
        
        ConfigA child = configurationTemplateManager.getInstance(moveHierarchy.childPath, ConfigA.class);
        configurationTemplateManager.delete(child.getBlist().get(0).getConfigurationPath());

        doMoveWithNoExpectedEvents(moveHierarchy);

        child = configurationTemplateManager.getInstance(moveHierarchy.childPath, ConfigA.class);
        assertEquals(Collections.<ConfigB>emptyList(), child.getBlist());
    }

    public void testMoveWithHiddenListItemDefinedInCommonAncestor()
    {
        // The child hides a blist item defined in the root.  Ensure it is
        // still hidden after the move. 
        MoveHierarchy moveHierarchy = setupSimpleMoveHierarchy();
        
        ConfigA root = configurationTemplateManager.deepClone(configurationTemplateManager.getInstance(rootPath, ConfigA.class));
        root.getBlist().add(new ConfigB("b"));
        configurationTemplateManager.save(root);
        
        ConfigA child = configurationTemplateManager.getInstance(moveHierarchy.childPath, ConfigA.class);
        configurationTemplateManager.delete(child.getBlist().get(0).getConfigurationPath());

        doMoveWithNoExpectedEvents(moveHierarchy);

        child = configurationTemplateManager.getInstance(moveHierarchy.childPath, ConfigA.class);
        assertEquals(Collections.<ConfigB>emptyList(), child.getBlist());
    }

    public void testMoveWithHiddenListItemInOneSubtree()
    {
        // grandchild1 hides a blist item defined in the child.  Ensure it is
        // still hidden after the move.  grandchild2 doesn't hide it - ensure
        // it is still there.
        MoveHierarchy moveHierarchy = setupSubtreeMoveHierarchy();
        
        ConfigA child = moveHierarchy.cloneChild();
        child.getBlist().add(new ConfigB("b"));
        configurationTemplateManager.save(child);
        
        ConfigA grandchild1 = configurationTemplateManager.getInstance(moveHierarchy.grandchild1Path, ConfigA.class);
        configurationTemplateManager.delete(grandchild1.getBlist().get(0).getConfigurationPath());

        doMoveWithNoExpectedEvents(moveHierarchy);
        
        grandchild1 = configurationTemplateManager.getInstance(moveHierarchy.grandchild1Path, ConfigA.class);
        assertEquals(Collections.<ConfigB>emptyList(), grandchild1.getBlist());

        ConfigA grandchild2 = configurationTemplateManager.getInstance(moveHierarchy.grandchild2Path, ConfigA.class);
        assertEquals(1, grandchild2.getBlist().size());
    }
    
    public void testMoveListOrderedInOriginalParent()
    {
        MoveHierarchy moveHierarchy = setupSimpleMoveHierarchy();

        String originalTemplateParentListPath = getPath(moveHierarchy.originalTemplateParentPath, "orderedBlist");
        configurationTemplateManager.insert(originalTemplateParentListPath, new ConfigB("1"));
        configurationTemplateManager.insert(originalTemplateParentListPath, new ConfigB("2"));
        
        reverseOrder(getPath(moveHierarchy.originalTemplateParentPath, "orderedBlist"));
        
        doMoveWithNoExpectedEvents(moveHierarchy);
        
        checkMovedOrderedList(moveHierarchy.childPath);
    }

    public void testMoveListOrderedInChild()
    {
        MoveHierarchy moveHierarchy = setupSimpleMoveHierarchy();
        
        String originalTemplateParentListPath = getPath(moveHierarchy.originalTemplateParentPath, "orderedBlist");
        configurationTemplateManager.insert(originalTemplateParentListPath, new ConfigB("1"));
        configurationTemplateManager.insert(originalTemplateParentListPath, new ConfigB("2"));
        
        reverseOrder(getPath(moveHierarchy.childPath, "orderedBlist"));
        
        doMoveWithNoExpectedEvents(moveHierarchy);

        checkMovedOrderedList(moveHierarchy.childPath);
    }

    public void testMoveSubtreeListOrderedInChild()
    {
        MoveHierarchy moveHierarchy = setupSubtreeMoveHierarchy();
        
        String originalTemplateParentListPath = getPath(moveHierarchy.originalTemplateParentPath, "orderedBlist");
        configurationTemplateManager.insert(originalTemplateParentListPath, new ConfigB("1"));
        configurationTemplateManager.insert(originalTemplateParentListPath, new ConfigB("2"));
        
        reverseOrder(getPath(moveHierarchy.childPath, "orderedBlist"));

        doMoveWithNoExpectedEvents(moveHierarchy);

        checkMovedOrderedList(moveHierarchy.childPath);
        checkMovedOrderedList(moveHierarchy.grandchild1Path);
        checkMovedOrderedList(moveHierarchy.grandchild2Path);
    }

    private void checkMovedOrderedList(String path)
    {
        ConfigA instance = configurationTemplateManager.getInstance(path, ConfigA.class);
        List<ConfigB> list = instance.getOrderedBlist();
        assertEquals(2, list.size());
        assertEquals("2", list.get(0).getName());
        assertEquals("1", list.get(1).getName());
    }

    public void testPreviewMoveInvalidPath()
    {
        failedPreviewMoveHelper("invalid", rootPath, "path.nonexistant", "Path", "invalid");
    }

    public void testPreviewMoveInvalidNewTemplateParentPath()
    {
        String path = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, new ConfigA("templated"), rootPath, false);
        failedPreviewMoveHelper(path, "invalid", "path.nonexistant", "New template parent path", "invalid");
    }
    
    private void failedPreviewMoveHelper(String path, String newTemplateParentPath, String expectedMessageKey, Object... expectedMessageArgs)
    {
        try
        {
            configurationRefactoringManager.previewMove(path, newTemplateParentPath);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            assertThat(e.getMessage(), containsString(I18N.format(expectedMessageKey, expectedMessageArgs)));
        }
    }
    
    public void testPreviewMoveTrivial()
    {
        MoveHierarchy hierarchy = setupSimpleMoveHierarchy();

        Listener listener = registerListener();
        ConfigurationRefactoringManager.MoveResult result = configurationRefactoringManager.previewMove(hierarchy.childPath, hierarchy.originalTemplateParentPath);
        assertEquals(Collections.<String>emptyList(), result.getDeletedPaths());
        listener.assertEvents();
    }

    public void testPreviewMoveMakesNoChanges()
    {
        MoveHierarchy hierarchy = setupSimpleMoveHierarchy();
        
        // Add a recipe in the new parent, and ensure that it does not get
        // added to the child (it would if we actually made the move).
        ConfigA newTemplateParent = hierarchy.cloneNewTemplateParent();
        newTemplateParent.addRecipe(new ConfigRecipe("a"));

        Listener listener = registerListener();
        ConfigurationRefactoringManager.MoveResult result = configurationRefactoringManager.previewMove(hierarchy.childPath, hierarchy.originalTemplateParentPath);
        assertEquals(Collections.<String>emptyList(), result.getDeletedPaths());
        listener.assertEvents();
        
        assertFalse(configurationTemplateManager.pathExists(getPath(hierarchy.childPath, "recipes", "a")));
    }
    
    public void testPreviewMoveIncompatiblePaths()
    {
        final String RECIPE_NAME = "recipe";
        final String COMMAND_NAME = "command";

        // New template parent and child both have commands at the same path,
        // one is an ant command, the other a maven command. 
        MoveHierarchy moveHierarchy = setupSimpleMoveHierarchy();

        ConfigA newTemplateParent = moveHierarchy.cloneNewTemplateParent();
        ConfigRecipe newTemplateParentRecipe = new ConfigRecipe(RECIPE_NAME);
        newTemplateParentRecipe.addCommand(new ConfigMavenCommand(COMMAND_NAME));
        newTemplateParent.addRecipe(newTemplateParentRecipe);
        configurationTemplateManager.save(newTemplateParent);

        ConfigA child = moveHierarchy.cloneChild();
        ConfigRecipe childRecipe = new ConfigRecipe(RECIPE_NAME);
        childRecipe.addCommand(new ConfigAntCommand(COMMAND_NAME, new ConfigEnvironment()));
        child.addRecipe(childRecipe);
        configurationTemplateManager.save(child);

        Listener listener = registerListener();

        ConfigurationRefactoringManager.MoveResult result = configurationRefactoringManager.previewMove(moveHierarchy.childPath, moveHierarchy.newTemplateParentPath);
        String deletedCommandPath = getPath(moveHierarchy.childPath, "recipes", RECIPE_NAME, "commands", COMMAND_NAME);
        assertEquals(asList(deletedCommandPath), result.getDeletedPaths());
        
        // Check path was not actually deleted.
        assertTrue(configurationTemplateManager.pathExists(deletedCommandPath));

        listener.assertEvents();
    }
    
    private ConfigA createAInstance(String name)
    {
        ConfigA instance = new ConfigA(name);
        instance.setX(10);
        ConfigB b = new ConfigB("b");
        b.setY(44);
        instance.setB(b);
        instance.getBlist().add(new ConfigB("lisby"));
        ConfigB colby = new ConfigB("colby");
        colby.setY(1);
        instance.getBmap().put("colby", colby);
        instance.setRef(new Referee("ee"));
        return instance;
    }

    private void assertClone(ConfigA clone, String name)
    {
        assertAInstance(clone, "clone of " + name);
    }

    private void assertAInstance(ConfigA instance, String name)
    {
        assertEquals(name, instance.getName());
        assertEquals(10, instance.getX());
        ConfigB cloneB = instance.getB();
        assertEquals("b", cloneB.getName());
        assertEquals(44, cloneB.getY());
        assertEquals(1, instance.getBlist().size());
        assertEquals("lisby", instance.getBlist().get(0).getName());
        assertEquals(1, instance.getBmap().size());
        ConfigB cloneColby = instance.getBmap().get("colby");
        assertEquals("colby", cloneColby.getName());
        assertEquals(1, cloneColby.getY());
        Referee cloneRef = instance.getRef();
        assertNotNull(cloneRef);
        assertEquals("ee", cloneRef.getName());
    }

    private void invalidCloneNameHelper(String path, String cloneName, String expectedError) throws TypeException
    {
        try
        {
            configurationRefactoringManager.clone(path, cloneName);
            fail();
        }
        catch(IllegalArgumentException e)
        {
            assertEquals(expectedError, e.getMessage());
        }
        catch(ToveRuntimeException e)
        {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof ValidationException);
            assertEquals(expectedError, cause.getMessage());
        }
    }

    private String insertTemplateA(String templateParentPath, String name, boolean template) throws TypeException
    {
        return insertTemplateAInstance(templateParentPath, new ConfigA(name), template);
    }

    private String insertTemplateAInstance(String templateParentPath, ConfigA instance, boolean template) throws TypeException
    {
        MutableRecord record = unstantiate(instance);
        if(template)
        {
            configurationTemplateManager.markAsTemplate(record);
        }
        configurationTemplateManager.setParentTemplate(record, configurationTemplateManager.getRecord(templateParentPath).getHandle());
        return configurationTemplateManager.insertRecord(TEMPLATE_SCOPE, record);
    }

    private String addB(String aPath, String name)
    {
        return configurationTemplateManager.insert(getPath(aPath, "bmap"), new ConfigB(name));
    }

    private MoveHierarchy setupSimpleMoveHierarchy()
    {
        ConfigA originalTemplateParent = new ConfigA("originalp");
        String originalTemplateParentPath = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, originalTemplateParent, rootPath, true);

        ConfigA newTemplateParent = new ConfigA("newp");
        String newTemplateParentPath = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, newTemplateParent, rootPath, true);
        
        ConfigA child = new ConfigA("child");
        String childPath = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, child, originalTemplateParentPath, false);

        return new MoveHierarchy(originalTemplateParentPath, newTemplateParentPath, childPath);
    }

    private MoveHierarchy setupSubtreeMoveHierarchy()
    {
        ConfigA originalTemplateParent = new ConfigA("originalp");
        String originalTemplateParentPath = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, originalTemplateParent, rootPath, true);

        ConfigA newTemplateParent = new ConfigA("newp");
        String newTemplateParentPath = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, newTemplateParent, rootPath, true);
        
        ConfigA child = new ConfigA("child");
        String childPath = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, child, originalTemplateParentPath, true);
        
        ConfigA grandchild1 = new ConfigA("grand1");
        String grandchild1Path = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, grandchild1, childPath, false);

        ConfigA grandchild2 = new ConfigA("grand2");
        String grandchild2Path = configurationTemplateManager.insertTemplated(TEMPLATE_SCOPE, grandchild2, childPath, false);
        
        return new MoveHierarchy(originalTemplateParentPath, newTemplateParentPath, childPath, grandchild1Path, grandchild2Path);
    }

    private void doMoveWithNoExpectedEvents(MoveHierarchy moveHierarchy)
    {
        Listener listener = registerListener();
        ConfigurationRefactoringManager.MoveResult result = configurationRefactoringManager.move(moveHierarchy.childPath, moveHierarchy.newTemplateParentPath);
        assertEquals(Collections.<String>emptyList(), result.getDeletedPaths());
        listener.assertEvents();
    }

    private void reverseOrder(String path)
    {
        CollectionType type = configurationTemplateManager.getType(path, CollectionType.class);
        List<String> order = type.getOrder(recordManager.select(path));
        Collections.reverse(order);
        configurationTemplateManager.setOrder(path, order);
    }

    private class MoveHierarchy
    {
        String originalTemplateParentPath;
        String newTemplateParentPath;
        String childPath;
        String grandchild1Path;
        String grandchild2Path;

        private MoveHierarchy(String originalTemplateParentPath, String newTemplateParentPath, String childPath)
        {
            this.originalTemplateParentPath = originalTemplateParentPath;
            this.newTemplateParentPath = newTemplateParentPath;
            this.childPath = childPath;
        }

        private MoveHierarchy(String originalTemplateParentPath, String newTemplateParentPath, String childPath, String grandchild1Path, String grandchild2Path)
        {
            this.originalTemplateParentPath = originalTemplateParentPath;
            this.newTemplateParentPath = newTemplateParentPath;
            this.childPath = childPath;
            this.grandchild1Path = grandchild1Path;
            this.grandchild2Path = grandchild2Path;
        }

        ConfigA cloneOriginalTemplateParent()
        {
            return deepClone(originalTemplateParentPath);
        }

        ConfigA cloneNewTemplateParent()
        {
            return deepClone(newTemplateParentPath);
        }

        ConfigA cloneChild()
        {
            return deepClone(childPath);
        }
        
        ConfigA cloneGrandchild1()
        {
            return deepClone(grandchild1Path);
        }

        ConfigA cloneGrandchild2()
        {
            return deepClone(grandchild2Path);
        }
        
        private ConfigA deepClone(String path)
        {
            return configurationTemplateManager.deepClone(configurationTemplateManager.getInstance(path, ConfigA.class));
        }
    }

    @SymbolicName("a")
    public static class ConfigA extends AbstractNamedConfiguration
    {
        private int x;
        private ConfigB b;
        private List<ConfigB> blist = new LinkedList<ConfigB>();
        @Ordered
        private List<ConfigB> orderedBlist = new LinkedList<ConfigB>();
        private Map<String, ConfigB> bmap = new HashMap<String, ConfigB>();
        @Ordered
        private Map<String, ConfigB> orderedBmap = new LinkedHashMap<String, ConfigB>();
        private Referee ref;
        @Reference
        private Referee refToRef;
        @Reference
        private List<Referee> listOfRefs = new LinkedList<Referee>();
        
        private Map<String, ConfigRecipe> recipes = new LinkedHashMap<String, ConfigRecipe>();

        public ConfigA()
        {
        }

        public ConfigA(String name)
        {
            super(name);
        }

        public int getX()
        {
            return x;
        }

        public void setX(int x)
        {
            this.x = x;
        }

        public ConfigB getB()
        {
            return b;
        }

        public void setB(ConfigB b)
        {
            this.b = b;
        }

        public List<ConfigB> getBlist()
        {
            return blist;
        }

        public void setBlist(List<ConfigB> blist)
        {
            this.blist = blist;
        }

        public List<ConfigB> getOrderedBlist()
        {
            return orderedBlist;
        }

        public void setOrderedBlist(List<ConfigB> orderedBlist)
        {
            this.orderedBlist = orderedBlist;
        }

        public Map<String, ConfigB> getBmap()
        {
            return bmap;
        }

        public void setBmap(Map<String, ConfigB> bmap)
        {
            this.bmap = bmap;
        }

        public Map<String, ConfigB> getOrderedBmap()
        {
            return orderedBmap;
        }

        public void setOrderedBmap(Map<String, ConfigB> orderedBmap)
        {
            this.orderedBmap = orderedBmap;
        }

        public Referee getRef()
        {
            return ref;
        }

        public void setRef(Referee ref)
        {
            this.ref = ref;
        }

        public Referee getRefToRef()
        {
            return refToRef;
        }

        public void setRefToRef(Referee refToRef)
        {
            this.refToRef = refToRef;
        }

        public List<Referee> getListOfRefs()
        {
            return listOfRefs;
        }

        public void setListOfRefs(List<Referee> listOfRefs)
        {
            this.listOfRefs = listOfRefs;
        }

        public Map<String, ConfigRecipe> getRecipes()
        {
            return recipes;
        }

        public void setRecipes(Map<String, ConfigRecipe> recipes)
        {
            this.recipes = recipes;
        }
        
        public void addRecipe(ConfigRecipe recipe)
        {
            recipes.put(recipe.getName(), recipe);
        }
    }

    @SymbolicName("b")
    public static class ConfigB extends AbstractNamedConfiguration
    {
        private int y;
        private Referee ref;
        @Reference
        private Referee refToRef;
        @Reference
        private ConfigA refToA;
        private Map<String, ConfigC> cmap = new HashMap<String, ConfigC>();

        public ConfigB()
        {
        }

        public ConfigB(String name)
        {
            super(name);
        }

        public int getY()
        {
            return y;
        }

        public void setY(int y)
        {
            this.y = y;
        }

        public Referee getRef()
        {
            return ref;
        }

        public void setRef(Referee ref)
        {
            this.ref = ref;
        }

        public Referee getRefToRef()
        {
            return refToRef;
        }

        public void setRefToRef(Referee refToRef)
        {
            this.refToRef = refToRef;
        }

        public ConfigA getRefToA()
        {
            return refToA;
        }

        public void setRefToA(ConfigA refToA)
        {
            this.refToA = refToA;
        }

        public Map<String, ConfigC> getCmap()
        {
            return cmap;
        }

        public void setCmap(Map<String, ConfigC> cmap)
        {
            this.cmap = cmap;
        }
    }
    
    @SymbolicName("c")
    public static class ConfigC extends AbstractNamedConfiguration
    {
        public static final String DEFAULT_VALUE = "default";

        private String value = DEFAULT_VALUE;

        public ConfigC()
        {
        }

        public ConfigC(String name)
        {
            super(name);
        }

        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }
    }

    @SymbolicName("referee")
    public static class Referee extends AbstractNamedConfiguration
    {
        public Referee()
        {
        }

        public Referee(String name)
        {
            super(name);
        }
    }

    @SymbolicName("recipe")
    public static class ConfigRecipe extends AbstractNamedConfiguration
    {
        private Map<String, ConfigCommand> commands = new HashMap<String, ConfigCommand>();

        public ConfigRecipe()
        {
        }

        public ConfigRecipe(String name)
        {
            super(name);
        }

        public Map<String, ConfigCommand> getCommands()
        {
            return commands;
        }

        public void addCommand(ConfigCommand command)
        {
            commands.put(command.getName(), command);
        }
        
        public void setCommands(Map<String, ConfigCommand> commands)
        {
            this.commands = commands;
        }
    }

    @SymbolicName("command")
    public static interface ConfigCommand extends NamedConfiguration
    {

    }

    @SymbolicName("command.ant")
    public static class ConfigAntCommand extends AbstractNamedConfiguration implements ConfigCommand
    {
        private List<ConfigEnvironment> environments = new LinkedList<ConfigEnvironment>();

        public ConfigAntCommand()
        {
        }

        public ConfigAntCommand(String name, ConfigEnvironment environment)
        {
            super(name);
            this.environments.add(environment);
        }

        public List<ConfigEnvironment> getEnvironments()
        {
            return environments;
        }

        public void setEnvironments(List<ConfigEnvironment> field)
        {
            this.environments = field;
        }
    }

    @SymbolicName("command.maven")
    public static class ConfigMavenCommand extends AbstractNamedConfiguration implements ConfigCommand
    {
        public ConfigMavenCommand()
        {
        }

        public ConfigMavenCommand(String name)
        {
            super(name);
        }
    }
    
    @SymbolicName("environment")
    public static class ConfigEnvironment extends AbstractConfiguration
    {
        private String key;
        private String value;

        public ConfigEnvironment()
        {
        }

        public ConfigEnvironment(String key, String value)
        {
            this.key = key;
            this.value = value;
        }

        public String getKey()
        {
            return key;
        }

        public void setKey(String key)
        {
            this.key = key;
        }

        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }
    }
}
