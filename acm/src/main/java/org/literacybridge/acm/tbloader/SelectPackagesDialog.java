package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;

import javax.swing.AbstractListModel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import static org.literacybridge.acm.gui.Assistant.AssistantPage.getMaxWidthForWidget;

/**
 * A Dialog to let the user choose which Content Image(s) to deploy to a Talking Book.
 */
class SelectPackagesDialog extends JDialog {
    public int MAX_SELECTIONS;

    // All of the Content Packages available in the Deployment.
    private final List<String> availablePackages;
    // The "default" package (presumably for whatever Recipient is selected).
    private final List<String> defaultPackages;
    // The currently selected packages. Starts as the "default".
    private final List<String> selectedPackages;
    // If true, tells the caller that the user wants to remember the selected packages.
    private boolean isRememberSelection;
    // Was the OK button clicked?
    private boolean isOk = false;

    private JList<String> selectedPackagesList;
    private JList<String> availablePackagesList;
    private final Map<String, String> packageNameMap;
    private JButton moveDownButton;
    private JButton moveUpButton;
    private JButton addButton;
    private JButton removeButton;
    private final JCheckBox rememberSelection;
    private final JButton okButton;

    // Public API.
    public boolean isOk() {
        return isOk;
    }

    public List<String> getSelectedPackages() {
        return selectedPackages;
    }

    public boolean isRememberSelection() {
        return isRememberSelection;
    }

    /**
     * Dialog constructor.
     *  @param owner               Owning window.
     * @param availablePackages   List of packages in the Deployment
     * @param packageNameMap      Map package name to language and variant (if known)
     * @param defaultPackages     List of default packages for the recipient.
     * @param currentPackages     List of currently selected packages for the recipient.
     * @param isRememberSelection True if the "locked" option has been chosen.
     */
    SelectPackagesDialog(Frame owner,
        List<String> availablePackages,
        Map<String, String> packageNameMap,
        List<String> defaultPackages,
        List<String> currentPackages,
        boolean isRememberSelection,
        int MAX_SELECTIONS) {
        super(owner);
        setTitle("Choose Package for Recipient");

        this.MAX_SELECTIONS = MAX_SELECTIONS;
        this.availablePackages = availablePackages;
        this.packageNameMap = packageNameMap;
        this.defaultPackages = defaultPackages;
        // Clone the list so that we may modify it.
        this.selectedPackages = new ArrayList<>();
        if (currentPackages != null) {
            this.selectedPackages.addAll(currentPackages);
        } else if (defaultPackages != null && defaultPackages.size() > 0) {
            this.selectedPackages.addAll(defaultPackages);
        }
        this.isRememberSelection = isRememberSelection;

        // Put the dialog controls in a vertical box.
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        Border emptyBorder = new EmptyBorder(10, 10, 10, 10);
        dialogPanel.setBorder(emptyBorder);
        add(dialogPanel);

        //Create the options buttons.
        rememberSelection = new JCheckBox("Remember selection.", isRememberSelection);
        rememberSelection.setMnemonic(KeyEvent.VK_U);
        rememberSelection.setActionCommand("Remember Selection");
        rememberSelection.addActionListener(e -> this.isRememberSelection = rememberSelection.isSelected());

        JButton restoreDefaults = new JButton("Restore Defaults");
        restoreDefaults.setToolTipText("Sets Selected Packages to the default for the selected recipient.\nIf no recipient has been selected yet, clears Selected Packages.");
        restoreDefaults.addActionListener(this::restoreDefaults);

        Box optionsBox = Box.createHorizontalBox();
        optionsBox.add(rememberSelection);
        optionsBox.add(Box.createHorizontalGlue());
        optionsBox.add(restoreDefaults);

        dialogPanel.add(optionsBox);
        dialogPanel.add(Box.createVerticalStrut(10));

        // The real work of the dialog is in here.
        Component packagesBox = makePackageChooser();
        dialogPanel.add(packagesBox);

        // Add the OK / Cancel buttons
        okButton = new JButton(LabelProvider.getLabel("OK"));
        okButton.addActionListener(e -> {
            isOk = true;
            this.setVisible(false);
        });

        JButton cancelButton = new JButton(LabelProvider.getLabel("CANCEL"));
        cancelButton.addActionListener(e -> this.setVisible(false));

        // Layout the buttons
        dialogPanel.add(Box.createVerticalStrut(20));
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(okButton);
        buttonBox.add(Box.createHorizontalStrut(6));
        buttonBox.add(cancelButton);
        dialogPanel.add(buttonBox);

        // Make the dialog modal, and don't let it be obscured.
        setSize(550, 400);
        setAlwaysOnTop(true);
        setModalityType(ModalityType.DOCUMENT_MODAL);

        enableButtons();

        // For debugging sizing issues.
//        addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                System.out.printf("Size %dx%d%n",
//                    SelectPackagesDialog.this.getWidth(),
//                    SelectPackagesDialog.this.getHeight());
//            }
//        });
    }

