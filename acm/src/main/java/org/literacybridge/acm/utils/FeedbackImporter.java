package org.literacybridge.acm.utils;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.importexport.FileImporter;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.MetadataValue;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedbackImporter {
  private static final Set<String> EXTENSIONS_TO_IMPORT = new HashSet<>(Arrays.asList("a18"));
  private final Params params;
  private int filesImported = 0;
  private int filesFailedToImport = 0;
  private int projectsImportedNoErrors = 0;
  private int projectsImportedWithErrors = 0;
  private int projectsFailedToImport = 0;
  private int unknownProjectsIgnored = 0;
  private int unknownUpdatesIgnored = 0;

  public static void main(String[] args) throws Exception {
    int rc = 0;
    Params params = new Params();
    CmdLineParser parser = new CmdLineParser(params);
    parser.parseArgument(args);

    FeedbackImporter importer = new FeedbackImporter(params);
    if (!importer.validateCommandLineArgs()) {
      printUsage(parser);
    } else {
      importer.doImports();
    }

    System.exit(importer.getExitCode());
  }

  /**
   * Compute the return code for the import:
   * **** With these, it is OK to return the files:
   * 0 - files were imported, and no errors at all.
   * 1 - files were imported, but some files failed ot import. No db errors.
   * 2 - no files were imported or failed. No db errors.
   * **** With these, it is not OK to remove the files:
   * 20 - no files imported; there were import errors, but no db errors. Very strange.
   * 21 - there were errors opening or creating a database.
   *
   * @return the rc for System.exit()
   */
  private int getExitCode() {
    if (projectsFailedToImport == 0) {
      if (filesImported > 0 && filesFailedToImport == 0)  return 0;
      if (filesImported > 0 && filesFailedToImport > 0)   return 1;
      if (filesImported == 0 && filesFailedToImport == 0) return 2;
      // There were files, but all of them failed to import. Strange.
      if (filesImported == 0 && filesFailedToImport > 0)  return 20;
    }

    // There were projects that failed to open. Should not happen.
    return 21;
  }

  private FeedbackImporter(Params params) {
    this.params = params;
    CommandLineParams acmParams = new CommandLineParams();
    acmParams.disableUI = true;
    ACMConfiguration.initialize(acmParams);
  }

  /**
   * validateCommandLineArgs.
   * @return True if the args are OK, False otherwise.
   */
  private boolean validateCommandLineArgs() {
    if (params.dirNames.isEmpty()) return false;

    // Validate the dirs arguments.
    boolean filesOk = true;
    for (String dirName : params.dirNames) {
      File dir = new File(dirName);
      if (!dir.isDirectory()) {
        System.err.println(
                String.format("'%s' is not a directory", dirName));
        filesOk = false;
      }
      if (!dir.exists()) {
        System.err.println(
                String.format("Directory '%s' does not exist", dirName));
        filesOk = false;
      }
    }

    return filesOk;
  }

  /**
   * Main worker for the import. For each command line dir, for each project,
   * for each content update, import feedback.
   */
  private void doImports() {
    // Iterate over the given user recordings directories...
    for (String dirName : params.dirNames) {
      File userRecordingsDir = new File(dirName);
      // Iterate over the project sub-directories of the user recordings...
      File[] projectDirs = userRecordingsDir.listFiles(File::isDirectory);
      for (File projectDir : projectDirs != null ? projectDirs : new File[0]) {
        // Ignore unknown projects. They have no ACM anyway...
        if (projectDir.getName().equalsIgnoreCase("unknown")) {
          unknownProjectsIgnored++;
          continue;
        }
        // Iterate over the update sub-directories of the projects...
        File[] updateDirs = projectDir.listFiles(File::isDirectory);
        for (File updateDir : updateDirs != null ? updateDirs : new File[0]) {
          // Ignore unknown updates.
          if (updateDir.getName().equalsIgnoreCase("unknown")) {
            unknownUpdatesIgnored++;
            continue;
          }
          importDirectory(projectDir, updateDir);
        }
      }
    }

    System.out.println(String.format("%d files imported", filesImported));
    System.out.println(String.format("%d files failed to import", filesFailedToImport));
    System.out.println(String.format("%d projects imported with no file errors", projectsImportedNoErrors));
    System.out.println(String.format("%d projects imported with file errors", projectsImportedWithErrors));
    System.out.println(String.format("%d projects failed to open for imports", projectsFailedToImport));
    System.out.println(String.format("%d unknown projects skipped", unknownProjectsIgnored));
    System.out.println(String.format("%d unknown updates skipped", unknownUpdatesIgnored));
    System.out.println(String.format("Return code: %d", getExitCode()));
  }

  /**
   * Imports one content update directory for one project.
   * @param projectDir The project directory. Only needed for its name.
   * @param updateDir The content update directory. Contains the actual user feedback.
   */
  private void importDirectory(File projectDir, File updateDir) {
    boolean importSuccess;

    Set<File> filesToImport = gatherFiles(updateDir);

    try {
      openFeedbackProjectDb(projectDir, updateDir);
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
      projectsFailedToImport++;
      return;
    }

    importSuccess = importFiles(filesToImport);
    ACMConfiguration.getInstance().getCurrentDB().getControlAccess().updateDB();
    ACMConfiguration.getInstance().closeCurrentDB();
    if (importSuccess) {
      projectsImportedNoErrors++;
    } else {
      projectsImportedWithErrors++;
    }
  }

  /**
   * Given a project (UWR, CARE, ...) and an update (2016-3, CARE-2016-3, ...),
   * open the ACM database for {PROJECT}-FB-{UPDATE} where {UPDATE} is the
   * update name, minus any leading text. The resulting ACM directory will be
   * named like:
   *   ~/Dropbox / UWR-FB-2016-3
   *
   * Will attempt to create the ACM database if it doesn't exist.
   *
   * @param projectDir Project specific directory containing updates
   * @param updateDir Directory containing feedback for a given update
   */
  private void openFeedbackProjectDb(File projectDir, File updateDir)
          throws Exception {
    // Pattern to match & remove XYZ- at the start of a string.
    Pattern updatePattern = Pattern.compile("^(\\p{Alpha}+-)(.*)$");

    // Get the project name and update name. This is astonishingly ugly.
    String mainProject = "ACM-" + projectDir.getName().toUpperCase();
    // Trim the update name, removing any leading 'XYZ-'.
    String updateName = updateDir.getName().toUpperCase();
    Matcher matcher = updatePattern.matcher(updateName);
    if (matcher.matches()) {
      updateName = matcher.group(2);
    }
    // Make the combined name
    String feedbackProject = mainProject + "-FB-" + updateName;

    try {
      // Throws an exception to indicate "not found".
      ACMConfiguration.getInstance().setCurrentDB(feedbackProject);
    } catch (Exception setEx) {
      try {
        ACMConfiguration.getInstance().createNewDb(mainProject, feedbackProject);
      } catch (Exception createEx) {
        throw new Exception(String.format("Couldn't open or create DB '%s': %s", feedbackProject, createEx.getMessage()), createEx);
      }
    }
  }

  /**
   * Imports a set of files into the currently opened Acm project.
   * @param filesToImport A Set<File> of files to import.
   * @return True if all imported with no error, False if one or more errors.
   */
  private boolean importFiles(Set<File> filesToImport) {
    boolean success = true;
    int count = 0;
    Metadata metadata = new Metadata();

    FileImporter importer = FileImporter.getInstance();
    MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
    int id = ACMConfiguration.getInstance().getCurrentDB().getNextCorrelationId() + 1;
    ACMConfiguration.getInstance().getCurrentDB().setNextCorrelationId(id + filesToImport.size());

    for (File file : filesToImport) {
      try {
        if (params.verbose) {
          System.out.println(String.format("Importing %d of %d: %s", ++count, filesToImport.size(), file.getName()));
        }
        metadata.setMetadataField(MetadataSpecification.LB_CORRELATION_ID, new MetadataValue<Integer>(id++));
        importer.importFile(store, null /* category */, file, metadata);
        filesImported++;
      } catch (Exception e) {
        System.err.println(String.format("Failed to import '%s': %s", file.getName(), e.getMessage()));
        filesFailedToImport++;
        success = false;
      }
    }

    return success;
  }

  /**
   * Given a directory, recursively find all importable files within it.
   * @param dir The directory to search.
   * @return A Set<File> of files found.
   */
  private static Set<File> gatherFiles(File dir) {
    Set<File> toImport = new HashSet<>();
    File[] files = dir.listFiles();
    for (File file : files != null ? files : new File[0]) {
      if (file.isDirectory()) {
        toImport.addAll(gatherFiles(file));
      } else {
        String ext = "";
        String name = file.getName().toLowerCase();
        int ix = name.lastIndexOf('.');
        if (ix > 0) {
          ext = name.substring(ix+1);
        }
        if (EXTENSIONS_TO_IMPORT.contains(ext)) {
          toImport.add(file);
        }
      }
    }

    return toImport;
  }

  private static void printUsage(CmdLineParser parser) {
    System.err.println(
            "java -cp acm.jar:lib/* org.literacybridge.acm.utils.FeedbackImporter [--verbose] <dir1> [<dir2> ...]");
    parser.printUsage(System.err);
  }

  private static final class Params {
    @Option(name="--verbose", aliases="-v", usage="Give verbose output.")
    boolean verbose;

    @Argument(usage="Directory(ies) to import.")
    public List<String> dirNames;
  }


}
