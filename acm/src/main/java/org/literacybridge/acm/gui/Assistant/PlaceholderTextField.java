package org.literacybridge.acm.gui.Assistant;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.GapContent;
import javax.swing.text.PlainDocument;
import javax.swing.text.Segment;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Arrays;

/**
 * A text field subclass with formal placeholder support.
 */
@SuppressWarnings({ "serial", "unused" })
public class PlaceholderTextField extends JTextField {
    private boolean painting = false;
    private char maskChar = (char)0;
    private char[] maskChars = null;
    private String placeholder;

    public PlaceholderTextField() {
    }

    public PlaceholderTextField(
        final Document pDoc,
        final String pText,
        final int pColumns)
    {
        super(pDoc, pText, pColumns);
    }

    public PlaceholderTextField(final int pColumns) {
        super(pColumns);
    }

    public PlaceholderTextField(final String pText) {
        super(pText);
    }

    public PlaceholderTextField(final String pText, final int pColumns) {
        super(pText, pColumns);
    }

    @Override
    public void cut() {
        if (maskChar == 0)
            super.cut();
    }

    @Override
    public void copy() {
        if (maskChar == 0)
            super.copy();
    }

    @Override
    protected Document createDefaultModel() {
        // Our Content object provides masked characters when painting.
        return new PlainDocument(new MaskedContent());
    }

    /**
     * A custom Content that will provide maskChars for painting, but the real chars otherwise.
     */
    class MaskedContent extends GapContent {
        @Override
        public void getChars(int where, int len, Segment chars) throws BadLocationException {
            if (painting && maskChar != 0 && len > 0) {
                if (maskChars == null || len > maskChars.length) {
                    maskChars = new char[len + Math.max(len/2, 18)];
                    Arrays.fill(maskChars, maskChar);
                }
                chars.array = maskChars;
                chars.offset = 0;
                chars.count = len;
            } else
                super.getChars(where, len, chars);
        }
    }

    public char getMaskChar() {
        return maskChar;
    }
    public void setMaskChar(char maskChar) {
        if (maskChar != this.maskChar) {
            this.maskChars = null;
            this.maskChar = maskChar;
            repaint();
        }
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Paint the component text, mask characters, or placeholder text, depending...
     * If there is no component text, paint the placeholder.
     * If there is component text, but a maskChar has been set, paint one mask character for
     *   every character in the component text.
     * Otherwise, paint the component text.
     *
     * The placeholder text is simply painted into the Graphics when there is no other content
     * there.
     *
     * Swing paints the text in collaboration with the Component's 'Document' object, which
     * in turn gets the text to paint from its 'Content' object. We paint the mask characters
     * by providing our own Content object that is overridden to return a string of the
     * mask characters when painting, but the real text at other times.
     *
     * @param pG the Graphics into which to paint.
     */
    @Override
    protected void paintComponent(final Graphics pG) {
        painting = true;
        super.paintComponent(pG);
        painting = false;

        if (placeholder == null || placeholder.length() == 0 || getText().length() > 0) {
            return;
        }

        final Graphics2D g = (Graphics2D) pG;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getDisabledTextColor());
        g.drawString(placeholder,
            getInsets().left,
            pG.getFontMetrics().getMaxAscent() + getInsets().top);
    }
    
}
