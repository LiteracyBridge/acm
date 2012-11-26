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
	
	public CachingRepository(AudioItemRepository localCacheRepository,
							 AudioItemRepository sharedRepository) {
		this.localCacheRepository = localCacheRepository;
		this.sharedRepository = sharedRepository;
	}

	@Override
	protected File resolveFile(AudioItem audioItem, AudioFormat format) {
		if (format == AudioFormat.A18) {
			return sharedRepository.resolveFile(audioItem, format);
		}
		
		return localCacheRepository.resolveFile(audioItem, format);
	}
	
	@Override
	protected void gc() throws IOException {
		// only perform garbage collection on local cache, not on shared repo
		localCacheRepository.gc();
	}
	
}
