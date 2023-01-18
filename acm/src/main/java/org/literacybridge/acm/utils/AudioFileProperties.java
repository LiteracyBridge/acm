package org.literacybridge.acm.utils;

import org.literacybridge.acm.config.ACMConfiguration;

import java.io.File;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.literacybridge.acm.utils.ExternalCommandRunner.LineProcessorResult.HANDLED;

public class AudioFileProperties {

    public static Props readFromFile(String audioFilePath) {
        MediaInfoWrapper mi = new MediaInfoWrapper(audioFilePath);
        mi.go();
        return mi;
    }


    public interface Props {
        String getDuration();

        int getChannels();

        int getKbps();
    }

    private static class MediaInfoWrapper extends ExternalCommandRunner.CommandWrapper implements Props {
        // Audio
        // Duration                                 : 149081
        // Duration                                 : 2 min 29 s
        // Duration                                 : 2 min 29 s 81 ms
        // Duration                                 : 2 min 29 s
        //*Duration                                 : 00:02:29.081
        //*Duration                                 : 00:02:29.081
        //*Bit rate                                 : 256000
        // Bit rate                                 : 256 kb/s
        // Channel(s)                               : 2
        //*Channel(s)                               : 2 channels
        private static final Pattern GENERAL = Pattern.compile("(?i)^General.*");
        private static final Pattern AUDIO = Pattern.compile("(?i)^Audio.*");
        private static final Pattern DURATION = Pattern.compile("(?i)\\s*Duration[\\s:]*((\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3}))");
        private static final Pattern BITRATE = Pattern.compile("(?i)Bit rate[\\s:]*(\\d*)");
        private static final Pattern CHANNELS = Pattern.compile("(?i)Channel\\(s\\)[\\s:]*(\\d*) channels?");

        private final String audioFilePath;

        private MediaInfoWrapper(String audioFilePath) {
            this.audioFilePath = audioFilePath;
        }

        @Override
        protected File getRunDirectory() {
            return new File(".");
        }

        public String getFFprobeExePath() {
            return ACMConfiguration.getInstance().getSoftwareDir().getPath() + "/converters/ffmpeg/mediainfo.exe";
        }

        protected String[] getCommand() {
            return new String[]{getFFprobeExePath(), "-f", audioFilePath};
        }

        @Override
        protected List<ExternalCommandRunner.LineHandler> getLineHandlers() {
            return Arrays.asList(
                    new ExternalCommandRunner.LineHandler(GENERAL, (writer, matcher) -> {
                        infoSection = INFOSECTION.GENERAL;
                        return HANDLED;
                    }),
                    new ExternalCommandRunner.LineHandler(AUDIO, (writer, matcher) -> {
                        infoSection = INFOSECTION.AUDIO;
                        return HANDLED;
                    }),
                    new ExternalCommandRunner.LineHandler(DURATION, this::gotDuration),
                    new ExternalCommandRunner.LineHandler(BITRATE, this::gotBitrate),
                    new ExternalCommandRunner.LineHandler(CHANNELS, this::gotChannels)
            );
        }

        private ExternalCommandRunner.LineProcessorResult gotChannels(Writer writer, Matcher matcher) {
            if (infoSection == INFOSECTION.AUDIO) {
                try {
                    channels = Integer.parseInt(matcher.group(1));
                } catch (Exception ignored) {
                }
            }
            return HANDLED;
        }

        private ExternalCommandRunner.LineProcessorResult gotBitrate(Writer writer, Matcher matcher) {
            if (infoSection == INFOSECTION.AUDIO) {
                try {
                    kbps = Integer.parseInt(matcher.group(1)) / 1000;
                } catch (Exception ignored) {
                }
            }
            return HANDLED;
        }

        private enum INFOSECTION {NONE, GENERAL, AUDIO}

        INFOSECTION infoSection = INFOSECTION.NONE;
        private String duration = null;
        private int channels = 0;
        private int kbps = -1;

        public String getDuration() {
            return duration;
        }

        public int getChannels() {
            return channels;
        }

        public int getKbps() {
            return kbps;
        }

