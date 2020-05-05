package org.literacybridge.acm.gui.Assistant;

import org.literacybridge.acm.gui.UIConstants;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.Document;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FlexTextField extends PlaceholderTextField {
    private static final char MASK_CHAR = '●';
    private static final char NOMASK_CHAR = (char)'\0';
    private IconHelper helper;
    private IconHelper getHelper() {
        if (helper == null) {
            helper = new IconHelper(this);
            helper.setClickHandler(this::onClicked);
        }
        return helper;
    }

    private boolean isPassword = false;
    private boolean revealPasswordEnabled = false;
    private boolean isPasswordRevealed = false;

    private static ImageIcon eyeIcon;
    private static ImageIcon noEyeIcon;

    private FlexTextField synchronizedPasswordView;

    public FlexTextField() {
        this(null, null, 0);
    }

    public FlexTextField(Document pDoc, String pText, int pColumns)
    {
        super(pDoc, pText, pColumns);
    }

    public FlexTextField(int pColumns) {
        this(null, null, pColumns);
    }

    public FlexTextField(String pText) {
        this(null, pText, 0);
    }

    public FlexTextField(String pText, int pColumns) {
        this(null, pText, pColumns);
    }

    @Override
    protected void paintComponent(Graphics pG) {
        super.paintComponent(pG);
        getHelper().onPaintComponent(pG);
    }

    @Override
    public void setText(String t) {
        if (t==null && getText()!=null || t!=null && !t.equals(getText()))
            super.setText(t);
    }

    public void setIcon(ImageIcon icon) {
        getHelper().onSetIcon(icon);
    }

    public void setIconSpacing(int spacing) {
        getHelper().onSetIconSpacing(spacing);
    }

    public void setIconRight(boolean right) {
        getHelper().onSetRight(right);
    }

    @Override
    public void setBorder(Border border) {
        getHelper().onSetBorder(border);
        super.setBorder(getHelper().getBorder());
    }

    public void setGreyBorder() {
        getHelper().onSetGreyBorder();
    }

    public boolean isPassword() {
        return isPassword;
    }

    public FlexTextField setIsPassword(boolean password) {
        if (isPassword != password) {
            if (password) {
                revealPasswordEnabled = false;
                isPasswordRevealed = false;
            }
            isPassword = password;
            setPasswordDecorations();
        }
        return this;
    }

    public boolean isRevealPasswordEnabled() {
        return revealPasswordEnabled;
    }

    public FlexTextField setRevealPasswordEnabled(boolean revealPasswordEnabled) {
        if (this.revealPasswordEnabled != revealPasswordEnabled) {
            this.revealPasswordEnabled = revealPasswordEnabled;
            setPasswordDecorations();
        }
        return this;
    }

    public boolean isPasswordRevealed() {
        return isPasswordRevealed;
    }

    public FlexTextField setPasswordRevealed(boolean passwordRevealed) {
        isPasswordRevealed = passwordRevealed;
        return this;
    }

    public boolean setSynchronizedPasswordView(FlexTextField other) {
        if (other == null) {
            // Disconnect.
            if (synchronizedPasswordView != null) {
                synchronizedPasswordView.setSynchronizedPasswordView(null);
                synchronizedPasswordView = null;
            }
            return true;
        } else {
            // Connect.
            if (synchronizedPasswordView != null) {
                // Already connected.
                return false;
            } else {
                synchronizedPasswordView = other;
                if (other.setSynchronizedPasswordView(this)) {
                    // This is the first one, if that matters.
                }
                return true;
            }
        }
    }
    boolean synchronizing = false;
    private void synchronizePasswordView() {
        if (synchronizing || synchronizedPasswordView==null) return;
        synchronizing = true;
        try {
            synchronizedPasswordView.synchronizePasswordView(revealPasswordEnabled, isPasswordRevealed);
        } finally {
            synchronizing = false;
        }
    }
    private void synchronizePasswordView(boolean revealEnabled, boolean revealed) {
        if (synchronizing || synchronizedPasswordView==null) return;
        synchronizing = true;
        try {
            revealPasswordEnabled = revealEnabled;
            isPasswordRevealed = revealed;
            setPasswordDecorations();
        } finally {
            synchronizing = false;
        }
    }

    private void setPasswordDecorations() {
        setMaskChar(isPasswordRevealed?NOMASK_CHAR:MASK_CHAR);
        if (revealPasswordEnabled) {
            setIconRight(true);
            setIcon(isPasswordRevealed ? getEyeIcon() : getNoEyeIcon());
        } else {
            setIcon(null);
        }
        synchronizePasswordView();
    }

    private void onClicked() {
        if (!isPassword || !revealPasswordEnabled) {
            return;
        }
        isPasswordRevealed = !isPasswordRevealed;
        setPasswordDecorations();
//        if (isPasswordRevealed) {
//            setMaskChar(NOMASK_CHAR);
//            setIcon(getNoEyeIcon());
//        } else {
//            setMaskChar(MASK_CHAR);
//            setIcon(getEyeIcon());
//        }
    }

    private ImageIcon getEyeIcon() {
        if (eyeIcon == null) {
            eyeIcon = new ImageIcon(UIConstants.getResource("eye_256.png"));
        }
        return eyeIcon;
    }

    private ImageIcon getNoEyeIcon() {
        if (noEyeIcon == null) {
            noEyeIcon = new ImageIcon(UIConstants.getResource("no-eye_256.png"));
        }
        return noEyeIcon;
    }

    class IconHelper {
        private static final int ICON_SPACING = 4;

        private Color borderColor;
        private Border border;
        private ImageIcon givenIcon;
        private Icon scaledIcon;
        private Border originalBorder;
//        private final PlaceholderTextField textField;
        private int iconSpacing = ICON_SPACING;
        private boolean iconRight = false;

        private int height;

        IconHelper(PlaceholderTextField component) {
//            textField = component;
            originalBorder = component.getBorder();
            border = originalBorder;

            MouseAdapter mouseListener = new MouseAdapter() {
                private boolean in = false;
                private boolean hitTest(MouseEvent e) {
                    if (scaledIcon == null) return false;
                    int x = e.getX();
                    Insets iconInsets = originalBorder.getBorderInsets(FlexTextField.this);
                    int iconHitX0 = iconRight ? FlexTextField.this.getWidth()- scaledIcon.getIconWidth()-iconInsets.right : 0;
                    int iconHitX1 = iconRight ? FlexTextField.this.getWidth() : scaledIcon.getIconWidth() + iconInsets.left;
                    return (x >= iconHitX0 && x <= iconHitX1);
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (hitTest(e)) {
                        onClicked();
                    } else {
                        super.mouseClicked(e);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    super.mouseEntered(e);
                    in = hitTest(e);
                    FlexTextField.this.setCursor(Cursor.getPredefinedCursor(in ? Cursor.DEFAULT_CURSOR : Cursor.TEXT_CURSOR));
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    super.mouseMoved(e);
                    boolean newIn = hitTest(e);
                    if (newIn != in) {
                        in = newIn;
                        FlexTextField.this.setCursor(Cursor.getPredefinedCursor(in ? Cursor.DEFAULT_CURSOR : Cursor.TEXT_CURSOR));
                    }
                }
            };
            component.addMouseListener(mouseListener);
            component.addMouseMotionListener(mouseListener);

            height = -1;
            ComponentListener resizeListener = new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    int h = FlexTextField.this.getFontMetrics(FlexTextField.this.getFont()).getHeight();
                    if (h != height) {
                        IconHelper.this.height = h;
                        System.out.printf("Resized to height %d\n", h);
                        resetBorder();
                    }
                }
            };
            component.addComponentListener(resizeListener);
        }

        /**
         * The click handler is called when the icon is clicked.
         */
        Runnable clickHandler = null;
        private void setClickHandler(Runnable clickHandler) {
            this.clickHandler = clickHandler;
        }
        void onClicked() {
            if (clickHandler != null) {
                clickHandler.run();
            }
        }

        Border getBorder() {
            return border;
        }

        void onPaintComponent(Graphics g) {
            if (scaledIcon != null) {
                Insets iconInsets = originalBorder.getBorderInsets(FlexTextField.this);
                int iconPaintOffsetX = iconRight ?
                                       FlexTextField.this.getWidth() - scaledIcon.getIconWidth() - iconInsets.right
                                           - iconSpacing :
                                       iconInsets.left + iconSpacing;
                scaledIcon.paintIcon(FlexTextField.this, g, iconPaintOffsetX, iconInsets.top+1);
            }
        }

        void onSetBorder(Border border) {
            originalBorder = border;

            if (givenIcon == null) {
                this.border = border;
            } else {
                // Optimistically assume the icon is already the desired new height.
                int newWidth = givenIcon.getIconWidth();
                int newHeight = givenIcon.getIconHeight();
                int textFieldHeight = FlexTextField.this.getFontMetrics(FlexTextField.this.getFont()).getHeight() - 1;
                if (textFieldHeight == 0) textFieldHeight = FlexTextField.this.getPreferredSize().height - 1;
                if(newHeight != textFieldHeight)
                {
                    newHeight = textFieldHeight;
                    newWidth = (givenIcon.getIconWidth() * newHeight) / givenIcon.getIconHeight();
                }

                scaledIcon = new ImageIcon(givenIcon.getImage().getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH));

                int w = newWidth + iconSpacing * 2;
                int l = iconRight ? 0 : w;
                int r = iconRight ? w : 0;
                Border margin = BorderFactory.createMatteBorder(0, l, 0, r, borderColor);
                this.border = BorderFactory.createCompoundBorder(border, margin);
            }
        }

        private void onSetGreyBorder() {
            borderColor = new Color(0, 0, 0, 0.07f);
        }

        void onSetIcon(ImageIcon icon) {
            if (icon != this.givenIcon) {
                this.scaledIcon = null;
                this.givenIcon = icon;
                resetBorder();
            }
        }

        private void resetBorder() {
            FlexTextField.this.setBorder(originalBorder);
        }

        public void onSetIconSpacing(int spacing) {
            iconSpacing = spacing;
        }

        public void onSetRight(boolean right) {
            if (iconRight != right) {
                iconRight = right;
                resetBorder();
            }
        }
    }
}
