package org.literacybridge.acm.tbbuilder;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.deployment.DeploymentInfo;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.tbloader.TBLoaderConstants;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.literacybridge.acm.tbbuilder.TBBuilder.CATEGORIES_IN_PACKAGES_CSV_FILE_NAME;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CONTENT_IN_PACKAGES_CSV_FILE_NAME;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CSV_COLUMNS_CATEGORIES_IN_PACKAGE;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CSV_COLUMNS_CONTENT_IN_PACKAGE;
import static org.literacybridge.acm.tbbuilder.TBBuilder.CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT;
import static org.literacybridge.acm.tbbuilder.TBBuilder.PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME;

public abstract class CreateFromDeploymentInfo {


    protected final TBBuilder tbBuilder;
    protected final TBBuilder.BuilderContext builderContext;
    protected final DeploymentInfo deploymentInfo;
    protected final AudioItemRepository repository = ACMConfiguration.getInstance().getCurrentDB().getRepository();

    protected final File stagingDir;

    public abstract AudioItemRepository.AudioFormat getAudioFormat();
    public abstract String getPackageName(DeploymentInfo.PackageInfo packageInfo);
    
    CreateFromDeploymentInfo(TBBuilder tbBuilder, TBBuilder.BuilderContext builderContext, DeploymentInfo deploymentInfo) {
        this.tbBuilder = tbBuilder;
        this.builderContext = builderContext;
        this.deploymentInfo = deploymentInfo;

        stagingDir = builderContext.stagingDir; // new File(builderContext.stagingDir.getParent(), "v1"+builderContext.stagingDir.getName());
    }

    void go() throws Exception {
        createBaseDeployment();

        for (DeploymentInfo.PackageInfo packageInfo : deploymentInfo.getPackages()) {
            addImageForPackage(packageInfo);
        }
        finalizeDeployment();

        exportMetadata();
    }

    abstract protected void addImageForPackage(DeploymentInfo.PackageInfo packageInfo) throws Exception;
    protected void finalizeDeployment() throws Exception {};
    abstract protected void exportFirmware() throws Exception;
    abstract protected List<String> getAcceptableFirmwareVersions();

    /**
     * Creates the structure for a Deployment, into which packages can be added.
     *
     * @throws Exception if there is an IO error.
     */
    protected void createBaseDeployment() throws Exception {
//        File stagedMetadataDir = new File(stagingDir, "metadata" + File.separator + deploymentInfo.getName());
        String revFileName = String.format(TBLoaderConstants.UNPUBLISHED_REVISION_FORMAT, builderContext.buildTimestamp, deploymentInfo.getName());
        // use LB Home Dir to create folder, then zip to Dropbox and delete the
        // folder
//        IOUtils.deleteRecursive(builderContext.stagedDeploymentDir);
        builderContext.stagedDeploymentDir.mkdirs();

        exportFirmware();

        exportProgramSpec();

        Utils.deleteRevFiles(stagingDir);
        // Leave a marker to indicate that there exists an unpublished deployment here.
        File newRev = new File(stagingDir, revFileName);
        newRev.createNewFile();
        // Put a marker inside the unpublished content, so that we will be able to tell which of
        // possibly several is the unpublished one.
        Utils.deleteRevFiles(builderContext.stagedDeploymentDir);
        newRev = new File(builderContext.stagedDeploymentDir, revFileName);
        newRev.createNewFile();

        builderContext.reportStatus("%nDone with deployment of basic/community content.%n");
    }

