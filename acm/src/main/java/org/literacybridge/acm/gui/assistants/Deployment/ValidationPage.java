package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.ContentImport.WelcomePage;
import org.literacybridge.acm.gui.assistants.Deployment.DeploymentContext.PlaylistPrompts;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.Playlist;
import org.literacybridge.acm.tbbuilder.TBBuilder;
import org.literacybridge.acm.utils.IOUtils;
import org.literacybridge.core.spec.Content;
import org.literacybridge.core.spec.RecipientList;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.assistants.ContentImport.WelcomePage.qualifiedPlaylistName;

public class ValidationPage extends AssistantPage<DeploymentContext> {

    private final JLabel deployment;

    private DeploymentContext context;

    private MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

    private RecipientList recipients;
    private Set<String> languages;
    private Map<String, List<Content.Playlist>> allProgramSpecPlaylists;
    private Map<String, List<Playlist>> allAcmPlaylists;

    ValidationPage(PageHelper listener) {
        super(listener);
        context = getContext();
        setLayout(new GridBagLayout());

        Insets insets = new Insets(0, 0, 20, 0);
        GridBagConstraints gbc = new GridBagConstraints(0,
            GridBagConstraints.RELATIVE,
            1,
            1,
            1.0,
            0.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.HORIZONTAL,
            insets,
            1,
            1);

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Validation</span>" + "</ul>"
                + "<br/>Examine any issues, and click \"Next\" if you wish to proceed. "

                + "</html>");
        add(welcome, gbc);

        Border greyBorder = new LineBorder(Color.green); //new LineBorder(new Color(0xf0f0f0));
        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Importing message content for deployment "));
        deployment = new JLabel();
        deployment.setOpaque(true);
        deployment.setBackground(Color.white);
        deployment.setBorder(greyBorder);
        hbox.add(deployment);
        hbox.add(Box.createHorizontalGlue());
        hbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(hbox, gbc);