    /**
     * Creates the lists of selected and available packages, and the buttons to move them around.
     *
     * @return a component implementing the chooser.
     */
    private Component makePackageChooser() {
        JPanel chooserPanel = new JPanel();
        chooserPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC()
            .setFill(GridBagConstraints.BOTH)
            .setWeighty(0.5);

        SelectedPackagesModel selectedPackagesModel = new SelectedPackagesModel();
        selectedPackagesList = new JList<>(selectedPackagesModel);
        Component selectedPackagesBox = makeListPanel(selectedPackagesList, true, "Selected Packages");

        // Buttons to add to and remove from the "selected" list.
        JPanel selectionButtonsPanel = new JPanel(new GridBagLayout());
        GBC buttonsGBC = new GBC()
            .setInsets(new Insets(10, 10, 0, 10))
            .setAnchor(GridBagConstraints.CENTER)
            .setFill(GridBagConstraints.HORIZONTAL)
            .setGridx(0);
        selectionButtonsPanel.add(new JLabel(" "), buttonsGBC);

        addButton = new JButton(">>> Add");
        addButton.addActionListener(this::onAdd);
        selectionButtonsPanel.add(addButton, buttonsGBC);

        removeButton = new JButton("<<< Remove");
        removeButton.addActionListener(this::onRemove);
        selectionButtonsPanel.add(removeButton, buttonsGBC);
        int moveButtonsWidth = getMaxWidthForWidget(removeButton,
            Collections.singletonList(removeButton.getText()));

        selectionButtonsPanel.add(new JLabel(""), buttonsGBC.withWeighty(1.0));

        availablePackagesList = new JList<>(new Vector<>(availablePackages));
        Component availablePackagesBox = makeListPanel(availablePackagesList, false, "Available Packages");

        // Put them together
        chooserPanel.add(availablePackagesBox, gbc);
        chooserPanel.add(selectionButtonsPanel, gbc);
        chooserPanel.add(selectedPackagesBox, gbc);

        return chooserPanel;
    }

    /**
     * Formats and wraps one of the panels. This helps the two panels to look more similar.
     * @param list The JList to be wrapped.
     * @param moveButtons If true, add the Move Up / Move Down buttons to the bottom.
     * @return a Component containing the list.
     */
    Component makeListPanel(JList<String> list, boolean moveButtons, String heading) {
        Box selectedPackagesBox = Box.createVerticalBox();
        Box headingBox = Box.createHorizontalBox();
        headingBox.add(new JLabel(heading));
        headingBox.add(Box.createVerticalGlue());
        selectedPackagesBox.add(headingBox);
        selectedPackagesBox.add(Box.createVerticalStrut(5));

        list.setCellRenderer(new PackageCellRenderer());
        list.addListSelectionListener(e -> enableButtons());

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(Math.max(2, list.getModel().getSize()));

        JScrollPane selectedListScroller = new JScrollPane(list);

        int buttonsWidth = getMaxWidthForWidget(new JButton(), Collections.singletonList("Move Down")) +
            getMaxWidthForWidget(new JButton(), Collections.singletonList("Move Up")) + 60;
        int packageNameWidth = getMaxWidthForWidget(list, availablePackages) + 40;

        packageNameWidth = Math.max(buttonsWidth, packageNameWidth);

        setWidths(list, packageNameWidth);

        selectedListScroller.setPreferredSize(new Dimension(1000, 2500));
        setWidths(selectedListScroller, packageNameWidth + 10);
        selectedPackagesBox.add(selectedListScroller);

        if (moveButtons) {
            SelectedPackagesModel model = (SelectedPackagesModel) list.getModel();

            Box reorderButtonsBox = Box.createHorizontalBox();
            moveDownButton = new JButton("Move Down");
            moveDownButton.addActionListener(this::onMoveDown);
            moveUpButton = new JButton("Move Up");
            moveUpButton.addActionListener(this::onMoveUp);
            reorderButtonsBox.add(Box.createHorizontalGlue());
            reorderButtonsBox.add(moveDownButton);
            reorderButtonsBox.add(Box.createHorizontalStrut(10));
            reorderButtonsBox.add(moveUpButton);
            reorderButtonsBox.add(Box.createHorizontalGlue());

            selectedPackagesBox.add(reorderButtonsBox);
        }
        return selectedPackagesBox;
    }

    /**
     * Attempt to get around some of the stupidity in Swing layout. Sets the minimum and preferred width to
     * the given value. Sets the max to a large number, because otherwise it will (likely, depending on the
     * component) default to the "preferred" value.
     * @param component The component whose width to set.
     * @param width The width to set.
     */
    private void setWidths(Component component, int width) {
        Dimension d = component.getMaximumSize();
        d.width = 99999;
        component.setMaximumSize(d);

        d = component.getPreferredSize();
        d.width = width;
        component.setPreferredSize(d);

        d = component.getMinimumSize();
        d.width = width;
        component.setMinimumSize(d);
    }

