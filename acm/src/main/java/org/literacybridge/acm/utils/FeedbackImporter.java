package org.literacybridge.acm.utils;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.Taxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.literacybridge.acm.Constants.CATEGORY_UNCATEGORIZED_FEEDBACK;
import static org.literacybridge.acm.config.AccessControlResolver.*;

public class FeedbackImporter {
  private static final Logger logger = LoggerFactory.getLogger(FeedbackImporter.class);

  // Note public

  private static final int too_short_duration = 5; // anything 5 seconds or less is too short
    private static final int too_long_duration = 300; // anything 5 minutes or more is too long

  private static final Set<String> EXTENSIONS_TO_IMPORT = new HashSet<>(
          Collections.singletonList("a18"));
  private static final String FEEDBACK_IMPORT_REPORT = "feedbackImport.txt";
  private final Params params;
  // Cache for includelisted content updates (only includelisted updates are to be imported).
  private Map<String, Includelister> deploymentsIncludelistCache = new HashMap<>();

  public static void main(String[] args) throws Exception {
    Params params = new Params();
    CmdLineParser parser = new CmdLineParser(params);
    parser.parseArgument(args);

    FeedbackImporter importer = new FeedbackImporter(params);
    if (!importer.validateCommandLineArgs()) {
      printUsage(parser);
      System.exit(100);
    }

    ImportResults results = importer.doImports();

    System.exit(results.getExitCode());
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
        System.err.printf("'%s' is not a directory%n", dirName);
        filesOk = false;
      }
      if (!dir.exists()) {
        System.err.printf("Directory '%s' does not exist%n", dirName);
        filesOk = false;
      }
    }

    if (params.processed != null) {
      if (!params.processed.isDirectory()) {
        System.err.printf("'%s' is not a directory%n", params.processed.getName());
        filesOk = false;
      } else if (!params.processed.exists()) {
        System.err.printf("'%s' does not exist%n", params.processed.getName());
        filesOk = false;
      }
    }

    if (params.skipped != null) {
      if (!params.skipped.isDirectory()) {
        System.err.printf("'%s' is not a directory%n", params.skipped.getName());
        filesOk = false;
      } else if (!params.skipped.exists()) {
        System.err.printf("'%s' does not exist%n", params.skipped.getName());
        filesOk = false;
      }
    }

    if (params.report != null) {
      if (params.report.exists()) {
        if (params.report.isDirectory()) {
          System.err.printf("'%s' is a directory.%n", params.report
                  .getName());
          filesOk = false;
        }
      }
    }
    return filesOk;
  }

  /**
   * Main worker for the import. For each command line dir, for each project,
   * for each content update, import feedback.
   */
  private ImportResults doImports() {
    ImportResults results = new ImportResults();
    // Iterate over the given user recordings directories...
    for (String dirName : params.dirNames) {
      logger.info(String.format("Looking for recordings in %s", dirName));
      // Every argument should be a directory containing {PROJECT} / {UPDATE} subdirs.
      File userRecordingsDir = new File(dirName);
      ImportResults dirResults = new ImportResults();

      // Iterate over the project sub-directories of the user recordings...
      File[] projectDirs = userRecordingsDir.listFiles(File::isDirectory);
      for (File projectDir : projectDirs != null ? projectDirs : new File[0]) {
        // Ignore unknown projects. They have no ACM anyway...
        if (projectDir.getName().equalsIgnoreCase("unknown")) {
          dirResults.projectSkipped(projectDir.getName());
          skippedProject(projectDir);
          continue;
        }
        logger.info(String.format("  importing from project directory %s", projectDir.getName()));

        // Iterate over the update sub-directories of the projects...
        ImportResults projectResults = new ImportResults();
        File[] updateDirs = projectDir.listFiles(File::isDirectory);
        for (File updateDir : updateDirs != null ? updateDirs : new File[0]) {
          // Ignore unknown updates, and updates to be deferred.
          if (updateDir.getName().equalsIgnoreCase("unknown") ||
                  shouldUpdateBeSkipped(projectDir.getName(), updateDir.getName())) {
            projectResults.updateSkipped(projectDir.getName(), updateDir.getName());
            skippedUpdate(projectDir, updateDir);
            continue;
          }
          logger.info(String.format("    importing from update directory %s", updateDir.getName()));

          projectResults.add(importDirectory(projectDir, updateDir));
          processedUpdate(projectDir, updateDir);
        }

        // Accumulate results from the project
        dirResults.add(projectResults);

        // We've updated the project. If the directory is empty, we can remove it. If it doesn't
        // remove, that's fine.
          //noinspection ResultOfMethodCallIgnored
          projectDir.delete();
      }
      // Make a per command line import directory report, in the import directory.
      dirResults.makeReport(new File(dirName, FEEDBACK_IMPORT_REPORT));
      results.add(dirResults);
    }

    // Make an overall report, on stdout.
    if (params.report != null) {
      results.makeReport(params.report);
    } else {
      results.makeReport(System.out);
    }
    return results;
  }

  private void processedUpdate(File projectDir, File updateDir) {
    if(params.processed != null) {
      tryMoveDirectory(updateDir, new File(params.processed, projectDir.getName()));
    }
  }

  private void skippedUpdate(File projectDir, File updateDir) {
    if(params.skipped != null) {
      tryMoveDirectory(updateDir, new File(params.skipped, projectDir.getName()));
    }
  }

  private void skippedProject(File projectDir) {
    if(params.skipped != null) {
      tryMoveDirectory(projectDir, params.skipped);
    }
  }

  /**
   * Attempts to move a directory to another directory. If there is already
   * a directory of the same name, falls back to copy and delete. Ignores
   * exceptions.
   * @param sourceDir The directory to be moved.
   * @param targetDir The target directory into which the source directory
   *                  is to be moved.
   */
  private void tryMoveDirectory(File sourceDir, File targetDir) {
    try {
      try {
        FileUtils.moveDirectoryToDirectory(sourceDir, targetDir, true);
      } catch (FileExistsException e) {
        // Already exists (from a different tbloader's feedback), so copy & delete.
        FileUtils.copyDirectoryToDirectory(sourceDir, targetDir);
        FileUtils.deleteDirectory(sourceDir);
      }
    } catch (IOException ie) {
      // We tried to move the directory, but couldn't. So, leave it there.
    }
  }

  /**
   * Imports one content update directory for one project.
   * @param projectDir The project directory. Only needed for its name.
   * @param updateDir The content update directory. Contains the actual user feedback.
   */
  private ImportResults importDirectory(File projectDir, File updateDir) {
    ImportResults results;

    Set<File> filesToImport = gatherFiles(updateDir);

    try {
      openFeedbackProjectDb(projectDir, updateDir);
    } catch (Exception ex) {
      System.err.println(ex.getMessage());
      results = new ImportResults();
      results.updateFailedToImport(projectDir.getName(), updateDir.getName());
      return results;
    }

    results = importFiles(filesToImport);
    ACMConfiguration.getInstance().closeCurrentDb(ACMConfiguration.DB_CLOSE_DISPOSITION.COMMIT);
    if (results.isSuccess()) {
      results.updateImportedNoErrors(projectDir.getName(), updateDir.getName(),
              results.filesImported.size());
    } else {
      results.updateImportedWithErrors(projectDir.getName(), updateDir.getName(),
              results.filesImported.size(), results.filesFailedToImport.size());
    }
    return results;
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
    if (params.importACM != null) {
      if (!ACMConfiguration.getInstance().setCurrentDB(params.importACM)) {
        AccessStatus status = ACMConfiguration.getInstance().getCurrentDB().getDbAccessStatus();
        throw new Exception(String.format("Couldn't open or create DB '%s': %s", params.importACM, status));
      }
      return;
    }

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
      logger.info(String.format("    Opening feedback project %s", feedbackProject));

      // Throws an exception to indicate "not found".
      if (!ACMConfiguration.getInstance().setCurrentDB(feedbackProject)) {
        AccessStatus status = ACMConfiguration.getInstance().getCurrentDB().getDbAccessStatus();
        if (status == AccessStatus.noDbError) {
          // This error is worthwhile to retry -- the db doesn't exist, so create it!
          ACMConfiguration.getInstance().createNewDb(mainProject, feedbackProject);
        } else {
          throw new Exception(String.format("Couldn't open or create DB '%s': %s", feedbackProject, status));
        }
      }
    } catch (Exception setEx) {
      try {
        ACMConfiguration.getInstance().createNewDb(mainProject, feedbackProject);
      } catch (Exception createEx) {
        throw new Exception(String.format("Couldn't open or create DB '%s': %s", feedbackProject, createEx.getMessage()), createEx);
      }
    }
  }

    /**
     * Given an audio item, adjust the feedback category based on the item's duration. If the
     * duration is < 5 seconds, make the category 92-2, "Too Short". If the duration is > 5
     * minutes, make the category 92-6, "Too Long". And if we can't read the duration, make the
     * category 92-8, "Unknown length".
     * @param item The audio item to adjust.
     */
    private static ImportResults.TWEAKS adjustCategoriesForDuration(AudioItem item) {
        // Tweak category based on play time.
        Pattern durationPattern = Pattern.compile("^(\\d\\d+):(\\d\\d).*$");
        MetadataValue<String> metaString = item.getMetadata().getMetadataValue(
                MetadataSpecification.LB_DURATION);
        int seconds;
        ImportResults.TWEAKS result = ImportResults.TWEAKS.NO_CHANGE;
        String newCategory = null;
        if (metaString != null) {
            String duration = metaString.getValue();
            try {
                Matcher matcher = durationPattern.matcher(duration);
                if (matcher.matches()) {
                    seconds = Integer.parseInt(matcher.group(1)) * 60;
                    seconds += Integer.parseInt(matcher.group(2));
                    if (seconds <= too_short_duration) {
                        newCategory = Constants.CATEGORY_TOO_SHORT_FEEDBACK;
                        result = ImportResults.TWEAKS.TOO_SHORT;
                    } else if (seconds >= too_long_duration) {
                        newCategory = Constants.CATEGORY_TOO_LONG_FEEDBACK;
                        result = ImportResults.TWEAKS.TOO_LONG;
                    }
                } else {
                    newCategory = Constants.CATEGORY_UNKNOWN_LENGTH_FEEDBACK;
                    result = ImportResults.TWEAKS.INDETERMINATE;
                }
            } catch (NumberFormatException ex) {
                // can't read this audio item
                newCategory = Constants.CATEGORY_UNKNOWN_LENGTH_FEEDBACK;
                result = ImportResults.TWEAKS.INDETERMINATE;
            }
            if (newCategory != null) {
                Taxonomy taxonomy = ACMConfiguration.getInstance()
                        .getCurrentDB()
                        .getMetadataStore()
                        .getTaxonomy();
                item.removeCategory(taxonomy.getCategory(CATEGORY_UNCATEGORIZED_FEEDBACK));
                item.addCategory(taxonomy.getCategory(newCategory));
            }
        }
        return result;
    }

  /**
   * Imports a set of files into the currently opened Acm project.
   * @param filesToImport A Set<File> of files to import.
   * @return True if all imported with no error, False if one or more errors.
   */
  private ImportResults importFiles(Set<File> filesToImport) {
    ImportResults results = new ImportResults();
    int count = 0;
    // Array hack because the variable must final. Not the contents, though.
    final ImportResults.TWEAKS[] tweaks = new ImportResults.TWEAKS[1];
    Metadata metadata = new Metadata();
    logger.info(String.format("      Importing %d files", filesToImport.size()));

    AudioImporter importer = AudioImporter.getInstance();
    MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

    for (File file : filesToImport) {
      try {
        if (params.verbose) {
          System.out.printf("Importing %d of %d: %s%n", ++count, filesToImport.size(), file.getName());
          logger.info(String.format("        Importing %d of %d: %s", ++count, filesToImport.size(), file.getName()));
        }
        // Reset the value, because the lambda may not be called, if the file is already in DB.
        tweaks[0] = ImportResults.TWEAKS.NO_CHANGE;
        importer.importAudioItemFromFile(file,
                            (item)->{
                                item.getMetadata().addValuesFromOtherWithExclusions(metadata);
                                tweaks[0] = adjustCategoriesForDuration(item);
                            });
        results.fileImported(file.getName(), tweaks[0]);
      } catch (Exception e) {
        System.err.printf("Failed to import '%s': %s%n", file.getName(), e.getMessage());
        logger.info(String.format("Failed to import '%s': %s", file.getName(), e.getMessage()));
        results.fileFailedToImport(file.getName());
      }
    }

    return results;
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

  /**
   * Checks whether user feedback for an update within a project should be
   * skipped or imported. It should be skipped if it is in the deferred list.
   * @param project The project with updates.
   * @param deployment The update.
   * @return True if the update should be skipped, False if it should be imported.
   */
  private boolean shouldUpdateBeSkipped(String project, String deployment) {
    if (params.importACM != null) {
      return false;
    }
    Includelister includelister = getDeploymentsIncludelistForProject(project);
    // Function is "isSkipped", opposite of "isIncluded"
    return ! includelister.isIncluded(deployment);
  }

  /**
   * Gets the includelist of deployments for which user feedback should be processed.
   * @param project The project name.
   * @return an includelist object that will filter deployments.
   */
  private Includelister getDeploymentsIncludelistForProject(String project) {
    project = project.toUpperCase();
    Includelister includelister = deploymentsIncludelistCache.get(project);
    if (includelister == null) {
      File includelistFile = ACMConfiguration.getInstance().getPathProvider(project).getProgramUserFeedbackInclusionFile();
      // Create, and save for next time.
      includelister = new Includelister(includelistFile, Includelister.OPTIONS.regex);
      deploymentsIncludelistCache.put(project, includelister);
    }
    return includelister;
  }

  /**
   * A class to hold the statistics and results of an import operation.
   *
   * Results can be accumulated from other results objects, allowing for
   * individual and aggregate results.
   */
  private static class ImportResults {
    public enum TWEAKS {NO_CHANGE, TOO_SHORT, TOO_LONG, INDETERMINATE}

    List<String> filesImported = new ArrayList<>();
    int filesTooShort = 0;
    int filesTooLong = 0;
    int filesIndeterminate = 0;
    List<String> filesFailedToImport = new ArrayList<>();
    List<String> updatesImportedNoErrors = new ArrayList<>();
    List<String> updatesImportedWithErrors = new ArrayList<>();
    List<String> updatesFailedToImport = new ArrayList<>();
    List<String> projectsSkipped = new ArrayList<>();
    List<String> updatesSkipped = new ArrayList<>();

    void fileImported(String filename) {
      fileImported(filename, TWEAKS.NO_CHANGE);
    }
    void fileImported(String filename, TWEAKS tweaks) {
        filesImported.add(filename);
        switch (tweaks) {
        case NO_CHANGE:
            break;
        case TOO_SHORT:
            filesTooShort++;
            break;
        case TOO_LONG:
            filesTooLong++;
            break;
        case INDETERMINATE:
            filesIndeterminate++;
            break;
        }
    }
    void fileFailedToImport(String filename) {
      filesFailedToImport.add(filename);
    }
    void updateImportedNoErrors(String projectName, String updateName, int numImported) {
      updatesImportedNoErrors.add(String.format("%s / %s : %d file(s)", projectName, updateName, numImported));
    }
    void updateImportedWithErrors(String projectName, String updateName, int numImported, int numErrors) {
      updatesImportedWithErrors.add(String.format("%s / %s : %d file(s), %d error(s)", projectName, updateName, numImported, numErrors));
    }
    void updateFailedToImport(String projectName, String updateName) {
      updatesFailedToImport.add(String.format("%s / %s", projectName, updateName));
    }
    void projectSkipped(String projectName) {
      projectsSkipped.add(projectName);
    }
    void updateSkipped(String projectName, String updateName) {
      updatesSkipped.add(String.format("%s / %s", projectName, updateName));
    }

    public ImportResults add(ImportResults moreResults) {
      filesImported.addAll(moreResults.filesImported);
      filesFailedToImport.addAll(moreResults.filesFailedToImport);
      updatesImportedNoErrors.addAll(moreResults.updatesImportedNoErrors);
      updatesImportedWithErrors.addAll(moreResults.updatesImportedWithErrors);
      updatesFailedToImport.addAll(moreResults.updatesFailedToImport);
      projectsSkipped.addAll(moreResults.projectsSkipped);
      updatesSkipped.addAll(moreResults.updatesSkipped);
      filesTooShort += moreResults.filesTooShort;
      filesTooLong += moreResults.filesTooLong;
      filesIndeterminate += moreResults.filesIndeterminate;
      return this;
    }
    public boolean isSuccess() {
      return filesFailedToImport.size() == 0 && updatesFailedToImport.size() == 0;
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
    int getExitCode() {
      if (updatesFailedToImport.size() == 0) {
        if (filesImported.size() > 0 && filesFailedToImport.size() == 0)  return 0;
        if (filesImported.size() > 0)   return 1;
        if (filesFailedToImport.size() == 0) return 2;
        // There were files, but all of them failed to import. Strange.
        return 20;
      }

      // There were projects that failed to open. Should not happen.
      return 21;
    }

    void makeReport(PrintStream ps) {
      new Report(ps, false).generate();
    }
    void makeReport(File file) {
      try {
        boolean html = file.getName().toLowerCase().endsWith(".html");
        // If the file exists, append a new report to it.
        FileOutputStream fos = new FileOutputStream(file, true);
        PrintStream ps = new PrintStream(fos);

        new Report(ps, html).generate();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    private class Report {
        PrintStream ps;
        boolean html;

        Report(PrintStream ps, boolean html) {
          this.ps = ps;
          this.html = html;
        }

      /**
       * Create a report on the results of the feedback import.
       *
       */
      void generate() {
        reportHeading(String.format("User feedback import run %s", new Date().toString()));
        if (html) {
          ps.print("<ul style=\"font-family:monospace; list-style:none\">");
        }
        reportLine(String.format("%4d file(s) imported", filesImported.size()));
        if (filesTooShort>0) reportLine(String.format("%4d file(s) were too short, < %d seconds", filesTooShort, too_short_duration));
        if (filesTooLong>0) reportLine(String.format("%4d file(s) were too long, > %d seconds", filesTooLong, too_long_duration));
        if (filesIndeterminate>0) reportLine(String.format("%4d file(s) were of indeterminate length", filesIndeterminate));
        reportDetailLine("file(s) failed to import", filesFailedToImport);
        reportDetailLine("update(s) imported with no file errors",
                updatesImportedNoErrors);
        reportDetailLine("update(s) imported with file errors",
                updatesImportedWithErrors);
        reportDetailLine("update(s) failed to open for imports",
                updatesFailedToImport);
        reportDetailLine("unknown project(s) skipped",
                projectsSkipped);
        reportDetailLine("deferred update(s) skipped", updatesSkipped);
        reportLine(String.format("  Return code: %d", getExitCode()));
        if (html) {
          ps.print("</ul>");
        }
        ps.flush();
      }

      /**
       * Print a report line with optional details. If html format, surround with
       * <li></li>, and place any details in a <ul></ul>
       *
       * @param title   The line to print.
       * @param details Details for the line, if any.
       */
      private void reportDetailLine(String title, List<String> details) {
        if (html) ps.print("<li>");
        ps.printf("%4d %s", details.size(), title);
        if (details.size() > 0) {
          if (html)
              ps.print("<ul>");
          else
              ps.println();
          for (String d : details) {
            if (html)
              ps.print("<li>");
            else
              ps.print("       ");
            ps.printf("%s", d);
            if (html)
              ps.print("</li>");
            else
              ps.println();
          }
          if (html) ps.print("</ul>");
        }
        if (html)
          ps.print("</li>");
        else
          ps.println();
      }

      /**
       * Print a report line. If html format, surround with <li></li>
       *
       * @param line The line to print.
       */
      private void reportLine(String line) {
        if (html) ps.print("<li>");
        ps.print(line);
        if (html)
          ps.print("</li>");
        else
          ps.println();
      }

      private void reportHeading(String line) {
        if (html) ps.print("<h2>");
        ps.print(line);
        if (html)
          ps.print("</h2>");
        else
          ps.println();
      }

    }
  }

  private static void printUsage(CmdLineParser parser) {
    System.err.println(
            "java -cp acm.jar:lib/* org.literacybridge.acm.utils.FeedbackImporter [--verbose] <dir1> [<dir2> ...]");
    parser.printUsage(System.err);
  }

  private static final class Params {
    @Option(name="--verbose", aliases="-v", usage="Give verbose output.")
    boolean verbose;

    @Option(name="--processed", usage="Move processed sub-directories to this directory.")
    File processed;

    @Option(name="--skipped", usage="Move skipped sub-directories to this directory.")
    File skipped;

    @Option(name="--report", usage="Generate a report to this file. If *.html, format as HTML.")
    File report;

    @Option(name="--ACM", usage="Force all input to be imported here.")
    String importACM;

    @Argument(usage="Directory(ies) to import.")
    List<String> dirNames;
  }

}
