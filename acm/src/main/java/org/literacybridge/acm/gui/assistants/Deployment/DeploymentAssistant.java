package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.gui.Assistant.Assistant;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.ContentImport.FilesPage;
import org.literacybridge.acm.gui.assistants.ContentImport.MatchPage;
import org.literacybridge.acm.gui.assistants.ContentImport.SummaryPage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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
