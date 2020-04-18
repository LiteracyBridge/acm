package org.literacybridge.acm.gui.Assistant;

import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * GridBagConstraints with some modern setters.
 *
 * The "with*" methods return a new GridBagConstraints that is the original object modified
 * as indicated by the name, for example "withGridx()" returns a new GBC that is identical
 * except for the "gridx" member.
 *
 * The "set*" methods, on the other hand, modify the GBC, and then return the modified
 * object for chaining.
 */
public class GBC extends GridBagConstraints {
    public GBC() {
    }

    public GBC(GridBagConstraints gbc) {
        this(gbc.gridx, gbc.gridy,
            gbc.gridwidth, gbc.gridheight,
            gbc.weightx, gbc.weighty,
            gbc.anchor, gbc.fill,
            new Insets(gbc.insets.top, gbc.insets.left, gbc.insets.bottom, gbc.insets.right),
            gbc.ipadx, gbc.ipady);
    }

    public GBC(int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty,
        int anchor, int fill, Insets insets, int ipadx, int ipady)
    {
        super(gridx, gridy, gridwidth, gridheight, weightx, weighty,
            anchor, fill, insets, ipadx, ipady);
    }

    GBC withGridx(int newValue) {
        GBC clone = (GBC) this.clone();
        clone.gridx = newValue;
        return clone;
    }

    GBC withGridy(int newValue) {
        GBC clone = (GBC) this.clone();
        clone.gridy = newValue;
        return clone;
    }

    GBC withGridwidth(int newValue) {
        GBC clone = (GBC) this.clone();
        clone.gridwidth = newValue;
        return clone;
    }

    GBC withGridheight(int newValue) {
        GBC clone = (GBC) this.clone();
        clone.gridheight = newValue;
        return clone;
    }

    GBC withWeightx(double newValue) {
        GBC clone = (GBC) this.clone();
        clone.weightx = newValue;
        return clone;
    }

    public GBC withWeighty(double newValue) {
        GBC clone = (GBC) this.clone();
        clone.weighty = newValue;
        return clone;
    }

    public GBC withAnchor(int newValue) {
        GBC clone = (GBC) this.clone();
        clone.anchor = newValue;
        return clone;
    }

    public GBC withFill(int newValue) {
        GBC clone = (GBC) this.clone();
        clone.fill = newValue;
        return clone;
    }

    GBC withInsets(Insets newValue) {
        GBC clone = (GBC) this.clone();
        clone.insets = newValue;
        return clone;
    }

    GBC withIpadx(int newValue) {
        GBC clone = (GBC) this.clone();
        clone.ipadx = newValue;
        return clone;
    }

    GBC withIpady(int newValue) {
        GBC clone = (GBC) this.clone();
        clone.ipady = newValue;
        return clone;
    }

    GBC setGridx(int newValue) {
        this.gridx = newValue;
        return this;
    }

    GBC setGridy(int newValue) {
        this.gridy = newValue;
        return this;
    }

    GBC setGridwidth(int newValue) {
        this.gridwidth = newValue;
        return this;
    }

    GBC setGridheight(int newValue) {
        this.gridheight = newValue;
        return this;
    }

    GBC setWeightx(double newValue) {
        this.weightx = newValue;
        return this;
    }

    GBC setWeighty(double newValue) {
        this.weighty = newValue;
        return this;
    }

    public GBC setAnchor(int newValue) {
        this.anchor = newValue;
        return this;
    }

    public GBC setFill(int newValue) {
        this.fill = newValue;
        return this;
    }

    GBC setInsets(Insets newValue) {
        this.insets = newValue;
        return this;
    }

    GBC setIpadx(int newValue) {
        this.ipadx = newValue;
        return this;
    }

    GBC setIpady(int newValue) {
        this.ipady = newValue;
        return this;
    }

}
