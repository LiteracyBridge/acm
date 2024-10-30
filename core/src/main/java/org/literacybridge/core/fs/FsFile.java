package org.literacybridge.core.fs;

import org.apache.commons.io.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is an implementation of TbFile that wraps the Java File.
 */

public class FsFile extends TbFile {
    private final File file;

    public FsFile(File file) {
        this.file = file;
    }

    @Override
    public FsFile open(String child) {
        return new FsFile(new File(this.file, child));
    }

    @Override
    public TbFile getParent() {
        return new FsFile(file.getParentFile());
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    @Override
    public void renameTo(String newName) {
        try {
            if (file.isDirectory()) {
                FileUtils.moveDirectory(file, new File(newName));
            } else {
                FileUtils.moveFile(file, new File(newName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean mkdir() {
        if (!file.exists())
            return file.mkdir();
        return false;
    }

    @Override
    public boolean mkdirs() {
        if (!file.exists())
            return file.mkdirs();
        return false;
    }

    @Override
    public void createNew(InputStream content, Flags... flags) throws IOException {
        boolean append = Arrays.asList(flags).contains(Flags.append);

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file, append))) {
            copy(content, out);
            out.flush();
        }
    }

    @Override
    public OutputStream createNew(Flags... flags) throws IOException {
        boolean append = Arrays.asList(flags).contains(Flags.append);
        return new BufferedOutputStream(new FileOutputStream(file, append));
    }

    @Override
    public boolean delete() {
        return file.delete();
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public String[] list() {
        return file.list();
    }

    @Override
    public String[] list(final FilenameFilter filter) {
        final FsFile self = this;
        return file.list((dir, name) -> filter.accept(self, name));
    }

    @Override
    public FsFile[] listFiles(final FilenameFilter filter) {
        if (file == null || !file.isDirectory()) return new FsFile[0];

        List<FsFile> filteredFiles = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null) {
            for (File file : files) {
                if (filter == null || filter.accept(this, file.getName())) {
                    filteredFiles.add(new FsFile(file));
                }
            }
        }
        return filteredFiles.toArray(new FsFile[0]);
    }

    @Override
    public long getFreeSpace() {
        return file.getFreeSpace();
    }

    @Override
    public InputStream openFileInputStream() throws IOException {
        return new FileInputStream(file);
    }
}
