package org.literacybridge.acm.gui.assistants.ContentImport;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AudioItemSelectionDialog {
    private final Color bgColor;
    private final Color bgSelectionColor;
    private final Color bgAlternateColor;
    private JList<String> list;
    private JLabel label;
    private JButton okButton, cancelButton;
    private ActionListener okEvent, cancelEvent;
    private JDialog dialog;

    public AudioItemSelectionDialog(String title, String message, JList<String> listToDisplay) {
        this.list = listToDisplay;
        label = new JLabel(message);

        bgColor = Color.white; // table.getBackground();
        bgSelectionColor = list.getSelectionBackground();
        bgAlternateColor = new Color(235, 245, 252);

        setupButtons();
        JPanel pane = layoutComponents();
        JOptionPane optionPane = new JOptionPane(pane);
        optionPane.setOptions(new Object[] { okButton, cancelButton });
        dialog = optionPane.createDialog(title);
    }

    private void setupButtons() {
        okButton = new JButton("Ok");
        okButton.addActionListener(e -> dialog.setVisible(false));

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.setVisible(false));
    }

    private JPanel layoutComponents() {
        list.setCellRenderer(listCellRenderer);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(label, BorderLayout.NORTH);
        panel.add(list, BorderLayout.CENTER);
        list.setBorder(new LineBorder(Color.red, 1));
        return panel;
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

    public String show() {
        dialog.setVisible(true);
        return list.getSelectedValue();
    }
    public String getSelectedItem() { return list.getSelectedValue(); }
}

