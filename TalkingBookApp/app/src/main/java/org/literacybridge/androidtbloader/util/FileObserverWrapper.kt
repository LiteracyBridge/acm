package org.literacybridge.androidtbloader.util

import android.os.Build
import android.os.FileObserver
import java.io.File


/**
 * A wrapper around FileObserver to support older API levels.
 * Adapted from: https://stackoverflow.com/a/75917948/7125294
 */
open class FileObserverWrapper(path: String, mask: Int) {
    private var fileObserver: FileObserver? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            fileObserver = object : FileObserver(File(path), mask) {
                override fun onEvent(event: Int, path: String?) {
                    this@FileObserverWrapper.onEvent(event,path)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            fileObserver = object : FileObserver(path, mask) {
                override fun onEvent(event: Int, path: String?) {
                    this@FileObserverWrapper.onEvent(event,path)
                }
            }
        }
    }

    /**
     * @description does nothing, can be overridden. Equivalent to FileObserver.onEvent
     */
    open fun onEvent(event: Int, path: String?){}

    open fun startWatching() {
        fileObserver?.startWatching()
    }

    open fun stopWatching() {
        fileObserver?.stopWatching()
    }
}