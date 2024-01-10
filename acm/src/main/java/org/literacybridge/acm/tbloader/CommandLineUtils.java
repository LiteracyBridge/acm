package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.utils.ExternalCommandRunner;
import org.literacybridge.core.OSChecker;
import org.literacybridge.core.tbloader.FileSystemUtilities;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLineUtils extends FileSystemUtilities {
    //private static final Logger LOG = Logger.getLogger(CommandLineUtils.class.getName());
    private final File windowsUtilsDirectory;
    
    private static final long CHKDSK_TIMEOUT = 60 * 1000;

    public CommandLineUtils(File softwareDir) {
        this.windowsUtilsDirectory = softwareDir;
    }

   @Override
    public boolean formatDisk(String drive, String newLabel)  {
        if (!OSChecker.WINDOWS) {
            throw new IllegalStateException("format operation is only supported on Windows");
        }
        System.out.printf("Formatting %s with label %s\n", drive, newLabel);
        boolean result = DiskFormatter.format(drive, newLabel);
        System.out.printf("Format completted: %s\n", result?"success":"failure");
        return result;
    }

    @Override
    public RESULT checkDisk(String drive) throws IOException {
        if (!OSChecker.WINDOWS) {
            throw new IllegalStateException("chkdsk operation is only supported on Windows");
        }
        long startTime = System.currentTimeMillis();
        System.out.printf("Checking drive %s\n", drive);
        RESULT result = FileSystemCheck.checkDisk(drive);
        System.out.printf("Chkdsk completed in %dms: %s\n", System.currentTimeMillis()-startTime, result.toString());
        return result;
    }

    @Override
    public RESULT checkDiskAndFix(String drive, String logfilename) throws IOException {
        if (!OSChecker.WINDOWS) {
            throw new IllegalStateException("chkdsk operation is only supported on Windows");
        }
        long startTime = System.currentTimeMillis();
        System.out.printf("Checking drive %s (with /f).\n", drive);
        RESULT result = FileSystemCheck.checkDiskAndFix(drive, logfilename);
        System.out.printf("Chkdsk /f completed in %dms: %s\n", System.currentTimeMillis()-startTime, result.toString());
        return result;
    }

    @Override
    public boolean relabel(String drive, String newLabel) throws IOException {
        if (!OSChecker.WINDOWS) {
            throw new IllegalStateException("relabel operation is only supported on Windows");
        }
        return super.relabel(drive, newLabel);
    }

    public boolean hasDfuDriver() {
        if (!OSChecker.WINDOWS) return false;
        return DFU_DriverDetector.hasDFU_Driver();
    }

    /**
     * Class to determine if the STMicro DFU_Driver is installed on this computer. It actually looks for a registry
     * value set by our DFU support installer, because non-privileged users can't run 'pnputil' (the best and most
     * portable way to determine if the driver is actually installed). So, this may return incorrect results.
     *
     * If it returns an incorrect answer, running setup again should fix the problem.
     */
    private static class DFU_DriverDetector implements BiFunction<Writer, ExternalCommandRunner.LineReader, Boolean> {
        private static final Pattern INSTALLED = Pattern.compile("(?i).*DFU_installed.*REG_DWORD\\s*(0x\\d*).*");

        private static boolean hasDFU_Driver() {
            DFU_DriverDetector detector = new DFU_DriverDetector();
            String[] command = new String[]{"reg", "query", "HKLM\\Software\\Amplio-Network", "/reg:64"};
            return Boolean.TRUE.equals(ExternalCommandRunner.run(command, detector));
        }

        boolean foundDfu = false;
        @Override
        public Boolean apply(Writer writer, ExternalCommandRunner.LineReader stream) {
            String line;
            try {
                while ((line = stream.readLine()) != null) {
                    System.out.println(line);
                    Matcher matcher = INSTALLED.matcher(line);
                    if (matcher.matches()) {
                        gotEntry(matcher);
                    }
                }
                System.out.println("Got EOF on DFU detector.");
            } catch (IOException ignored) {
            }
            return foundDfu;
        }

        private void gotEntry(Matcher matcher) {
            String valueString = matcher.group(1);
            int radix = 10;
            int value = 0;
            if (valueString.startsWith("0x")) {
                valueString = valueString.substring(2);
                radix = 16;
            }
            try {
                value = Integer.parseInt(valueString, radix);
            } catch (Exception ignored) {}
            foundDfu |= value != 0;
        }
    }

    private static class DiskFormatter implements BiFunction<Writer, ExternalCommandRunner.LineReader, Boolean> {
        private static final Pattern INSERT_NEW_DISK = Pattern.compile("(?i)Insert new disk for drive.*");
        private static final Pattern ENTER_WHEN_READY = Pattern.compile("(?i).*ENTER when ready\\.{3}.*$");
        private static final Pattern FORMAT_COMPLETE = Pattern.compile("(?i)Format complete.*");
        private static final Pattern VOLUME_SERIAL = Pattern.compile("(?i)Volume Serial Number is (\\p{XDigit}{4}-\\p{XDigit}{4}).*");

        private static boolean format(String volume, String label) {
            DiskFormatter foh = new DiskFormatter();
            String[] command = new String[]{"cmd", "/c", "format", volume, "/Q", "/V:"+label};
            return Boolean.TRUE.equals(ExternalCommandRunner.run(command, foh));
        }

        boolean formatComplete = false;

        @Override
        public Boolean apply(java.io.Writer writer, ExternalCommandRunner.LineReader stream) {
            List<LineHandler<BiConsumer<java.io.Writer, java.util.regex.Matcher>>> lineHandlers = new ArrayList<>();
            lineHandlers.add(new LineHandler<>(FORMAT_COMPLETE, this::gotFormatComplete));
            lineHandlers.add(new LineHandler<>(VOLUME_SERIAL, this::gotVolumeSerial));

            String line;
            try {
                if (waitForInsert(stream) && waitForReady(writer, stream)) {
                    while ((line = stream.readLine()) != null) {
                        System.out.println(line);
                        for (LineHandler<BiConsumer<Writer, Matcher>> lh : lineHandlers) {
                            Matcher matcher = lh.linePattern.matcher(line);
                            if (matcher.matches()) {
                                lh.lineProcessor.accept(writer, matcher);
                                break;
                            }
                        }
                    }
                }
                System.out.println("Got EOF on program output.");
            } catch (IOException ignored) {
            }
            return formatComplete;
        }

        private void gotVolumeSerial(Writer writer, Matcher matcher) {
            String volumeSerial = matcher.group(1);
        }

        private void gotFormatComplete(Writer writer, Matcher matcher) {
            formatComplete = true;
        }

        private boolean waitForInsert(ExternalCommandRunner.LineReader stream) throws IOException {
            String line;
            while ((line = stream.readLine()) != null) {
                System.out.println(line);
                if (INSERT_NEW_DISK.matcher(line).matches())
                    return true;
            }
            return false;
        }

        private boolean waitForReady(Writer writer, ExternalCommandRunner.LineReader stream) throws IOException {
            String line;
            while ((line = stream.readLine(ENTER_WHEN_READY)) != null) {
                System.out.println(line);
                if (ENTER_WHEN_READY.matcher(line).matches()) {
                    writer.write("\\n");
                    writer.flush();
                    writer.close();
                    return true;
                }
            }
            return false;
        }
    }

    private static class FileSystemCheck implements BiFunction<Writer, ExternalCommandRunner.LineReader, RESULT> {
        private static final Pattern NO_PROBLEMS = Pattern.compile("(?i).*(?:Windows has scanned the file system and found no problem|Windows a analys.* le syst.*me de fichiers sans trouver de probl.*).*");
        private static final Pattern CONVERT_CHAINS = Pattern.compile("(?i).*(?:(?:files.*|chains.*){2}|Convertir les liens perdus en fichiers).*\\?");
        private static final Pattern FOUND_ERRORS = Pattern.compile("(?i).*(?:Windows found errors on the disk, but will not fix them|Windows a trouv.* des erreurs sur le disque, mais ne les corrigera pas).*");
        private static final Pattern NO_F_PARAM = Pattern.compile("(?i).*(?:because disk checking was run without the /F|effectu.* sans le param.* /F \\(correction\\)).*");
        private static final Pattern PERCENT_COMPLETE = Pattern.compile("(?i)\\D*(\\d+) (?:percent complete|pour cent effectu.*).*");
        private static final Pattern FORCE_DISMOUNT = Pattern.compile("(?i).*(?:Would you like to force a dismount on this volume|Voulez-vous forcer le d.*montage de ce volume) *\\? \\((Y/N|O/N)\\) ");
        private static final Pattern INSUFFICIENT_PRIVILEGES = Pattern.compile("(?i).*Access Denied as you do not have sufficient privileges.*");


        private final List<LineHandler<BiConsumer<Writer, Matcher>>> handlers = Arrays.asList(
            new LineHandler<BiConsumer<Writer, Matcher>>(CONVERT_CHAINS, this::gotConvertChains),
            new LineHandler<BiConsumer<Writer, Matcher>>(FORCE_DISMOUNT, this::gotForceDismount),
            new LineHandler<BiConsumer<Writer, Matcher>>(FOUND_ERRORS, this::foundErrors),
            new LineHandler<BiConsumer<Writer, Matcher>>(NO_F_PARAM, this::noFParam),
            new LineHandler<BiConsumer<Writer, Matcher>>(NO_PROBLEMS, this::noProblems),
            new LineHandler<BiConsumer<Writer, Matcher>>(INSUFFICIENT_PRIVILEGES, this::insufficientPrivileges)
        );
        // Patterns of output from chkdsk that require a response.
        private final List<Pattern> patterns = Arrays.asList(FORCE_DISMOUNT, CONVERT_CHAINS);

        public ExternalCommandRunner.LineReader lineReader;

        private boolean hasErrors = false;
        private boolean hasNoProblems = false;

        private RESULT result = RESULT.TIMEOUT;


        private static RESULT checkDisk(String volume) throws FileNotFoundException {
            FileSystemCheck fsck = new FileSystemCheck(null);
            String[] command = new String[]{"chkdsk", volume};
            RESULT result = ExternalCommandRunner.run(command, fsck, CHKDSK_TIMEOUT);
            if (result == null) {
                result = RESULT.TIMEOUT;
            }
            return result;
        }

        private static RESULT checkDiskAndFix(String volume, String logfileName) throws FileNotFoundException {
            FileSystemCheck fsck = new FileSystemCheck(logfileName);
            String[] command = new String[]{"chkdsk", "/f", volume};
            RESULT result = ExternalCommandRunner.run(command, fsck, CHKDSK_TIMEOUT);
            if (result == null) {
                result = RESULT.TIMEOUT;
            }
            return result;
        }

        private FileSystemCheck(String logfileName) throws FileNotFoundException {
            PrintStream logfile = (logfileName != null) ? new PrintStream(logfileName) : System.out;
        }

        /**
         * Waits for chkdsk output.
         *
         * @param stream Output from the chkdsk command programmer.
         * @return result the chkdsk operation
         */
        @Override
        public RESULT apply(Writer writer, ExternalCommandRunner.LineReader stream) {
            lineReader = stream;
            try {
                String programOutputLine;
                while ((programOutputLine = stream.readLine(patterns)) != null) {
                    Matcher matcher = PERCENT_COMPLETE.matcher(programOutputLine);
                    if (matcher.matches()) {
                        this.percentComplete(writer, matcher);
                    } else {
                        System.out.println(programOutputLine);
                        for (LineHandler<BiConsumer<Writer, Matcher>> lh : handlers) {
                            matcher = lh.linePattern.matcher(programOutputLine);
                            if (matcher.matches()) {
                                lh.lineProcessor.accept(writer, matcher);
                                break;
                            }
                        }
                    }
                    if (hasErrors || hasNoProblems)
                        break;
                }
                System.out.println("Got EOF on chkdsk output.");
            } catch (IOException ignored) {
            }
            return result;
        }

        private void noProblems(Writer writer, Matcher matcher) {
            System.out.print("Found 'no problems'.");
            hasNoProblems = true;
            result = RESULT.SUCCESS;
        }

        private void insufficientPrivileges(Writer writer, Matcher matcher) {
            hasErrors = true;
            result = RESULT.ACCESS_DENIED;
        }

        private void gotConvertChains(Writer writer, Matcher matcher) {
            reply("n\r\n", writer, matcher);
        }

        private void gotForceDismount(Writer writer, Matcher matcher) {
            // Group is 'Y/N' or 'O/N', depending on language.
            char ch = matcher.group(1).charAt(0);
            String response = ch + "\r\n";
            reply(response, writer, matcher);
        }

        private void reply(String reply, Writer writer, Matcher unused) {
            try {
                writer.write(reply);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void foundErrors(Writer writer, Matcher matcher) {
            hasErrors = true;
            result = RESULT.FAILURE;
        }
        private void noFParam(Writer writer, Matcher matcher) {
            hasErrors = true;
            result = RESULT.FAILURE;
        }

        private void percentComplete(Writer writer, Matcher matcher) {
            int progress = -1;
            try {
                progress = Integer.parseInt(matcher.group(1));
            } catch (Exception ignored) {}
            if (progress % 10 == 0) {
                System.out.printf("%d", progress);
                if (progress == 100) {
                    System.out.println();
                }
            } else {
                System.out.print('.');
            }
        }
    }


    protected static class LineHandler<CONSUMER> {
        public final Pattern linePattern;
        public final CONSUMER lineProcessor;

        public LineHandler(Pattern linePattern, CONSUMER lineProcessor) {
            this.linePattern = linePattern;
            this.lineProcessor = lineProcessor;
        }
    }
}
