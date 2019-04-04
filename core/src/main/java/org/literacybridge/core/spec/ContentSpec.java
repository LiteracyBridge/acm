package org.literacybridge.core.spec;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ContentSpec {

    private List<DeploymentSpec> deploymentSpecs = new ArrayList<>();
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
    public List<PlaylistSpec> getPlaylists(int deploymentNumber) {
        DeploymentSpec deploymentSpec = getDeployment(deploymentNumber);
        if (deploymentSpec == null) return new ArrayList<PlaylistSpec>();
        return deploymentSpec.getPlaylistSpecs();
    }
    public List<PlaylistSpec> getPlaylists(int deploymentNumber, String language) {
        List<PlaylistSpec> result = new ArrayList<>();
        for (PlaylistSpec playlistSpec : getPlaylists(deploymentNumber)) {
            PlaylistSpec plCopy = new PlaylistSpec(playlistSpec.deploymentNumber, playlistSpec.playlistTitle);
            for (MessageSpec msg : playlistSpec.getMessageSpecs()) {
                if (StringUtils.isAllBlank(msg.language) || new StringFilter(msg.language).test(language)) {
                    plCopy.addMessage(msg);
                }
            }
            if (plCopy.messageSpecs.size() > 0) {
                result.add(plCopy);
            }
        }
        return result;
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
            deploymentSpecs.sort((a, b) -> a.deploymentNumber.compareTo(b.deploymentNumber));
        }

        deploymentSpec.addMessage(messageSpec);
    }

    public class DeploymentSpec {
        private final Integer deploymentNumber;
        private List<PlaylistSpec> playlistSpecs = new ArrayList<>();

        DeploymentSpec(Integer deploymentNumber) {
            this.deploymentNumber = deploymentNumber;
        }

        public Integer getDeploymentNumber() {
            return deploymentNumber;
        }

        public List<PlaylistSpec> getPlaylistSpecs() {
            return playlistSpecs;
        }
        int getPlaylistIx(String playlistTitle) {
            for (int ix = 0; ix< playlistSpecs.size(); ix++) {
                if (playlistSpecs.get(ix).playlistTitle.equals(playlistTitle))
                    return ix;
            }
            return -1;
        }
        PlaylistSpec getPlaylist(String playlistTitle) {
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
        private List<MessageSpec> messageSpecs = new ArrayList<>();

        public PlaylistSpec(Integer deploymentNumber, String playlistTitle) {
            this.deploymentNumber = deploymentNumber;
            this.playlistTitle = playlistTitle;
        }

        public String getPlaylistTitle() {
            return playlistTitle;
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
        public List<MessageSpec> getMessagesForLanguage(String languageCode) {
            return messageSpecs
                .stream()
                .filter(msg -> msg.includesLanguage(languageCode))
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
        public final String language;
        public final String default_category;

        private StringFilter languageFilter;

        public MessageSpec(Map<String, String> properties) {
            this.deploymentNumber = Integer.parseInt(properties.get(columns.deploymentNumber.externalName));
            this.playlistTitle = properties.get(columns.playlistTitle.externalName);
            this.title = properties.get(columns.title.externalName);
            this.keyPoints = properties.get(columns.keyPoints.externalName);
            this.format = properties.get(columns.format.externalName);
            this.language = properties.get(columns.language.externalName);
            this.default_category = properties.get(columns.default_category.externalName);
            this.languageFilter = new StringFilter(this.language);
        }

        public boolean includesLanguage(String languageCode) {
            return languageFilter.test(languageCode);
        }

        public String getName() {
            return String.format("%d / %s / %s", deploymentNumber, playlistTitle, title);
        }

        public String getTitle() {
            return title;
        }

        public String getPlaylistTitle() {
            return playlistTitle;
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
        language("language"),
        default_category;

        String externalName;

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

}
