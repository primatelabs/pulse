package com.cinnamonbob.core2.config;

import com.cinnamonbob.model.CommandResult;
import com.cinnamonbob.util.FileSystemUtils;
import junit.framework.TestCase;

import java.io.File;

/**
 * 
 *
 */
public class ExecutableCommandTest extends TestCase
{
        
    private File outputDirectory; 
    
    public void setUp() throws Exception
    {
        super.setUp();
        outputDirectory = FileSystemUtils.createTmpDirectory(ExecutableCommandTest.class.getName(), "");
    }
    
    public void tearDown() throws Exception
    {
        FileSystemUtils.removeDirectory(outputDirectory);        
        super.tearDown();
    }
    
    public void testExecuteSuccessExpected() throws Exception
    {
        ExecutableCommand command = new ExecutableCommand();
        command.setExe("dir");
        command.setArgs(".");
        CommandResult result = command.execute(outputDirectory);
        assertTrue(result.succeeded());        
    }
    
    public void testExecuteFailureExpected() throws Exception
    {
        ExecutableCommand command = new ExecutableCommand();
        command.setExe("dir");
        command.setArgs("w");
        CommandResult result = command.execute(outputDirectory);
        assertFalse(result.succeeded());        
    }

    public void testExecuteSuccessExpectedNoArg() throws Exception
    {
        ExecutableCommand command = new ExecutableCommand();
        command.setExe("netstat");
        CommandResult result = command.execute(outputDirectory);
        assertTrue(result.succeeded());        
    }
    
    public void testExecuteExceptionExpected() throws Exception
    {
        ExecutableCommand command = new ExecutableCommand();
        command.setExe("unknown");
        command.setArgs("command");
        try 
        {
            command.execute(outputDirectory);
            assertTrue(false);
        } catch (CommandException e)
        {
            // noop            
        }
    }
}
