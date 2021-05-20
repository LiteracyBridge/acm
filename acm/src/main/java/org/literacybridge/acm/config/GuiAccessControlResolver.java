package org.literacybridge.acm.config;

import org.literacybridge.acm.gui.Application;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.logging.Logger;

import static javax.swing.JOptionPane.NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;

/**
 * This is a class to handle prompting the user when opening or closing the database.
 * <p>
 * The real work is done in the super class.
 */

public class GuiAccessControlResolver implements AccessControlResolver {
    private static final Logger LOG = Logger.getLogger(GuiAccessControlResolver.class.getName());
    private final Component parent = Application.getApplication();

    @Override
    public ACCESS_CHOICE resolveAccessStatus(AccessControl accessControl,
        AccessStatus accessStatus) {
        ACCESS_CHOICE choice = ACCESS_CHOICE.AS_REQUESTED;
        String message;
        switch (accessStatus) {
            case none:
                throw new IllegalStateException("Should not happen");
            // ************************************************************************************
            // Notifications. The database will open.
            case checkedOut:
                message = "You have already checked out this ACM.\nYou can now continue making changes to it.";
                JOptionPane.showMessageDialog(parent, message);
                break;
            case newDatabase:
                // Nothing to do here.
                break;

            // ************************************************************************************
            // Fatal conditions. The database will not open, no matter what.
            case lockError:
                message = "Can't open ACM - another instance is already running";
                JOptionPane.showMessageDialog(parent, message);
                break;
            case processError:
                message = "Can't open ACM";
                JOptionPane.showMessageDialog(parent, message);
                break;
            case previouslyCheckedOutError:
                message = "Sandbox mode forced, but DB is currently checked out. Check DB in and restart with sandbox flag.";
                JOptionPane.showMessageDialog(parent, message);
                break;
            case noNetworkNoDbError:
                message = "Cannot connect to Amplio server and no available database. Shutting down.";
                JOptionPane.showMessageDialog(parent, message);
                break;
            case noDbError:
                message = "There is no copy of this ACM database on this computer.\nIt may be that the database has not been uploaded and downloaded yet.\nShutting down.";
                JOptionPane.showMessageDialog(parent, message);
                break;

            // ************************************************************************************
            // The caller can opt to continue in sandbox mode.
            case noServer: {
                if (accessControl.isSandboxed()) {
                    return ACCESS_CHOICE.USE_READONLY;
                }
                String title = "Cannot Connect to Server";
                message = "Cannot reach Amplio server.  You may shutdown and wait or use Demo Mode.";
                choice = chooseOpenMode(title, message);
                break;
            }
            case syncFailure: {
                if (accessControl.isSandboxed()) {
                    return ACCESS_CHOICE.USE_READONLY;
                }
                String title = "Cannot Connect to Server";
                message = "Cannot synchronize with Amplio content server.\nYou may shutdown and wait or begin demonstration mode with the available content.";
                choice = chooseOpenMode(title, message);
                break;
            }
            case outdatedDb: {
                if (accessControl.isSandboxed()) {
                    return ACCESS_CHOICE.USE_READONLY;
                }
                String title = "Cannot Get Write Access";
                message = "The latest version of the ACM database has not yet downloaded to this computer.\nYou may shutdown and wait or begin demonstration mode with the previous version.";
                choice = chooseOpenMode(title, message);
                break;
            }
            case notAvailable: {
                if (accessControl.isSandboxed()) {
                    return ACCESS_CHOICE.USE_READONLY;
                }
                String title = "Cannot Get Write Access";
                String openby = accessControl.getPosessor().getOrDefault("openby", "unknown user");
                String opendate = accessControl.getPosessor().getOrDefault("opendate", "unknown");
                String computername = accessControl.getPosessor().getOrDefault("computername", "unknown");
                message = String.format("Another user currently has write access to the ACM.\n%s at %s on computer %s\n",
                    openby, opendate, computername);
                choice = chooseOpenMode(title, message);
                break;
            }
            case userReadOnly: {
                if (accessControl.isSandboxed()) {
                    return ACCESS_CHOICE.USE_READONLY;
                }
                String title = "Readonly Access";
                message = "You only have read-only access to the database. You may shutdown or proceed in demo mode.";
                choice = chooseOpenMode(title, message);
                break;
            }
            case available: {
                if (accessControl.isSandboxed()) {
                        return ACCESS_CHOICE.USE_READONLY;
                } else if (ACMConfiguration.getInstance().isDoUpdate()) {
                    // --update means "Don't ask, just update, if it's OK."
                    return ACCESS_CHOICE.AS_REQUESTED;
                }
                String title = "Update or Demo Mode?";
                message = "Do you want to update the shared database?";
                Object[] options = {"Update Shared Database", "Use Demo Mode"};
                choice = chooseOpenMode(title, message, options);
                break;
            }

        }
        return choice;
    }

