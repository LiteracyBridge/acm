package org.literacybridge.talkingbookapp.util.device_manager

import android.content.ContentResolver
import androidx.documentfile.provider.DocumentFile
import org.literacybridge.core.fs.TbFile
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Arrays


/**
 * This is an implementation of TbFile that wraps the Android DocumentFile. The DocumentFile is
 * a performance train wreck. We might eke out a bit more performance by caching lists of child
 * files, rather than always asking the OS.
 */
class AndroidDocFile : TbFile {
    private var parent: AndroidDocFile?
    private var filename: String?
    private var file: DocumentFile?
    private var resolver: ContentResolver

    /**
     * Creates a "root" AndroidDocFile.
     * @param file The DocFile.
     * @param resolver The ContentResolver for the docfile and it's children.
     */
    internal constructor(file: DocumentFile?, resolver: ContentResolver) {
        parent = null
        filename = null
        this.file = file
        this.resolver = resolver
    }

    /**
     * Private constructor for when we don't know the child file, or it doesn't exist yet.
     * @param parent The parent of this new file.
     * @param child The name of this new file.
     */
    private constructor(parent: AndroidDocFile, child: String) {
        this.parent = parent
        filename = child
        file = null
        resolver = parent.resolver
    }

    /**
     * Private constructor for when we already have the DocumentFile.
     * @param parent Parent directory of the new file.
     * @param child The new DocumentFile.
     */
    private constructor(parent: AndroidDocFile, child: DocumentFile) {
        this.parent = parent
        filename = child.name!!
        file = child
        resolver = parent.resolver
    }

    /**
     * Attempts to "resolve" the actual DocumentFile for this wrapper object. Resolves
     * parent first.
     */
    private fun resolve() {
        if (file == null) {
            parent?.resolve()
            if (parent?.file != null) {
                file = filename?.let { parent?.file?.findFile(it) }
            }
        }
    }

    override fun open(child: String): AndroidDocFile {
        return AndroidDocFile(this, child)
    }

    override fun getParent(): AndroidDocFile? {
        return parent
    }

    override fun getName(): String? {
        return filename
    }

    override fun getAbsolutePath(): String {
        resolve()
        return file!!.uri.path!!
    }

    override fun renameTo(newName: String) {
        resolve()
        if (file != null) {
            file!!.renameTo(newName)
            filename = newName
        }
    }

    override fun exists(): Boolean {
        resolve()
        return file != null
    }

    override fun isDirectory(): Boolean {
        resolve()
        return file != null && file!!.isDirectory
    }

    override fun mkdir(): Boolean {
        resolve()
        // Is there already a file or directory here? Can't create one.
        if (file != null) return false
        // Is there a parent? If not, can't create this child.
        if (parent?.file == null) return false
        file = filename?.let { parent?.file!!.createDirectory(it) }
        return file != null
    }

    override fun mkdirs(): Boolean {
        resolve()
        // Is there already a file or directory here? Can't create one.
        if (file != null) return false
        // If there's no parent, try to create it.
        if (parent?.file == null) {
            val parentOk = parent!!.mkdirs()
            if (!parentOk) return false
        }
        // See if the directory already exists.
        file = parent?.file!!.findFile(filename!!)
        // TODO: throw error if exists as a file?
        // If not, create it.
        if (file == null) file = parent?.file!!.createDirectory(filename!!)
        return file != null
    }

    @Throws(IOException::class)
    override fun createNew(content: InputStream, vararg flags: Flags) {
        val appendToExisting = Arrays.asList(*flags).contains(Flags.append)
        val streamFlags = if (appendToExisting) "wa" else "w"
        resolve()
        if (file == null) {
            file = parent?.file!!.createFile("application/octet-stream", filename!!)
        }
        resolver.openOutputStream(file!!.uri, streamFlags).use { out -> copy(content, out) }
    }

    @Throws(IOException::class)
    override fun createNew(vararg flags: Flags): OutputStream {
        val appendToExisting = Arrays.asList(*flags).contains(Flags.append)
        val streamFlags = if (appendToExisting) "wa" else "w"
        resolve()
        if (file == null) {
            file = parent?.file!!.createFile("application/octet-stream", filename!!)
        }
        return resolver.openOutputStream(file!!.uri, streamFlags)!!
    }

    override fun delete(): Boolean {
        resolve()
        // If no file, consider it successfully deleted.
        if (file == null) return true
        // Otherwise try to delete, and if successful, null out the file handle.
        val result = file!!.delete()
        if (result) file = null
        return result
    }

    override fun length(): Long {
        resolve()
        return if (file != null) file!!.length() else 0
    }

    override fun lastModified(): Long {
        return file!!.lastModified()
    }

    override fun list(): Array<String?> {
        return list(null)
    }

    override fun list(filter: FilenameFilter?): Array<String?> {
        resolve()
        if (file == null || !file!!.isDirectory) return mutableListOf<String>().toTypedArray()
        val fileNames: MutableList<String?> = ArrayList()
        val files = file!!.listFiles()
        for (file in files) {
            val name = file.name
            if (name == null) {
                // We used to log these, but there was no useful information. This is
                // probably the same as the !exists() check in listFiles.
            } else {
                if (filter == null || filter.accept(this, file.name)) {
                    fileNames.add(file.name)
                }
            }
        }
        return fileNames.toTypedArray()
    }

    override fun listFiles(filter: FilenameFilter): Array<out AndroidDocFile?> {
        resolve()
        if (file == null || !file!!.isDirectory) return arrayOfNulls(0)
        val filteredFiles: MutableList<AndroidDocFile> = ArrayList()
        val files = file!!.listFiles()
        for (file in files) {
            // The "exists" is to deal with a situation in Android 5.1 where the listFiles() call returns
            // a file named ".android_secure", but file.exists() is false.
            // Jeez, Google, how the hell is anybody supposed to deal with stunts like this?
            if (file.exists() && (filter == null || filter.accept(this, file.name))) {
                filteredFiles.add(AndroidDocFile(this, file))
            }
        }
        return filteredFiles.toTypedArray()
    }

    override fun getFreeSpace(): Long {
        return 0
    }

    @Throws(IOException::class)
    override fun openFileInputStream(): InputStream {
        return resolver.openInputStream(file!!.uri)!!
    }
}

