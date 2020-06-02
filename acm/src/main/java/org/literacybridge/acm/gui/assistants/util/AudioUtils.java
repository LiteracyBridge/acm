package org.literacybridge.acm.gui.assistants.util;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.audioconverter.api.AudioConversionFormat;
import org.literacybridge.acm.audioconverter.api.ExternalConverter;
import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.importexport.AudioImporter;
import org.literacybridge.acm.repository.A18Utils;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.Category;
import org.literacybridge.acm.store.Metadata;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.literacybridge.acm.store.MetadataSpecification.DC_IDENTIFIER;
import static org.literacybridge.acm.store.MetadataSpecification.DC_LANGUAGE;
import static org.literacybridge.acm.store.MetadataSpecification.DC_TITLE;

public class AudioUtils {

    private static Pattern playlistPattern = Pattern.compile("(\\d+)-(.*)-(\\w+)");

    /**
     * Copies the given fromFile as the toFile, converting to A18 format if the file is not already an A18.
     *
     * THIS IS ONLY FOR NON-AUDIO-REPOSITORY FILES. CONTENT SHOULD USE THE REPOSITORY API.
     *
     * @param title The title to be added as metadata. (Why? We don't keep the metadata.)
     * @param languagecode The language code to add as metadat. (Why?)
     * @param fromFile File to copy from. May be any format.
     * @param toFile File to copy to.
     * @throws BaseAudioConverter.ConversionException
     * @throws IOException
     */
    public static void copyOrConvert(String title, String languagecode, File fromFile, File toFile)
        throws BaseAudioConverter.ConversionException, IOException
    {
        Metadata metadata = AudioImporter.getInstance().getExistingMetadata(fromFile);
        // Don't expect to usually find existing metadata.
        if (metadata == null) {
            metadata = new Metadata();
        }
        Collection<Category> categories = new ArrayList<>();
        // get a new id, even if the object already had one.
        String id = ACMConfiguration.getInstance().getNewAudioItemUID();
        metadata.put(DC_IDENTIFIER, id);
        metadata.put(DC_LANGUAGE, languagecode);
        metadata.put(DC_TITLE, title);
        Category communities = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().getCategory(Constants.CATEGORY_COMMUNITIES);
        categories.add(communities);

        if (FilenameUtils.getExtension(fromFile.getName()).equalsIgnoreCase(AudioItemRepository.AudioFormat.A18.getFileExtension())) {
            A18Utils.copyA18WithoutMetadata(fromFile, toFile);
        } else {
            AudioConversionFormat audioConversionFormat = AudioItemRepository.AudioFormat.A18.getAudioConversionFormat();
            new ExternalConverter().convert(fromFile, toFile, audioConversionFormat, true);
        }

        A18Utils.appendMetadataToA18(metadata, categories, toFile);
    }

