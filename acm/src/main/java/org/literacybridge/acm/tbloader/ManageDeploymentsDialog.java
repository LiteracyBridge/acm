package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.gui.resourcebundle.LabelProvider;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class ManageDeploymentsDialog extends JDialog {
    Map<String, File> deployments;
    Vector<String> deploymentNames;


    public ManageDeploymentsDialog(Map<String, File> deployments) {
        this.deployments = deployments;
        this.deploymentNames = new Vector(deployments.keySet());

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        Border emptyBorder = new EmptyBorder(10, 10, 10, 10);
        dialogPanel.setBorder(emptyBorder);
        add(dialogPanel);

        //Create the buttons.
        JRadioButton latestOnly = new JRadioButton("Latest Only");
        latestOnly.setMnemonic(KeyEvent.VK_B);
        latestOnly.setActionCommand("Latest Only");
        latestOnly.setSelected(true);

        JCheckBox unpublished = new JCheckBox("Include unpublished");
        Font f = unpublished.getFont();
        unpublished.setFont(new Font(f.getFontName(), f.getStyle(), f.getSize()*3/4));
        unpublished.setMnemonic(KeyEvent.VK_C);
        unpublished.setSelected(true);

        JRadioButton manage = new JRadioButton("Manage Deployments");
        manage.setMnemonic(KeyEvent.VK_C);
        manage.setActionCommand("Manage Deployments");

        //Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(latestOnly);
        group.add(manage);

        // Layout the buttons in the dialog.
        Box latestOnlyPanel = Box.createHorizontalBox();
        latestOnlyPanel.add(latestOnly);
        latestOnlyPanel.add(Box.createHorizontalGlue());

        Box unpublishedPanel = Box.createHorizontalBox();
        unpublishedPanel.add(Box.createHorizontalStrut(20));
        unpublishedPanel.add(unpublished);
        unpublishedPanel.add(Box.createHorizontalGlue());

        Box managePanel = Box.createHorizontalBox();
        managePanel.add(manage);
        managePanel.add(Box.createHorizontalGlue());

        dialogPanel.add(latestOnlyPanel);
        dialogPanel.add(unpublishedPanel);
        dialogPanel.add(managePanel);

        // Add the deployments box
        JList list = new JList(deploymentNames);
        list.setCellRenderer(new CheckBoxListCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);

        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(250, 2500));
        dialogPanel.add(listScroller);

        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                System.out.println(deploymentNames.elementAt(list.getSelectedIndex()));
            }
        });

        // Add the OK / Cancel buttons

        //dialogPanel.add(Box.createVerticalGlue());
        dialogPanel.add(Box.createVerticalStrut(20));

        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        JButton exportButton = new JButton(LabelProvider.getLabel("OK"));
        exportButton.addActionListener(e -> System.out.println("OK"));
        //exportButton.setEnabled(false);
        buttonBox.add(exportButton);
        buttonBox.add(Box.createHorizontalStrut(6));
        JButton cancelButton = new JButton(LabelProvider.getLabel("CANCEL"));
        cancelButton.addActionListener(e -> System.out.println("Cancel"));
        buttonBox.add(cancelButton);
        dialogPanel.add(buttonBox);


        setSize(400, 350);
        setAlwaysOnTop(true);
        setModalityType(ModalityType.DOCUMENT_MODAL);
    }

    private class CheckBoxListCellRenderer  extends JPanel
        implements ListCellRenderer  {

        private JLabel label;
        private Color darker;
        private Color selected;
        private Font  selectedFont;
        private Font  normalFont;

        CheckBoxListCellRenderer() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            label = new JLabel();
            add(Box.createHorizontalStrut(5));
            add(label);
            darker = new Color(240, 240, 240);
            selected = new Color(100, 127, 255);

            normalFont = label.getFont();
            selectedFont = new Font(normalFont.getName(), normalFont.getStyle() | Font.BOLD, normalFont.getSize());
        }
        @Override
        public Component getListCellRendererComponent(JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            // Oh, Swing. The selection foreground is still black. So, hard code some colors.
            Color fg = isSelected ? Color.WHITE : Color.BLACK;
            Color bg;
            if (isSelected) {
                bg =  selected;
                label.setFont(selectedFont);
            } else {
                bg = index%2==0 ? list.getBackground() : darker;
                label.setFont(normalFont);
            }

            setBackground(bg);
            label.setForeground(fg);

            String key = value.toString();
            String str = "<html>" + key;
            File f = deployments.get(key);
            if (f != null) {
                str += "<br>"+f.getName();
            }

            label.setText(str);
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d_label = this.label.getPreferredSize();
            return new Dimension( 5 + d_label.width, 10+d_label.height);
        }

    }


}
