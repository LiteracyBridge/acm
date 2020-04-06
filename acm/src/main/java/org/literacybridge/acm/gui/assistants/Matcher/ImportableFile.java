package org.literacybridge.acm.gui.assistants.Matcher;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class ImportableFile {
    private File file;

    public ImportableFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }
    public String getTitle() {
        return toString();
    }

    @Override
    public String toString() {
        return file!=null ? FilenameUtils.getBaseName(file.getName()) : "(null)";
    }
}
