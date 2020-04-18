package org.literacybridge.acm.gui.Assistant;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;

/**
 * A Label that performs the essential function of a JButton. Used to create a nice, big,
 * pretty button with an image.
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public class PanelButton extends JPanel {
    // True if we've set the hover color. So we can set it appropriately on a move event.
    private boolean isHover = false;
    private boolean in = false;
    private boolean isPressed = false;

    private final Color bgColor = getBackground();
    private Color normalColor = bgColor;
    private Color disabledColor = bgColor.brighter();
    private Color normalHoverColor = new Color(0xe0e0e0);
    private Color disabledHoverColor = new Color(0xf3f3f3);
    private Color hoverColor = isEnabled() ? normalHoverColor : disabledHoverColor;
    private Color pressedColor = new Color(0xc0c0c0);

    private Color currentBgColor = bgColor;

    @SuppressWarnings("FieldMayBeFinal")
    private int roundingRadius = 6;

    private static final Color linkColor = new Color(0x337ab7);
    private final Border focusedBorder = new CompoundBorder(new RoundedLineBorder(linkColor/*.brighter()*/, 2, roundingRadius, 2),
        new RoundedLineBorder(bgColor, 1, roundingRadius-2, 2));
    private Border hoverBorder;
    private Border normalBorder;
    private Border disabledBorder = new LineBorder(bgColor, 2);

    private String text;
    private double xPad = 2.0;
    private double yPad = 2.0;

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
     * Create a PanelButton with the given text.
     * @param text of the button
     */
    public PanelButton(String text) {
        super();
        setOpaque(true);

        setBackground(Color.lightGray);
        normalBorder = null;
        hoverBorder = null;

        setText(text);
        applyBorder();
        actionCommand = text;

        setFocusable(true);
        setVisible(true);
        setEnabled(true);

        applyBackground();

        // For debugging sizing issues.
//        addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                System.out.println(String.format("Size %dx%d", LabelButton.this.getWidth(), LabelButton.this.getHeight()));
//            }
//        });

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                applyBorder();
            }

            @Override
            public void focusLost(FocusEvent e) {
                applyBorder();
            }
        });

        addPropertyChangeListener("enabled", evt -> {
            hoverColor = isEnabled() ? normalHoverColor : disabledHoverColor;
            applyBackground();
            applyBorder();
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                ev('m');
                if (!isHover) {
                    isHover = true;
                    applyBackground();
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ev('c');
                if (PanelButton.this.isEnabled()) {
                    isHover = false;
                    isPressed = false;
                    applyBackground();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                ev('p');
                if (PanelButton.this.isEnabled()) {
                    isHover = false;
                    isPressed = true;
                    applyBackground();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ev('r');
                if (PanelButton.this.isEnabled()) {
                    isHover = in;
                    isPressed = false;
                    applyBackground();
                    if (in) {
                        fireActionPerformed();
                    }
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                ev('e');
                isPressed = false;
                isHover = true;
                in = true;
                applyBackground();
                applyBorder();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ev('x');
                isPressed = false;
                isHover = false;
                in = false;
                applyBackground();
                applyBorder();
            }

        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ')
                    fireActionPerformed();
                super.keyTyped(e);
            }
        });
    }

    /**
     * Applies the current border to the panel.
     */
    public void applyBorder() {
        Border border;
        if (isEnabled()) {
            if (isFocusOwner()) border = focusedBorder;
            else if (isHover) border = hoverBorder;
            else border = normalBorder;
        } else border = disabledBorder;
        super.setBorder(border);
    }

    /**
     * Applies the current background to the panel.
     */
    private void applyBackground() {
        Color newBgColor;
        if (isEnabled()) {
            if (isPressed) newBgColor = pressedColor;
            else if (isHover) newBgColor = normalHoverColor;
            else newBgColor = normalColor;
        } else {
            if (isHover) newBgColor = disabledHoverColor;
            else newBgColor = disabledColor;
        }

        if (!newBgColor.equals(currentBgColor)) {
//            System.out.printf("Set bg to %s\n", newBgColor);
            currentBgColor = newBgColor;
            super.setBackground(currentBgColor);
        }
    }

    /**
     * Sets the minimum and preferred size based on text, font, and padding factor. A nicer
     * implementation would keep track of whether the user has explicitly set these sizes
     * and honor those settings.
     */
    private void setSizes() {
        if (text != null) {
            FontMetrics fm = getFontMetrics(getFont());
            int h = (int) (yPad * fm.getHeight());
            int w = (int) (xPad * fm.stringWidth(text));
            Dimension d = new Dimension(w, h);
            setMinimumSize(d);
            setPreferredSize(d);
        }
    }

    public void setText(String text) {
        // If text nullness changes, or text isn't null, but changed, update.
        if ((text == null) != (this.text == null) || text != null && !text.equals(this.text)) {
            this.text = text;
            setSizes();
            repaint();
        }
    }

    public String getText() {
        return this.text;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        setSizes();
    }

    /**
     * This is a padding factor, how many times the width and height of the text should the
     * panel be?
     * @param xPad factor for the width
     * @param yPad factor for the height
     */
    public void setPadding(double xPad, double yPad) {
        if (xPad != 0.0) {
            this.xPad = xPad;
        }
        if (yPad != 0.0) {
            this.yPad = yPad;
        }
        setSizes();
    }

    @Override
    protected void paintComponent(Graphics pG) {
        // We paint everything.
        // super.paintComponent(pG);
        int dh = 0;
        int dw = 0;
        int h = getHeight();
        int w = getWidth();

        Graphics2D g2d = (Graphics2D) pG;
        // We should possibly get the current parent's current background color.
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, w, h);

        g2d.setColor(currentBgColor);
        g2d.fillRoundRect(dw, dh, w - dw * 2, h - dh * 2, roundingRadius, roundingRadius);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.white);
        FontMetrics fm = pG.getFontMetrics();
        Rectangle2D textSize = fm.getStringBounds(text, pG);
        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        int x = (w - (int) textSize.getWidth()) / 2;
        // This seems wrong, theoretically, but seems to work in practice.
        int y = (h + ascent - descent) / 2;
        g2d.drawString(text, x, y);
    }

    /**
     * Average two colors.
     *
     * @param c1 one color
     * @param c2 another color
     * @return average of the RGB values
     */
    private Color average(Color c1, @SuppressWarnings("SameParameterValue") Color c2) {
        return new Color((c1.getRed() + c2.getRed()) / 2,
            (c1.getGreen() + c2.getGreen()) / 2,
            (c1.getBlue() + c2.getBlue()) / 2,
            (c1.getAlpha() + c2.getAlpha()) / 2);
    }

    private Color darker(Color color, @SuppressWarnings("SameParameterValue") double FACTOR) {
        return new Color(Math.max((int) (color.getRed() * FACTOR + 0.5), 0),
            Math.max((int) (color.getGreen() * FACTOR + 0.5), 0),
            Math.max((int) (color.getBlue() * FACTOR + 0.5), 0),
            255);
    }

    public void setBgColorPalette(Color normalColor) {
        Color disabledColor = average(normalColor, Color.white);
        Color hoverColor = darker(normalColor, 0.85);
        Color pressedColor = darker(hoverColor, 0.85);
        setBgColors(normalColor, disabledColor, hoverColor, disabledColor, pressedColor);
        // Try to pick a contrasting text color.
        if (normalColor.getRed() + normalColor.getGreen() + normalColor.getBlue() > 384) {
            setForeground(Color.black);
        } else {
            setForeground(Color.white);
        }
    }

    public void setBgColors(Color normalColor,
        Color disabledColor,
        Color normalHoverColor,
        Color disabledHoverColor,
        Color pressedColor)
    {
        this.normalColor = normalColor;
        this.disabledColor = disabledColor;
        this.normalHoverColor = normalHoverColor;
        this.disabledHoverColor = disabledHoverColor;
        this.hoverColor = isEnabled() ? normalHoverColor : disabledHoverColor;
        this.pressedColor = pressedColor;
        this.normalBorder = new RoundedLineBorder(normalColor.darker(), 1, roundingRadius);
        this.disabledBorder = new RoundedLineBorder(disabledColor.darker(), 1, roundingRadius);
        this.hoverBorder = normalBorder;
        applyBackground();
    }

    public void setBorderColor(Color normalColor) {
        normalBorder = new LineBorder(normalColor, 2);
    }

    public void setHoverBorderColor(Color hoverColor) {
        hoverBorder = new LineBorder(hoverColor, 2);
    }

    private String actionCommand;

    private String getActionCommand() {
        return actionCommand;
    }

    public void setActionCommand(String actionCommand) {
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
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ActionListener.class) {
                // Lazily create the event:
                if (actionEvent == null) {

                    int modifiers = 0;
                    AWTEvent currentEvent = EventQueue.getCurrentEvent();
                    if (currentEvent instanceof InputEvent) {
                        modifiers = ((InputEvent) currentEvent).getModifiers();
                    } else if (currentEvent instanceof ActionEvent) {
                        modifiers = ((ActionEvent) currentEvent).getModifiers();
                    }
                    actionEvent = new ActionEvent(this,
                        ActionEvent.ACTION_PERFORMED,
                        getActionCommand(),
                        EventQueue.getMostRecentEventTime(),
                        modifiers);

                }
                ((ActionListener) listeners[i + 1]).actionPerformed(actionEvent);
            }
        }
    }

//    //    private static String ttfResource = "Didot-HTF-L24-Light.ttf";
//    // A decorative typeface, more subdued than Papyrus.
//    public static final String PALATION = "Palation_Sans_LT_W04_Light.ttf";
//    // A very light, very readable typeface.
//    public static final String AVENIR = "AvenirLTStd-Light.ttf";
//
//    public static Font getCustomFont(float size) {
//        return getCustomFont(PALATION, size);
//    }
//
//    public static Font getCustomFont(String name, float size) {
//        // <div>Font made from <a href="http://www.onlinewebfonts.com">oNline Web Fonts</a>is licensed by CC BY 3.0</div>
//        Font font = null;
//        try {
//            Font created = fontResource(name);
//            font = created.deriveFont(size);
//        } catch (Exception e) {
//            // Ignore.
//        }
//        return font;
//    }
//
//    public static Font fontResource(String name) {
//        Font font = null;
//        InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
//        if (stream != null) {
//            try {
//                font = Font.createFont(Font.TRUETYPE_FONT, stream);
//            } catch (FontFormatException | IOException e) {
//                // Ignore, return null.
//            }
//        }
//        return font;
//    }
}
