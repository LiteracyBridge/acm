package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;

import java.io.File;

public class GreetingFile extends ImportableFile {

    public GreetingFile(File file) {
        super(file);
    }

    @Override
    public String getTitle() {
        return toString();
    }

    @Override
    public String toString() {
        if (getFile() == null) {
            return "(null)";
        }
        // If this is .../{community-group-agent}/languages/{language}/10.a18, return
        // {community-group-agent} as the string.
        String title = FilenameUtils.getBaseName(getFile().getName());
        if (title.equals("10")) {
            File maybeLanguage = getFile().getParentFile();
            File maybeFiller = maybeLanguage!=null ? maybeLanguage.getParentFile() : null;
            File maybeCommunity = maybeFiller!=null ? maybeFiller.getParentFile() : null;
            if (maybeCommunity != null && maybeFiller.getName().equalsIgnoreCase("languages")) {
                title = maybeCommunity.getName();
            }
        }
        return title;
    }
}
