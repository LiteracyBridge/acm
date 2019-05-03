package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MATCH;
import org.literacybridge.acm.gui.assistants.Matcher.MatchTableRenderers;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;
import org.literacybridge.acm.gui.assistants.Matcher.MatcherTableTransferHandler;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.core.spec.ContentSpec;

import javax.swing.*;
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

public class ContentMatchPage2 extends ContentImportBase<ContentImportContext> {
    private static final int MAXIMUM_THRESHOLD = 100;
    private static final int DEFAULT_THRESHOLD = 80;
    private static final int MINIMUM_THRESHOLD = 60;

    private final ImportReminderLine importReminderLine;

    private MatcherTable table;
    private MatcherTableModel model;
    private JButton unMatch;
    private JButton manualMatch;

    static {
        // Configure the renderers for the match table.
        MatchTableRenderers.colorCodeMatches = false;
        MatchTableRenderers.isColorCoded = true;
    }

    ContentMatchPage2(PageHelper<ContentImportContext> listener) {
        super(listener);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();
        gbc.insets.bottom = 5;

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2em'>Match Files with Content.</span>"
                + "<br/><br/><p>The Assistant has automatically matched files as possible with content. "
                + "Only high-confidence matches are performed, so manual matching may be required. "
                + "Perform any additional matching (or un-matching) as required, then click \"Next\" to continue.</p>"
                + "</html>");
        add(welcome, gbc);

        importReminderLine = new ImportReminderLine();
        add(importReminderLine.getLine(), gbc);

        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(makeTable(), gbc);
        gbc.weighty = 0;

        gbc.insets.bottom = 0;
        add(makeManualMatchButtons(), gbc);

        if (context.fuzzyThreshold == null) {
            context.fuzzyThreshold = DEFAULT_THRESHOLD;
        } else {
            context.fuzzyThreshold = Integer.max(MINIMUM_THRESHOLD, Integer.min(MAXIMUM_THRESHOLD, context.fuzzyThreshold));
        }
    }

    /**
     * Creates the table and sets the model and various renderers, filters, etc.
     */
    private Component makeTable() {
        table = new MatcherTable();
        model = table.getModel();

        table.setFilter(this::filter);

        MatchTableRenderers mtr = new MatchTableRenderers(table, model);
        table.setRenderer(MatcherTableModel.Columns.Left, mtr.getAudioItemRenderer());
        table.setRenderer(MatcherTableModel.Columns.Update, mtr.getUpdatableBooleanRenderer());
        table.setRenderer(MatcherTableModel.Columns.Status, mtr.getStatusRenderer());
        table.setRenderer(MatcherTableModel.Columns.Right, mtr.getMatcherRenderer());

        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);
        table.setGridColor(new Color(224, 224, 224));
        table.setGridColor(Color.RED);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);

        table.setDragEnabled(true);
        table.setDropMode(DropMode.ON);

        TransferHandler matchTableTransferHandler = new MatcherTableTransferHandler<AudioMatchable> (table, model) {
            public void onMatched(AudioMatchable sourceRow, AudioMatchable targetRow) {
                context.matcher.setMatch(sourceRow, targetRow);
                model.fireTableDataChanged();
            }
        };
        table.setTransferHandler(matchTableTransferHandler);

        table.addMouseListener(tableMouseListener);

        return scrollPane;
    }

    /**
     * The filter for showing and hiding matched columns.
     *
     * @param t   The item to be filtered.
     * @param <T> Type of the item, some subclass of MatchableItem.
     * @return true if the item shoudl be shown.
     */
    private <T extends MatchableItem<?, ?>> boolean filter(T t) {
        MATCH match = t.getMatch();
        return match != MATCH.NONE;
    }

    private Box makeManualMatchButtons() {
        // Control buttons
        Box hbox = Box.createHorizontalBox();
        unMatch = new JButton("Unmatch");
        unMatch.addActionListener(this::onUnMatch);
        unMatch.setToolTipText("Unmatch the Audio Item from the File.");
        hbox.add(unMatch);
        hbox.add(Box.createHorizontalStrut(5));
        manualMatch = new JButton("Manual Match");
        manualMatch.addActionListener(this::onManualMatch);
        manualMatch.setToolTipText("Manually match this file with an message.");
        hbox.add(manualMatch);
        hbox.add(Box.createHorizontalStrut(5));

        unMatch.setEnabled(false);
        manualMatch.setEnabled(false);

        hbox.add(Box.createHorizontalGlue());

        table.getSelectionModel().addListSelectionListener(ev -> enableButtons());

        return hbox;
    }

    private MouseListener tableMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                AudioMatchable selectedRow = selectedRow();
                if (selectedRow != null && selectedRow.getMatch().isUnmatched()) {
                    onManualMatch(null);
                }
            }
        }
    };

    private void onUnMatch(ActionEvent actionEvent) {
        int viewRow = table.getSelectionModel().getLeadSelectionIndex();
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            AudioMatchable row = model.getRowAt(modelRow);
            if (row != null && row.getMatch().isMatch()) {
                context.matcher.unMatch(modelRow);
                model.fireTableDataChanged();
            }
        }
    }

    private void onManualMatch(ActionEvent actionEvent) {
        AudioMatchable selectedRow = selectedRow();
        AudioMatchable chosenMatch = null;

        ManualMatcherDialog dialog = new ManualMatcherDialog();
        if (selectedRow.getMatch().isUnmatched()) {
            chosenMatch = dialog.chooseMatchFor(selectedRow, context.matcher.matchableItems);
        }

        if (chosenMatch != null) {
            context.matcher.setMatch(chosenMatch, selectedRow);
            model.fireTableDataChanged();
        }
    }

    private void enableButtons() {
        AudioMatchable row = selectedRow();
        unMatch.setEnabled(row != null && row.getMatch().isMatch());
        manualMatch.setEnabled(row != null && !row.getMatch().isMatch());
    }
    
    private AudioMatchable selectedRow() {
        AudioMatchable row = null;
        int viewRow = table.getSelectionModel().getLeadSelectionIndex();
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            row = model.getRowAt(modelRow);
        }
        return row;
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
                AudioTarget importableAudio = new AudioTarget(messageSpec, playlist);
                // See if we already have this title in the ACM.
                AudioItem audioItem = findAudioItemForTitle(messageSpec.title, languagecode);
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
            model.setData(context.matcher.matchableItems);
            context.matcher.autoMatch(context.fuzzyThreshold);
            context.matcher.sortByProgramSpecification();
        }
        model.fireTableDataChanged();

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
}
