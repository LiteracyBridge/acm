package org.literacybridge.acm.gui.dialogs;

import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to simplify creating pop-ups, with some shared utility.
 */
public class PopUp {
    @SuppressWarnings("unused")
    public static class Builder {
        private Component parent = null;
        private String title;
        private Object contents;
        private int optionType = JOptionPane.DEFAULT_OPTION;
        private int messageType = JOptionPane.PLAIN_MESSAGE;
        private Icon icon = null;
        private Object[] options = null;
        private Object initialValue = null;

        private boolean optOut = false;

        /**
         * The parent is used to position the dialog. Otherwise system dependent.
         * @param parent of the dialog.
         * @return this, for chaining.
         */
        public Builder withParent(Component parent) {
            this.parent = parent;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        /**
         * The actual contents to display. Can be a String, or a Component.
         * @param contents to display.
         * @return this, for chaining.
         */
        public Builder withContents(Object contents) {
            this.contents = contents;
            return this;
        }

        /**
         * The JOptionPane "option type".
         * @param optionType, like DEFAULT_OPTION, YES_NO_OPTION, ...
         * @return this, for chaining.
         */
        public Builder withOptionType(int optionType) {
            this.optionType = optionType;
            return this;
        }

        /**
         * The JOptionPane "message type".
         * @param messageType, like PLAIN_MESSAGE, ERROR_MESSAGE, ...
         * @return this, for chaining.
         */
        public Builder withMessageType(int messageType) {
            this.messageType = messageType;
            return this;
        }

        /**
         * The icon, otherwise set based on message type.
         * @param icon to be used.
         * @return this, for chaining.
         */
        public Builder withIcon(Icon icon) {
            this.icon = icon;
            return this;
        }

        /**
         * "An array of objects indicating the possible choices the user can make..."
         * See JOptionPane.showOptionDialog.
         * @param options to be shown. The "toString()" value is shown for each object.
         * @return this, for chaining.
         */
        public Builder withOptions(Object[] options) {
            this.options = options;
            return this;
        }

        /**
         * The default value. Only valid if options are also specified.
         * @param initialValue The object representing the initial value, the default.
         * @return this, for chaining.
         */
        public Builder withInitialValue(Object initialValue) {
            this.initialValue = initialValue;
            return this;
        }

        /**
         * This option adds a "[ ] Don't show this again." to the dialog. If it is checked at
         * the time that the dialog is dismissed, whatever the return value is will be returned
         * immediately from any futer invocations of this same dialog.
         *
         * Another dialog is considered to be "the same" if the title is the same.
         * @return this, for chaining.
         */
        public Builder withOptOut() {
            this.optOut = true;
            return this;
        }

        /**
         * Creates and displays a dialog with the options that have been set.
         * @return the return from the dialog.
         */
        public int go() {
            if (title == null || contents == null)
                throw new IllegalStateException("Must provide title and contents");
            return new PopUp(this).go();
        }
    }

    private static final Map<String, Integer> optedOut = new HashMap<>();

    private final Builder builder;

    private PopUp(Builder builder) {
        this.builder = builder;
    }

    private int go() {
        if (optedOut.containsKey(builder.title)) return optedOut.get(builder.title);
        boolean[] optedOut = {false};
        Object message;
        JPanel panel = new JPanel();
        panel.setLayout(new VerticalLayout());

        if (builder.optOut) {
            if (builder.contents instanceof String) {
                String msg = "<html>" + ((String) builder.contents).replace("\n", "<br>");
                panel.add(new JLabel(msg));
            } else {
                panel.add((Component) builder.contents);
            }
            panel.add(new JLabel(" "));
            JCheckBox optOut = new JCheckBox("Don't show this again.");
            optOut.addActionListener(e -> {
                if (e.getSource() instanceof JCheckBox) optedOut[0] = ((JCheckBox)e.getSource()).isSelected();
            });
            panel.add(optOut);
            message = panel;
        } else {
            message = builder.contents;
        }
        int result = JOptionPane.showOptionDialog(builder.parent,
            message,
            builder.title,
            builder.optionType,
            builder.messageType,
            builder.icon,
            builder.options,
            builder.initialValue);
        if (optedOut[0]) {
            PopUp.optedOut.put(builder.title, result);
        }
        return result;
    }

}
