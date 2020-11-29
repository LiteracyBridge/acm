package org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog;

import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.MainWindow.audioItems.AudioItemView;
import org.literacybridge.acm.gui.messages.RequestAndSelectAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestedAudioItemMessage;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.ACMDialog;
import org.literacybridge.acm.gui.util.FocusTraversalOnArray;
import org.literacybridge.acm.store.AudioItem;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.util.Observer;

public class AudioItemPropertiesDialog extends ACMDialog {

    private static final long serialVersionUID = -3854016276035587383L;
    private AudioItemPropertiesTable propertiesTable = null;

    private JButton backButton;
    private JButton nextButton;
    private JButton closeButton;

    public AudioItemPropertiesDialog(JFrame parent, AudioItemView view,
            Iterable<String> audioItemList, AudioItem showItem) {
        super(parent, LabelProvider.getLabel("AUDIO_ITEM_PROPERTIES"), true);

        addToMessageService();
        
        createNavButtons();
        createControlsForAvailableProperties();

        setMinimumSize(new Dimension(500, 570));
        // For debugging sizing issues.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                System.out.printf("Size %dx%d%n", AudioItemPropertiesDialog.this.getWidth(), AudioItemPropertiesDialog.this.getHeight());
            }
        });

        setFocusTraversalPolicy(new FocusTraversalOnArray(
                new Component[]{backButton, nextButton, closeButton}));

        // show current item first
        RequestAndSelectAudioItemMessage msg = new RequestAndSelectAudioItemMessage(
                RequestAudioItemMessage.RequestType.Current);
        Application.getMessageService().pumpMessage(msg);
    }

    protected void addToMessageService() {
        Application.getMessageService().addObserver(observer);
    }

    private void showAudioItem(AudioItem item) {
        if (item == null) {
            return; // JTBD
        }
        showMetadata(item);
    }

    private void getNextItem(ActionEvent e) {
        RequestAndSelectAudioItemMessage msg = new RequestAndSelectAudioItemMessage(
                RequestAudioItemMessage.RequestType.Next);
        Application.getMessageService().pumpMessage(msg);
    }

    private void getPrevItem(ActionEvent e) {
        RequestAndSelectAudioItemMessage msg = new RequestAndSelectAudioItemMessage(
                RequestAudioItemMessage.RequestType.Previews);
        Application.getMessageService().pumpMessage(msg);
    }

    private void createNavButtons() {
        Box hBox = Box.createHorizontalBox();
        hBox.setBorder(new EmptyBorder(4,8,4,8));

        backButton = new JButton(LabelProvider.getLabel("GOTO_PREV_AUDIO_ITEM"));
        backButton.addActionListener(this::getPrevItem);
        hBox.add(backButton);

        nextButton = new JButton(LabelProvider.getLabel("GOTO_NEXT_AUDIO_ITEM"));
        nextButton.addActionListener(this::getNextItem);
        hBox.add(Box.createHorizontalGlue());
        hBox.add(nextButton);

        add(hBox, BorderLayout.NORTH);

        hBox = Box.createHorizontalBox();
        hBox.setBorder(new EmptyBorder(4,8,4,8));
        hBox.add(Box.createHorizontalGlue());
        closeButton = new JButton(LabelProvider.getLabel("CLOSE"));
        closeButton.addActionListener(e -> {
            Application.getFilterState().updateResult(true);
            setVisible(false);
        });
        hBox.add(closeButton);
        add(hBox, BorderLayout.SOUTH);
    }

    private void createControlsForAvailableProperties() {
        JPanel propertiesPanel = new JPanel();
        propertiesPanel.setBorder(new EmptyBorder(4,8,4,8));
        propertiesPanel.setLayout(new BorderLayout());

        // Show properties table
        JScrollPane theScrollPane = new JScrollPane();
        propertiesTable = new AudioItemPropertiesTable(this);
        propertiesTable.setShowGrid(true, false);
        // use fixed color; there seems to be a bug in some plaf
        // implementations that cause strange rendering
//        propertiesTable.addHighlighter(HighlighterFactory
//                .createAlternateStriping(Color.white, new Color(237, 243, 254)));

        final HighlightPredicate predicate = (comnponent, adapter) ->
                ((AudioItemPropertiesModel) propertiesTable.getModel()).highlightRow(adapter.row);
        AbstractHighlighter highlighter = new AbstractHighlighter() {
            @Override
            protected Component doHighlight(Component component,
                    ComponentAdapter adapter) {
                if (predicate.isHighlighted(component, adapter)) {
                    component.setFont(component.getFont().deriveFont(Font.BOLD));
                    component.setForeground(Color.RED);
                } else if (adapter.row%2==0) {
                    component.setBackground(new Color(237, 243, 254));
                }
                return component;
            }
        };

//         ColorHighlighter highlighter = new ColorHighlighter(predicate, null,
//         Color.RED, null, null);
        propertiesTable.addHighlighter(highlighter);

        theScrollPane.setViewportView(propertiesTable);
        propertiesPanel.add(theScrollPane, BorderLayout.CENTER);
        add(propertiesPanel, BorderLayout.CENTER);
    }

    private void showMetadata(AudioItem audioItem) {
        propertiesTable.setModel(new AudioItemPropertiesModel(audioItem));
        propertiesTable.getTableHeader().getColumnModel()
                .getColumn(AudioItemPropertiesModel.EDIT_COL).setMaxWidth(25);
    }

    private final Observer observer = (o, arg) -> {
        if (arg instanceof RequestedAudioItemMessage) {
            RequestedAudioItemMessage msg = (RequestedAudioItemMessage) arg;
            showAudioItem(msg.getAudioItem());
        }
    };
}
