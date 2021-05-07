package org.literacybridge.acm.sandbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class Sandbox {
    private static final Logger LOG = Logger.getLogger(Sandbox.class.getName());

    private final File baseDir;
    private final File shadowDir;

    private final Map<Path, FileOp> workQueue = new LinkedHashMap<>();

    public Sandbox(File baseDir, File shadowDir) {
        this.baseDir = baseDir;
        this.shadowDir = shadowDir;
    }

    public void commit() {
        commit(x->{}, x->{});
    }

    /**
     * Commits the work queue items from sandbox to base.
     * @param writtenFileHandler callback for each file added or updated.
     * @param removedFileHandler callback for each file removed.
     */
    public void commit(Consumer<File> writtenFileHandler, Consumer<File> removedFileHandler) {
//        System.out.printf("\nCommitting %d items:\n", workQueue.size());
        for (Map.Entry<Path, FileOp> e : workQueue.entrySet()) {
            if (e.getValue() instanceof DeleteOp) {
                // The file was deleted in the sandbox; delete in the base.
                File f = baseDir.toPath().resolve(e.getKey()).toFile();
                boolean ok = f.delete();
                removedFileHandler.accept(f);
//                System.out.printf("delete: %s - [%s]\n", e.getKey(), ok?"ok":"error");
                
            } else if (e.getValue() instanceof AddOp) {
                // The file was added or updated in the sandbox; make the same change in the base.
                File moveTo = baseDir.toPath().resolve(e.getKey()).toFile();
                File moveFrom = shadowDir.toPath().resolve(e.getKey()).toFile();
                boolean ok = moveFrom.renameTo(moveTo);
                writtenFileHandler.accept(moveTo);
//                System.out.printf("   add: %s + [%s]\n", e.getKey(), ok?"ok":"error");
                
            } else if (e.getValue() instanceof MoveOp) {
                // The file was renamed (or moved) in the sandbox; do the same in the base, OR the file didn't 
                // exist in the sandbox proper, but was requested to be renamed in the base; do that now.
                File moveTo = baseDir.toPath().resolve(e.getKey()).toFile();
                Path moveFromPath = ((MoveOp)e.getValue()).fromPath;
                File moveFrom = baseDir.toPath().resolve(moveFromPath).toFile();
                boolean ok = moveFrom.renameTo(moveTo);
                writtenFileHandler.accept(moveTo);
//                System.out.printf("  move: %s + from %s [%s]\n", e.getKey(), moveFromPath, ok?"ok":"error");
                
            } else if (e.getValue() instanceof MovedOutOp) {
                // This file had no corresponding sandbox copy, and was requested for rename. The new name had
                // a MoveOp. This file has been renamed elsewhere, so notify the handler that it no longer exists
                // as its original name.
                File movedFromFile = baseDir.toPath().resolve(e.getKey()).toFile();
                boolean ok = !movedFromFile.exists();
                removedFileHandler.accept(movedFromFile);
//                System.out.printf(" moved: %s - [%s]\n", e.getKey(), ok?"ok":"error");
                
            }
        }
        workQueue.clear();
//        System.out.println("Done.\n");
    }

    public boolean exists(File file) {
        return exists(file.toPath());
    }
    public boolean exists(Path path) {
        ensureValidPath(path);
        Path relativePath = path.isAbsolute() ? baseDir.toPath().relativize(path) : path;
        // If there is a shadow file, the file will continue to exist.
        if (shadowDir.toPath().resolve(relativePath).toFile().exists()) return true;
        // Check for any operation that affects this path...
        FileOp op = workQueue.get(relativePath);
        // Maybe something will be renamed to this path.
        if (op instanceof MoveOp) return true;
        // No shadow file; if it has been deleted, it won't exist.
        if (op instanceof DeleteOp) return false;
        // No shadow file, not explicitly deleted. Does the base file exist?
        return (baseDir.toPath().resolve(relativePath).toFile()).exists();
    }

    public void delete(File file) {
        delete(file.toPath());
    }
    /**
     * Marks a file for deletion. Actual deletion doesn't happen until commit() is called, but in the meantime
     * fileExists(path) will return false.
     * @param path Path, relative to persistantDir, to be deleted.
     */
    public void delete(Path path) {
        ensureValidPath(path);
        Path relativePath = path.isAbsolute() ? baseDir.toPath().relativize(path) : path;
        File shadowFile = shadowDir.toPath().resolve(relativePath).toFile();
        if (shadowFile.exists()) shadowFile.delete();

        // Check for any operation that affects this path...
        workQueue.remove(relativePath);
        workQueue.put(relativePath, new DeleteOp());
    }

    /**
     * Gets a FileInputStream from the given Path or File.
     * @param file or path to be opened.
     * @return a FileInputStream
     * @throws FileNotFoundException if there is no backing file.
     */
    public FileInputStream fileInputStream(File file) throws FileNotFoundException { return fileInputStream(file.toPath()); }
    public FileInputStream fileInputStream(Path path) throws FileNotFoundException {
        ensureValidPath(path);
        Path relativePath = path.isAbsolute() ? baseDir.toPath().relativize(path) : path;
        // If there's a shadow file, return it.
        File file = shadowDir.toPath().resolve(relativePath).toFile();
        if (file.exists()) return new FileInputStream(file);

        // Check for any operation that affects this path...
        FileOp op = workQueue.get(relativePath);
        // Maybe something is scheduled to be renamed to this name?
        if (op instanceof MoveOp) {
            MoveOp mop = (MoveOp)op;
            File fromFile= baseDir.toPath().resolve(mop.fromPath).toFile();
            return new FileInputStream(fromFile);        }
        if (op instanceof DeleteOp) {
            throw new FileNotFoundException("File does not exist: " + relativePath);
        }

        // Not explicitly deleted or moved, so defer to base file.
        file = baseDir.toPath().resolve(relativePath).toFile();
        return new FileInputStream(file);
    }

    /**
     * Gets a FileOutputStream from the given File or Path.
     * @param newFile or path: the File or Path to be opened.
     * @param append: if true, open the file for appending, otherwise truncate any existing file.
     * @return the FileOutputStream
     * @throws FileNotFoundException if the file can't be created.
     */
    @SuppressWarnings("JavadocReference")
    public FileOutputStream fileOutputStream(File newFile) throws FileNotFoundException { return fileOutputStream(newFile, false); }
    public FileOutputStream fileOutputStream(File newFile, boolean append) throws FileNotFoundException { return fileOutputStream(newFile.toPath(), append); }
    public FileOutputStream fileOutputStream(Path path) throws FileNotFoundException { return fileOutputStream(path, false); }
    public FileOutputStream fileOutputStream(Path path, boolean append) throws FileNotFoundException {
        ensureValidPath(path);
        Path relativePath = path.isAbsolute() ? baseDir.toPath().relativize(path) : path;
        Path shadowedPath = shadowDir.toPath().resolve(relativePath);

        // **** State tracking ****
        // Check for any operation that affects this path...
        workQueue.remove(relativePath);
        // Remember that we added it.
        workQueue.put(relativePath, new AddOp());

        return new FileOutputStream(shadowedPath.toFile(), append);
    }

    /**
     * Rename (move) a file. If the source file is outside of the sandbox, this is equivalent to a new file.
     * @param from File or Path to be renamed/moved from.
     * @param to File or Path to be renamed/moved to.
     * @throws FileNotFoundException if the from file can't be found.
     */
    public boolean rename(File from, File to) throws FileNotFoundException {
        return rename(from.toPath(), to.toPath());
    }
    public boolean rename(Path from, Path to) throws FileNotFoundException {
        boolean result = false;
        ensureValidPath(to);
        Path relativeToPath = to.isAbsolute() ? baseDir.toPath().relativize(to) : to;
        File shadowedToFile = shadowDir.toPath().resolve(relativeToPath).toFile();
        if (isSandboxedPath(from)) {
            // Moving a file within the sandboxed files.
            Path relativeFromPath = from.isAbsolute() ? baseDir.toPath().relativize(from) : from;
            File shadowedFromFile = shadowDir.toPath().resolve(relativeFromPath).toFile();
            File baseFromFile = baseDir.toPath().resolve(relativeFromPath).toFile();
            if (shadowedFromFile.exists()) {
                // Move the existing shadowed file.
                if (shadowedFromFile.renameTo(shadowedToFile)) {
                    result = true;
                    workQueue.remove(relativeToPath);
                    workQueue.put(relativeToPath, new AddOp());
                    workQueue.remove(relativeFromPath);
                    if (baseFromFile.exists()) {
                        workQueue.put(relativeFromPath, new DeleteOp());
                    }
                }
            } else if (baseFromFile.exists()) {
                result = true; // we're assuming the rename will work in the future.
                workQueue.remove(relativeToPath);
                workQueue.put(relativeToPath, new MoveOp(relativeFromPath));
                workQueue.remove(relativeFromPath);
                // If we don't put another file here, we'll need to let the handlers know that this file no
                // longer exists.
                workQueue.put(relativeFromPath, new MovedOutOp());
            }  
        } else {
            // Moving an external file into the sandboxed files.
            if (!from.isAbsolute()) {
                throw new AbsolutePathRequired(from);
            }
            if (from.toFile().renameTo(shadowedToFile)) {
                result = true;
                workQueue.remove(relativeToPath);
                workQueue.put(relativeToPath, new AddOp());
            }
        }
        return result;
    }
    
    
    public boolean isSandboxedFile(File file) {
        return isSandboxedPath(file.toPath());
    }
    public boolean isSandboxedPath(Path path) {
        return path.isAbsolute() ? path.startsWith(baseDir.getAbsolutePath()) : !path.startsWith("..");
    }

    void ensureValidPath(Path path) {
        if (!isSandboxedPath(path)) {
            throw new NotASandboxedDirectory(path);
        }
    }

    /**
     * Gets a copy of the workqueue. Currently used in testing.
     * @return a copy of the work queue.
     */
    Map<Path, FileOp> getWorkQueue() {
        return new LinkedHashMap<>(workQueue);
    }

    // File operation & sequencing support
    abstract static class FileOp { }
    static class DeleteOp extends FileOp { }
    static class MovedOutOp extends FileOp { }
    static class AddOp extends FileOp { }
    static class MoveOp extends FileOp {
        final Path fromPath;
        public MoveOp(Path fromPath) {
            this.fromPath = fromPath;
        }
    }
    
    /**
     * Exception thrown when a Path should be relative or contained within persistentDir, but isn't.
     */
    public static class NotASandboxedDirectory extends IllegalArgumentException {
        public NotASandboxedDirectory(Path path) {
            super(path.toString());
        }
    }
    public static class AbsolutePathRequired extends IllegalArgumentException {
        public AbsolutePathRequired(Path path) {
            super(path.toString());
        }
    }
}
