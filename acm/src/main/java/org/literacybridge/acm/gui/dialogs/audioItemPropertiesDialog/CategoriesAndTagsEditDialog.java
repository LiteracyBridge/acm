package org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.util.ACMDialog;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;

import com.google.common.collect.Lists;

public class CategoriesAndTagsEditDialog extends ACMDialog {
    private final AudioItem audioItem;
    private final JList categories;

    // TODO: support removing tags in this dialog too
    //private final JList tags;

    public CategoriesAndTagsEditDialog(Frame owner, final AudioItem audioItem) {
        super(owner, "Edit categories and labels", true);
        this.audioItem = audioItem;
        setMinimumSize(new Dimension(200, 100));
        setSize(200, 100);
        setUndecorated(true);

        List<Category> categoryLeaves = Lists.newArrayList(audioItem.getCategoryLeavesList());

        categories = new JList(new AbstractListModel() {
            @Override public int getSize() {
                return categoryLeaves.size();
            }

            @Override public Object getElementAt(int index) {
                return categoryLeaves.get(index);
            }

        });

        final JButton removeButton = new JButton("Remove selected categories");
        removeButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                for (Object selected : categories.getSelectedValues()) {
                    audioItem.removeCategory((Category) selected);
                }
                ACMConfiguration.getCurrentDB().getMetadataStore().commit(audioItem);
                setVisible(false);
            }
        });

        removeButton.setEnabled(false);

        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        categories.addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                removeButton.setEnabled(categories.getSelectedIndex() != -1);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(removeButton);
        buttonPanel.add(cancelButton);

        JLabel categoryLabel = new JLabel("Select categories to remove");
        getContentPane().add(categoryLabel, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(categories), BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
    }
}
