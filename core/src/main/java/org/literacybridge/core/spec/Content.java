package org.literacybridge.core.spec;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Content {

    private List<Deployment> deployments = new ArrayList<>();
    public List<Deployment> getDeployments() {
        return deployments;
    }
    public Deployment getDeployment(int deploymentNumber) {
        return deployments.stream()
            .filter(depl->depl.deploymentNumber == deploymentNumber)
            .findFirst()
            .orElse(null);
    }
    public List<Playlist> getPlaylists(int deploymentNumber) {
        Deployment deployment = getDeployment(deploymentNumber);
        if (deployment == null) return new ArrayList<Playlist>();
        return deployment.getPlaylists();
    }
    public List<Playlist> getPlaylists(int deploymentNumber, String language) {
        List<Playlist> result = new ArrayList<>();
        for (Playlist playlist : getPlaylists(deploymentNumber)) {
            Playlist plCopy = new Playlist(playlist.deploymentNumber, playlist.playlistTitle);
            for (Message msg : playlist.getMessages()) {
                if (StringUtils.isAllBlank(msg.language) || new StringFilter(msg.language).test(language)) {
                    plCopy.addMessage(msg);
                }
            }
            if (plCopy.messages.size() > 0) {
                result.add(plCopy);
            }
        }
        return result;
    }

    /**
     * Add a message to the list of messages. Messages know all about themselves, so we can
     * determine the deployment # and playlist from the message itself.
     *
     * Deployment and Playlist records are added as necessary.
     *
     * @param message to be added.
     */
    void addMessage(Message message) {
        Deployment deployment = getDeployment(message.deploymentNumber);

        if (deployment == null) {
            deployment = new Deployment(message.deploymentNumber);
            deployments.add(deployment);
            // Deployments are kept in deployment order; other lists are in found order.
            deployments.sort((a, b) -> a.deploymentNumber.compareTo(b.deploymentNumber));
        }

        deployment.addMessage(message);
    }

    public class Deployment {
        private final Integer deploymentNumber;
        private List<Playlist> playlists = new ArrayList<>();

        Deployment(Integer deploymentNumber) {
            this.deploymentNumber = deploymentNumber;
        }

        public Integer getDeploymentNumber() {
            return deploymentNumber;
        }

        public List<Playlist> getPlaylists() {
            return playlists;
        }
        int getPlaylistIx(String playlistTitle) {
            for (int ix=0; ix<playlists.size(); ix++) {
                if (playlists.get(ix).playlistTitle.equals(playlistTitle))
                    return ix;
            }
            return -1;
        }
        Playlist getPlaylist(String playlistTitle) {
            return playlists.stream()
                .filter(pl->pl.playlistTitle.equals(playlistTitle))
                .findFirst()
                .orElse(null);
        }

        void addMessage(Message message) {
            Playlist playlist = getPlaylist(message.playlistTitle);

            if (playlist == null) {
                playlist = new Playlist(message.deploymentNumber, message.playlistTitle);
                playlists.add(playlist);
            }

            playlist.addMessage(message);
        }
    }

    public class Playlist {
        private final Integer deploymentNumber;
        private final String playlistTitle;
        private List<Message> messages = new ArrayList<>();

        public Playlist(Integer deploymentNumber, String playlistTitle) {
            this.deploymentNumber = deploymentNumber;
            this.playlistTitle = playlistTitle;
        }

        public String getPlaylistTitle() {
            return playlistTitle;
        }

        public List<Message> getMessages() {
            return messages;
        }
        int getMessageIx(Message message) {
            for (int ix=0; ix<messages.size(); ix++) {
                if (messages.get(ix).title.equals(message.title))
                    return ix;
            }
            return -1;
        }

        void addMessage(Message message) {
            int ix = getMessageIx(message);
            if (ix < 0)
                messages.add(message);
            else
                messages.set(ix, message);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Playlist playlist = (Playlist) o;
            return deploymentNumber.equals(playlist.deploymentNumber) && playlistTitle.equals(
                playlist.playlistTitle);
        }

        @Override
        public int hashCode() {
            return Objects.hash(deploymentNumber, playlistTitle);
        }
    }


    public class Message {
        final int deploymentNumber;
        public final String playlistTitle;
        public final String title;
        final String keyPoints;
        public final String format;
        final String audience;
        final String comments;
        /**
         * This is a language *filter*. In which languages is (or is not) the message expected to
         * be present? If no filter, all languages in the deployment.
         */
        public final String language;
        

        public Message(int deploymentNumber,
            String playlistTitle,
            String title,
            String keyPoints,
            String format,
            String audience,
            String comments,
            String language)
        {
            this.deploymentNumber = deploymentNumber;
            this.playlistTitle = playlistTitle;
            this.title = title;
            this.keyPoints = keyPoints;
            this.format = format;
            this.audience = audience;
            this.comments = comments;
            this.language = language;
        }

        public Message(Map<String, String> properties) {
            this.deploymentNumber = Integer.parseInt(properties.get(columns.deploymentNumber.externalName));
            this.playlistTitle = properties.get(columns.playlistTitle.externalName);
            this.title = properties.get(columns.title.externalName);
            this.keyPoints = properties.get(columns.keyPoints.externalName);
            this.format = properties.get(columns.format.externalName);
            this.audience = properties.get(columns.audience.externalName);
            this.comments = properties.get(columns.comments.externalName);
            this.language = properties.get(columns.language.externalName);
        }

        public String getName() {
            return String.format("%d / %s / %s", deploymentNumber, playlistTitle, title);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Message message = (Message) o;
            return deploymentNumber == message.deploymentNumber
                && playlistTitle.equals(message.playlistTitle) && title.equals(message.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(deploymentNumber, playlistTitle, title);
        }
    }

    private enum columns {
        deploymentNumber("Deployment #"),
        playlistTitle("Playlist Title"),
        title("Message Title"),
        keyPoints("Key Points"),
        format,
        audience,
        comments,
        language("Existing recording");

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
