package org.literacybridge.acm.ui.player;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.literacybridge.acm.player.SimpleSoundPlayer;

public class PlayerUI extends Container {

	private static final long serialVersionUID = -1827563460140622507L;

	// Player (will run in a different thread!)
	private SimpleSoundPlayer player = new SimpleSoundPlayer();
	
	private JButton playPauseBtn = null;
	private JButton forwardBtn = null;
	private JButton backwardBtn = null;
	private JSlider positionSlider = null;
	private JLabel playerTimeLbl = null;
	private JLabel remainingTimeLbl = null;
	
	ImageIcon imagePlay = new ImageIcon(getClass().getResource("/play-24px.png"));
	ImageIcon imageRight = new ImageIcon(getClass().getResource("/back-24px.png"));
	ImageIcon imageLeft = new ImageIcon(getClass().getResource("/forward-24px.png"));
	ImageIcon imagePause = new ImageIcon(getClass().getResource("/pause-24px.png"));
	
	public PlayerUI() {
		createPlayerUI();	
		
		// testing
		String audioFile = "/Volumes/MAC_HOME/USERS/coder/Projects/talkingbook/acm/TestData/testWav.wav";
		initPlayer(audioFile);
	}
	
	private void createPlayerUI() {
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;		
		
		/*
		 * buttons
		 */
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 3));
		
		forwardBtn = new JButton(imageRight);
		buttonPanel.add(forwardBtn);
		playPauseBtn = new JButton(imagePlay);
		buttonPanel.add(playPauseBtn);
		backwardBtn = new JButton(imageLeft);
		buttonPanel.add(backwardBtn);

		c.gridx = 2;
		c.gridy = 0;
		c.gridwidth = 4;
		add(buttonPanel, c);
		
		/*
		 * Labels & Slider
		 */
		JPanel timePanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 3));
		playerTimeLbl = new JLabel(secondsToTimeString(0));
		timePanel.add(playerTimeLbl);
		positionSlider = new JSlider();
		timePanel.add(positionSlider);
		remainingTimeLbl = new JLabel(secondsToTimeString(0));
		timePanel.add(remainingTimeLbl);
	
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 6;
		add(timePanel, c);
	}	
	


	private boolean initPlayer(String audioFilePath) {
		File audioFile = new File(audioFilePath);
		player.setClip(audioFile);		
		return true;
	}
	
	
	private String secondsToTimeString(int seconds) {
		final int SECONDS_PER_MINUTE = 60;
		return String.format("%d:%02d", 
				  seconds / SECONDS_PER_MINUTE, 
				  seconds % SECONDS_PER_MINUTE);
	}
}
