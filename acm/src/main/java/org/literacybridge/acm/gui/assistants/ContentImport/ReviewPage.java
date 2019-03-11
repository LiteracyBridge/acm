package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableAudioItem;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.store.Playlist;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class ReviewPage extends AssistantPage<ContentImportContext> {

    private final JLabel importPreviewLabel;
    private final DefaultListModel<String> importPreviewModel;
    private final JScrollPane importPreviewScroller;
    private ContentImportContext context;

    public ReviewPage(PageHelper listener) {
        super(listener);
        context = getContext();
        setLayout(new GridBagLayout());

        Insets insets = new Insets(0,0,20,0);
        GridBagConstraints gbc = new GridBagConstraints(0,
            GridBagConstraints.RELATIVE,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL,
            insets,
            1,
            1);

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Review & Import</span>"
                + "<br/>When you are satisfied with these imports, click \"Finish\" to perform the import. "

                + "</html>");
        add(welcome, gbc);

        // Title preview.
        importPreviewLabel = new JLabel("Files to be imported:");
        insets = new Insets(0,0,00,0);
        gbc.insets = insets;
        add(importPreviewLabel, gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0,0));
        importPreviewModel = new DefaultListModel<>();
        JList<String> importPreview = new JList<>(importPreviewModel);
        importPreviewScroller = new JScrollPane(importPreview);
        panel.add(importPreviewScroller, BorderLayout.CENTER);
        gbc.ipadx = 10;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        insets = new Insets(0,10,20,30);
        gbc.insets = insets;
        add(panel, gbc);

        // Absorb any vertical space.
        //gbc.weighty = 1.0;
        //add(new JLabel(), gbc);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        Matcher<ImportableAudioItem, ImportableFile, MatchableImportableAudio> matcher = context.matcher;
        // For the imports, create a "item from \n file" label, and add to the preview.
        matcher.matchableItems.stream()
            .filter(i->i.getMatch().isMatch())
            .filter(i->!i.getLeft().hasAudioItem())
            .map(i->"<html>" +i.getLeft() + "&nbsp;&nbsp;&lt;--- <i>import from</i>"
                + "<br/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span style='font-family:Courier'>"
                + i.getRight().getFile().getName()
                + "</span></html>")
            .forEach(importPreviewModel::addElement);

        setComplete();
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
    }

    @Override
    protected String getTitle() {
        return "Review Files to Import";
    }

}
