package org.literacybridge.acm.gui.assistants;

import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.Assistant;
import org.literacybridge.acm.gui.Assistant.LabelButton;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.gui.assistants.ContentImport.ContentImportAssistant;
import org.literacybridge.acm.gui.assistants.Deployment.DeploymentAssistant;
import org.literacybridge.acm.gui.assistants.GreetingsImport.GreetingsImportAssistant;
import org.literacybridge.acm.gui.assistants.PromptsImport.PromptImportAssistant;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static org.literacybridge.acm.utils.SwingUtils.addEscapeListener;
import static org.literacybridge.acm.utils.SwingUtils.getApplicationRelativeLocation;

public class Chooser extends JDialog {
    private static final Logger LOG = Logger.getLogger(Chooser.class.getName());
    private static Chooser dialog;

    synchronized private static void deactivate() {
        if (dialog != null) {
            dialog.setVisible(false);
            dialog = null;
        }
    }

    synchronized public static void showChooserMenu(@SuppressWarnings("unused") ActionEvent actionEvent) {
        deactivate();
        dialog = new Chooser(Application.getApplication());
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
        setResizable(false);
        setUndecorated(true);

        JDialog panel = this;
        panel.setLayout(new GridBagLayout());
        setBackground(new Color(0xe0fff0));

        // USB Flash Drive by fahmionline from the Noun Project
        ImageIcon usbIcon = new ImageIcon(UIConstants.getResource("usb_64.png"));
        ImageIcon tbIcon = new ImageIcon(UIConstants.getResource("tb_64g.png"));
        ImageIcon langIcon = new ImageIcon(UIConstants.getResource("language_64.png"));
        ImageIcon plIcon = new ImageIcon(UIConstants.getResource("pl_64.png"));
        ImageIcon peopleIcon = new ImageIcon(UIConstants.getResource("people_64.png"));

        Insets insets = new Insets(0,0,0,0);
        GridBagConstraints gbc = new GridBagConstraints(0,GridBagConstraints.RELATIVE, 1,1, 1.0,1.0, CENTER,BOTH, insets, 1,1);

        JLabel header = new JLabel(" ACM Assistants ");
        header.setFont(LabelButton.getCustomFont(16f));
        Box hbox = Box.createHorizontalBox();
        hbox.setOpaque(true);
        hbox.setBackground(new Color(235, 245, 252));
        hbox.add(Box.createHorizontalGlue());
        hbox.add(header);
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        Dimension size = new Dimension(400, 92);
        LabelButton contentButton = new LabelButton(usbIcon, "Import Content");
        contentButton.addActionListener(e -> runAssistant(ContentImportAssistant::create));
        contentButton.setMinimumSize(size);
        contentButton.setPreferredSize(size);
//        contentButton.setMaximumSize(size);
        panel.add(contentButton, gbc);

        LabelButton playlistButton = new LabelButton(plIcon, "Playlist Prompts");
        playlistButton.addActionListener(e -> runAssistant(ContentImportAssistant::createPromptImporter));
        playlistButton.setMinimumSize(size);
        playlistButton.setPreferredSize(size);
        panel.add(playlistButton, gbc);
//        playlistButton.setEnabled(true);

        LabelButton deploymentButton = new LabelButton(tbIcon, "Create Deployment");
        deploymentButton.addActionListener(e -> runAssistant(DeploymentAssistant::create));
        deploymentButton.setMinimumSize(size);
        deploymentButton.setPreferredSize(size);
        panel.add(deploymentButton, gbc);

        LabelButton greetingsButton = new LabelButton(peopleIcon, "Custom Greetings");
        greetingsButton.addActionListener(e -> runAssistant(GreetingsImportAssistant::create));
        greetingsButton.setMinimumSize(size);
        greetingsButton.setPreferredSize(size);
        panel.add(greetingsButton, gbc);
//        greetingsButton.setEnabled(true);

        LabelButton languageButton = new LabelButton(langIcon, "System Prompts");
        languageButton.addActionListener(e -> runAssistant(PromptImportAssistant::create));
        languageButton.setMinimumSize(size);
        languageButton.setPreferredSize(size);
        panel.add(languageButton, gbc);
//        languageButton.setEnabled(true);

        // To experiment with sizes, uncomment this, and comment out "setUndecorated(true)"
//        panel.addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                super.componentResized(e);
//                System.out.println(String.format("Size %dx%d", panel.getWidth(), panel.getHeight()));
//            }
//        });

        addEscapeListener(this);
        addWindowListener(windowListener);
        setAlwaysOnTop(true);
        setUndecorated(true);

        setMinimumSize(new Dimension(400, 4*92));
    }

    private void runAssistant(Supplier<Assistant> factory) {
        setVisible(false);
        Assistant da = factory.get();
        da.setVisible(true);
    }

    private WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowDeactivated(WindowEvent e) {
            deactivate();
        }
    };

}
