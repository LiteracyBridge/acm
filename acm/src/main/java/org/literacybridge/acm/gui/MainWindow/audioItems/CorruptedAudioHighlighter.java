package org.literacybridge.acm.gui.MainWindow.audioItems;


import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.literacybridge.acm.gui.util.AudioItemNode;

import java.awt.*;

public class CorruptedAudioHighlighter extends AbstractHighlighter {
    @Override
    protected Component doHighlight(Component component, ComponentAdapter adapter) {
        AudioItemNode<?> audioItemNode = (AudioItemNode<?>) adapter.getValue();
        String duration = audioItemNode.getAudioItem().getDuration().trim();

        // Audio is considered as corrupted if it has a negative or 00:00 duration
        if (duration.trim().indexOf('-') != -1 || duration.trim().equals("00:000")) {
            component.setForeground(Color.red);
        }

        return component;
    }
}