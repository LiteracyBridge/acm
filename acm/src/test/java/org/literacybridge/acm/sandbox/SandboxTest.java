package org.literacybridge.acm.sandbox;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.literacybridge.acm.sandbox.Sandbox.AddOp;
import org.literacybridge.acm.sandbox.Sandbox.DeleteOp;
import org.literacybridge.acm.sandbox.Sandbox.FileOp;
import org.literacybridge.acm.sandbox.Sandbox.MoveOp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SandboxTest {
    public static final String filename = "myFile";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File base;
    private File sandbox;
    private File sandboxData;
    private File external;
    private int fileNo = 0;
    private Sandbox sb;

    private void init() throws IOException {
        external = null;
        fileNo = 100;
        base = folder.newFolder("persistent");
        sandbox = folder.newFolder("sandbox");
        sandboxData = new File(sandbox, "data");
        sb = new Sandbox(base, sandbox);
    }

    private void initWithFile() throws IOException {
        init();
        File newFile = new File(base, filename);
        newFile.createNewFile();
    }

    private void initWithBaseFiles() throws IOException {
        initWithFile();
        try (FileOutputStream fos = new FileOutputStream(new File(base, "a"))) {
            makeData(fos, 10);
        }
        try (FileOutputStream fos = new FileOutputStream(new File(base, "b"))) {
            makeData(fos, 20);
        }
    }

    private void initWithTree() throws IOException {
        init();
        File dir = new File(base, "parent");
        dir.mkdir();
        new File(dir, "child1").createNewFile();
        new File(dir, "child2").createNewFile();
        File subdir = new File(dir, "subdir1");
        subdir.mkdir();
        new File(subdir, "sub1-child1").createNewFile();
        new File(subdir, "sub1-child2").createNewFile();
        File subdir2 = new File(dir, "subdir2");
        subdir2.mkdir();
        new File(subdir2, "sub2-child1").createNewFile();
        new File(subdir2, "sub2-child2").createNewFile();

    }

    @Test
    public void testCreate() throws IOException {
        Sandbox sb = new Sandbox(folder.newFolder("persistent"), folder.newFolder("sandbox"));
    }

    @Test
    public void testContained() throws IOException {
        init();
        boolean threw = false;

        File sandboxedFile = new File(base, filename);
        boolean isSandboxed = sb.isSandboxedFile(sandboxedFile);
        assertTrue("Expected file isSandboxedPath()", isSandboxed);

        try {
            sb.ensureValidPath(sandboxedFile.toPath());
        } catch (Sandbox.NotASandboxedDirectory ex) {
            threw = true;
        }
        assertFalse("Should NOT have thrown", threw);
    }

    @Test
    public void testNotContained() throws IOException {
        init();
        boolean threw = false;

        File notSandboxedFile = new File(sandbox, filename);
        boolean isSandboxed = sb.isSandboxedFile(notSandboxedFile);
        assertFalse("Expected NOT file isSandboxedPath()", isSandboxed);

        try {
            sb.ensureValidPath(notSandboxedFile.toPath());
        } catch (Sandbox.NotASandboxedDirectory ex) {
            threw = true;
        }
        assertTrue("Should have thrown", threw);
    }

    @Test
    public void testGetOutputStream() throws IOException {
        init();

        OutputStream os = sb.fileOutputStream(new File(base, filename));
    }

    @Test
    public void testWriteNewFile() throws IOException {
        init();

        try (OutputStream os = sb.fileOutputStream(new File(base, filename));
             OutputStreamWriter fw = new OutputStreamWriter(os);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("Hello, world!");
        }
        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected one sb file", 1, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected no base file", 0, bFiles == null ? 0 : bFiles.length);
    }

    @Test
    public void testWriteOverwriteFile() throws IOException {
        initWithFile();

        try (OutputStream os = sb.fileOutputStream(new File(base, filename));
             OutputStreamWriter fw = new OutputStreamWriter(os);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("Hello, world!");
        }
        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected one sb file", 1, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected one base file", 1, bFiles == null ? 0 : bFiles.length);
    }

    @Test
    public void testExistence1() throws IOException {
        initWithFile();
        boolean exists = sb.exists(new File(base, filename));

        assertTrue("Expect file to exist", exists);

        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected NO sb file", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected ONE base file", 1, bFiles == null ? 0 : bFiles.length);
    }

    @Test
    public void testExistence2() throws IOException {
        initWithFile();
        boolean exists = sb.exists(new File(base, filename));

        assertTrue("Expect file to exist", exists);

        try (OutputStream os = sb.fileOutputStream(new File(base, filename));
             OutputStreamWriter fw = new OutputStreamWriter(os);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("Hello, world!");
        }
        assertTrue("Expect file to still exist", exists);

        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected one sb file", 1, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected one base file", 1, bFiles == null ? 0 : bFiles.length);
    }

    @Test
    public void testExistence3() throws IOException {
        initWithFile();
        File testFile = new File(base, filename);

        boolean exists = sb.exists(testFile);
        assertTrue("Expect file to exist", exists);

        sb.delete(testFile);
        exists = sb.exists(testFile);
        assertFalse("Expect file NOT to exist", exists);

        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected NO sb file", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected ONE base file", 1, bFiles == null ? 0 : bFiles.length);
    }

    @Test
    public void testExistence4() throws IOException {
        initWithFile();

        File testFile = new File(base, filename);
        boolean exists = sb.exists(testFile);
        assertTrue("Expect file to exist", exists);

        sb.delete(testFile);
        exists = sb.exists(testFile);
        assertFalse("Expect file NOT to exist", exists);

        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected NO sb file", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected ONE base file", 1, bFiles == null ? 0 : bFiles.length);
        try (OutputStream os = sb.fileOutputStream(new File(base, filename));
             OutputStreamWriter fw = new OutputStreamWriter(os);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("Hello, world!");
        }

        exists = sb.exists(testFile);
        assertTrue("Expect file to again exist", exists);

        sbFiles = sandboxData.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected one sb file", 1, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected one base file", 1, bFiles == null ? 0 : bFiles.length);
    }

    @Test
    public void testExistence5() throws IOException {
        initWithFile();

        File testFile = new File(base, filename);
        boolean exists = sb.exists(testFile);
        assertTrue("Expect file to exist", exists);

        sb.delete(testFile);
        exists = sb.exists(testFile);
        assertFalse("Expect file NOT to exist", exists);

        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected NO sb file", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected ONE base file", 1, bFiles == null ? 0 : bFiles.length);
        try (OutputStream os = sb.fileOutputStream(new File(base, filename));
             OutputStreamWriter fw = new OutputStreamWriter(os);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("Hello, world!");
        }

        exists = sb.exists(testFile);
        assertTrue("Expect file to again exist", exists);

        sbFiles = sandboxData.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected one sb file", 1, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected one base file", 1, bFiles == null ? 0 : bFiles.length);

        sb.delete(testFile);
        exists = sb.exists(testFile);
        assertFalse("Expect file NOT to exist", exists);

        sbFiles = sandboxData.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected no sb file", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected one base file", 1, bFiles == null ? 0 : bFiles.length);
    }

    @Test
    public void testStructure1() throws IOException {
        initWithBaseFiles();

        // sb.fileOutputStream(new File(base, "c"));
        File a = new File(base, "a");
        File b = new File(base, "b");
        File c = new File(base, "c");

        addFile(c, ++fileNo);

        sb.moveFile(a, b);    // Only marks the move.
        sb.moveFile(c, a);    // Renames the sandboxed c as a
        listOperationQueue("Add c, move a->b, move c->a");

        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected 1 sb file", 1, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles == null ? 0 : bFiles.length);

    }

    @Test
    public void testStructure2() throws IOException {
        initWithBaseFiles();

        File a = new File(base, "a");
        File b = new File(base, "b");
        File c = new File(base, "c");
        File d = new File(base, "d");

        addFile(a, ++fileNo);
        addFile(c, ++fileNo);
        addFile(d, ++fileNo);

        sb.moveFile(a, b);
        sb.moveFile(c, a);
        listOperationQueue("Add a, add c, add d, move a->b, move c->a");

        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected 3 sb files", 3, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles == null ? 0 : bFiles.length);

        sb.commit();

        sbFiles = sandboxData.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected no sb files", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected 4 base files", 4, bFiles == null ? 0 : bFiles.length);

    }

    @Test
    public void testStructure3() throws IOException {
        initWithBaseFiles();
        System.out.println("\n testStructure3");

        File myFile = new File(base, filename);
        File myOther = new File(base, filename + "2");
        File a = new File(base, "a");
        File b = new File(base, "b");
        File c = new File(base, "c");
        File d = new File(base, "d");

        addFile(a, ++fileNo);
        addFile(c, ++fileNo);

        addFile(d, ++fileNo);

        sb.moveFile(a, b);
        sb.moveFile(c, a);
        listOperationQueue("Add a, add c, add d, move a->b, move c->a");

        addFile(c, ++fileNo);
        sb.moveFile(a, b);
        sb.moveFile(c, a);
        listOperationQueue("Add a, add c, add d, move a->b, move c->a, add c, move a->b, move c->a");

        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected 3 sb files", 3, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles == null ? 0 : bFiles.length);

        sb.moveFile(myFile, myOther);
        listOperationQueue(
            "Add a, add c, add d, move a->b, move c->a, add c, move a->b, move c->a, move myFile->myFile2");

        sb.commit(x -> System.out.printf("Wrote to %s\n", x), x -> System.out.printf("Removed %s\n", x));

        sbFiles = sandboxData.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected no sb files", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected 4 base files", 4, bFiles == null ? 0 : bFiles.length);

    }

    @Test
    public void testStructure4() throws IOException {
        initWithBaseFiles();
        System.out.println("\n testStructure4");

        // sb.fileOutputStream(new File(base, "c"));
        File a = new File(base, "a");
        long aSize = a.length();
        File b = new File(base, "b");
        long bSize = b.length();

        long newASize = ++fileNo;
        addFile(a, newASize);
        sb.moveFile(a, b);

        listOperationQueue("Add a, move a->b");

        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected 1 sb file", 1, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles == null ? 0 : bFiles.length);

        sb.commit(x -> System.out.printf("Wrote to %s\n", x), x -> System.out.printf("Removed %s\n", x));

        sbFiles = sandboxData.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected no sb file", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected 2 base files", 2, bFiles == null ? 0 : bFiles.length);

        assertFalse("Expected a to not exist ", a.exists());
        assertEquals("Expected b to be length " + newASize, newASize, b.length());
    }

    @Test
    public void testStructure5() throws IOException {
        initWithBaseFiles();
        System.out.println("\n testStructure5");

        // sb.fileOutputStream(new File(base, "c"));
        File a = new File(base, "a");
        long aSize = a.length();
        File b = new File(base, "b");
        long bSize = b.length();

        sb.moveFile(a, b);
        long newASize = ++fileNo;
        addFile(a, newASize);

        listOperationQueue("Move a->b, add a");

        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected 1 sb file", 1, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles == null ? 0 : bFiles.length);

        sb.commit(x -> System.out.printf("Wrote to %s\n", x), x -> System.out.printf("Removed %s\n", x));

        sbFiles = sandboxData.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected no sb file", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles == null ? 0 : bFiles.length);

        assertTrue("Expected a to exist ", a.exists());
        assertTrue("Expected b to exist ", b.exists());
        assertEquals("Expected a to be length " + newASize, newASize, a.length());
        assertEquals("Expected b to be length " + aSize, aSize, b.length());
    }

    @Test
    public void testStructure6() throws IOException {
        initWithBaseFiles();
        System.out.println("\n testStructure6");

        // sb.fileOutputStream(new File(base, "c"));
        File a = new File(base, "a");
        long aSize = a.length();
        File b = new File(base, "b");
        long bSize = b.length();

        sb.moveFile(a, b);

        listOperationQueue("Move a->b");

        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected 0 sb file", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles == null ? 0 : bFiles.length);

        sb.commit(x -> System.out.printf("Wrote to %s\n", x), x -> System.out.printf("Removed %s\n", x));

        sbFiles = sandboxData.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected no sb file", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected 2 base files", 2, bFiles == null ? 0 : bFiles.length);

        assertFalse("Expected a to NOT exist ", a.exists());
        assertTrue("Expected b to exist ", b.exists());
        assertEquals("Expected b to be length " + aSize, aSize, b.length());
    }

    @Test
    public void testTreeOperation1() throws IOException {
        initWithTree();
        File dir = new File(base, "parent");

        Collection<Path> paths = sb.listPaths(dir.toPath());
        assertEquals("Expected 4 children", 4, paths.size());
    }

    @Test
    public void testTreeOperation2() throws IOException {
        initWithTree();
        File dir = new File(base, "parent");
        File subdir1 = new File(dir, "subdir1");

        Collection<Path> paths = sb.listPaths(subdir1.toPath());
        assertEquals("Expected 2 children", 2, paths.size());

        sb.removeRecursive(subdir1.toPath());

        paths = sb.listPaths(subdir1.toPath());
        assertEquals("Expected No children", 0, paths.size());
    }

    @Test
    public void testTreeOperation3() throws IOException {
        initWithTree();
        File dir = new File(base, "parent");
        File subdir1 = new File(dir, "subdir1");
        File subdirX = new File(dir, "subdirX");

        Collection<Path> paths = sb.listPaths(subdir1.toPath());
        assertEquals("Expected 2 children", 2, paths.size());

        sb.moveDirectory(subdir1, subdirX);
        listOperationQueue("Rename subdir1->subdirX");

        paths = sb.listPaths(subdir1.toPath());
        assertEquals("Expected No children in subdir1", 0, paths.size());
        paths = sb.listPaths(subdirX.toPath());
        assertEquals("Expected 2 children in subdirX", 2, paths.size());

        File[] s1Files = subdir1.listFiles();
        File[] sxFiles = subdirX.listFiles();
        assertEquals("Expected 2 subdir1 files", 2, s1Files == null ? 0 : s1Files.length);
        assertEquals("Expected 0 subdirX files", 0, sxFiles == null ? 0 : sxFiles.length);

        sb.commit();

        s1Files = subdir1.listFiles();
        sxFiles = subdirX.listFiles();
        assertEquals("Expected 0 subdir1 files", 0, s1Files == null ? 0 : s1Files.length);
        assertEquals("Expected 2 subdirX files", 2, sxFiles == null ? 0 : sxFiles.length);

    }

    @Test
    public void testTreeOperation4() throws IOException {
        initWithTree();
        File dir = new File(base, "parent");
        File subdir1 = new File(dir, "subdir1");
        File subdirX = new File(dir, "subdirX");

        File x1 = new File(subdirX, "subX-file1");
        addFile(x1, ++fileNo);
        File x2 = new File(subdirX, "subX-file2");
        addFile(x2, ++fileNo);

        Collection<Path> paths = sb.listPaths(subdir1.toPath());
        assertEquals("Expected 2 children in subdir1", 2, paths.size());
        paths = sb.listPaths(subdirX.toPath());
        assertEquals("Expected 2 children in subdirX", 2, paths.size());

        File sbDir = new File(sandboxData, "parent");
        File[] s1Files = new File(sbDir, "subdir1").listFiles();
        File[] sxFiles = new File(sbDir, "subdirX").listFiles();
        assertEquals("Expected 0 subdir1 files in sandbox", 0, s1Files == null ? 0 : s1Files.length);
        assertEquals("Expected 2 subdirX files in sandbox", 2, sxFiles == null ? 0 : sxFiles.length);

        sb.removeRecursive(subdir1.toPath());
        listOperationQueue("Create subdirX, remove subdir1");

        sb.moveDirectory(subdirX, subdir1);
        listOperationQueue("Create subdirX, remove subdir1, ename subdirX->subdir1");

        paths = sb.listPaths(subdir1.toPath());
        assertEquals("Expected 2 children in subdir1", 2, paths.size());
        paths = sb.listPaths(subdirX.toPath());
        assertEquals("Expected 0 children in subdirX", 0, paths.size());

        s1Files = subdir1.listFiles();
        sxFiles = subdirX.listFiles();
        assertEquals("Expected 2 subdir1 files in base", 2, s1Files == null ? 0 : s1Files.length);
        assertEquals("Expected 0 subdirX files in base", 0, sxFiles == null ? 0 : sxFiles.length);
        s1Files = new File(sbDir, "subdir1").listFiles();
        sxFiles = new File(sbDir, "subdirX").listFiles();
        assertEquals("Expected 2 subdir1 files in sandbox", 2, s1Files == null ? 0 : s1Files.length);
        assertEquals("Expected 0 subdirX files in sandbox", 0, sxFiles == null ? 0 : sxFiles.length);

        sb.commit();

        s1Files = subdir1.listFiles();
        sxFiles = subdirX.listFiles();
        assertEquals("Expected 2 subdir1 files", 2, s1Files == null ? 0 : s1Files.length);
        assertEquals("Expected 0 subdirX files", 0, sxFiles == null ? 0 : sxFiles.length);

    }

    @Test
    public void testMoveFile1() throws IOException {
        init();

        // Create the file that will be moved.
        File f1 = new File(base, filename);
        try (OutputStream os = new FileOutputStream(new File(base, filename));
             OutputStreamWriter fw = new OutputStreamWriter(os);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("Hello, world!");
        }
        File[] sbFiles = sandboxData.listFiles();
        File[] bFiles = base.listFiles();
        assertEquals("Expected no sb file", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected one base file", 1, bFiles == null ? 0 : bFiles.length);

        sb.moveFile(f1, new File(f1.getName()+".moved"));
        sbFiles = sandboxData.listFiles();
        bFiles = base.listFiles();
        assertEquals("Expected no sb file", 0, sbFiles == null ? 0 : sbFiles.length);
        assertEquals("Expected one base file", 1, bFiles == null ? 0 : bFiles.length);

        Collection<Path> resultingPaths = sb.listPaths(base.toPath());
        assertEquals("Expected to find one net file.", 1, resultingPaths.size());

        // If the test is passing, there's exactly one. here isn't a Collection.get(0), so use the forEach.
        resultingPaths.forEach(p->{
            assertEquals("Expected resulting file to be '" + filename + ".moved'.",
                filename + ".moved",
                p.toFile().getName());
        });
    }

    @Test
    public void testPersistance() throws IOException {
        init();

        File a = new File(base, "a");
        File b = new File(base, "b");
        File c = new File(base, "c");

        addFile(a, ++fileNo);
        addFile(b, ++fileNo);
        addFile(c, ++fileNo);
        Map<Path,FileOp> q1 = sb.getWorkQueue();

        Sandbox sb2 = new Sandbox(base, sandbox);
        Map<Path,FileOp> q2 = sb2.getWorkQueue();

        assertEquals("Expect work queues to be same size.", q1.size(), q2.size());

    }

    /**
     * Prints the operation queue.
     *
     * @param heading before the listing.
     */
    private void listOperationQueue(String heading) {
        Map<Path, FileOp> queue = sb.getWorkQueue();
        System.out.printf("Work Queue after %s:\n", heading);
        for (Map.Entry<Path, FileOp> e : queue.entrySet()) {
            if (e.getValue() instanceof DeleteOp) {
                System.out.printf("delete: %s\n", e.getKey());
            } else if (e.getValue() instanceof AddOp) {
                System.out.printf("   add: %s\n", e.getKey());
            } else if (e.getValue() instanceof MoveOp) {
                System.out.printf("  move: %s from %s\n", e.getKey(), ((MoveOp) e.getValue()).fromPath);
            } else if (e.getValue() instanceof Sandbox.MovedOutOp) {
                System.out.printf("  moved out: %s\n", e.getKey());
            } else if (e.getValue() instanceof Sandbox.MkDirOp) {
                System.out.printf("  mkdir: %s\n", e.getKey());
            } else if (e.getValue() instanceof Sandbox.RmDirOp) {
                System.out.printf("  rmdir: %s\n", e.getKey());
            }
        }
    }

    private void addFile(File newFile, long size) throws IOException {
        try (FileOutputStream fos = sb.fileOutputStream(newFile)) {
            makeData(fos, size);
        }
    }

    private File makeData(boolean isExternal) throws IOException {
        String fn = String.format("f%d", ++fileNo);
        return makeData(fn, fileNo, isExternal);
    }

    private File makeData(String fn, int size, boolean isExternal) throws IOException {
        File newFile;
        if (isExternal) {
            if (external == null) external = folder.newFolder("external");
            newFile = new File(external, fn);
        } else {
            newFile = new File(base, fn);
        }
        try (FileOutputStream fos = isExternal?new FileOutputStream(newFile):sb.fileOutputStream(newFile)) {
            // Give it an identifiable size.
            makeData(fos, size);
        }
        return newFile;
    }

    private void makeData(FileOutputStream fos, long size) throws IOException {
        byte[] data = new byte[1];
        for (int i = 0; i < size; i++) fos.write(data);
    }


}