package com.zutubi.pulse.master.tove.config.project.types;

import com.zutubi.pulse.core.engine.RecipeConfiguration;
import com.zutubi.pulse.core.test.api.PulseTestCase;

import java.util.Arrays;

public class MultiRecipeTypeDefaultRecipeOptionProviderTest extends PulseTestCase
{
    private MultiRecipeTypeDefaultRecipeOptionProvider provider = new MultiRecipeTypeDefaultRecipeOptionProvider();

    public void testNullInstance()
    {
        assertEquals(0, provider.getOptions(null, null, null).size());
    }

    public void testNoRecipes()
    {
        assertEquals(0, provider.getOptions(new MultiRecipeTypeConfiguration(), null, null).size());
    }

    public void testSimple()
    {
        MultiRecipeTypeConfiguration type = new MultiRecipeTypeConfiguration();
        type.addRecipe(new RecipeConfiguration("default"));
        type.addRecipe(new RecipeConfiguration("absolutely"));
        type.addRecipe(new RecipeConfiguration("fabulous"));
        assertEquals(Arrays.asList("absolutely", "default", "fabulous"), provider.getOptions(type, null, null));
    }
}
