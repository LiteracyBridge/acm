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
import org.literacybridge.acm.gui.settings.SettingsDialog;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.MetadataSpecification;
import org.literacybridge.acm.utils.OsUtils;

import javax.swing.*;
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

public class ToolbarView extends JToolBar  {

  private static final long serialVersionUID = -1827563460140622507L;

  private static final int TOOLBAR_HEIGHT = 45;
  private static final int CONTROL_HEIGHT = 30;
  private static final int SCRUBBER_WIDTH = 200;
  private static final int SEARCH_FIELD_WIDTH = 300;

  private static final Dimension ICON_SIZE = new Dimension(40, 32);

  // Player (will run in a different thread!)
  private SimpleSoundPlayer player;
  private double durtation = 0.1;
  private Timer updatePlayerStateTimer = new Timer(100, this::onUpdateTimerTick);

  private ImageIcon backwardImageIcon = new ImageIcon(
      UIConstants.getResource(UIConstants.ICON_BACKWARD_24_PX));
  private ImageIcon playImageIcon = new ImageIcon(
      UIConstants.getResource(UIConstants.ICON_PLAY_24_PX));
  private ImageIcon pauseImageIcon = new ImageIcon(
      UIConstants.getResource(UIConstants.ICON_PAUSE_24_PX));
  private ImageIcon forwardImageIcon = new ImageIcon(
      UIConstants.getResource(UIConstants.ICON_FORWARD_24_PX));
  private ImageIcon searchImageIcon = new ImageIcon(
      UIConstants.getResource(UIConstants.ICON_SEARCH_32_PX));

  private ImageIcon gearImageIcon = new ImageIcon(
      UIConstants.getResource(UIConstants.ICON_GEAR_32_PX));
  private ImageIcon assistantImageIcon = new ImageIcon(
      UIConstants.getResource(UIConstants.ICON_ASSISTANT_32_PX));

  // assistant ("b") <div>Icon made from <a href="http://www.onlinewebfonts.com/icon">Icon Fonts</a> is licensed by CC BY 3.0</div>

  // Intelligence Assistance by ProSymbols from the Noun Project
  // config icon: configurations by I Putu Kharismayadi from the Noun Project
  // gears: configuration by Bieutuong Hai from the Noun Project
  
  private JButton configureButton;

  private JButton backwardBtn;
  private JButton forwardBtn;
  private JSlider positionSlider;
  private boolean positionSliderGrapped = false;
  private JButton playBtn;
  private JLabel playedTimeLbl;
  private JLabel remainingTimeLbl;
  private JTextField searchTF;
  private JLabel titleInfoLbl;

  // Textfield Search
  private String placeholderText = LabelProvider.getLabel(LabelProvider.PLACEHOLDER_TEXT);
  private Font placeholderFont = new Font("Verdana", Font.ITALIC, 14);
  private Font defaultTextfieldFont = null;
  private JButton assistantButton;

  public ToolbarView(AudioItemView audioItemView) {
    initComponents();
    addEventHandler();
    addPositionSliderHandler();
    addSearchTFListener();

    Application.getMessageService().addObserver(this::onApplicationUpdate);
  }

  private void initPlayer(File audioFile) {
    if (player == null) {
      this.player = Application.getApplication().getSoundPlayer();
    }
    player.setClip(audioFile);
  }

  private String secondsToTimeString(int seconds) {
    final int SECONDS_PER_MINUTE = 60;
    return String.format("%d:%02d", seconds / SECONDS_PER_MINUTE,
        seconds % SECONDS_PER_MINUTE);
  }

  private void addEventHandler() {
    addPlayBtnHandler();
    // Today, we have only one configuration option, the visible categories, so we can
    // go straight to that dialog. If and when we have more configurations, this will
    // open some sort of configuration container.

    configureButton.addActionListener(SettingsDialog::showDialog);
//    configureButton.addActionListener(VisibleCategoriesDialog::showDialog);
    assistantButton.addActionListener(Chooser::showChooserMenu);
  }

