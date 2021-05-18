package org.literacybridge.acm.config;

import org.literacybridge.acm.config.AccessControlResolver.AccessStatus;
import org.literacybridge.acm.gui.Application;

import javax.swing.*;
import java.awt.Component;
import java.util.logging.Logger;

import static javax.swing.JOptionPane.NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;

/**
 * This is a class to handle prompting the user when opening or closing the database.
 * <p>
 * The real work is done in the super class.
 */

public class GuiAccessControl extends AccessControl {
    private static final Logger LOG = Logger.getLogger(GuiAccessControl.class.getName());

    private Component parent = Application.getApplication();
    GuiAccessControl(DBConfiguration config) {
        super(config);
    }

    @Override
    public void initDb() {
        boolean useSandbox = ACMConfiguration.getInstance().isForceSandbox() ||
            dbConfiguration.userIsReadOnly();
        int buttonIx;
        accessStatus = AccessStatus.none;

        if (ACMConfiguration.getInstance().isDisableUI()) {
            throw new IllegalStateException("Can't call this function without UI");
        }

        // See if the user can open the database, and if not, ask them what they want to do.
        // If they can, ask if they want RO or RW (unless they have RO access).
        // Some of these try again, hence the loop.
        statusLoop:
        while (true) {
            accessStatus = super.determineAccessStatus();
            switch (accessStatus) {
            case none:
                throw new IllegalStateException("Should not happen");
            case lockError:
                String msg = "Can't open ACM - another instance is already running";
                JOptionPane.showMessageDialog(parent, msg);
                stackTraceExit(1);
                break;
            case processError:
                msg = "Can't open ACM";
                JOptionPane.showMessageDialog(parent, msg);
                stackTraceExit(1);
                break;
            case previouslyCheckedOutError:
                msg = "Sandbox mode forced, but DB is currently checked out. Check DB in and restart with sandbox flag.";
                JOptionPane.showMessageDialog(parent, msg);
                stackTraceExit(1);
                break;
            case noNetworkNoDbError:
                msg = "Cannot connect to Amplio server and no available database. Shutting down.";
                JOptionPane.showMessageDialog(parent, msg);
                stackTraceExit(1);
                break;
            case noDbError:
                msg = "There is no copy of this ACM database on this computer.\nIt may be that the database has not been uploaded and downloaded yet.\nShutting down.";
                JOptionPane.showMessageDialog(parent, msg);
                stackTraceExit(0);
                break;
            case checkedOut:
                msg = "You have already checked out this ACM.\nYou can now continue making changes to it.";
                JOptionPane.showMessageDialog(parent, msg);
                break statusLoop;
            case newDatabase:
                // Nothing to ask.
                break statusLoop;
            case noServer: {
                if (useSandbox) break statusLoop;
                Object[] options = { "Try again", "Use Demo Mode" };
                msg = "Cannot reach Amplio server.  Do you want to get online now and try again or use Demo Mode?";
                String title = "Cannot Connect to Server";
                buttonIx = JOptionPane.showOptionDialog(parent, msg, title,
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, options,
                        options[0]);
                switch (buttonIx) {
                    case JOptionPane.CLOSED_OPTION:
                        stackTraceExit(1);
                    case JOptionPane.YES_OPTION:
                        // Try again
                        break;
                    case JOptionPane.NO_OPTION:
                        useSandbox = true;
                        break statusLoop;
                }
            }
            case syncFailure:
                // fall through to notAvailable
            case notAvailable:
                // Another user has the db checked out. If user requested sandbox anyway, just honor that.
                if (useSandbox) break statusLoop;
                // fall through to outdatedDB.
            case outdatedDb: {
                Object[] options = { "Shutdown", "Use Demo Mode" };
                if (accessStatus == AccessStatus.outdatedDb) {
                    msg = "The latest version of the ACM database has not yet downloaded to this computer.\nYou may shutdown and wait or begin demonstration mode with the previous version.";
                } else if (accessStatus == AccessStatus.syncFailure) {
                    msg = "Cannot synchronize with Amplio content server.\nYou may shutdown and wait or begin demonstration mode with the available content.";
                } else {
                    String openby = super.getPosessor().getOrDefault("openby", "unknown user");
                    String opendate = super.getPosessor().getOrDefault("opendate", "unknown");
                    String computername = super.getPosessor().getOrDefault("computername", "unknown");
                    msg = String.format("Another user currently has write access to the ACM.\n%s at %s on computer %s\n",
                                        openby, opendate, computername);
                }
                String title = "Cannot Get Write Access";
                buttonIx = JOptionPane.showOptionDialog(parent, msg, title,
                                                        JOptionPane.YES_NO_CANCEL_OPTION,
                                                        JOptionPane.QUESTION_MESSAGE, null, options,
                                                        options[0]);
                switch (buttonIx) {
                case JOptionPane.CLOSED_OPTION:
                case JOptionPane.YES_OPTION:
                    stackTraceExit(1);
                    break;
                case JOptionPane.NO_OPTION:
                    useSandbox = true;
                    break statusLoop;
                }
            }
            break;
            case userReadOnly:
                // If the user only has read-only access, don't need to ask anything.
                useSandbox = true;
                break statusLoop;
            case available: {
                if (useSandbox) {
                    break statusLoop;
                } else if (ACMConfiguration.getInstance().isDoUpdate()) {
                    // --update means "Don't ask, just update, if it's OK."
                    break statusLoop;
                }
                Object[] options = { "Update Shared Database", "Use Demo Mode" };
                msg = "Do you want to update the shared database?";
                String title = "Update or Demo Mode?";
                buttonIx = JOptionPane.showOptionDialog(parent, msg, title, JOptionPane.YES_NO_OPTION,
                                                        JOptionPane.QUESTION_MESSAGE, null, options,
                                                        options[0]);
                switch (buttonIx) {
                case JOptionPane.YES_OPTION:
                    break statusLoop;
                case JOptionPane.NO_OPTION:
                    useSandbox = true;
                    break statusLoop;
                case JOptionPane.CLOSED_OPTION:
                    stackTraceExit(1);
                }
            }
            break;
            }
        }

        // If we're here, we're going to try to open the database.
        openStatus = super.open(useSandbox);
        switch (openStatus) {
        case none:
            throw new IllegalStateException("Should not happen");
        case serverError:
            String msg = "Cannot connect to Amplio server to check out database. Shutting down.";
            JOptionPane.showMessageDialog(parent, msg);
            stackTraceExit(1);
            break;
        case notAvailableError:
            String openby = super.getPosessor().getOrDefault("openby", "unknown user");
            String opendate = super.getPosessor().getOrDefault("opendate", "unknown");
            String computername = super.getPosessor().getOrDefault("computername", "unknown");
            msg = String.format("Another user currently has write access to the ACM.\n%s at %s on computer %s\n",
                openby, opendate, computername);
            msg = String.format(
                    "Sorry, but another user must have just checked out this ACM a moment ago!\nTry contacting %s\n"
                            + "\nAfter clicking OK, the ACM will shut down."
                            + "\nOpen by %s at %s on computer %s.",
                    openby, openby, opendate, computername);
            JOptionPane.showMessageDialog(parent, msg);
            stackTraceExit(1);
            break;
        case opened:
            break;
        case reopened:
            break;
        case newDatabase:
            msg = "ACM does not exist yet. Creating a new ACM and giving you write access.";
            JOptionPane.showMessageDialog(parent, msg);
            break;
        case openedSandboxed:
            if (!ACMConfiguration.getInstance().isForceSandbox()) {
                msg = "The ACM is running in demonstration mode.\nPlease remember that your changes will not be saved.";
                JOptionPane.showMessageDialog(parent, msg);
            }
            break;
        }
        openStatus.isOpen();
    }

