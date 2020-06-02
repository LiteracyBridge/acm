package org.literacybridge.acm.repository;

import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A repository implementation that stores .a18 files in a shared repository,
 * and all other files in a local cache.
 */
class CachingRepository implements FileRepositoryInterface {
    private final FileRepositoryInterface localCacheRepository;
    private final FileRepositoryInterface globalSharedRepository;
    private final FileRepositoryInterface sandboxRepository;
    private final Set<AudioFormat> nativeFormats;

    public CachingRepository(FileRepositoryInterface localCacheRepository,
                             FileRepositoryInterface globalSharedRepository,
                             FileRepositoryInterface sandboxRepository,
                             Collection<AudioFormat> nativeFormats)
    {
        this.localCacheRepository = localCacheRepository;
        this.globalSharedRepository = globalSharedRepository;
        this.sandboxRepository = sandboxRepository;
        this.nativeFormats = new HashSet<>();

        this.nativeFormats.addAll(nativeFormats);
    }

    private boolean isNativeFormat(AudioFormat format) {
        return nativeFormats.contains(format);
    }

    @Override
    public File resolveFile(AudioItem audioItem, AudioFormat format, boolean writeAccess) {
        if (isNativeFormat(format)) {
            File f;
            if (sandboxRepository == null) {
                // If no sandbox, file must be in the global shared repository, or doesn't exist.
                f = globalSharedRepository.resolveFile(audioItem, format, writeAccess);
                if (writeAccess) {
                    System.out.printf("From global: %s is %s\n", audioItem.getTitle(), f);
                }
            } else if (writeAccess) {
                // Have a sandbox, and want to write, so put into sandbox.
                f = sandboxRepository.resolveFile(audioItem, format, writeAccess);
                System.out.printf("From sandbox: %s is %s\n", audioItem.getTitle(), f);
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
            // Not a format we persist, so it must be in local cache, or doesn't exist.
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
