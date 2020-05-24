package org.literacybridge.core.fs;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import static org.literacybridge.core.tbloader.TBLoaderConstants.UTC;

/**
 * Log operations of the applications. Uploaded to server, to extract app metrics, usage, and updates.
 */

public class OperationLog {
    private static final Pattern NEWLINE = Pattern.compile("\n");
    private static final Pattern COMMA = Pattern.compile(",");
    private static final DateFormat logFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
    static {
        logFormat.setTimeZone(UTC);
    }

    /**
     * OperationLog's required implementation. Applications implement this, then call 'setImplementation()'
     */
    public interface Implementation {
        void logEvent(Operation operation);
        void closeLogFile();
    }

    /**
     * This is mostly for convenience of adding any object with a toString() to the map of
     * values.
     */
    public static class Info {
        private Map<String, String> info = new LinkedHashMap<>();
        public <T> Info put(String key, T value) {
            info.put(key, value.toString());
            return this;
        }
    }

    /**
     * For an operation with a time (ie, not simply a point in time), an application can get one of
     * these when the operation starts, put properties as they become available, and end it when it finishes.
     */
    public static abstract class Operation {
        /**
         * Put a key:value pair. Anything with a toString() for the value
         * @param key The key; any name that's meaningful to the application. Note that another
         *            put to the same name replaces the earlier value.
         * @param value Anything with a toString() as the value.
         * @return this so that calls can be chained.
         */
        public abstract <T> Operation put(String key, T value);

        /**
         * Put key:value pairs.
         * @param info object with key:value pairs.
         * @return this, for chaining.
         */
        public abstract Operation put(Info info);

        /**
         * Marks the time, by recording the value of the elapsed milliseconds as key. Note: only
         * applies to timed events; ignored otherwise.
         * @param key The name of the timer.
         * @return The Operation, so this can be chained with put()
         */
        public abstract Operation split(String key);

        /**
         * End the operation, and provide more info.
         * @param info A Map<String, String> of additional key:value pairs. Any keys here will overwrite
         *             any keys set through 'put()'.
         */
        public abstract void finish(Map<String,String> info);

        /**
         * Ends any timer, and saves the event.
         */
        public abstract void finish();

        /**
         * Implementations specific options for the Operation. May or may not affect how the log is
         * handled.
         * @param key Name of the option
         * @param value Value of the option
         * @return The Operation, so this can be chained.
         */
        public abstract <T> Operation option(String key, T value);

        public abstract Map<String, String> getInfo();
        public abstract String getName();
        public abstract boolean hasOption(String optionName);
        public abstract String getOption(String optionName);

        public abstract String formatLog();
    }

    /**
     * Where applications set the implementation.
     */
    private static Implementation implementation;
    public static void setImplementation(Implementation implementation) {
        OperationLog.implementation = implementation;
    }

    /**
     * And application can call this and if there is an implementation, we'll forward the call.
     */
    public synchronized static void close() {
        if (implementation != null) {
            implementation.closeLogFile();
        }
    }

    /**
     * And application can call this and if there is an implementation, we'll forward the call.
     */
    public synchronized static void logEvent(Operation operation) {
        if (implementation != null) {
            implementation.logEvent(operation);
        }
    }
    public synchronized static void logEvent(String name, Map<String, String> info) {
        OperationEvent opEvent = new OperationEvent(name);
        opEvent.finish(info);
    }

    public static Operation log(String name) {
        return new OperationEvent(name);
    }

    /**
     * Applications call this to start a timed operation.
     * @param name String that's meaningful to the application.
     * @return An Object implementing Operation.
     */
    public static Operation startOperation(String name) {
        return new TimedOperationEvent(name);
    }

    /**
     * Implementation of Operation.
     */
    private static class OperationEvent extends Operation {
        private Map<String, String> options = new LinkedHashMap<>();
        private Map<String, String> info = new LinkedHashMap<>();
        private String name;

        private OperationEvent(String name) {
            this.name = name;
            assert name != null && name.length() > 0 : "Must provide a name for log entry.";
        }

        @Override
        public <T> Operation put(String key, T value) {
            info.put(key, value.toString());
            return this;
        }

        public Operation put(Info info) {
            this.info.putAll(info.info);
            return this;
        }

        /**
         * Marks the time, by recording the value of the elapsed milliseconds as key.
         *
         * @param key The name of the timer.
         * @return The Operation, so this can be chained with put()
         */
        @Override
        public Operation split(String key) {
            return this;
        }

        @Override
        public void finish(Map<String, String> info) {
            if (info != null && info.size() > 0)
                this.info.putAll(info);
            finish();
        }

        @Override
        public void finish() {
            // Only record once.
            if (name != null) {
                OperationLog.logEvent(this);
                name = null;
            }
        }

        /**
         * Implementations specific options for the Operation. May or may not affect how the log is
         * handled.
         *
         * @param key   Name of the option
         * @param value Value of the option
         * @return The Operation, so this can be chained.
         */
        @Override
        public <T> Operation option(String key, T value) {
            options.put(key, value.toString());
            return this;
        }

        @Override
        public Map<String, String> getInfo() {
            return info;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean hasOption(String optionName) {
            return options.containsKey(optionName);
        }

        @Override
        public String getOption(String optionName) {
            return options.get(optionName);
        }

        /**
         * Replaces newlines with "↵" and commas with semicolons.
         * @param rawString String that may have problematic characters.
         * @return String with those characters removed.
         */
        private String enquote(String rawString) {
            rawString = NEWLINE.matcher(rawString).replaceAll("↵");
            rawString = COMMA.matcher(rawString).replaceAll(";");
            return rawString;
        }

        @Override
        public String formatLog() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("%s,%s", logFormat.format(new Date()), getName()));
            Map<String, String> info = getInfo();
            // Convert Map<String,String> to string of key:value,key:value.
            // Note that \n is munged to ↵ and , to ;
            for (Map.Entry<String, String> entry : info.entrySet()) {
                builder.append(',').append(entry.getKey()).append(':');
                builder.append(enquote(entry.getValue()));
            }
            builder.append("\n");
            return builder.toString();
        }
    }

    private static class TimedOperationEvent extends OperationEvent {
        private static final String ELAPSED = "elapsedTime";
        private long startTime;
        private long splitStart;

        private TimedOperationEvent(String name) {
            super(name);
            this.startTime = System.currentTimeMillis();
            this.splitStart = startTime;
            this.put(ELAPSED, 0);
        }

        @Override
        public Operation split(String name) {
            long t = System.currentTimeMillis();
            this.put(name, Long.toString(t-splitStart));
            splitStart = t; // for next split
            return this;
        }
        @Override
        public void finish() {
            // Only record once.
            if (startTime > 0) {
                this.put(ELAPSED, Long.toString(System.currentTimeMillis() - startTime));
                startTime = -1;
                super.finish();
            }
        }
    }

}
