package org.literacybridge.acm.ui.dialogs.audioItemPropertiesDialog;

import java.util.Locale;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;

import org.literacybridge.acm.util.language.LanguageUtil;

public class LanguageComboBoxModel extends DefaultComboBoxModel {
	private Vector<Locale> supportedLanguages;
	private Object selectedItem;
	
	public LanguageComboBoxModel() {
		supportedLanguages = new Vector<Locale>();
		supportedLanguages.add(Locale.ENGLISH);
		supportedLanguages.add(Locale.GERMAN);
		supportedLanguages.add(Locale.FRENCH);
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
