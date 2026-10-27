package org.literacybridge.acm.gui.fx;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.config.*;
import org.literacybridge.acm.gui.ApplicationV2;
import org.literacybridge.acm.gui.dialogs.S3SyncDialog;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.fx.LoginController.PROGRAMS_INFO_FILE;

public class ProgramSelectionController {
    private static final Logger LOG = Logger
            .getLogger(ProgramSelectionController.class.getName());
    private Map<String, String> programsAndRolesForUser;
    private List<String> s3RepositoryList;
    private Map<String, String> programNames;
    public static final String FIELD_OFFICER_ROLE_STRING = "FO";
    private final Map<String, String> locallyAvailablePrograms = new HashMap<>();
    private static Image iconImage;

    @FXML
    private Pane rootPane;

    @FXML
    private ComboBox<String> comboBox;

    @FXML
    private Button openProgramBtn;

    public ProgramSelectionController() {
        URL iconURL = ApplicationV2.class.getResource("/tb.png");
        assert iconURL != null;
        iconImage = new ImageIcon(iconURL).getImage();
    }

    @FXML
    public void initialize() {
        getProgramNames();

        comboBox.getItems().addAll(programNames.values().stream().sorted().collect(Collectors.toList()));
        openProgramBtn.setDisable(false);
        openProgramBtn.disableProperty().bind(comboBox.selectionModelProperty().isNull());
    }

    @FXML
    private void openProgram() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        String selected = comboBox.getValue();
        String code = (programNames.entrySet().stream()
                .filter(entry -> selected.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()).orElseThrow();

        ApplicationV2.cliParams.sharedACM = code;

        System.out.println("Currently selected: " + code);
        openAcmDb(code, stage);

        try {
            if (ApplicationV2.cliParams.cleanUnreferenced) {
                ACMConfiguration.getInstance().getCurrentDB().cleanUnreferencedFiles();
            }

            LOG.log(Level.INFO, "ACM successfully started.");
            ACMConfiguration.getInstance().getCurrentDB().setupWavCaching(sizeMB -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("WAV Cache");
                alert.setHeaderText(null);
                alert.setContentText("The WAV cache is currently using " + sizeMB
                        + " MB disk space and a cleanup is recommended. Perform cleanup?");
                alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
                alert.initOwner(stage);

                Optional<ButtonType> result = alert.showAndWait();
                return result.isPresent() && result.get() == ButtonType.YES;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean isProgramS3(String program) {
        return s3RepositoryList != null && s3RepositoryList.contains(program);
    }

    /**
     * Gets a list of the programs that can be opened locally, ie, with no internet connection.
     * NOTE: Filters out programs that happen to be locally in Dropbox, but have been converted to S3
     * in the cloud.
     *
     * @return a list of programs that can be opened locally.
     */
    public List<String> getLocallyAvailablePrograms() {
        return locallyAvailablePrograms.keySet().stream()
                .filter(this::isProgramS3)
                .collect(Collectors.toList());

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

    /**
     * Opens the ACM database. If the ACM is in S3, performs a sync. If the ACM has never been downloaded, that
     * may take some time.
     *
     * @param sharedACM ACM to be opened.
     */
    private void openAcmDb(String sharedACM, Stage stage) {
        boolean syncOk = true;
        PathsProvider pathsProvider = ACMConfiguration.getInstance().getPathProvider(sharedACM);
        // Uncomment lines to debug opening from S3.
//      System.out.printf("Opening database '%s' in %s\n", sharedACM,
        if (isProgramS3(sharedACM)) {
//            splashScreen.setProgressLabel("Synchronizing content database...");
            if (pathsProvider == null || !pathsProvider.getProgramHomeDir().exists() || pathsProvider.getProgramHomeDir().listFiles().length == 0) {
//              System.out.println("pathsProvider is null");
                // The database doesn't exist locally, but it does exist in S3.
                syncOk = syncFromS3(sharedACM, S3SyncDialog.SYNC_STYLE.DOWNLOAD);
            } else {
//              System.out.println("Syncing...");
                // Mere sync required.
                syncOk = syncFromS3(sharedACM, S3SyncDialog.SYNC_STYLE.SYNC);
            }
        }

//        splashScreen.setProgressLabel("Opening content database...");
        // init database
        try {
            AccessControlResolver accessControlResolver = new GuiAccessControlResolver();
            String text = "Syncing database for " + sharedACM;
            ACMConfiguration.S3SyncState syncState = syncOk
                    ? ACMConfiguration.S3SyncState.NOT_REQUIRED
                    : ACMConfiguration.S3SyncState.FAILED;
            ACMConfiguration.getInstance().setCurrentDB(sharedACM, syncState, accessControlResolver);
        } catch (Exception e) {
            e.printStackTrace();

            Alert firstAlert = new Alert(Alert.AlertType.ERROR);
            firstAlert.setTitle("Database Error");
            firstAlert.setHeaderText(null);
            firstAlert.setContentText("Unable to connect to database. Please try restarting the ACM.");
            firstAlert.initOwner(stage);
            firstAlert.showAndWait();

//            System.exit(1);
        }
    }

    /**
     * Sync the given program with S3.
     *
     * @param program   The program to be synchronized.
     * @param syncStyle DOWNLOAD or SYNC
     * @return true if the sync completed successfully.
     */
    private boolean syncFromS3(String program,
                               S3SyncDialog.SYNC_STYLE syncStyle) {
        S3SyncDialog dialog = new S3SyncDialog(null, program, syncStyle);
        dialog.setIconImage(iconImage);
        dialog.go();
        ACMConfiguration.getInstance().discoverDB(program);
        return !dialog.hasSyncError();
    }
}