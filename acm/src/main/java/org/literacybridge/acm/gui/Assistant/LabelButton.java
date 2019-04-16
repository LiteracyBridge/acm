package org.literacybridge.acm.gui.Assistant;

import org.literacybridge.acm.utils.OsUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.io.InputStream;

/**
 * A Label that performs the essential function of a JButton. Used to create a nice, big,
 * pretty button with an image.
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public class LabelButton extends JLabel {
    // True if we've set the hover color. So we can set it appropriately on a move event.
    private boolean isHover = false;
    private boolean in = false;

    private Color bgColor = getBackground();
    private Color normalColor = bgColor;
    private Color normalHoverColor  = new Color(0xe0e0e0);
    private Color disabledHoverColor = new Color(0xf3f3f3);
    private Color hoverColor = isEnabled() ? normalHoverColor : disabledHoverColor;
    private Color pressedColor = new Color(0xc0c0c0);

    private Border hoverBorder = new LineBorder(Color.white, 2);
    private Border normalBorder = new LineBorder(bgColor, 2);

//    int nCh = 0;
    private synchronized void ev(char c) {
        // Debugging code
//        System.out.print(c);
//        if (++nCh >= 80) {
//            System.out.println();
//            nCh = 0;
//        }
    }

    /**
     * Create a LabelButton with only an image.
     * @param icon for the button.
     */
    public LabelButton(ImageIcon icon) {
        this(icon, null);
    }

    /**
     * Create a LabelButton with an image and text.
     * @param icon for the button.
     * @param text for the button.
     */
    public LabelButton(ImageIcon icon, String text) {
        super();
        setOpaque(true);
        setIcon(icon);

        setText(text);
        setBorder(normalBorder);
        actionCommand = text;

        // For debugging sizing issues.
//        addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                System.out.println(String.format("Size %dx%d", LabelButton.this.getWidth(), LabelButton.this.getHeight()));
//            }
//        });

        addPropertyChangeListener("enabled",
            evt -> hoverColor = isEnabled() ? normalHoverColor : disabledHoverColor);

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                ev('m');
                if (!isHover) {
                    isHover = true;
                    setBackground(hoverColor);
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ev('c');
                if (LabelButton.this.isEnabled()) {
                    setBackground(normalColor);
                    isHover = false;
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                ev('p');
                if (LabelButton.this.isEnabled()) {
                    isHover = false;
                    setBackground(pressedColor);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ev('r');
                if (LabelButton.this.isEnabled()) {
                    isHover = in;
                    setBackground(in ? hoverColor : normalColor);
                    if (in) {
                        fireActionPerformed();
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                ev('e');
                isHover = true;
                in = true;
                setBackground(hoverColor);
                setBorder(hoverBorder);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ev('x');
                isHover = false;
                in = false;
                setBackground(normalColor);
                setBorder(normalBorder);
            }

        });
    }

    @Override
    public void setText(String text) {
        if (text == null) {
            super.setText(null);
        } else {
            Font cf = getCustomFont(36f);
            if (cf != null) {
                setFont(cf);
                super.setText(text);
            } else {
                String font = OsUtils.MAC_OS ? "Papyrus,Helvetica" : "Palatino Linotype"; // Segoe UI
//                font = "Tahoma";
                String html = String.format("<html><span style='font-weight:100;font-family:" + font
                    + ";font-size:2.0em;'>%s</style></html>", text);
                super.setText(html);
            }
        }


    }

    private String actionCommand;
    private String getActionCommand() {
        return actionCommand;
    }
    void setActionCommand(String actionCommand) {
        this.actionCommand = actionCommand;
    }

    /**
     * Action support. Copied directly from Swing.
     */
    private ActionEvent actionEvent = null;
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }
    private void fireActionPerformed() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >=0; i -= 2){
            if (listeners[i] == ActionListener.class) {
                // Lazily create the event:
                if (actionEvent == null) {

                    int modifiers = 0;
                    AWTEvent currentEvent = EventQueue.getCurrentEvent();
                    if (currentEvent instanceof InputEvent) {
                        modifiers = ((InputEvent)currentEvent).getModifiers();
                    } else if (currentEvent instanceof ActionEvent) {
                        modifiers = ((ActionEvent)currentEvent).getModifiers();
                    }
                    actionEvent =
                        new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                            getActionCommand(),
                            EventQueue.getMostRecentEventTime(),
                            modifiers);

                }
                ((ActionListener) listeners[i + 1]).actionPerformed(actionEvent);
            }
        }
    }


    //    private static String ttfResource = "Didot-HTF-L24-Light.ttf";
    public static final String PALATION = "Palation_Sans_LT_W04_Light.ttf";
    public static final String AVENIR = "AvenirLTStd-Light.ttf";
    public static Font getCustomFont(float size) {
        // <div>Font made from <a href="http://www.onlinewebfonts.com">oNline Web Fonts</a>is licensed by CC BY 3.0</div>
        Font font = null;
        try {
            Font created = fontResource(PALATION);
            font = created.deriveFont(size);
        } catch (Exception e) {
            // Ignore.
        }
        return font;
    }
    public static Font fontResource(String name) {
        Font font = null;
        InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
        if (stream != null) {
            try {
                font = Font.createFont(Font.TRUETYPE_FONT, stream);
            } catch (FontFormatException | IOException e) {
                // Ignore, return null.
            }
        }
        return font;
    }
}
