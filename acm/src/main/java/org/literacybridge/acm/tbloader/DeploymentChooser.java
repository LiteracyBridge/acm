package org.literacybridge.acm.tbloader;

import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.gui.dialogs.BusyDialog;
import org.literacybridge.acm.gui.util.UIUtils;
import org.literacybridge.core.spec.ProgramSpec;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.function.BiConsumer;

import static org.literacybridge.acm.gui.util.UIUtils.UiOptions.TOP_THIRD;

class DeploymentChooser {
    private final TBLoader tbLoader;
    private final String program;

    private String newDeployment;
    private String newRevision;
    private String newDeploymentDescription;
    private String[] packagesInDeployment;

    DeploymentChooser(TBLoader tbLoader) {
        this.tbLoader = tbLoader;
        program = tbLoader.getProgram();
    }

    /**
     * Gets the name of the chosen deployment, like TEST-20-1
     *
     * @return the name.
     */
    public String getNewDeployment() {
        return newDeployment;
    }

    @SuppressWarnings("unused")
    public String getNewRevision() {
        return newRevision;
    }

    /**
     * Gets the name of the deployment with the revision, like "TEST-20-1 (b)"
     *
     * @return The decorated name.
     */
    public String getNewDeploymentDescription() {
        return newDeploymentDescription;
    }

    /**
     * Returns an array of strings listing all of the packages in the chosen deployment,
     * like [ "TEST-20-1-EN", "TEST-20-1-FR" ]
     *
     * @return The array.
     */
    public String[] getPackagesInDeployment() {
        return packagesInDeployment;
    }