    /**
     * Find the index at which a message should be placed in the ACM playlist.
     *
     * Given a message title (from the program spec) and an ACM playlist, find the target
     * index as follows:
     * - Get a list of the titles already in the ACM playlist.
     * - Get a list of the titles in the program spec playlist. Find the given message in that
     *   list of titles.
     * - Take the immediately preceding title from the program spec playlist, and see if that
     *   title is in the ACM playlist. If it is, put the new message immediately after that
     *   existing message.
     * - If the immediately preceding title is not found, take the immediately following title
     *   from the program spec playlist, and look for that in the ACM playlist. If it is found,
     *   put the new message immediately before that existing message.
     * - If neither of the immediate neighbors was found, try the next closest previous title,
     *   and if it is not found, try the next closest following title.
     * - Continue until a neighbor (however distant) is found in the ACM playlist, or until there
     *   are no further program spec titles for which to look. If no neighbor is found, put the
     *   new title at index 0, the first item in the playlist.
     *
     * @param message The MessageSpec for which we want the proper index in the Playlist.
     * @param playlist The Playlist in which we want the index.
     * @param languagecode The Playlist's (assumed) language. (Language is not a formal property of playlist.)
     * @return the index, or 0 if an index can't be determined.
     */
    public static int findIndexForMessageInPlaylist(ContentSpec.MessageSpec message, Playlist playlist, String languagecode) {
        MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();
        String title = message.getTitle();
        // Get the list of titles of items already in the ACM playlist.
        List<String> acmTitles = playlist
            .getAudioItemList()
            .stream()
            .map(id -> store.getAudioItem(id).getTitle())
            .collect(Collectors.toList());
        // Get the list of titles as specified by the Program Specification.
        List<String> specTitles = message
            .getPlaylist()
            .getMessagesForLanguage(languagecode)
            .stream()
            .map(ContentSpec.MessageSpec::getTitle)
            .collect(Collectors.toList());
        int specIx = specTitles.indexOf(title);
        // Start with immediate previous sibling, then immediate next sibling, then -2, then +2, ...
        // try to find a sibling already in the ACM playlist. Put this message after or before
        // that found message.
        int offset = -1;
        int maxDistance = Math.max(specIx, specTitles.size()-1-specIx);
        while (Math.abs(offset) <= maxDistance) {
            // Next index of a progspec title to look for.
            int testIx = specIx + offset;
            // If a valid progspec index, see if there's an acm item of that title.
            int acmIx = testIx>=0&&testIx<specTitles.size() ? acmTitles.indexOf(specTitles.get(testIx)) : -1;
            // If there's an acm item, put this new title after or before, depending on which way we were looking.
            if (acmIx >= 0) {
                return acmIx + (offset<0 ? 1 : 0);
            }
            offset = (offset<0) ? -offset : -offset-1;
        }

        // Didn't find any neighbors. Put this at the beginning. Next item, for which this will be
        // a neighbor, will get placed appropriately after or before this one.
        return 0;
    }

    // TODO: Under construction
//    /**
//     * Brings a playlist into better conformance with the program specification. If messages are
//     * missing from the playlist, but exist in the ACM, adds them. If any messages are out of order
//     * wrt the spec, re-orders them.
//     * @param playlist from the ACM.
//     * @param playlistSpec to which the playlist should conform.
//     */
//    public static void reorderPlaylistToSpecification(Playlist playlist, ContentSpec.PlaylistSpec playlistSpec) {
//        String languageCode = undecoratedPlaylistLanguagecode(playlist.getName());
//        List<ContentSpec.MessageSpec> messageList = playlistSpec.getMessagesForLanguage(languageCode);
//        for (ContentSpec.MessageSpec messageSpec : messageList) {
//
//        }
//
//    }

    /**
     * Given a playlist title, a deployment, and a language, build the decorated playlist name,
     * like 1-Health-swh
     * @param title of the playlist
     * @param deploymentNo of the deployment
     * @param languagecode of the playlist
     * @return the decorated name.
     */
    public static String decoratedPlaylistName(String title, int deploymentNo, String languagecode) {
        title = normalizePlaylistTitle(title);
        return String.format("%d-%s-%s", deploymentNo, title, languagecode);
    }

    /**
     * Given a decorated playlist name, strip off the deployment and language, and return just
     * the name. Underscores are converted (back) to spaces.
     * @param decoratedName to be un-decorated.
     * @return the un-decorated name.
     */
    public static String undecoratedPlaylistName(String decoratedName) {
        Matcher matcher = playlistPattern.matcher(decoratedName);
        if (matcher.matches() && matcher.groupCount()==3) {
            return matcher.group(2).replaceAll("_", " ");
        }
        return decoratedName;
    }

    public static int undecoratedPlaylistDeployment(String decoratedName) {
        Matcher matcher = playlistPattern.matcher(decoratedName);
        if (matcher.matches() && matcher.groupCount()==3) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }

    public static String undecoratedPlaylistLanguagecode(String decoratedName) {
        Matcher matcher = playlistPattern.matcher(decoratedName);
        if (matcher.matches() && matcher.groupCount()==3) {
            return matcher.group(3).replaceAll("_", " ");
        }
        return null;
    }

    /**
     * Given a playlist title, trim leading and trailing spaces, and replace remaining spaces
     * with underscores.
     * @param title, possibly with spaces.
     * @return title without spaces.
     */
    private static String normalizePlaylistTitle(String title) {
        title = title.trim().replaceAll(" ", "_");
        return title;
    }
}
