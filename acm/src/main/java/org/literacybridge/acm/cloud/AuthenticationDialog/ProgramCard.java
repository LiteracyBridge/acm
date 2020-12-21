package org.literacybridge.acm.cloud.AuthenticationDialog;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.PanelButton;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.WEST;
import static org.literacybridge.acm.cloud.Authenticator.LoginOptions.NO_WAIT;
import static org.literacybridge.acm.cloud.Authenticator.UPDATING_ROLES;
import static org.literacybridge.acm.cloud.Authenticator.ALL_USER_ROLES;
import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class ProgramCard extends CardContent {
    private static final String DIALOG_TITLE = "Select Program";
    protected static final int CARD_HEIGHT = 315;

    private final PanelButton okButton;
    private final JList<String> choicesList;
    private final JCheckBox forceSandbox;
    private final JScrollPane choicesListScrollPane;
    private List<String> allowedToBeUpdated = new ArrayList<>();
    private List<String> mustBeDownloaded;

    private final Map<String, String> descriptionToProgramid = new HashMap<>();
    private Set<String> shownProgramids;
    private boolean chooseByDescription = true;
    public static boolean giveChooserChoice = true; // an easy way to essentially #ifdef.

    public ProgramCard(WelcomeDialog welcomeDialog,
            WelcomeDialog.Cards panel) {
        super(welcomeDialog, DIALOG_TITLE, panel);
        JPanel dialogPanel = this;

        // The GUI
        dialogPanel.setLayout(new GridBagLayout());
        GBC gbc = new GBC(getGBC()).withAnchor(CENTER);
        gbc.insets.bottom = 12; // tighter bottom spacing.

        // Amplio logo
        addScaledLogo();

        JLabel promptLabel = new JLabel("<html>Choose the ACM to open.</html>");
        if (giveChooserChoice) {
            Box promptBox = Box.createHorizontalBox();
            promptBox.add(promptLabel);
            promptBox.add(Box.createHorizontalGlue());

            JCheckBox useProgramid = new JCheckBox("Choose by Description.", chooseByDescription);
            useProgramid.addActionListener(this::chooseByDescriptionClicked);
            promptBox.add(useProgramid);

            dialogPanel.add(promptBox, gbc);
        } else {
            dialogPanel.add(promptLabel, gbc);
        }

        choicesList = new JList<>();
        choicesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        choicesList.addListSelectionListener(this::listSelectionListener);
        choicesList.addFocusListener(listFocusListener);
        choicesList.addMouseListener(listMouseListener);
        choicesList.setCellRenderer(listCellRenderer);

        choicesListScrollPane = new JScrollPane(choicesList);
        dialogPanel.add(choicesListScrollPane, gbc.withWeighty(1.0).withFill(BOTH));

        forceSandbox = new JCheckBox("Use demo mode");
        forceSandbox.addActionListener(this::onSandboxClicked);
        if (welcomeDialog.options.contains(Authenticator.LoginOptions.OFFER_DEMO_MODE)) {
            dialogPanel.add(forceSandbox, gbc.withAnchor(WEST).withFill(NONE));
            // If not authenticated (roughly ~offline), sandbox is the only option, so don't let
            // the user change it.
            if (!welcomeDialog.cognitoInterface.isAuthenticated()) {
                forceSandbox.setSelected(true);
                forceSandbox.setEnabled(false);
            }
            forceSandbox.setToolTipText(
                    "In DEMO mode (or TRAINING mode), you can make changes, but they will be discarded when you are done.");
        }

        okButton = new PanelButton("Ok");
        okButton.setFont(getTextFont());
        okButton.setBgColorPalette(AMPLIO_GREEN);
        okButton.addActionListener(e -> onOk());
        okButton.setEnabled(false);
        dialogPanel.add(okButton, gbc.withFill(NONE));

        addComponentListener(componentAdapter);
    }

    private void chooseByDescriptionClicked(ActionEvent actionEvent) {
        String selectedProgramid = getSelectedProgramid();
        chooseByDescription = ((JCheckBox)actionEvent.getSource()).isSelected();
        onShown(null);
        selectProgramid(selectedProgramid);
    }

    Boolean explicitSandbox = null; // An explicit sandbox choice, if user made one.
    private void onSandboxClicked(ActionEvent actionEvent) {
        explicitSandbox = forceSandbox.isSelected();
    }

    @Override
    void onShown(ActionEvent actionEvent) {
        super.onShown(actionEvent);
        findProgramidsToShow();
        fillChoicesList();

        // If there are no choices to be made, either program or sandbox, we're done.
        // - Only one program, not updatable, matches default (if any)
        List<String> programidList = new ArrayList<>(shownProgramids);
        if (programidList.size() == 1 && !allowedToBeUpdated.contains(programidList.get(0)) &&
            (StringUtils.isBlank(welcomeDialog.defaultProgram) ||
                programidList.get(0).equals(welcomeDialog.defaultProgram))) {
            choicesList.setSelectedIndex(0); // so onOk() can find the proper value.
            onOk();
        } else if (StringUtils.isNotBlank(welcomeDialog.defaultProgram)) {
            // There are choices. If there's a pre-defined program, pre-select it.
            selectProgramid(welcomeDialog.defaultProgram);
        }
        // If there is only one name, select it.
        if (shownProgramids.size() == 1) {
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

        // If no_wait is specified, and we have everything we need, go!
        if (welcomeDialog.options.contains(NO_WAIT) && okButton.isEnabled()) onOk();

    }

    /**
     * Select the given program id in the choices list. If the dialog is currently set to "chooseByDescription",
     * this means to look up the description for the programid, and select that.
     * @param programidToSelect to be selected in the choices list.
     */
    private void selectProgramid(String programidToSelect) {
        if (chooseByDescription) {
            // Use descriptionToProgramid because the descriptions in the choice list may have been decorated, and
            // we need to account for that.
            String descriptionToSelect = descriptionToProgramid.entrySet().stream()
                .filter(e -> e.getValue().equals(programidToSelect))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");
            choicesList.setSelectedValue(descriptionToSelect, true);
        } else {
            choicesList.setSelectedValue(programidToSelect, true);
        }

    }

    /**
     * Find the programids to be shown to the user, from which they can make a selection. The list is built
     * depending on the calling application and whether the user is authenticated or not. (We use authentication
     * as a proxy for "online".) When not authenticated (when offline), all local programs are offered. We never
     * want to prevent someone doing their job because of network issues; really matters for the TB-Loader.
     *
     * Presumably, if a program is downloaded to the local machine, the user has the right to use it.
     */
    private void findProgramidsToShow() {
        String ALL_ROLES = String.join(",", ALL_USER_ROLES);
        // Build the list of ACM names from which to choose. The list depends on whether we're
        // authenticated (not being authenticated is roughly equivalent to not having network
        // access, so we give access to all local programs). The list also depends on the option
        // LOCAL_DATA_ONLY, because the ACM can only work with local data.
        //
        if (welcomeDialog.cognitoInterface.isAuthenticated()) {
            Map<String, String> programs = welcomeDialog.cognitoInterface.getProgramRoles();
            shownProgramids = programs.keySet();
            // If data must already be local, filter the list to those ACMs that are local.
            Map<String, String> localPrograms = welcomeDialog.cognitoInterface.getLocallyAvailablePrograms();
            Set<String> localProgramids = localPrograms.keySet();
            if (welcomeDialog.options.contains(Authenticator.LoginOptions.LOCAL_DATA_ONLY)) {
                final Set<String> usersPrograms = shownProgramids;
                shownProgramids = localProgramids.stream()
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
                    .collect(Collectors.toSet());
            }
            // If updating (not sandboxing) is an option, determine which ACMs will be sandbox
            // only, and which allow a choice.
            if (welcomeDialog.options.contains(Authenticator.LoginOptions.OFFER_DEMO_MODE)) {
                allowedToBeUpdated = shownProgramids
                    .stream()
                    .filter(name -> {
                        Set<String> roles = Arrays.stream(programs.getOrDefault(name, ALL_ROLES).split(","))
                            .collect(Collectors.toSet());
                        roles.retainAll(UPDATING_ROLES);
                        return roles.size() > 0;
                    })
                    .collect(Collectors.toList());
            }

            mustBeDownloaded = shownProgramids.stream()
                .filter(name -> {
                    // If the program is not available locally, or it is locally dropbox but is really an s3 program,
                    // then it must be downloaded
                    return !localProgramids.contains(name) ||
                        (Authenticator.getInstance().isLocallyDropbox(name) && Authenticator.getInstance().isProgramS3(name));
                }).collect(Collectors.toList());

        } else {
            shownProgramids = welcomeDialog.cognitoInterface.getLocallyAvailablePrograms().keySet();
        }
    }

    /**
     * Fill the choices list from "shownProgramids". If using "choose by description", we will need to
     * show the description for each programid.
     */
    int widest = -1;
    private void fillChoicesList() {
        String[] choices;
        descriptionToProgramid.clear();

        if (chooseByDescription) {
            Map<String, String> programDescriptions;
            if (welcomeDialog.cognitoInterface.isAuthenticated()) {
                programDescriptions = welcomeDialog.cognitoInterface.getProgramDescriptions();
            } else {
                programDescriptions = welcomeDialog.cognitoInterface.getLocallyAvailablePrograms();
            }
            descriptionToProgramid.putAll(reverseDescriptionsMap(programDescriptions));
            choices = descriptionToProgramid.entrySet().stream()
                .filter(e -> shownProgramids.contains(e.getValue()))
                .map(Map.Entry::getKey)
                .sorted(String::compareToIgnoreCase)
                .toArray(String[]::new);
            if (widest < 0) {
                // A description string can easily be wider than the selection dialog. The first time through here,
                // resize the dialog to fit. (Programids are always short, and always fit.)
                Graphics g = choicesList.getGraphics();
                Font f = choicesList.getFont();
                FontMetrics fm = g.getFontMetrics(f);
                widest = Arrays.stream(choices)
                    .map(s -> {
                        Rectangle2D r = fm.getStringBounds(s, g);
                        return (int) r.getWidth();
                    })
                    .max(Integer::compareTo).orElse(1);
                resizeForWidth(widest);
            }
        } else {
            choices = shownProgramids.stream()
                .sorted(String::compareToIgnoreCase)
                .toArray(String[]::new);
        }

        choicesList.setListData(choices);
    }

    /**
     * Given a map of {programid: description}, build a map of {description: programid}. Because the descriptions
     * are not required to be unique, if there are collisions, decorate with the (programid).
     *
     * @param descriptions to be reverse-mapped.
     * @return the reverse map.
     */
    private Map<String,String> reverseDescriptionsMap(Map<String,String> descriptions) {
        // Find any description used for multiple programs...
        Map<String, String> reverseMap = new HashMap<>();
        Set<String> needsQualification = new HashSet<>();
        for (Map.Entry<String, String> item : descriptions.entrySet()) {
            String programid = item.getKey();
            String description = item.getValue().toLowerCase(Locale.ROOT);
            if (reverseMap.containsKey(description)) {
                needsQualification.add(programid);
                needsQualification.add(reverseMap.get(description));
            } else {
                reverseMap.put(description, programid);
            }
        }
        // Rebuild the map without changing the case, and decorating with programid for uniqueness.
        reverseMap.clear();
        for (Map.Entry<String, String> item : descriptions.entrySet()) {
            // key==programid, value==description
            String programid = item.getKey();
            String description = item.getValue();
            if (needsQualification.contains(programid)) {
                // Decorate description with "... (programid)"
                String newDescription = String.format("%s (%s)", description, programid);
                reverseMap.put(newDescription, programid);
            } else {
                reverseMap.put(description, programid);
            }
        }
        return reverseMap;
    }

    @Override
    void onEnter() {
        onOk();
    }

    void onOk() {
        welcomeDialog.setProgram(getSelectedProgramid());
        welcomeDialog.setSandboxSelected(forceSandbox.isSelected());
        ok();
    }

    private List<String> getProgramIdsToShow() {
        List<String> localProgramIds = Authenticator.getInstance().getLocallyAvailablePrograms();
        boolean localOnly = welcomeDialog.options.contains(Authenticator.LoginOptions.LOCAL_DATA_ONLY);
        boolean localOrS3 = welcomeDialog.options.contains(Authenticator.LoginOptions.LOCAL_OR_S3);
        boolean includeUF = welcomeDialog.options.contains(Authenticator.LoginOptions.INCLUDE_FB_ACMS);

        // If data must already be local, filter the list to those ACMs that are local.
        // If "LOCAL_OR_S3" means local or S3 cloud, from whence we can download it.
        Predicate<String> filter = name -> {
            int fb = name.indexOf("-FB-");
            if (fb > 0) {
                // Use the part before the "-FB-" to determine if one of the user's programs?
                if (includeUF) { name = name.substring(0, fb); } else { return false; }
            }
            if (localProgramIds.contains(name)) return true;
            if (localOnly) return false;
            return !localOrS3 || Authenticator.getInstance().isProgramS3(name);
        };

        List<String> usersPrograms = welcomeDialog.cognitoInterface.getProgramRoles().keySet().stream()
                .filter(filter)
                .collect(Collectors.toList());

        mustBeDownloaded = usersPrograms.stream()
                .filter(name -> {
                    // If the program is not available locally, or it is locally dropbox but is really an s3 program,
                    // then it must be downloaded
                    return !localProgramIds.contains(name) ||
                            (Authenticator.getInstance().isLocallyDropbox(name) && Authenticator.getInstance().isProgramS3(name));
                }).collect(Collectors.toList());
        return usersPrograms;
    }

    /**
     * A renderer to add "(requires download)" to programs that can be opened, but aren't local, and therefore
     * must be downloaded.
     */
    ListCellRenderer<? super String> listCellRenderer = new DefaultListCellRenderer() {
        private void decorateDownloads(JLabel toBeDecorated, boolean decorate) {
            if (decorate) {
                String newText = String.format("<html>%s<em> (requires download)</em></html>", toBeDecorated.getText());
                toBeDecorated.setText(newText);
            }
        }

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String name = list.getModel().getElementAt(index).toString();
            decorateDownloads((JLabel) result, mustBeDownloaded.contains(name));
            return result;
        }
    };


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
     *
     * @param listSelectionEvent isunused.
     */
    private void listSelectionListener(@SuppressWarnings("unused") ListSelectionEvent listSelectionEvent) {
        String selectedProgram = getSelectedProgramid();
        boolean haveSelection = !StringUtils.isEmpty(selectedProgram);
        okButton.setEnabled(haveSelection);
        setListBorder();
        if (haveSelection) {
            boolean canUpdate = allowedToBeUpdated.contains(selectedProgram);
            if (canUpdate) {
                boolean suggestSandbox = welcomeDialog.options.contains(Authenticator.LoginOptions.SUGGEST_DEMO_MODE);
                forceSandbox.setEnabled(true);
                forceSandbox.setSelected(explicitSandbox != null ? explicitSandbox : suggestSandbox);
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

    /**
     * Sets the program list border. A wider border to indicate focus, and red to indicate that a selection
     * is still needed.
     */
    private void setListBorder() {
        boolean haveFocus = choicesList.hasFocus();
        boolean haveSelection = choicesList.getLeadSelectionIndex() >= 0;
        int ix = (haveSelection ? 0 : 2) + (haveFocus ? 0 : 1);
        choicesListScrollPane.setBorder(borders[ix]);
    }

    private final Color borderColor = new Color(136, 176, 220);
    private final RoundedLineBorder[] borders = {
            new RoundedLineBorder(borderColor, 2, 6),
            new RoundedLineBorder(borderColor, 1, 6, 2),
            new RoundedLineBorder(Color.RED, 2, 6),
            new RoundedLineBorder(Color.RED, 1, 6, 2)
    };

    /**
     * Get the selected programid. If using chooseByDescription, translate the chosen description to
     * the corresponding program id.
     * @return the currently selected programid.
     */
    public String getSelectedProgramid() {
        String selectedValue =  choicesList.getSelectedValue();
        return chooseByDescription ? descriptionToProgramid.get(selectedValue) : selectedValue;
    }

}
