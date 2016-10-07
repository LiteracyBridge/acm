package org.literacybridge.acm.config;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class LockACM {
  private static File lockFile;
  private static FileChannel channel;
  private static FileLock lock;

  public static void lockDb(DBConfiguration config) {
    try {
      lockFile = new File(config.getTempACMsDirectory() + "/"
          + config.getSharedACMname() + ".lock");
      // Opens the file if it exists, otherwise creates it.
      channel = new RandomAccessFile(lockFile, "rw").getChannel();
      lock = channel.tryLock();
      if (lock == null) {
        // File is lock by other application
        channel.close();
        throw new RuntimeException("Two instances for the same ACM can't run at the same time.");
      }
      // Add shutdown hook to release lock when application shutdown
      ShutdownHook shutdownHook = new ShutdownHook();
      Runtime.getRuntime().addShutdownHook(shutdownHook);
    } catch (IOException e) {
      throw new RuntimeException("Could not start process.", e);
    }
  }

  public static void unlockDb() {
    // release and delete file lock
    try {
      if (lock != null) {
        lock.release();
        channel.close();
        lockFile.delete();
      }
    } catch (ClosedChannelException e) {
      if (lockFile.exists())
        e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static class ShutdownHook extends Thread {
    @Override
    public void run() {
      unlockDb();
    }
  }
}
