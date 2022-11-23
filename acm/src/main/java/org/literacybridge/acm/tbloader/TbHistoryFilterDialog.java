package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.core.spec.RecipientList.RecipientAdapter;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.List;

import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;
import static org.literacybridge.acm.utils.SwingUtils.addEscapeListener;

public class TbHistoryFilterDialog extends JDialog {
    static private List<String> previousSelectedPath = null;
    static private Boolean previousCollectionSelected = null;

    private boolean ok = false;
    private JRecipientChooser recipientChooser;
    private List<RecipientAdapter> selectedRecipients;
    private List<String> selectionPath;
    private Boolean collectionSelected;
    private JRadioButton collectedButton;
    private JRadioButton deployedButton;

    TbHistoryFilterDialog() {
        setLayout(new BorderLayout());

        makeButtons();
        makeFilters();

        setSize(new Dimension(600, 450));
        setAlwaysOnTop(true);
        setModalityType(ModalityType.APPLICATION_MODAL);

        addEscapeListener(this);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        UIUtils.centerWindow(this, TOP_THIRD);

        // For debugging sizing issues.
//        addComponentListener(new java.awt.event.ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                System.out.printf("Size %dx%d%n",
//                    TbHistoryFilterDialog.this.getWidth(),
//                    TbHistoryFilterDialog.this.getHeight());
//            }
//        });
    }

    public boolean isOk() {
        return ok;
    }

    public List<RecipientAdapter> getSelectedRecipients() {
        return selectedRecipients;
    }

    public List<String> getSelectionPath() {
        return selectionPath;
    }

    public Boolean getCollectionSelected() {
        return collectionSelected;
    }

    private void makeFilters() {
        JPanel filterPanel = new JPanel();
        filterPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.X_AXIS));

        filterPanel.add(makeActivitySelectionButtons());
        filterPanel.add(Box.createHorizontalGlue());
        filterPanel.add(makeRecipientChooserPanel());
        filterPanel.add(Box.createHorizontalGlue());

        add(filterPanel, BorderLayout.CENTER);
    }

    private JComponent makeActivitySelectionButtons() {
        JPanel widgetsPanel = new JPanel();
        widgetsPanel.setLayout(new BoxLayout(widgetsPanel, BoxLayout.Y_AXIS));
        Border b = new CompoundBorder(new RoundedLineBorder(Color.lightGray, 1, 4), new EmptyBorder(10, 10, 10, 10));
        widgetsPanel.setBorder(b);

        widgetsPanel.add(Box.createVerticalStrut(5));
        widgetsPanel.add(new JLabel("Show counts of TBs needing to be:"));

        //Create the radio buttons.
        deployedButton = new JRadioButton(LabelProvider.getLabel("Updated"));
        deployedButton.setMnemonic(KeyEvent.VK_U);
        deployedButton.setActionCommand("Updated");

        collectedButton = new JRadioButton(LabelProvider.getLabel("Collected"));
        collectedButton.setMnemonic(KeyEvent.VK_C);
        collectedButton.setActionCommand("Collected");

        if (previousCollectionSelected == null || !previousCollectionSelected) {
            deployedButton.setSelected(true);
        } else {
            collectedButton.setSelected(true);
        }

        //Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(deployedButton);
        group.add(collectedButton);

        //Put the radio buttons in a column in a panel.
        widgetsPanel.add(deployedButton);
        widgetsPanel.add(collectedButton);
        widgetsPanel.add(Box.createVerticalGlue());

        return widgetsPanel;
    }

    private JComponent makeRecipientChooserPanel() {
        JPanel chooserPanel = new JPanel();
        chooserPanel.setLayout(new GridBagLayout());

        Border b = new CompoundBorder(new RoundedLineBorder(Color.lightGray, 1, 4), new EmptyBorder(10, 10, 10, 10));
        chooserPanel.setBorder(b);
        GBC gbc = new GBC().setGridx(0).setAnchor(GridBagConstraints.LINE_START).setFill(GridBagConstraints.HORIZONTAL);

        chooserPanel.add(new JLabel("Choose the entity to track:"), gbc);

        chooserPanel.add(new JLabel(), gbc.withWeightx(0.5).withGridx(1));

        recipientChooser = new JRecipientChooser(TBLoader.getApplication().getProgramSpec());
        recipientChooser.setHighlightWhenNoSelection(false);
        if (previousSelectedPath != null) {
            recipientChooser.setSelectionPath(previousSelectedPath);
        }
        chooserPanel.add(recipientChooser, gbc.withWeightx(1.0));

        // Consume additional vertical space.
        chooserPanel.add(new JLabel(), gbc.withWeighty(1));

        return chooserPanel;
    }


    private void makeButtons() {
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        JButton resetButton = new JButton(LabelProvider.getLabel("Reset"));
        resetButton.addActionListener(this::onReset);
        resetButton.setForeground(Color.green.darker());
        buttonBox.add(resetButton);
        buttonBox.add(Box.createHorizontalStrut(20));
        JButton cancelButton = new JButton(LabelProvider.getLabel("Cancel"));
        cancelButton.addActionListener(e -> setVisible(false));
        buttonBox.add(cancelButton);
        buttonBox.add(Box.createHorizontalStrut(10));
        JButton okButton = new JButton(LabelProvider.getLabel("OK"));
        okButton.addActionListener(this::onOk);
        buttonBox.add(okButton);
        buttonBox.add(Box.createHorizontalStrut(20));
        buttonBox.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(buttonBox, BorderLayout.SOUTH);
    }

    private void onReset(ActionEvent actionEvent) {
        recipientChooser.reset();
    }

    private void onOk(ActionEvent actionEvent) {
        ok = true;
        selectedRecipients = recipientChooser.getRecipientsForPartialSelection();
        selectionPath = recipientChooser.getSelectionPath();
        collectionSelected = collectedButton.isSelected();
        previousSelectedPath = recipientChooser.getSelectionPath();
        previousCollectionSelected = collectionSelected;
        this.setVisible(false);
    }
}
