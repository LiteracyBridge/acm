package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.assistants.common.AbstractFilesPage;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PromptFilesPage extends AbstractFilesPage<PromptImportContext> {
    PromptFilesPage(PageHelper<PromptImportContext> listener) {
        super(listener);
    }

    protected List<JComponent> getPageIntro() {
        List<JComponent> components = new ArrayList<>();
        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2em'>Choose Files to Import.</span>"
                + "<br/><br/><p>Choose the files containing the system prompts that you wish to import.</p>" + "</html>");
        components.add(welcome);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Importing System Prompts."));
        hbox.add(Box.createHorizontalGlue());
        components.add(hbox);

        return components;
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        super.onPageEntered(progressing);

        if (ACMConfiguration.isTestData() && progressing && context.importableRoots.size()==0) {
            context.importableRoots.addAll(Collections.singletonList(new File("/Users/bill/A-communities")));
        }

    }

    @Override
    protected String getTitle() {
        return "Prompts to Import";
    }

}