    /**
     * Exports the program spec, and adds some additional information about the program.
     * @throws IOException If the program spec can't be read or written.
     */
    protected void exportProgramSpec() throws IOException {
        if (builderContext.sourceProgramspecDir != null) {
            // Copy the complete program spec directory, including any cruft that's there. The resulting deployment
            // will contain an exact copy of the program spec.
            IOUtils.deleteRecursive(builderContext.stagedProgramspecDir);
            builderContext.stagedProgramspecDir.mkdirs();
            FileUtils.copyDirectory(builderContext.sourceProgramspecDir, builderContext.stagedProgramspecDir);

            // Copy some values from the program's config.properties.
            DBConfiguration dbConfig = ACMConfiguration.getInstance().getCurrentDB();
            Properties deploymentProperties = new Properties();
            deploymentProperties.setProperty(Constants.AUDIO_LANGUAGES, dbConfig.getConfigLanguages());
            deploymentProperties.setProperty(TBLoaderConstants.PROGRAM_FRIENDLY_NAME_PROPERTY, dbConfig.getFriendlyName());
            deploymentProperties.setProperty(TBLoaderConstants.PROGRAM_DESCRIPTION_PROPERTY, dbConfig.getFriendlyName());
            deploymentProperties.setProperty(TBLoaderConstants.PROGRAM_ID_PROPERTY, dbConfig.getProgramId());
            deploymentProperties.setProperty(TBLoaderConstants.DEPLOYMENT_NUMBER, Integer.toString(builderContext.deploymentNo));

            // Record "acceptable firmware versions", whatever that means for this architecture (v1 and v2 are different).
            Date now = new Date();
            deploymentProperties.setProperty(TBLoaderConstants.ACCEPTABLE_FIRMWARE_VERSIONS, String.join(",", getAcceptableFirmwareVersions()));

            // When is this deployment being executed?
            DateFormat localTime = new SimpleDateFormat("HH:mm:ss a z", Locale.getDefault());
            DateFormat localDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            deploymentProperties.setProperty(TBLoaderConstants.DEPLOYMENT_CREATION_TIME, localTime.format(now));
            deploymentProperties.setProperty(TBLoaderConstants.DEPLOYMENT_CREATION_DATE, localDate.format(now));

            // Who is creating the deployment?
            String userName = ""; // TODO: replace with greeting? full name? Authenticator.getInstance().getUserName();
            String userEmail = Authenticator.getInstance().getUserEmail();
            String user = (StringUtils.isEmpty(userName) ? "" : '('+userName+") ") + userEmail;
            deploymentProperties.setProperty(TBLoaderConstants.DEPLOYMENT_CREATION_USER, user);

            // Map of language, variant : package, for easier lookup from recipient's (languagecode, variant) to package.
            for (DeploymentInfo.PackageInfo packageInfo : deploymentInfo.getPackages()) {
                String key = packageInfo.getLanguageCode();
                String variant = packageInfo.getVariant();
                if (StringUtils.isNotBlank(variant)) key = key + "," + variant;
                deploymentProperties.put(key, getPackageName(packageInfo));
            }

            // Write the properties file out to the program spec directory.
            File propsFile = new File(builderContext.stagedProgramspecDir, ProgramSpec.DEPLOYMENT_INFO_PROPERTIES_NAME);
            try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(propsFile))) {
                deploymentProperties.store(out, null);
            } catch (IOException e) {
                // Ignore and continue without deployment properties.
            }

        }
    }

    //=================================================================================================================
    // Export metadata
    private void exportMetadata() throws IOException {
        File metadataDir = new File(stagingDir, "metadata" + File.separator + deploymentInfo.getName());
        IOUtils.deleteRecursive(metadataDir);
        metadataDir.mkdirs();

        exportPackagesInDeployment(metadataDir);
        exportCategoriesInPackages(metadataDir);
        exportContentInPackages(metadataDir);
    }

    private void exportContentInPackages(File metadataDir) throws IOException {
        // contentinpackages
        File csvFile = new File(metadataDir, CONTENT_IN_PACKAGES_CSV_FILE_NAME);
        try (FileWriter csvFileWriter = new FileWriter(csvFile);
             ICSVWriter contentInPackageCSVWriter = new CSVWriterBuilder(csvFileWriter).build()) {
            // "project", "contentpackage", "contentid", "categoryid", "order"
            contentInPackageCSVWriter.writeNext(CSV_COLUMNS_CONTENT_IN_PACKAGE);
            String[] contentColumns = new String[5];
            contentColumns[0] = deploymentInfo.getProgramId().toUpperCase();
            for (DeploymentInfo.PackageInfo packageInfo : deploymentInfo.getPackages()) {
                contentColumns[1] = getPackageName(packageInfo).toUpperCase();
                for (DeploymentInfo.PackageInfo.PlaylistInfo playlistInfo : packageInfo.getPlaylists()) {
                    contentColumns[3] = playlistInfo.getCategoryId();
                    int messagePosition = 0;
                    for (String audioItemId : playlistInfo.getAudioItemIds()) {
                        contentColumns[2] = audioItemId;
                        contentColumns[4] = Integer.toString(++messagePosition);
                        contentInPackageCSVWriter.writeNext(contentColumns);
                    }
                }
            }
        }
    }

    private void exportCategoriesInPackages(File metadataDir) throws IOException {
        // categoriesinpackage
        File csvFile = new File(metadataDir, CATEGORIES_IN_PACKAGES_CSV_FILE_NAME);
        try (FileWriter csvFileWriter = new FileWriter(csvFile);
             ICSVWriter categoriesInPackageCSVWriter = new CSVWriterBuilder(csvFileWriter).build()) {
            // "project", "contentpackage", "categoryid", "order"
            categoriesInPackageCSVWriter.writeNext(CSV_COLUMNS_CATEGORIES_IN_PACKAGE);
            String[] categoriesColumns = new String[4];
            categoriesColumns[0] = deploymentInfo.getProgramId().toUpperCase();
            for (DeploymentInfo.PackageInfo packageInfo : deploymentInfo.getPackages()) {
                categoriesColumns[1] = getPackageName(packageInfo).toUpperCase();
                int plPosition = 0;
                boolean hasUserFeedbackContent = false;
                for (DeploymentInfo.PackageInfo.PlaylistInfo playlistInfo : packageInfo.getPlaylists()) {
                    // Categories in Package
                    categoriesColumns[2] = playlistInfo.getCategoryId();
                    categoriesColumns[3] = Integer.toString(++plPosition);
                    categoriesInPackageCSVWriter.writeNext(categoriesColumns);
                    // So we'll know whether to add it.
                    hasUserFeedbackContent |= playlistInfo.getCategoryId().equals(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK);
                }
                if (!packageInfo.isUfHidden() && !hasUserFeedbackContent) {
                    // User feedback categories, even if there isn't content published in the category.
                    categoriesColumns[2] = Constants.CATEGORY_UNCATEGORIZED_FEEDBACK;
                    categoriesColumns[3] = Integer.toString(++plPosition);
                    categoriesInPackageCSVWriter.writeNext(categoriesColumns);
                }
                if (packageInfo.hasTutorial()) {
                    // Tutorial Categories; the constant is prefixed with a "$" that is not a real part of the category
                    categoriesColumns[2] = Constants.CATEGORY_TUTORIAL.replace("$", "");
                    categoriesColumns[3] = Integer.toString(++plPosition);
                    categoriesInPackageCSVWriter.writeNext(categoriesColumns);
                }
            }
        }
    }

    private void exportPackagesInDeployment(File metadataDir) throws IOException {
        // packagesindeployment
        File csvFile = new File(metadataDir, PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME);
        try (FileWriter csvFileWriter = new FileWriter(csvFile);
             ICSVWriter packagesInDeploymentCSVWriter = new CSVWriterBuilder(csvFileWriter).build()) {
            // "project", "deployment", "contentpackage", "packagename", "startDate",
            //    "endDate", "languageCode", "grouplangs", "distribution"
            packagesInDeploymentCSVWriter.writeNext(CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT);
            String[] packagesCsv = new String[9];
            packagesCsv[0] = deploymentInfo.getProgramId().toUpperCase();
            for (DeploymentInfo.PackageInfo packageInfo : deploymentInfo.getPackages()) {
                packagesCsv[1] = deploymentInfo.getName().toUpperCase(); // default: ${proramid}-${year}-${depl#}

                // Packages in Deployment
                packagesCsv[2] = getPackageName(packageInfo).toUpperCase();
                packagesCsv[3] = getPackageName(packageInfo).toUpperCase();
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1;
                int date = cal.get(Calendar.DAY_OF_MONTH);
                packagesCsv[4] = month + "/" + date + "/"
                    + year; // approx start date
                packagesCsv[5] = null; // end date unknown at this point
                packagesCsv[6] = packageInfo.getLanguageCode();
                String groupsconcat = String.join(",", packageInfo.getGroups());
                packagesCsv[7] = groupsconcat;
                packagesCsv[8] = null; // distribution name not known until publishing
                // NOTE that we don't ever include the distribution name in the metadata.
                // It's grabbed by the shell scripts from the folder name,
                // and then a SQL UPDATE adds it in after uploading the CSV.
                packagesInDeploymentCSVWriter.writeNext(packagesCsv);
            }
        }
    }


}
