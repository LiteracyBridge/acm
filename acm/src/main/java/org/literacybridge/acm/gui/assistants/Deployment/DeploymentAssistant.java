package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant;

public class DeploymentAssistant {

    public static Assistant<DeploymentContext> create() {

        // Development debugging
        DeploymentContext context = new DeploymentContext();
        // Debugging & development:
        if (ACMConfiguration.isTestData()) {
            context.deploymentNo = 1;
        }

        Assistant<DeploymentContext> assistant = new Assistant.Factory<DeploymentContext>()
            .withContext(context)
            .withPageFactories(WelcomePage::new,
                ValidationPage::Factory,
                TweaksPage::Factory,
                DeployedPage::new)
            .withTitle("Deployment Assistant")
            .create();

        return assistant;
    }

}