    /**
     * Sets the enabled state of the various buttons, based on selections.
     */
    private void enableButtons() {
        int ix = selectedPackagesList.getSelectedIndex();
        moveUpButton.setEnabled(ix > 0);
        moveDownButton.setEnabled(ix >= 0 && ix < selectedPackages.size() - 1);
        removeButton.setEnabled(ix >= 0);
        ix = availablePackagesList.getSelectedIndex();
        String availableItem = availablePackagesList.getSelectedValue();
        // Respect the limit on max packages. And only allow adding packages not already added.
        addButton.setEnabled(selectedPackages.size() < MAX_SELECTIONS && ix >= 0 && !selectedPackages.contains(
            availableItem));
        okButton.setEnabled(selectedPackages.size() > 0);
    }

    /**
     * Sets the selected packages to the "default" packages, and turns off the "remember selection" switch.
     *
     * @param actionEvent is ignored.
     */
    private void restoreDefaults(ActionEvent actionEvent) {
        isRememberSelection = false;
        rememberSelection.setSelected(false);
        selectedPackages.clear();
        selectedPackages.addAll(defaultPackages);
        selectedPackagesList.repaint();
        enableButtons();
    }

    /**
     * Moves a selected package "up" in the list. Because TBv1 only supports two packages, this is only
     * applicable to the "other" package, making it the first one. (The first package is what the TB
     * always boots into.)
     *
     * @param actionEvent is ignored.
     */
    private void onMoveUp(ActionEvent actionEvent) {
        int ix = selectedPackagesList.getSelectedIndex();
        ((SelectedPackagesModel) selectedPackagesList.getModel()).moveUp(ix);
        selectedPackagesList.ensureIndexIsVisible(ix - 1);
        enableButtons();
    }

    /**
     * Moves a selected package "down" in the list. Because TBv1 only supports two packages, this is only
     * applicable to the first package. (The first package is what the TB always boots into.)
     *
     * @param actionEvent is ignored.
     */
    private void onMoveDown(ActionEvent actionEvent) {
        int ix = selectedPackagesList.getSelectedIndex();
        ((SelectedPackagesModel) selectedPackagesList.getModel()).moveDown(ix);
        selectedPackagesList.ensureIndexIsVisible(ix + 1);
        enableButtons();
    }

    /**
     * Adds an available package to the list of selected packages.
     *
     * @param actionEvent is ignored.
     */
    private void onAdd(ActionEvent actionEvent) {
        String newValue = availablePackagesList.getSelectedValue();
        if (!selectedPackages.contains(newValue) && selectedPackages.size() < MAX_SELECTIONS) {
            ((SelectedPackagesModel) selectedPackagesList.getModel()).append(newValue);
        }
        selectedPackagesList.setSelectedValue(newValue, true);
        availablePackagesList.setSelectedIndex(-1);
        enableButtons();
    }

    /**
     * Removes a selected package; un-selects it.
     *
     * @param actionEvent is ignored.
     */
    private void onRemove(ActionEvent actionEvent) {
        int ix = selectedPackagesList.getSelectedIndex();
        if (ix >= 0) {
            ((SelectedPackagesModel) selectedPackagesList.getModel()).remove(ix);
        }
        selectedPackagesList.setSelectedIndex(-1);
        availablePackagesList.setSelectedIndex(-1);
        enableButtons();
    }

    /**
     * A class that implements the selected packages list model.
     */
    private class SelectedPackagesModel extends AbstractListModel<String> {

        @Override
        public int getSize() {
            return selectedPackages.size();
        }

        @Override
        public String getElementAt(int index) {
            return selectedPackages.get(index);
        }

        public void append(String item) {
            selectedPackages.add(item);
            fireIntervalAdded(this, selectedPackages.size() - 1, selectedPackages.size() - 1);
        }

        /**
         * Move up in the list, to a lower index.
         *
         * @param index to be moved up
         */
        private void moveUp(int index) {
            if (index < 1 || index >= selectedPackages.size()) return;
            doMove(index, index - 1);
        }

        /**
         * Move down in the list, that is, to a higher index.
         *
         * @param index to be moved down
         */
        private void moveDown(int index) {
            if (index < 0 || index >= selectedPackages.size() - 1) return;
            doMove(index, index + 1);
        }

        private void doMove(int oldIndex, int newIndex) {
            String savedItem = selectedPackages.get(oldIndex);

            selectedPackages.remove(oldIndex);
            fireIntervalRemoved(this, oldIndex, oldIndex);

            selectedPackages.add(newIndex, savedItem);
            fireIntervalAdded(this, newIndex, newIndex);

            selectedPackagesList.setSelectedIndex(newIndex);
        }

        private void remove(int index) {
            if (index < 0 || index >= selectedPackages.size()) return;
            selectedPackages.remove(index);
            fireIntervalRemoved(this, index, index);

            selectedPackagesList.setSelectedIndex(index);
        }

    }

    private class PackageCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
            if (packageNameMap.containsKey(value.toString())) value = packageNameMap.get(value.toString());
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

}
