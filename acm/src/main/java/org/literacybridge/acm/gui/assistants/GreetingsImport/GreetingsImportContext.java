package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.gui.assistants.Matcher.GreetingsMatcher;
import org.literacybridge.acm.gui.assistants.common.FilesPage;
import org.literacybridge.core.spec.ProgramSpec;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class GreetingsImportContext implements FilesPage.FileImportContext {
    public ProgramSpec programSpec;

    // Which recipients have custom greetings?
    public Map<String, Boolean> recipientHasRecording = new HashMap<>();

    // From the Files page

    /**
     * The files that the user selected to import.
     */
    final Set<File> importableRoots = new LinkedHashSet<>();
    final Set<File> importableFiles = new LinkedHashSet<>();

    // Accessors to satisfy FileImportContext.
    @Override
    public Set<File> getImportableRoots() {
        return importableRoots;
    }
    @Override
    public Set<File> getImportableFiles() {
        return importableFiles;
    }

    GreetingsMatcher matcher = new GreetingsMatcher();

}
