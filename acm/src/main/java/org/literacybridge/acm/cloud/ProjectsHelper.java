package org.literacybridge.acm.cloud;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectsHelper {
    static final String DEPLOYMENTS_BUCKET_NAME = "acm-content-updates";
    static final String CONTENT_BUCKET_NAME = "amplio-program-content";
    static final String PROGSPEC_BUCKET_NAME = "amplio-progspecs";
    public static final String PROGSPEC_ETAGS_FILE_NAME = "etags.properties";

    /**
     * The files that make up past and present program specs, as a list-of-lists.
     * Each of the sub-lists is the same thing, preferred name first.
     */
    public static final String[][] PROGSPEC_PREFERED_NAMES = new String[][] {
            // New, old files
            new String[] { "pub_recipients.csv", "recipients.csv" },
            new String[] { "pub_deployments.csv", "deployment_spec.csv" },
            new String[] { "pub_content.csv", "content.csv" },
            new String[] { "pub_languages.csv", "languages.csv" },
            // Added files
            new String[] { "pub_general.csv" },
            new String[] { "pub_progspec.xlsx" },
            // Obsolete file
            new String[] { "recipients_map.csv" },
    };
    // Easy lookup to see if a given file is a program spec file.
    private static final Set<String> PROGSPEC_OBJECT_NAMES = Arrays.stream(PROGSPEC_PREFERED_NAMES)
            .flatMap(Arrays::stream)
            .collect(Collectors.toSet());

    IdentityPersistence identityPersistence;

    public ProjectsHelper(IdentityPersistence identityPersistence) {
        this.identityPersistence = identityPersistence;
    }

    public static class DeploymentInfo {
        String bucket; // Where this deployment content can be found.
        String key; // Key within the bucket.

        String project; // Like UNICEF-CHPS
        String deploymentName; // Like UNICEF-CHPS-19-3
        String versionMarker; // Like UNICEF-CHPS-19-3-a
        String revId; // Like a
        String fileName; // name of the .zip file
        Date fileDate; // Date the file was last modified (ie, uploaded to s3)
        String eTag; // eTag of the .zip file.
        long size; // size of the .zip file.
        boolean isCurrent; // true if this is the ".current" Deployment

        DeploymentInfo(String project) {
            this.project = project;
        }

        public String getBucket() {
            return bucket;
        }

        public String getKey() {
            return key;
        }

        public String getProject() {
            return project;
        }

        public String getDeploymentName() {
            return deploymentName;
        }

        public String getVersionMarker() {
            return versionMarker;
        }

        public String getRevId() {
            return revId;
        }

        public String getFileName() {
            return fileName;
        }

        public Date getFileDate() {
            return fileDate;
        }

        public String geteTag() {
            return eTag;
        }

        public long getSize() {
            return size;
        }

        public boolean isCurrent() {
            return isCurrent;
        }
    }

    private final Authenticator authInstance = Authenticator.getInstance();

    private final Collection<String> projects = null;

    // Matches deploymentName-suffix.current or .rev. Like TEST-19-2-ab.rev
    private final Pattern deploymentPattern = Pattern.compile("((\\w+(?:-\\w+)*)-(\\w+))");
    private final Pattern markerPattern = Pattern.compile("((\\w+(?:-\\w+)*)-(\\w+))\\.(current|rev)");
    private final Pattern zipPattern = Pattern.compile("((\\w+(?:-\\w+)*)-(\\w+))\\.zip");

    /**
     * Get the latest deployment info for the project.
     *
     * @param project Project for which to get Deployment info.
     * @return A map from Deployment Name to Deployment Info. Currently, only a
     *         single
     *         Deployment.
     */
    public Map<String, DeploymentInfo> getDeploymentInfo(String project) {
        Map<String, DeploymentInfo> deplInfo = new HashMap<>();

        Map<String, DeploymentInfo> s3Info = getS3DeploymentInfo(project);

        AmazonS3 s3Client = authInstance.getAwsInterface().getS3Client();
        ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request().withBucketName(
                ProjectsHelper.DEPLOYMENTS_BUCKET_NAME).withPrefix("projects/" + project + "/");

        ListObjectsV2Result result = s3Client.listObjectsV2(listObjectsV2Request);
        List<S3ObjectSummary> s3ObjectSummaries = result.getObjectSummaries();

        ProjectsHelper.DeploymentInfo deploymentInfo = new ProjectsHelper.DeploymentInfo(project);

        // First, look for the ".current" or ".rev" file. Then we'll look for the
        // matching .zip.
        for (S3ObjectSummary summary : s3ObjectSummaries) {
            String[] parts = summary.getKey().split("/");
            if (parts.length == 3) {
                Matcher matcher = markerPattern.matcher(parts[2]);
                if (matcher.matches()) {
                    deploymentInfo.isCurrent = true;
                    deploymentInfo.versionMarker = matcher.group(1); // like TEST-19-1-ap
                    deploymentInfo.deploymentName = matcher.group(2); // like TEST-19-1
                    deploymentInfo.revId = matcher.group(3); // like ap
                }
            }
        }

        // Knowing the current Deployment (in s3), gather its size.
        Pattern targetZipPattern = Pattern.compile(String.format("(?:content-)?%s.zip",
                deploymentInfo.versionMarker));
        for (S3ObjectSummary summary : s3ObjectSummaries) {
            // The S3 key is like projects/UNICEF-CHPS/content-UNICEF-CHPS-19-3-a.zip
            String[] parts = summary.getKey().split("/");
            if (parts.length == 3) {
                Matcher matcher = targetZipPattern.matcher(parts[2]);
                if (matcher.matches()) {
                    deploymentInfo.bucket = summary.getBucketName();
                    deploymentInfo.key = summary.getKey();
                    deploymentInfo.size = summary.getSize(); // 20mB or whatever
                    deploymentInfo.eTag = summary.getETag(); // hash of content
                    deploymentInfo.fileDate = summary.getLastModified(); // Date object stored
                    deploymentInfo.fileName = matcher.group(); // like content-UNICEF-CHPS-19-3-a.zip
                    deplInfo.put(deploymentInfo.deploymentName, deploymentInfo);
                }
            }
        }
        return deplInfo;
    }

    /**
     * Gets published deployment info for an S3 hosted program.
     *
     * @param programid The programid for which the deployments are wanted.
     * @return a map {deployment-name: deployment-info} of deployment data.
     */
    public Map<String, DeploymentInfo> getS3DeploymentInfo(String programid) {
        Map<String, DeploymentInfo> deplInfo = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        AmazonS3 s3Client = authInstance.getAwsInterface().getS3Client();
        String continuationToken = null;
        boolean more = true;

        while (more) {
            ListObjectsV2Request s3PublishedDir = new ListObjectsV2Request()
                    .withBucketName(ProjectsHelper.CONTENT_BUCKET_NAME)
                    .withPrefix(programid + "/TB-Loaders/published/");
            if (continuationToken != null) {
                s3PublishedDir.setContinuationToken(continuationToken);
            }
            ListObjectsV2Result result = s3Client.listObjectsV2(s3PublishedDir);
            more = result.isTruncated();
            continuationToken = result.getNextContinuationToken();

            for (S3ObjectSummary s3publishedObject : result.getObjectSummaries()) {
                // Care about TEST/TB-Loaders/published/TEST-21-3-a.rev or
                // TEST/TB-Loaders/published/TEST-21-3-a/TEST-21-3-a.zip
                String[] parts = s3publishedObject.getKey().split("/");
                if (parts.length == 4) {
                    Matcher markerMatcher = markerPattern.matcher(parts[3]);
                    if (markerMatcher.matches()) {
                        String key = markerMatcher.group(1);
                        DeploymentInfo di = deplInfo.computeIfAbsent(key, unused -> {
                            DeploymentInfo newDi = new DeploymentInfo(programid);
                            newDi.versionMarker = markerMatcher.group(1); // like TEST-19-1-ap
                            newDi.deploymentName = markerMatcher.group(2); // like TEST-19-1
                            newDi.revId = markerMatcher.group(3); // like ap
                            return newDi;
                        });
                        di.isCurrent = true;
                    }
                } else if (parts.length == 5) {
                    Matcher zipMatcher = zipPattern.matcher(parts[4]);
                    if (zipMatcher.matches()) {
                        Matcher deploymentMatcher = deploymentPattern.matcher(parts[3]);
                        if (deploymentMatcher.matches()) {
                            String key = deploymentMatcher.group(1);
                            DeploymentInfo di = deplInfo.computeIfAbsent(key, unused -> {
                                DeploymentInfo newDi = new DeploymentInfo(programid);
                                newDi.versionMarker = deploymentMatcher.group(1); // like TEST-19-1-ap
                                newDi.deploymentName = deploymentMatcher.group(2); // like TEST-19-1
                                newDi.revId = deploymentMatcher.group(3); // like ap
                                return newDi;
                            });
                            di.bucket = s3publishedObject.getBucketName();
                            di.key = s3publishedObject.getKey();
                            di.size = s3publishedObject.getSize(); // 20mB or whatever
                            di.eTag = s3publishedObject.getETag(); // hash of content
                            di.fileDate = s3publishedObject.getLastModified(); // Date object stored
                            di.fileName = zipMatcher.group(); // like content-UNICEF-CHPS-19-3-a.zip
                        }
                    }
                }
            }
        }

        // Find the latest rev of each deployment.
        Map<String, String> latestRevs = new HashMap<>();
        deplInfo.values().forEach(v -> {
            String deployment = v.getDeploymentName();
            String rev = v.getRevId();
            String latestRev = latestRevs.computeIfAbsent(deployment, (unused) -> rev);
            if (rev.length() > latestRev.length()
                    || (rev.length()) == latestRev.length() && rev.compareToIgnoreCase(latestRev) > 0) {
                latestRevs.put(deployment, rev);
            }
        });

        // Filter by latest rev; only ones with a .zip file. Drop the revision from the
        // key.
        return deplInfo.entrySet().stream()
                .filter(e -> {
                    String thisRev = e.getValue().getRevId();
                    String latestRev = latestRevs.get(e.getValue().getDeploymentName());
                    return thisRev.equalsIgnoreCase(latestRev);
                })
                .filter(e -> e.getValue().getKey() != null)
                .collect(Collectors.toMap(e -> e.getValue().getDeploymentName(), Map.Entry::getValue));
    }

    /**
     * Gets current program spec etags from S3.
     *
     * @param programid for which program spec etags are needed.
     * @return a map of {name : S3ObjectSummary} of the program spec components for
     *         the program.
     */
    public Map<String, S3ObjectSummary> getProgSpecInfo(String programid) {
        Map<String, S3ObjectSummary> results = new HashMap<>();
        String prefix = programid + "/";

        AmazonS3 s3Client = authInstance.getAwsInterface().getS3Client();
        String continuationToken = null;
        boolean more = true;

        while (more) {
            // Request program spec files.
            ListObjectsV2Request s3ProgSpecs = new ListObjectsV2Request()
                    .withBucketName(ProjectsHelper.PROGSPEC_BUCKET_NAME)
                    .withPrefix(prefix);
            // There should really never be a continuation, but, architecturally in S3,
            // there could be, so handle it.
            if (continuationToken != null) {
                s3ProgSpecs.setContinuationToken(continuationToken);
            }
            ListObjectsV2Result result = s3Client.listObjectsV2(s3ProgSpecs);
            more = result.isTruncated();
            continuationToken = result.getNextContinuationToken();

            for (S3ObjectSummary s3progspecObject : result.getObjectSummaries()) {
                String objectName = s3progspecObject.getKey().substring(prefix.length());
                if (PROGSPEC_OBJECT_NAMES.contains(objectName)) {
                    results.put(objectName, s3progspecObject);
                }
            }
        }

        return results;
    }

    /**
     * Download a given program spec part. We use the etag to be sure we get the
     * desired version. There is still
     * a race because the parts of the program spec are updated non-transactionally.
     *
     * @param key        of the part, like "recipients.csv"
     * @param etag       of the object
     * @param outputFile to which the bits should be written
     * @return true if an object was downloaded, false otherwise.
     */
    public boolean downloadProgSpecFile(String key, String etag, File outputFile) {
        if (authInstance.isAuthenticated() && authInstance.isOnline()) {
            GetObjectRequest request = new GetObjectRequest(PROGSPEC_BUCKET_NAME, key)
                    .withMatchingETagConstraint(etag);
            return authInstance.getAwsInterface().downloadS3Object(request,
                    outputFile,
                    null);
        }
        return false;
    }

    public boolean downloadDeployment(DeploymentInfo deploymentInfo,
            File outputFile,
            BiConsumer<Long, Long> progressHandler) {
        if (authInstance.isAuthenticated() && authInstance.isOnline()) {
            return authInstance.getAwsInterface().downloadS3Object(deploymentInfo.getBucket(),
                    deploymentInfo.getKey(),
                    outputFile,
                    progressHandler);
        }
        return false;
    }
}
