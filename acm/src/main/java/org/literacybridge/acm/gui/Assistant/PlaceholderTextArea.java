package org.literacybridge.acm.gui.Assistant;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class PlaceholderTextArea extends JTextArea {
    private String placeholder;

    public PlaceholderTextArea() {
    }

    public PlaceholderTextArea(
        final Document pDoc,
        final String pText,
        final int rows,
        final int pColumns)
    {
        super(pDoc, pText, rows, pColumns);
    }

    public PlaceholderTextArea(final int rows, final int pColumns) {
        super(rows, pColumns);
    }

    public PlaceholderTextArea(final String pText) {
        super(pText);
    }

    public PlaceholderTextArea(final String pText, final int rows, final int pColumns) {
        super(pText, rows, pColumns);
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }
    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    protected void paintComponent(final Graphics pG) {
        super.paintComponent(pG);

        if (placeholder == null || placeholder.length() == 0 || getText().length() > 0) {
            return;
        }

        final Graphics2D g = (Graphics2D) pG;
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getDisabledTextColor());
        g.drawString(placeholder, getInsets().left, pG.getFontMetrics()
            .getMaxAscent() + getInsets().top);
    }
}
