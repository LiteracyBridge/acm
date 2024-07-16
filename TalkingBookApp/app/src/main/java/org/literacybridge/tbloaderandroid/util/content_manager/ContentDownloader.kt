package org.literacybridge.tbloaderandroid.util.content_manager
//
//import android.os.AsyncTask
//import android.util.Log
////import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
////import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver
////import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
//import com.amplifyframework.core.Amplify
//import com.amplifyframework.storage.s3.AWSS3StoragePlugin
//import org.literacybridge.core.fs.ZipUnzip
//import org.literacybridge.androidtbloader.util.PathsProvider
//import org.literacybridge.androidtbloader.util.content_manager.S3Helper.transferUtility
//import java.io.File
//import java.io.IOException
//import java.util.function.Supplier
//import kotlin.concurrent.Volatile
//
///**
// * Performs download of a Deployment
// */
//class ContentDownloader(
//    private val mContentInfo: ContentInfo,
//    private val mDownloadListener: DownloadListener
//) {
//    private var mObserver: TransferObserver? = null
//    private var mProjectDir: File? = null
//
//    @Volatile
//    private var mCancelRequested = false
//
//    /**
//     * Like TransferListener, but with onUnzipProgress added.
//     */
//    interface DownloadListener : TransferListener {
//        fun onUnzipProgress(id: Int, current: Long, total: Long)
//    }
//
//    val bytesTransferred: Long
//        /**
//         * Returns the number of bytes transferred so far. If the transfer hasn't started, or failed to
//         * start, returns 0.
//         * @return The number of bytes transferred.
//         */
//        get() = if (mObserver == null) 0 else mObserver!!.bytesTransferred
//    // Only deals with a single part transfer.
//    /**
//     * Cancels any active download.
//     */
//    fun cancel() {
//        mCancelRequested = true
//        transferUtility!!.cancel(mObserver!!.id)
//    }
//
//    /**
//     * Starts downloading the Deployment, and listens for status from S3.
//     */
//    fun start() {
////        new UnattendedAuthenticator(new GenericHandler() {
////            @Override
////            public void onSuccess() {
////                start2();
////            }
////
////            @Override
////            public void onFailure(Exception exception) {
////                mDownloadListener.onStateChanged(0, TransferState.FAILED);
////            }
////        }).authenticate();
//        start2()
//    }
//
//    private fun start2() {
//        mProjectDir = PathsProvider.getLocalContentProjectDirectory(mContentInfo.programId)
//        Log.d(TAG, "Starting download of content")
//
////        val plugin = Amplify.Storage.getPlugin("awsS3StoragePlugin") as AWSS3StoragePlugin
////       val client =  plugin.escapeHatch.listObjectsV2 {
////            this.bucket = s3BucketName
////            this.prefix = path
////        }
//
//        val listener: TransferListener = object : TransferListener {
//            override fun onStateChanged(id: Int, state: TransferState) {
//                if (state == TransferState.COMPLETED) {
//                    transferUtility!!.deleteTransferRecord(id)
//                    onDownloadCompleted(id, state)
//                } else if (state == TransferState.FAILED || state == TransferState.CANCELED) {
//                    transferUtility!!.deleteTransferRecord(id)
//                    mDownloadListener.onStateChanged(id, state)
//                } else {
//                    mDownloadListener.onStateChanged(id, state)
//                }
//            }
//
//            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
//                mDownloadListener.onProgressChanged(id, bytesCurrent, mContentInfo.size)
//            }
//
//            override fun onError(id: Int, ex: Exception) {
//                // After we return from this onError call, we'll get another call, to
//                // onStateChanged with TransferState.FAILED.
//
//                // TODO: If ex is AmazonClientException, with cause IOException, with cause
//                // ErrnoException, with errno 28, ENOSPC, then we need a different error code,
//                // because retry is pointless.
//                mDownloadListener.onError(id, ex)
//            }
//        }
//
//        // Where the file will be downloaded
//        val file = fileForDownload("content")
//        file.parentFile.mkdirs()
//        // Initiate the download
//        Log.d(
//            TAG,
//            String.format(
//                "Starting download of content, b: %s, k: %s",
//                mContentInfo.bucketName,
//                mContentInfo.key
//            )
//        )
//        mObserver = transferUtility!!.download(mContentInfo.bucketName, mContentInfo.key, file)
//        mObserver!!.setTransferListener(listener)
//    }
//
//    private class BackgroundDownloader(
//        private val id: Int,
//        private val state: TransferState,
//        private val downloadListener: DownloadListener,
//        private val contentInfo: ContentInfo,
//        private val downloadedZip: File,
//        private val projectDir: File?,
//        private val isCancelRequested: Supplier<Boolean>
//    ) : AsyncTask<Void?, Void?, Void?>() {
//        val t1 = System.currentTimeMillis()
//        private var thrown: Exception? = null
//        override fun doInBackground(vararg params: Void?): Void? {
//            Log.d(TAG, "Unzipping file")
//            try {
//                ZipUnzip.unzip(downloadedZip, downloadedZip.parentFile) { current, total ->
//                    downloadListener.onUnzipProgress(id, current, total)
//                    !isCancelRequested.get() // true: continue unzipping
//                }
//            } catch (ex: Exception) {
//                thrown = ex
//            }
//            return null
//        }
//
//        override fun onPostExecute(aVoid: Void?) {
//            if (thrown != null) {
//                Log.d(TAG, String.format("Exception unzipping %s", downloadedZip.name), thrown)
//                // Simulate an S3 exception, by onError() then onStateChanged().
//                downloadListener.onError(id, thrown)
//                downloadListener.onStateChanged(id, TransferState.FAILED)
//                return
//            }
//            if (isCancelRequested.get()) {
//                // Simulate download cancelled. We can only be here if the actual last state was
//                // COMPLETED. We know that the transfer has already been deleted.
//                downloadListener.onStateChanged(id, TransferState.CANCELED)
//                return
//            }
//            Log.d(
//                TAG, String.format(
//                    "Unzipped %s, %dms",
//                    downloadedZip.name,
//                    System.currentTimeMillis() - t1
//                )
//            )
//            // Create the mVersion marker. Like DEMO-2017-2-a.current
//            val filename = contentInfo.versionedDeployment + ".current"
//            val marker = File(projectDir, filename)
//            try {
//                marker.createNewFile()
//                downloadedZip.delete()
//            } catch (e: IOException) {
//                // This really should never happen. But if it does, we won't recognize this downloaded content.
//                // If we leave it, we'll clean it up next time. Return the error, and leave the mess 'til then.
//                downloadListener.onError(id, e)
//                return
//            }
//            downloadListener.onStateChanged(id, state)
//        }
//    }
//
//    /**
//     * Helper to unzip the downloaded content on a background thread. Creates a file like
//     * "DEMO-2017-2-a.current" on success.
//     * @param id The id of the completed transfer.
//     * @param state The new download state. Always TransferState.COMPLETED
//     */
//    private fun onDownloadCompleted(id: Int, state: TransferState) {
//        val t1 = System.currentTimeMillis()
//
//        // The file is like content-2016-03-c.zip, and contains a top level directory "content".
//        val downloadedZip = fileForDownload("content")
//        BackgroundDownloader(
//            id,
//            state,
//            mDownloadListener,
//            mContentInfo,
//            downloadedZip,
//            mProjectDir
//        ) { mCancelRequested }.execute()
//        // Unzip the content. This is lengthy, so use an async thread.
////        new AsyncTask<Void, Void, Void>() {
////            Exception thrown = null;
////
////            @Override
////            protected Void doInBackground(Void... params) {
////                Log.d(TAG, "Unzipping file");
////                try {
////                    ZipUnzip.unzip(downloadedZip, downloadedZip.getParentFile(), new ZipUnzip.UnzipListener() {
////                        @Override
////                        public boolean progress(long current, long total) {
////                            mDownloadListener.onUnzipProgress(id, current, total);
////                            return !mCancelRequested; // true: continue unzipping
////                        }
////                    });
////                } catch (Exception ex) {
////                    thrown = ex;
////                }
////                return null;
////            }
////
////            @Override
////            protected void onPostExecute(Void result) {
////                if (thrown != null) {
////                    Log.d(TAG, String.format("Exception unzipping %s", downloadedZip.getName()), thrown);
////                    // Simulate an S3 exception, by onError() then onStateChanged().
////                    mDownloadListener.onError(id, thrown);
////                    mDownloadListener.onStateChanged(id, TransferState.FAILED);
////                    return;
////                }
////
////                if (mCancelRequested) {
////                    // Simulate download cancelled. We can only be here if the actual last state was
////                    // COMPLETED. We know that the transfer has already been deleted.
////                    mDownloadListener.onStateChanged(id, TransferState.CANCELED);
////                    return;
////                }
////
////                Log.d(TAG,
////                    String.format("Unzipped %s, %dms",
////                        downloadedZip.getName(),
////                        System.currentTimeMillis() - t1));
////                // Create the mVersion marker. Like DEMO-2017-2-a.current
////                String filename = mContentInfo.getVersionedDeployment() + ".current";
////                File marker = new File(mProjectDir, filename);
////                try {
////                    marker.createNewFile();
////                    // TODO: Delete the .zip file!
////                } catch (IOException e) {
////                    // This really should never happen. But if it does, we won't recognize this downloaded content.
////                    // If we leave it, we'll clean it up next time. Return the error, and leave the mess 'til then.
////                    mDownloadListener.onError(id, e);
////                    return;
////                }
////                mDownloadListener.onStateChanged(id, state);
////            }
////        }.execute();
//    }
//
//    /**
//     * Helper to generate the filename for a download component.
//     * @param component The component being downloaded; currently "content"
//     * @return The name of the object in s3, like "content-DEMO-2017-2-a"
//     */
//    private fun fileForDownload(component: String): File {
//        val filename = component + "-" + mContentInfo.versionedDeployment + ".zip"
//        return File(mProjectDir, filename)
//    }
//
//    companion object {
//        private const val TAG = "TBL!:" + "ContentDownloader"
//    }
//}
