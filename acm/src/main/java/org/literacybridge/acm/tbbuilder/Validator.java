package org.literacybridge.acm.tbbuilder;

import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.tbloader.TBLoaderConstants;
import org.literacybridge.core.tbloader.TBLoaderUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.literacybridge.core.tbloader.TBLoaderConstants.RECIPIENTID_PROPERTY;

class Validator {

    private final TBBuilder tbBuilder;
    private final TBBuilder.BuilderContext builderContext;

    Validator(TBBuilder tbBuilder, TBBuilder.BuilderContext builderContext) {
        this.tbBuilder = tbBuilder;
        this.builderContext = builderContext;
    }

    /**
     * Validates that the packages and communities pass certain sanity tests. (See individual
     * verifications for details.)
     *
     * @throws IOException if a file can't be read.
     */
    void validateDeployment(List<TBBuilder.PackageInfo> packages) throws IOException {
        boolean strictNaming = ACMConfiguration.getInstance().getCurrentDB().isStrictDeploymentNaming();
        if (strictNaming) {
            // Validate that the deployment is listed in the deployments.csv file.
            File deploymentsList = new File(builderContext.sourceProgramspecDir, "deployments.csv");
            if (deploymentsList.exists()) {
                boolean found = false;
                FileReader fileReader = new FileReader(deploymentsList);
                CSVReader csvReader = new CSVReader(fileReader);


                String nextDeploymentName = null;
                String prevDeploymentName = null;
                int deploymentIx = -1;
                int deploymentNumberIx = -1;
                int startDateIx = -1;
                String[] line = csvReader.readNext();
                for (int ix = 0; ix < line.length; ix++) {
                    if (line[ix].equalsIgnoreCase("deployment")) {
                        deploymentIx = ix;
                    }
                    else if (line[ix].equalsIgnoreCase("deploymentnumber")) {
                        deploymentNumberIx = ix;
                    }
                    else if (line[ix].equalsIgnoreCase("startdate")) {
                        startDateIx = ix;
                    }
                }

                while ((line = csvReader.readNext()) != null) {
                    // Look for the given deployment in the deployments.csv. Also look for a
                    // likely deployment name, either the next deployment after today, or the
                    // last deployment in the list. This won't be the right one when re-building
                    // the current deployment, but the prompt should give a good hint as to what
                    // the name should be.


                    if (deploymentIx < 0) {
                        if (deploymentNumberIx >= 0 && Utils.isAcceptableNameForDeployment(builderContext.deploymentName,
                                builderContext.project,
                                line[deploymentNumberIx])) {
                            found = true;
                            break;
                        }
                    }
                    else {
                        if (line[deploymentIx].equalsIgnoreCase(builderContext.deploymentName)) {
                            found = true;
                            break;
                        }
                        else if (StringUtils.isEmpty(nextDeploymentName)) {
                            DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
                            try {
                                Date startDate = df1.parse(line[startDateIx]);
                                if (startDate.after(new Date())) {
                                    nextDeploymentName = line[deploymentIx];
                                }
                                else {
                                    prevDeploymentName = line[deploymentIx];
                                }
                            } catch (ParseException e) {
                                // Not a valid iso8601 string. Ignore it.
                            }
                        }
                    }
                }
                if (!found) {
                    String invalidMessage = String.format("'%s' is not a valid deployment for ACM '%s'.",
                            builderContext.deploymentName,
                            builderContext.project);
                    if (StringUtils.isNotEmpty(nextDeploymentName) || StringUtils.isNotEmpty(prevDeploymentName)) {
                        String name = StringUtils.defaultIfEmpty(nextDeploymentName, prevDeploymentName);
                        invalidMessage += " (Did you mean '" + name + "'?)";
                    }
                    builderContext.fatalMessages.add(invalidMessage);
                }
            }
        }

        // Get all of the languages and groups in the Deployment. As we iterate over the
        // packages and their langugages, validate the package/language combinations.
        Set<String> groups = new HashSet<>();
        Set<String> languages = new HashSet<>();

        for (TBBuilder.PackageInfo pi : packages) {
            validatePackageForLanguage(pi);
            languages.add(pi.language);
            Collections.addAll(groups, pi.groups);
        }

        validateCommunities(new File(builderContext.sourceTbLoadersDir, "communities"), languages, groups);

        // If there are errors or warnings, print them and let user decide whether to continue.
        if (builderContext.fatalMessages.size() > 0 || builderContext.errorMessages.size() > 0 || builderContext.warningMessages
                .size() > 0) {
            if (builderContext.fatalMessages.size() > 0) {
                System.err.printf(
                        "%n%n********************************************************************************%n");
                System.err.printf("%d Fatal Error(s) found in Deployment:%n", builderContext.fatalMessages.size());
                for (String msg : builderContext.fatalMessages)
                    System.err.println(msg);
            }
            if (builderContext.errorMessages.size() > 0) {
                System.err.printf(
                        "%n%n================================================================================%n");
                System.err.printf("%d Error(s) found in Deployment:%n", builderContext.errorMessages.size());
                for (String msg : builderContext.errorMessages)
                    System.err.println(msg);
                if (builderContext.errorCommunities.size() > 0) {
                    System.err.printf("%nThe following communities may not work properly with this Deployment:%n");
                    for (String community : builderContext.errorCommunities) {
                        System.err.printf("'%s' ", community);
                    }
                    System.err.printf("%n");
                }
                if (builderContext.errorLanguages.size() > 0) {
                    System.err.printf("%nThe following languages may not work properly with this Deployment:%n");
                    for (String community : builderContext.errorLanguages) {
                        System.err.printf("'%s' ", community);
                    }
                    System.err.printf("%n");
                }
            }
            if (builderContext.warningMessages.size() > 0) {
                System.err.printf(
                        "%n%n--------------------------------------------------------------------------------%n");
                System.err.printf("%d Warning(s) found in Deployment:%n", builderContext.warningMessages.size());
                for (String msg : builderContext.warningMessages)
                    System.err.println(msg);
            }

            if (builderContext.fatalMessages.size() > 0) {
                System.err.printf("%n%nCannot continue, aborting.%n");
                System.exit(1);
            }
            System.err.printf("%n%nDo you want to continue (y/N)? ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String response = br.readLine().trim();
            if (response.length() == 0 || response.toLowerCase().charAt(0) != 'y') {
                System.err.println("Exiting.");
                System.exit(1);
            }
            System.err.printf("%nContinuing with %s.%n%n",
                    builderContext.errorMessages.size() > 0 ? "errors" : "warnings");

        }
    }


    /**
     * Checks the exported package for the given language. Checks that the system prompt
     * recordings exist in TB-Loaders/TB_Options/languages/{language}/cat, and that the
     * playlist's list file exists.
     * <p>
     * If any file is missing, prints an error message and exits.
     *
     * @param pi Information about the package: name, language, groups
     *           (TB-Loaders/packages/{name}/messages/lists/1/)
     *           containing the _activeLists.txt and individual playlist .txt
     *           list files.
     */
    private void validatePackageForLanguage(TBBuilder.PackageInfo pi) {
        // Get the directory containing the _activeLists.txt file plus the playlist files (like "2-0.txt")
        String listsPath =
                "packages/" + pi.name + "/messages/lists/" + TBBuilder.firstMessageListName;
        File sourceListsDir = new File(builderContext.sourceTbLoadersDir, listsPath);

        // Get the directory with the system prompt recordings for the language.
        String languagesPath = "TB_Options" + File.separator + "languages";
        File languagesDir = new File(builderContext.sourceTbLoadersDir, languagesPath);
        File languageDir = IOUtils.FileIgnoreCase(languagesDir, pi.language);
        File promptsDir = new File(languageDir, "cat");

        // Read the source _activeLists.txt file.
        File activeList = new File(sourceListsDir, "_activeLists.txt");
        if (!activeList.exists()) {
            builderContext.fatalMessages.add(
                    String.format("File '%s' not found for Package '%s'.", activeList.getName(),
                            pi.name));
        }
        else {
            //read file into stream, try-with-resources
            try (BufferedReader br = new BufferedReader(new FileReader(activeList))) {
                String line;
                boolean foundUserFeedback = false;
                while ((line = br.readLine()) != null) {
                    // '!' means subject is locked.
                    if (line.charAt(0) == '!') { line = line.substring(1); }
                    line = line.trim();
                    if (line.length() < 1) { continue; }

                    if (line.equals(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK)) {
                        foundUserFeedback = true;
                    }

                    // We have the category, ensure the system prompt exists.
                    File p1 = new File(promptsDir, line + ".a18");
                    File p2 = new File(promptsDir, "i" + line + ".a18");
                    if (!p1.exists()) {
                        builderContext.errorMessages.add(
                                String.format("Missing category prompt for %s in language %s.", line,
                                        pi.language));
                        builderContext.errorLanguages.add(pi.language);
                    }
                    if (!p2.exists()) {
                        builderContext.errorMessages.add(
                                String.format("Missing long category prompt for %s in language %s.", line,
                                        pi.language));
                        builderContext.errorLanguages.add(pi.language);
                    }

                    // Be sure there is a .txt list file for the category. We don't check for
                    // uncategorized feedback because it will be created on demand, and we
                    // don't check for the TB long description because it is provided in
                    // the languages directory.
                    if (!line.equals(Constants.CATEGORY_UNCATEGORIZED_FEEDBACK) && !line.equals(Constants.CATEGORY_TUTORIAL)) {
                        File pList = new File(sourceListsDir, line + ".txt");
                        if (!pList.exists()) {
                            builderContext.errorMessages.add(
                                    String.format("Missing playlist file '%s.txt', for Package '%s', language '%s'.",
                                            line, pi.name, pi.language));
                            builderContext.errorLanguages.add(pi.language);
                        }
                    }
                }
                String[] required_messages = TBBuilder.REQUIRED_SYSTEM_MESSAGES;
                for (String prompt : required_messages) {
                    File p1 = new File(languageDir, prompt + ".a18");
                    if (!p1.exists()) {
                        builderContext.errorMessages.add(
                                String.format("Missing system message for %s in language %s.", prompt,
                                        pi.language));
                        builderContext.errorLanguages.add(pi.language);
                    }
                }
                // A firmware update may be required to support hidden user feedback. Check the
                // version currently in the project.
                if (!foundUserFeedback) {
                    String image = tbBuilder.utils.latestFirmwareImage().getName().toLowerCase();
                    if (image.compareTo(TBBuilder.MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE) < 0) {
                        builderContext.fatalMessages.add(String.format(
                                "Minimum firmware image for hidden user feedback is %s, but found %s.",
                                TBBuilder.MINIMUM_USER_FEEDBACK_HIDDEN_IMAGE,
                                image));
                    }
                }

                // If User Feedback will be hidden, warn the user of that fact, in case it is unexpected.
                if (!foundUserFeedback) {
                    builderContext.warningMessages.add(String.format(
                            "User Feedback WILL BE HIDDEN for Package '%s' language '%s'.",
                            pi.name,
                            pi.language));
                }
            } catch (Exception ex) {
                builderContext.errorMessages.add(
                        String.format("Exception reading _activeLists.txt for Package '%s' language '%s': %s",
                                pi.name, pi.language, ex.getMessage()));
                builderContext.errorLanguages.add(pi.language);
            }
        }
    }

    /**
     * Validates the Deployment for the project's communities:
     * For every directory in {project}/TB-Loaders/communities
     * -- look for a languages directory
     * --- look for individual language subdirectories ('en', 'dga', etc.)
     * --- ensure that at least one language is in the Deployment languages
     * ---- find any greetings (10.a18) files in the language directory
     * warn in no greetings
     * -- look for a system directory
     * --- look for the individual group files (dga.grp, default.grp, etc.)
     * --- ensure that at least one group is in the Deployment groups
     *
     * @param communitiesDir 'communities' directory, contains one directory per community
     * @param languages      in the Deployment, lowercase
     * @param groups         in the Deployment, lowercase
     */
    private void validateCommunities(
            File communitiesDir, Set<String> languages, Set<String> groups) {
        Map<String, List<String>> foundRecipientIds = new HashMap<>();
        File[] communities = communitiesDir.listFiles();
        if (communities == null || communities.length == 0) {
            builderContext.errorMessages.add("Missing or empty directory: " + communitiesDir.getAbsolutePath());
            return;
        }
        for (File c : communities) {
            if (c.exists() && c.isDirectory()) {
                boolean foundLanguage = false;
                boolean oneLanguage = false;
                boolean foundGreeting = false;
                boolean foundGroup = false;
                // Examine the community directory. We *must* find languages/{lang} where lang is
                // in {languages}, and we *want* to find languages/{lang}/10.a18
                // But, if the custom greeting is missing, we still have the default 10.a18.
                File languagesDir = new File(c, "languages");
                if (!languagesDir.exists() || !languagesDir.isDirectory()) {
                    builderContext.errorMessages.add(String.format(
                            "Missing or empty 'languages' directory for community '%s'.",
                            c.getName()));
                    builderContext.errorCommunities.add(c.getName());
                }
                else {
                    // Look for individual language directories, 'en', 'dga', ...
                    File[] langs = languagesDir.listFiles(File::isDirectory);
                    oneLanguage = langs != null && langs.length == 1;
                    for (File lang : Objects.requireNonNull(langs)) {
                        // Look for a greeting in the language.
                        if (lang.exists()) {
                            String languageName = lang.getName().toLowerCase();
                            if (languages.contains(languageName)) {
                                foundLanguage = true;
                                foundGreeting |= IOUtils.FileIgnoreCase(lang, "10.a18").exists();
                            }
                        }
                    }
                }
                // We *must* find system / {group}.grp
                File systemDir = new File(c, "system");
                if (!systemDir.exists() || !systemDir.isDirectory()) {
                    builderContext.errorMessages.add(String.format(
                            "Missing or empty 'system' directory for community '%s'.",
                            c.getName()));
                    builderContext.errorCommunities.add(c.getName());
                }
                else {
                    // Look for .grp files.
                    File[] grps = systemDir.listFiles((dir, name) -> name.toLowerCase()
                            .endsWith(TBLoaderConstants.GROUP_FILE_EXTENSION));
                    for (File grp : Objects.requireNonNull(grps)) {
                        String groupName = StringUtils.substring(grp.getName().toLowerCase(), 0,
                                -4);
                        foundGroup |= groups.contains(groupName);
                    }
                }
                // Validate recipientid, if present, is unique.
                String recipientid = TBLoaderUtils.getRecipientProperty(c, RECIPIENTID_PROPERTY);
                if (recipientid != null) {
                    List<String> recips = foundRecipientIds.computeIfAbsent(recipientid, k -> new ArrayList<>());
                    recips.add(c.getName());
                }
                if (!foundLanguage) {
                    builderContext.errorMessages.add(
                            String.format("Community '%s' does not have any language in the Deployment.",
                                    c.getName()));
                    builderContext.errorCommunities.add(c.getName());
                }
                else {
                    if (!foundGreeting) {
                        builderContext.warningMessages.add(
                                String.format("No custom greeting is for community '%s'.", c.getName()));
                    }
                    if (!oneLanguage) {
                        builderContext.warningMessages.add(
                                String.format("Community '%s' has multiple languages.", c.getName()));
                    }
                }
                if (!foundGroup) {
                    builderContext.errorMessages.add(
                            String.format("Community '%s' is not in any group in the Deployment.",
                                    c.getName()));
                    builderContext.errorCommunities.add(c.getName());
                }
            }
        }
        for (Map.Entry<String, List<String>> e : foundRecipientIds.entrySet()) {
            if (e.getValue().size() > 1) {
                String dirs = String.join(", ", e.getValue());
                String msg = String.format("Recipientid %s found in multiple communities: %s",
                        e.getKey(), dirs);
                builderContext.fatalMessages.add(msg);
            }
        }
    }



}
