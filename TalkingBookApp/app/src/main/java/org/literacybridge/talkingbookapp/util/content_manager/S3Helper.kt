package org.literacybridge.talkingbookapp.util.content_manager

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ListObjectsV2Result
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.auth.CognitoCredentialsProvider
import com.amplifyframework.core.Amplify
import org.literacybridge.talkingbookapp.App
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobile.config.AWSConfiguration


//import com.amazonaws.services.s3.model.ListObjectsV2Request;
/**
 * Common code for S3.
 */
object S3Helper {
    private const val TAG = "TBL!:" + "S3Helper"

    // We only need one instance of the clients and credentials provider
    private val sS3Client: AmazonS3Client? = null
    private var sTransferUtility: TransferUtility? = null
    private var sApplicationContext: App? = null
    fun init(applicationContext: App?) {
        sApplicationContext = applicationContext
    }

    @OptIn(InternalAmplifyApi::class)
    val s3Client: S3Client
        /**
         * Gets an instance of a S3 client which is constructed using the given
         * Context.
         *
         * @return A default S3 client.
         */
        get() {
//            val credentialsProvider = CognitoCredentialsProvider(
//                "us-west-2_3evpQGyi5",
//                "5oviumtu4cmhspn9qt2bvn130s",
//                // You can leave refresh token blank if not using refresh flow
//                refreshToken = "",
//                "us-west-2"
//            )
//            val cong = AWSCognitoIdentityProvider()
            val config = AWSConfiguration(App.context)
            val credentialsProvider = CognitoCachingCredentialsProvider(
                App.context,
                config,
//                "us-west-2:a544b58b-8be0-46db-aece-e6fe14d29124",
//                "us-west-2"
            )
//            val cred = CredentialsProvider()
//                        AWSCredentialsProvider credentialsProvider = crede


// 2. Configure S3 Client
            val s3Client = S3Client {
                this.region = "us-west-2"
//                this.credentialsProvider = credentialsProvider
//                this.credentialsProvider = CognitoCredentialsProvider(
//                    Amplify.Auth
//                )
//                credentialsProvider = credentialsProvider
            }

            return s3Client

//            val plugin = Amplify.Storage.getPlugin("awsS3StoragePlugin") as AWSS3StoragePlugin
//            return plugin.escapeHatch
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

    @JvmStatic
    val transferUtility: TransferUtility?
        /**
         * Gets an instance of the TransferUtility which is constructed using the
         * given Context
         *
         * @return a TransferUtility instance
         */
        get() {
            if (sTransferUtility == null) {
                sTransferUtility = TransferUtility(s3Client as AmazonS3, sApplicationContext)
            }
            return sTransferUtility
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
//    @JvmStatic
    fun listObjects(request: ListObjectsV2Request?, resultListener: ListObjectsListener) {
//        async{
//            result = s3Client.listObjectsV2(request!!)
//        }

//        object : AsyncTask<ListObjectsV2Request?, Void?, ListObjectsV2Result?>() {
//            var thrownException: Exception? = null
//            protected override fun doInBackground(vararg params: ListObjectsV2Request?): ListObjectsV2Result? {
//                var result: ListObjectsV2Response? = null
//                try {
//                    // Queries files in the bucket from S3. Only returns 1000 entries.
//                    result = s3Client.listObjectsV2(request!!)
//                } catch (e: Exception) {
//                    thrownException = e
//                }
//                return result
//            }
//
//            override fun onPostExecute(result: ListObjectsV2Result?) {
//                if (thrownException != null) {
//                    resultListener.onFailure(thrownException)
//                } else {
//                    resultListener.onSuccess(result)
//                }
//            }
//        }.execute()
    }

    interface ListObjectsListener {
        fun onSuccess(result: ListObjectsV2Result?)
        fun onFailure(ex: Exception?)
    }
}
