package com.zutubi.pulse.scm.p4;

import com.zutubi.pulse.config.PropertiesConfig;
import com.zutubi.pulse.personal.PersonalBuildUI;
import com.zutubi.pulse.scm.FileStatus;
import com.zutubi.pulse.scm.SCMException;
import com.zutubi.pulse.scm.WorkingCopyStatus;
import static com.zutubi.pulse.scm.p4.P4Constants.*;
import com.zutubi.pulse.test.PulseTestCase;
import com.zutubi.pulse.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 */
public class P4WorkingCopyTest extends PulseTestCase implements PersonalBuildUI
{
    private static final String CLIENT_NAME = "test-client-1";
    private static final String OTHER_CLIENT_NAME = "test-client-2";
    private static final String HEAD_REVISION = "2";

    private File tempDir;
    private Process p4dProcess;
    private File clientRoot;
    private P4Client client;
    private File otherClientRoot;
    private P4Client otherClient;

    private List<String> debugs = new LinkedList<String>();
    private List<String> statuses = new LinkedList<String>();
    private List<String> errors = new LinkedList<String>();
    private List<String> warnings = new LinkedList<String>();

    private P4WorkingCopy wc;

    protected void setUp() throws Exception
    {
        tempDir = FileSystemUtils.createTempDirectory(P4WorkingCopyTest.class.getName(), "");
        tempDir = tempDir.getCanonicalFile();
        File repoDir = new File(tempDir, "repo");
        repoDir.mkdir();

        File repoZip = getTestDataFile("core", "repo", "zip");
        FileSystemUtils.extractZip(repoZip, repoDir);

        // Restore from checkpoint
        p4dProcess = Runtime.getRuntime().exec(new String[] { "p4d", "-r", repoDir.getAbsolutePath(), "-jr", "checkpoint.1"});
        p4dProcess.waitFor();
        
        p4dProcess = Runtime.getRuntime().exec(new String[] { "p4d", "-r", repoDir.getAbsolutePath()});
        waitForServer(1666);

        createClients();

        wc = new P4WorkingCopy(null, new PropertiesConfig());
        wc.setUI(this);
        wc.getClient().setEnv(ENV_CLIENT, CLIENT_NAME);
        wc.getClient().setEnv(ENV_PORT, ":1666");
        wc.getClient().setEnv(ENV_USER, "test-user");
        wc.getClient().setWorkingDir(clientRoot);
    }

    private void createClients() throws SCMException
    {
        clientRoot = new File(tempDir, CLIENT_NAME);
        clientRoot.mkdir();

        client = new P4Client();
        client.setEnv(ENV_CLIENT, CLIENT_NAME);
        client.setEnv(ENV_PORT, ":1666");
        client.setEnv(ENV_USER, "test-user");
        client.setEnv("PWD", clientRoot.getAbsolutePath());
        client.setWorkingDir(clientRoot);

        client.createClient("client-1", CLIENT_NAME, clientRoot);
        client.runP4(null, P4_COMMAND, COMMAND_SYNC, FLAG_FORCE);

        otherClientRoot = new File(tempDir, OTHER_CLIENT_NAME);
        otherClientRoot.mkdir();

        otherClient = new P4Client();
        otherClient.setEnv(ENV_CLIENT, OTHER_CLIENT_NAME);
        otherClient.setEnv(ENV_PORT, ":1666");
        otherClient.setEnv(ENV_USER, "test-user");
        otherClient.setEnv("PWD", otherClientRoot.getAbsolutePath());
        otherClient.setWorkingDir(otherClientRoot);

        otherClient.createClient("client-1", OTHER_CLIENT_NAME, otherClientRoot);
        otherClient.runP4(null, P4_COMMAND, COMMAND_SYNC, FLAG_FORCE);
    }

    protected void tearDown() throws Exception
    {
        p4dProcess.destroy();
        p4dProcess.waitFor();
        p4dProcess = null;
        Thread.sleep(100);

        FileSystemUtils.removeDirectory(tempDir);

        statuses = null;
        errors = null;
        warnings = null;
    }

    public void testRepositoryMatches() throws SCMException
    {
        Properties properties = new Properties();
        properties.put(PROPERTY_PORT, ":1666");
        assertTrue(wc.matchesRepository(properties));
    }

