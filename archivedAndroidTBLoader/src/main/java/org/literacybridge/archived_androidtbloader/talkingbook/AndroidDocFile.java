package org.literacybridge.archived_androidtbloader.talkingbook;

import static org.literacybridge.core.fs.TbFile.Flags.append;

import android.content.ContentResolver;

import androidx.documentfile.provider.DocumentFile;

import org.literacybridge.core.fs.TbFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is an implementation of TbFile that wraps the Android DocumentFile. The DocumentFile is
 * a performance train wreck. We might eke out a bit more performance by caching lists of child
 * files, rather than always asking the OS.
 */

public class AndroidDocFile extends TbFile {

    private AndroidDocFile parent;
    private String filename;
    private DocumentFile file;
    private ContentResolver resolver;

    /**
     * Creates a "root" AndroidDocFile.
     * @param file The DocFile.
     * @param resolver The ContentResolver for the docfile and it's children.
     */
    AndroidDocFile(DocumentFile file, ContentResolver resolver) {
        this.parent = null;
        this.filename = null;
        this.file = file;
        this.resolver = resolver;
    }

    /**
     * Private constructor for when we don't know the child file, or it doesn't exist yet.
     * @param parent The parent of this new file.
     * @param child The name of this new file.
     */
    private AndroidDocFile(AndroidDocFile parent, String child) {
        this.parent = parent;
        this.filename = child;
        this.file = null;
        this.resolver = parent.resolver;
    }

    /**
     * Private constructor for when we already have the DocumentFile.
     * @param parent Parent directory of the new file.
     * @param child The new DocumentFile.
     */
    private AndroidDocFile(AndroidDocFile parent, DocumentFile child) {
        this.parent = parent;
        this.filename = child.getName();
        this.file = child;
        this.resolver = parent.resolver;
    }

    /**
     * Attempts to "resolve" the actual DocumentFile for this wrapper object. Resolves
     * parent first.
     */
    private void resolve() {
        if (file == null) {
            parent.resolve();
            if (parent.file != null) {
                file = parent.file.findFile(this.filename);
            }
        }
    }

    @Override
    public AndroidDocFile open(String child) {
        return new AndroidDocFile(this, child);
    }

    @Override
    public AndroidDocFile getParent() {
        return parent;
    }

    @Override
    public String getName() {
        return filename;
    }

    @Override
    public String getAbsolutePath() {
        resolve();
        return file.getUri().getPath();
    }

    @Override
    public void renameTo(String newName) {
        resolve();
        if (file != null) {
            file.renameTo(newName);
            filename = newName;
        }
    }

    @Override
    public boolean exists() {
        resolve();
        return file != null;
    }

    @Override
    public boolean isDirectory() {
        resolve();
        return file != null && file.isDirectory();
    }

    @Override
    public boolean mkdir() {
        resolve();
        // Is there already a file or directory here? Can't create one.
        if (file != null) return false;
        // Is there a parent? If not, can't create this child.
        if (parent.file == null) return false;
        file = parent.file.createDirectory(filename);
        return file != null;
    }

    @Override
    public boolean mkdirs() {
        resolve();
        // Is there already a file or directory here? Can't create one.
        if (file != null) return false;
        // If there's no parent, try to create it.
        if (parent.file == null) {
            boolean parentOk = parent.mkdirs();
            if (!parentOk) return false;
        }
        // See if the directory already exists.
        file = parent.file.findFile(filename);
        // TODO: throw error if exists as a file?
        // If not, create it.
        if (file == null)
            file = parent.file.createDirectory(filename);
        return file != null;
    }

    @Override
    public void createNew(InputStream content, Flags... flags) throws IOException {
        boolean appendToExisting = Arrays.asList(flags).contains(append);
        String streamFlags = appendToExisting ? "wa" : "w";
        resolve();
        if (file == null) {
            file = parent.file.createFile("application/octet-stream", filename);
        }
        try (OutputStream out = resolver.openOutputStream(file.getUri(), streamFlags) ) {
            copy(content, out);
        }
    }

    @Override
    public OutputStream createNew(Flags... flags) throws IOException {
        boolean appendToExisting = Arrays.asList(flags).contains(append);
        String streamFlags = appendToExisting ? "wa" : "w";
        resolve();
        if (file == null) {
            file = parent.file.createFile("application/octet-stream", filename);
        }
        return resolver.openOutputStream(file.getUri(), streamFlags);
    }

    @Override
    public boolean delete() {
        resolve();
        // If no file, consider it successfully deleted.
        if (file == null) return true;
        // Otherwise try to delete, and if successful, null out the file handle.
        boolean result = file.delete();
        if (result) file = null;
        return result;
    }

    @Override
    public long length() {
        resolve();
        if (file != null) return file.length();
        return 0;
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

    @Override
    public String[] list() {
        return list(null);
    }

    @Override
    public String[] list(FilenameFilter filter) {
        resolve();
        if (file == null || !file.isDirectory()) return new String[0];

        List<String> fileNames = new ArrayList<>();
        DocumentFile[] files = file.listFiles();
        for (DocumentFile file : files) {
            String name = file.getName();
            if (name == null) {
                // We used to log these, but there was no useful information. This is
                // probably the same as the !exists() check in listFiles.
            } else {
                if (filter == null || filter.accept(this, file.getName())) {
                    fileNames.add(file.getName());
                }
            }
        }
        return fileNames.toArray(new String[fileNames.size()]);
    }

    @Override
    public AndroidDocFile[] listFiles(final FilenameFilter filter) {
        resolve();
        if (file == null || !file.isDirectory()) return new AndroidDocFile[0];

        List<AndroidDocFile> filteredFiles = new ArrayList<>();
        DocumentFile[] files = file.listFiles();
        for (DocumentFile file : files) {
            // The "exists" is to deal with a situation in Android 5.1 where the listFiles() call returns
            // a file named ".android_secure", but file.exists() is false.
            // Jeez, Google, how the hell is anybody supposed to deal with stunts like this?
            if (file.exists() && (filter == null || filter.accept(this, file.getName()))) {
                filteredFiles.add(new AndroidDocFile(this, file));
            }
        }
        return filteredFiles.toArray(new AndroidDocFile[filteredFiles.size()]);
    }

    @Override
    public long getFreeSpace() {
        return 0;
    }

    @Override
    public InputStream openFileInputStream() throws IOException {
        return resolver.openInputStream(file.getUri());
    }
}
