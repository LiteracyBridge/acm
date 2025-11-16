package org.literacybridge.acm.tools

import com.opencsv.CSVWriterBuilder
import com.opencsv.ICSVWriter
import kotlinx.serialization.json.*
import org.json.simple.JSONObject
import org.literacybridge.acm.cloud.Authenticator
import org.literacybridge.acm.config.ACMConfiguration
import org.literacybridge.acm.gui.CommandLineParams
import org.literacybridge.acm.gui.util.language.LanguageUtil
import org.literacybridge.acm.importexport.CSVExporter
import org.literacybridge.acm.store.AudioItem
import org.literacybridge.acm.store.Category
import org.literacybridge.acm.tbbuilder.TBBuilder
import org.literacybridge.acm.utils.EmailHelper
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.Writer
import java.util.logging.Logger

class DBExporter private constructor(
    dbName: String,
    private val exportDirectory: File,
    private val filenamePrefix: String
) {
    private val project = dbName.substring(TBBuilder.ACM_PREFIX.length)

    /**
     * Creates and initializes an exporter.
     * @param dbName The ACM directory, with the ACM- part; 'ACM-EXAMPLE'.
     * @param exportDir A directory into which to export the metadata.
     * @throws Exception If a file could not be written.
     */
    constructor(dbName: String, exportDir: File) : this(dbName, exportDir, "")

    init {
        ACMConfiguration.getInstance().setCurrentDB(dbName)
    }

    fun export() {
        categoriesExporter()
        languagesExporter()
        metadataExporter()
        uploadMetadata()
    }

    private fun categoriesExporter() {
        val header = arrayOf("ID", "Name", "Project")
        val exportFile = File(exportDirectory, filenamePrefix + "categories.csv")

        try {
            val writer = CSVWriterBuilder(FileWriter(exportFile)).build()
            writer.writeNext(header)
            val leaf = ACMConfiguration.getInstance().currentDB
                .metadataStore.taxonomy.rootCategory
            getChildren(writer, leaf)
            writer.close()
        } catch (e: IOException) {
            println("==========>Could not export categories!")
            e.printStackTrace()
        }
    }

    private fun languagesExporter() {
        val header = arrayOf("ID", "Name", "Project")
        val exportFile = File(exportDirectory, filenamePrefix + "languages.csv")
        try {
            val writer = CSVWriterBuilder(FileWriter(exportFile)).build()
            // String[] header = {"ID","Name","Project"};
            writer.writeNext(header)
            val languages = ACMConfiguration.getInstance().currentDB
                .audioLanguages
            for (l in languages) {
                val languageCode = l.language
                val languageLabel = LanguageUtil.getLocalizedLanguageName(l)
                val values = arrayOf(languageCode, languageLabel, project)
                writer.writeNext(values)
            }
            writer.close()
        } catch (e: IOException) {
            println("==========>Could not export languages!")
            e.printStackTrace()
        }
    }

    private fun metadataExporter() {
        val exportFile = File(exportDirectory, filenamePrefix + "metadata.csv")
        try {
            val exportWriter: Writer = FileWriter(exportFile)
            val items: Iterable<AudioItem> = ACMConfiguration.getInstance().currentDB.metadataStore.audioItems
            CSVExporter.exportMessages(items, exportWriter)
        } catch (e: IOException) {
            println("==========>Could not export metadata!")
            e.printStackTrace()
        }
    }

    private fun getChildren(writer: ICSVWriter, cat: Category) {
        for (child in cat.sortedChildren) {
            val values = arrayOfNulls<String>(3)
            values[0] = child.id
            values[1] = child.categoryName
            values[2] = project
            writer.writeNext(values)
            if (child.hasChildren()) {
                getChildren(writer, child)
            }
        }
    }

    private fun uploadMetadata() {
        val requestURL = Authenticator.ACCESS_CONTROL_API + "/deployment-metadata?platform=talking-book"
//        var test = File(metadataDir, TBBuilder.CONTENT_IN_PACKAGES_CSV_FILE_NAME).readText(Charsets.UTF_8)

        val jsonObject = buildJsonObject {
            var csv = File(exportDirectory, "contentinpackages.csv")
            if (csv.exists()) {
                put("contentInPackages", csv.readText())
            }

            csv = File(exportDirectory, "categoriesinpackages.csv")
            if (csv.exists()) {
                put("categoriesInPackage", csv.readText())
            }

            csv = File(exportDirectory, "packagesindeployment.csv")
            if (csv.exists()) {
                put("packagesInDeployment", csv.readText())
            }

            csv = File(exportDirectory, "categories.csv")
            if (csv.exists()) {
                put("categories", csv.readText())
            }

            csv = File(exportDirectory, "languages.csv")
            if (csv.exists()) {
                put("languages", csv.readText())
            }

            csv = File(exportDirectory, "metadata.csv")
            if (csv.exists()) {
                put("metadata", csv.readText())
            }
        }

        println(jsonObject.toString())
//        test.
//        metadataFile.writeText(toJson(), Charsets.UTF_8)

        val requestBody = JSONObject(jsonObject)

        val jsonResponse =
            Authenticator.getInstance().awsInterface.authenticatedPostCall(requestURL, requestBody)
        if (jsonResponse != null) {
            var o = jsonResponse["ResponseMetadata"]
            if (o is JSONObject) {
                o = (o as JSONObject)["HTTPStatusCode"]
            }
//            if (o is Long) {
//                status_aws = o == EmailHelper.EMAIL_SENT_RESPONSE.toLong()
//            }
            Logger.getLogger(EmailHelper::class.java.name)
                .info(String.format("email: %s\n          %s\n", requestBody, jsonResponse))
        }
        println(jsonResponse)
    }


    companion object {
        private fun printUsage() {
            println(
                "Usage: java -cp acm.jar:lib/* org.literacybridge.acm.tools.DBExporter <export directory> <acm_name>+"
            )
        }

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val argCount = args.size
            if (argCount < 2) {
                printUsage()
                System.exit(1)
            }
            val exportDir = File(args[0])
            if (!exportDir.isDirectory) {
                throw Exception(
                    """
                        Export directory doesn't exist.
                        ${exportDir.absolutePath}
                        """.trimIndent()
                )
            }

            val params = CommandLineParams()
            params.disableUI = true
            params.sandbox = true
            ACMConfiguration.initialize(params)

            for (i in 1..<argCount) {
                try {
                    val prefix = args[i].substring(TBBuilder.ACM_PREFIX.length) + "-"
                    val exporter = DBExporter(args[i], exportDir, prefix)
                    exporter.export()
                } catch (ex: IllegalArgumentException) {
                    System.out.printf("Failed to export %s: %s%n", args[i], ex.message)
                }
            }
        }
    }
}
