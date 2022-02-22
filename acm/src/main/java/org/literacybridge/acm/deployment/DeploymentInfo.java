package org.literacybridge.acm.deployment;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.assistants.Deployment.PlaylistPrompts;
import org.literacybridge.acm.store.AudioItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class contains all the information needed to create a deployment.
 *
 * To use:
 * - Create a DeploymentInfo, set options for the deployment (UF hidden, has tutorial)
 * - Create Packages for the deployment, with options (has intro)
 * - Add the playlists to the packages, in their order of appearance. For each playlist,
 *     add the announcement and invitation, and note if it is "user feedback".
 * - Add the content to the playlists, in their order of appearance
 */
public class DeploymentInfo {
    private final String programid;
    private final int deploymentNumber;
    private final String name;
    private boolean ufHidden;
    private boolean hasTutorial;

    private final List<PackageInfo> packages = new ArrayList<>();

    public DeploymentInfo(String programid, int deploymentNumber, Calendar startDate) {
        this.programid = programid;
        this.deploymentNumber = deploymentNumber;
        Calendar today = new Calendar.Builder().setInstant(System.currentTimeMillis()).build();
        name = String.format("%s-%d-%d", programid, startDate.get(Calendar.YEAR)%100, deploymentNumber);
    }

    public String getName() {
        return name;
    }

    public boolean isUfHidden() { return ufHidden; }
    public DeploymentInfo ufHidden() { return setUfHidden(true); }
    public DeploymentInfo setUfHidden(boolean ufHidden) {
        this.ufHidden = ufHidden;
        return this;
    }

    public boolean hasTutorial() { return hasTutorial; }
    public void setTutorial(boolean hasTutorial) {
        this.hasTutorial = hasTutorial;
    }

    public PackageInfo addPackage(String languageCode, String variant) {
        PackageInfo packageInfo = new PackageInfo(languageCode, variant);
        packages.add(packageInfo);
        return packageInfo;
    }

    /**
     * Removes empty packages. An empty package is one which has no playlists, or only
     * empty playlists. An empty playlist is non-feedback one with no messages.
     */
    public void prune() {
        List<PackageInfo> pruned = packages.stream()
            .map(PackageInfo::prune)
            .filter(PackageInfo::isNotEmpty)
            .collect(Collectors.toList());
        packages.clear();
        packages.addAll(pruned);
    }

    @Override
    public String toString() {
        return String.format("%s-%d", programid, deploymentNumber);
    }

    public List<? extends PackageInfo> getPackages() {
        return packages;
    }

    public String getProgramId() {
        return programid;
    }

    public int getDeploymentNumber() {
        return deploymentNumber;
    }

    public class PackageInfo {
        private final String languageCode;
        private final String variant;
        private final String name;
        private final String shortName;
        private AudioItem introMessage;

        private final List<PlaylistInfo> playlists = new ArrayList<>();

        private PackageInfo(String languageCode, String variant) {
            this.languageCode = languageCode;
            this.variant = variant;
            this.name = makeName(false);
            this.shortName = makeName(true);
        }

        public void setIntroItem(AudioItem introMessage) {
            this.introMessage = introMessage;
        }
        public boolean hasIntro() { return introMessage != null; }

        public PlaylistInfo addPlaylist(PlaylistBuilder playlistBuilder) {
            PlaylistInfo playlistInfo = playlistBuilder.build();
            playlists.add(playlistInfo);
            return playlistInfo;
        }

        public boolean hasUserFeedbackPlaylist() {
            return playlists.stream().anyMatch(pl->pl.isUserFeedback);
        }
        public boolean hasTutorial() {
            // Note that this is inherited from the Deployment. To enable per-package tutorials, add
            // a "hasTutorial" to the PackageInfo.
            return hasTutorial;
        }

        /**
         * "Prune" the package info by removing playlists that don't have content. It could be that there are
         * no messages in the package's language.
         * @return the package info, with empty playlists removed. Note that the packageinfo is mutated.
         */
        public PackageInfo prune() {
            List<PlaylistInfo> pruned = playlists.stream()
                .map(PlaylistInfo::prune)
                .filter(PlaylistInfo::isNotEmpty)
                .collect(Collectors.toList());
            playlists.clear();
            playlists.addAll(pruned);
            return this;
        }

