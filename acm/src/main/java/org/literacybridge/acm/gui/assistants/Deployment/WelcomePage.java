package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.stream.IntStream;

public class WelcomePage extends AssistantPage<DeploymentContext> {

    private final JComboBox<Object> deploymentChooser;

    private DeploymentContext context;

    public WelcomePage(PageHelper listener) {
        super(listener);
        context = getContext();
        setLayout(new GridBagLayout());

        Insets insets = new Insets(0,0,20,0);
        GridBagConstraints gbc = new GridBagConstraints(0,
            GridBagConstraints.RELATIVE,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL,
            insets,
            1,
            1);

        JLabel welcome = new JLabel("<html>"
            + "<span style='font-size:2.5em'>Welcome to the Deployment Creation Assistant.</span>"
            + "<br/><br/><p>This assistant will guide you through creating a Deployment. To create the Deployment:</p>"
            + "<ul>" + "<li> You will indicate the Deployment number you are creating.</li>"
            + "<li> The assistant will automatically determine if the required files and content are available, per the program specification.</li>"
            + "<li> You will then have an opportunity to make minor modifications to the Deployment playlists."
            + "<li> Once the configuration of the Deployment is satisfactory, you can give your approval, and the Deployment will be created and published.</li>"
            + "</ul>" + "<br/>Choose the Deployment, then click \"Next\" to get started. "

            + "</html>");
        add(welcome, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Import for Deployment: "));
        deploymentChooser = new JComboBox<>();
        deploymentChooser.addActionListener(this::onSelection);
        setComboWidth(deploymentChooser, "Choose...");
        deploymentChooser.setMaximumSize(deploymentChooser.getPreferredSize());

        hbox.add(deploymentChooser);
        hbox.add(Box.createHorizontalGlue());
        hbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(hbox, gbc);

        gbc.weighty = 1.0;
        add(new JLabel(), gbc);

        getProgramInformation();
    }

    /**
     * Called when a selection changes. Inspect the deployment and language
     * selections, and if both have a selection, enable the "Next" button.
     *
     * @param actionEvent
     */
    private void onSelection(ActionEvent actionEvent) {
        int deplIx = deploymentChooser.getSelectedIndex();
        deploymentChooser.setBorder(deplIx <= 0 ? redBorder : blankBorder);

        setComplete(deplIx >= 1);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        // Fill deployments

        deploymentChooser.removeAllItems();
        deploymentChooser.addItem("Choose...");
        IntStream.rangeClosed(1, 5)
            .boxed()
            .map(i -> i.toString())
            .forEach(s -> deploymentChooser.addItem(s));

        // If previously selected, re-select.
        int deploymentNo = context.deploymentNo;
        if (deploymentNo >= 0) {
            deploymentChooser.setSelectedItem(Integer.toString(deploymentNo));
        }
        onSelection(null);
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
        // Since this is the welcome page, there must be something selected in order to move on.
        context.deploymentNo = Integer.parseInt(deploymentChooser.getSelectedItem().toString());
    }

    @Override
    protected String getTitle() {
        return "Introduction";
    }

    private void getProgramInformation() {
        String project = ACMConfiguration.cannonicalProjectName(ACMConfiguration.getInstance()
            .getCurrentDB()
            .getSharedACMname());
        File programSpecDir = ACMConfiguration.getInstance().getProgramSpecDirFor(project);

        context.programSpec = new ProgramSpec(programSpecDir);
    }


}
