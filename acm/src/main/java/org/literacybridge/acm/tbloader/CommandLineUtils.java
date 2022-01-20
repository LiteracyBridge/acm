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
import java.util.stream.Collectors;

public class CommandLineUtils extends FileSystemUtilities {
    private static final Logger LOG = Logger.getLogger(CommandLineUtils.class.getName());
    private final File windowsUtilsDirectory;

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
    public boolean checkDisk(String drive) throws IOException {
        if (!OSChecker.WINDOWS) {
            throw new IllegalStateException("chkdsk operation is only supported on Windows");
        }
        System.out.printf("Checking drive %s\n", drive);
        boolean result = FileSystemCheck.checkDisk(drive);
        System.out.printf("Chkdsk completted: %s\n", result?"OK":"errors");
        return result;
    }

    @Override
    public boolean checkDiskAndFix(String drive, String logfilename) throws IOException {
        if (!OSChecker.WINDOWS) {
            throw new IllegalStateException("chkdsk operation is only supported on Windows");
        }
        System.out.printf("Checking drive %s (with /f).\n", drive);
        boolean result = FileSystemCheck.checkDiskAndFix(drive, logfilename);
        System.out.printf("Chkdsk completted: %s\n", result?"OK":"errors");
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
     * Class to determine if the STMicro DFU_Driver is installed on this computer.
     */
    private static class DFU_DriverDetector implements BiFunction<Writer, LineReader, Boolean> {
        private static final Pattern STM32BOOTLOADER = Pattern.compile("(?i).*stm32bootloader.inf.*");

        private static boolean hasDFU_Driver() {
            DFU_DriverDetector detector = new DFU_DriverDetector();
            String[] command = new String[]{"pnputil", "/enum-drivers"};
            return Boolean.TRUE.equals(runExternalCommand(command, detector));
        }

        @Override
        public Boolean apply(Writer writer, LineReader stream) {
            String line;
            boolean foundDfu = false;
            try {
                while ((line = stream.readLine()) != null) {
                    System.out.println(line);
                    Matcher matcher = STM32BOOTLOADER.matcher(line);
                    if (matcher.matches()) {
                        System.out.println(line);
                        foundDfu = true;
                    }
                }
                System.out.println("Got EOF on program output.");
            } catch (IOException ignored) {
            }
            return foundDfu;
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
        private static final Pattern VOLUME_CREATED = Pattern.compile("(?i)Volume ([\\p{Alnum}-]+) created (.*)$");
        private static final Pattern NO_PROBLEMS = Pattern.compile("(?i).*Windows has scanned the file system and found no problem.*");
        private static final Pattern NO_FURTHER_ACTION = Pattern.compile("(?i).*No further action is required.*");
        private static final Pattern WINDOWS_INSANITY = Pattern.compile("(?i).*ALL OPENED HANDLES TO THIS VOLUME WOULD THEN BE INVALID.*");
        private static final Pattern CONVERT_CHAINS = Pattern.compile("(?i).*(files.*|chains.*){2}\\?");
        private static final Pattern FOUND_ERRORS = Pattern.compile("(?i).*Windows found errors on the disk, but will not fix them.*");
        private static final Pattern NO_F_PARAM = Pattern.compile("(?i).*because disk checking was run without the /F.*");
        private static final Pattern PERCENT_COMPLETE = Pattern.compile("(?i)\\D*(\\d+) percent complete.*");
        private static final Pattern FORCE_DISMOUNT = Pattern.compile("(?i).*Would you like to force a dismount on this volume\\? \\(Y/N\\) ");

        private final List<LineHandler<BiConsumer<Writer, Matcher>>> handlers = Arrays.asList(
            new LineHandler<BiConsumer<Writer, Matcher>>(VOLUME_CREATED, this::gotVolume),
            new LineHandler<BiConsumer<Writer, Matcher>>(NO_FURTHER_ACTION, this::gotNoFurtherAction),
            new LineHandler<BiConsumer<Writer, Matcher>>(WINDOWS_INSANITY, this::windowsInsanity),
            new LineHandler<BiConsumer<Writer, Matcher>>(CONVERT_CHAINS, this::gotConvertChains),
            new LineHandler<BiConsumer<Writer, Matcher>>(FORCE_DISMOUNT, this::gotForceDismount),
            new LineHandler<BiConsumer<Writer, Matcher>>(FOUND_ERRORS, this::foundErrors),
            new LineHandler<BiConsumer<Writer, Matcher>>(NO_F_PARAM, this::noFParam),
            new LineHandler<BiConsumer<Writer, Matcher>>(NO_PROBLEMS, this::noProblems),
            new LineHandler<BiConsumer<Writer, Matcher>>(PERCENT_COMPLETE, this::percentComplete)
        );
        // Patterns of output from chkdsk that require a response.
        private final List<Pattern> patterns = Arrays.asList(FORCE_DISMOUNT, CONVERT_CHAINS);

        public LineReader lineReader;

        private String programOutputLine;
        private boolean noFurtherAction = false;
        private boolean hasErrors = false;
        private boolean hasNoProblems = false;

        private static boolean checkDisk(String volume) throws FileNotFoundException {
            FileSystemCheck fsck = new FileSystemCheck(null);
            String[] command = new String[]{"chkdsk", volume};
            return Boolean.TRUE.equals(runExternalCommand(command, fsck));
        }

        private static boolean checkDiskAndFix(String volume, String logfileName) throws FileNotFoundException {
            FileSystemCheck fsck = new FileSystemCheck(logfileName);
            String[] command = new String[]{"chkdsk", "/f", volume};
            return Boolean.TRUE.equals(runExternalCommand(command, fsck));
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
                while ((programOutputLine = stream.readLine(patterns)) != null) {
                    for (LineHandler<BiConsumer<Writer, Matcher>> lh : handlers) {
                        Matcher matcher = lh.linePattern.matcher(programOutputLine);
                        if (matcher.matches()) {
                            lh.lineProcessor.accept(writer, matcher);
                            break;
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
            // What should we do here? The results are inconclusive.
            return noFurtherAction;
        }

        private void noProblems(Writer writer, Matcher matcher) {
            System.out.println(programOutputLine);
            hasNoProblems = true;
        }

        private void gotConvertChains(Writer writer, Matcher matcher) {
            System.out.println(programOutputLine);
            reply("n\r\n", writer, matcher);
        }

        private void gotForceDismount(Writer writer, Matcher matcher) {
            System.out.println(programOutputLine);
            reply("y\r\n", writer, matcher);
        }

        private void reply(String reply, Writer writer, Matcher matcher) {
            try {
                writer.write(reply);
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void windowsInsanity(Writer writer, Matcher matcher) {
            System.out.println(programOutputLine);
            boolean windowsInsanity = true;
        }

        private void gotVolume(Writer writer, Matcher matcher) {
            System.out.println(programOutputLine);
            String volumeLabel = matcher.group(1);
//        createDate = matcher.group(2);
        }

        private void gotNoFurtherAction(Writer writer, Matcher matcher) {
            System.out.println(programOutputLine);
            noFurtherAction = true;
        }

        private void foundErrors(Writer writer, Matcher matcher) {
            System.out.println(programOutputLine);
            hasErrors = true;
        }
        private void noFParam(Writer writer, Matcher matcher) {
            System.out.println(programOutputLine);
            hasErrors = true;
        }

        private void percentComplete(Writer writer, Matcher matcher) {
            System.out.print('.');
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
        try {
            java.util.List<String> cmdList = Arrays.asList(cmdarray);
            LOG.log(Level.INFO, "Executing: " + String.join(" ", cmdarray));
            Process proc = new ProcessBuilder(cmdList)
                    .redirectErrorStream(true) // merge stderr into stdout
                    .directory(new File("c:\\WINDOWS\\System32"))
                    .start();

            // Stdout is called the "InputStream". Connect a reader to that.
            LineReader reader = new LineReader(new InputStreamReader(proc.getInputStream()));
            OutputStreamWriter writer = new OutputStreamWriter(proc.getOutputStream());
            // And let the handler have the output from the command.
            R result = handler.apply(writer, reader);
            // Drain the process output.
            //noinspection StatementWithEmptyBody
            while (reader.readLine() != null);
            // Wait for the process to terminate.
            proc.waitFor();
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
            StringBuilder s=null;
            boolean eol = false;
            for (;;) {
                int ch = read();
//                System.out.print((char)ch);
                if (ch < 0) {
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
