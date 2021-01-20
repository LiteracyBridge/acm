package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.WEST;
import static org.literacybridge.acm.cloud.Authenticator.UPDATING_ROLES;
import static org.literacybridge.acm.cloud.Authenticator.ALL_USER_ROLES;
import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class ProgramCard extends CardContent {
    private static final String DIALOG_TITLE = "Select Program";
    protected static final int CARD_HEIGHT = 580;

    private final PanelButton okButton;
    private final JList<String> choicesList;
    private final JCheckBox forceSandbox;
    private final JScrollPane choicesListScrollPane;
    private List<String> programsAllowingUpdates = new ArrayList<>();

    public ProgramCard(WelcomeDialog welcomeDialog,
        WelcomeDialog.Cards panel)
    {
        super(welcomeDialog, DIALOG_TITLE, panel);
        JPanel dialogPanel = this;

        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC(getGBC()).withAnchor(CENTER);
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // Amplio logo
        JLabel logoLabel = new JLabel(getScaledLogo());
        dialogPanel.add(logoLabel, gbc);

        JLabel promptLabel = new JLabel("<html>Choose the ACM to open.</html>");
        dialogPanel.add(promptLabel, gbc);

        choicesList = new JList<>();
        choicesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        choicesList.addListSelectionListener(this::listSelectionListener);
        choicesList.addFocusListener(listFocusListener);
        choicesList.addMouseListener(listMouseListener);

        choicesListScrollPane = new JScrollPane(choicesList);
        dialogPanel.add(choicesListScrollPane, gbc.withWeighty(1.0).withFill(BOTH));

        forceSandbox = new JCheckBox("Use demo mode");
        forceSandbox.addActionListener(this::onSandbox);
        if (welcomeDialog.options.contains(Authenticator.LoginOptions.OFFER_DEMO_MODE)) {
            dialogPanel.add(forceSandbox, gbc.withAnchor(WEST).withFill(NONE));
            // If not authenticated (roughly ~offline), sandbox is the only option, so don't let
            // the user change it.
            if (!welcomeDialog.cognitoInterface.isAuthenticated()) {
                forceSandbox.setSelected(true);
                forceSandbox.setEnabled(false);
            }
            forceSandbox.setToolTipText("In DEMO mode (or TRAINING mode), you can make changes, but they will be discarded when you are done.");
        }

        okButton = new PanelButton("Ok");
        okButton.setFont(getTextFont());
        okButton.setBgColorPalette(AMPLIO_GREEN);
        okButton.addActionListener(e -> onOk());
        okButton.setEnabled(false);
        dialogPanel.add(okButton, gbc.withFill(NONE));

        addComponentListener(componentAdapter);
    }

    Boolean explicitSandbox = null; // An explicit sandbox choice, if user made one.
    private void onSandbox(ActionEvent actionEvent) {
        explicitSandbox = forceSandbox.isSelected();
    }

    @Override
    void onShown(ActionEvent actionEvent) {
        super.onShown(actionEvent);
        String ALL_ROLES = String.join(",", ALL_USER_ROLES);
        // Build the list of ACM names from which to choose. The list depends on whether we're
        // authenticated (not being authenticated is roughly equivalent to not having network
        // access, so we give access to all local programs). The list also depends on the option
        // LOCAL_DATA_ONLY, because the ACM can only work with local data.
        //
        List<String> acmNames;
        if (welcomeDialog.cognitoInterface.isAuthenticated()) {
            Map<String, String> programs = welcomeDialog.cognitoInterface.getPrograms();
            acmNames = new ArrayList<>(programs.keySet());
            // If data must already be local, filter the list to those ACMs that are local.
            if (welcomeDialog.options.contains(Authenticator.LoginOptions.LOCAL_DATA_ONLY)) {
                List<String> localNames = Authenticator.getInstance().getLocallyAvailablePrograms();
                final List<String> usersPrograms = acmNames;
                acmNames = localNames.stream()
                    .filter(name -> {
                        if (usersPrograms.contains(name)) return true;
                        if (welcomeDialog.options.contains(Authenticator.LoginOptions.INCLUDE_FB_ACMS) &&
                            name.contains("-FB-")) {
                            int fb = name.indexOf("-FB-");
                            // Is the part before the "-FB-" one of the user's programs?
                            return usersPrograms.contains(name.substring(0, fb));
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
            }
            // If updating (not sandboxing) is an option, determine which ACMs will be sandbox
            // only, and which allow a choice.
            if (welcomeDialog.options.contains(Authenticator.LoginOptions.OFFER_DEMO_MODE)) {
                programsAllowingUpdates = acmNames
                        .stream()
                        .filter(name -> {
                            Set<String> roles = Arrays.stream(programs.getOrDefault(name, ALL_ROLES).split(","))
                                    .collect(Collectors.toSet());
                            roles.retainAll(UPDATING_ROLES);
                            return roles.size() > 0;
                        })
                        .collect(Collectors.toList());
            }
        } else {
            acmNames = Authenticator.getInstance().getLocallyAvailablePrograms();
        }
        acmNames.sort(String::compareToIgnoreCase);
        choicesList.setListData(acmNames.toArray(new String[0]));

        // If there are no choices to be made, either program or sandbox, we're done.
        // - Only one program, not updatable, matches default (if any)
        if (acmNames.size() == 1 && !programsAllowingUpdates.contains(acmNames.get(0)) &&
            (StringUtils.isBlank(welcomeDialog.defaultProgram) ||
                acmNames.get(0).equals(welcomeDialog.defaultProgram))) {
            choicesList.setSelectedIndex(0); // so onOk() can find the proper value.
            onOk();
        } else if (StringUtils.isNotBlank(welcomeDialog.defaultProgram)) {
            // There are choices. If there's a pre-defined program, pre-select it.
            choicesList.setSelectedValue(welcomeDialog.defaultProgram, true);
        }
        // If there is only one name, select it.
        if (acmNames.size() == 1) {
            choicesList.setSelectedIndex(0);
            // Set the focus to the first actionable widget.
            if (welcomeDialog.options.contains(Authenticator.LoginOptions.OFFER_DEMO_MODE)) {
                forceSandbox.setRequestFocusEnabled(true);
                forceSandbox.requestFocusInWindow();
            } else {
                okButton.setRequestFocusEnabled(true);
                okButton.requestFocusInWindow();
            }
        } else {
            choicesList.setRequestFocusEnabled(true);
            choicesList.requestFocusInWindow();
        }
    }

    @Override
    void onEnter() {
        onOk();
    }

    void onOk() {
        welcomeDialog.setProgram(getSelectedItem());
        welcomeDialog.setSandboxSelected(forceSandbox.isSelected());
        ok();
    }

    /**
     * Mouse listener so we can accept a match on a double click.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final MouseListener listMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) onOk();
        }
    };

    /**
     * Listens for list selection, and enables the OK button whenever an item is selected.
     * @param listSelectionEvent isunused.
     */
    private void listSelectionListener(@SuppressWarnings("unused") ListSelectionEvent listSelectionEvent) {
        String selectedProgram = choicesList.getSelectedValue();
        boolean haveSelection = !StringUtils.isEmpty(selectedProgram);
        okButton.setEnabled(haveSelection);
        setListBorder();
        if (haveSelection) {
            boolean canUpdate = programsAllowingUpdates.contains(selectedProgram);
            if (canUpdate) {
                boolean suggestSandbox = welcomeDialog.options.contains(Authenticator.LoginOptions.SUGGEST_DEMO_MODE);
                forceSandbox.setEnabled(true);
                forceSandbox.setSelected(explicitSandbox!=null?explicitSandbox:suggestSandbox);
            } else {
                forceSandbox.setEnabled(false);
                forceSandbox.setSelected(true);
            }
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final FocusListener listFocusListener = new FocusListener() {
        @Override
        public void focusGained(FocusEvent e) {
            setListBorder();
        }

        @Override
        public void focusLost(FocusEvent e) {
            setListBorder();
        }
    };

    private void setListBorder() {
        boolean haveFocus = choicesList.hasFocus();
        boolean haveSelection = choicesList.getLeadSelectionIndex() >= 0;
        int ix = (haveSelection?0:2) + (haveFocus?0:1);
        choicesListScrollPane.setBorder(borders[ix]);
    }

    private final Color borderColor = new Color(136, 176, 220);
    private final RoundedLineBorder[] borders = {
        new RoundedLineBorder(borderColor, 2, 6),
        new RoundedLineBorder(borderColor, 1, 6, 2),
        new RoundedLineBorder(Color.RED, 2, 6),
        new RoundedLineBorder(Color.RED, 1, 6, 2)
    };

    public String getSelectedItem() {
        return choicesList.getSelectedValue();
    }

}
