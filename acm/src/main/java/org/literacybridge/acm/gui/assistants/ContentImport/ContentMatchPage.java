package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Deployment.PlaylistPrompts;
import org.literacybridge.acm.gui.assistants.Matcher.AbstractMatchTableModel;
import org.literacybridge.acm.gui.assistants.Matcher.ColumnProvider;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchTableRenderers;
import org.literacybridge.acm.gui.assistants.common.AbstractMatchPage;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.acm.gui.assistants.util.AudioUtils;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

//public class ContentMatchPageOld extends ContentImportBase<ContentImportContext> {
public class ContentMatchPage extends
                                    AbstractMatchPage<ContentImportContext, AudioTarget, ImportableFile, AudioMatchable> {

    private ContentImportBase.ImportReminderLine importReminderLine;

    static {
        // Configure the renderers for the match table.
        MatchTableRenderers.colorCodeMatches = false;
        MatchTableRenderers.isColorCoded = true;
    }

    ContentMatchPage(PageHelper<ContentImportContext> listener) {
        super(listener);

        if (context.fuzzyThreshold == null) {
            context.fuzzyThreshold = Constants.FUZZY_THRESHOLD_DEFAULT;
        } else {
            context.fuzzyThreshold = Integer.max(Constants.FUZZY_THRESHOLD_MINIMUM, Integer.min(
                Constants.FUZZY_THRESHOLD_MAXIMUM, context.fuzzyThreshold));
        }

        table.moveColumn(tableModel.getFileColumnNo(), 1);
    }


    @Override
    protected Component getWelcome() {
        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2em'>Match Files with Content.</span>"
                + "<br/><br/><p>The Assistant has automatically matched as many files as possible with content. "
                + "Only high-confidence matches are performed, so you may need to match some files manually. "
                + "Match or unmatch files as needed, and then click \"Next\" to continue.</p>"
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
        return "Unmatch the title from the file.";
    }

    @Override
    protected String getMatchTooltip() {
        return "Manually match this file with a title.";
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
        MatchTableRenderers<?> mtr = new MatchTableRenderers<>(table, tableModel);

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
        ManualMatcherDialog dialog = new ManualMatcherDialog(this, selectedRow, context.matcher.matchableItems);
        return dialog.getSelectedItem();
    }


    @Override
    protected void onPageEntered(boolean progressing) {
        String languagecode = context.languagecode;

        importReminderLine.getDeployment().setText(Integer.toString(context.deploymentNo));
        importReminderLine.getLanguage().setText(AcmAssistantPage.getLanguageAndName(context.languagecode));

        int deploymentNo = context.deploymentNo;

        // List of titles (left side list)
        List<AudioTarget> titles = new ArrayList<>();
        List<ContentSpec.PlaylistSpec> contentPlaylistSpecs = context.getProgramSpec().getContentSpec()
                                                                                 .getDeployment(deploymentNo)
                                                                                 .getPlaylistSpecsForLanguage(context.languagecode);

        for (ContentSpec.PlaylistSpec contentPlaylistSpec : contentPlaylistSpecs) {
            String playlistName = AudioUtils.decoratedPlaylistName(contentPlaylistSpec.getPlaylistTitle(), deploymentNo, languagecode);
            Playlist playlist = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore().findPlaylistByName(playlistName);

            // If this isn't the special "Intro Message" playlist, add playlist prompts to the list of items that may be imported.
            if (!context.introMessageCategoryName.equalsIgnoreCase(contentPlaylistSpec.getPlaylistTitle())) {
                PlaylistPrompts prompts = context.playlistPromptsMap.get(contentPlaylistSpec.getPlaylistTitle());
                AudioPlaylistTarget plItem = new AudioPlaylistTarget(contentPlaylistSpec,
                    false,
                    playlist);
                if (prompts.hasShortPrompt()) {
                    if (prompts.getShortItem() != null) {
                        plItem.setItem(prompts.getShortItem());
                    } else {
                        plItem.setFile(prompts.getShortFile());
                    }
                }
                titles.add(plItem);

                plItem = new AudioPlaylistTarget(contentPlaylistSpec, true, playlist);
                if (prompts.hasLongPrompt()) {
                    if (prompts.getLongItem() != null) {
                        plItem.setItem(prompts.getLongItem());
                    } else {
                        plItem.setFile(prompts.getLongFile());
                    }
                }
                titles.add(plItem);
            }

            if (!context.promptsOnly) {
                for (ContentSpec.MessageSpec messageSpec : contentPlaylistSpec.getMessagesForLanguage(languagecode)) {
                    AudioTarget importableAudio = new AudioMessageTarget(messageSpec, playlist);
                    // See if we already have this title in the ACM.
                    AudioItem audioItem = ContentImportBase.findAudioItemForTitle(messageSpec.title, languagecode);
                    importableAudio.setItem(audioItem);
                    titles.add(importableAudio);
                }
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

    public static class AudioTargetColumnProvider implements ColumnProvider<AudioTarget> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return "Title";
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return AudioTarget.class;
        }

        @Override
        public Object getValueAt(AudioTarget target, int columnIndex) {
            return target;
        }
    }

}
