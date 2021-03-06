package com.zutubi.pulse.core.engine;

import com.zutubi.pulse.core.marshal.FileResolver;

import java.io.File;

/**
 * Able to provide a pulse file, primarily the file contents but possibly also
 * other information.
 */
public interface PulseFileProvider
{
    String getPath();
    String getFileContent(FileResolver resolver) throws Exception;
    File getImportRoot();
}
