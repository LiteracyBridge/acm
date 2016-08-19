package org.literacybridge.acm.utils;

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
import java.lang.StringBuilder;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

/**
 * AcmActionLogger class
 *
 * Used to track & record user changes to the ACM for auditing and diagnostic purposes.
 * Uploads logs to s3 bucket: 'acm-logging' --> records/date-time
 *
 * @author  Nihala Thanikkal
 * @since   2016-07-25
 */


public class AcmActionLogger {
    private static StringBuilder sb = new StringBuilder(2000);

    // create a file to batch up records: 'LiteracyBridge/record.log'
    private static File getBatchRecord() {
        return new File(DBConfiguration.getLiteracyBridgeHomeDir(), Constants.BATCH_RECORD);
    }

    private static void deleteBatchRecord(){
        File f = getBatchRecord();
        f.delete();
    }

    // write batch record from current session memory to disk
    private static void writeBatchToFile(){
        try (
                FileWriter fw = new FileWriter(getBatchRecord(), true);    //append to file
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)){
                out.print(sb.toString());
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to write to file: " + getBatchRecord(), e);
        }
    }

    /**
     * Deletes the batched up records stored in memory from the current user session.
     * NOTE: did not implement yet --> not needed for discard or denied check-in because the program is terminated right
     * after and so the memory is cleared. not sure where to implement this for creating/publishing deployments in the
     * TBBuilder
     */
    public static void clearBatchMemory(){
        sb.delete(0, sb.length());
    }

    /**
     * Records changes made to the acm (with timestamp, user, & action taken) & adds it to the batch record in memory.
     *
     * @param action string  a description of what acm modification was made by the user
     *                       (e.g. 'Added audioitem: ITEM_NAME')
     */
    public static void recordAction(String action) {
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
        String timestamp = formatter.format(today);
        String acm = ACMConfiguration.getInstance().getCurrentDB().getSharedACMname();
        String user = ACMConfiguration.getInstance().getUserName();
        String record = timestamp + ","+ acm + ",'" + user + "'," + action + "\n";
        sb.append(record);
    }

    /**
     * Creates a local batch file from logs stored in application memory. Uploads file to s3 bucket: 'acm-logging' in
     * subdirectory 'records' with the datetime stamp as the s3 object name.
     * Deletes local copy only on successful upload. Otherwise, local copy is kept and appended to on next use of acm.
     */
    public static void uploadRecord(){
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
        String fileName = formatter.format(today);
        String bucketName = Constants.S3_BUCKET;
        String keyName = "records/"+fileName;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        AmazonS3 s3client = new AmazonS3Client(new ProfileCredentialsProvider());

        writeBatchToFile();
        // early return to prevent creating empty logs in s3
        if (getBatchRecord().length() == 0){
            return;
        }

        try {
            // NOTE: file size limit for single put object request 5GB; recommended limit 300MB
            s3client.putObject(new PutObjectRequest(bucketName, keyName, getBatchRecord()).withMetadata(metadata));
            // delete record because it was uploaded to s3
            deleteBatchRecord();
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
