package com.zutubi.pulse.master.restore;

import com.zutubi.pulse.master.scheduling.Task;
import com.zutubi.pulse.master.scheduling.TaskExecutionContext;
import com.zutubi.util.logging.Logger;

/**
 *
 *
 */
public class AutomatedBackupTask implements Task
{
    private static final Logger LOG = Logger.getLogger(AutomatedBackupTask.class);

    private BackupManager backupManager;

    public void execute(TaskExecutionContext context)
    {
        backupManager.triggerBackup();
    }

    public void setBackupManager(BackupManager backupManager)
    {
        this.backupManager = backupManager;
    }
}
