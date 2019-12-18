package org.literacybridge.acm.cloud;

import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.Map;

/**
 * Label that behaves a bit like a link. When the mouse hovers over it, it becomes underlined.
 * Users of the class can call addActionListener() to get action events.
 */
class ActionLabel extends JLabel {
    private static Color linkColor = new Color(0x337ab7);
    private static Border focusedBorder = new RoundedLineBorder(linkColor, 1, 4, 2);
    private static Border unfocusedBorder = new EmptyBorder(2, 2, 2, 2);
    private static Font hoveredFont = null;
    private static Font normalFont = null;

    private String actionCommand;

    ActionLabel(String text) {
        super(text);

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(linkColor);
        setFocusable(true);

        // Normal and hover fonts for label.
        if (normalFont == null) {
            normalFont = getFont();
            Map attributes = normalFont.getAttributes();
            //noinspection unchecked
            attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            //noinspection unchecked
            hoveredFont = normalFont.deriveFont(attributes);
        }

        setFont(normalFont);
        setBorder(unfocusedBorder);

        addMouseListener(labelMouseListener);
        addFocusListener(labelFocusListener);
        addKeyListener(labelKeyListener);

    }

    @SuppressWarnings("FieldCanBeLocal")
    private KeyListener labelKeyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() == ' ') fireActionPerformed();
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private MouseListener labelMouseListener = new MouseAdapter() {
        /**
         * Mouse clicked on the label. Fire the action event.
         * @param e the mouse event.
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            super.mouseClicked(e);
            fireActionPerformed();
        }

        /**
         * Mouse hovers, underline text.
         * @param e the mouse event.
         */
        @Override
        public void mouseEntered(MouseEvent e) {
            super.mouseEntered(e);
            JLabel label = (JLabel) e.getComponent();
            label.setFont(hoveredFont);
        }

        /**
         * Mouse leaves, remove underline.
         * @param e the mouse event.
         */
        @Override
        public void mouseExited(MouseEvent e) {
            super.mouseExited(e);
            JLabel label = (JLabel) e.getComponent();
            label.setFont(normalFont);
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final FocusListener labelFocusListener = new FocusListener() {
        /**
         * Gained focus, add the focus border.
         * @param e the focus event.
         */
        @Override
        public void focusGained(FocusEvent e) {
            Component comp = e.getComponent();
            if (comp != null) ((JLabel) comp).setBorder(focusedBorder);
        }

        /**
         * Lost focus, remove the focus border.
         * @param e the focus event.
         */
        @Override
        public void focusLost(FocusEvent e) {
            Component comp = e.getComponent();
            if (comp != null) ((JLabel) comp).setBorder(unfocusedBorder);
        }
    };

    private String getActionCommand() {
        return actionCommand;
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    @SuppressWarnings("DuplicatedCode")
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

}
