package org.literacybridge.acm.gui.MainWindow.audioItems;


import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.literacybridge.acm.gui.util.AudioItemNode;

import java.awt.*;

public class CorruptedAudioHighlighter extends AbstractHighlighter {
    private int durationColumnIndex; // The column index containing the duration
    private Color highlightColor;    // The color for highlighting

    public CorruptedAudioHighlighter(int durationColumnIndex, Color highlightColor) {
        this.durationColumnIndex = durationColumnIndex;
        this.highlightColor = highlightColor;
    }

    @Override
    protected Component doHighlight(Component component, ComponentAdapter adapter) {
        AudioItemNode<?> audioItemNode = (AudioItemNode<?>) adapter.getValue();

        String duration = audioItemNode.getAudioItem().getDuration().trim();
//            ^(-\d+:-?\d+)|(00:00).*
        Boolean isCorrupted = duration.trim().indexOf('-') != -1 || duration.trim().equals("00:000");
        System.out.println(isCorrupted);

        if (isCorrupted) {
//            if (Pattern.compile("^(-\\d+:-?\\d+.*)|(00:00.*)").matcher(duration).matches()) {
            component.setForeground(Color.red);
        }
//        if (adapter.column == durationColumnIndex) {
//            Object value = adapter.getValue(durationColumnIndex);
//            System.out.println(value);
//            if (value != null) {
//                String duration = value.toString();
//                if (duration.equals("00:00") || duration.startsWith("-")) {
//                    component.setBackground(highlightColor);
//                }
////                else {
////                    component.setBackground(Color.WHITE); // Set the default background color
////                }
//            }
//        }
        return component;
    }
}