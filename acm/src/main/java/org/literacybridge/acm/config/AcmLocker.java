package org.literacybridge.acm.config;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;

public class AcmLocker {
    private static final Map<String, AcmLocker> locks = new HashMap<>();

    private File lockFile;
    private FileChannel channel;
    private FileLock lock;

    public static class MultipleInstanceException extends RuntimeException {
        public MultipleInstanceException(String msg) {
            super(msg);
        }
    }

    public synchronized static void lockDb(DBConfiguration config) {
        AcmLocker acmLocker = new AcmLocker();
        acmLocker.lock(config);
        locks.put(config.getProgramName(), acmLocker);
    }

    public synchronized static void unlockDb() {
        locks.values().forEach(AcmLocker::unlock);
        locks.clear();
    }

    public synchronized static void unlockDb(String name) {
        if (locks.containsKey(name)) {
            locks.get(name).unlock();
            locks.remove(name);
        }
    }

    public void lock(DBConfiguration config) {
        lockFile = config.getPathProvider().getLocalLockFile();
        lock(lockFile);
    }

    public void lock(File lockFile) {
        try {
            // Opens the file if it exists, otherwise creates it.
            channel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = channel.tryLock();
            if (lock == null) {
                // File is lock by other application
                channel.close();
                throw new MultipleInstanceException("Two instances for the same ACM can't run at the same time.");
            }
            // Add shutdown hook to release lock when application shutdown
            ShutdownHook shutdownHook = new ShutdownHook();
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        } catch (IOException e) {
            throw new RuntimeException("Could not start process.", e);
        }
    }

    public void unlock() {
        // release and delete file lock
        try {
            if (lock != null) {
                lock.release();
                channel.close();
                lockFile.delete();
            }
        } catch (ClosedChannelException e) {
            if (lockFile.exists()) { e.printStackTrace(); }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isLocked() {
        return locks.size() > 0;
    }

    public static boolean isLocked(String name) {
        return locks.containsKey(name);
    }

    static class ShutdownHook extends Thread {
        @Override
        public void run() {
            unlockDb();
        }
    }
}
