package org.literacybridge.androidtbloader.talkingbook;

import android.content.ContentResolver;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TBFileSystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class AndroidTBFileSystem extends TBFileSystem {
    private static final String TAG = AndroidTBFileSystem.class.getSimpleName();

    private final DocumentFile root;
    private final ContentResolver contentResolver;

    public AndroidTBFileSystem(ContentResolver contentResolver, DocumentFile root) {
        this.contentResolver = contentResolver;
        this.root = root;
    }

    @Override
    public boolean isDirectory(RelativePath relativePath) throws IOException {
        DocumentFile file = resolveRelativePath(relativePath);
        return file.isDirectory();
    }

    @Override
    public boolean fileExists(RelativePath relativePath) throws IOException {
        try {
            DocumentFile file = resolveRelativePath(relativePath);
            return file != null && file.exists();
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    @Override
    public long fileLength(RelativePath relativePath) throws IOException {
        DocumentFile file = resolveRelativePath(relativePath);
        return file.length();
    }

    @Override
    protected void doCreateNewFile(RelativePath relativePath, InputStream content) throws IOException {
        DocumentFile file = resolveRelativePath(relativePath, Action.CreateFile);
        OutputStream out = null;
        try {
            out = contentResolver.openOutputStream(file.getUri());
            copy(content, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    public void doCreateNewDirectory(RelativePath relativePath) throws IOException {
        resolveRelativePath(relativePath, Action.CreateDirectory);
    }

    @Override
    public void doDeleteFile(RelativePath relativePath) throws IOException {
        resolveRelativePath(relativePath, Action.Delete);
    }

    @Override
    protected void doDeleteDirectory(RelativePath relativePath) throws IOException {
        resolveRelativePath(relativePath, Action.Delete);
    }

    @Override
    public InputStream openFileInputStream(RelativePath relativePath) throws IOException {
        return contentResolver.openInputStream(resolveRelativePath(relativePath).getUri());
    }

    @Override
    public OutputStream openFileOutputStream(RelativePath relativePath) throws IOException {
        return contentResolver.openOutputStream(resolveRelativePath(relativePath, Action.CreateFile).getUri());
    }

    @Override
    public String[] list(RelativePath relativePath) throws IOException {
        return list(relativePath, null);
    }

    @Override
    public String[] list(RelativePath relativePath, FilenameFilter filter) throws IOException {
        DocumentFile[] files = resolveRelativePath(relativePath).listFiles();
        if (files == null || files.length == 0) {
            return new String[0];
        }
        List<String> fileNames = new ArrayList<String>();
        for (DocumentFile file : files) {
            RelativePath dir = RelativePath.getRelativePath(
                    root.getUri().getPath(), file.getUri().getPath());
            if (file.getName() != null && (filter == null || filter.accept(this, dir, file.getName()))) {
                fileNames.add(file.getName());
            }
        }
        return fileNames.toArray(new String[fileNames.size()]);
    }

    @Override
    public boolean isHidden(RelativePath relativePath) throws IOException {
        return false;
    }

    @Override
    public String getRootPath() {
        return root.getUri().getPath();
    }

    private DocumentFile resolveRelativePath(RelativePath relativePath) throws IOException {
        return resolveRelativePath(relativePath, Action.None);
    }

    private DocumentFile resolveRelativePath(RelativePath relativePath,
                                             Action action) throws IOException {
        Log.d(this.getClass().getName(), "Action: " + action.name() + ", RelativePath: " + relativePath.asString());
        DocumentFile parent = null;
        DocumentFile file = root;

        List<String> pathSegments = relativePath.getSegments();
        if (pathSegments == null || pathSegments.isEmpty()) {
            return file;
        }

        for (String segment : pathSegments) {
            if (file == null) {
                throw new FileNotFoundException("File " + relativePath + " not found.");
            }
            parent = file;
            file = parent.findFile(segment);
        }

        if (Action.None == action || Action.Delete == action) {
            if (action == Action.Delete) {
                if (file == null) {
                    throw new FileNotFoundException("File " + relativePath + " not found.");
                }

                file.delete();
                return null;
            }

            return file;
        }

        if (file != null && file.exists()) {
            return file;
        }

        if (action == Action.CreateFile) {
            file = parent.createFile("application/octet-stream", relativePath.getLastSegment());
        } else if (action == Action.CreateDirectory) {
            file = parent.createDirectory(relativePath.getLastSegment());
        }

        return file;
    }

    private enum Action {
        None,
        CreateFile,
        CreateDirectory,
        Delete
    }
}
