package org.literacybridge.acm.gui.assistants;

import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.Assistant;
import org.literacybridge.acm.gui.Assistant.ImageLabel;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.assistants.ContentImport.ContentImportAssistant;
import org.literacybridge.acm.gui.assistants.Deployment.DeploymentAssistant;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.logging.Logger;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static org.literacybridge.acm.utils.SwingUtils.getApplicationRelativeLocation;

public class Chooser extends JDialog {
    private static final Logger LOG = Logger.getLogger(Chooser.class.getName());

    public static void showChooserMenu(ActionEvent actionEvent) {
        Chooser dialog = new Chooser(Application.getApplication());
        // Place the new dialog within the application frame. This is hacky, but if it fails, the dialog
        // simply winds up in a funny place. Unfortunately, Swing only lets us get the location of a
        // component relative to its parent.

        Point pMainView = getApplicationRelativeLocation(Application.getApplication()
            .getMainView());
        pMainView.x += 10;
        pMainView.y += 10;
        dialog.setLocation(pMainView);
        dialog.setVisible(true);
    }

    private Chooser(JFrame parent) {
        super(parent, "", false);
        System.out.println(this.isVisible());
        setResizable(false);
        setUndecorated(true);

        JDialog panel = this;
        panel.setLayout(new GridBagLayout());
        setBackground(new Color(0xe0fff0));

        // USB Flash Drive by fahmionline from the Noun Project
        ImageIcon backwardImageIcon = new ImageIcon(
            UIConstants.getResource(UIConstants.ICON_BACKWARD_24_PX));

        ImageIcon usbIcon = new ImageIcon(UIConstants.getResource("usb_64.png"));
        ImageIcon tbIcon = new ImageIcon(UIConstants.getResource("tb_64g.png"));
        ImageIcon langIcon = new ImageIcon(UIConstants.getResource("language_64.png"));
        ImageIcon plIcon = new ImageIcon(UIConstants.getResource("pl_64.png"));

        Insets insets = new Insets(0,0,0,0);
        GridBagConstraints gbc = new GridBagConstraints(0,GridBagConstraints.RELATIVE, 1,1, 1.0,1.0, CENTER,BOTH, insets, 1,1);

        JLabel header = new JLabel(" ACM Assistants ");
        header.setFont(ImageLabel.getCustomFont(16f));
        Box hbox = Box.createHorizontalBox();
        hbox.setOpaque(true);
        hbox.setBackground(new Color(235, 245, 252));
        hbox.add(Box.createHorizontalGlue());
        hbox.add(header);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        Dimension size = new Dimension(400, 92);
        JLabel contentButton = new ImageLabel(usbIcon, "Import Content", this::runImport);
        contentButton.setMinimumSize(size);
        contentButton.setPreferredSize(size);
        contentButton.setMaximumSize(size);
        panel.add(contentButton, gbc);

        JLabel languageButton = new ImageLabel(langIcon, "Language Prompts", this::runDeployment);
        languageButton.setMinimumSize(size);
        languageButton.setPreferredSize(size);
        panel.add(languageButton, gbc);
        languageButton.setEnabled(false);

        JLabel playlistButton = new ImageLabel(plIcon, "Playlist Prompts", this::runDeployment);
        playlistButton.setMinimumSize(size);
        playlistButton.setPreferredSize(size);
        panel.add(playlistButton, gbc);
        playlistButton.setEnabled(true);

        JLabel deploymentButton = new ImageLabel(tbIcon, "Create Deployment", this::runDeployment);
        deploymentButton.setMinimumSize(size);
        deploymentButton.setPreferredSize(size);
        panel.add(deploymentButton, gbc);

        // To experiment with sizes, uncomment this, and comment out "setUndecorated(true)"
//        panel.addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                super.componentResized(e);
//                System.out.println(String.format("Size %dx%d", panel.getWidth(), panel.getHeight()));
//            }
//        });

        addWindowListener(windowListener);
        addKeyListener(keyListener);
        setAlwaysOnTop(true);
        setUndecorated(true);

        setMinimumSize(new Dimension(400, 4*92));
    }

    private void runDeployment() {
        setVisible(false);
        Assistant da = DeploymentAssistant.create();
        da.setVisible(true);
        boolean finished = da.finished;
        if (finished) System.out.println("deployment created");
    }

    private void runImport() {
        setVisible(false);
        Assistant ca = ContentImportAssistant.create();
        ca.setVisible(true);
        boolean finished = ca.finished;
        if (finished) System.out.println("import finished");
    }

    private KeyListener keyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            // escape?
            if (e.getKeyChar() == 0x1b) {
                setVisible(false);
            }
        }
    };

    private WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowDeactivated(WindowEvent e) {
            setVisible(false);
        }
    };

}
