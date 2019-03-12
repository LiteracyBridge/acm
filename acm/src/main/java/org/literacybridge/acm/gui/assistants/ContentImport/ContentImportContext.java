package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.ImportableAudioItem;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.core.spec.Content;
import org.literacybridge.core.spec.ProgramSpec;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * This is the context for a content import. By the time the Assistant has finished, this will
 * contain all of the data for importing the audio tracks for one language in one deployment.
 */
public class ContentImportContext {

    // From the Welcome page

    /**
     * Program Spec for the project.
     */
    ProgramSpec programSpec;
    Set<String> programLanguagecodes;

    /**
     * The deployment for which audio is to be imported.
     */
    int deploymentNo = -1;
    /**
     * The language code for which audio is to be imported.
     */
    String languagecode;

    /**
     * The deployment from the Program Specification. Contains one or more playlists, each with
     * one or more messages.
     */
    Content.Deployment deploymentSpec;

    /**
     * The fully-decorated titles from the Program Specification. If an audio item already exists,
     * that will be recorded in the individual items. Also, each item carries the ACM Playlist
     * into which it should be imported.
     */
    List<ImportableAudioItem> importableAudioItems;

    // From the Files page

    /**
     * The files that the user selected to import.
     */
    Set<File> importableFiles;

    // From the Match page

    /**
     * The matcher object. Lets us know how titles were matched against files.
     */
    Matcher<ImportableAudioItem, ImportableFile, MatchableImportableAudio> matcher = new Matcher<>();

    /**
     * Created playlists, in the ACM.
     */
    Set<String> createdPlaylists = new LinkedHashSet<>();


}
