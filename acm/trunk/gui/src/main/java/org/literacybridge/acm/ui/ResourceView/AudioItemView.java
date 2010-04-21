package ui.ResourceView;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.literacybridge.acm.api.IDataRequestResult;

import ui.Application;

public class AudioItemView extends Container implements Observer {

	private static final long serialVersionUID = -2886958461177831842L;

	// model
	private IDataRequestResult currResult = null;
	
	// table
	private JTable audioItemTable = null;

		
	public AudioItemView() {
		setLayout(new BorderLayout());
		createTable();	
		Application.getMessageService().addObserver(this);
	}

	private void createTable() {
		audioItemTable = new JTable();
		updateTable(); // init empty
 		
	    JScrollPane scrollPane = new JScrollPane(audioItemTable);
	    add(BorderLayout.CENTER, scrollPane);
	}
	
	private void updateTable() {
		audioItemTable.setModel(new AudioItemTableModel(currResult));
	}
	
	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof IDataRequestResult) {
			currResult = (IDataRequestResult) arg;
			updateTable();
		}
	}	
 }