        public boolean isNotEmpty() {
            return playlists.size() > 0 || hasTutorial;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getLanguageCode() {
            return languageCode;
        }

        public String getVariant() {
            return variant;
        }

        public String getShortName() {
            return shortName;
        }

        public Collection<PlaylistInfo> getPlaylists() {
            return playlists;
        }

        public AudioItem getIntro() {
            return introMessage;
        }

        public String getName() {
            return name;
        }

        public AudioItem getIntroMessage() {
            return introMessage;
        }

        public boolean isUfHidden() {
            return DeploymentInfo.this.isUfHidden();
        }

        /**
         * Groups are an old concept, supplanted by variants. This function exists to help produce identical
         * deplayments using this new DeploymentInfo strutures.
         * @return The list of old-style groups for this package.
         */
        public List<String> getGroups() {
            List<String> result = new ArrayList<>();
            if (DeploymentInfo.this.getPackages().get(0) == this) {
                result.add("default");
            }
            if (StringUtils.isNotBlank(variant)) {
                result.add(variant);
            } else {
                result.add(languageCode);
            }
            return result;
        }

        /**
         * Helper to build package names. For TBv1 we require a package name shorter than 20 characters,
         * to fit into a fixed-length field on the device.
         *
         * @param shortName if true, munge the name until it is < 20 characters.
         * @return the name.
         */
        private String makeName(boolean shortName) {
            String variantStr = StringUtils.isNotBlank(variant) ? '-' + variant.toLowerCase() : "";
            String packageName = programid + '-' + deploymentNumber + '-' + languageCode + variantStr;

            // If a short name is needed, and this is too long, shorten it.
            if (shortName && packageName.length() > Constants.MAX_PACKAGE_NAME_LENGTH) {
                // Eliminate hyphens.
                variantStr = StringUtils.isNotBlank(variant) ? variant.toLowerCase() : "";
                packageName = programid + deploymentNumber + languageCode + variantStr;

                // If thats still too long, eliminate vowels in project name.
                String programStr = programid;
                if (packageName.length() > Constants.MAX_PACKAGE_NAME_LENGTH) {
                    assert programid != null;
                    programStr = programid.replaceAll("[aeiouAEIOU]", "");
                    packageName = programStr + deploymentNumber + languageCode + variantStr;
                }
                // If thats still too long, eliminate underscores and hyphens in project name.
                if (packageName.length() > Constants.MAX_PACKAGE_NAME_LENGTH) {
                    programStr = programStr.replaceAll("[_-]", "");
                    packageName = programStr + deploymentNumber + languageCode + variantStr;
                }
                // If still too long, truncate project name.
                if (packageName.length() > Constants.MAX_PACKAGE_NAME_LENGTH) {
                    int neededTrim = packageName.length() - Constants.MAX_PACKAGE_NAME_LENGTH;
                    int keep = programStr.length() - neededTrim;
                    if (keep > 0) {
                        programStr = programStr.substring(0, keep);
                        packageName = programStr + deploymentNumber + languageCode + variantStr;
                    } else {
                        // This means either a very long variant or a very long language. Should never happen.
                        // Use the hashcode of the string, and hope?
                        // Put the vowels back
                        programStr = ACMConfiguration.getInstance().getCurrentDB().getProgramId();
                        variantStr = StringUtils.isNotBlank(variant) ? '-' + variant.toLowerCase() : "";
                        packageName = programStr + deploymentNumber + languageCode + variantStr;
                        packageName = Integer.toHexString(packageName.hashCode());
                    }
                }
            }
            return packageName;
        }
        public class PlaylistBuilder {
            private String title;
            private PromptInfo announcement;
            private PromptInfo invitation;
            private String categoryId;
            private boolean isUserFeedback;
            private boolean isLocked;

            public PlaylistBuilder withTitle(String title) {
                this.title=title;
                return this;
            }
            public PlaylistBuilder withPrompts(PlaylistPrompts prompts) {
                this.announcement = new PromptInfo(prompts.getShortItem(), prompts.getShortFile());
                this.invitation = new PromptInfo(prompts.getLongItem(), prompts.getLongFile());
                this.categoryId = prompts.getCategoryId();
                return this;
            }
            public PlaylistBuilder isUserFeedback(boolean isUserFeedback) {
                this.isUserFeedback = isUserFeedback;
                return this;
            }
            public PlaylistBuilder isLocked(boolean isLocked) {
                this.isLocked = isLocked;
                return this;
            }

            private PlaylistInfo build() {
                return new PlaylistInfo(this);
            }
        }
        public class PlaylistInfo {

            private final String title;
            private final String categoryId;
            private final PromptInfo announcement;
            private final PromptInfo invitation;
            private final boolean isUserFeedback;
            private final List<String> content = new ArrayList<>();

            private PlaylistInfo(PlaylistBuilder playlistBuilder) {
                this.title = playlistBuilder.title;
                this.categoryId = playlistBuilder.categoryId;
                this.announcement = playlistBuilder.announcement;
                this.invitation = playlistBuilder.invitation;
                this.isUserFeedback = playlistBuilder.isUserFeedback;
                boolean isLocked = playlistBuilder.isLocked;
            }

            public void addContent(AudioItem audioItem) {
                content.add(audioItem.getId());
            }

            /**
             * The content member either has audio items or it doesn't.
             * @return this
             */
            public PlaylistInfo prune() {
                return this;
            }

            public boolean isNotEmpty() {
                return content.size() > 0 || isUserFeedback;
            }

            public String getTitle() {
                return title;
            }

            public boolean isUserFeedback() {
                return isUserFeedback;
            }

            public PlaylistPrompts getPlaylistPrompts() {
                return new PlaylistPrompts(title, languageCode, categoryId,
                    announcement.audioFile, announcement.audioItem, invitation.audioFile, invitation.audioItem);
            }
            public PromptInfo getAnnouncement() {
                return this.announcement;
            }
            public PromptInfo getInvitation() {
                return this.invitation;
            }

            public List<? extends String> getAudioItemIds() {
                return content;
            }

            @Override
            public String toString() {
                return title;
            }

            public String getCategoryId() {
                return categoryId;
            }
        }

    }

    public static class PromptInfo {
        // Must be either AudioItem or File, but only one.
        public final AudioItem audioItem;
        public final File audioFile;

        public PromptInfo(AudioItem audioItem, File file) {
            if (audioItem==null && file==null) {
                throw new IllegalArgumentException("Must pass one of AudioItem or File.");
            }
            this.audioItem = audioItem;
            this.audioFile = audioItem == null ? file : null;
//            this.audioFile = file;
//            this.audioItem = audioFile == null ? audioItem : null;
        }
    }
}
