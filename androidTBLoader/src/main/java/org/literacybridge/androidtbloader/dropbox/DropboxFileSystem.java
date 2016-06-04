package org.literacybridge.androidtbloader.dropbox;

import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;

import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TBFileSystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class DropboxFileSystem extends TBFileSystem {
    private final int DEFAULT_NUM_ENTRIES = -1;

    private final DropboxAPI<AndroidAuthSession> mApi;
    private final DropboxAPI.Entry root;
    private final RelativePath rootPath;

    public DropboxFileSystem(DropboxAPI<AndroidAuthSession> mApi,
                             String rootPath) throws IOException {
        this.mApi = mApi;
        try {
            this.root = mApi.metadata(rootPath, DEFAULT_NUM_ENTRIES, null, false, null);
        } catch (DropboxException e) {
            throw new IOException(e);
        }
        this.rootPath = RelativePath.parse(rootPath);
    }

    @Override
    public boolean isDirectory(RelativePath relativePath) throws IOException {
        try {
            DropboxAPI.Entry file = mApi.metadata(resolve(relativePath), 1, null, false, null);
            return file.isDir;
        } catch (DropboxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean fileExists(RelativePath relativePath) throws IOException {
        try {
            String path = resolve(relativePath);
            Log.d("dropbox", "fileExists: " + path);
            DropboxAPI.Entry file = mApi.metadata(path, 1, null, false, null);
            return file != null && !file.isDeleted;
        } catch (DropboxException e) {
            String ex = e.toString();
            Log.d("dropbox", "exception: " + ex);
            if (ex != null && ex.contains("404 Not Found")) {
                return false;
            }
            throw new IOException(e);
        }
    }

    @Override
    public long fileLength(RelativePath relativePath) throws IOException {
        try {
            DropboxAPI.Entry file = mApi.metadata(resolve(relativePath), 1, null, false, null);
            return file.bytes;
        } catch (DropboxException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void doCreateNewFile(RelativePath relativePath, InputStream content) throws IOException {
        final int MAX_RETRIES = 5;

        try {
            DropboxAPI.ChunkedUploader uploader = mApi.getChunkedUploader(content, -1);
            int retryCounter = 0;
            while(!uploader.isComplete()) {
                try {
                    uploader.upload();
                } catch (DropboxException e) {
                    if (retryCounter > MAX_RETRIES) {
                        break;  // Give up after a while.
                    }
                    retryCounter++;
                }
            }
            uploader.finish(resolve(relativePath), null);
        } catch (DropboxException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void doCreateNewDirectory(RelativePath relativePath) throws IOException {
        try {
            mApi.createFolder(resolve(relativePath));
        } catch (DropboxException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void doDeleteFile(RelativePath relativePath) throws IOException {
        try {
            mApi.delete(resolve(relativePath));
        } catch (DropboxException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void doDeleteDirectory(RelativePath relativePath) throws IOException {
        try {
            mApi.delete(resolve(relativePath));
        } catch (DropboxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public InputStream openFileInputStream(RelativePath relativePath) throws IOException {
        try {
            return mApi.getFileStream(resolve(relativePath), null);
        } catch (DropboxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public OutputStream openFileOutputStream(RelativePath relativePath) throws IOException {
        throw new UnsupportedOperationException("The Dropbox API does not support file appends. "
                + "Use createNewFile() instead.");
    }

    @Override
    public String[] list(RelativePath relativePath) throws IOException {
        return list(relativePath, null);
    }

    @Override
    public String[] list(RelativePath relativePath, FilenameFilter filter) throws IOException {
        try {
            DropboxAPI.Entry dir = mApi.metadata(resolve(relativePath), DEFAULT_NUM_ENTRIES, null, true, null);
            if (dir == null) {
                throw new FileNotFoundException("File not found: " + relativePath);
            }
            if (dir.contents == null || dir.contents.size() == 0) {
                return new String[0];
            }
            List<String> files = new ArrayList<String>();
            for (DropboxAPI.Entry file : dir.contents) {
                if (filter == null || filter.accept(this, relativePath, file.fileName())) {
                    files.add(file.fileName());
                }
            }

            return files.toArray(new String[files.size()]);
        } catch (DropboxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean isHidden(RelativePath relativePath) throws IOException {
        return false;
    }

    @Override
    public String getRootPath() {
        return root.path;
    }

    private final String resolve(RelativePath relativePath) {
        return "/" + RelativePath.concat(rootPath, relativePath).asString();
    }
}
