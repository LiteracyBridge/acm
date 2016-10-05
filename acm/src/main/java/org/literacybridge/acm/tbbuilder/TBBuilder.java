package org.literacybridge.acm.tbbuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.tbloader.TBLoader;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.acm.utils.ZipUnzip;
import org.literacybridge.core.tbloader.TBLoaderConstants;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class TBBuilder {
  private static final String[] CSV_COLUMNS_CONTENT_IN_PACKAGE = { "project",
      "contentpackage", "contentid", "categoryid", "order" };
  private static final String[] CSV_COLUMNS_CATEGORIES_IN_PACKAGE = { "project",
      "contentpackage", "categoryid", "order" };
  private static final String[] CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT = {
      "project", "deployment", "contentpackage", "packagename", "startDate",
      "endDate", "languageCode", "groups", "distribution" };
  private static final String CONTENT_IN_PACKAGES_CSV_FILE_NAME = "contentinpackages.csv";
  private static final String CATEGORIES_IN_PACKAGES_CSV_FILE_NAME = "categoriesinpackages.csv";
  private static final String PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME = "packagesindeployment.csv";
  public static String firstMessageListName = "1";
  public static String IntroMessageID = "0-5";
  private static String IntroMessageListFilename = IntroMessageID + ".txt";
  public static final String ACM_PREFIX = "ACM-";
  private final static int MAX_DEPLOYMENTS = 5;
  private File dropboxTbLoadersDir;
  private File targetDeploymentDir;
  private CSVWriter contentInPackageCSVWriter;
  private CSVWriter categoriesInPackageCSVWriter;
  private CSVWriter packagesInDeploymentCSVWriter;
  private File sourceTbOptionsDir;
  private File targetTempDir;
  private String deploymentNumber;
  public String project;

  public static void main(String[] args) throws Exception {
    System.out.println("TB-Builder v" + Constants.ACM_VERSION);
    if (args.length == 0) {
      printUsage();
      System.exit(1);
    } else if (args[0].equalsIgnoreCase("CREATE")) {
      doCreate(args);
    } else if (args[0].equalsIgnoreCase("PUBLISH")) {
      doPublish(args);
    } else {
      printUsage();
      System.exit(1);
    }
  }

  /**
   * Gathers the files that will make a deployment, into the directory
   * ~/LiteracyBridge/TB-Loaders/PROJ/.
   * Existing content in the /content/DEPLOYMENT and /metadata/DEPLOYMENT
   * subdirectories of that directory will be deleted.
   * @param args Command line args to the program.
   *             The first arg is "CREATE" (or we wouldn't be here)
   *             The second is the ACM name _with_ ACM- prefix
   *             Next is the deployment name. By convention project-year-name-sequence
   *             Then there is either:
   *                 package-name language
   *             Or one or more instances of:
   *                 (package-name language group) ...
   * @throws Exception
   */
  private static void doCreate(String[] args) throws Exception {
    if (args.length < 5 || (args.length > 5 && ((args.length % 3) != 0))) {
      printUsage();
      System.exit(1);
    }

    TBBuilder tbb = new TBBuilder(args[1]);
    tbb.createDeployment(args[2]);

    if (args.length == 5) {
      // one package with only default group
      tbb.addImage(args[3], args[4], TBLoaderConstants.DEFAULT_GROUP_LABEL);
    } else {
      // one or more packages with specified group
      int argIx = 3;
      while (argIx < args.length) {
        // First package is also for default group
        if (argIx == 3) {
          tbb.addImage(args[argIx], args[argIx+1], TBLoaderConstants.DEFAULT_GROUP_LABEL, args[argIx+2]);
        } else {
          tbb.addImage(args[argIx], args[argIx+1], args[argIx+2]);
        }
        argIx += 3;
      }
    }

    tbb.contentInPackageCSVWriter.close();
    tbb.categoriesInPackageCSVWriter.close();
    tbb.packagesInDeploymentCSVWriter.close();

  }

  /**
   * Publishes a previously created deployment to Dropbox.
   * @param args Command line args to the program.
   *             The first arg is "PUBLISH" (or we wouldn't be here)
   *             The second is the ACM name _with_ ACM- prefix
   *             Next is the deployment name, as above.
   *             (There can optionally be additional deployments to be bundled
   *             into the main deployment.)
   * @throws Exception
   */
  private static void doPublish(String[] args) throws Exception {
    TBBuilder tbb;
    if (args.length < 3) {
      printUsage();
      System.exit(1);
    }
    tbb = new TBBuilder(args[1]);
    int deploymentCount = args.length - 2;
    if (deploymentCount > MAX_DEPLOYMENTS)
      deploymentCount = MAX_DEPLOYMENTS;
    String[] deployments = new String[deploymentCount];
    for (int i = 0; i < deploymentCount; i++) {
      deployments[i] = args[i + 2];
    }
    tbb.publish(deployments);
  }

  private static void printUsage() {
    System.out.println("TB-Builder.bat runs java -cp acm.jar:lib/* org.literacybridge.acm.tbbuilder.TBBuilder");
    System.out.println(
            "Usage: TB-Builder.bat CREATE <acm_name> <deployment> <package_name> <language> (<group> (<package_name2> <language2> <group2>)...)");
    System.out.println(
            "OR   : TB-Builder.bat PUBLISH <acm_name> <default_deployment> (<deployment2>...) ");
  }

  public TBBuilder(String sharedACM) throws Exception {
    CommandLineParams params = new CommandLineParams();
    params.disableUI = true;
    params.sandbox = true;
    params.sharedACM = sharedACM;
    ACMConfiguration.initialize(params);
    ACMConfiguration.getInstance().setCurrentDB(params.sharedACM);
    // Like ~/Dropbox/ACM-UWR/TB-Loaders
    dropboxTbLoadersDir = ACMConfiguration.getInstance().dirACM(sharedACM); // .getCurrentDB().getTBLoadersDirectory();
    project = sharedACM.substring(ACM_PREFIX.length());
    // ~/LiteracyBridge/TB-Loaders
    File localTbLoadersDir = new File(DBConfiguration.getLiteracyBridgeHomeDirectory(), Constants.TBLoadersHomeDir);
    // Like ~/LiteracyBridge/TB-Loaders/UWR
    targetTempDir = new File(localTbLoadersDir, project);
  }

  public void addImage(String packageName, String languageCode, String... groups)
      throws Exception {
    boolean hasIntro = false;
    int groupCount = groups.length;
    System.out.println("\n\nExporting package " + packageName);

    File sourcePackageDir = new File(dropboxTbLoadersDir,
        "packages/" + packageName);
    File sourceMessagesDir = new File(sourcePackageDir, "messages");
    File sourceListsDir = new File(sourceMessagesDir,
        "lists/" + TBBuilder.firstMessageListName);
    File targetImagesDir = new File(targetDeploymentDir, "images");
    File targetImageDir = new File(targetImagesDir, packageName);

    IOUtils.deleteRecursive(targetImageDir);
    targetImageDir.mkdirs();

    if (!sourceListsDir.exists()) {
      System.err.println("Directory not found: " + sourceListsDir);
      System.exit(1);
    } else if (sourceListsDir.listFiles().length == 0) {
      System.err.println("No lists found in " + sourceListsDir);
      System.exit(1);
    }

    File targetMessagesDir = new File(targetImageDir, "messages");
    FileUtils.copyDirectory(sourceMessagesDir, targetMessagesDir);

    File targetAudioDir = new File(targetMessagesDir, "audio");
    File targetListsDir = new File(targetMessagesDir,
        "lists/" + TBBuilder.firstMessageListName);
    File targetLanguagesDir = new File(targetImageDir, "languages");
    File targetLanguageDir = new File(targetLanguagesDir, languageCode);
    File targetWelcomeMessageDir = targetLanguageDir;

    if (!targetAudioDir.exists() && !targetAudioDir.mkdirs()) {
      System.err.println("Unable to create directory: " + targetAudioDir);
      System.exit(1);
    }

    if (!targetLanguageDir.exists() && !targetLanguageDir.mkdirs()) {
      System.err.println("Unable to create directory: " + targetLanguageDir);
      System.exit(1);
    }

    if (!targetWelcomeMessageDir.exists()
        && !targetWelcomeMessageDir.mkdirs()) {
      System.err
          .println("Unable to create directory: " + targetWelcomeMessageDir);
      System.exit(1);
    }

    File[] lists = targetListsDir.listFiles();
    for (File list : lists) {
      if (list.getName().equals(TBBuilder.IntroMessageListFilename)) {
        exportList(packageName, list, targetWelcomeMessageDir, "intro.a18",
            false);
        list.delete();
        hasIntro = true;
      } else if (list.getName().equalsIgnoreCase("_activeLists.txt")) {
        exportCategoriesInPackage(packageName, list);
      } else {
        exportList(packageName, list, targetAudioDir, true);
      }
    }

    File sourceBasic = new File(sourceTbOptionsDir, "basic");
    FileUtils.copyDirectory(sourceBasic, targetImageDir);

    File sourceConfigFile = new File(sourceTbOptionsDir,
        "config_files/config.txt");
    File targetSystemDir = new File(targetImageDir, "system");
    FileUtils.copyFileToDirectory(sourceConfigFile, targetSystemDir);

    File sourceLanguage = new File(sourceTbOptionsDir,
        "languages/" + languageCode);
    FileUtils.copyDirectory(sourceLanguage, targetLanguageDir);

    File sourceControlFile;
    if (hasIntro) {
      sourceControlFile = new File(sourceTbOptionsDir,
          "system_menus/control-with_intro.txt");
    } else {
      sourceControlFile = new File(sourceTbOptionsDir,
          "system_menus/control-no_intro.txt");
    }
    FileUtils.copyFile(sourceControlFile,
        new File(targetLanguageDir, "control.txt"));

    // create profile.txt
    String profileString = packageName.toUpperCase() + "," + languageCode + ","
        + TBBuilder.firstMessageListName + ",menu\n";
    File profileFile = new File(targetSystemDir, "profiles.txt");
    BufferedWriter out = new BufferedWriter(new FileWriter(profileFile));
    out.write(profileString);
    out.close();

    for (int i = 0; i < groupCount; i++) {
      File f = new File(targetSystemDir,
          groups[i] + TBLoaderConstants.GROUP_FILE_EXTENSION);
      f.createNewFile();
    }

    File f = new File(targetSystemDir, packageName + ".pkg");
    f.createNewFile();

    exportPackagesInDeployment(packageName, languageCode, groups);
    System.out.println("Done with adding image for " + packageName + " and "
        + languageCode + ".");

  }

  public void createDeployment(String deployment) throws Exception {
    deploymentNumber = deployment;
    targetDeploymentDir = new File(targetTempDir,
        "content/" + deploymentNumber);
    File targetMetadataDir = new File(targetTempDir,
        "metadata/" + deploymentNumber);

    sourceTbOptionsDir = new File(dropboxTbLoadersDir, "TB_Options");

    // use LB Home Dir to create folder, then zip to Dropbox and delete the
    // folder
    IOUtils.deleteRecursive(targetDeploymentDir);
    targetDeploymentDir.mkdirs();
    IOUtils.deleteRecursive(targetMetadataDir);
    targetMetadataDir.mkdirs();

    contentInPackageCSVWriter = new CSVWriter(
        new FileWriter(
            new File(targetMetadataDir, CONTENT_IN_PACKAGES_CSV_FILE_NAME)),
        ',');
    categoriesInPackageCSVWriter = new CSVWriter(
        new FileWriter(
            new File(targetMetadataDir, CATEGORIES_IN_PACKAGES_CSV_FILE_NAME)),
        ',');
    packagesInDeploymentCSVWriter = new CSVWriter(
        new FileWriter(
            new File(targetMetadataDir, PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME)),
        ',');

    // write column headers
    contentInPackageCSVWriter.writeNext(CSV_COLUMNS_CONTENT_IN_PACKAGE);
    categoriesInPackageCSVWriter.writeNext(CSV_COLUMNS_CATEGORIES_IN_PACKAGE);
    packagesInDeploymentCSVWriter.writeNext(CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT);

    // Find the lexically greatest filename of firmware. Works because we'll never exceed 4 digits.
    File sourceFirmware = null;
    File[] firmwareOptions = new File(sourceTbOptionsDir, "firmware")
        .listFiles();
    for (File f : firmwareOptions) {
      if (sourceFirmware == null) {
        sourceFirmware = f;
      } else if (sourceFirmware.getName()
          .compareToIgnoreCase(f.getName()) < 0) {
        sourceFirmware = f;
      }
    }
    File targetBasicDir = new File(targetDeploymentDir, "basic");
    FileUtils.copyFileToDirectory(sourceFirmware, targetBasicDir);

    File sourceCommunitiesDir = new File(dropboxTbLoadersDir, "communities");
    File targetCommunitiesDir = new File(targetDeploymentDir, "communities");
    FileUtils.copyDirectory(sourceCommunitiesDir, targetCommunitiesDir);

    File localSoftware = new File(targetTempDir, "software");
    FileUtils.deleteDirectory(localSoftware);
    FileUtils.copyDirectory(new File(dropboxTbLoadersDir, "software"),
        localSoftware);

    deleteRevFiles(targetTempDir);
    String revision;
    revision = TBLoaderConstants.UNPUBLISHED_REV + "_"
        + TBLoader.getDateTime().substring(8, 17);
    File newRev = new File(targetTempDir, revision + ".rev");
    newRev.createNewFile();

    System.out.println(
        "\nDone with deployment of software and basic/community content.");
  }

  private static char getLatestDistributionRevision(File publishTbLoadersDir,
      final String distribution, boolean updateRevision) throws Exception {
    char revision = 'a';
    File[] files = publishTbLoadersDir.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(".rev")
            && name.toLowerCase().startsWith(distribution);
      }
    });
    if (files.length > 1)
      throw new Exception("Too many *rev files.  There can only be one.");
    else if (files.length == 1) {
      revision = files[0].getName()
          .subSequence(distribution.length() + 1, distribution.length() + 2)
          .charAt(0);
    }
    if (updateRevision) {
      revision++;
      // search files again, but this time for all *.rev files, not just the one
      // with the same deployment number so we can delete them all
      files = publishTbLoadersDir.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.toLowerCase().endsWith(".rev");
        }
      });
      int fileCount = files.length;
      for (int i = 0; i < fileCount; i++) {
        files[i].delete();
      }
      File newRev = new File(publishTbLoadersDir,
          distribution + "-" + revision + ".rev");
      newRev.createNewFile();
    }
    return revision;
  }

  public void publish(final String[] deployments) throws Exception {
    // e.g. 'ACM-UWR/TB-Loaders/published/'
    final File publishBaseDir = new File(dropboxTbLoadersDir, "published");
    publishBaseDir.mkdirs();
    String distribution = deployments[0]; // assumes first deployment is most
                                          // recent and should be name of
                                          // distribution
    char revision = getLatestDistributionRevision(publishBaseDir, distribution,
        true);
    final String publishDistributionName = distribution + "-" + revision; // e.g.
                                                                          // '2015-6-c'

    // e.g. 'ACM-UWR/TB-Loaders/published/2015-6-c'
    final File publishDistributionDir = new File(publishBaseDir,
        publishDistributionName);
    publishDistributionDir.mkdirs();

    String zipSuffix = distribution + "-" + revision + ".zip";
    File localContent = new File(targetTempDir, "content");
    ZipUnzip.zip(localContent,
        new File(publishDistributionDir, "content-" + zipSuffix), true,
        deployments);

    // merge csv files
    File localMetadata = new File(targetTempDir, "metadata");
    File[] deploymentDirs = localMetadata.listFiles(new FileFilter() {
      @Override
      public boolean accept(File f) {
        return f.isDirectory()
            && Arrays.asList(deployments).contains(f.getName());
      }
    });
    final List<File> inputContentCSVFiles = new LinkedList<File>();
    final List<File> inputCategoriesCSVFiles = new LinkedList<File>();
    final List<File> inputPackagesCSVFiles = new LinkedList<File>();
    for (File deploymentDir : deploymentDirs) {
      deploymentDir.listFiles(new FileFilter() {
        @Override
        public boolean accept(File f) {
          if (f.getName().endsWith(CONTENT_IN_PACKAGES_CSV_FILE_NAME)) {
            inputContentCSVFiles.add(f);
            return true;
          } else if (f.getName()
              .endsWith(CATEGORIES_IN_PACKAGES_CSV_FILE_NAME)) {
            inputCategoriesCSVFiles.add(f);
            return true;
          } else if (f.getName()
              .endsWith(PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME)) {
            inputPackagesCSVFiles.add(f);
            return true;
          }
          return false;
        }
      });
    }
    File metadataDir = new File(publishDistributionDir, "metadata");
    if (!metadataDir.exists())
      metadataDir.mkdir();
    File mergedCSVFile = new File(metadataDir,
        CONTENT_IN_PACKAGES_CSV_FILE_NAME);
    mergeCSVFiles(inputContentCSVFiles, mergedCSVFile,
        CSV_COLUMNS_CONTENT_IN_PACKAGE);

    mergedCSVFile = new File(metadataDir, CATEGORIES_IN_PACKAGES_CSV_FILE_NAME);
    mergeCSVFiles(inputCategoriesCSVFiles, mergedCSVFile,
        CSV_COLUMNS_CATEGORIES_IN_PACKAGE);

    mergedCSVFile = new File(metadataDir, PACKAGES_IN_DEPLOYMENT_CSV_FILE_NAME);
    mergeCSVFiles(inputPackagesCSVFiles, mergedCSVFile,
        CSV_COLUMNS_PACKAGES_IN_DEPLOYMENT);

    deleteRevFiles(targetTempDir);
    ZipUnzip.zip(new File(dropboxTbLoadersDir, "software"),
        new File(publishDistributionDir, "software-" + zipSuffix), true);
  }

  private static void deleteRevFiles(File dir) {
    File[] files = dir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(
            ".rev") /* && name.toLowerCase().startsWith(deploymentNumber) */;
      }
    });
    for (File revisionFile : files) {
      revisionFile.delete();
    }
  }

  private void exportList(String contentPackage, File list,
      File targetDirectory, boolean writeToCSV) throws Exception {
    exportList(contentPackage, list, targetDirectory, null, writeToCSV);
  }

  private void exportPackagesInDeployment(String contentPackage,
      String languageCode, String[] groups) throws IOException {
    String groupsconcat = StringUtils.join(groups, ',');
    String[] csvColumns = new String[9];
    csvColumns[0] = project.toUpperCase();
    csvColumns[1] = deploymentNumber.toUpperCase();
    csvColumns[2] = contentPackage.toUpperCase();
    csvColumns[3] = contentPackage.toUpperCase();
    Calendar cal = Calendar.getInstance();
    int year = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH) + 1;
    int date = cal.get(Calendar.DAY_OF_MONTH);
    csvColumns[4] = String.valueOf(month) + "/" + String.valueOf(date) + "/"
        + String.valueOf(year); // approx start date
    csvColumns[5] = null; // end date unknown at this point
    csvColumns[6] = languageCode;
    csvColumns[7] = groupsconcat;
    csvColumns[8] = null; // distribution name not known until publishing
    // NOTE that we don't ever include the distribution name in the metadata.
    // It's grabbed by the shell scripts from the folder name,
    // and then a SQL UPDATE adds it in after uploading the CSV.
    packagesInDeploymentCSVWriter.writeNext(csvColumns);
  }

  private void exportCategoriesInPackage(String contentPackage,
      File activeLists) throws IOException {
    String[] csvColumns = new String[4];
    csvColumns[0] = project.toUpperCase();
    csvColumns[1] = contentPackage.toUpperCase();

    // AudioItemRepository repository =
    // ACMConfiguration.getCurrentDB().getRepository();
    BufferedReader reader = new BufferedReader(new FileReader(activeLists));

    int order = 1;
    try {
      while (reader.ready()) {
        String categoryID = reader.readLine().trim();
        if (categoryID.startsWith("!")) {
          categoryID = categoryID.substring(1);
        }
        if (categoryID.startsWith("$")) {
          categoryID = categoryID.substring(1);
        }
        csvColumns[2] = categoryID;
        csvColumns[3] = Integer.toString(order);
        categoriesInPackageCSVWriter.writeNext(csvColumns);
        order++;
      }
    } finally {
      reader.close();
    }
  }

  private void exportList(String contentPackage, File list,
      File targetDirectory, String filename, boolean writeToCSV)
      throws Exception {
    System.out.println("  Exporting list " + list);
    String[] csvColumns = new String[5];
    csvColumns[0] = project.toUpperCase();
    csvColumns[1] = contentPackage.toUpperCase();
    csvColumns[3] = list.getName().substring(0, list.getName().length() - 4); // strip
                                                                              // .txt

    AudioItemRepository repository = ACMConfiguration.getInstance()
        .getCurrentDB().getRepository();
    BufferedReader reader = new BufferedReader(new FileReader(list));

    int order = 1;
    try {
      while (reader.ready()) {
        String uuid = reader.readLine();
        AudioItem audioItem = ACMConfiguration.getInstance().getCurrentDB()
            .getMetadataStore().getAudioItem(uuid);
        System.out.println(
            "    Exporting audioitem " + uuid + " to " + targetDirectory);
        if (filename == null) {
          repository.exportA18WithMetadata(audioItem, targetDirectory);
        } else {
          repository.exportA18WithMetadataToFile(audioItem,
              new File(targetDirectory, filename));
        }

        if (writeToCSV) {
          csvColumns[2] = uuid;
          csvColumns[4] = Integer.toString(order);
          contentInPackageCSVWriter.writeNext(csvColumns);
        }

        order++;
      }
    } finally {
      reader.close();
    }
  }

  private static void mergeCSVFiles(Iterable<File> inputFiles, File output,
      String[] header) throws IOException {
    CSVWriter writer = new CSVWriter(new FileWriter(output), ',');

    try {
      writer.writeNext(header);

      for (File input : inputFiles) {
        CSVReader reader = new CSVReader(new FileReader(input), ',');
        // skip header
        reader.readNext();
        writer.writeAll(reader.readAll());
        reader.close();
      }
    } finally {
      writer.close();
    }
  }
}
