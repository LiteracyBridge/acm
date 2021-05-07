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
    private File external;
    private int fileNo = 0;
    private Sandbox sb;

    private void init() throws IOException {
        external = null;
        fileNo = 100;
        base = folder.newFolder("persistent");
        sandbox = folder.newFolder("sandbox");
        sb = new Sandbox(base, sandbox);
    }

    private void initWithFile() throws IOException {
        init();
        File newFile = new File(base, filename);
        newFile.createNewFile();
    }

    private void initWithBaseFiles() throws IOException {
        initWithFile();
        makeData(new FileOutputStream(new File(base, "a")), 10);
        makeData(new FileOutputStream(new File(base, "b")), 20);
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
        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected one sb file", 1, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected no base file", 0, bFiles==null?0:bFiles.length);
    }

    @Test
    public void testWriteOverwriteFile() throws IOException {
        initWithFile();

        try (OutputStream os = sb.fileOutputStream(new File(base, filename));
             OutputStreamWriter fw = new OutputStreamWriter(os);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("Hello, world!");
        }
        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected one sb file", 1, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected one base file", 1, bFiles==null?0:bFiles.length);
    }

    @Test
    public void testExistence1() throws IOException {
        initWithFile();
        boolean exists = sb.exists(new File(base, filename));

        assertTrue("Expect file to exist", exists);

        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected NO sb file", 0, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected ONE base file", 1, bFiles==null?0:bFiles.length);
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

        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected one sb file", 1, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected one base file", 1, bFiles==null?0:bFiles.length);
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

        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected NO sb file", 0, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected ONE base file", 1, bFiles==null?0:bFiles.length);
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

        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected NO sb file", 0, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected ONE base file", 1, bFiles==null?0:bFiles.length);
        try (OutputStream os = sb.fileOutputStream(new File(base, filename));
             OutputStreamWriter fw = new OutputStreamWriter(os);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("Hello, world!");
        }

        exists = sb.exists(testFile);
        assertTrue("Expect file to again exist", exists);

        sbFiles = sandbox.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected one sb file", 1, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected one base file", 1, bFiles==null?0:bFiles.length);
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

        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected NO sb file", 0, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected ONE base file", 1, bFiles==null?0:bFiles.length);
        try (OutputStream os = sb.fileOutputStream(new File(base, filename));
             OutputStreamWriter fw = new OutputStreamWriter(os);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("Hello, world!");
        }

        exists = sb.exists(testFile);
        assertTrue("Expect file to again exist", exists);

        sbFiles = sandbox.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected one sb file", 1, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected one base file", 1, bFiles==null?0:bFiles.length);

        sb.delete(testFile);
        exists = sb.exists(testFile);
        assertFalse("Expect file NOT to exist", exists);

        sbFiles = sandbox.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected no sb file", 0, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected one base file", 1, bFiles==null?0:bFiles.length);
    }

    @Test
    public void testStructure1() throws IOException {
        initWithBaseFiles();

        // sb.fileOutputStream(new File(base, "c"));
        File a = new File(base, "a");
        File b = new File(base, "b");
        File c = new File(base, "c");

        addFile(c, ++fileNo);

        sb.rename(a, b);    // Only marks the move.
        sb.rename(c, a);    // Renames the sandboxed c as a
        listOperationQueue("Add c, move a->b, move c->a");

        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected 1 sb file", 1, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles==null?0:bFiles.length);

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

        sb.rename(a, b);
        sb.rename(c, a);
        listOperationQueue("Add a, add c, add d, move a->b, move c->a");

        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected 3 sb files", 3, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles==null?0:bFiles.length);

        sb.commit();

        sbFiles = sandbox.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected no sb files", 0, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected 4 base files", 4, bFiles==null?0:bFiles.length);

    }

    @Test
    public void testStructure3() throws IOException {
        initWithBaseFiles();
        System.out.println("\n testStructure3");

        File myFile = new File(base, filename);
        File myOther = new File(base, filename+"2");
        File a = new File(base, "a");
        File b = new File(base, "b");
        File c = new File(base, "c");
        File d = new File(base, "d");

        addFile(a, ++fileNo);
        addFile(c, ++fileNo);

        addFile(d, ++fileNo);

        sb.rename(a, b);
        sb.rename(c, a);
        listOperationQueue("Add a, add c, add d, move a->b, move c->a");

        addFile(c, ++fileNo);
        sb.rename(a, b);
        sb.rename(c, a);
        listOperationQueue("Add a, add c, add d, move a->b, move c->a, add c, move a->b, move c->a");

        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected 3 sb files", 3, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles==null?0:bFiles.length);

        sb.rename(myFile, myOther);
        listOperationQueue("Add a, add c, add d, move a->b, move c->a, add c, move a->b, move c->a, move myFile->myFile2");

        sb.commit(x->System.out.printf("Wrote to %s\n", x), x->System.out.printf("Removed %s\n", x));

        sbFiles = sandbox.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected no sb files", 0, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected 4 base files", 4, bFiles==null?0:bFiles.length);

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
        sb.rename(a, b);

        listOperationQueue("Add a, move a->b");

        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected 1 sb file", 1, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles==null?0:bFiles.length);

        sb.commit(x->System.out.printf("Wrote to %s\n", x), x->System.out.printf("Removed %s\n", x));

        sbFiles = sandbox.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected no sb file", 0, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected 2 base files", 2, bFiles==null?0:bFiles.length);

        assertFalse("Expected a to not exist ", a.exists());
        assertEquals("Expected b to be length "+newASize, newASize, b.length());
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

        sb.rename(a, b);
        long newASize = ++fileNo;
        addFile(a, newASize);

        listOperationQueue("Move a->b, add a");

        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected 1 sb file", 1, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles==null?0:bFiles.length);

        sb.commit(x->System.out.printf("Wrote to %s\n", x), x->System.out.printf("Removed %s\n", x));

        sbFiles = sandbox.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected no sb file", 0, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles==null?0:bFiles.length);

        assertTrue("Expected a to exist ", a.exists());
        assertTrue("Expected b to exist ", b.exists());
        assertEquals("Expected a to be length "+newASize, newASize, a.length());
        assertEquals("Expected b to be length "+aSize, aSize, b.length());
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

        sb.rename(a, b);

        listOperationQueue("Move a->b");

        File[] sbFiles = sandbox.listFiles();
        File[] bFiles = base.listFiles();

        assertEquals("Expected 0 sb file", 0, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected 3 base files", 3, bFiles==null?0:bFiles.length);

        sb.commit(x->System.out.printf("Wrote to %s\n", x), x->System.out.printf("Removed %s\n", x));

        sbFiles = sandbox.listFiles();
        bFiles = base.listFiles();

        assertEquals("Expected no sb file", 0, sbFiles==null?0:sbFiles.length);
        assertEquals("Expected 2 base files", 2, bFiles==null?0:bFiles.length);

        assertFalse("Expected a to NOT exist ", a.exists());
        assertTrue("Expected b to exist ", b.exists());
        assertEquals("Expected b to be length "+aSize, aSize, b.length());
    }

    /**
     * Prints the operation queue.
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
                System.out.printf("  move: %s from %s\n", e.getKey(), ((MoveOp)e.getValue()).fromPath);
            }
        }
    }

    private void addFile(File newFile, long size) throws IOException {
        FileOutputStream fos = sb.fileOutputStream(newFile);
        makeData(fos, size);
    }

    private File makeData(boolean isExternal) throws IOException {
        String fn = String.format("f%d", ++fileNo);
        return makeData(fn, fileNo, isExternal);
    }
    private File makeData(String fn, int size, boolean isExternal) throws IOException {
        File newFile;
        FileOutputStream fos;
        if (isExternal) {
            if (external == null) external = folder.newFolder("external");
            newFile = new File(external, fn);
            fos = new FileOutputStream(newFile);
        } else {
            newFile = new File(base, fn);
            fos = sb.fileOutputStream(newFile);
        }
        // Give it an identifiable size.
        makeData(fos, size);
        return newFile;
    }
    private void makeData(FileOutputStream fos, long size) throws IOException {
        byte[] data = new byte[1];
        for (int i=0; i<size; i++) fos.write(data);
    }


}