  private void addPlayBtnHandler() {
    playBtn.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseClicked(MouseEvent e) {
        if (player == null) {
          RequestAudioItemToPlayMessage msg = new RequestAudioItemToPlayMessage(
              RequestAudioItemMessage.RequestType.Current);
          Application.getMessageService().pumpMessage(msg);
        } else {
          PlayerStateDetails psd = player.getPlayerStateDetails();
          if (psd.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.PAUSED) {
            player.play();
            updatePlayerStateTimer.start();
          } else if (psd.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.RUNNING) {
            player.stop();
            updatePlayerStateTimer.stop();
            playBtn.setIcon(playImageIcon); // call explicit to avoid missing updates
            playBtn.setToolTipText("Play");
          }
        }
      }
    });

    forwardBtn.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (player != null) {
          RequestAudioItemToPlayMessage msg = new RequestAudioItemToPlayMessage(
              RequestAudioItemMessage.RequestType.Next);
          Application.getMessageService().pumpMessage(msg);
        }
      }
    });

    backwardBtn.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (player != null) {
          RequestAudioItemToPlayMessage msg = new RequestAudioItemToPlayMessage(
              RequestAudioItemMessage.RequestType.Previews);
          Application.getMessageService().pumpMessage(msg);
        }
      }
    });
  }

  private void addPositionSliderHandler() {
    positionSlider.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseMoved(MouseEvent e) {
        if (player.getPlayerStateDetails()
            .getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.RUNNING) {
          int value = positionSlider.getValue();
          updatePlayerTimes(value);
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        positionSliderGrapped = true;
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        PlayerStateDetails psd = player.getPlayerStateDetails();
        // handle only if player is running
        if (psd.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.RUNNING) {
          int value = positionSlider.getValue();
          player.play(value);
        }
        positionSliderGrapped = false;
      }
    });
  }

  private void mirrorPlayerState(PlayerStateDetails newState) {
    if (newState.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.PAUSED) {
      playBtn.setIcon(playImageIcon);
      playBtn.setToolTipText("Play");
    } else if (newState.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.RUNNING) {
      playBtn.setIcon(pauseImageIcon);
      playBtn.setToolTipText("Pause");
      durtation = player.getDurationInSecs();

      // update only if slide is not moved by user
      if (!positionSliderGrapped) {
        positionSlider.setMaximum((int) durtation);
        positionSlider.setValue((int) newState.getCurrentPoitionInSecs());

        int playedTimeInSecs = (int) newState.getCurrentPoitionInSecs();
        playedTimeLbl.setText(secondsToTimeString(playedTimeInSecs));
        remainingTimeLbl.setText(secondsToTimeString((int) (durtation - playedTimeInSecs)));
      }
    }
  }

  private void updatePlayerTimes(int currPosInSecs) {
    playedTimeLbl.setText(secondsToTimeString(currPosInSecs));
    remainingTimeLbl.setText(secondsToTimeString((int) (durtation - currPosInSecs)));
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
    try {
      // convert on the fly if necessary
      Application parent = Application.getApplication();
      parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      File f = ACMConfiguration.getInstance().getCurrentDB().getRepository()
          .convert(item, AudioFormat.WAV);
      parent.setCursor(Cursor.getDefaultCursor());
      Application.getFilterState().updateResult();
      initPlayer(f);
      player.play();
      updatePlayerStateTimer.start();
      titleInfoLbl.setText(item.getMetadata()
          .getMetadataValue(MetadataSpecification.DC_TITLE).getValue());
    } catch (IOException | ConversionException e) {
      throw new RuntimeException(e);
    }
  }

  private void onApplicationUpdate(Observable o, Object arg) {
    if (arg instanceof PlayAudioItemMessage && OsUtils.WINDOWS) {
      PlayAudioItemMessage item = (PlayAudioItemMessage) arg;
      play(item.getAudioItem());
    }

    if (arg instanceof SearchRequestMessage) {
      searchTF.setText(((SearchRequestMessage) arg).getSearchString());
    }
  }

  private void initComponents() {
    boolean showConfigButton = !ACMConfiguration.getInstance().getCurrentDB().isSandboxed() ||
                               ACMConfiguration.getInstance().getCurrentDB().configurationDialog();

    setPreferredSize(new Dimension(300, TOOLBAR_HEIGHT));
    // I (bill) think the toolbar looks better without this:
    // setBorder(javax.swing.BorderFactory.createEtchedBorder());
    setFloatable(false);
    setRollover(false);

    // Create components
    assistantButton = new JButton();
    configureButton = new JButton();

    backwardBtn = new JButton();
    playBtn = new JButton();
    forwardBtn = new JButton();

    positionSlider = new JSlider();
    playedTimeLbl = new JLabel();
    remainingTimeLbl = new JLabel();
    titleInfoLbl = new JLabel();

    JLabel searchLabel = new JLabel();
    searchTF = new JTextField();

    // Additional component initialization.
    assistantButton.setIcon(assistantImageIcon);
    assistantButton.setToolTipText("Assistants");
    configureButton.setIcon(gearImageIcon);
    configureButton.setToolTipText("Configuration");

    backwardBtn.setIcon(backwardImageIcon);
    backwardBtn.setToolTipText("Previous Track");
    playBtn.setIcon(playImageIcon);
    playBtn.setToolTipText("Play");
    forwardBtn.setIcon(forwardImageIcon);
    forwardBtn.setToolTipText("Next Track");

    positionSlider.setValue(0);
    playedTimeLbl.setText("00:00:00");
    playedTimeLbl.setName("null"); // NOI18N
    remainingTimeLbl.setText("00:00:00");
    titleInfoLbl.setText(" ");
    Font f = titleInfoLbl.getFont();
    titleInfoLbl.setFont(f.deriveFont(f.getStyle() | Font.BOLD));

    defaultTextfieldFont = searchTF.getFont();
    searchLabel.setIcon(searchImageIcon);
    searchTF.setFont(placeholderFont);
    searchTF.setText(placeholderText);
    searchTF.setForeground(Color.GRAY);

    // Accessibility. This is probably just what happened by accident in the Netbeans 6 GUI builder.
    playedTimeLbl.getAccessibleContext().setAccessibleName("");
    playBtn.getAccessibleContext().setAccessibleName("Play");

    // Layout -- sizes
    assistantButton.setPreferredSize(ICON_SIZE);
    configureButton.setPreferredSize(ICON_SIZE);

    backwardBtn.setPreferredSize(ICON_SIZE);
    playBtn.setPreferredSize(ICON_SIZE);
    forwardBtn.setPreferredSize(ICON_SIZE);
    backwardBtn.setMaximumSize(ICON_SIZE);
    playBtn.setMaximumSize(ICON_SIZE);
    forwardBtn.setMaximumSize(ICON_SIZE);

    positionSlider.setPreferredSize(new Dimension(SCRUBBER_WIDTH, CONTROL_HEIGHT));

    searchLabel.setPreferredSize(ICON_SIZE);
    searchTF.setPreferredSize(new Dimension(SEARCH_FIELD_WIDTH, CONTROL_HEIGHT));

    // Layout -- grouping
    Box playBox = Box.createHorizontalBox();
    playBox.add(backwardBtn);
    playBox.add(playBtn);
    playBox.add(forwardBtn);
//    playBox.setMaximumSize(playBox.getPreferredSize());

    Box positionBox = Box.createVerticalBox();
    positionBox.add(titleInfoLbl);
    titleInfoLbl.setHorizontalAlignment(CENTER);

    Box sliderBox = Box.createHorizontalBox();
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
    if (showConfigButton) {
      add(configureButton);
    }

    // Set the minimum size to the preferred size, or it will wind up *larger* than the
    // preferrred size, and the control will grow as the toolbar gets very narrow.
    SwingUtilities.invokeLater(() -> {
      playBox.setMinimumSize(playBox.getPreferredSize());
      playBox.setMaximumSize(playBox.getPreferredSize());
    });

  }

}