    public void testRepositoryDoesntMatch() throws SCMException
    {
        Properties properties = new Properties();
        properties.put(PROPERTY_PORT, "anotherhost:1666");
        assertFalse(wc.matchesRepository(properties));
        assertWarning("P4PORT setting ':1666' does not match Pulse project's P4PORT 'anotherhost:1666'");
    }

    public void testStatusNoChanges() throws SCMException
    {
        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertEquals(0, status.getChanges().size());
    }

    public void testStatusOpenForEdit() throws SCMException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, "file1");

        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertChange(status, "file1", FileStatus.State.MODIFIED, false, true);
    }

    public void testStatusOpenForEditExecutable() throws SCMException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, "script1");

        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertChange(status, "script1", FileStatus.State.MODIFIED, false, true);
    }

    public void testStatusOpenForEditAddExecutable() throws SCMException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, FLAG_TYPE, "text+x", "file1");

        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertExecutable(status, "file1", true);
    }

    public void testStatusOpenForEditRemoveExecutable() throws SCMException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, FLAG_TYPE, "text", "script1");

        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertExecutable(status, "script1", false);
    }

    public void testStatusOpenForEditBinary() throws SCMException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, "bin1");

        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertChange(status, "bin1", FileStatus.State.MODIFIED, false, false);
    }

    public void testStatusOpenForEditAddBinary() throws SCMException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, FLAG_TYPE, "binary", "file1");

        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertChange(status, "file1", FileStatus.State.MODIFIED, false, false);
    }

    public void testStatusOpenForEditRemoveBinary() throws SCMException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, FLAG_TYPE, "text", "bin1");

        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertChange(status, "bin1", FileStatus.State.MODIFIED, false, true);
    }

    public void testStatusOpenForAdd() throws SCMException, IOException
    {
        File newFile = new File(clientRoot, "newfile");
        FileSystemUtils.createFile(newFile, "new");
        client.runP4(null, P4_COMMAND, COMMAND_ADD, "newfile");

        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertChange(status, "newfile", FileStatus.State.ADDED, false, true);
    }

    public void testStatusOpenForAddBinary() throws SCMException, IOException
    {
        File newFile = new File(clientRoot, "newfile");
        FileSystemUtils.createFile(newFile, "new");
        client.runP4(null, P4_COMMAND, COMMAND_ADD, FLAG_TYPE, "binary", "newfile");

        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertChange(status, "newfile", FileStatus.State.ADDED, false, false);
    }

    public void testStatusOpenForAddExecutable() throws SCMException, IOException
    {
        File newFile = new File(clientRoot, "newfile");
        FileSystemUtils.createFile(newFile, "new");
        client.runP4(null, P4_COMMAND, COMMAND_ADD, FLAG_TYPE, "text+x", "newfile");

        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertExecutable(status, "newfile", true);
    }

    public void testStatusOpenForDelete() throws SCMException, IOException
    {
        client.runP4(null, P4_COMMAND, COMMAND_DELETE, "dir1/file1");

        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertChange(status, "dir1/file1", FileStatus.State.DELETED, false, false);
    }

    public void testStatusOpenForIntegrate() throws SCMException, IOException
    {
        client.runP4(null, P4_COMMAND, COMMAND_INTEGRATE, "file1", "integrated");

        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertChange(status, "integrated", FileStatus.State.BRANCHED, false, true);
    }

    public void testStatusOpenForIntegrateEdited() throws SCMException, IOException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, "branches/1/file1");
        client.submit("edit on branch 1");

        client.runP4(null, P4_COMMAND, COMMAND_INTEGRATE, "branches/1/...", "...");
        client.runP4(null, P4_COMMAND, COMMAND_RESOLVE, FLAG_AUTO_MERGE);

        WorkingCopyStatus status = wc.getStatus();
        assertChange(status, "file1", FileStatus.State.MERGED, false, true);
    }

    public void testStatusOpenForIntegrateBinary() throws SCMException, IOException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, "branches/1/bin1");
        client.submit("edit on branch 1");

        client.runP4(null, P4_COMMAND, COMMAND_INTEGRATE, "branches/1/...", "...");
        client.runP4(null, P4_COMMAND, COMMAND_RESOLVE, FLAG_AUTO_MERGE);

        WorkingCopyStatus status = wc.getStatus();
        assertChange(status, "bin1", FileStatus.State.MERGED, false, false);
    }

    public void testStatusOpenForIntegrateExecutable() throws SCMException, IOException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, "branches/1/script1");
        client.submit("edit on branch 1");

        client.runP4(null, P4_COMMAND, COMMAND_INTEGRATE, "branches/1/...", "...");
        client.runP4(null, P4_COMMAND, COMMAND_RESOLVE, FLAG_AUTO_MERGE);

        WorkingCopyStatus status = wc.getStatus();
        assertChange(status, "script1", FileStatus.State.MERGED, false, true);
    }

    public void testStatusOpenForIntegrateAddExecutable() throws SCMException, IOException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, FLAG_TYPE, "text+x", "branches/1/file1");
        client.submit("edit on branch 1");

        client.runP4(null, P4_COMMAND, COMMAND_INTEGRATE, FLAG_TYPE, "branches/1/...", "...");
        client.runP4(null, P4_COMMAND, COMMAND_RESOLVE, FLAG_AUTO_MERGE);

        WorkingCopyStatus status = wc.getStatus();
        assertExecutable(status, "file1", true);
    }

    public void testStatusOpenForIntegrateRemoveExecutable() throws SCMException, IOException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, FLAG_TYPE, "text", "branches/1/script1");
        client.submit("edit on branch 1");

        client.runP4(null, P4_COMMAND, COMMAND_INTEGRATE, FLAG_TYPE, "branches/1/...", "...");
        client.runP4(null, P4_COMMAND, COMMAND_RESOLVE, FLAG_AUTO_MERGE);

        WorkingCopyStatus status = wc.getStatus();
        assertExecutable(status, "script1", false);
    }

    public void testStatusOODEdited() throws SCMException, IOException
    {
        otherEdit();

        WorkingCopyStatus status = wc.getStatus();
        assertChange(status, "file1", FileStatus.State.UNCHANGED, true, false);
    }

    public void testStatusOODAdded() throws SCMException, IOException
    {
        File newFile = new File(otherClientRoot, "newfile");
        FileSystemUtils.createFile(newFile, "new");
        otherClient.runP4(null, P4_COMMAND, COMMAND_ADD, "newfile");
        otherClient.submit("comment");

        WorkingCopyStatus status = wc.getStatus();
        assertChange(status, "newfile", FileStatus.State.UNCHANGED, true, false);
    }

    public void testStatusOODDeleted() throws SCMException
    {
        otherClient.runP4(null, P4_COMMAND, COMMAND_DELETE, "file1");
        otherClient.submit("comment");

        WorkingCopyStatus status = wc.getStatus();
        assertChange(status, "file1", FileStatus.State.UNCHANGED, true, false);
    }

    public void testStatusOODAndLocalChange() throws SCMException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, "file1");

        otherClient.runP4(null, P4_COMMAND, COMMAND_DELETE, "file1");
        otherClient.submit("comment");

        WorkingCopyStatus status = wc.getStatus();
        assertChange(status, "file1", FileStatus.State.MODIFIED, true, true);
    }

    public void testStatusEditEdited() throws SCMException, IOException
    {
        otherEdit();

        client.runP4(null, P4_COMMAND, COMMAND_EDIT, "file1");
        client.runP4(null, P4_COMMAND, COMMAND_SYNC);
        
        WorkingCopyStatus status = wc.getStatus();
        assertChange(status, "file1", FileStatus.State.UNRESOLVED, false, false);
    }

    public void testStatusEditDeleted() throws SCMException
    {
        otherClient.runP4(null, P4_COMMAND, COMMAND_DELETE, "file1");
        otherClient.submit("comment");

        client.runP4(null, P4_COMMAND, COMMAND_EDIT, "file1");
        client.runP4(null, P4_COMMAND, COMMAND_SYNC);
        
        WorkingCopyStatus status = wc.getStatus();
        assertChange(status, "file1", FileStatus.State.UNRESOLVED, false, false);
    }

    public void testStatusDeleteEdited() throws SCMException, IOException
    {
        otherEdit();

        client.runP4(null, P4_COMMAND, COMMAND_DELETE, "file1");
        client.runP4(null, P4_COMMAND, COMMAND_SYNC);

        WorkingCopyStatus status = wc.getStatus();
        assertChange(status, "file1", FileStatus.State.DELETED, false, false);
    }

    public void testStatusDeleteDeleted() throws SCMException
    {
        otherClient.runP4(null, P4_COMMAND, COMMAND_DELETE, "file1");
        otherClient.submit("comment");

        client.runP4(null, P4_COMMAND, COMMAND_DELETE, "file1");
        client.runP4(null, P4_COMMAND, COMMAND_SYNC);

        WorkingCopyStatus status = wc.getStatus();
        assertChange(status, "file1", FileStatus.State.DELETED, false, false);
    }

    public void testStatusUnresolvedMerge() throws SCMException
    {
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, "branches/1/file1");
        client.submit("edit on branch");
        
        client.runP4(null, P4_COMMAND, COMMAND_INTEGRATE, "//depot/branches/1/...", "//depot/...");

        WorkingCopyStatus status = wc.getStatus();
        assertChange(status, "file1", FileStatus.State.UNRESOLVED, false, false);
    }

    public void testUpdateAlreadyUpToDate() throws SCMException
    {
        wc.update();
        WorkingCopyStatus status = wc.getStatus();
        assertEquals(HEAD_REVISION, status.getRevision().getRevisionString());
        assertEquals(0, status.getChanges().size());
    }

    public void testSimpleUpdate() throws SCMException, IOException
    {
        otherEdit();

        wc.update();
        WorkingCopyStatus status = wc.getStatus();
        assertEquals("3", status.getRevision().getRevisionString());
        assertEquals(0, status.getChanges().size());
    }

    public void testUpdateMerge() throws SCMException, IOException
    {
        otherEdit();
        client.runP4(null, P4_COMMAND, COMMAND_EDIT, "file1");

        wc.update();
        WorkingCopyStatus status = wc.getStatus();
        assertEquals("3", status.getRevision().getRevisionString());
        assertChange(status, "file1", FileStatus.State.MODIFIED, false, true);
    }

    public void testUpdateConflict() throws SCMException, IOException
    {
        otherEdit();

        client.runP4(null, P4_COMMAND, COMMAND_EDIT, "file1");
        File file1 = new File(clientRoot, "file1");
        FileSystemUtils.createFile(file1, "this");

        wc.update();
        WorkingCopyStatus status = wc.getStatus();
        assertEquals("3", status.getRevision().getRevisionString());
        assertChange(status, "file1", FileStatus.State.UNRESOLVED, false, false);
    }

    private void otherEdit() throws SCMException, IOException
    {
        otherClient.runP4(null, P4_COMMAND, COMMAND_EDIT, "file1");
        File other1 = new File(otherClientRoot, "file1");
        FileSystemUtils.createFile(other1, "other");
        otherClient.submit("comment");
    }

    private void assertChange(WorkingCopyStatus status, String path, FileStatus.State state, boolean outOfDate, boolean text)
    {
        List<FileStatus> changes = status.getChanges();
        assertEquals(1, changes.size());
        FileStatus fs = changes.get(0);
        assertEquals(path, fs.getPath());
        assertEquals(state, fs.getState());
        assertFalse(fs.isDirectory());
        assertEquals(outOfDate, fs.isOutOfDate());
        if(text)
        {
            assertEquals(1, fs.getProperties().size());
            assertEquals(FileStatus.EOLStyle.TEXT.toString(), fs.getProperty(FileStatus.PROPERTY_EOL_STYLE));
        }
        else
        {
            assertEquals(0, fs.getProperties().size());
        }
    }

    private void assertExecutable(WorkingCopyStatus status, String path, boolean executable)
    {
        FileStatus fs = status.getFileStatus(path);
        assertEquals(path, fs.getPath());
        assertEquals(Boolean.toString(executable), fs.getProperty(FileStatus.PROPERTY_EXECUTABLE));
    }


    private void assertWarning(String message)
    {
        assertEquals(message, warnings.remove(0));
    }

    public void setVerbosity(Verbosity verbosity)
    {
    }

    public void debug(String message)
    {
        debugs.add(message);
    }

    public void status(String message)
    {
        statuses.add(message);
    }

    public void warning(String message)
    {
        warnings.add(message);
    }

    public void error(String message)
    {
        errors.add(message);
    }

    public void error(String message, Throwable throwable)
    {
        errors.add(message);
    }

    public Response ynaPrompt(String question, Response defaultResponse)
    {
        return defaultResponse;
    }
}
