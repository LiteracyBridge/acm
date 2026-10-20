package org.literacybridge.acm.gui.fx;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.literacybridge.acm.config.AmplioHome;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.fx.LoginController.PROGRAMS_INFO_FILE;

public class ProgramSelectionController {
    private Map<String, String> programsAndRolesForUser;
    private List<String> s3RepositoryList;
    private Map<String, String> programNames;
    public static final String FIELD_OFFICER_ROLE_STRING = "FO";
    private final Map<String, String> locallyAvailablePrograms = new HashMap<>();

    @FXML
    private ComboBox<String> programNamesCombobox;

    @FXML
    private Button openProgramBtn;

    @FXML
    public void initialize() {
        getProgramNames();

        programNamesCombobox.getItems().addAll(programNames.keySet());
        openProgramBtn.setDisable(false);
    }

    /**
     * Reads a JSONObject from the ~/Amplio/programs.info file or ~/LiteracyBridge/programs.info. Parses
     * the read JSONObject.
     */
    private void getProgramNames() {
        File programsInfoFile = new File(AmplioHome.getDirectory(), PROGRAMS_INFO_FILE);
        try (FileReader fr = new FileReader(programsInfoFile)) {
            JSONObject programsInfo = (JSONObject) JSONValue.parse(fr);
            parseProgramsInfoJSON(programsInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses a JSONObject of programs info. Populates 'userProgramsAndRoles', 's3RepositoryList', and 'programNames'
     *
     * @param programsInfo to be parsed.
     */
    @SuppressWarnings("unchecked")
    public void parseProgramsInfoJSON(JSONObject programsInfo) {
        String defaultProgram = "0";
        Map<String, Map<String, String>> programs_info = new HashMap<>();
        String implicit_repository = programsInfo.getOrDefault("implicit_repository", "dbx").toString();
        ((JSONObject) programsInfo.get("programs")).forEach((key, value) -> {
            Set<Map.Entry<Object, Object>> valueEntries = ((JSONObject) value).entrySet();
            Map<String, String> program_info = valueEntries.stream()
                    .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
            programs_info.put(key.toString(), program_info);
        });

        // Build map of {programid: "role,role"}
        programsAndRolesForUser = programs_info.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get("roles")));
        // Build a list of [programid] of those programs with s3 as repository.
        s3RepositoryList = programs_info.entrySet().stream()
                .filter(e -> e.getValue().getOrDefault("repository", implicit_repository).equalsIgnoreCase("s3"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        // Extract the friendly names as {programid: "name"}
        programNames = programs_info.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getOrDefault("name", e.getKey())));

        // If the "defaultProgram" is not an S3 program, then it is strictly local to the user's computer.
        // If the defaultProgram is not configured (on the server) for this user, simulate a FO
        // role in that program, so that it is possible to open (but not update) it.
        if (StringUtils.isNotBlank(defaultProgram) && !programsAndRolesForUser.containsKey(defaultProgram)
                && !s3RepositoryList.contains(defaultProgram)) {
            programsAndRolesForUser.put(defaultProgram, FIELD_OFFICER_ROLE_STRING);
            // We could look in the program's properties.config for the friendly name. But, since the user
            // had to pass the programid on the command line, it is probably slightly more user-friendly to
            // simply show the programid, and it is certainly cheaper.
            programNames.put(defaultProgram, defaultProgram);
        }

//        if (loginOptions.contains(Authenticator.LoginOptions.INCLUDE_FOUND_ACMS)) {
        // Add any locally available programs that werern't in the user's list, but
        // add them as read-only.
        locallyAvailablePrograms.keySet().stream()
                .filter(p -> !programsAndRolesForUser.containsKey(p))
                .forEach(p -> {
                    programsAndRolesForUser.put(p, FIELD_OFFICER_ROLE_STRING);
                    programNames.put(p, p);
                });
//        }
    }

    // TODO: move openAcmDb function from applicationv2 class here
//    @FXML private PasswordField newPasswordField, confirmPasswordField;
//    @FXML private Label errorLabel;
//    @FXML private Button resetButton;
//    @FXML private ProgressIndicator spinner;

//    @FXML
//    private void handleReset() {
//        String pw = newPasswordField.getText();
//        String confirm = confirmPasswordField.getText();
//
//        if (pw.length() < 8) {
//            showError("Password must be at least 8 characters.");
//        } else if (!pw.equals(confirm)) {
//            showError("Passwords do not match.");
//        } else {
//            errorLabel.setVisible(false);
//            errorLabel.setManaged(false);
//            // TODO: send new password to backend, then go back to login
//        }
//    }

//    private void showError(String msg) {
//        errorLabel.setText(msg);
//        errorLabel.setVisible(true);
//        errorLabel.setManaged(true);
//    }
}