    @Override
    public void resolveOpenStatus(AccessControl accessControl, OpenStatus openStatus) {
        switch (openStatus) {
            case none:
            case serverError:
                String message = "Cannot connect to Amplio server to check out database. Shutting down.";
                JOptionPane.showMessageDialog(parent, message);
                break;
            case notAvailableError:
                String openby = accessControl.getPosessor().getOrDefault("openby", "unknown user");
                String opendate = accessControl.getPosessor().getOrDefault("opendate", "unknown");
                String computername = accessControl.getPosessor().getOrDefault("computername", "unknown");
                message = String.format(
                    "Sorry, but another user must have just checked out this ACM a moment ago!\nTry contacting %s\n"
                        + "\nAfter clicking OK, the ACM will shut down."
                        + "\nOpen by %s at %s on computer %s.",
                    openby, openby, opendate, computername);
                JOptionPane.showMessageDialog(parent, message);
                break;
            case opened:
                break;
            case reopened:
                break;
            case newDatabase:
                message = "ACM does not exist yet. Creating a new ACM and giving you write access.";
                JOptionPane.showMessageDialog(parent, message);
                break;
            case openedSandboxed:
                message = "The ACM is running in demonstration mode.\nPlease remember that your changes will not be saved.";
                JOptionPane.showMessageDialog(parent, message);
                break;
            default:
                throw new IllegalStateException("Unknown case in notifyStatus(OpenStatus): Should not happen.");
        }
    }

    private ACCESS_CHOICE chooseOpenMode(String title, String message) {
        Object[] options = {"Shutdown", "Use Demo Mode"};
        return chooseOpenMode(title, message, options);
    }

    @Override
    public UPDATE_CHOICE resolveUpdateStatus(AccessControl accessControl, UpdateDbStatus updateStatus) {
        UPDATE_CHOICE choice = UPDATE_CHOICE.KEEP;
        switch (updateStatus) {
            case ok:
                break;
            case denied:
                String title = "Release Forced by Admin";
                String msg = "Someone has forced control of this ACM, so you cannot check-in your changes.\nIf you are worried about losing a lot of work, contact support@amplio.org for assistance.";
                JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE);
                break;
            case networkError:
                title = "Cannot Connect to Server";
                msg = "Cannot reach Amplio server to release your checkout.\nPlease try again later so that others can check out this ACM.";
                JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE);
                break;
            case zipError:
                Object[] options = { "Keep Your Changes", "Throw Away Your Latest Changes" };
                msg = "There is a problem getting your changes into Dropbox.  Do you want to keep your changes and try to get this problem fixed or throw away your latest changes?";
                title = "Problem creating zip file on Dropbox";
                int buttonIx = JOptionPane.showOptionDialog(parent, msg, title,
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, options,
                    options[0]);
                if (buttonIx == NO_OPTION) {
                    // User chose not to save work. Retry with discard option.
                    choice = UPDATE_CHOICE.DELETE;
                }
                break;
        }
        return choice;
    }

    private ACCESS_CHOICE chooseOpenMode(String title, String message, Object[] options) {
        int buttonIx = JOptionPane.showOptionDialog(parent, message, title,
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, options,
            options[0]);
        // Did user explicitly choose "Use Demo Mode"? If not, terminate.
        return (buttonIx == NO_OPTION) ? ACCESS_CHOICE.USE_READONLY : ACCESS_CHOICE.AS_REQUESTED;
    }

}
