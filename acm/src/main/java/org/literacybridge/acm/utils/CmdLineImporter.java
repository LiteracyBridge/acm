package org.literacybridge.acm.utils;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Taxonomy;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CmdLineImporter {
    static String SUCCESS_DIR = "success";

    private Params params;
    private Set<File> filesToImport = new HashSet<>();
    private List<Category> categories;

    public static void main(String[] args) throws Exception {
        new LogHelper().inDirectory("logs").withName("CmdLineImporter.log").initialize();
        Params params = new Params();
        CmdLineParser parser = new CmdLineParser(params);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            printUsage(parser);
            System.exit(1);
        }

        CmdLineImporter importer = new CmdLineImporter(params);
        if (!importer.validateParams()) {
            printUsage(parser);
            System.exit(1);
        }

        boolean success = importer.doImport();
        if (success) {
            System.out.println("All files imported without an exception.");
        } else {
            System.out.println("At least one file could not be imported.");
        }

        System.exit(success ? 0 : 4);
    }


    private CmdLineImporter(Params params) {
        this.params = params;
    }

    /**
     * Validates the command line params, and prepares them for the actual import.
     * @return true if params are valid
     */
    private boolean validateParams() {
        boolean ok = true;
        if (params.dirs.isEmpty()) {
            ok = false;
            System.err.println("Must specify files to import.");
        }

        // start ACM and acquire write access
        try {
            CommandLineParams acmParams = new CommandLineParams();
            acmParams.disableUI = true;
            acmParams.sharedACM = params.acmName;
            ACMConfiguration.initialize(acmParams);
            ACMConfiguration.getInstance().setCurrentDB(acmParams.sharedACM);

            if (!ACMConfiguration.getInstance().getCurrentDB().isWritable()) {
                System.err.println("Unable to open ACM for write.");
                System.exit(3);
            }
        } catch (Exception e) {
            System.err.println("Unable to open ACM.");
            System.exit(2);
        }

        if (params.categories != null && !params.categories.isEmpty()) {
            categories = new ArrayList<>();
            Taxonomy taxonomy = ACMConfiguration.getInstance()
                .getCurrentDB()
                .getMetadataStore()
                .getTaxonomy();
            for (String catString : params.categories) {
                Category category = taxonomy.getCategory(catString);
                if (category == null) {
                    System.err.println(String.format("'%s' is not a valid category code.", catString));
                    ok = false;
                }
                categories.add(category);
            }
        }

        Set<File> inputDirs = Sets.newHashSet();
        for (String dirName : params.dirs) {
            File dir = new File(dirName);
            if (!dir.exists()) {
                System.err.println("Directory does not exist: " + dirName);
                System.exit(1);
            }
            inputDirs.add(dir);
        }

        try {
            gatherFiles(inputDirs, filesToImport, params.recursive);
        } catch (Exception ex) {
            ok = false;
            System.err.println("Can't scan source directories");
        }

        return ok;
    }

    /**
     * Opens the ACM, imports the files, and closes the ACM.
     * @return true if all successful, false if any errors.
     */
    private boolean doImport() {

        boolean success = false;
        try {
            boolean allImportedOk = importFiles();
            ACMConfiguration.getInstance().commitCurrentDB();
            success = allImportedOk;
        } catch(Exception ex) {
            ACMConfiguration.getInstance().closeCurrentDB();
        }
        return success;
    }

    /**
     * Iterates over filesToImport, and imports each one.
     * @return true if all imported successfully
     */
    private boolean importFiles() {
        boolean success = true;
        AudioImporter importer = AudioImporter.getInstance();
        MetadataStore metadataStore = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        long count = 0;

        for (File file : filesToImport) {
            try {
                System.out.println(String.format("%sImporting file %d of %d: %s" , params.dryrun?"NOT ":"", ++count, filesToImport.size(), file));
                if (params.dryrun) continue;

                importer.importOrUpdateAudioItemFromFile(file, (item) -> {
                    if (categories != null) {
                        item.removeAllCategories();
                        item.addCategories(categories);
                    }
                });
                if (!params.moveToDir.equals(".")) {
                    FileUtils.moveToDirectory(file,
                        new File(file.getParentFile(), params.moveToDir), true);
                }
            } catch (Exception e) {
                success = false;
                logError(file, e);
            }
        }

        return success;
    }

    /**
     * Given a set of directories, add any ".a18" files to the list of files.
     * @param inputDirs List of directories in which to look for .a18 files.
     * @param filesToImport Any files found are added here.
     * @param recursive If true, recurse into any sub-directories found. Note that the "moveToDir" is
     *                  not examined.
     */
    private void gatherFiles(Iterable<File> inputDirs, Set<File> filesToImport, boolean recursive) {
        for (File dir : inputDirs) {
            if (dir.isDirectory() && recursive) {
                File[] subdirs = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory() && !file.getName().equals(params.moveToDir);
                    }
                });

                gatherFiles(Sets.newHashSet(subdirs), filesToImport, recursive);
            }

            File[] a18Files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String fileName) {
                    return fileName.toLowerCase().endsWith(".a18");
                }
            });

            Collections.addAll(filesToImport, a18Files);
        }
    }

    private static void logError(File a18File, Exception exception) {
        File errorFile = new File(a18File.getParentFile(), a18File.getName() + ".error.txt");
        try (PrintStream out = new PrintStream(errorFile)) {
            exception.printStackTrace(out);
        } catch (IOException e) {
            // ignore
        }
    }

    private static void printUsage(CmdLineParser parser) {
        System.err.println(String.format("java -cp acm.jar:lib/* %s ", CmdLineImporter.class.getName()));
        parser.printUsage(System.err);
    }

    private static final class Params {
        @Option(name = "--recursive", aliases={"-r"}, usage = "Traverse directories recursively to discover .a18 files.")
        public boolean recursive = false;

        @Option(name = "--dryrun", aliases={"-n"}, usage = "Do not actually import or move files.")
        public boolean dryrun = false;

        @Option(name = "--acm", aliases={"-a","-acm"}, usage = "Name of the project or ACM directory.", metaVar="ACM")
        public String acmName;

        @Option(name="--moveto", usage="Upon successful import, move files to IMPORT/TO, default 'success'.", metaVar="TO")
        String moveToDir = SUCCESS_DIR;

        @Option(name="--category", usage="Assign to category.", metaVar="CAT")
        List<String> categories;

        @Argument(metaVar="IMPORT[,IMPORT...]", required=true, usage="Directories to import")
        public List<String> dirs;
    }

}
