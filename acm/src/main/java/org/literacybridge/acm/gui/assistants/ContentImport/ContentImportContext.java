package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.ImportableAudioItem;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.core.spec.ProgramSpec;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This is the context for a content import. By the time the Assistant has finished, this will
 * contain all of the data for importing the audio tracks for one language in one deployment.
 */
class ContentImportContext {
    /**
     * Program Spec for the project.
     */
    ProgramSpec programSpec;

    // From the Welcome page

    /**
     * The deployment for which audio is to be imported.
     */
    int deploymentNo = -1;
    /**
     * The language code for which audio is to be imported.
     */
    String languagecode;

    // From the Files page

    /**
     * The files that the user selected to import.
     */
    Set<File> importableRoots = new LinkedHashSet<>();
    Set<File> importableFiles = new LinkedHashSet<>();

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