    /**
     * Calls AccessControl commitDbChanges or discardDbChanges, with appropriate dialogs before and
     * after. This function doesn't actually DO anything, that's all deferred to AccessControl.
     *
     * @return True if the update was saved successful.
     */
    public boolean updateDb() {
        boolean checkoutRevoked = false;
        boolean checkinOk = false;
        boolean saveWork = true;
        boolean savedWork = false; // was it actually saved OK.
        int buttonIx;

        if (ACMConfiguration.getInstance().isDisableUI()) {
            throw new IllegalStateException("Can't call this function without UI");
        }

        if (!super.dbConfiguration.getMetadataStore().hasChanges()) {
            super.discardDbChanges();
            return savedWork;
        }

        Object[] optionsSaveWork = { "Save Work", "Throw Away Your Latest Changes" };
        String msg = "If you made a mistake you can throw away all your changes now.";
        String title = "Save Work?";
        buttonIx = JOptionPane.showOptionDialog(parent, msg, title, JOptionPane.YES_NO_CANCEL_OPTION,
                                                JOptionPane.QUESTION_MESSAGE, null, optionsSaveWork,
                                                optionsSaveWork[0]);
        if (buttonIx == 1) {
            msg = "Are you sure you want to throw away all your work since opening the ACM?";
            title = "Are You Sure?";
            buttonIx = JOptionPane.showOptionDialog(parent, msg, title, JOptionPane.OK_CANCEL_OPTION,
                                                    JOptionPane.WARNING_MESSAGE, null, null,
                                                    JOptionPane.CANCEL_OPTION);
            if (buttonIx == JOptionPane.OK_OPTION) {
                saveWork = false;
            }
        }

        checkinLoop:
        while (true) {
            AccessControlResolver.UpdateDbStatus updateStatus = saveWork ? super.commitDbChanges() : super.discardDbChanges();
            switch (updateStatus) {
            case ok:
                checkinOk = true;
                savedWork = saveWork; // We did what we set out to do.
                break checkinLoop;
            case denied:
                msg = "Someone has forced control of this ACM, so you cannot check-in your changes.\nIf you are worried about losing a lot of work, contact support@amplio.org for assistance.";
                JOptionPane.showMessageDialog(parent, msg);
                // To indicate that work was not saved. Will prevent deleting old .zip files, which we may need later.
                // There is really nothing more we can do here, however.
                saveWork = false;
                checkoutRevoked = true;
                break checkinLoop;
            case networkError: {
                Object[] options = { "Try again", "Shutdown" };
                msg = "Cannot reach Amplio server.\nDo you want to get online and try again or shutdown and try later?";
                title = "Cannot Connect to Server";
                buttonIx = JOptionPane.showOptionDialog(parent, msg, title,
                                                        JOptionPane.YES_NO_CANCEL_OPTION,
                                                        JOptionPane.QUESTION_MESSAGE, null, options,
                                                        options[0]);
                if (buttonIx == YES_OPTION) {
                    // User chose to try again.
                    break;
                }
                break checkinLoop;
            }
            case zipError: {
                Object[] options = { "Keep Your Changes", "Throw Away Your Latest Changes" };
                msg = "There is a problem getting your changes into Dropbox.  Do you want to keep your changes and try to get this problem fixed or throw away your latest changes?";
                title = "Problem creating zip file on Dropbox";
                buttonIx = JOptionPane.showOptionDialog(parent, msg, title,
                                                        JOptionPane.YES_NO_CANCEL_OPTION,
                                                        JOptionPane.WARNING_MESSAGE, null, options,
                                                        options[0]);
                if (buttonIx == NO_OPTION) {
                    // User chose not to save work. Retry with discard option.
                    saveWork = false;
                    break;
                }
                break checkinLoop;
            }
            }
        }

        if (checkoutRevoked) {
            // Already gave message above
            msg = null;
        } else if (checkinOk) {
            if (saveWork) {
                msg = "Your changes have been checked in.\n\nPlease stay online for a few minutes so your changes\ncan be uploaded (until Dropbox is 'Up to date').";
            } else {
                msg = null; // "Your changes have been discarded.";
            }
        } else {
            if (saveWork) {
                msg = "Your changes could not be checked in now, but you still have this ACM\nchecked out and can submit your changes later.";
            } else {
                msg = "Could not release your checkout.  Please try again later so that others can checkout this ACM.";
            }
        }
        if (msg != null)
            JOptionPane.showMessageDialog(parent, msg);

        return savedWork;
    }

    /**
     * Helper to print a stack trace and exit.
     *
     * @param rc the return code.
     */
    private void stackTraceExit(int rc) {
        new Throwable().printStackTrace();
        System.exit(rc);
    }

}
