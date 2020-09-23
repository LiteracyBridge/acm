package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class WelcomePage extends AssistantPage<DeploymentContext> {

    private final JComboBox<Object> deploymentChooser;

    private DeploymentContext context;

    WelcomePage(PageHelper<DeploymentContext> listener) {
        super(listener);
        context = getContext();
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel("<html>"
            + "<span style='font-size:2.5em'>Welcome to the Deployment Assistant.</span>"
            + "<br/><br/><p>The Assistant will guide you through creating a deployment. Here are the steps:</p>"
            + "<ol>" + "<li> Choose the deployment number you want to create.</li>"
            + "<li> The Assistant will see if the required files are available, based on information from the Program Specification. "
            + "If files are missing, you may receive a warning message.</li>"
            + "<li> If needed, you can then make minor changes to the deployment playlists.</li>"
            + "<li> Once you are happy with the deployment, you will give your approval and the deployment will be created and published.</li>"
            + "</ol>" + "<br/>Choose the deployment, and then click \"Next\" to get started. "

            + "</html>");
        add(welcome, gbc);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Create deployment: "));
        deploymentChooser = new JComboBox<>();
        deploymentChooser.addActionListener(this::onSelection);
        setComboWidth(deploymentChooser, "Choose...");
        deploymentChooser.setMaximumSize(deploymentChooser.getPreferredSize());

        hbox.add(deploymentChooser);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        gbc.weighty = 1.0;
        add(new JLabel(), gbc);

        getProgramInformation();
    }

    /**
     * Called when a selection changes. Inspect the deployment and language
     * selections, and if both have a selection, enable the "Next" button.
     *
     * @param actionEvent is unused.
     */
    @SuppressWarnings("unused")
    private void onSelection(ActionEvent actionEvent) {
        boolean complete = getSelectedDeployment() >= 0;
        deploymentChooser.setBorder(complete ? blankBorder : redBorder);
        setComplete(complete);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        // Fill deployments

        deploymentChooser.removeAllItems();
        deploymentChooser.addItem("Choose...");
        List<String> deployments = context.programSpec.getDeployments()
            .stream()
            .map(d -> Integer.toString(d.deploymentnumber))
            .collect(Collectors.toList());
        deployments
            .forEach(deploymentChooser::addItem);

        // If only one deployment, or previously selected, auto-select.
        if (deployments.size() == 1) {
            deploymentChooser.setSelectedIndex(1); // only item after "choose..."
        } else if (context.deploymentNo >= 0) {
            deploymentChooser.setSelectedItem(Integer.toString(context.deploymentNo));
        }
        onSelection(null);
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
        // Since this is the welcome page, there must be something selected in order to move on.
        context.deploymentNo = getSelectedDeployment();
    }

    @Override
    protected String getTitle() {
        return "Introduction";
    }

    private int getSelectedDeployment() {
        int deploymentNo = -1;
        Object deploymentStr = deploymentChooser.getSelectedItem();
        if (deploymentStr != null) {
            try {
                deploymentNo = Integer.parseInt(deploymentStr.toString());
            } catch (NumberFormatException ignored) {
                // ignored
            }
        }
        return deploymentNo;
    }

    private void getProgramInformation() {
        File programSpecDir = ACMConfiguration.getInstance()
                .getCurrentDB()
                .getPathProvider()
                .getProgramSpecDir();
        context.programSpec = new ProgramSpec(programSpecDir);
    }

}
