package org.literacybridge.talkingbookapp.util.content_manager

import com.amplifyframework.core.Consumer
import com.amplifyframework.storage.StorageAccessLevel
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.s3.configuration.AWSS3PluginPrefixResolver


// Define your own prefix resolver, that conforms to `AWSS3StoragePluginPrefixResolver`
class CustomS3PathResolver : AWSS3PluginPrefixResolver {
    override fun resolvePrefix(
        accessLevel: StorageAccessLevel,
        targetIdentity: String?,
        onSuccess: Consumer<String>,
        onError: Consumer<StorageException>?

    ) {
        onSuccess.accept("")
    }
}