package org.literacybridge.acm.repository;

import java.io.File;
import java.io.IOException;

import org.literacybridge.acm.content.AudioItem;

/**
 * A repository implementation that stores .a18 files in a shared repository,
 * and all other files in a local cache.
 */
public class CachingRepository extends AudioItemRepository {
	private final AudioItemRepository localCacheRepository;
	private final AudioItemRepository sharedRepository;
	private final AudioItemRepository sandboxRepository;
	
	public CachingRepository(AudioItemRepository localCacheRepository,
							 AudioItemRepository sharedRepository,
							 AudioItemRepository sandboxRepository) {
		this.localCacheRepository = localCacheRepository;
		this.sharedRepository = sharedRepository;
		this.sandboxRepository = sandboxRepository;
	}

	@Override
	protected File resolveFile(AudioItem audioItem, AudioFormat format, boolean writeAccess) {
		if (format == AudioFormat.A18) {
			File f;
			if (sandboxRepository == null)
				f = sharedRepository.resolveFile(audioItem, format, writeAccess);
			else if (writeAccess) {
				f = sandboxRepository.resolveFile(audioItem, format, writeAccess);
			} else {
				// read-access: check sandbox first; if missing, check shared repo 
				f = sandboxRepository.resolveFile(audioItem, format, writeAccess);
				if (!f.exists() || (f.isDirectory() && f.listFiles().length == 0)) {
					// empty directory
					f = sharedRepository.resolveFile(audioItem, format, writeAccess);
				}
			}
			return f;
		} else 
			return localCacheRepository.resolveFile(audioItem, format, writeAccess);
	}
	
	@Override
	protected void gc() throws IOException {
		// only perform garbage collection on local cache, not on shared repo
		localCacheRepository.gc();
	}
}
