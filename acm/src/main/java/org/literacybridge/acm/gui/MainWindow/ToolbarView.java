package org.literacybridge.acm.gui.MainWindow;

import org.literacybridge.acm.audioconverter.converters.BaseAudioConverter.ConversionException;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.MainWindow.audioItems.AudioItemView;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.assistants.Chooser;
import org.literacybridge.acm.gui.messages.PlayAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestAudioItemMessage;
import org.literacybridge.acm.gui.messages.RequestAudioItemToPlayMessage;
import org.literacybridge.acm.gui.messages.SearchRequestMessage;
import org.literacybridge.acm.gui.playerAPI.PlayerStateDetails;
import org.literacybridge.acm.gui.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.settings.AcmSettingsDialog;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.utils.SwingUtils;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Observable;

public class ToolbarView extends JToolBar {

    private static final long serialVersionUID = -1827563460140622507L;

    private static final int TOOLBAR_HEIGHT = 45;
    private static final int CONTROL_HEIGHT = 30;
    private static final int SCRUBBER_WIDTH = 200;
    private static final int SEARCH_FIELD_WIDTH = 300;

    private static final Dimension ICON_SIZE = new Dimension(40, 32);

    private static final int SLIDER_UNITS_PER_SECOND = 10;

    // Player (will run in a different thread!)
    private SimpleSoundPlayer player;
    private double duration = 0.1;
    private final Timer updatePlayerStateTimer = new Timer(100, this::onUpdateTimerTick);
    private Box sliderBox;

    private ImageIcon getImage(String name) { return SwingUtils.getScaledImage(name, -1, 32); }
    
    private final ImageIcon backwardImageIcon = getImage(UIConstants.ICON_BACKWARD);
    private final ImageIcon playImageIcon = getImage(UIConstants.ICON_PLAY);
    private final ImageIcon pauseImageIcon = getImage(UIConstants.ICON_PAUSE);
    private final ImageIcon forwardImageIcon = getImage(UIConstants.ICON_FORWARD);
    private final ImageIcon stopImageIcon = getImage(UIConstants.ICON_STOP);

    private final ImageIcon searchImageIcon = new ImageIcon(UIConstants.getResource(UIConstants.ICON_SEARCH_32_PX));
    private final ImageIcon settingsImageIcon = new ImageIcon(
            UIConstants.getResource(UIConstants.ICON_GEAR_32_PX));
    private final ImageIcon assistantImageIcon = new ImageIcon(
            UIConstants.getResource(UIConstants.ICON_ASSISTANT_32_PX));

    private static final String NOT_PLAYING = LabelProvider.getLabel("Not Playing");
    private static final String NO_TIME = LabelProvider.getLabel("0:00");

    // assistant ("b") <div>Icon made from <a href="http://www.onlinewebfonts.com/icon">Icon Fonts</a> is licensed by CC BY 3.0</div>

    // Intelligence Assistance by ProSymbols from the Noun Project
    // config icon: configurations by I Putu Kharismayadi from the Noun Project
    // gears: configuration by Bieutuong Hai from the Noun Project

    private JButton configureButton;

    private JButton backwardBtn;
    private JButton forwardBtn;
    private JSlider positionSlider;
    private boolean positionSliderGrabbed = false;
    private JButton playBtn;
    private JButton stopBtn;
    private JLabel playedTimeLbl;
    private JLabel remainingTimeLbl;
    private JTextField searchTF;
    private JLabel titleInfoLbl;

    // Textfield Search
    private final String placeholderText = LabelProvider.getLabel(LabelProvider.PLACEHOLDER_TEXT);
    private final Font placeholderFont = new Font("Verdana", Font.ITALIC, 14);
    private Font defaultTextfieldFont = null;
    private JButton assistantButton;

    public ToolbarView(AudioItemView audioItemView) {
        initComponents();
        addEventHandler();
        addPositionSliderHandler();
        addSearchTFListener();

        Application.getMessageService().addObserver(this::onApplicationUpdate);
    }

    private void initPlayer() {
        if (player == null) {
            this.player = Application.getApplication().getSoundPlayer();
            player.addObserver((o, arg) -> {
                if (arg instanceof PlayerStateDetails) {
                    mirrorPlayerState((PlayerStateDetails) arg);
                }
            });
        }
    }

