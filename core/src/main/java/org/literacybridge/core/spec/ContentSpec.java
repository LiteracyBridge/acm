package org.literacybridge.core.spec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The content specifications from a program specification. The ContentSpec consists of:
 * - a list of DeploymentSpecs, each of which has (among other properties)
 * -- a list of PlaylistSpecs, each of which has
 * --- a list of MessageSpecs.
 *
 * MessageSpecs are fully self-contained, with information about their containing playlist
 * and deployment. The hierarchy is created as an organizational convenience.
 */
public class ContentSpec {

    private final List<DeploymentSpec> deploymentSpecs = new ArrayList<>();
    public List<DeploymentSpec> getDeploymentSpecs() {
        return deploymentSpecs;
    }
    public DeploymentSpec getDeployment(int deploymentNumber) {
        return deploymentSpecs
            .stream()
            .filter(depl->depl.deploymentNumber == deploymentNumber)
            .findFirst()
            .orElse(null);
    }

    /**
     * Add a messageSpec to the list of messageSpecs. Messages know all about themselves, so we can
     * determine the deployment # and playlist from the message itself.
     *
     * DeploymentSpec and PlaylistSpec records are added as necessary.
     *
     * @param messageSpec to be added.
     */
    void addMessage(MessageSpec messageSpec) {
        DeploymentSpec deploymentSpec = getDeployment(messageSpec.deploymentNumber);

        if (deploymentSpec == null) {
            deploymentSpec = new DeploymentSpec(messageSpec.deploymentNumber);
            deploymentSpecs.add(deploymentSpec);
            // Deployments are kept in deployment order; other lists are in found order.
            deploymentSpecs.sort(Comparator.comparing(a -> a.deploymentNumber));
        }

        deploymentSpec.addMessage(messageSpec);
    }

    public class DeploymentSpec {
        private final Integer deploymentNumber;
        private final List<PlaylistSpec> playlistSpecs = new ArrayList<>();

        DeploymentSpec(Integer deploymentNumber) {
            this.deploymentNumber = deploymentNumber;
        }

        public Integer getDeploymentNumber() {
            return deploymentNumber;
        }

        public List<PlaylistSpec> getPlaylistSpecs() {
            return playlistSpecs;
        }
        public List<PlaylistSpec> getPlaylistSpecsForLanguage(String languagecode) {
            return getPlaylistSpecsForLanguageAndVariant(languagecode, null);
        }
        public List<PlaylistSpec> getPlaylistSpecsForLanguageAndVariant(String languagecode, String variant) {
            Predicate<MessageSpec> included = variant != null
                ? (msg) -> msg.includesLanguage(languagecode) && msg.includesVariant(variant)
                : (msg) -> msg.includesLanguage(languagecode);

            List<PlaylistSpec> result = new ArrayList<>();
            // For all of the playlists (in every language) in the deployment...
            for (PlaylistSpec playlistSpec : playlistSpecs) {
                PlaylistSpec plCopy = new PlaylistSpec(playlistSpec.deploymentNumber, playlistSpec.playlistTitle);
                // For all the messages in the playlist, if included, add to the copy.
                playlistSpec.getMessageSpecs().stream()
                    .filter(included)
                    .forEach(plCopy::addMessage);
                // If the playlist has any messages in the language, keep it.
                if (plCopy.messageSpecs.size() > 0) {
                    result.add(plCopy);
                }
            }
            return result;
        }

        int getPlaylistIx(String playlistTitle) {
            for (int ix = 0; ix< playlistSpecs.size(); ix++) {
                if (playlistSpecs.get(ix).playlistTitle.equals(playlistTitle))
                    return ix;
            }
            return -1;
        }
        public PlaylistSpec getPlaylist(String playlistTitle) {
            return playlistSpecs
                .stream()
                .filter(pl->pl.playlistTitle.equals(playlistTitle))
                .findFirst()
                .orElse(null);
        }

        void addMessage(MessageSpec messageSpec) {
            PlaylistSpec playlistSpec = getPlaylist(messageSpec.playlistTitle);

            if (playlistSpec == null) {
                playlistSpec = new PlaylistSpec(messageSpec.deploymentNumber, messageSpec.playlistTitle);
                playlistSpecs.add(playlistSpec);
            }

            playlistSpec.addMessage(messageSpec);
        }
    }

    public class PlaylistSpec {
        private final Integer deploymentNumber;
        private final String playlistTitle;
        private final List<MessageSpec> messageSpecs = new ArrayList<>();

        public PlaylistSpec(Integer deploymentNumber, String playlistTitle) {
            this.deploymentNumber = deploymentNumber;
            this.playlistTitle = playlistTitle;
        }

        public String getPlaylistTitle() {
            return playlistTitle;
        }

        public int getOrdinal() {
            int result = -1;
            DeploymentSpec deploymentSpec = getDeployment(this.deploymentNumber);
            if (deploymentSpec != null) {
                result = deploymentSpec.getPlaylistIx(this.playlistTitle);
            }
            return result;
        }

