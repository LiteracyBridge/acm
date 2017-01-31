package org.literacybridge.acm.tools;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.importexport.CSVExporter;
import org.literacybridge.acm.utils.FeedbackImporter;

public class CSVDatabaseExporter {
    public static void main(String[] args) throws Exception {
        Params params = new Params();
        CmdLineParser parser = new CmdLineParser(params);
        parser.parseArgument(args);

        if (params.args.size() != 2) {
            printUsage();
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
        ACMConfiguration.getInstance().setCurrentDB(ACMConfiguration.cannonicalAcmDirectoryName(params.args.get(0)));
    }

    private void export() throws IOException {
        File csvFile = new File(params.args.get(1));
        if (params.listCategories || params.listFullCategories) {
            CSVExporter.exportCategoryCodes(csvFile, params.listFullCategories);
        } else {
            CSVExporter.export(ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().getAudioItems(),
                               csvFile, params.categoryCodes);
        }
    }

    private static void printUsage() {
        System.out.println(
                "Usage: java -cp acm.jar:lib/* org.literacybridge.acm.tools.CSVDatabaseExporter <acm_name> <csv_file>");
    }

    /**
     * Class holding command line options for the export.
     */
    private static final class Params {
        @Option(name = "--verbose", aliases = "-v", usage = "Give verbose output.")
        boolean verbose;

        @Option(name = "--categorycodes", aliases = "-c", usage = "Export category codes instead of names.")
        boolean categoryCodes;

        @Option(name = "--listcategories", aliases = "-l", usage = "List all of the category codes with short category names.")
        boolean listCategories;

        @Option(name = "--listfullcategories", aliases = "-f", usage = "List all of the category codes with full category names.")
        boolean listFullCategories;

        @Argument(usage = "ACM to export, csv file name.")
        List<String> args;
    }

}
