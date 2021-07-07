package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.common.AbstractFilesPage;
import org.literacybridge.acm.gui.assistants.common.AbstractMatchPage;
import org.literacybridge.acm.gui.assistants.common.AssistantContext;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class PromptImportContext implements AssistantContext, AbstractFilesPage.FileImportContext, AbstractMatchPage.MatchContext<PromptTarget, ImportableFile, PromptMatchable> {
    public Set<String> specLanguagecodes;
    public Set<String> configLanguagecodes;

    // What are the prompts?  [0, 1, ...] and { 0: "bell", 1: "Begin speaking...", ...}
    public PromptsInfo promptsInfo;

    public String languagecode;

    // Which prompts have recordings?
    public Map<String, Boolean> promptHasRecording = new LinkedHashMap<>();

    // From the Files page

    /**
     * The files that the user selected to import.
     */
    final Set<File> importableRoots = new LinkedHashSet<>();
    final Set<File> importableFiles = new LinkedHashSet<>();
    public File tbLoadersDir;
    public File tbOptionsDir;
    public File languagesDir;
    public File languageDir;
    public File promptsDir;

    // Accessors to satisfy FileImportContext.
    @Override
    public Set<File> getImportableRoots() {
        return importableRoots;
    }
    @Override
    public Set<File> getImportableFiles() {
        return importableFiles;
    }

    PromptsMatcher matcher = new PromptsMatcher();
    @Override
    public PromptsMatcher getMatcher() {
        return matcher;
    }

}
