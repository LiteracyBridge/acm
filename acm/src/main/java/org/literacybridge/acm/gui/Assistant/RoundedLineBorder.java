package org.literacybridge.acm.gui.Assistant;

import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * A subclass of LineBorder that can draw rounded borders.
 */
public class RoundedLineBorder extends LineBorder {
    private int radius;
    private int insetThickness;

    public RoundedLineBorder(Color color, int thickness, int radius, int insetThickness) {
        super(color, thickness, true);
        this.radius = radius;
        this.insetThickness = insetThickness;
    }

    public RoundedLineBorder(Color color, int thickness, int radius) {
        super(color, thickness, true);
        this.radius = radius;
        this.insetThickness = thickness;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if ((this.thickness > 0) && (g instanceof Graphics2D)) {
            Graphics2D g2d = (Graphics2D) g;
            Color oldColor = g2d.getColor();

            g2d.setColor(this.lineColor);
            int offs = this.thickness;
            int size = offs + offs;
            float outerArc = radius;
            float innerArc = outerArc - offs;
            Shape outer = new RoundRectangle2D.Float(x, y, width, height, outerArc, outerArc);
            Shape inner = new RoundRectangle2D.Float(x + offs, y + offs, width - size, height - size, innerArc, innerArc);
            Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
            path.append(outer, false);
            path.append(inner, false);
            g2d.fill(path);

            g2d.setColor(oldColor);
        }
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(insetThickness, insetThickness, insetThickness, insetThickness);
        return insets;
    }

}
