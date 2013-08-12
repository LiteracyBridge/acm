package org.literacybridge.acm.gui.dialogs.audioItemPropertiesDialog;

import java.util.List;
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;

import org.literacybridge.acm.config.Configuration;
import org.literacybridge.acm.gui.util.language.LanguageUtil;

public class LanguageComboBoxModel extends DefaultComboBoxModel {
	private final List<Locale> supportedLanguages;
	private Object selectedItem;
	
	public LanguageComboBoxModel() {
		supportedLanguages = Configuration.getAudioLanguages();
	}

	@Override
	public Object getElementAt(int index) {
		return LanguageUtil.getLocalizedLanguageName(supportedLanguages.get(index));
	}

	@Override
	public int getSize() {
		return supportedLanguages.size();
	}
	
	@Override
	public Object getSelectedItem() {
		return selectedItem;
	}

	@Override
	public void setSelectedItem(Object anObject) {
		selectedItem = anObject;
	}

	public Locale getLocalForIndex(int index) {
		return supportedLanguages.get(index);
	}
}
