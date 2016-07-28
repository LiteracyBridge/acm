package org.literacybridge.acm.utils;

import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

/**
 * ACMRecorder class
 *
 * Used to track & record user changes to the ACM for auditing and diagnostic purposes.
 * Creates a local temp record file that is uploaded to s3 bucket: 'acm-logging' --> records/acm_name/date
 *
 * @author  Nihala Thanikkal
 * @since   2016-07-25
 */


public class ACMRecorder {

    // create temp record file: 'LiteracyBridge/record.log'
    private static File getTempRecord() {
        return new File(DBConfiguration.getLiteracyBridgeHomeDir(), Constants.TEMP_RECORD);
    }

    public static void deleteTempRecord(){
        File f = getTempRecord();
        f.delete();
    }

    // append actions as a new line in temp record
    public static void recordAction(String action) {
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
        String timestamp = formatter.format(today);
        String acm = ACMConfiguration.getInstance().getCurrentDB().getSharedACMname();
        String user = ACMConfiguration.getInstance().getUserName();
        try (
            FileWriter fw = new FileWriter(getTempRecord(), true);    //append to file
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)){
            out.println(timestamp + ","+ acm + ",'" + user + "'," + action);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to write record: " + getTempRecord(), e);
        }
    }

    // upload temp record file to s3 bucket & delete temp record locally
    // NOTE: file size limit for single put object request 5GB; recommended limit 300MB
    public static void uploadRecord(){
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
        String fileName = formatter.format(today);
        String bucketName = Constants.S3_BUCKET;
        String keyName = "records/"+fileName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        AmazonS3 s3client = new AmazonS3Client(new ProfileCredentialsProvider());
        try {
            System.out.println("Uploading a new object to S3 from a file\n");
            s3client.putObject(new PutObjectRequest(bucketName, keyName, getTempRecord()).withMetadata(metadata));
            // only delete if record is uploaded to s3
            deleteTempRecord();
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
                    "means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
                    "means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }
}
