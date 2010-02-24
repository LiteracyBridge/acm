package org.literacybridge.acm.sound.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JProgressBar;

import org.literacybridge.acm.sound.SimpleSoundPlayer;
import org.literacybridge.acm.sound.SoundPlayer;

public class TestSoundPlayer extends JComponent {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public TestSoundPlayer(final SoundPlayer player) {
        this.player = player;
        // Create player UI
        play = new JButton("Play");
        pause = new JButton("Pause");
        stop = new JButton("Stop");
        progress = new JProgressBar();
        player.setProgressBar(progress);
        currentClip = new File(TEST_FILE);

        player.setClip(currentClip);
        // When clicked, start playing the sound
        play.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                        player.play();
                }
            });
        pause.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                    player.pause();
            }
        });
        stop.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                    player.stop();
            }
        });
        
        // put those controls in a row
        Box row = Box.createHorizontalBox();
        row.add(play);
        row.add(pause);
        row.add(stop);
        row.add(progress);
        
        // And add them to this component.
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.add(row);
    }
    
    public static void main(String[] args) {
        SoundPlayer player;
        player = new SimpleSoundPlayer();
        TestSoundPlayer player_ui = new TestSoundPlayer(player);
        
        // Create a window
        JFrame f = new JFrame("SoundPlayer");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane( ).add(player_ui, "Center");
        f.pack( );
        f.setVisible(true);
    }
    
    private File currentClip; 
    private SoundPlayer player;
    private JButton play;             // The Play/Stop button
    private JButton pause;
    private JButton stop;
    private JProgressBar progress;         // Shows and sets current position in sound
    private static final String TEST_FILE = "tst/data/test.wav";
}

