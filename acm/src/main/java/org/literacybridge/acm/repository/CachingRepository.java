package org.literacybridge.acm.repository;

import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A repository implementation that stores .a18 files in a shared repository,
 * and all other files in a local cache.
 */
public class CachingRepository implements FileRepositoryInterface {
    private final FileRepositoryInterface localCacheRepository;
    private final FileRepositoryInterface globalSharedRepository;
    private final FileRepositoryInterface sandboxRepository;

    public CachingRepository(FileRepositoryInterface localCacheRepository,
        FileRepositoryInterface globalSharedRepository,
        FileRepositoryInterface sandboxRepository)
    {
        this.localCacheRepository = localCacheRepository;
        this.globalSharedRepository = globalSharedRepository;
        this.sandboxRepository = sandboxRepository;
    }

    @Override
    public File resolveFile(AudioItem audioItem, AudioFormat format, boolean writeAccess) {
        if (format == AudioFormat.A18) {
            File f;
            if (sandboxRepository == null)
                f = globalSharedRepository.resolveFile(audioItem, format, writeAccess);
            else if (writeAccess) {
                f = sandboxRepository.resolveFile(audioItem, format, writeAccess);
            } else {
                // read-access: check sandbox first; if missing, check shared repo
                f = sandboxRepository.resolveFile(audioItem, format, writeAccess);
                if (!f.exists() || (f.isDirectory() && f.listFiles().length == 0)) {
                    // empty directory
                    f = globalSharedRepository.resolveFile(audioItem, format, writeAccess);
                }
            }
            return f;
        } else {
            return localCacheRepository.resolveFile(audioItem, format, writeAccess);
        }
    }

    @Override
    public FileSystemGarbageCollector.GCInfo getGcInfo() throws IOException {
        return localCacheRepository.getGcInfo();
    }

    @Override
    public void gc() throws IOException {
        // only perform garbage collection on local cache, not on shared repo
        localCacheRepository.gc();
    }

    @Override
    public List<String> getAudioItemIds(Repository repo) {
        switch (repo) {
        case global:
            return globalSharedRepository.getAudioItemIds(repo);
        case cache:
            return localCacheRepository.getAudioItemIds(repo);
        case sandbox:
            return (sandboxRepository != null) ? sandboxRepository.getAudioItemIds(repo) : new ArrayList<>();
        }
        return null;
    }

    @Override
    public void delete(String id) {
        // This is a bit odd. Delete from the cache no matter what. If there is a sandbox,
        // delete from it. If no sandbox, delete from global. Thus, to delete from both global
        // and sandbox, this must be done with and without a sandbox.
        localCacheRepository.delete(id);
        if (sandboxRepository != null) {
            sandboxRepository.delete(id);
        } else {
            globalSharedRepository.delete(id);
        }
    }

    @Override
    public long size(String id) {
        long size = localCacheRepository.size(id);
        if (sandboxRepository != null) {
            size += sandboxRepository.size(id);
        } else {
            size += globalSharedRepository.size(id);
        }
        return size;
    }
}
