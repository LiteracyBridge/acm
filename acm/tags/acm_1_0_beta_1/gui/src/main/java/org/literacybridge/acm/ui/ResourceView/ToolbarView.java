package org.literacybridge.acm.ui.ResourceView;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.Timer;

import org.literacybridge.acm.content.LocalizedAudioItem;
import org.literacybridge.acm.metadata.MetadataSpecification;
import org.literacybridge.acm.playerAPI.PlayerStateDetails;
import org.literacybridge.acm.playerAPI.SimpleSoundPlayer;
import org.literacybridge.acm.repository.Repository;
import org.literacybridge.acm.resourcebundle.LabelProvider;
import org.literacybridge.acm.ui.Application;
import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemTableModel;
import org.literacybridge.acm.ui.ResourceView.audioItems.AudioItemView;
import org.literacybridge.acm.util.language.LanguageUtil;
import org.literacybridge.acm.util.language.UILanguageChanged;


public class ToolbarView extends JToolBar implements ActionListener 
													, Observer {

	private static final long serialVersionUID = -1827563460140622507L;

	// Player (will run in a different thread!)
	private SimpleSoundPlayer player;
	private PlayerStateDetails currPlayerDetails = null;
	private double durtation = 0.0;
	private Timer updatePlayerStateTimer = new Timer(100, this);
	
	private ImageIcon backwardImageIcon = new ImageIcon(getClass().getResource("/back-24px.png"));
	private ImageIcon playImageIcon = new ImageIcon(getClass().getResource("/play-24px.png"));
	private ImageIcon pauseImageIcon = new ImageIcon(getClass().getResource("/pause-24px.png"));
	private ImageIcon forwardImageIcon = new ImageIcon(getClass().getResource("/forward-24px.png"));

    private JButton backwardBtn;
    private JButton forwardBtn;
    private JLabel jLabel1;
    private JPanel jPanel1;
    private JSlider positionSlider;
    private boolean positionSliderGrapped = false;
    private JButton playBtn;
    private JLabel playedTimeLbl;
    private JLabel remainingTimeLbl;
    private JTextField searchTF;
    private JLabel titleInfoLbl;
    
    private final AudioItemView audioItemView;

    // Textfield Search
    private String searchFieldWatermarkText = LabelProvider.getLabel(LabelProvider.WATERMARK_SEARCH, LanguageUtil.getUILanguage());
    private Font watermarkTextfieldFont = new Font("Verdana", Font.ITALIC, 16);
    private Font defaultTextfieldFont = null;
    
	public ToolbarView(AudioItemView audioItemView) {
		this.audioItemView = audioItemView;
		initComponents();
		addEventHandler();
		addPositionSliderHandler();
		addSearchTFListener();
		
		Application.getMessageService().addObserver(this);
	}
	
	private boolean initPlayer(File audioFile) {
		if (player == null) {
			this.player = Application.getApplication().getSoundPlayer();
		}
		player.setClip(audioFile);		
		return true;
	}
	
	private String secondsToTimeString(int seconds) {
		final int SECONDS_PER_MINUTE = 60;
		return String.format("%d:%02d", 
				  seconds / SECONDS_PER_MINUTE, 
				  seconds % SECONDS_PER_MINUTE);
	}
	
	// Created with NetBeans 6.8
	private void initComponents() {
  	
		setPreferredSize(new Dimension(300, 80));
		setBorder(javax.swing.BorderFactory.createEtchedBorder());
		setFloatable(false);
		
        backwardBtn = new javax.swing.JButton();
        playBtn = new javax.swing.JButton();
        forwardBtn = new javax.swing.JButton();
        searchTF = new javax.swing.JTextField();
        searchTF.setPreferredSize(new Dimension(100, 30));
        jLabel1 = new javax.swing.JLabel();
        jLabel1.setPreferredSize(new Dimension(200, 30));
        jPanel1 = new javax.swing.JPanel();
        jPanel1.setPreferredSize(new Dimension(100, 30));
        positionSlider = new javax.swing.JSlider();
        playedTimeLbl = new javax.swing.JLabel();
        remainingTimeLbl = new javax.swing.JLabel();
        titleInfoLbl = new javax.swing.JLabel();

        backwardBtn.setIcon(backwardImageIcon); // NOI18N
        backwardBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));

        playBtn.setIcon(playImageIcon); // NOI18N
        playBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));
        playBtn.setPreferredSize(new java.awt.Dimension(44, 44));

        forwardBtn.setIcon(forwardImageIcon); // NOI18N
        forwardBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));

        defaultTextfieldFont = searchTF.getFont();
        searchTF.setFont(watermarkTextfieldFont);
        searchTF.setText(searchFieldWatermarkText);
        searchTF.setForeground(Color.GRAY);
        
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/search-glass-24px.png")));

        
        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        jPanel1.setMinimumSize(new java.awt.Dimension(300, 65));

        positionSlider.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        playedTimeLbl.setText("00:00:00");
        playedTimeLbl.setName("null"); // NOI18N

        remainingTimeLbl.setText("00:00:00");

        titleInfoLbl.setText(" ");
        titleInfoLbl.setHorizontalAlignment(CENTER);
        Font f = titleInfoLbl.getFont();
        titleInfoLbl.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));


        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(playedTimeLbl)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(positionSlider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE)
                    .add(titleInfoLbl, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 386, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(remainingTimeLbl))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(titleInfoLbl)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(playedTimeLbl, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                    .add(remainingTimeLbl, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                    .add(positionSlider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE))
                .addContainerGap())
        );

        playedTimeLbl.getAccessibleContext().setAccessibleName("");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(backwardBtn)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(playBtn, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(forwardBtn)
                .add(18, 18, 18)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jLabel1)
                .add(2, 2, 2)
                .add(searchTF, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 151, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(18, 18, 18)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(forwardBtn, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(playBtn, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(backwardBtn, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(searchTF, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 44, Short.MAX_VALUE)
                            .add(jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 65, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layout.linkSize(new java.awt.Component[] {forwardBtn, jLabel1, searchTF}, org.jdesktop.layout.GroupLayout.VERTICAL);

        playBtn.getAccessibleContext().setAccessibleName("Play");
    }// </editor-fold>     
    
    private void addEventHandler() {
    	addPlayBtnHandler();
    }
    
	private void addPlayBtnHandler() {
		playBtn.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (player == null) {
					int row = audioItemView.audioItemTable.getSelectedRow();
					if (row != -1) {
						AudioItemTableModel.LocalizedAudioItemNode item = 
		                	(AudioItemTableModel.LocalizedAudioItemNode) audioItemView.audioItemTable.getModel().getValueAt(row, 0);
						play(item.getLocalizedAudioItem());
					}
				} else {
					PlayerStateDetails psd = player.getPlayerStateDetails();
					if (psd.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.PAUSED) {
						player.play();
						updatePlayerStateTimer.start();
					} else if (psd.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.RUNNING) {
						player.stop();
						updatePlayerStateTimer.stop();
						playBtn.setIcon(playImageIcon); // call explicit to avoid missing updates
					}
				}
			}			
		});	
		
		forwardBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (player != null) {
					int row = audioItemView.audioItemTable.getSelectedRow();
					if (row != -1 && audioItemView.audioItemTable.getRowCount() > 0) {
						player.stop();
						updatePlayerStateTimer.stop();

						row = (row + 1) % audioItemView.audioItemTable.getRowCount();
						AudioItemTableModel.LocalizedAudioItemNode item = 
		                	(AudioItemTableModel.LocalizedAudioItemNode) audioItemView.audioItemTable.getModel().getValueAt(row, 0);
						audioItemView.audioItemTable.changeSelection(row, 0, false, false);
						play(item.getLocalizedAudioItem());
					}
	
				}
			}			
		});

		backwardBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (player != null) {
					
					int row = audioItemView.audioItemTable.getSelectedRow();
					if (row != -1 && audioItemView.audioItemTable.getRowCount() > 0) {
						player.stop();
						updatePlayerStateTimer.stop();

						row--;
						if (row < 0) {
							row = audioItemView.audioItemTable.getRowCount() - 1;
						}
						AudioItemTableModel.LocalizedAudioItemNode item = 
		                	(AudioItemTableModel.LocalizedAudioItemNode) audioItemView.audioItemTable.getModel().getValueAt(row, 0);
						audioItemView.audioItemTable.changeSelection(row, 0, false, false);
						play(item.getLocalizedAudioItem());
					}
	
				}
			}			
		});
	}

	private void addPositionSliderHandler() {
		positionSlider.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseMoved(MouseEvent e) {
				if (player.getPlayerStateDetails().getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.RUNNING) {
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
		currPlayerDetails = newState;
		if (currPlayerDetails.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.PAUSED) {
			playBtn.setIcon(playImageIcon);
		} else if (currPlayerDetails.getCurrentPlayerState() == SimpleSoundPlayer.PlayerState.RUNNING) {
			playBtn.setIcon(pauseImageIcon);
	    	durtation = player.getDurationInSecs();
	    	
	    	// update only if slide is not moved by user
	    	if (!positionSliderGrapped) {
	    		positionSlider.setMaximum((int) durtation);
	    		positionSlider.setValue((int) currPlayerDetails.getCurrentPoitionInSecs());	    		
	    			
				int playedTimeInSecs = (int) currPlayerDetails.getCurrentPoitionInSecs();
				playedTimeLbl.setText(secondsToTimeString(playedTimeInSecs));
				remainingTimeLbl.setText(secondsToTimeString((int) (durtation - playedTimeInSecs)));
	    	}
		}
	}
	
	private void updatePlayerTimes(int currPosInSecs) {
		playedTimeLbl.setText(secondsToTimeString(currPosInSecs));
		remainingTimeLbl.setText(secondsToTimeString((int) (durtation - currPosInSecs)));
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		PlayerStateDetails details = player.getPlayerStateDetails();
		mirrorPlayerState(details);		
	}    
	
	private void addSearchTFListener() {
		searchTF.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				String currText = searchTF.getText();
				if (currText.equals("") ) {
					searchTF.setForeground(Color.GRAY);
					searchTF.setFont(watermarkTextfieldFont);
					searchTF.setText(searchFieldWatermarkText);
				}
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				String currText = searchTF.getText();
				if (currText.equals(searchFieldWatermarkText) ) {
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

	private void updateControlsLanguage(Locale newLocale) {
		String currText = searchTF.getText();
		if (currText.equals(searchFieldWatermarkText) ) {
			searchFieldWatermarkText = LabelProvider.getLabel(LabelProvider.WATERMARK_SEARCH, newLocale);
			searchTF.setText(searchFieldWatermarkText);
		} else {
			searchFieldWatermarkText = LabelProvider.getLabel(LabelProvider.WATERMARK_SEARCH, newLocale);
		}
	}

	private void play(LocalizedAudioItem item) {
		File f = Repository.getRepository().getWAVFile(item);
		initPlayer(f);
		player.play();
		updatePlayerStateTimer.start();
		titleInfoLbl.setText(item.getMetadata().getMetadataValues(
				MetadataSpecification.DC_TITLE).get(0).getValue());
	}
	
	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof UILanguageChanged) {
			UILanguageChanged newLocale = (UILanguageChanged) arg;
			updateControlsLanguage(newLocale.getNewLocale());
		}		
		
		if (arg instanceof LocalizedAudioItem) {
			LocalizedAudioItem item = (LocalizedAudioItem) arg;
			play(item);
		}
	}
}
