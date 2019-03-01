package org.literacybridge.acm.gui.assistants.Deployment;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.ContentImport.WelcomePage;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.Category;
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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.literacybridge.acm.gui.assistants.ContentImport.WelcomePage.qualifiedPlaylistName;

public class ValidationPage extends AssistantPage<DeploymentContext> {

    private final JLabel deployment;

    private DeploymentContext context;

    MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

    private RecipientList recipients;
    private Set<String> languages;
    private Map<String, List<Content.Playlist>> allContentPlaylists;
    private Map<String, List<Playlist>> allAcmPlaylists;

    public ValidationPage(PageHelper listener) {
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
     * @param actionEvent
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

        allContentPlaylists = getContentPlaylists(deploymentNo, languages);
        allAcmPlaylists = getAcmPlaylists(deploymentNo, languages);

    }

    private void validateDeployment(int deploymentNo) {

        // Get the languages, and playlists for each language.
        // Check that we have all messages in all playlists; check if anything was added.
        validatePlaylists(deploymentNo, languages);

        // Check that we have all system prompts in all languages.
        validateSystemPrompts(languages);

        // Check that we have all category prompts for categories in each language.
        validatePlaylistPrompts(languages);

        // Check that we have all recipient prompts for recipients in the deployment.

    }

    /**
     * Validate the Playlists in a Deployment. Each language is evaluated separately, both because
     * playlists can legitimately differ between languages, and because there will be different
     * errors between languages.
     * @param deploymentNo deployment # to be validated.
     * @param languages list of languages in the deployment.
     */
    private void validatePlaylists(int deploymentNo, Set<String> languages) {
        // For each language in the Deployment...
        for (String language : languages) {
            // Get the Program Spec playlists, and the ACM playlists (that match by pattern).
            List<Content.Playlist> contentPlaylists = allContentPlaylists.get(language);
            List<Playlist> acmPlaylists = allAcmPlaylists.get(language);

            // For all the playlists in the Program Spec...
            Set<Playlist> foundPlaylists = new HashSet<>();
            for (Content.Playlist contentPlaylist : contentPlaylists) {
                // Get the corresponding ACM playlist, if it exists.
                String qualifiedPlaylistName = qualifiedPlaylistName(contentPlaylist.getPlaylistTitle(),
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
                    for (Content.Message message : contentPlaylist.getMessages()) {
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

    private void validatePlaylistPrompts(Collection<String> languages) {
        for (String language : languages) {
            // Here, we care about the actual playlists for the Deployment.
            List<Playlist> acmPlaylists = allAcmPlaylists.get(language);
            for (Playlist playlist : acmPlaylists) {
                String title = WelcomePage.basePlaylistName(playlist.getName());
                File promptFile = findClassicPrompt(title, language);
            }
        }
    }

    private File findClassicPrompt(String title, String language) {
        // We know the taxonomy category names use spaces, not underscores.
        final String nTitle = title.replaceAll("_", " ");

        String categoryId = StreamSupport.stream(store.getTaxonomy().breadthFirstIterator().spliterator(), false)
            .filter(c->c.getCategoryName().equalsIgnoreCase(nTitle))
            .map(Category::getId)
            .findFirst().orElse(null);
        if (categoryId != null) {
            return findPromptForCategory(categoryId, language);
        }
        return null;
    }
    private File findPromptForCategory(String id, String language) {
        File tbLoadersDir = ACMConfiguration.getInstance().getCurrentDB().getTBLoadersDirectory();
        String languagesPath = "TB_Options" + File.separator + "languages" + File.separator + language + File.separator + "cat";
        File categoriesDir = new File(tbLoadersDir, languagesPath);
        File catFile = new File(categoriesDir, id + ".a18");

        return null;
    }

    /**
     * Gets the playlists defined in the Program Spec for a given Deployment. Note that playlists
     * may be different between languages, due to missing content in some languages.
     *
     * @param deploymentNo of the Deployment.
     * @param languages    of all the Recipients in the Deployment.
     * @return a map of { language : [Content.Playlist ] }
     */
    private Map<String, List<Content.Playlist>> getContentPlaylists(int deploymentNo,
        Set<String> languages)
    {
        Content content = context.programSpec.getContent();
        Map<String, List<Content.Playlist>> contentPlaylists = new HashMap<>();
        for (String language : languages) {
            contentPlaylists.put(language, content.getPlaylists(deploymentNo, language));
        }
        return contentPlaylists;
    }

    /**
     * Gets the playlists defined in the ACM for a given Deployment. If all content was imported,
     * and playlists were not manually edited, these will completely match the content playlists.
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