    /**
     * Determines the most recent deployment in ~/Amplio.
     */
    void select() {
        DeploymentsManager dm = new DeploymentsManager(program);
        DeploymentsManager.State state = dm.getState();
        boolean keepUnpublished = false; // If user chooses to keep unpublished Deployment.
        int answer;
        String message;

        switch (state) {
        case Missing_Latest:
            // Problem with S3, can't continue.
            message = "TB-Loader can not determine the latest Deployment, and can not continue.\n"
                + "(There is no .rev file in the 'published' directory.)";
            JOptionPane.showMessageDialog(tbLoader,
                message,
                "Cannot Determine Latest Deployment",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            break;
        case Bad_Local:
            // Either exit or delete & copy.
            Object[] optionsFix = { "Fix Automatically", "Exit and Fix Manually" };
            // Default: fix automatically
            answer = JOptionPane.showOptionDialog(tbLoader,
                "There is an error in the local deployment.\nDo you wish to exit and fix the problem yourself, or clean up automatically?",
                "Error in Local Deployment",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                optionsFix,
                optionsFix[0]);
            if (answer == JOptionPane.NO_OPTION) {
                // User chose Exit and Fix Manually
                System.exit(1);
            }
            dm.clearLocalDeployments();
            break;
        case OK_Unpublished:
            // prompt for unpublished, keep or copy
            Object[] optionsRefresh = { "Keep Unpublished", "Refresh From Latest" };
            // Default: Keep Unpublished
            answer = JOptionPane.showOptionDialog(tbLoader,
                "tbLoader TB Loader is running an unpublished deployment.\nDo you wish to keep the unpublished version, or delete it and use the latest published version?",
                "Unpublished Deployment",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                optionsRefresh,
                optionsRefresh[0]);
            if (answer == JOptionPane.NO_OPTION) {
                // User chose Refresh.
                dm.clearLocalDeployments();
            } else {
                keepUnpublished = true;
            }

            break;
        case No_Deployment:
        case Not_Latest:
            // copy choice or latest
            break;
        case OK_Latest:
            // good to go
            break;
        case OK_Cached: // enum value meaning dm.getAvailableDeployments().isOffline()
            DeploymentsManager.LocalDeployment localDeployment = dm.getLocalDeployment();
            // If we're offline, we have only whatever we have locally.
            newDeployment = localDeployment.localDeployment;
            String newRevision = localDeployment.localRevision;
            newDeploymentDescription = String.format("%s (rev: %s)", newDeployment, newRevision);
            // Good to go
            break;
        }

        if (keepUnpublished) {
            newDeployment = dm.getLocalDeployment().localContent.getName();
            newDeploymentDescription = String.format("UNPUBLISHED: %s", newDeployment);
            newRevision = "UNPUBLISHED";
        } else if (state != DeploymentsManager.State.OK_Cached) {
            newDeployment = getVersionedDeployment(dm);
            newDeploymentDescription = String.format("%s (rev: %s)",
                newDeployment,
                dm.getLocalDeployment().localRevision);
        }

        File localDeploymentDir = new File(tbLoader.getLocalTbLoaderDir(),
            TBLoaderConstants.CONTENT_SUBDIR + File.separator + newDeployment);
        packagesInDeployment = TBLoaderUtils.getPackagesInDeployment(localDeploymentDir);
    }

    /**
     * Select the deployment to be loaded. If the ACM is configured to keep multiple
     * deployments available, those are offered as a choice to the user. Otherwise
     * the latest (only) is used.
     *
     * @param dm a DeploymentsManager with information about the local and global Deployments.
     * @return The versioned deployment, like "DEMO-2018-4-b"
     */
    private String getVersionedDeployment(DeploymentsManager dm) {
        DeploymentsManager.LocalDeployment localDeployment = dm.getLocalDeployment();
        DeploymentsManager.AvailableDeployments available = dm.getAvailableDeployments();
        String desiredDeployment;
        String desiredRevision;

        if (dm.getAvailableDeployments().isOffline()) {
            // If we're offline, we have only whatever we have locally.
            desiredDeployment = localDeployment.localDeployment;
            desiredRevision = localDeployment.localRevision;
        } else {
            desiredDeployment = available.getCurrentDeployment();
            // Are there multiple Deployments from which to choose?
            if (available.getDeploymentDescriptions().size() > 1) {
                ManageDeploymentsDialog dialog = new ManageDeploymentsDialog(tbLoader,
                    available.getDeploymentDescriptions(),
                    localDeployment.localDeployment);
                // Place the new dialog within the application frame.
                dialog.setLocation(tbLoader.getX() + 20, tbLoader.getY() + 20);
                dialog.setVisible(true);
                desiredDeployment = dialog.selection;
            }
            desiredRevision = dm.getAvailableDeployments().getRevIdForDeployment(desiredDeployment);

            // If we don't have what we want, get it from S3.
            if (!desiredDeployment.equals(localDeployment.localDeployment)
                || !desiredRevision.equals(localDeployment.localRevision)) {

                String template = "Downloading %d %%";
                BusyDialog dialog = new BusyDialog("Downloading Deployment", tbLoader);
                UIUtils.centerWindow(dialog, TOP_THIRD);
                BiConsumer<Long, Long> progressHandler = (p, t) -> dialog.update(String.format(template, (p * 100) / t));
                final String deploymentToFetch = desiredDeployment;

                Runnable job = () -> {
                    try {
                        dm.getDeployment(deploymentToFetch, progressHandler);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // A race is architecturally possible, even if a very remote possibility.
                    while (!dialog.isVisible()) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(10);
                        } catch (InterruptedException ignored) {
                            break;
                        }
                    }
                    UIUtils.hideDialog(dialog);
                };
                new Thread(job).start();
                dialog.setVisible(true);
            }
        }
        newRevision = desiredRevision;
        return desiredDeployment;
    }

    String getDeploymentProvenance(ProgramSpec programSpec) {
        StringBuilder result = new StringBuilder("<html>").append(getNewDeploymentDescription());
        String deploymentUser = null, deploymentTime = null, deploymentDate = null;
        if (programSpec != null) {
            Properties properties = programSpec.getDeploymentProperties();
            if (properties != null) {
                deploymentDate = properties.getProperty(TBLoaderConstants.DEPLOYMENT_CREATION_DATE,
                    null);
                deploymentTime = properties.getProperty(TBLoaderConstants.DEPLOYMENT_CREATION_TIME,
                    null);
                deploymentUser = properties.getProperty(TBLoaderConstants.DEPLOYMENT_CREATION_USER,
                    null);
            }
            if (deploymentDate != null || deploymentTime != null || deploymentUser != null) {
                result.append(" Created");
                if (deploymentDate != null) result.append(" ").append(deploymentDate);
                if (deploymentTime != null) result.append(" ").append(deploymentTime);
                if (deploymentUser != null) result.append(" by ").append(deploymentUser);
            }
        }
        result.append("</html>");

        return result.toString();
    }

}
