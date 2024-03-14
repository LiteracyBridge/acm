package org.literacybridge.talkingbookapp.util.content_manager;

import android.os.AsyncTask;


import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
//import com.amazonaws.services.s3.model.ListObjectsV2Request;

import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageItem;
import com.amplifyframework.storage.options.StoragePagedListOptions;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;

import org.literacybridge.talkingbookapp.App;

import aws.sdk.kotlin.services.s3.S3Client;

/**
 * Common code for S3.
 */

public class S3Helper {
    private static final String TAG = "TBL!:" + "S3Helper";

    // We only need one instance of the clients and credentials provider
    private static AmazonS3Client sS3Client;
    private static TransferUtility sTransferUtility;

    private static App sApplicationContext;

    public static void init(App applicationContext) {
        sApplicationContext = applicationContext;
    }

    /**
     * Gets an instance of a S3 client which is constructed using the given
     * Context.
     *
     * @return A default S3 client.
     */
    private static S3Client getS3Client() {
        AWSS3StoragePlugin plugin = (AWSS3StoragePlugin) Amplify.Storage.getPlugin("awsS3StoragePlugin");

        return plugin.getEscapeHatch();
//        if (sS3Client == null) {
//            ClientConfiguration config = new ClientConfiguration();
//            config.setConnectionTimeout(60 * 1000);
//            config.setSocketTimeout(60 * 1000);
//            config.setMaxErrorRetry(4);
//            AWSCredentialsProvider credentialsProvider = UserHelper.getInstance().getCredentialsProvider(sApplicationContext);
//            sS3Client = new AmazonS3Client(credentialsProvider, config);
//        }
//        return sS3Client;
    }

    /**
     * Gets an instance of the TransferUtility which is constructed using the
     * given Context
     *
     * @return a TransferUtility instance
     */
    public static TransferUtility getTransferUtility() {
        if (sTransferUtility == null) {
            sTransferUtility = new TransferUtility((AmazonS3) getS3Client(), sApplicationContext);
        }

        return sTransferUtility;
    }


    public interface ListObjectsListener {
        void onSuccess(ListObjectsV2Result result);

        void onFailure(Exception ex);
    }

//    public static void listFiles() {
//        StoragePagedListOptions options = StoragePagedListOptions.builder()
//                .setPageSize(1000)
//                .build();
//
//        Amplify.Storage.list(
//                "",
//                options,
//                result -> {
//                    for (StorageItem item : result.getItems()) {
//                        Log.i("MyAmplifyApp", "Item: " + item.getKey());
//                    }
//                    Log.i("MyAmplifyApp", "Next Token: " + result.getNextToken());
//                },
//                error -> Log.e("MyAmplifyApp", "List failure", error)
//        );
//    }

    public static void listObjects(final ListObjectsV2Request request, final ListObjectsListener resultListener) {
        new AsyncTask<ListObjectsV2Request, Void, ListObjectsV2Result>() {
            Exception thrownException = null;

            @Override
            protected ListObjectsV2Result doInBackground(ListObjectsV2Request... inputs) {
                ListObjectsV2Result result = null;
                try {
                    // Queries files in the bucket from S3. Only returns 1000 entries.
                    result = getS3Client().listObjectsV2(request)
                } catch (Exception e) {
                    thrownException = e;
                }
                return result;
            }

            @Override
            protected void onPostExecute(ListObjectsV2Result result) {
                if (thrownException != null) {
                    resultListener.onFailure(thrownException);
                } else {
                    resultListener.onSuccess(result);
                }
            }
        }.execute();
    }

}