        gbc.weighty = 1.0;
        add(new JLabel(), gbc);
    }

    /**
     * Called when a selection changes.
     *
     * @param actionEvent is ignored.
     */
    private void onSelection(ActionEvent actionEvent) {
        setComplete();
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        deployment.setText(Integer.toString(context.deploymentNo));

        if (progressing) {
            collectDeploymentInformation(context.deploymentNo);
        }

        validateDeployment(context.deploymentNo);
    }

    @Override
    protected String getTitle() {
        return "Validate the Deployment";
    }

    private void issue(String issue, Object... args) {
        System.out.println(String.format(issue, args));
    }

    private void collectDeploymentInformation(int deploymentNo) {
        recipients = context.programSpec.getRecipientsForDeployment(deploymentNo);
        languages = recipients.stream()
            .map(r -> r.language)
            .collect(Collectors.toSet());

        allProgramSpecPlaylists = getProgramSpecPlaylists(deploymentNo, languages);
        allAcmPlaylists = getAcmPlaylists(deploymentNo, languages);

    }

    private void validateDeployment(int deploymentNo) {

        // Get the languages, and playlists for each language.
        // Check that all languages are a language in the ACM.
        validateDeploymentLanguages(languages);

        // Check that we have all messages in all playlists; check if anything was added.
        validatePlaylists(deploymentNo, languages);

        // Check that we have all system prompts in all languages.
        validateSystemPrompts(languages);

        // Check that we have all category prompts for categories in each language.
        validatePlaylistPrompts(languages);

        // Check that we have all recipient prompts for recipients in the deployment.

    }

    private void validateDeploymentLanguages(Collection<String> languages) {
        List<Locale> locales = ACMConfiguration.getInstance().getCurrentDB().getAudioLanguages();
        for (String language : languages) {
            Locale locale = new Locale(language);
            if (!locales.contains(locale)) {
                issue("Language '%s' is used in the Deployment, but is not defined in the ACM.", language);
            }
        }
    }

    /**
     * Validate the Playlists in a Deployment. Each language is evaluated separately, both because
     * playlists can legitimately differ between languages, and because there will be different
     * errors between languages.
     * @param deploymentNo deployment # to be validated.
     * @param languages list of languages in the deployment.
     */
    private void validatePlaylists(int deploymentNo, Collection<String> languages) {
        // For each language in the Deployment...
        for (String language : languages) {
            // Get the Program Spec playlists, and the ACM playlists (that match by pattern).
            List<Content.Playlist> programSpecPlaylists = allProgramSpecPlaylists.get(language);
            List<Playlist> acmPlaylists = allAcmPlaylists.get(language);

            // For all the playlists in the Program Spec...
            Set<Playlist> foundPlaylists = new HashSet<>();
            for (Content.Playlist programSpecPlaylist : programSpecPlaylists) {
                // Get the corresponding ACM playlist, if it exists.
                String qualifiedPlaylistName = qualifiedPlaylistName(programSpecPlaylist.getPlaylistTitle(),
                    deploymentNo,
                    language);
                Playlist playlist = acmPlaylists.stream()
                    .filter(p -> p.getName().equals(qualifiedPlaylistName))
                    .findFirst().orElse(null);
                // If the ACM playlist is missing, note the issue.
                if (playlist == null) {
                    issue("A Playlist is missing in the ACM: %s", qualifiedPlaylistName);
                } else {
                    // ...otherwise, we have the playlist, so check the contents.
                    foundPlaylists.add(playlist);
                    Set<AudioItem> foundItems = new HashSet<>();
                    // For the messages in the Program Spec...
                    for (Content.Message message : programSpecPlaylist.getMessages()) {
                        // Get the corresponding ACM AudioItem.
                        AudioItem audioItem = playlist.getAudioItemList().stream().map(store::getAudioItem)
                            .filter(it -> it.getTitle().equals(message.title))
                            .findFirst().orElse(null);
                        // If the ACM AudioItem is missing, note the issue.
                        if (audioItem == null) {
                            issue("A title is missing in Playlist '%s' in the ACM: %s", playlist.getName(),
                                message.title);
                        } else {
                            foundItems.add(audioItem);
                        }
                    }
                    // We've looked in the ACM for messages in the Program Spec.
                    // Now see if there are messages added to the ACM playlist.
                    playlist.getAudioItemList().stream().map(store::getAudioItem)
                        .filter(i->!foundItems.contains(i))
                        .forEach(i->issue("A title was added to Playlist '%s' in the ACM: %s",
                            playlist.getName(), i.getTitle()));
                }
            }
            acmPlaylists.stream()
                .filter(p->!foundPlaylists.contains(p))
                .forEach(p->issue("A playlist was added to the ACM: %s", p.getName()));
        }

    }

    /**
     * Determines whether we have all required system prompts in all languages for the Deployment.
     * @param languages to be checked.
     */
    private void validateSystemPrompts(Collection<String> languages) {
        boolean hasUserFeedback = true;
        String[] required_messages = hasUserFeedback ? TBBuilder.REQUIRED_SYSTEM_MESSAGES_UF : TBBuilder.REQUIRED_SYSTEM_MESSAGES_NO_UF;

        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getTBLoadersDirectory();
        String languagesPath = "TB_Options" + File.separator + "languages";
        File languagesDir = new File(tbLoadersDir, languagesPath);

        for (String language : languages) {
            File languageDir = IOUtils.FileIgnoreCase(languagesDir, language);

            for (String prompt : required_messages) {
                File p1 = new File(languageDir, prompt + ".a18");
                if (!p1.exists()) {
                    issue("The system prompt '%s' is missing for language %s.", prompt, language);
                }
            }
        }
    }

    /**
     * Validates that the playlist prompts exist (short and long) for all playlists in all
     * languages.
     * @param languages to check.
     */
    private void validatePlaylistPrompts(Collection<String> languages) {
        for (String language : languages) {
            // Here, we care about the actual playlists for the Deployment.
            List<Playlist> acmPlaylists = allAcmPlaylists.get(language);
            for (Playlist playlist : acmPlaylists) {
                String title = WelcomePage.basePlaylistName(playlist.getName());
                title = title.replaceAll("_", " ");
                PlaylistPrompts prompts = context.new PlaylistPrompts(title, language);
                prompts.findPrompts();

                File shortFile = prompts.shortPromptFile;
                File longFile = prompts.longPromptFile;
                AudioItem shortItem = prompts.shortPromptItem;
                AudioItem longItem = prompts.longPromptItem;

                if (shortFile == null && shortItem == null) {
                    issue("Missing short prompt for Playlist %s", title);
                } else if (shortFile != null && shortItem != null) {
                    issue("Ambiguous short prompt for Playlist %s", title);
                }

                if (longFile == null && longItem == null) {
                    issue("Missing long description for Playlist %s", title);
                } else if (longFile != null && longItem != null) {
                    issue("Ambiguous long description for Playlist %s", title);
                }

                // If there is one of each, are they the same flavor?
                if ( ((shortFile==null ^ shortItem==null) && (longFile==null ^ longItem==null)) &&
                    ((shortFile==null) != (longFile==null))) {
                    issue("Mismatched classic vs content prompts for Playlist %s", title);
                }
            }
        }
    }



    /**
     * Gets the playlists defined in the Program Spec for a given Deployment. Note that playlists
     * may be different between languages, due to missing content in some languages.
     *
     * @param deploymentNo of the Deployment.
     * @param languages    of all the Recipients in the Deployment.
     * @return a map of { language : [Content.Playlist ] }
     */
    private Map<String, List<Content.Playlist>> getProgramSpecPlaylists(int deploymentNo,
        Set<String> languages)
    {
        Content content = context.programSpec.getContent();
        Map<String, List<Content.Playlist>> programSpecPlaylists = new HashMap<>();
        for (String language : languages) {
            programSpecPlaylists.put(language, content.getPlaylists(deploymentNo, language));
        }
        return programSpecPlaylists;
    }

    /**
     * Gets the playlists defined in the ACM for a given Deployment. If all content was imported,
     * and playlists were not manually edited, these will completely match the programSpec playlists.
     * Additional playlists may be present, if there were any created with the pattern #-pl-lang.
     *
     * @param deploymentNo of the Deployment.
     * @param languages    of all the Recipients in the Deployment.
     * @return a map of { language : [ Playlist ] }
     */
    private Map<String, List<Playlist>> getAcmPlaylists(int deploymentNo, Set<String> languages) {
        Map<String, List<Playlist>> acmPlaylists = new HashMap<>();
        Collection<Playlist> playlists = store.getPlaylists();
        for (String language : languages) {
            List<Playlist> langPlaylists = new ArrayList<>();
            // Look for anything matching the pattern, whether from the Program Spec or not.
            Pattern pattern = Pattern.compile(String.format("%d-.*-%s", deploymentNo, language));
            for (Playlist pl : playlists) {
                Matcher plMatcher = pattern.matcher(pl.getName());
                if (plMatcher.matches()) {
                    langPlaylists.add(pl);
                }
            }
            acmPlaylists.put(language, langPlaylists);
        }
        return acmPlaylists;
    }

}
