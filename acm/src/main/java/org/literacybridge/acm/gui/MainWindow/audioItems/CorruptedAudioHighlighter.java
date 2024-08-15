package org.literacybridge.acm.gui.MainWindow.audioItems;


import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.literacybridge.acm.gui.util.AudioItemNode;

import java.awt.*;

public class CorruptedAudioHighlighter extends AbstractHighlighter {
    @Override
    protected Component doHighlight(Component component, ComponentAdapter adapter) {
        AudioItemNode<?> audioItemNode = (AudioItemNode<?>) adapter.getValue();

        if (audioItemNode.getAudioItem().isCorrupted()) {
            component.setForeground(Color.red);
        }

        return component;
    }
}