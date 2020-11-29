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
        roundedRect(g, x, y, width, height, this.radius, this.thickness, this.lineColor);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(insetThickness, insetThickness, insetThickness, insetThickness);
        return insets;
    }

    public static void roundedRect(Graphics g, int x, int y, int width, int height, int radius, int thickness, Color lineColor) {
        if ((thickness > 0) && (g instanceof Graphics2D)) {
            Graphics2D g2d = (Graphics2D) g;
            Color oldColor = g2d.getColor();

            g2d.setColor(lineColor);
            int size = thickness + thickness;
            float innerArc = (float) radius - thickness;
            Shape outer = new RoundRectangle2D.Float(x, y, width, height, (float) radius, (float) radius);
            Shape inner = new RoundRectangle2D.Float(x + thickness, y + thickness, width - size, height - size, innerArc, innerArc);
            Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
            path.append(outer, false);
            path.append(inner, false);
            g2d.fill(path);

            g2d.setColor(oldColor);
        }
    }

}
