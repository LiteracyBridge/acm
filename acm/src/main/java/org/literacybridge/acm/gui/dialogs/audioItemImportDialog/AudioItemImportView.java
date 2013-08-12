package org.literacybridge.acm.gui.dialogs.audioItemImportDialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;
import org.literacybridge.acm.gui.util.language.LanguageUtil;

public class AudioItemImportView extends Container implements Observer {

	private JXTable table;
	private AudioItemImportModel model;
	
	private static final long serialVersionUID = 4107384434412515579L;

	public AudioItemImportView() {
		setLayout(new BorderLayout());
		createTable();
	}

	public List<File> getAudioItemsForImport() {
		return model.getEnabledAudioItems();
	}
	
	public void setData(List<File> filesToImport) throws IOException {
		model = new AudioItemImportModel(filesToImport);
		table.setModel(model);
		initColumnSize();
	}
	
	public void setCheckSetForAllItems(boolean enable) {
		model.setStateForAllItems(enable);
	}
	
	private void createTable() {
		AudioItemImportModel.initializeTableColumns(getColumnTitles(LanguageUtil.getUILanguage()));
		
		table = new JXTable();
		table.setShowGrid(false, false); 
		
		// use fixed color; there seems to be a bug in some plaf implementations that cause strange rendering
		table.addHighlighter(HighlighterFactory.createAlternateStriping(
				Color.white, new Color(237, 243, 254)));
		
		
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(800, 500));
	
		add(BorderLayout.CENTER, scrollPane);	
	}
	
	
	private String[] getColumnTitles(Locale locale) {
		// order MUST fit match to table titles
		String[] columnTitleArray = new String[AudioItemImportModel.NUM_COLUMNS]; // SET
		columnTitleArray[AudioItemImportModel.INFO_ICON] = "";
		columnTitleArray[AudioItemImportModel.TITLE] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_TITLE , locale);
		columnTitleArray[AudioItemImportModel.CATEGORIES] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_CATEGORIES , locale);
		columnTitleArray[AudioItemImportModel.LANGUAGES] = LabelProvider.getLabel(LabelProvider.AUDIO_ITEM_TABLE_COLUMN_LANGUAGE , locale);
			
		return columnTitleArray;
	}
	
	private void initColumnSize() {
		table.setAutoCreateColumnsFromModel( false );
	
		table.getTableHeader().getColumnModel().getColumn(AudioItemImportModel.INFO_ICON).setMaxWidth(25);
		table.getTableHeader().getColumnModel().getColumn(AudioItemImportModel.TITLE).setPreferredWidth(250);
		table.getTableHeader().getColumnModel().getColumn(AudioItemImportModel.CATEGORIES).setPreferredWidth(150);
		table.getTableHeader().getColumnModel().getColumn(AudioItemImportModel.LANGUAGES).setPreferredWidth(25);
	}

	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
	}
	
	
}
