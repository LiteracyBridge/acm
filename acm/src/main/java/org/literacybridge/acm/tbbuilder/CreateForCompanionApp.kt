package org.literacybridge.acm.tbbuilder

import org.literacybridge.acm.config.ACMConfiguration
import org.literacybridge.acm.deployment.DeploymentInfo
import org.literacybridge.acm.repository.AudioItemRepository
import org.literacybridge.acm.store.AudioItemModel
import org.literacybridge.acm.store.PackageMetadata
import org.literacybridge.acm.tbbuilder.TBBuilder.BuilderContext
import java.io.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/*
* Builds a TBv1 deployment.
* 

* Given a program 'DEMO', a deployment 'DEMO-21-1', and a package 'DEMO-1-en-c'
*
* a "tree.txt" file with empty directories to be created in each image.
*   inbox
*   log
*   log-archive
*   statistics

DEMO/content/DEMO-21-1/images/DEMO-1-en-c/
*
* DEMO/content/DEMO-21-1/
├── firmware.v2
├── recipients
├── shaodowFiles
├── images.v2
│   ├── DEMO-1-en
│   │   ├── content
│   │   │   ├── prompts
│   │   │   │   └── en
│   │   │   │       ├── 0.mp3
│   │   │   │       ├── 1.mp3
│   │   │   │        . . .
│   │   │   │       ├── 9.mp3
│   │   │   │       ├── 9-0.mp3
│   │   │   │       ├── LB-2_kkg8lufhqr_jp.mp3
│   │   │   │        . . .
│   │   │   │       └── iLB-2_uzz71upxwm_vn.mp3
│   │   │   ├── messages
│   │   │   │   ├── LB-2_uzz71upxwm_vg.mp3
│   │   │   │    . . .
│   │   │   │   └── LB-2_uzz71upxwm_zd.mp3
│   │   │   └── packages_data.txt
│   │   └── system
│   │       ├── DEMO-1-en-c.pkg     <<-- zero-byte marker file
│   │       ├── c.grp               <<-- zero-byte marker file
│   │       ├── config.txt          <<-- fairly constant across programs
│   │       └── profiles.txt        <<-- "DEMO-1-EN-C,en,1,menu"
│   ├── DEMO-1-en-c
│   . . .
└── programspec
   ├── content.csv
   ├── content.json                <<-- remove
   ├── deployment.properties
   ├── deployment_spec.csv
   ├── deployments.csv             <<-- remove
   ├── etags.properties
   ├── pending_spec.xlsx           <<-- remove?
   ├── program_spec.xlsx           <<-- remove
   ├── recipients.csv
   └── recipients_map.csv

Format of the package_data.txt file:
1 # format version, currently "1"
${deployment_name} # name of the deployment, like TEST-21-4
${num_path_directories} # number of directories containing audio. Their ordinals are used to make paths
${directory_1} # First path directory; referred to as "1"
${directory_2} # Second path directory
. . .
${num_packages} # number of packages that follow
#- - - - - - - - - - - - start of a package
${package_name_1} # name of the first package
 ${path_ordinal} ${package_announcement} # ordinal of the path that contains the ${package_announcement}
 ${prompts_path} # up to 10 path ordinals, separated by semicolons. Searched in order for system prompts.
 ${num_playlists} # number of playlists that follow
 ${playlist_name_1} # name of the first playlist. Only used for logging purposes.
   ${path_ordinal} ${playlist_announcement_1} # path ordinal and file name of short prompt for playlist 1
   ${path_ordianl} ${playlist_invitation_2} # path ordinal and file name of invitation for playlist 1
   ${num_messages} # number of messages that follow
   ${path_ordinal} ${message_1} # path ordinal and file name of first message
   ${path_ordianl} ${message_2} # path ordinal and file name of second message
   . . .
 ${playlist_name_2} # name of second playlist
 . . .
#- - - - - - - - - - - - end of previous package, start of next package
* ${package_name_2} # name of the second package
. . .

Each image will contain a 1-package package_data.txt. To combine several on a single Talking Book,
concatenate the path lines, and adjust the path ordinals in the audio file lines. Concatenate the
resulting package sections

*
*/
class CreateForCompanionApp internal constructor(
    tbBuilder: TBBuilder,
    private val builderContext: BuilderContext,
    private val deploymentInfo: DeploymentInfo?
) {
    val audioFormat: AudioItemRepository.AudioFormat = AudioItemRepository.AudioFormat.MP3
    private val repository = ACMConfiguration.getInstance().currentDB.repository

    private val baseDir = File(System.getProperty("java.io.tmpdir"), builderContext.deploymentName)
    private var audioItems: List<AudioItemModel> = emptyList()
    private val metadata: PackageMetadata = PackageMetadata()

    init {
//        super(tbBuilder, builderContext, deploymentInfo);
//        allPackagesData = PackagesData(builderContext.deploymentName)
//        imagesDir = File(builderContext.stagedDeploymentDir, "")
//        this.builderContext = builderContext
        createDirs(this.baseDir)
    }

    fun go() {
        // TODO: check for convertion errors

        val languages = ACMConfiguration.getInstance().currentDB.db.query<AudioItemModel>(
            "SELECT language FROM audio_items aud\n" +
                    "INNER JOIN playlists p ON p.id = aud.playlist_id\n" +
                    "INNER JOIN deployments d ON d.id = p.deployment_id AND d.deployment_number = ?\n" +
                    "WHERE type = 'Message' AND (aud.variant = '' OR aud.variant IS NULL)\n" +
                    "GROUP BY language;",
            builderContext.deploymentNo
        )!!.map { it.language }

        for (language in languages) {
            val content = PackageMetadata.PackageContent()
            addMessageToPackage(language, content)

            val path = File(baseDir, language)
            addPromptsToPackage(
                createDirs(File(path, "playlist-prompts")),
                language,
                AudioItemModel.ItemType.PlaylistPrompt,
                content
            )
            addPromptsToPackage(
                createDirs(File(path, "system-prompts")),
                language,
                AudioItemModel.ItemType.SystemPrompt,
                content
            )

            metadata.addMessage(language, content)
        }

        val languageVariants = ACMConfiguration.getInstance().currentDB.db.query<AudioItemModel>(
            "SELECT language, variant FROM audio_items aud\n" +
                    "INNER JOIN playlists p ON p.id = aud.playlist_id\n" +
                    "INNER JOIN deployments d ON d.id = p.deployment_id AND d.deployment_number = ?\n" +
                    "WHERE type = 'Message' AND (aud.variant != '' AND aud.variant IS NOT NULL)\n" +
                    "GROUP BY language, variant;",
            builderContext.deploymentNo
        )!!

        for (rec in languageVariants) {
            if (rec.variant == null || rec.variant!!.isEmpty()) continue;

            val content = PackageMetadata.PackageContent()
            val label = "${rec.language}-${rec.variant}"
            addMessageToPackage(label, content)

            val f = File(baseDir, label)
            addPromptsToPackage(
                createDirs(File(f, "playlist-prompts")),
                rec.language,
                AudioItemModel.ItemType.PlaylistPrompt,
                content
            )
            addPromptsToPackage(
                createDirs(File(f, "system-prompts")),
                rec.language,
                AudioItemModel.ItemType.SystemPrompt,
                content
            )
            metadata.addMessage(label, content)

        }

        // Write metadata to file
        metadata.deployment = PackageMetadata.DeploymentDescription(
            name = builderContext.deploymentName,
            number = builderContext.deploymentNo
        )
        metadata.revision = "TODO: get revision from build context"
        metadata.createdAt =
            Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        metadata.computerName = "TODO: get computer name"
        metadata.createdBy = "TODO: get user email"
        metadata.size = baseDir.length()
        metadata.project = builderContext.project

        val metadataFile = File(baseDir, "metadata.json")
        metadataFile.writeText(metadata.toJson(), Charsets.UTF_8)
    }

    private fun addMessageToPackage(
        language: String,
        content: PackageMetadata.PackageContent,
        variant: String? = null
    ) {
        val dir = if (variant == null) {
            createDirs(File(baseDir, language))
        } else {
            createDirs(File(baseDir, "$language-$variant"))
        }
        val messagesDir = createDirs(File(dir, "messages"))

        var sql =
            "SELECT a.id, a.title, a.acm_id, a.type, a.language, a.variant, p.title AS playlist_title FROM audio_items a\n" +
                    "INNER JOIN playlists p ON p.id = a.playlist_id\n" +
                    "INNER JOIN deployments d ON d.id = p.deployment_id AND d.deployment_number = ?\n" +
                    "WHERE a.language = '${language}' AND type = 'Message' "
        sql += if (variant != null) {
            "  AND a.variant = '$variant'"
        } else {
            "  AND (a.variant = '' OR a.variant IS NULL)"
        }

        // Query messages and add them to the packages
        val audioItems = ACMConfiguration.getInstance().currentDB.db.query<AudioItemModel>(
            sql,
            builderContext.deploymentNo
        )!!
        audioItems.forEach { audioItem ->
            val file = addToPackage(audioItem, messagesDir)
            content.addMessage(audioItem, file, baseDir)
            // Add audio item to the package_data.txt.
//            val exportPath = makePath(File(messagesDir, filename))
//            playlistData.addMessage(audioItem.title, exportPath)
        }
    }

    private fun addPromptsToPackage(
        destDir: File,
        language: String,
        type: AudioItemModel.ItemType,
        content: PackageMetadata.PackageContent
    ) {
        var sql = "SELECT a.id, a.title, a.acm_id, a.type, a.language, a.variant FROM audio_items a\n"
        if (type.name == AudioItemModel.ItemType.PlaylistPrompt.name) {
            sql += "INNER JOIN playlists p ON p.id = a.playlist_id\n" +
                    "INNER JOIN deployments d ON d.id = p.deployment_id AND d.deployment_number = ${builderContext.deploymentNo}\n"
        }
        sql += "WHERE a.language = '${language}' AND type = '$type' AND deleted_at IS NULL"

        // Query prompts and add them to the packages
        ACMConfiguration.getInstance().currentDB.db.query<AudioItemModel>(sql)!!.forEach { audioItem ->
            val file = addToPackage(audioItem, destDir)
            if (type.name == AudioItemModel.ItemType.PlaylistPrompt.name) {
                content.addPlaylistPrompt(audioItem, file, baseDir)
            } else {
                content.addSystemPrompt(audioItem, file, baseDir)
            }
            // Add audio item to the package_data.txt.
//            val exportPath = makePath(File(messagesDir, filename))
//            playlistData.addMessage(audioItem.title, exportPath)

        }

        // If playlist prompts, then and add talking book & user feedback prompts
        if (type.name == AudioItemModel.ItemType.SystemPrompt.name) {
            sql = "SELECT id, title, acm_id, language FROM audio_items " +
                    "WHERE title IN ('user feedback - invitation', 'user feedback', 'talking book - invitation', 'talking book') " +
                    " AND language = '$language'"
            ACMConfiguration.getInstance().currentDB.db.query<AudioItemModel>(sql)!!.forEach { audioItem ->
                val file = addToPackage(audioItem, destDir)
                content.addSystemPrompt(audioItem, file, baseDir)
            }
        }

    }

    private fun addToPackage(audioItem: AudioItemModel, dest: File): File {
        println(String.format("    Exporting audioitem %s to %s%n", audioItem.acm_id, dest))
        builderContext.reportStatus(
            String.format(
                "    Exporting audioitem %s to %s%n",
                audioItem.acm_id,
                dest
            )
        )

        val audioRef = ACMConfiguration.getInstance().currentDB
            .metadataStore.getAudioItem(audioItem.acm_id)
        val filename: String = repository.getAudioFilename(audioRef, audioFormat)

        // Export the audio file.
        val exportFile = File(dest, filename)
        if (!exportFile.exists()) {
            try {
                repository.exportAudioFileWithFormat(audioRef, exportFile, audioFormat)
                return exportFile
            } catch (ex: Exception) {
                builderContext.logException(ex)
            }
        }
        return exportFile
    }


    private fun addPlaylistContentToImage() {
        val messagesDir =
            File(File(System.getProperty("java.io.tmpdir"), builderContext.deploymentName), "messages")
//        val messagesDir = File(builderContext.stagedDeploymentDir, "messages")
        audioItems.forEach { audioItem ->
            println(String.format("    Exporting audioitem %s to %s%n", audioItem.acm_id, messagesDir))
            builderContext.reportStatus(
                String.format(
                    "    Exporting audioitem %s to %s%n",
                    audioItem.acm_id,
                    messagesDir
                )
            )

            val audioRef = ACMConfiguration.getInstance().currentDB
                .metadataStore.getAudioItem(audioItem.acm_id)
            val filename: String = repository.getAudioFilename(audioRef, audioFormat)

            // Export the audio file.
            val exportFile = File(messagesDir, filename)
            println(exportFile.path)
            if (!exportFile.exists()) {
                try {
                    repository.exportAudioFileWithFormat(audioRef, exportFile, audioFormat)
                } catch (ex: Exception) {
                    builderContext.logException(ex)
                }
            }
            // Add audio item to the package_data.txt.
//            val exportPath = makePath(File(messagesDir, filename))
//            playlistData.addMessage(audioItem.title, exportPath)
        }
    }

    private fun createDirs(dir: String): File {
        val f = File(dir)
        if (!f.exists()) {
            f.mkdirs()
        }
        return f
    }

    private fun createDirs(f: File): File {
        if (!f.exists()) {
            f.mkdirs()
        }
        return f
    }
}

