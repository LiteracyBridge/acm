package org.literacybridge.acm.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ExternalCommandRunner {
    private static final Logger LOG = Logger.getLogger(ExternalCommandRunner.class.getName());
    protected static File system32Directory = new File("c:\\WINDOWS\\System32");

    public enum LineProcessorResult {
        NOT_HANDLED, HANDLED, QUIT
    }

    // Make it easier to define the line processors.
    public interface LineProcessor extends BiFunction<java.io.Writer, java.util.regex.Matcher, LineProcessorResult> {}

    /**
     * Groups a line pattern, for recognizing a line, a handler, for handling recognized lines,
     * and a boolean indicating whether the pattern should be an early-recognition pattern.
     */
    public static class LineHandler {
        public final Pattern pattern;
        public final boolean isPartial;
        public final LineProcessor processor;

        @SuppressWarnings("unused")
        public LineHandler(java.util.regex.Pattern pattern, boolean isPartial, LineProcessor processor) {
            this.pattern = pattern;
            this.isPartial = isPartial;
            this.processor = processor;
        }
        public LineHandler(java.util.regex.Pattern pattern, LineProcessor processor) {
            this.pattern = pattern;
            this.isPartial = false;
            this.processor = processor;
        }
    }

    /**
     * A helper class for implementing external commands.
     *
     * Sub-classes should implement getLineHandlers() to return a list of the patterns and corresponding
     * handlers/processors for the line-by-line output of the external command, then call
     */
    @SuppressWarnings("JavadocBlankLines")
    public static abstract class CommandWrapper implements BiFunction<Writer, ExternalCommandRunner.LineReader, Boolean> {
        /**
         * Sub-classes should call this with the command to be run.
         */
        public boolean go() {
            return Boolean.TRUE.equals(run(getRunDirectory(), getCommand(), this));
        }

        abstract protected List<LineHandler> getLineHandlers();
        abstract protected String[] getCommand();
        protected File getRunDirectory() { return system32Directory; }

        @Override
        public Boolean apply(java.io.Writer writer, ExternalCommandRunner.LineReader stream) {
            List<LineHandler> lineHandlers = getLineHandlers();
            List<Pattern> partialLinePatterns = lineHandlers.stream()
                .filter(lh -> lh.isPartial)
                .map(lh -> lh.pattern)
                .collect(Collectors.toList());

            String line;
            try {
                outerloop:
                while ((line = stream.readLine(partialLinePatterns)) != null) {
                    System.out.println(line);
                    innerloop:
                    for (LineHandler lh : lineHandlers) {
                        Matcher matcher = lh.pattern.matcher(line);
                        if (matcher.matches()) {
                            switch (lh.processor.apply(writer, matcher)) {
                                case NOT_HANDLED:
                                    break;  // Try next processor.
                                case HANDLED:
                                    break innerloop; // Go on to next line.
                                case QUIT:
                                    break outerloop; // Done.
                            }
                            break;
                        }
                    }
                }
                System.out.println("Got EOF on program output.");
            } catch (IOException ignored) {
            }
            return true;
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
    private static <R> R run(String[] cmdarray, Function<LineReader, R> handler) {
        return run(cmdarray, (w, r) -> handler.apply(r));
    }
    private static <R> R run(File system32Directory, String[] cmdarray, Function<LineReader, R> handler) {
        return run(system32Directory, cmdarray, (w, r) -> handler.apply(r));
    }

    public static <R> R run(String[] cmdarray, BiFunction<Writer, LineReader, R> handler) {
        return run(cmdarray, handler, -1);
    }
    public static <R> R run(File system32Directory, String[] cmdarray, BiFunction<Writer, LineReader, R> handler) {
        return run(system32Directory, cmdarray, handler, -1);
    }

    public static <R> R run(String[] cmdarray, BiFunction<Writer, LineReader, R> handler, long timeout) {
        return run(system32Directory, cmdarray, handler, timeout);
    }

    public static <R> R run(File directory, String[] cmdarray, BiFunction<Writer, LineReader, R> handler, long timeout) {
        try {
            List<String> cmdList = Arrays.asList(cmdarray);
            LOG.log(Level.INFO, "Executing: " + String.join(" ", cmdarray));
            Process proc = new ProcessBuilder(cmdList)
                .redirectErrorStream(true) // merge stderr into stdout
                .directory(directory)
                .start();

            Timer killTimer = null;
            if (timeout > 0) {
                System.out.printf("Setting timeout for %d ms\n", timeout);
                killTimer = new Timer();
                killTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        System.out.println("Timer expired.");
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


    /**
     * A Reader that returns a line at a time. A partial line can be returned early if the partial line matches
     * a provided regular expression pattern.
     */
    public static class LineReader extends Reader {
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
            return readLine((List<Pattern>) null);
        }

        public String readLine(List<Pattern> patterns) throws IOException {
            if (eof) return null;
            StringBuilder s = null;
            boolean eol = false;
            for (; ; ) {
                int ch = read();
//                System.out.print((char)ch);
                if (ch < 0) {
                    eof = true;
                    return s == null ? null : s.toString();
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
                    s.append((char) ch);
                    if (patterns != null && patterns.size() > 0) {
                        String partial = s.toString();
                        eol = patterns.stream().anyMatch(p -> p.matcher(partial).matches());
                    }
                }
                if (eol) {
                    return s.toString();
                }
            }
        }

        /**
         * Reads up to 'len' characters from the input stream.
         *
         * @param cbuf Buffer to receive characters.
         * @param off  Offset to start receiving characters.
         * @param len  Desired number of characters.
         * @return Actual number of characters read. Returns -1 if the stream returns EOF before characters are read.
         * @throws IOException if the stream throws an exception.
         */
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int n = 0;
            while (n < len) {
                int nr = in.read(cbuf, off + n, len - n);
                if (nr < 0) return n > 0 ? n : nr;
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
