package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.gui.Assistant.Assistant;

public class DeploymentAssistant {

    public static Assistant<DeploymentContext> create() {

        // Development debugging
        DeploymentContext context = new DeploymentContext();
        context.deploymentNo = 1;

        Assistant<DeploymentContext> assistant = new Assistant.Factory<DeploymentContext>()
            .withContext(context)
            .withPageCtors(WelcomePage::new, ValidationPage::new)
            .withTitle("Deployment Assistant")
            .create();

        return assistant;
    }

}
