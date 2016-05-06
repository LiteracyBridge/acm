package org.literacybridge.acm.tools;

import java.io.File;
import java.io.IOException;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.CommandLineParams;
import org.literacybridge.acm.importexport.CSVExporter;

public class CSVDatabaseExporter {
    private CSVDatabaseExporter(String acmName) throws Exception {
        CommandLineParams params = new CommandLineParams();
        params.disableUI = true;
        params.sandbox = true;
        params.sharedACM = acmName;
        Application.startUp(params);
    }

    private void export(File csvFile) throws IOException {
        CSVExporter.export(ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().getAudioItems(), csvFile);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            printUsage();
            System.exit(1);
        }

        CSVDatabaseExporter exporter = new CSVDatabaseExporter(args[0]);
        exporter.export(new File(args[1]));
    }

    private static void printUsage() {
        System.out.println("Usage: java -cp acm.jar:lib/* org.literacybridge.acm.tools.CSVDatabaseExporter <acm_name> <csv_file>");
    }


}