        private ExternalCommandRunner.LineProcessorResult gotDuration(Writer writer, Matcher matcher) {
            this.duration = matcher.group(1);
            return HANDLED;
        }

    }

//    private static class FfProbeWrapper extends ExternalCommandRunner.CommandWrapper implements Props {
//        // mp3
//        // Duration: 00:00:00.73, start: 0.050113, bitrate: 34 kb/s
//        // Stream #0:0: Audio: mp3, 22050 Hz, mono, fltp, 32 kb/s
//        // pcm
//        // Duration: 00:00:05.95, bitrate: 768 kb/s
//        // Stream #0:0: Audio: pcm_s16le ([1][0][0][0] / 0x0001), 48000 Hz, 1 channels, s16, 768 kb/s
//        // aac
//        // [aac @ 0x7fc244104080] Estimating duration from bitrate, this may be inaccurate
//        // Duration: 00:00:02.61, bitrate: 131 kb/s
//        // Stream #0:0: Audio: aac (LC), 48000 Hz, mono, fltp, 131 kb/s
//        // ogg
//        // Duration: 00:00:05.95, start: 0.000000, bitrate: 21 kb/s
//        // Stream #0:0: Audio: vorbis, 48000 Hz, mono, fltp, 80 kb/s
//        // mp3
//        // [mp3 @ 0x7ff6b5704700] Estimating duration from bitrate, this may be inaccurate
//        // Duration: 00:00:06.05, start: 0.000000, bitrate: 16 kb/s
//        // Stream #0:0: Audio: mp3, 16000 Hz, mono, fltp, 16 kb/s
//        private static final Pattern ESTIMATED = Pattern.compile("(?i).*Estimating duration from bitrate.*");
//        private static final Pattern DURATION = Pattern.compile("(?i)\\s*Duration: ((\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{2}))");
//        private static final Pattern STREAM = Pattern
//                .compile("(?i)\\s*Stream #(?<n>\\d+):(?<of>\\d+): Audio: (?<encoding>[^,]+), (?<rate>[^,]+), (?<channels>[^,]+), (?<num>[^,]+), (?<bps>\\d+) [km]b/s\\s*$");
//        private static final Pattern CHANNELS = Pattern.compile("(?i)(?<mono>mono|stereo)|(?<channels>\\d+) channels?");
//
//        private final String audioFilePath;
//
//        private FfProbeWrapper(String audioFilePath) {
//            this.audioFilePath = audioFilePath;
//        }
//
//        @Override
//        protected File getRunDirectory() {
//            return new File(".");
//        }
//
//        public String getFFprobeExePath() {
//            return ACMConfiguration.getInstance().getSoftwareDir().getPath() + "/converters/ffmpeg/ffprobe.exe";
//        }
//
//        protected String[] getCommand() {
//            return new String[]{getFFprobeExePath(), "-hide_banner", audioFilePath};
//        }
//
//        @Override
//        protected List<ExternalCommandRunner.LineHandler> getLineHandlers() {
//            return Arrays.asList(
//                    new ExternalCommandRunner.LineHandler(ESTIMATED, this::gotEstimated),
//                    new ExternalCommandRunner.LineHandler(DURATION, this::gotDuration),
//                    new ExternalCommandRunner.LineHandler(STREAM, this::gotStream)
//            );
//        }
//
//        private String duration = null;
//        private int channels = 0;
//        private int bps = -1;
//
//        public String getDuration() {
//            return duration;
//        }
//
//        public int getChannels() {
//            return channels;
//        }
//
//        public int getKbps() {
//            return bps;
//        }
//
//        private ExternalCommandRunner.LineProcessorResult gotEstimated(Writer writer, Matcher matcher) {
////            estimated = true;
//            return HANDLED;
//        }
//
//        private ExternalCommandRunner.LineProcessorResult gotDuration(Writer writer, Matcher matcher) {
//            this.duration = matcher.group(1);
//            return HANDLED;
//        }
//
//        private ExternalCommandRunner.LineProcessorResult gotStream(Writer writer, Matcher matcher) {
//            Matcher channelsMatcher = CHANNELS.matcher(matcher.group("channels"));
//            if (channelsMatcher.matches()) {
//                if (StringUtils.isNotBlank(channelsMatcher.group("mono"))) {
//                    if (channelsMatcher.group("mono").equalsIgnoreCase("mono"))
//                        this.channels = 1;
//                    else if (channelsMatcher.group("mono").equalsIgnoreCase("stereo"))
//                        this.channels = 2;
//                } else if (StringUtils.isNotBlank(channelsMatcher.group("channels"))) {
//                    try {
//                        this.channels = Integer.parseInt(channelsMatcher.group("channels"));
//                    } catch (Exception ignored) {
//                    }
//                }
//            }
//            try {
//                bps = Integer.parseInt(matcher.group("bps"));
//            } catch (Exception ignored) {
//            }
//            return HANDLED;
//        }
//
//    }


}
