package org.literacybridge.acm.tbloader;

import org.literacybridge.core.OSChecker;
import org.literacybridge.core.tbloader.FileSystemUtilities;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLineUtils extends FileSystemUtilities {
    private static final Logger LOG = Logger.getLogger(CommandLineUtils.class.getName());
    private final File windowsUtilsDirectory;

    private static final long CHKDSK_TIMEOUT = 50 * 1000;

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

    @Override
    public boolean disconnectDrive(String drive) throws IOException {
        String cmd = String.format("%s %s",new File(windowsUtilsDirectory, "RemoveDrive.exe") ,drive);
        String errorLine = execute(cmd);
        return errorLine == null;
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
    private static class DFU_DriverDetector implements BiFunction<Writer, LineReader, Boolean> {
        private static final Pattern INSTALLED = Pattern.compile("(?i).*DFU_installed.*REG_DWORD\\s*(0x\\d*).*");

        private static boolean hasDFU_Driver() {
            DFU_DriverDetector detector = new DFU_DriverDetector();
            String[] command = new String[]{"reg", "query", "HKLM\\Software\\Amplio-Network", "/reg:64"};
            return Boolean.TRUE.equals(runExternalCommand(command, detector));
        }

        boolean foundDfu = false;
        @Override
        public Boolean apply(Writer writer, LineReader stream) {
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

    private static class DiskFormatter implements BiFunction<Writer, LineReader, Boolean> {
        private static final Pattern INSERT_NEW_DISK = Pattern.compile("(?i)Insert new disk for drive.*");
        private static final Pattern ENTER_WHEN_READY = Pattern.compile("(?i).*ENTER when ready\\.{3}.*$");
        private static final Pattern FORMAT_COMPLETE = Pattern.compile("(?i)Format complete.*");
        private static final Pattern VOLUME_SERIAL = Pattern.compile("(?i)Volume Serial Number is (\\p{XDigit}{4}-\\p{XDigit}{4}).*");

        private static boolean format(String volume, String label) {
            DiskFormatter foh = new DiskFormatter();
            String[] command = new String[]{"cmd", "/c", "format", volume, "/Q", "/V:"+label};
            return Boolean.TRUE.equals(runExternalCommand(command, foh));
        }

        boolean formatComplete = false;

        @Override
        public Boolean apply(Writer writer, LineReader stream) {
            List<LineHandler<BiConsumer<Writer, Matcher>>> lineHandlers = new ArrayList<>();
            lineHandlers.add(new LineHandler<>(FORMAT_COMPLETE, this::gotFormatComplete));
            lineHandlers.add(new LineHandler<>(VOLUME_SERIAL, this::gotVolumeSerial));

            String line;
            try {
                if (waitForInsert(stream) &&
                    waitForReady(writer, stream))
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

        private boolean waitForInsert(LineReader stream) throws IOException {
            String line;
            while ((line = stream.readLine()) != null) {
                System.out.println(line);
                if (INSERT_NEW_DISK.matcher(line).matches())
                    return true;
            }
            return false;
        }

        private boolean waitForReady(Writer writer, LineReader stream) throws IOException {
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

    private static class FileSystemCheck implements BiFunction<Writer, LineReader, Boolean> {
        private static final Pattern NO_PROBLEMS = Pattern.compile("(?i).*(?:Windows has scanned the file system and found no problem|Windows a analysé le système de fichiers sans trouver de problème).*");
        private static final Pattern CONVERT_CHAINS = Pattern.compile("(?i).*(?:(?:files.*|chains.*){2}|Convertir les liens perdus en fichiers).*\\?");
        private static final Pattern FOUND_ERRORS = Pattern.compile("(?i).*(?:Windows found errors on the disk, but will not fix them|Windows a trouvé des erreurs sur le disque, mais ne les corrigera pas).*");
        private static final Pattern NO_F_PARAM = Pattern.compile("(?i).*(?:because disk checking was run without the /F|effectuée sans le paramètre /F \\(correction\\)).*");
        private static final Pattern PERCENT_COMPLETE = Pattern.compile("(?i)\\D*(\\d+) (?:percent complete|pour cent effectués).*");
        private static final Pattern FORCE_DISMOUNT = Pattern.compile("(?i).*(?:Would you like to force a dismount on this volume|Voulez-vous forcer le démontage de ce volume) *\\? \\((Y/N|O/N)\\) ");
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

        public LineReader lineReader;

        private boolean hasErrors = false;
        private boolean hasNoProblems = false;
        private Boolean result = null;

        private static RESULT checkDisk(String volume) throws FileNotFoundException {
            FileSystemCheck fsck = new FileSystemCheck(null);
            String[] command = new String[]{"chkdsk", volume};
            Boolean result = runExternalCommand(command, fsck, CHKDSK_TIMEOUT);
            return RESULT.result(result);
        }

        private static RESULT checkDiskAndFix(String volume, String logfileName) throws FileNotFoundException {
            FileSystemCheck fsck = new FileSystemCheck(logfileName);
            String[] command = new String[]{"chkdsk", "/f", volume};
            Boolean result = runExternalCommand(command, fsck, CHKDSK_TIMEOUT);
            return RESULT.result(result);
        }

        private FileSystemCheck(String logfileName) throws FileNotFoundException {
            PrintStream logfile = (logfileName != null) ? new PrintStream(logfileName) : System.out;
        }

        /**
         * Waits for chkdsk output.
         *
         * @param stream Output from the chkdsk command programmer.
         * @return true if the chkdsk completes; false if it fails to complete.
         */
        @Override
        public Boolean apply(Writer writer, LineReader stream) {
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
            if (hasNoProblems) {
                return true;
            }
            if (hasErrors) {
                return false;
            }
            return result;
        }

        private void noProblems(Writer writer, Matcher matcher) {
            hasNoProblems = true;
            result = true;
        }

        private void insufficientPrivileges(Writer writer, Matcher matcher) {
            hasErrors = true;
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

        private void reply(String reply, Writer writer, Matcher matcher) {
            try {
                writer.write(reply);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void foundErrors(Writer writer, Matcher matcher) {
            hasErrors = true;
            result = false;
        }
        private void noFParam(Writer writer, Matcher matcher) {
            hasErrors = true;
            result = false;
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


    /**
     * Runs an external command.
     *
     * @param cmdarray An array with the commands to run. Does not contain the cube executable itself.
     * @param handler  a callback to be invoked with a BufferedReader of the command output.
     * @param <R>      The generic type returned by the handler, also returned by this method.
     * @return Whatever the handler returned.
     */
    private static <R> R runExternalCommand(String[] cmdarray, Function<LineReader, R> handler) {
        return runExternalCommand(cmdarray, (w,r)->handler.apply(r));
    }
    private static <R> R runExternalCommand(String[] cmdarray, BiFunction<Writer, LineReader, R> handler) {
        return runExternalCommand(cmdarray, handler, -1);
    }
    private static <R> R runExternalCommand(String[] cmdarray, BiFunction<Writer, LineReader, R> handler, long timeout) {
        try {
            java.util.List<String> cmdList = Arrays.asList(cmdarray);
            LOG.log(Level.INFO, "Executing: " + String.join(" ", cmdarray));
            Process proc = new ProcessBuilder(cmdList)
                    .redirectErrorStream(true) // merge stderr into stdout
                    .directory(new File("c:\\WINDOWS\\System32"))
                    .start();

            Timer killTimer = null;
            if (timeout > 0) {
                killTimer = new Timer();
                killTimer.schedule(new TimerTask(){
                    @Override
                    public void run() {
                        proc.destroy();
                    }
                }, timeout);
            }

            // Stdout is called the "InputStream". Connect a reader to that.
            LineReader reader = new LineReader(new InputStreamReader(proc.getInputStream()));
            OutputStreamWriter writer = new OutputStreamWriter(proc.getOutputStream());
            // And let the handler have the output from the command.
            R result = handler.apply(writer, reader);
            // Drain the process output.
            reader.close();
            System.out.println("Closed process reader.");
            // Wait for the process to terminate.
            proc.waitFor();
            System.out.println("Process ended.");
            if (killTimer != null) {
                killTimer.cancel();
            }
            return result;
        } catch (InterruptedException | IOException e) {
            System.out.printf("Exception starting command: %s\n", e);
            return null;
        }
    }

    public static class LineHandler<CONSUMER> {
        final Pattern linePattern;
        final CONSUMER lineProcessor;

        public LineHandler(Pattern linePattern, CONSUMER lineProcessor) {
            this.linePattern = linePattern;
            this.lineProcessor = lineProcessor;
        }
    }

    private static class LineReader extends Reader {
        Reader in;
        boolean skipLf = false;
        boolean eof = false;
        public LineReader(Reader in) {
            super(in);
            this.in = in;
        }

        public String readLine(Pattern pattern) throws IOException {
            return readLine(Collections.singletonList(pattern));
        }
        public String readLine() throws IOException {
            return readLine((List<Pattern>)null);
        }
        public String readLine(List<Pattern> patterns) throws IOException {
            if (eof) return null;
            StringBuilder s=null;
            boolean eol = false;
            for (;;) {
                int ch = read();
//                System.out.print((char)ch);
                if (ch < 0) {
                    eof = true;
                    return s==null ? null : s.toString();
                }
                if (skipLf && ch == '\n') {
                    skipLf = false;
                    continue;
                }
                if (s == null) s = new StringBuilder();
                if (ch == '\n' || ch == '\r') {
                    skipLf = ch == '\r';
                    eol = true;
                } else {
                    s.append((char)ch);
                    if (patterns != null && patterns.size()>0) {
                        String partial = s.toString();
                        eol = patterns.stream().anyMatch(p->p.matcher(partial).matches());
                    }
                }
                if (eol) {
                    return s.toString();
                }
            }
        }

        /**
         * Reads up to 'len' characters from the input stream.
         * @param cbuf Buffer to receive characters.
         * @param off Offset to start receiving characters.
         * @param len Desired number of characters.
         * @return Actual number of characters read. Returns -1 if the stream returns EOF before characters are read.
         * @throws IOException if the stream throws an exception.
         */
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int n = 0;
            while (n < len) {
                int nr = in.read(cbuf, off + n, len - n);
                if (nr < 0) return n>0 ? n : nr;
                n += nr;
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }


}
