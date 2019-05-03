package org.literacybridge.acm.gui.assistants.common;

import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.store.MetadataStore;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.util.Vector;

public abstract class AcmAssistantPage<Context> extends AssistantPage<Context> {
    public static Color bgColor = Color.white; // table.getBackground();
    public static Color bgSelectionColor = new JTable().getSelectionBackground();
    public static Color bgAlternateColor = new Color(235, 245, 252);

    // Speaker with sound coming out of it.
    public static ImageIcon soundImage = new ImageIcon(UIConstants.getResource("sound-1.png"));
    // Speaker with no sound coming out.
    public static ImageIcon noSoundImage = new ImageIcon(UIConstants.getResource("sound-3.png"));

    protected Context context;
    protected MetadataStore store = ACMConfiguration.getInstance().getCurrentDB().getMetadataStore();

    protected AcmAssistantPage(Assistant.PageHelper<Context> listener) {
        super(listener);
        context = getContext();
    }

    private DBConfiguration dbConfig = ACMConfiguration.getInstance().getCurrentDB();
    protected String getLanguageAndName(String languagecode) {
        String label = dbConfig.getLanguageLabel(languagecode);
        return label==null ? languagecode : (label + " (" + languagecode + ')');
    }

    public class LanguageListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            if (value instanceof String) {
                value = getLanguageAndName((String)value);
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

    public class LanguageChooser extends JComboBox<String> {
        public LanguageChooser(ComboBoxModel<String> aModel) {
            super(aModel);
            setRenderer(new LanguageListCellRenderer());
        }

        public LanguageChooser(String[] items) {
            super(items);
            setRenderer(new LanguageListCellRenderer());
        }

        public LanguageChooser(Vector<String> items) {
            super(items);
            setRenderer(new LanguageListCellRenderer());
        }

        public LanguageChooser() {
            setRenderer(new LanguageListCellRenderer());
        }
    }
}
