package org.literacybridge.acm.tools;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.importexport.CSVExporter;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.utils.LogHelper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class CSVDatabaseExporter {
    public static void main(String[] args) throws Exception {
        new LogHelper().inDirectory("logs").withName("CSVDatabaseExporter.log").initialize();
        Params params = new Params();
        CmdLineParser parser = new CmdLineParser(params);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            printUsage(parser);
            System.exit(1);
        }

        CSVDatabaseExporter exporter = new CSVDatabaseExporter(params);
        exporter.export();
    }

    private Params params;

    private CSVDatabaseExporter(Params params) throws Exception {
        this.params = params;

        CommandLineParams configParams = new CommandLineParams();
        configParams.disableUI = true;
        configParams.sandbox = true;
        ACMConfiguration.initialize(configParams);
        ACMConfiguration.getInstance().setCurrentDB(params.acmName);
    }

    private void export() throws IOException {
        Writer csvWriter = (params.csvName == null) ?
                           new PrintWriter(System.out) :
                           new FileWriter(new File(params.csvName));
        if (params.listCategories || params.listFullCategories) {
            CSVExporter.exportCategoryCodes(csvWriter, optionsFromParams());
        } else {
            Iterable<AudioItem> items = ACMConfiguration.getInstance()
                .getCurrentDB()
                .getMetadataStore()
                .getAudioItems();
            CSVExporter.exportMessages(items, csvWriter, optionsFromParams());
        }
    }

    private CSVExporter.OPTION[] optionsFromParams() {
        List<CSVExporter.OPTION> opts = new ArrayList<>();
        if (params.listFullCategories
            || params.categoryAsFullNames) opts.add(CSVExporter.OPTION.CATEGORY_AS_FULL_NAME);
        if (params.noheader) opts.add(CSVExporter.OPTION.NO_HEADER);
        if (params.categoryAsCodes) opts.add(CSVExporter.OPTION.CATEGORIES_AS_CODES);
        return opts.toArray(new CSVExporter.OPTION[0]);
    }

    private static void printUsage(CmdLineParser parser) {
        System.err.println(String.format("java -cp acm.jar:lib/* %s ",
            CSVDatabaseExporter.class.getName()));
        parser.printUsage(System.err);
    }

    /**
     * Class holding command line options for the export.
     */
    private static final class Params {
        @Option(name = "--verbose", aliases = "-v", usage = "Give verbose output.")
        boolean verbose;

        @Option(name = "--categorycodes", aliases = "-c", usage = "Export category codes instead of names.")
        boolean categoryAsCodes;

        @Option(name = "--categoryfullnames", aliases = "-n", usage = "Export category full names.")
        boolean categoryAsFullNames;

        @Option(name = "--listcategories", aliases = "-l", usage = "List all of the category codes with short category names.")
        boolean listCategories;

        @Option(name = "--listfullcategories", aliases = "-f", usage = "List all of the category codes with short and full category names.")
        boolean listFullCategories;

        @Option(name = "--noheader", usage = "Do not put headers in the exported files.")
        boolean noheader = false;

        @Argument(usage = "Project or ACM name to export.", index = 0, required = true, metaVar = "ACM")
        String acmName;

        @Argument(usage = "CSV file name, default stdout.", index = 1, metaVar = "CSVFILE")
        String csvName;
    }

}