    /**
     * Formats a time for display on the player controls.
     *
     * @param seconds to be formatted.
     * @return a formatted string.
     */
    private String secondsToTimeString(int seconds) {
        final int SECONDS_PER_MINUTE = 60;
        final int MINUTES_PER_HOUR = 60;
        int s = seconds % SECONDS_PER_MINUTE;
        int m = (seconds / SECONDS_PER_MINUTE) % MINUTES_PER_HOUR;
        int h = seconds / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR);
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s);
        }
        return String.format("%d:%02d", m, s);
    }

    private void addEventHandler() {
        addPlayerButtonHandlers();

        configureButton.addActionListener(AcmSettingsDialog::showDialog);
        assistantButton.addActionListener(Chooser::showChooserMenu);
    }

    private void addPlayerButtonHandlers() {
        playBtn.addActionListener(e -> {
            if (player == null) {
                RequestAudioItemToPlayMessage msg = new RequestAudioItemToPlayMessage(
                        RequestAudioItemMessage.RequestType.Current);
                Application.getMessageService().pumpMessage(msg);
            } else {
                PlayerStateDetails psd = player.getPlayerStateDetails();
                if (psd.getCurrentPlayerState().isPaused()) {
                    player.resume();
                    updatePlayerStateTimer.start();
                } else if (psd.getCurrentPlayerState().isPlaying()) {
                    player.pause();
                    updatePlayerStateTimer.stop();
                    playBtn.setIcon(playImageIcon); // call explicit to avoid missing updates
                    playBtn.setToolTipText(LabelProvider.getLabel("Play"));
                } else {
                    AudioItem item = Application.getApplication()
                            .getMainView()
                            .getAudioItemView()
                            .getCurrentAudioItem();
                    if (item != null) {
                        play(item);
                    }
                }
            }
        });

        stopBtn.addActionListener(e -> {
            if (player != null) {
                PlayerStateDetails psd = player.getPlayerStateDetails();
                if (psd.getCurrentPlayerState().isStarted()) {
                    player.stop();
                    updatePlayerStateTimer.stop();
                }
            }
        });

        forwardBtn.addActionListener(e -> {
            if (player != null) {
                RequestAudioItemToPlayMessage msg = new RequestAudioItemToPlayMessage(
                        RequestAudioItemMessage.RequestType.Next);
                Application.getMessageService().pumpMessage(msg);
            }
        });

        backwardBtn.addActionListener(e -> {
            if (player != null) {
                RequestAudioItemToPlayMessage msg = new RequestAudioItemToPlayMessage(
                        RequestAudioItemMessage.RequestType.Previews);
                Application.getMessageService().pumpMessage(msg);
            }
        });
    }

    private void addPositionSliderHandler() {
        MouseAdapter mouseListener = new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (player.getPlayerStateDetails()
                        .getCurrentPlayerState().isStarted()) {
                    int value = positionSlider.getValue() / SLIDER_UNITS_PER_SECOND;
                    updatePlayerTimes(value);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                positionSliderGrabbed = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                PlayerStateDetails psd = player.getPlayerStateDetails();
                //  only seek if player is running
                if (psd.getCurrentPlayerState().isStarted()) {
                    double value = ((double) positionSlider.getValue()) / SLIDER_UNITS_PER_SECOND;
                    player.seek(value);
                }
                positionSliderGrabbed = false;
            }
        };
        positionSlider.addMouseListener(mouseListener);
        positionSlider.addMouseMotionListener(mouseListener);
    }

    private void mirrorPlayerState(PlayerStateDetails newState) {
        if (newState.getCurrentPlayerState().isNotPlaying()) {
            playBtn.setIcon(playImageIcon);
            playBtn.setToolTipText(LabelProvider.getLabel("Play"));
            boolean isPaused = newState.getCurrentPlayerState().isPaused();
            stopBtn.setEnabled(isPaused);
            positionSlider.setEnabled(isPaused);
            if (newState.getCurrentPlayerState().isStopped()) {
                titleInfoLbl.setText(NOT_PLAYING);
                playedTimeLbl.setText(NO_TIME);
                remainingTimeLbl.setText(NO_TIME);
                positionSlider.setValue(0);
            } else {
                positionSlider.setMaximum((int) (duration * SLIDER_UNITS_PER_SECOND));
                positionSlider.setValue((int) (newState.getCurrentPoitionInSecs() * SLIDER_UNITS_PER_SECOND));

                int playedTimeInSecs = (int) newState.getCurrentPoitionInSecs();
                playedTimeLbl.setText(secondsToTimeString(playedTimeInSecs));
                remainingTimeLbl.setText(secondsToTimeString((int) (duration - playedTimeInSecs)));
            }
            updatePlayerStateTimer.stop();

        } else if (newState.getCurrentPlayerState().isPlaying()) {
            playBtn.setIcon(pauseImageIcon);
            playBtn.setToolTipText(LabelProvider.getLabel("Pause"));
            duration = player.getDurationInSecs();
            stopBtn.setEnabled(true);
            positionSlider.setEnabled(true);

            // update slider only if slide is not being moved by user
            if (!positionSliderGrabbed) {
                positionSlider.setMaximum((int) (duration * SLIDER_UNITS_PER_SECOND));
                positionSlider.setValue((int) (newState.getCurrentPoitionInSecs() * SLIDER_UNITS_PER_SECOND));

                int playedTimeInSecs = (int) newState.getCurrentPoitionInSecs();
                playedTimeLbl.setText(secondsToTimeString(playedTimeInSecs));
                remainingTimeLbl.setText(secondsToTimeString((int) (duration - playedTimeInSecs)));
            }
            updatePlayerStateTimer.start();

        }
    }

    private void updatePlayerTimes(int currPosInSecs) {
        playedTimeLbl.setText(secondsToTimeString(currPosInSecs));
        remainingTimeLbl.setText(secondsToTimeString((int) (duration - currPosInSecs)));
    }

    private void onUpdateTimerTick(ActionEvent e) {
        PlayerStateDetails details = player.getPlayerStateDetails();
        mirrorPlayerState(details);
    }

    private void addSearchTFListener() {
        searchTF.addFocusListener(new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
                String currText = searchTF.getText();
                if (currText.equals("")) {
                    searchTF.setForeground(Color.GRAY);
                    searchTF.setFont(placeholderFont);
                    searchTF.setText(placeholderText);
                }
            }

            @Override
            public void focusGained(FocusEvent e) {
                String currText = searchTF.getText();
                if (currText.equals(placeholderText)) {
                    searchTF.setFont(defaultTextfieldFont);
                    searchTF.setForeground(Color.BLACK);
                    searchTF.setText("");
                }
            }
        });

        searchTF.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                Application.getFilterState().setFilterString(searchTF.getText());
            }
        });
    }

    private void play(AudioItem item) {
        if (player != null) {
            player.stop();
            updatePlayerStateTimer.stop();
        }
        System.out.println("Audio Item to play:" + item.getId());
        Application parent = Application.getApplication();
        try {
            // convert on the fly if necessary
            parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            File f = ACMConfiguration.getInstance().getCurrentDB().getRepository()
                    .getAudioFile(item, AudioFormat.WAV);
            Application.getFilterState().updateResult();
            initPlayer();
            player.play(f);
            updatePlayerStateTimer.start();
            // This is a bit hacky. At this point we know that the slider has been layed out, so we can
            // get it's real size, and apply that to the label.
            Dimension d = sliderBox.getSize();
            d.width -= 20;
            titleInfoLbl.setMaximumSize(d);
            titleInfoLbl.setPreferredSize(d);
            titleInfoLbl.setText(item.getMetadata()
                    .getMetadataValue(MetadataSpecification.DC_TITLE).getValue());
        } catch (IOException | ConversionException | AudioItemRepository.UnsupportedFormatException e) {
            // There really isn't anything to do with this exception.
//      throw new RuntimeException(e);
        } finally {
            parent.setCursor(Cursor.getDefaultCursor());
        }
    }

    private void onApplicationUpdate(Observable o, Object arg) {
        // The player won't work unless we have or can get a .wav file. But if we can get a .wav file, the
        // player works fine.
        if (arg instanceof PlayAudioItemMessage) { // && OsUtils.WINDOWS) {
            PlayAudioItemMessage item = (PlayAudioItemMessage) arg;
            play(item.getAudioItem());
        } else if (arg instanceof SearchRequestMessage) {
            searchTF.setText(((SearchRequestMessage) arg).getSearchString());
        }
    }

    private void initComponents() {
        setPreferredSize(new Dimension(300, TOOLBAR_HEIGHT));
        // I (bill) think the toolbar looks better without this:
        // setBorder(javax.swing.BorderFactory.createEtchedBorder());
        setFloatable(false);
        setRollover(false);

        // Create components
        assistantButton = new JButton();
        configureButton = new JButton();

        backwardBtn = new JButton();
        stopBtn = new JButton();
        stopBtn.setEnabled(false);
        playBtn = new JButton();
        forwardBtn = new JButton();


        positionSlider = new JSlider();
        positionSlider.setEnabled(false);
        playedTimeLbl = new JLabel();
        remainingTimeLbl = new JLabel();
        titleInfoLbl = new JLabel();

        JLabel searchLabel = new JLabel();
        searchTF = new JTextField();

        // Additional component initialization.
        assistantButton.setIcon(assistantImageIcon);
        assistantButton.setToolTipText(LabelProvider.getLabel("Assistants"));
        configureButton.setIcon(settingsImageIcon);
        configureButton.setToolTipText(LabelProvider.getLabel("Configuration"));

        backwardBtn.setIcon(backwardImageIcon);
        backwardBtn.setToolTipText(LabelProvider.getLabel("Previous Message"));
        stopBtn.setIcon(stopImageIcon);
        stopBtn.setToolTipText(LabelProvider.getLabel("Stop Playing"));
        playBtn.setIcon(playImageIcon);
        playBtn.setToolTipText(LabelProvider.getLabel("Play"));
        forwardBtn.setIcon(forwardImageIcon);
        forwardBtn.setToolTipText(LabelProvider.getLabel("Next Message"));

        positionSlider.setValue(0);
        playedTimeLbl.setText(NO_TIME);
        playedTimeLbl.setName("null"); // NOI18N
        remainingTimeLbl.setText(NO_TIME);
        titleInfoLbl.setText(NOT_PLAYING);
        Font f = titleInfoLbl.getFont();
        titleInfoLbl.setFont(f.deriveFont(f.getStyle() | Font.ITALIC));

        defaultTextfieldFont = searchTF.getFont();
        searchLabel.setIcon(searchImageIcon);
        searchTF.setFont(placeholderFont);
        searchTF.setText(placeholderText);
        searchTF.setForeground(Color.GRAY);

        // Accessibility. This is probably just what happened by accident in the Netbeans 6 GUI builder.
        playedTimeLbl.getAccessibleContext().setAccessibleName("");
        stopBtn.getAccessibleContext().setAccessibleName("Stop");
        playBtn.getAccessibleContext().setAccessibleName("Play");

        // Layout -- sizes
        assistantButton.setPreferredSize(ICON_SIZE);
        configureButton.setPreferredSize(ICON_SIZE);

        backwardBtn.setPreferredSize(ICON_SIZE);
        stopBtn.setPreferredSize(ICON_SIZE);
        playBtn.setPreferredSize(ICON_SIZE);
        forwardBtn.setPreferredSize(ICON_SIZE);
        backwardBtn.setMaximumSize(ICON_SIZE);
        stopBtn.setMaximumSize(ICON_SIZE);
        playBtn.setMaximumSize(ICON_SIZE);
        forwardBtn.setMaximumSize(ICON_SIZE);

        positionSlider.setPreferredSize(new Dimension(SCRUBBER_WIDTH, CONTROL_HEIGHT));

        searchLabel.setPreferredSize(ICON_SIZE);
        searchTF.setPreferredSize(new Dimension(SEARCH_FIELD_WIDTH, CONTROL_HEIGHT));

        // Layout -- grouping
        Box playBox = Box.createHorizontalBox();
        playBox.add(backwardBtn);
        playBox.add(stopBtn);
        playBox.add(playBtn);
        playBox.add(forwardBtn);

        Box positionBox = Box.createVerticalBox();
        Box titleBox = Box.createHorizontalBox();
        titleBox.add(Box.createHorizontalGlue());
        titleBox.add(titleInfoLbl);
        titleBox.add(Box.createHorizontalGlue());
        positionBox.add(titleBox);
        titleInfoLbl.setHorizontalAlignment(CENTER);

        sliderBox = Box.createHorizontalBox();
        sliderBox.add(Box.createHorizontalStrut(10));
        sliderBox.add(playedTimeLbl);
        sliderBox.add(Box.createHorizontalStrut(10));
        sliderBox.add(positionSlider);
        sliderBox.add(Box.createHorizontalStrut(10));
        sliderBox.add(remainingTimeLbl);
        sliderBox.add(Box.createHorizontalStrut(10));

        positionBox.add(sliderBox);

        Box searchBox = Box.createHorizontalBox();
        searchBox.add(searchLabel);
        searchBox.add(searchTF);

        // Layout -- finally, layout the toolbar
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        add(assistantButton);
        add(playBox);
        add(positionBox);
        add(searchBox);
        add(configureButton);

        // Set the minimum size to the preferred size, or it will wind up *larger* than the
        // preferrred size, and the control will grow as the toolbar gets very narrow.
        SwingUtilities.invokeLater(() -> {
            playBox.setMinimumSize(playBox.getPreferredSize());
            playBox.setMaximumSize(playBox.getPreferredSize());
        });

    }

}
