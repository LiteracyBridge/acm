package org.literacybridge.core.fs;

import java.util.HashMap;
import java.util.Map;

/**
 * Log operations of the applications. Uploaded to server, to extract app metrics, usage, and updates.
 */

public class OperationLog {

    public interface Implementation {
        void logEvent(String name, Map<String, String> info);
        void closeLogFile();
    }

    public interface Operation {
        <T> void put(String key, T value);
        void end(Map<String,String> info);
        void end();
    }

    private static Implementation implementation;
    public static void setImplementation(Implementation implementation) {
        OperationLog.implementation = implementation;
    }

    public synchronized static void close() {
        if (implementation != null) {
            implementation.closeLogFile();
        }
    }

    public synchronized static void logEvent(String name, Map<String, String> info) {
        if (implementation != null) {
            implementation.logEvent(name, info);
        }
    }

    public static Operation startOperation(String name) {
        return new OperationEvent(name);
    }

    private static class OperationEvent implements Operation {
        private Map<String, String> info = new HashMap<>();
        private long startTime;
        private String name;

        private OperationEvent(String name) {
            this.name = name;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public <T> void put(String key, T value) {
            info.put(key, value.toString());
        }

        @Override
        public void end(Map<String, String> info) {
            this.info.putAll(info);
            end();
        }

        @Override
        public void end() {
            info.put("time", Long.toString(System.currentTimeMillis()-startTime));
            OperationLog.logEvent(name, info);
        }
    }

}
