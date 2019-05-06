package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.GreetingsImport.GreetingMatchable;
import org.literacybridge.acm.gui.assistants.GreetingsImport.GreetingTarget;
import org.literacybridge.acm.gui.assistants.GreetingsImport.GreetingsImportContext;
import org.literacybridge.acm.gui.assistants.GreetingsImport.GreetingsMatchPage;
import org.literacybridge.acm.gui.assistants.Matcher.AbstractMatchTableModel;
import org.literacybridge.acm.gui.assistants.Matcher.ColumnProvider;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MATCH;
import org.literacybridge.acm.gui.assistants.Matcher.MatchTableRenderers;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.acm.gui.assistants.Matcher.MatcherTableTransferHandler;
import org.literacybridge.acm.gui.assistants.SystemPromptsImport.PromptTarget;
import org.literacybridge.acm.gui.assistants.common.AbstractMatchPage;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

//public class ContentMatchPage extends ContentImportBase<ContentImportContext> {
public class ContentMatchPageNew extends
                                    AbstractMatchPage<ContentImportContext, AudioTarget, ImportableFile, AudioMatchable> {

    private static final int MAXIMUM_THRESHOLD = 100;
    private static final int DEFAULT_THRESHOLD = 80;
    private static final int MINIMUM_THRESHOLD = 60;

    private ContentImportBase.ImportReminderLine importReminderLine;

    static {
        // Configure the renderers for the match table.
        MatchTableRenderers.colorCodeMatches = false;
        MatchTableRenderers.isColorCoded = true;
    }

    ContentMatchPageNew(PageHelper<ContentImportContext> listener) {
        super(listener);

        if (context.fuzzyThreshold == null) {
            context.fuzzyThreshold = DEFAULT_THRESHOLD;
        } else {
            context.fuzzyThreshold = Integer.max(MINIMUM_THRESHOLD, Integer.min(MAXIMUM_THRESHOLD, context.fuzzyThreshold));
        }

        table.moveColumn(tableModel.getFileColumnNo(), 1);
    }


    @Override
    protected Component getWelcome() {
        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2em'>Match Files with Content.</span>"
                + "<br/><br/><p>The Assistant has automatically matched files as possible with content. "
                + "Only high-confidence matches are performed, so manual matching may be required. "
                + "Perform any additional matching (or un-matching) as required, then click \"Next\" to continue.</p>"
                + "</html>");
        importReminderLine = new ContentImportBase.ImportReminderLine();

        Box vbox = Box.createVerticalBox();
        Box hbox = Box.createHorizontalBox();
        hbox.add(welcome);
        hbox.add(Box.createHorizontalGlue());
        vbox.add(hbox);

        hbox = Box.createHorizontalBox();
        hbox.add(importReminderLine.getLine());
        hbox.add(Box.createHorizontalGlue());
        vbox.add(hbox);
        return vbox;
    }

    @Override
    protected String getFilterSwitchPrompt() {
        return null;
    }

    @Override
    protected String getUnmatchTooltip() {
        return "Unmatch the Audio Item from the File.";
    }

    @Override
    protected String getMatchTooltip() {
        return "Manually match this file with an message.";
    }

    @Override
    protected AbstractMatchTableModel<AudioTarget, AudioMatchable> getModel() {
        return new AudioMatchModel();
    }

    @Override
    protected RowFilter<AbstractMatchTableModel<AudioTarget, AudioMatchable>, Integer> getFilter() {
        return null;
    }

    @Override
    protected void setCellRenderers() {
        MatchTableRenderers mtr = new MatchTableRenderers(table, tableModel);

        TableColumn columnModel = table.getColumnModel().getColumn(0);
        columnModel.setCellRenderer(mtr.getAudioItemRenderer());

        columnModel = table.getColumnModel().getColumn(tableModel.getReplaceColumnNo());
        columnModel.setCellRenderer(mtr.getUpdatableBooleanRenderer());

        columnModel = table.getColumnModel().getColumn(tableModel.getStatusColumnNo());
        columnModel.setCellRenderer(mtr.getStatusRenderer());

        columnModel = table.getColumnModel().getColumn(tableModel.getFileColumnNo());
        columnModel.setCellRenderer(mtr.getMatcherRenderer());

    }

    @Override
    protected void sizeColumns() {
        // Set columns 1 & 2 width (Update? and Status) on header & values.
        AssistantPage.sizeColumns(table, tableModel.getReplaceColumnNo(), tableModel.getStatusColumnNo());
    }

    @Override
    protected void updateFilter() {

    }

    @Override
    protected AudioMatchable onManualMatch(AudioMatchable selectedRow) {
        ManualMatcherDialog dialog = new ManualMatcherDialog();
        return dialog.chooseMatchFor(selectedRow, context.matcher.matchableItems);
    }


    @Override
    protected void onPageEntered(boolean progressing) {
        String languagecode = context.languagecode;

        importReminderLine.getDeployment().setText(Integer.toString(context.deploymentNo));
        importReminderLine.getLanguage().setText(languagecode);

        int deploymentNo = context.deploymentNo;

        // List of titles (left side list)
        List<AudioTarget> titles = new ArrayList<>();
        List<ContentSpec.PlaylistSpec> contentPlaylistSpecs = context.programSpec.getContentSpec()
                                                                                 .getDeployment(deploymentNo)
                                                                                 .getPlaylistSpecs();
        for (ContentSpec.PlaylistSpec contentPlaylistSpec : contentPlaylistSpecs) {
            String playlistName = WelcomePage.decoratedPlaylistName(contentPlaylistSpec.getPlaylistTitle(), deploymentNo, languagecode);
            Playlist playlist = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().findPlaylistByName(playlistName);
            for (ContentSpec.MessageSpec messageSpec : contentPlaylistSpec.getMessagesForLanguage(languagecode)) {
                AudioTarget importableAudio = new AudioMessageTarget(messageSpec, playlist);
                // See if we already have this title in the ACM.
                AudioItem audioItem = ContentImportBase.findAudioItemForTitle(messageSpec.title, languagecode);
                importableAudio.setItem(audioItem);
                titles.add(importableAudio);
            }
        }
        // List of files (right side list)
        List<ImportableFile> files = context.importableFiles.stream()
            .map(ImportableFile::new)
            .collect(Collectors.toList());

        if (progressing) {
            context.matcher.setData(titles, files, AudioMatchable::new);
//            tableModel.setData(context.matcher.matchableItems);
            context.matcher.autoMatch(context.fuzzyThreshold);
            context.matcher.sortByProgramSpecification();
        }
        tableModel.fireTableDataChanged();
        sizeColumns();

        setComplete();
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
        if (progressing) {
            context.matcher.sortByProgramSpecification();
        }
    }

    @Override
    protected String getTitle() {
        return "Auto-match Files with Content";
    }

    class AudioMatchModel extends AbstractMatchTableModel<AudioTarget, AudioMatchable> {
        AudioMatchModel() {
            super(new AudioTargetColumnProvider());
        }

        @Override
        public int getRowCount() {
            return context.matcher.matchableItems.size();
        }

        @Override
        public AudioMatchable getRowAt(int rowIndex) {
            return context.matcher.matchableItems.get(rowIndex);
        }
    }

    public class AudioTargetColumnProvider implements ColumnProvider<AudioTarget> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return "Audio Item";
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public Object getValueAt(AudioTarget target, int columnIndex) {
            return target;
        }
    }

}