        public List<MessageSpec> getMessageSpecs() {
            return messageSpecs;
        }

        int getMessageIx(MessageSpec messageSpec) {
            for (int ix = 0; ix< messageSpecs.size(); ix++) {
                if (messageSpecs.get(ix).title.equals(messageSpec.title))
                    return ix;
            }
            return -1;
        }
        public List<MessageSpec> getMessagesForLanguage(String languagecode) {
            return messageSpecs
                .stream()
                .filter(msg -> msg.includesLanguage(languagecode))
                .collect(Collectors.toList());
        }
        public List<MessageSpec> getMessagesForLanguageAndVariant(String languagecode, String variant) {
            return messageSpecs
                .stream()
                .filter(msg -> msg.includesLanguage(languagecode) && msg.includesVariant(variant))
                .collect(Collectors.toList());
        }

        void addMessage(MessageSpec messageSpec) {
            int ix = getMessageIx(messageSpec);
            if (ix < 0)
                messageSpecs.add(messageSpec);
            else
                messageSpecs.set(ix, messageSpec);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlaylistSpec playlistSpec = (PlaylistSpec) o;
            return deploymentNumber.equals(playlistSpec.deploymentNumber) && playlistTitle.equals(
                playlistSpec.playlistTitle);
        }

        @Override
        public int hashCode() {
            return Objects.hash(deploymentNumber, playlistTitle);
        }
    }


    public class MessageSpec {
        public final int deploymentNumber;
        public final String playlistTitle;
        public final String title;
        public final String keyPoints;
        public final String format;
        /**
         * This is a language *filter*. In which languages is (or is not) the message expected to
         * be present? If no filter, all languages in the deployment.
         */
        public final String languagecode;
        private final StringFilter languageFilter;

        public final String variant;
        private final StringFilter variantFilter;

        public final String default_category;
        public final String sdg_goals;
        public final String sdg_targets;

        public MessageSpec(Map<String, String> properties) {
            this.deploymentNumber = Integer.parseInt(properties.get(columns.deploymentNumber.externalName));
            this.playlistTitle = properties.get(columns.playlistTitle.externalName);
            this.title = properties.get(columns.title.externalName);
            this.keyPoints = properties.get(columns.keyPoints.externalName);
            this.format = properties.get(columns.format.externalName);
            String language = properties.getOrDefault(columns.language.externalName, "");
            this.languagecode = properties.getOrDefault(columns.languagecode.externalName, language).toLowerCase();
            this.variant = properties.getOrDefault(columns.variant.externalName, "");
            this.default_category = properties.get(columns.default_category.externalName);
            this.sdg_goals = properties.get(columns.sdg_goals.externalName);
            this.sdg_targets = properties.get(columns.sdg_targets.externalName);

            this.languageFilter = new StringFilter(this.languagecode);
            this.variantFilter = new StringFilter(this.variant);
        }

        public boolean includesLanguage(String languagecode) {
            return languageFilter.test(languagecode);
        }

        public boolean includesVariant(String variant) {
            return variantFilter.test(variant);
        }

        public Collection<String> variantItems() { return variantFilter.items(); }

        public String getName() {
            return String.format("%d / %s / %s", deploymentNumber, playlistTitle, title);
        }

        public String getTitle() {
            return title;
        }

        public String getPlaylistTitle() {
            return playlistTitle;
        }

        public int getOrdinal() {
            int result = -1;
            PlaylistSpec playlistSpec = getPlaylist();
            if (playlistSpec != null) {
                result = playlistSpec.getMessageIx(this);
            }
            return result;
        }

        public PlaylistSpec getPlaylist() {
            DeploymentSpec depl = getDeployment(this.deploymentNumber);
            if (depl != null) {
                return depl.getPlaylist(this.playlistTitle);
            }
            return null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MessageSpec messageSpec = (MessageSpec) o;
            return deploymentNumber == messageSpec.deploymentNumber
                && playlistTitle.equals(messageSpec.playlistTitle) && title.equals(messageSpec.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(deploymentNumber, playlistTitle, title);
        }

    }

    private enum columns {
        deploymentNumber("deployment_num"),
        playlistTitle("playlist_title"),
        title("message_title"),
        keyPoints("key_points"),
        format,
        language("language"),   // deprecated in favor of languagecode
        languagecode("languagecode"),
        variant("variant"),
        sdg_goals("sdg_goals"),
        sdg_targets("sdg_targets"),
        default_category;

        final String externalName;

        columns() { externalName = this.name(); }

        columns(String externalName) { this.externalName = externalName; }
    }

    static String[] columnNames;
    static {
        columnNames = new String[columns.values().length];
        for (int ix = 0; ix < columns.values().length; ix++) {
            columnNames[ix] = columns.values()[ix].externalName;
        }
    }
    static String[] FILENAMES = new String[]{"pub_content.csv", "content.csv"};

}
