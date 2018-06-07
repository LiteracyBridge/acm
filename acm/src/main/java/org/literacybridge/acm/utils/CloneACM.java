package org.literacybridge.acm.utils;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.CommandLineParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class CloneACM {

    /**
     *
     * Clones an ACM database to another name WITHOUT CONTENT. Only users and properties are copied.
     *
     */

    private static final Logger logger = LoggerFactory.getLogger(MessageExtractor.class);

    public static void main(String[] args) throws IOException {
        new LogHelper().inDirectory("logs").withName("CloneACM.log").initialize();
        Params params = new Params();
        CmdLineParser parser = new CmdLineParser(params);
        try {
            parser.parseArgument(args);
        } catch (Exception e) {
            printUsage(parser);
            System.exit(100);
        }

        CloneACM cloner = new CloneACM(params);
        if (!cloner.validateCommandLineArgs()) {
            printUsage(parser);
            System.exit(100);
        }

        System.out.println(String.format("ACM Cloner..."));
        boolean result = cloner.doClone();

        System.exit(result ? 0 : 4);
    }

    private final Params params;

    private String fromACM;
    private String toACM;

    private CloneACM(Params params) {
        this.params = params;
        CommandLineParams acmParams = new CommandLineParams();
        acmParams.disableUI = true;
        ACMConfiguration.initialize(acmParams);
    }

    /**
     * Validates command line arguments, and prepares for processing.
     * @return true if no errors in command line arguments.
     */
    private boolean validateCommandLineArgs() {
        boolean ok = true;

        fromACM = ACMConfiguration.cannonicalAcmDirectoryName(params.fromACM);
        toACM = ACMConfiguration.cannonicalAcmDirectoryName(params.toACM);

        if (!(new File(ACMConfiguration.getInstance().getGlobalShareDir(), fromACM)).exists()) {
            ok = false;
            System.err.println(String.format("File '%s' does not exist", fromACM));
        }
        if ((new File(ACMConfiguration.getInstance().getGlobalShareDir(), toACM)).exists()) {
            ok = false;
            System.err.println(String.format("File '%s' already exists", toACM));
        }
        return ok;
    }


    private boolean doClone() {
        boolean ok = false;
        try {
            if (params.verbose) {
                System.out.println(String.format("Attempting clone from %s to %s", fromACM, toACM));
            }
            ACMConfiguration.getInstance().createNewDb(fromACM, toACM);
            ACMConfiguration.getInstance().getCurrentDB().commitDbChanges();
            ok = true;

            if (params.verbose) {
                System.out.println(String.format("Cloned from %s to %s", fromACM, toACM));
            }
        } catch (Exception ex) {
            System.out.printf("Exception attempting database clone: %s\n", ex.getMessage());
            if (params.verbose) {
                ex.printStackTrace();
            }
        } finally {
            ACMConfiguration.getInstance().closeCurrentDB();
        }

        return ok;
    }

    private static void printUsage(CmdLineParser parser) {
        System.err.println(String.format(
            "java -cp acm.jar:lib/* %s [--verbose] <dir1> [<dir2> ...]", CloneACM.class));
        parser.printUsage(System.err);
    }

    private static final class Params {
        @Option(name="--verbose", aliases="-v", usage="Give verbose output.")
        boolean verbose;

        @Option(name="--from", usage="ACM or project to be cloned.", metaVar="ACM", required=true)
        String fromACM;

        @Option(name="--as", usage="The new ACM or project.", metaVar="ACM", required=true)
        String toACM;

    }

}

