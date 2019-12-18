package org.literacybridge.acm.tbloader;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Map;
import java.util.Vector;

/**
 * A Dialog to let the user choose which Deployment to make active, from the set of published
 * Deployments.
 */
class ManageDeploymentsDialog extends JDialog {
    private Map<String, String> deployments;
    private Vector<String> deploymentNames;

    public String selection;
    private int lastSelected;
    private final JList<String> deploymentsList;

    /**
     * Dialog constructor.
     * @param owner Owning window.
     * @param deployments A map of deployment name to versioned deployment directory. From this
     *                    we build the list of Deployments and their versions (-a, -b, ...).
     * @param localDeployment The Deployment currently copied locally, if any.
     */
    ManageDeploymentsDialog(Frame owner, Map<String, String> deployments, String localDeployment) {
        super(owner);
        setTitle("Choose Deployment for TB-Loader");

        this.deployments = deployments;
        this.deploymentNames = new Vector<>(deployments.keySet());
        this.deploymentsList = new JList<>(deploymentNames);

        // Put the dialog controls in a vertical box.
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        Border emptyBorder = new EmptyBorder(10, 10, 10, 10);
        dialogPanel.setBorder(emptyBorder);
        add(dialogPanel);

        //Create the buttons.
        JRadioButton useLatest = new JRadioButton("Use Latest");
        useLatest.setMnemonic(KeyEvent.VK_U);
        useLatest.setActionCommand("Use Latest");
        useLatest.addActionListener(e -> {
            deploymentsList.setEnabled(false);
            deploymentsList.clearSelection();
            selection = deploymentNames.get(0);
        });

        JRadioButton chooseDeployment = new JRadioButton("Choose Deployment");
        chooseDeployment.setMnemonic(KeyEvent.VK_C);
        chooseDeployment.setActionCommand("Choose Deployment");
        chooseDeployment.addActionListener(e -> {
            deploymentsList.setEnabled(true);
            deploymentsList.setSelectedIndex(lastSelected);
        });

        // Is there a current local Deployment, and is it in the list of Deployments?
        if (!StringUtils.isEmpty(localDeployment) &&
                deploymentNames.contains(localDeployment) &&
                deploymentNames.indexOf(localDeployment) > 0) {
            // The local deployment is in the list, so start with that pre-selected in "Choose
            // Deployment".
            chooseDeployment.setSelected(true);
            selection = localDeployment;
            lastSelected = deploymentNames.indexOf(localDeployment);
            deploymentsList.setSelectedIndex(lastSelected);
            deploymentsList.setEnabled(true);
        } else {
            // The local deployment is NOT in the list, so start with "Use Latest".
            useLatest.setSelected(true);
            selection = deploymentNames.get(0);
            lastSelected = 0;
            deploymentsList.setSelectedIndex(lastSelected);
            deploymentsList.clearSelection();
            deploymentsList.setEnabled(false);
        }

        //Group the radio buttons, and arrange them in the dialog.
        ButtonGroup group = new ButtonGroup();
        group.add(useLatest);
        group.add(chooseDeployment);

        Box useLatestPanel = Box.createHorizontalBox();
        useLatestPanel.add(useLatest);
        useLatestPanel.add(Box.createHorizontalGlue());

        Box chooseDeploymentPanel = Box.createHorizontalBox();
        chooseDeploymentPanel.add(chooseDeployment);
        chooseDeploymentPanel.add(Box.createHorizontalGlue());

        dialogPanel.add(useLatestPanel);
        dialogPanel.add(chooseDeploymentPanel);

        // Add the deployments list box
        deploymentsList.setCellRenderer(new DeploymentCellRenderer());
        deploymentsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deploymentsList.setLayoutOrientation(JList.VERTICAL);
        deploymentsList.setVisibleRowCount(-1);

        JScrollPane listScroller = new JScrollPane(deploymentsList);
        listScroller.setPreferredSize(new Dimension(250, 2500));
        dialogPanel.add(listScroller);

        deploymentsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                if (deploymentsList.isEnabled()) {
                    lastSelected = deploymentsList.getSelectedIndex();
                    selection = deploymentNames.elementAt(lastSelected);
                }
                System.out.println(selection);
            }
        });

        // Add the OK / Cancel buttons
        JButton okButton = new JButton(LabelProvider.getLabel("OK"));
        okButton.addActionListener(e -> {
            this.setVisible(false);
        });

        JButton cancelButton = new JButton(LabelProvider.getLabel("CANCEL"));
        cancelButton.addActionListener(e -> {
            selection = localDeployment;
            this.setVisible(false);
        });

        // Layout the buttons
        dialogPanel.add(Box.createVerticalStrut(20));
        Box buttonBox = Box.createHorizontalBox();
        buttonBox.add(Box.createHorizontalGlue());
        buttonBox.add(okButton);
        buttonBox.add(Box.createHorizontalStrut(6));
        buttonBox.add(cancelButton);
        dialogPanel.add(buttonBox);

        // Make the dialog modal, and don't let it be obscured.
        setSize(400, 350);
        setAlwaysOnTop(true);
        setModalityType(ModalityType.DOCUMENT_MODAL);
    }

    /**
     * Class to render a cell in the Deployments list box.
     */
    private class DeploymentCellRenderer extends JPanel
        implements ListCellRenderer<String> {

        private JLabel label;
        private Color darker;
        private Color selected;
        private Font selectedFont;
        private Font normalFont;

        DeploymentCellRenderer() {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            label = new JLabel();
            add(Box.createHorizontalStrut(5));
            add(label);
            darker = new Color(240, 240, 240);
            selected = new Color(100, 127, 255);

            normalFont = label.getFont();
            selectedFont = new Font(normalFont.getName(),
                normalFont.getStyle() | Font.BOLD,
                normalFont.getSize());
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list,
            String value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            // Oh, Swing. The selection foreground is still black. So, hard code some colors.
            Color fg = isSelected ? Color.WHITE : Color.BLACK;
            Color bg;
            if (isSelected) {
                bg = selected;
                label.setFont(selectedFont);
            } else {
                bg = index % 2 == 0 ? list.getBackground() : darker;
                label.setFont(normalFont);
            }

            setBackground(bg);
            label.setForeground(fg);

            String str = "<html>" + value;
            String f = deployments.get(value);
            if (f != null) {
                str += "<br>" + f;
            }

            label.setText(str);
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d_label = this.label.getPreferredSize();
            return new Dimension(5 + d_label.width, 10 + d_label.height);
        }
    }

}
