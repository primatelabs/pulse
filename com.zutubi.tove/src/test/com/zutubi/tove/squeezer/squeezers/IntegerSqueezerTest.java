package com.zutubi.tove.squeezer.squeezers;

import com.zutubi.tove.squeezer.SqueezeException;
import com.zutubi.util.junit.ZutubiTestCase;

/**
 * <class-comment/>
 */
public class IntegerSqueezerTest extends ZutubiTestCase
{
    private IntegerSqueezer squeezer;

    protected void setUp() throws Exception
    {
        super.setUp();

        squeezer = new IntegerSqueezer();
    }

    protected void tearDown() throws Exception
    {
        squeezer = null;

        super.tearDown();
    }

    public void testNullToString() throws SqueezeException
    {
        assertEquals("", squeezer.squeeze(null));
    }

    public void testIntegerToString() throws SqueezeException
    {
        assertEquals("25", squeezer.squeeze(new Integer(25)));
    }

    public void testPrimitiveToString() throws SqueezeException
    {
        assertEquals("15", squeezer.squeeze(15));
    }

    public void testStringToInteger() throws SqueezeException
    {
        assertEquals(15, squeezer.unsqueeze("15"));
    }

    public void testEmptyStringToInteger() throws SqueezeException
    {
        assertNull(squeezer.unsqueeze(""));
    }

    public void testInvalid() throws SqueezeException
    {
        try
        {
            squeezer.unsqueeze("a");
            fail();
        }
        catch (SqueezeException e)
        {
            assertEquals("'a' is not a valid integer", e.getMessage());
        }
    }
}
