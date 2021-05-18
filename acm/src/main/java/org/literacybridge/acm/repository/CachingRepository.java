package org.literacybridge.acm.repository;

import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A repository implementation that stores .a18 files in a shared repository,
 * and all other files in a local cache.
 */
class CachingRepository implements FileRepositoryInterface {
    private final GarbageCollectedFileSystemRepository localCacheRepository;
    private final SandboxingRepository sandboxingRepository;
    private final Set<AudioFormat> nativeFormats;
    final private File contentDir;

    public CachingRepository(DBConfiguration dbConfiguration) throws IOException {
        long cacheSizeInBytes = dbConfiguration.getCacheSizeInBytes();
        // The localCacheRepository lives in ~/LiteracyBridge/ACM/cache/ACM-FOO. It is used for all
        // non-A18 files. When .wav files (but not, say, mp3s) exceed max cache size, they'll be gc-ed.
        // The localCacheRepository lives in ~/LiteracyBridge/ACM/cache/ACM-FOO. It is used for all
        // non-A18 files. When .wav files (but not, say, mp3s) exceed max cache size, they'll be gc-ed.
        // Create the cache directory before it's actually needed, to trigger any security exceptions.
        File cacheDir = dbConfiguration.getLocalCacheDirectory();
        if ((!cacheDir.exists() && !cacheDir.mkdirs())) {
            throw new IOException("Can't create cache directory.");
        }
        localCacheRepository = new GarbageCollectedFileSystemRepository(cacheDir, cacheSizeInBytes);

        // The SandboxingRepository is backed by a Sandbox, so that changes can be accumulated
        // and committed when the user desides to "Save Changes".
        contentDir = dbConfiguration.getProgramContentDir();
        sandboxingRepository = new SandboxingRepository(dbConfiguration.getSandbox(), contentDir);

        // The caching repository directs resolve requests to one of the three above file based
        // repositories.
        nativeFormats = dbConfiguration.getNativeAudioFormats()
            .stream()
            .map(AudioItemRepositoryImpl::audioFormatForExtension)
            .collect(Collectors.toSet());
    }

    public synchronized void setupWavCaching(Predicate<Long> gcQuery) throws IOException {
        localCacheRepository.setupWavCaching(gcQuery);
    }

    private boolean isNativeFormat(AudioFormat format) {
        return nativeFormats.contains(format);
    }

    public boolean isSandboxedFile(File file) {
        return sandboxingRepository.isSandboxedFile(file);
    }

    @Override
    public File resolveFile(AudioItem audioItem, AudioFormat format, boolean writeAccess) {
        if (isNativeFormat(format)) {
            return sandboxingRepository.resolveFile(audioItem, format, writeAccess);
        } else {
            // Not a format we persist, so it must be in local cache, or doesn't exist.
            return localCacheRepository.resolveFile(audioItem, format, writeAccess);
        }
    }

    @Override
    public List<String> getAudioItemIds() {
        List<String> result = new ArrayList<>();
        result.addAll(localCacheRepository.getAudioItemIds());
        result.addAll(sandboxingRepository.getAudioItemIds());
        return result;
    }

    @Override
    public void delete(String id) {
        localCacheRepository.delete(id);
        sandboxingRepository.delete(id);
    }

    @Override
    public long size(String id) {
        long size = localCacheRepository.size(id);
        size += sandboxingRepository.size(id);
        return size;
    }

    @Override
    public Path basePath() {
        return this.contentDir.toPath();
    }


}
