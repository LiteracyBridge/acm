package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.assistants.Matcher.MATCH;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ManualMatchDialog {
    private final Color bgColor;
    private final Color bgSelectionColor;
    private final Color bgAlternateColor;
    private final JList<String> choicesList;
    private final JLabel promptLabel;
    private JButton okButton, cancelButton;
    private JDialog dialog;
    private final JOptionPane optionPane;
    private final Map<String, MatchableImportableAudio> unmatchedImportables;

    ManualMatchDialog() {
        promptLabel = new JLabel("");

        unmatchedImportables = new LinkedHashMap<>();
        choicesList = new JList<>();
        choicesList.addListSelectionListener(this::listSelectionListener);
        choicesList.addMouseListener(listMouseListener);

        setupButtons();
        optionPane = new JOptionPane();
        optionPane.setOptions(new Object[] { okButton, cancelButton });
        dialog = optionPane.createDialog("");
        
        bgColor = Color.white; // table.getBackground();
        bgSelectionColor = choicesList.getSelectionBackground();
        bgAlternateColor = new Color(235, 245, 252);
    }

    MatchableImportableAudio getFileForAudioItem(MatchableImportableAudio row,
        List<MatchableImportableAudio> matchableItems) {

        matchableItems
            .stream()
            .filter(item->item.getMatch()== MATCH.RIGHT_ONLY)
            .forEach(item-> unmatchedImportables.put(item.getRight().getFile().getName(), item));
        String[] unmatchedTitles = unmatchedImportables
            .keySet().toArray(new String[0]);

        choicesList.setListData(unmatchedTitles);
         /* bad */
        String message = String.format("<html>Choose the File for Audio Item <br/>&nbsp;&nbsp;<i>%s</i>.</html>", row.getLeft().getTitle());
        promptLabel.setText(message);

        JPanel pane = layoutComponents();
        optionPane.setMessage(pane);
        dialog = optionPane.createDialog("Choose File for Audio Item");

        return show();
    }

    MatchableImportableAudio getAudioItemForFile(MatchableImportableAudio row,
        List<MatchableImportableAudio> matchableItems) {

        matchableItems
            .stream()
            .filter(item->item.getMatch()== MATCH.LEFT_ONLY)
            .forEach(item-> unmatchedImportables.put(item.getLeft().getTitle(), item));
        String[] unmatchedTitles = unmatchedImportables
            .keySet().toArray(new String[0]);

        choicesList.setListData(unmatchedTitles);
        /* good */
        String message = String.format("<html>Choose the Audio item for file<br/>&nbsp;&nbsp;<i>%s</i>.</html>", row.getRight().getFile().getName());
        promptLabel.setText(message);

        JPanel pane = layoutComponents();
        optionPane.setMessage(pane);
        dialog = optionPane.createDialog("Choose Audio Item For File");

        return show();
    }

    private MouseListener listMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) dialog.setVisible(false);
        }
    };

    private void setupButtons() {
        okButton = new JButton("Ok");
        okButton.addActionListener(e -> dialog.setVisible(false));
        okButton.setEnabled(false);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            choicesList.clearSelection();
            dialog.setVisible(false);
        });
    }

    private JPanel layoutComponents() {
        choicesList.setCellRenderer(listCellRenderer);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(promptLabel, BorderLayout.NORTH);
        panel.add(choicesList, BorderLayout.CENTER);
        choicesList.setBorder(new LineBorder(Color.red, 1));
        return panel;
    }

    private void listSelectionListener(ListSelectionEvent listSelectionEvent) {
        okButton.setEnabled(choicesList.getLeadSelectionIndex() >= 0);
    }

    private ListCellRenderer listCellRenderer = new DefaultListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (isSelected)
                comp.setBackground(bgSelectionColor);
            else
                comp.setBackground(index%2==0 ? bgColor : bgAlternateColor);
            return comp;
        }
    };

    public MatchableImportableAudio show() {
        dialog.setVisible(true);
        return getSelectedItem();
    }
    public MatchableImportableAudio getSelectedItem() {
        String chosenTitle = choicesList.getSelectedValue();
        if (chosenTitle != null) {
            return unmatchedImportables.get(chosenTitle);
        }
        return null;
    }

}

