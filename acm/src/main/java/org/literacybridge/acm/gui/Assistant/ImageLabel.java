package org.literacybridge.acm.gui.Assistant;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.io.InputStream;

public class ImageLabel extends JLabel {
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

    final boolean MAC_OS = System.getProperty("os.name").startsWith("Mac OS");

    int nCh = 0;
    synchronized void ev(char c) {
        // Debugging code
//        System.out.print(c);
//        if (++nCh >= 80) {
//            System.out.println();
//            nCh = 0;
//        }
    }

    public ImageLabel(ImageIcon icon, String title, Runnable r) {
        super();
        setOpaque(true);
        setIcon(icon);

        Font cf = getCustomFont(36f);
        if (cf != null) {
            setFont(cf);
            setText(title);
        } else {
            String font = MAC_OS ? "Papyrus,Helvetica" : "Palatino Linotype"; // Segoe UI
//        font = "Tahoma";
            String html = String.format("<html><span style='font-weight:100;font-family:"+font+";font-size:2.0em;'>%s</style></html>", title);
            setText(html);
        }

        setBorder(normalBorder);

        // For debugging sizing issues.
//        addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent e) {
//                System.out.println(String.format("Size %dx%d", ImageLabel.this.getWidth(), ImageLabel.this.getHeight()));
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
                if (ImageLabel.this.isEnabled()) {
                    setBackground(normalColor);
                    isHover = false;
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                ev('p');
                if (ImageLabel.this.isEnabled()) {
                    isHover = false;
                    setBackground(pressedColor);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ev('r');
                if (ImageLabel.this.isEnabled()) {
                    isHover = in;
                    setBackground(in ? hoverColor : normalColor);
                    if (in)
                        r.run();
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

//    private static String ttfResource = "Didot-HTF-L24-Light.ttf";
    private static String ttfResource = "Palation_Sans_LT_W04_Light.ttf";
    public static Font getCustomFont(float size) {
        // <div>Font made from <a href="http://www.onlinewebfonts.com">oNline Web Fonts</a>is licensed by CC BY 3.0</div>
        Font font = null;
        InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(ttfResource);
        try {
            Font created = Font.createFont(Font.TRUETYPE_FONT, stream);
            font = created.deriveFont(size);
        } catch (FontFormatException | IOException e) {
            // Ignore.
        }
        return font;
    }

}
