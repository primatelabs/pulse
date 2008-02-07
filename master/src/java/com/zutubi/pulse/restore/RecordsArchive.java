package com.zutubi.pulse.restore;

import com.zutubi.prototype.type.record.RecordSerialiser;
import com.zutubi.prototype.type.record.DefaultRecordSerialiser;
import com.zutubi.prototype.type.record.Record;
import com.zutubi.prototype.type.record.RecordHandler;
import com.zutubi.prototype.type.record.store.RecordStore;

import java.io.File;

/**
 *
 *
 */
public class RecordsArchive implements ArchiveableComponent
{
    private RecordStore recordStore;

    public String getName()
    {
        return "records";
    }

    public void backup(Archive archive) throws ArchiveException
    {

    }

    public void restore(Archive archive) throws ArchiveException
    {

    }

    public void backup(File base)
    {
        Record export = recordStore.exportRecords();

        RecordSerialiser serialiser = new DefaultRecordSerialiser(base);
        serialiser.serialise("", export, true);
    }

    public void restore(File base)
    {
        RecordSerialiser serialiser = new DefaultRecordSerialiser(base);
        Record baseRecord = serialiser.deserialise("", new RecordHandler()
        {
            public void handle(String path, Record record)
            {
                // use these callbacks to provide user feedback.
            }
        });

        recordStore.importRecords(baseRecord);
    }

    public void setRecordStore(RecordStore recordStore)
    {
        this.recordStore = recordStore;
    }
}
