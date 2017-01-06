package org.literacybridge.androidtbloader.util;

import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;

import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.signin.UserHelper;

/**
 * Created by bill on 11/11/16.
 */

public class S3Helper {
    private static final String TAG = "S3Helper";

    // We only need one instance of the clients and credentials provider
    private static AmazonS3Client sS3Client;
    private static TransferUtility sTransferUtility;

    private static TBLoaderAppContext sApplicationContext;

    public static void init(TBLoaderAppContext applicationContext) {
        sApplicationContext = applicationContext;
    }

    /**
     * Gets an instance of a S3 client which is constructed using the given
     * Context.
     *
     * @return A default S3 client.
     */
    public static AmazonS3Client getS3Client() {
        if (sS3Client == null) {
            sS3Client = new AmazonS3Client(UserHelper.getCredentialsProvider(sApplicationContext));
        }
        return sS3Client;
    }

    /**
     * Gets an instance of the TransferUtility which is constructed using the
     * given Context
     *
     * @return a TransferUtility instance
     */
    public static TransferUtility getTransferUtility() {
        if (sTransferUtility == null) {
            sTransferUtility = new TransferUtility(getS3Client(), sApplicationContext);
        }

        return sTransferUtility;
    }


    public interface ListObjectsListener {
        void onSuccess(ListObjectsV2Result result);
        void onFailure(Exception ex);
    }


    public static void listObjects(final ListObjectsV2Request request, final ListObjectsListener resultListener) {
        new AsyncTask<ListObjectsV2Request, Void, ListObjectsV2Result>() {
            Exception thrownException = null;

            @Override
            protected ListObjectsV2Result doInBackground(ListObjectsV2Request... inputs) {
                ListObjectsV2Result result = null;
                try {
                    Log.d(TAG, "about to call listObjectsV2");
                    // Queries files in the bucket from S3. Only returns 1000 entries.
                    result = getS3Client().listObjectsV2(request);
                    Log.d(TAG, "back from call to listObjectsV2");
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
