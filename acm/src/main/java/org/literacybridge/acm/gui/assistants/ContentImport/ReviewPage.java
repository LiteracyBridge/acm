package org.literacybridge.acm.gui.assistants.ContentImport;

import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.literacybridge.acm.gui.Assistant.Assistant;
import org.literacybridge.acm.gui.Assistant.LabelButton;
import org.literacybridge.acm.gui.assistants.ContentImport.ContentImportBase.ImportReminderLine;
import org.literacybridge.acm.gui.assistants.Matcher.ColumnProvider;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableFileItem;
import org.literacybridge.acm.gui.assistants.Matcher.Target;
import org.literacybridge.acm.gui.assistants.common.AbstractReviewPage;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.acm.gui.assistants.util.AudioUtils;
import org.literacybridge.acm.store.Playlist;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReviewPage extends AbstractReviewPage<ContentImportContext, AudioMatchable> {

    ReviewPage(Assistant.PageHelper<ContentImportContext> listener) {
        super(listener);
    }

    private ImportReminderLine importReminderLine;

    @Override
    protected List<JComponent> getPageIntro() {
        List<JComponent> components = new ArrayList<>();

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Review & Import</span>"
                + "<br/><br/>When you are satisfied with these imports, click \"Finish\" to import the content. "

                + "</html>");
        components.add(welcome);

        importReminderLine = new ImportReminderLine();
        components.add(importReminderLine.getLine());

        return components;
    }

    @Override
    protected ColumnProvider<AudioMatchable> getColumnProvider() {
        return new PromptTargetColumnProvider();
    }

    @Override
    protected ImportPreviewTreeTableModel getTreeModel(MutableTreeTableNode root) {
        return new ImportPreviewTreeTableModel<>(root, new PromptTargetColumnProvider());
    }

    @Override
    protected AbstractTreeTableRenderer getTreeTableRenderer(ImportPreviewTreeTableModel model)
    {
        return new ImportPreviewTreeTableRenderer(model);
    }

    @Override
    protected void fillTreeModel() {

        // For the imports, create a "item from \n file" label, and add to the preview.
        List<AudioMatchable> importables = context.matcher.matchableItems.stream()
            .filter(item -> item.getMatch().isMatch() && item.getLeft().isImportable())
            .collect(Collectors.toList());

        Map<Playlist, PlaylistNode> playlistNodes = new HashMap<>();
        for (AudioMatchable importable : importables) {
            Playlist playlist = importable.getLeft().getPlaylist();
            PlaylistNode playlistNode = playlistNodes.get(playlist);
            if (playlistNode == null) {
                playlistNode = new PlaylistNode(playlist);
                playlistNodes.put(playlist, playlistNode);
                importPreviewTreeTableModel.insertNodeInto(playlistNode,
                    importPreviewRoot,
                    importPreviewTreeTableModel.getRoot().getChildCount());
            }
            PreviewTargetNode<AudioMatchable> previewTargetNode = new PreviewTargetNode<>(importable);
            playlistNode.add(previewTargetNode);
            importPreviewTreeTableModel.insertNodeInto(previewTargetNode,
                playlistNode,
                playlistNode.getChildCount());
        }

    }

    @Override
    protected void onPageEntered(boolean progressing) {
        super.onPageEntered(progressing);

        // Fill deployment and language
        importReminderLine.getDeployment().setText(Integer.toString(context.deploymentNo));
        importReminderLine.getLanguage()
            .setText(AcmAssistantPage.getLanguageAndName(context.languagecode));

    }

    private class PlaylistNode extends DefaultMutableTreeTableNode {
        PlaylistNode(Playlist playlist) {
            super(playlist, true);
        }

        @Override
        public Object getValueAt(int column) {
            if (column == 0) return toString();
            return "";
        }

        @Override
        public String toString() {
            return AudioUtils.undecoratedPlaylistName(((Playlist) getUserObject()).getName());
        }
    }

    private class PromptTargetColumnProvider implements ColumnProvider<AudioMatchable> {

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return "Title";
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public Object getValueAt(AudioMatchable data, int columnIndex) {
            if (data == null) return null;
            AudioTarget target = data.getLeft();
            return target.getTitle();
        }
    }

    private class ImportPreviewTreeTableRenderer<U extends MatchableFileItem<? extends Target, ? extends ImportableFile>>
        extends AbstractReviewPage.ImportPreviewTreeTableRenderer {

        private final Font normalFont;
        private final Font italicFont;

        ImportPreviewTreeTableRenderer(ImportPreviewTreeTableModel<U> model) {
            //noinspection unchecked
            super(model);

            normalFont = getFont();
            italicFont = LabelButton.fontResource(LabelButton.AVENIR)
                .deriveFont((float) normalFont.getSize())
                .deriveFont(Font.ITALIC);
        }

        private AudioMatchable getMatchable(int row) {
            if (row >= 0) {
                TreePath path = importPreviewTreeTable.getPathForRow(row);
                if (path != null) {
                    Object node = path.getLastPathComponent();
                    if (node instanceof AbstractReviewPage.PreviewTargetNode) {
                        return (AudioMatchable) ((PreviewTargetNode) node).getMatchable();
                    }
                }
            }
            return null;
        }

        private void setFont(AudioMatchable matchable) {
            boolean italics = matchable != null && matchable.getLeft().isPlaylist();
            setFont(italics ? italicFont : normalFont);
        }

        private void setAudioTooltip(AudioMatchable matchable) {
            String tip = null;
            if (matchable != null) {
                if (matchable.getLeft().isPlaylist()) {
                    AudioPlaylistTarget plt = (AudioPlaylistTarget) matchable.getLeft();
                    String text = plt.getPlaylistSpec().getPlaylistTitle();
                    boolean longPrompt = ((AudioPlaylistTarget) matchable.getLeft()).isLong();
                    if (!longPrompt) {
                        tip = String.format("Title: '%s'", text);
                    } else {
                        tip = String.format("Invitation: 'To learn about %s, press the tree.'",
                            text);
                    }
                } else {
                    tip = String.format("Message: '%s'", matchable.getLeft().getTitle());
                }
            }
            setToolTipText(tip);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
        {
            AudioMatchable am = getMatchable(row);
            setFont(am);

            String tip = null;
            int modelColumn = table.convertColumnIndexToModel(column);
            if (am != null && model.isCenterColumn(modelColumn)) {
                String thing = am.getLeft().isPlaylist() ? "playlist prompt" : "message";
                // If we are here, we're going to copy a file. Either "Import New" or "Replace".
                if (am.getLeft().targetExists()) {
                    tip = "Replace existing " + thing + " with a new recording.";
                } else {
                    tip = "Import a new recording for the " + thing + ".";
                }
            }
            setToolTipText(tip);

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus)
        {
            AudioMatchable am = getMatchable(row);
            setFont(am);
            setAudioTooltip(am);

            if (value instanceof AbstractMutableTreeTableNode)
                value = ((AbstractMutableTreeTableNode) value).getValueAt(0);
            return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }

    }

}
