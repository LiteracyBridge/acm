package org.literacybridge.acm.sandbox;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class Sandbox {
    private static final Logger LOG = Logger.getLogger(Sandbox.class.getName());
    private static final String DATA_DIR = "data";
    private static final String WORK_QUEUE_DATA = "workqueue.data";

    private final File baseDir;
    // Directory with control files and data
    private final File shadowDir;
    // The actual shadow data
    private final File shadowData;

    private final Map<Path, FileOp> workQueue = new LinkedHashMap<>();

    public Sandbox(File baseDir, File shadowDir) {
        this.baseDir = baseDir;
        this.shadowDir = shadowDir;
        this.shadowData = new File(shadowDir, DATA_DIR);
        System.out.printf("Sandbox created for %s, with sandbox in %s\n", baseDir, shadowDir);
        restoreWorkQueue();
    }

    private void persistWorkQueue() {
        File qFile = new File(shadowDir, WORK_QUEUE_DATA);
        if (!qFile.getParentFile().exists()) {
            qFile.getParentFile().mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(qFile);
             ObjectOutputStream os = new ObjectOutputStream(fos))
        {
            os.writeInt(workQueue.size());
            for (Map.Entry<Path,FileOp> e : workQueue.entrySet()) {
                os.writeChar(e.getValue().getId());
                os.writeObject(e.getKey().toString());
                if (e.getValue() instanceof MoveOp) {
                    os.writeObject(((MoveOp)e.getValue()).fromPath.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreWorkQueue() {
        File qFile = new File(shadowDir, WORK_QUEUE_DATA);
        if (qFile.exists()) {
            try (FileInputStream fis = new FileInputStream(qFile);
                 ObjectInputStream is = new ObjectInputStream(fis)) {
                int size = is.readInt();
                System.out.printf("Found work queue with %d items.\n", size);
                for (int i=0; i<size; i++) {
                    char ch = is.readChar();
                    String strKey = (String)is.readObject();
                    Path pKey = baseDir.toPath().relativize(baseDir.toPath().resolve(strKey));
                    switch (ch) {
                        case 'D': workQueue.put(pKey, new DeleteOp()); break;
                        case 'A': workQueue.put(pKey, new AddOp()); break;
                        case 'O': workQueue.put(pKey, new MovedOutOp()); break;
                        case 'K': workQueue.put(pKey, new MkDirOp()); break;
                        case 'L': workQueue.put(pKey, new RmDirOp()); break;
                        case 'M': String strValue = (String)is.readObject();
                            Path pValue = baseDir.toPath().relativize(baseDir.toPath().resolve(strValue));
                            workQueue.put(pKey, new MoveOp(pValue));
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Determine whether the sandbox contains changes that need to be applied to the base directory.
     *
     * @return true if there are changes to be applied, false otherwise.
     */
    public boolean hasChanges() {
        return workQueue.size() > 0;
    }

    public void discard() {
        workQueue.clear();
        FileUtils.deleteQuietly(this.shadowDir);
    }

    public void commit() {
        commit(x -> {}, x -> {});
    }

    /**
     * Commits the work queue items from sandbox to base.
     *
     * @param writtenFileHandler callback for each file added or updated.
     * @param removedFileHandler callback for each file removed.
     */
    public void commit(Consumer<File> writtenFileHandler, Consumer<File> removedFileHandler) {
        System.out.printf("\nCommitting %d items:\n", workQueue.size());
        for (Map.Entry<Path, FileOp> e : workQueue.entrySet()) {
            if (e.getValue() instanceof DeleteOp) {
                // The file was deleted in the sandbox; delete in the base.
                File f = baseDir.toPath().resolve(e.getKey()).toFile();
                boolean ok = f.delete();
                removedFileHandler.accept(f);
                System.out.printf("delete: %s - [%s]\n", e.getKey(), ok?"ok":"error");

            } else if (e.getValue() instanceof AddOp) {
                // The file was added or updated in the sandbox; make the same change in the base.
                File moveTo = baseDir.toPath().resolve(e.getKey()).toFile();
                File moveFrom = shadowData.toPath().resolve(e.getKey()).toFile();
                boolean ok = moveFrom.renameTo(moveTo);
                writtenFileHandler.accept(moveTo);
                System.out.printf("   add: %s + from: %s [%s]\n", moveTo.getAbsolutePath(), moveFrom.getAbsolutePath(), ok?"ok":"error");

            } else if (e.getValue() instanceof MoveOp) {
                // The file was renamed (or moved) in the sandbox; do the same in the base, OR the file didn't
                // exist in the sandbox proper, but was requested to be renamed in the base; do that now.
                File moveTo = baseDir.toPath().resolve(e.getKey()).toFile();
                Path moveFromPath = ((MoveOp) e.getValue()).fromPath;
                File moveFrom = baseDir.toPath().resolve(moveFromPath).toFile();
                boolean ok = moveFrom.renameTo(moveTo);
                writtenFileHandler.accept(moveTo);
                System.out.printf("  move: %s + from %s [%s]\n", moveTo.getAbsolutePath(), moveFrom.getAbsolutePath(), ok?"ok":"error");

            } else if (e.getValue() instanceof MovedOutOp) {
                // This file had no corresponding sandbox copy, and was requested for rename. The new name had
                // a MoveOp. This file has been renamed elsewhere, so notify the handler that it no longer exists
                // as its original name.
                File movedFromFile = baseDir.toPath().resolve(e.getKey()).toFile();
                boolean ok = !movedFromFile.exists();
                removedFileHandler.accept(movedFromFile);
                System.out.printf(" moved: %s - [%s]\n", e.getKey(), ok?"ok":"error");

            } else if (e.getValue() instanceof MkDirOp) {
                File newDir = baseDir.toPath().resolve(e.getKey()).toFile();
                boolean ok = newDir.exists() || newDir.mkdirs();
                System.out.printf(" mkdir: %s [%s]\n", newDir.getAbsolutePath(), ok?"ok":"error");

            } else if (e.getValue() instanceof RmDirOp ) {
                File oldDir = baseDir.toPath().resolve(e.getKey()).toFile();
                boolean ok = !oldDir.exists() || oldDir.delete();
                System.out.printf(" rmdir: %s [%s]\n", oldDir.getAbsolutePath(), ok?"ok":"error");

            }
        }
        discard();
    }

    public boolean exists(File file) {
        return exists(file.toPath());
    }

    public boolean exists(Path path) {
        ensureValidPath(path);
        Path relativePath = path.isAbsolute() ? baseDir.toPath().relativize(path) : path;
        // If there is a shadow file, the file will continue to exist.
        if (shadowData.toPath().resolve(relativePath).toFile().exists()) return true;
        // Check for any operation that affects this path...
        FileOp op = workQueue.get(relativePath);
        // Maybe something will be renamed to this path.
        if (op instanceof MoveOp) return true;
        // No shadow file; if it has been deleted, it won't exist.
        if (op instanceof DeleteOp || op instanceof MovedOutOp) return false;
        // No shadow file, not explicitly deleted. Does the base file exist?
        return (baseDir.toPath().resolve(relativePath).toFile()).exists();
    }

    public boolean isDirectory(File file) {
        return isDirectory(file.toPath());
    }

    public boolean isDirectory(Path path) {
        File sandboxedFile = inputFile(path);
        return sandboxedFile.exists() && sandboxedFile.isDirectory();
    }

    public enum Options {recursive, quietly}

    public void delete(File file, Options... options) {
        delete(file.toPath(), options);
    }

    /**
     * Marks a file for deletion. Actual deletion doesn't happen until commit() is called, but in the meantime
     * fileExists(path) will return false.
     *
     * @param path Path, relative to persistantDir, to be deleted.
     */
    public void delete(Path path, Options... options) {
        ensureValidPath(path);
        Set<Options> opts = new HashSet<>(Arrays.asList(options));
        Path relativePath = path.isAbsolute() ? baseDir.toPath().relativize(path) : path;
        File shadowFile = shadowData.toPath().resolve(relativePath).toFile();
        if (shadowFile.exists()) shadowFile.delete();

        // Check for any operation that affects this path...
        workQueue.remove(relativePath);
        workQueue.put(relativePath, new DeleteOp());
        persistWorkQueue();
    }

    /**
     * Lists the paths under the given path. If the path is not a directory, an empty
     * collection is returned.
     *
     * @param path for which the direct children is desired.
     * @return a collection of the paths of the files in the directory, relative to
     * the sandbox.
     */
    public Collection<Path> listPaths(Path path) {
        Set<Path> result = new HashSet<>();
        ensureValidPath(path);
        Path relativePath = path.isAbsolute() ? baseDir.toPath().relativize(path) : path;
        File shadowFile = shadowData.toPath().resolve(relativePath).toFile();
        File baseFile = baseDir.toPath().resolve(relativePath).toFile();
        FileOp op = workQueue.get(relativePath);
        File[] files = null;
        // Is the base itself scheduled for deletion or renaming?
        if (op instanceof MoveOp) {
            Path otherPath = ((MoveOp) op).fromPath;
            File otherFile = baseDir.toPath().resolve(otherPath).toFile();
            files = otherFile.listFiles();
        } else if (!(op instanceof DeleteOp || op instanceof MovedOutOp)) {
            files = baseFile.listFiles();
        }
        // Files in the base that aren't scheduled for deletion.
        if (files != null) {
            for (File file : files) {
                Path relativeSub = baseDir.toPath().relativize(file.toPath());
                // If it isn't scheduled for deletion add it to the list.
                op = workQueue.get(relativeSub);
                if (!(op instanceof DeleteOp || op instanceof MovedOutOp)) {
                    result.add(relativeSub);
                }
            }
        }
        // Is anything being moved in? (Adds will have physical files, so no need to look for them.)
        workQueue.entrySet().stream()
            .filter(e->e.getValue() instanceof MoveOp)
            .map(Map.Entry::getKey)
            .filter(k->k.getParent().toString().equals(relativePath.toString()))
            .forEach(result::add);
        // Add things in the shadow directory.
        files = shadowFile.listFiles();
        if (files != null) {
            for (File file : files) {
                result.add(shadowData.toPath().relativize(file.toPath()));
            }
        }
        return result;
    }

    public void removeRecursive(Path path) {
        listPaths(path).forEach(this::delete);
        delete(path);
    }

    /**
     * Given a path, return the file, if any, appropriate to open to read.
     *
     * @param path for which an input file is desired.
     * @return the file
     */
    public File inputFile(Path path) {
        ensureValidPath(path);
        Path relativePath = path.isAbsolute() ? baseDir.toPath().relativize(path) : path;
        // If there's a shadow file, return it.
        File file = shadowData.toPath().resolve(relativePath).toFile();
        if (file.exists()) return file;

        // Check for any operation that affects this path...
        FileOp op = workQueue.get(relativePath);
        // Maybe something is scheduled to be renamed to this name?
        if (op instanceof MoveOp) {
            MoveOp mop = (MoveOp) op;
            return baseDir.toPath().resolve(mop.fromPath).toFile();
        }

        // Not overwritten, so defer to base file.
        file = baseDir.toPath().resolve(relativePath).toFile();
        return file;
    }

    /**
     * Gets a FileInputStream from the given Path or File.
     *
     * @param file or path to be opened.
     * @return a FileInputStream
     * @throws FileNotFoundException if there is no backing file.
     */
    public FileInputStream fileInputStream(File file) throws
                                                      FileNotFoundException { return fileInputStream(file.toPath()); }

    public FileInputStream fileInputStream(Path path) throws FileNotFoundException {
        ensureValidPath(path);
        Path relativePath = path.isAbsolute() ? baseDir.toPath().relativize(path) : path;
        // If there's a shadow file, return it.
        File file = shadowData.toPath().resolve(relativePath).toFile();
        if (file.exists()) return new FileInputStream(file);

        // Check for any operation that affects this path...
        FileOp op = workQueue.get(relativePath);
        // Maybe something is scheduled to be renamed to this name?
        if (op instanceof MoveOp) {
            MoveOp mop = (MoveOp) op;
            file = baseDir.toPath().resolve(mop.fromPath).toFile();
            return new FileInputStream(file);
        }
        // Maybe this name has been explicitly deleted?
        if (op instanceof DeleteOp || op instanceof MovedOutOp) {
            throw new FileNotFoundException("File does not exist: " + relativePath);
        }

        // Not explicitly deleted or moved, so defer to base file.
        file = baseDir.toPath().resolve(relativePath).toFile();
        return new FileInputStream(file);
    }

    /**
     * Returns a File suitable for wrapping with a FileOutputStream.
     *
     * @param path    The path for which a file is desired.
     * @param noTrack If true, don't add ot the work queue. Only use this if checking to see if
     *                the file already exists.
     * @return the File.
     */
    @SuppressWarnings("JavadocReference")
    public File outputFile(Path path) {
        return outputFile(path, false);
    }

    public File outputFile(Path path, boolean noTrack) {
        ensureValidPath(path);
        Path relativePath = path.isAbsolute() ? baseDir.toPath().relativize(path) : path;
        Path shadowedPath = shadowData.toPath().resolve(relativePath);

        if (!noTrack) {
            // **** State tracking ****
            // Check for any operation that affects this path...
            workQueue.remove(relativePath);
            // Remember that we added it.
            workQueue.put(relativePath, new AddOp());
            persistWorkQueue();
        }
        File outputFile = shadowedPath.toFile();
        File parent = outputFile.getParentFile();
        if (!parent.exists()) parent.mkdirs();
        return outputFile;
    }

    /**
     * Gets a FileOutputStream from the given File or Path.
     *
     * @param newFile or path: the File or Path to be opened.
     * @param append: if true, open the file for appending, otherwise truncate any existing file.
     * @return the FileOutputStream
     * @throws FileNotFoundException if the file can't be created.
     */
    @SuppressWarnings("JavadocReference")
    public FileOutputStream fileOutputStream(File newFile) throws FileNotFoundException {
        return fileOutputStream(newFile,
            false);
    }

    public FileOutputStream fileOutputStream(File newFile, boolean append) throws
                                                                           FileNotFoundException {
        return fileOutputStream(newFile.toPath(),
            append);
    }

    public FileOutputStream fileOutputStream(Path path) throws FileNotFoundException {
        return fileOutputStream(path,
            false);
    }

    public FileOutputStream fileOutputStream(Path path, boolean append) throws FileNotFoundException {
        ensureValidPath(path);
        Path relativePath = path.isAbsolute() ? baseDir.toPath().relativize(path) : path;

        // **** State tracking ****
        // Check for any operation that affects this path...
        workQueue.remove(relativePath);
        // Remember that we added it.
        workQueue.put(relativePath, new AddOp());
        persistWorkQueue();

        File outputFile = outputFile(relativePath);
        File parent = outputFile.getParentFile();
        if (!parent.exists()) parent.mkdirs();
        return new FileOutputStream(outputFile, append);
    }

    /**
     * Rename (move) a file. If the source file is outside of the sandbox, this is equivalent to a new file.
     *
     * @param from File or Path to be renamed/moved from.
     * @param to   File or Path to be renamed/moved to.
     * @throws FileNotFoundException if the from file can't be found.
     */
    public boolean moveFile(File from, File to) throws FileNotFoundException {
        return moveFile(from.toPath(), to.toPath());
    }

    public boolean moveFile(Path from, Path to) throws FileNotFoundException {
        boolean result = false;
        ensureValidPath(to);
        Path relativeToPath = to.isAbsolute() ? baseDir.toPath().relativize(to) : to;
        File shadowedToFile = shadowData.toPath().resolve(relativeToPath).toFile();
        if (isSandboxedPath(from)) {
            // Moving a file within the sandboxed files.
            Path relativeFromPath = from.isAbsolute() ? baseDir.toPath().relativize(from) : from;
            File shadowedFromFile = shadowData.toPath().resolve(relativeFromPath).toFile();
            File baseFromFile = baseDir.toPath().resolve(relativeFromPath).toFile();
            if (shadowedFromFile.exists()) {
                if (shadowedFromFile.isDirectory()) {
                    throw new IllegalArgumentException("Argument to moveFile must not be a directory: "+shadowedFromFile.getAbsolutePath());
                }
                // Move the existing shadowed file.
                File toParent = shadowedToFile.getParentFile();
                if (!toParent.exists()) toParent.mkdirs();
                if (shadowedFromFile.renameTo(shadowedToFile)) {
                    result = true;
                    workQueue.remove(relativeToPath);
                    workQueue.put(relativeToPath, new AddOp());
                    workQueue.remove(relativeFromPath);
                    if (baseFromFile.exists()) {
                        workQueue.put(relativeFromPath, new DeleteOp());
                    }
                    persistWorkQueue();
                }
            } else if (baseFromFile.exists()) {
                if (baseFromFile.isDirectory()) {
                    throw new IllegalArgumentException("Argument to moveFile must not be a directory: "+baseFromFile.getAbsolutePath());
                }
                result = true; // we're assuming the rename will work in the future.
                workQueue.remove(relativeToPath);
                workQueue.put(relativeToPath, new MoveOp(relativeFromPath));
                workQueue.remove(relativeFromPath);
                // If we don't put another file here, we'll need to let the handlers know that this file no
                // longer exists.
                workQueue.put(relativeFromPath, new MovedOutOp());
                persistWorkQueue();
            }
        } else {
            // Moving an external file into the sandboxed files.
            if (!from.isAbsolute()) {
                throw new AbsolutePathRequired(from);
            }
            File fromFile = from.toFile();
            if (fromFile.isDirectory()) {
                throw new IllegalArgumentException("Argument to moveFile must not be a directory: "+fromFile.getAbsolutePath());
            }
            if (fromFile.renameTo(shadowedToFile)) {
                result = true;
                workQueue.remove(relativeToPath);
                workQueue.put(relativeToPath, new AddOp());
                persistWorkQueue();
            }
        }
        return result;
    }

    public boolean moveDirectory(File from, File to) throws FileNotFoundException {
        return moveDirectory(from.toPath(), to.toPath());
    }
    public boolean moveDirectory(Path from, Path to) throws FileNotFoundException {
        if (!isDirectory(from)) {
            throw new NotASandboxedDirectory(from);
        }
        if (exists(to) && !isDirectory(to)) {
            throw new NotASandboxedDirectory(to);
        }
        boolean ok = true;
        ensureValidPath(to);
        Path relativeToRoot = to.isAbsolute() ? baseDir.toPath().relativize(to) : to;
        File shadowedToFile = shadowData.toPath().resolve(relativeToRoot).toFile();
        Path relativeFromRoot = from.isAbsolute() ? baseDir.toPath().relativize(from) : from;
        workQueue.put(relativeToRoot, new MkDirOp());
        // Move the children individually.
        Collection<Path> children = listPaths(from);
        for (Path relativeFromChild : children) {
            Path relativeDiff = relativeFromRoot.relativize(relativeFromChild);
            Path relativeToChild = relativeToRoot.resolve(relativeDiff);
            if (isDirectory(relativeFromChild)) {
                ok &= moveDirectory(relativeFromChild, relativeToChild);
            } else {
                ok &= moveFile(relativeFromChild, relativeToChild);
            }
        }
        workQueue.put(relativeFromRoot, new RmDirOp());
        return ok;
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
     *
     * @return a copy of the work queue.
     */
    Map<Path, FileOp> getWorkQueue() {
        return new LinkedHashMap<>(workQueue);
    }

    // File operation & sequencing support
    abstract static class FileOp {
        abstract char getId();
    }

    static class DeleteOp extends FileOp {
        @Override
        char getId() { return 'D'; }
    }

    static class MovedOutOp extends FileOp {
        @Override
        char getId() { return 'O'; }
    }

    static class AddOp extends FileOp {
        @Override
        char getId() { return 'A'; }
    }

    static class MoveOp extends FileOp {
        final Path fromPath;
        public MoveOp(Path fromPath) {
            this.fromPath = fromPath;
        }
        @Override
        char getId() { return 'M'; }
    }

    static class MkDirOp extends FileOp {
        @Override
        char getId() { return 'K'; }
    }

    static class RmDirOp extends FileOp {
        @Override
        char getId() { return 'L'; }
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
