package org.literacybridge.acm.gui.assistants.common;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.config.ACMConfiguration;
import org.literacybridge.acm.config.DBConfiguration;
import org.literacybridge.acm.gui.Assistant.Assistant;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.UIConstants;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

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

    protected static void fillDeploymentChooser(JComboBox<String> deploymentChooser,
        ProgramSpec programSpec,
        int deploymentNo) {
        deploymentChooser.removeAllItems();
        deploymentChooser.insertItemAt("Choose...", 0);
        List<String> deployments = programSpec.getDeployments()
            .stream()
            .map(d -> Integer.toString(d.deploymentnumber))
            .collect(Collectors.toList());
        deployments
            .forEach(deploymentChooser::addItem);

        // If only one deployment, or previously selected, auto-select.
        if (deploymentChooser.getItemCount() == 2) {
            deploymentChooser.setSelectedIndex(1); // only item after "choose..."
        } else if (deploymentNo >= 0) {
            deploymentChooser.setSelectedItem(Integer.toString(deploymentNo));
        } else {
            // Select "Choose..."
            deploymentChooser.setSelectedIndex(0);
        }
    }

    protected static void fillLanguageChooser(JComboBox<String> languageChooser,
        int deploymentNo,
        ProgramSpec programSpec,
        String defaultLanguageCode) {
        // Languages from the program spec.
        Set<String> languageCodes = programSpec.getLanguagesForDeployment(deploymentNo);
        // Languages from the program's "properties.config"
        Set<String> configLanguageCodes = ACMConfiguration.getInstance().getCurrentDB().getAudioLanguages()
                .stream()
                .map(Locale::getISO3Language)
                .collect(Collectors.toSet());
        languageCodes.addAll(configLanguageCodes);
        fillLanguageChooser(languageChooser, languageCodes, defaultLanguageCode);
    }

    protected static void fillLanguageChooser(JComboBox<String> languageChooser,
        Collection<String> languageCodes,
        String defaultLanguageCode) {
        languageChooser.removeAllItems();
        languageChooser.insertItemAt("Choose...", 0);
        languageCodes.forEach(languageChooser::addItem);

        if (languageChooser.getItemCount() == 2) {
            languageChooser.setSelectedIndex(1); // only item after "choose..."
        } else if (StringUtils.isNotEmpty(defaultLanguageCode)) {
            languageChooser.setSelectedItem(defaultLanguageCode);
        } else {
            // Select "Choose..."
            languageChooser.setSelectedIndex(0);
        }
    }


    public static String getLanguageAndName(String languagecode) {
        DBConfiguration dbConfig = ACMConfiguration.getInstance().getCurrentDB();
        String label = dbConfig.getLanguageLabel(languagecode);
        return label==null ? languagecode : (label + " (" + languagecode + ')');
    }

    public static class LanguageListCellRenderer extends DefaultListCellRenderer {
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

    @SuppressWarnings("unused")
    public static class LanguageChooser extends JComboBox<String> {
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
