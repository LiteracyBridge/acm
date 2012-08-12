package org.literacybridge.acm.tbloader;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

import org.literacybridge.acm.utils.OSChecker;

//import org.jdesktop.swingx.JXDatePicker;

public class TBLoader extends JFrame implements ActionListener {
	private static final String VERSION = "v1.02";
	private static final String END_OF_INPUT = "\\Z";
	private static final String COLLECTION_SUBDIR = "\\collected-data";
	private static String TEMP_COLLECTION_DIR = "";
	private static final String SW_SUBDIR = ".\\software\\";
	private static final String CREATE_BACKUP = SW_SUBDIR + "backup1.bat ";
	private static final String SCRIPT_SUBDIR = SW_SUBDIR + "scripts\\";
	private static final String NO_SERIAL_NUMBER = "(not found)";
	private static final String NO_DRIVE = "(nothing connected)";
	private static final String TRIGGER_FILE_CHECK = "checkdir";
	private JComboBox communityList;
	private JComboBox driveList;
	private JTextField id;
	private static JTextField status;
	private static String homepath;
	private String sourceDir;
	private static JButton update;
	private static JButton reformat;
	private static JButton backup;
	private static JButton xfer;
	private static JButton setCommunity;
	private static String copyTo;
	private JCheckBox fetchIDFromServer;
	private JCheckBox handIcons;
	
	class WindowEventHandler extends WindowAdapter {
		public void windowClosing(WindowEvent evt) {
			checkDirUpdate();
			Logger.LogString("closing app");
			Logger.close();
		    System.exit(0);
		}
	}
	
	private String communityNames[] = new String[] {
			"Non-specific",
			"Behee-Wa Municipal",
			"Duori Degri-Jirapa",
			"Gozu-Jirapa",
			"Gyangvuuri-Jirapa",
			"Jeffiri-Jirapa",
			"Jonga-Wa Municipal",
			"Saabaalong-Jirapa",
			"Tugo Yagra-Jirapa",
			"Ving-Ving-Jirapa",
			"Zengpeni-Jirapa"};
	
	public TBLoader(String path, String label) throws Exception {
		sourceDir = path;
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addWindowListener(new WindowEventHandler());
		setTitle("TB-Loader " + VERSION + "   " + label);
		
		JPanel panel = new JPanel();
		
		JLabel communityLabel = new JLabel("Community:");
		JLabel deviceLabel = new JLabel("Talking Book Device:");
		JLabel idLabel = new JLabel("Serial number:");
		status = new JTextField("STATUS: Ready");
		status.setEditable(false);
		id = new JTextField();
		id.setEditable(false);
/*		final JXDatePicker datePicker = new JXDatePicker();
		datePicker.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				id.setText(datePicker.getDate().toString());
			}
		});
*/		 	
		communityList = new JComboBox();
		driveList = new JComboBox();
		driveList.addActionListener(this);
		fetchIDFromServer = new JCheckBox("Get new serial number");
		fetchIDFromServer.setSelected(false);
		handIcons = new JCheckBox("Use hand icon msgs");
		handIcons.setSelected(false);
		update = new JButton("Update");
		update.addActionListener(this);
		reformat = new JButton("Reformat");
		reformat.addActionListener(this);
		backup = new JButton("Backup");
		backup.addActionListener(this);
		xfer = new JButton("Upload Files");
		xfer.addActionListener(this);
		setCommunity = new JButton("Set Community");
		setCommunity.addActionListener(this);
		
		
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createSequentialGroup()
	        		.addGroup(layout.createParallelGroup(LEADING)
//	        				.addComponent(datePicker)
	        				.addComponent(deviceLabel)
	        				.addComponent(communityLabel)
	        				.addComponent(idLabel)
             				.addComponent(xfer)
	        				.addComponent(setCommunity))
	                .addGroup(layout.createParallelGroup(LEADING)
	                		.addComponent(driveList)
	                		.addComponent(communityList)
	                		.addComponent(id)
	                		.addGroup(layout.createSequentialGroup()
	                				.addComponent(fetchIDFromServer)
	                				.addComponent(handIcons))
	    	                .addGroup(layout.createSequentialGroup()
	                				.addComponent(update)
	                				.addComponent(backup)
	                				.addComponent(reformat))
	                		.addComponent(status))
            );
        
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(BASELINE)
                        .addComponent(deviceLabel)
                        .addComponent(driveList))
                .addGroup(layout.createParallelGroup(BASELINE)
//                		.addComponent(datePicker)
                		.addComponent(communityLabel)
                        .addComponent(communityList))
                .addGroup(layout.createParallelGroup(BASELINE)
		            .addComponent(idLabel)
		            .addComponent(id))
		        .addGroup(layout.createParallelGroup(BASELINE)
		        	.addComponent(xfer)
		        	.addComponent(fetchIDFromServer)
    				.addComponent(handIcons))
    		    .addGroup(layout.createParallelGroup(BASELINE)
    		    	.addComponent(setCommunity)
    		    	.addComponent(update)
    	    		.addComponent(backup)
    	    		.addComponent(reformat))
    	    	.addComponent(status)
            );
        
        setSize(500,215);
        add(panel, BorderLayout.CENTER);
//      add(status, BorderLayout.SOUTH);
//      add(xfer, BorderLayout.EAST);
		setLocationRelativeTo(null);
		
		setCopyToPath();
		Logger.init();
		resetUI(true);
		Logger.LogString("about to set visibility");
		setVisible(true);
		Logger.LogString("set visibility");
		Logger.LogString("starting drive monitoring");
		deviceMonitorThread.setDaemon(true);
		deviceMonitorThread.start();
		startUpDone = true;
	}
	
	public static boolean startUpDone = false;
	public static boolean monitoringDrive = false;
	
	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.out.println("Usage: java -jar device_registration.jar [window title]");
		} else {
			File f = new File(args[0]);
			String label = "";
			if (args.length >= 1) {
				label = args[0];
			}
			new TBLoader(f.getAbsolutePath(), label);
		} 
	}

	private static String getDateTime() {
		Calendar cal = Calendar.getInstance();
		int month = cal.get(Calendar.MONTH) + 1	;
		int date = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		String dateTime = String.valueOf(month) + "m" + String.valueOf(date) + "d" + 
				String.valueOf(hour) + "h" + String.valueOf(min) + "m" + String.valueOf(sec) + "s";
		return dateTime;
	}
	
	private static String getLogFileName() {
		String filename;
		
		filename = copyTo + "\\log-" + getDateTime()+".txt";
		return filename;
	}
	private void setCopyToPath() {
		int i = 0;
		final int MAX_PATHS = 5;
		String paths[]= new String[MAX_PATHS];

		try {
			Process proc = Runtime.getRuntime().exec("cmd /C echo %HOMEDRIVE%%HOMEPATH%");
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			homepath = new String(reader.readLine());
			reader.close();
			TEMP_COLLECTION_DIR = new String(homepath + "\\TB-Loader");
			Logger.LogString("Temp Collection Dir: " + TEMP_COLLECTION_DIR);
			File f = new File(TEMP_COLLECTION_DIR);
			f.mkdirs();
			// now that local path is established, the 'collected-data' subdir is what should 
			// only be present when there is local storage (the xfer button is only enabled if it's there
			TEMP_COLLECTION_DIR += COLLECTION_SUBDIR; 
			reader = new BufferedReader(new FileReader(new File(SW_SUBDIR + "paths.txt")));
			copyTo = "";
			while (reader.ready() && i < MAX_PATHS) {
				paths[i] = reader.readLine().replaceAll("%HOMEPATH%", Matcher.quoteReplacement(homepath));
				Logger.LogString(paths[i]);
				if ((new File(paths[i])).exists()) {
					copyTo = paths[i];
					Logger.LogString("GOOD PATH = "+copyTo);
					break;						
				}
			}
			reader.close();
		} catch (Exception e) {
			Logger.LogString(e.toString());
			Logger.init();
			e.printStackTrace();
		}
		if (copyTo =="") {
			Logger.LogString("***No Dropbox path not found. Storing all files locally.");
			copyTo = TEMP_COLLECTION_DIR;
		}
		else {
			copyTo = copyTo += COLLECTION_SUBDIR;
			new File(copyTo).mkdir();  // creates COLLECTION_SUBDIR if good path is found
		}
		Logger.LogString("copyTo:"+copyTo);
	}
	
	int idCounter = 0;
	
	private String fetchNextDeviceID() throws Exception {
		try {
			if (!debug) {
				URL url = new URL("http://library.maybefriday.net/register_device.json");
				InputStream in = url.openStream();
				Scanner scanner = new Scanner(in);
			    scanner.useDelimiter(END_OF_INPUT);
			    String json = scanner.next();
			    json = json.substring(14);
			    return json.substring(0, json.length() - 2);
			} else {
				return "TB-" + idCounter++;
			}
		} catch (Exception ex){
			Logger.LogString(ex.toString());
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "An error occured while fetching a new device ID from the Literacy Bridge server. Make sure you have a working internet connection.",
	                "Error", JOptionPane.ERROR_MESSAGE);
			throw ex;
		}
	}
	
	private File prevSelected = null;
	private int prevSelectedCommunity = -1;
	
	private String getCommunityFromCurrentDrive() {
		String communityName = communityNames[0];
		DriveInfo di = (DriveInfo) driveList.getSelectedItem();
		if (di.drive == null) {
			return communityName;
		}
		File rootPath = new File(di.drive.getAbsolutePath());
		File systemPath = new File(di.drive.getAbsolutePath(), "system");
		try {
			File[] files;
			// get Location file info
			// check root first, in case device was just assigned a new community (e.g. from this app)
			files = rootPath.listFiles(new FilenameFilter() {
				@Override public boolean accept(File dir, String name) {
					String lowercase = name.toLowerCase();
					return lowercase.endsWith(".loc");
				}
			});
			if (files == null) {
				Logger.LogString("This does not look like a TB: " + rootPath);
				
			}
			else if (files.length == 1) {
				String locFileName = files[0].getName();
				communityName = locFileName.substring(0, locFileName.length() - 4);
			}
			else if (files.length == 0 && systemPath.exists()) {
				// get Location file info
				files = systemPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".loc");
					}
				});				
				if (files.length == 1) {
					String locFileName = files[0].getName();
					communityName = locFileName.substring(0, locFileName.length() - 4);
				}
			} 
		} catch (Exception ignore) {
			Logger.LogString(ignore.toString());
			Logger.init();
			ignore.printStackTrace();	
			// ignore and keep going with empty string
		}
		Logger.LogString("TB's current community name is "+communityName);
		return communityName;
	}
	
	private synchronized void fillCommunityList() {
		
		communityList.removeAllItems();
		for (int i=0; i < communityNames.length; i++) {
			communityList.addItem(communityNames[i]);
		}
		if (prevSelectedCommunity != -1)
			communityList.setSelectedIndex(prevSelectedCommunity);
		setCommunityList();
	}

	private synchronized void setCommunityList() {
		String driveCommunity;
		int communityMatchIndex = -1;

		driveCommunity = getCommunityFromCurrentDrive();
		
		for (int i=0; i < communityNames.length; i++) {
			if (communityNames[i].equalsIgnoreCase(driveCommunity))
				communityMatchIndex = i;
		}
		if (communityMatchIndex == -1 && prevSelectedCommunity != -1)
			communityMatchIndex = prevSelectedCommunity;
		communityList.setSelectedIndex(communityMatchIndex);
	}
	
	private synchronized void setSNFromCurrentDrive() {
		String sn="";
		DriveInfo di;
		if (driveList == null)
			return;
		di= (DriveInfo) driveList.getSelectedItem();
		if (di == null || di.drive == null)
			return;
		File systemPath = new File(di.drive.getAbsolutePath(), "system");
		try {
			File[] files;
			if (systemPath.exists()) {
				// get Serial Number file info
				files = systemPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".srn") && !lowercase.startsWith("-erase");
					}
				});
				
				if (files.length > 0) {
					String tsnFileName = files[0].getName();
					sn = tsnFileName.substring(0, tsnFileName.length() - 4);
	//				if (sn.startsWith("srn."))
	//					sn = sn.substring(4);
				}
			}
		} catch (Exception ignore) {
			Logger.LogString("exception - ignore and keep going with empty string");
		}
		if (sn.equals("")) {
			sn = NO_SERIAL_NUMBER;
		}
		id.setText(sn);
		di.serialNumber = sn;
	}	

	private synchronized void fillList(File[] roots) {
		driveList.removeAllItems();
		int index = -1;
		int i = 0;
		for (File root : roots) {
			if (root.getAbsoluteFile().toString().compareTo("D:") >= 0 && root.listFiles() != null) {
				String label = FileSystemView.getFileSystemView().getSystemDisplayName(root);
				if (label.trim().equals("CD Drive"))
					continue;
				driveList.addItem(new DriveInfo(root, label));
				if (prevSelected != null && root.getAbsolutePath().equals(prevSelected.getAbsolutePath())) {
					index = i;
				} else if (label.startsWith("TB") || label.startsWith("ECHO"))
					index = i;
				i++;
			}
		}
		if (driveList.getItemCount() == 0) {
			Logger.LogString("No drives");
			driveList.addItem(new DriveInfo(null,NO_DRIVE));
			index = 0;
		}
			
		if (index == -1) {
			index = i-1;
		}
		if (index != -1) {
			driveList.setSelectedIndex(index);
		}
	}

	private synchronized File[] getRoots() {
		File[] roots = null;
		if (OSChecker.WINDOWS) {
			roots = File.listRoots();
		} else if (OSChecker.MAC_OS) {
			roots = new File("/Volumes").listFiles();
		}
		return roots;
	}
	
	private Thread deviceMonitorThread = new Thread() {
		@Override public void run() {
			Set<String> oldList = new HashSet<String>();
			
			while (true) {
				File[] roots = getRoots();
				
				boolean refresh = oldList.size() != roots.length;
				if (!refresh) {
					for (File root : roots) {
						if (!oldList.contains(root.getAbsolutePath())) {
							refresh = true;
							break;
						}
					}
				}
				
				if (refresh) {
					monitoringDrive = true;
					Logger.LogString("deviceMonitor sees new drive");
					fillList(roots);
//					Logger.LogString("monitor filled drive list");
					fillCommunityList();
//					Logger.LogString("monitor filled community list");
					setSNFromCurrentDrive();
//					Logger.LogString("monitor got SN");
					refreshUI();
//					Logger.LogString("monitor refreshed UI");
					oldList.clear();
					for (File root : roots) {
						oldList.add(root.getAbsolutePath());
					}
					monitoringDrive = false;
//					Logger.LogString("monitor updates complete");
				}
				
				try {
					sleep(2000);
				} catch (InterruptedException e) {
					Logger.LogString(e.toString());
					Logger.init();
					throw new RuntimeException(e);
				}

			}
		}
	};

	private void setCommunity(String path, String community, String dateTime) {
		String locFilename = path + community + ".loc";
		String rtcFilename = path + dateTime + ".rtc";
		
		try {
			execute("cmd /C del \"" + path + "*.loc\" \"" + path + "system\\*.loc\" \"" + path + "*.rtc\"");

			File locFile = new File(locFilename);
			File rtcFile = new File(rtcFilename);
			File inspectFile = new File(path + "inspect");
			locFile.createNewFile();
			rtcFile.createNewFile();
			inspectFile.createNewFile();
		} catch (Exception e) {
			Logger.LogString(e.toString());
			Logger.init();
			e.printStackTrace();
		}
		Logger.LogString("Commmunity set to " + community + ".");
		JOptionPane.showMessageDialog(this, "Commmunity set to " + community + ".",
                "Success", JOptionPane.DEFAULT_OPTION);
	}

	private void xferFiles() {
		int response = JOptionPane.showConfirmDialog(this, "Do you currently have a good\n" +
				"Internet connection to upload large files" +
				"\n   in" + TEMP_COLLECTION_DIR + 
				"\n   to " + copyTo + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
		if (response != JOptionPane.YES_OPTION)
			return;
		xfer.setEnabled(false);
		update.setEnabled(false);
		try {
			execute("cmd /C " + SW_SUBDIR + "robocopy \"" + TEMP_COLLECTION_DIR + "\" \"" + copyTo +"\" /MOVE /E");
		} catch (Exception e) {
			Logger.LogString(e.toString());
			Logger.init();
			e.printStackTrace();
		}
		Logger.LogString("Files transferred");
		JOptionPane.showMessageDialog(this, "Files transferred",
                "Success", JOptionPane.DEFAULT_OPTION);
	}

/* @Override
   public void itemStateChanged(ItemEvent evt) {
		Logger.LogString("Drive changed: " + evt.getItem());
		// JComboBox cb = (JComboBox)evt.getSource();
		setCommunityList();
	}
*/
	@Override
	public void actionPerformed(ActionEvent e) {
		DriveInfo di;
		Object o = e.getSource();
		JButton b;
		if (monitoringDrive || !startUpDone)
			return;
		if (o instanceof JButton)
			b = (JButton)e.getSource();
		else if (o instanceof JComboBox) {
			di = (DriveInfo)((JComboBox)e.getSource()).getSelectedItem();
			Logger.LogString("Drive changed: " + di.drive + di.label);
			// JComboBox cb = (JComboBox)evt.getSource();
			id.setText("");
			fillCommunityList();
			setSNFromCurrentDrive();
			return;
		} else
			return;
		
		disableAll();
		try {
			Logger.LogString("ACTION: " + b.getText());
			Object item = driveList.getSelectedItem();
			di = (DriveInfo)item;
			File drive = di.drive;
			if (drive == null) { 
				refreshUI();
				return;
			}
			Logger.LogString("ID:"+((DriveInfo)driveList.getSelectedItem()).serialNumber);
			setSNFromCurrentDrive();
			if (di.serialNumber == NO_SERIAL_NUMBER) {
				// use drive label if exists
				int parens = di.label.indexOf(" (");
				if (parens > 0)
					di.serialNumber = di.label.substring(0, parens);			
				if (di.serialNumber.startsWith("TBXXXX") || (!di.serialNumber.startsWith("ECH") && !di.serialNumber.startsWith("TB"))) {
					Logger.LogString("No .srn found.  Disk label was " + di.serialNumber + ". Using temp id for now.");
					String milliseconds = new String (String.valueOf((Calendar.getInstance().getTimeInMillis())));
					milliseconds = milliseconds.substring(milliseconds.length()-5, milliseconds.length());
					di.serialNumber = "TBt" + milliseconds;
				}
			}
			status.setText("STATUS: Starting ...");
			if (b == xfer) {
				xferFiles();
				Logger.init();
				return;
			} 
			String dateTime = getDateTime();
			if (item != null) {
				prevSelected = drive;
			}
			
			String devicePath = drive.getAbsolutePath();
			String community = communityList.getSelectedItem().toString();
			Logger.LogString("Community: " + community);
			if ((b == reformat || b == update) && fetchIDFromServer.isSelected()) {
				di.serialNumber = fetchNextDeviceID();
				this.id.setText(di.serialNumber);
				Logger.LogString("SN: " + di.serialNumber);
			} 
			if (communityList.getSelectedIndex() == 0) {
				int response = JOptionPane.showConfirmDialog(this, "No community selected.\nAre you sure?", 
	                "Confirm", JOptionPane.YES_NO_OPTION);
				if (response != JOptionPane.YES_OPTION) {
					Logger.LogString("No community selected. Are you sure? NO");
					refreshUI();
					return;
				} else 
					Logger.LogString("No community selected. Are you sure? YES");
			} else
				prevSelectedCommunity = communityList.getSelectedIndex();
			if ((b == reformat) && (communityList.getSelectedIndex() == 4 ||communityList.getSelectedIndex() == 6) && !handIcons.isSelected()) {
				// warn if reformatting for Jonga or Gyanvuuri but not adding hand icon messages
				int response = JOptionPane.showConfirmDialog(this, "Reformatting, but hand icons not selected for " + community + ". \nAre you sure?",
	                "Confirm", JOptionPane.YES_NO_OPTION);
				if (response != JOptionPane.YES_OPTION) {
					Logger.LogString("Hand icons not selected for reformatting of " + community + ". Are you sure? NO");
					refreshUI();
					return;
				} else
					Logger.LogString("Hand icons not selected for reformatting of " + community + ". Are you sure? YES");
			} else if ((b == reformat || b == update) && (communityList.getSelectedIndex() != 4 && communityList.getSelectedIndex() != 6) && handIcons.isSelected()){
				// warn if adding hand icon messages to arrow communities when either updating or reformatting 
				int response = JOptionPane.showConfirmDialog(this, "Hand icons are selected for " + community + ". \nAre you sure?",
		                "Confirm", JOptionPane.YES_NO_OPTION);
				if (response != JOptionPane.YES_OPTION) {
					Logger.LogString("Hand icons are selected for " + community + ". Are you sure? NO");
					refreshUI();
					return;
				} else
					Logger.LogString("Hand icons are selected for " + community + ". Are you sure? YES");
			} else
				prevSelectedCommunity = communityList.getSelectedIndex();
			if (b == update) {
				boolean goodDisk = chkDsk(di);
				if (goodDisk) {
					Logger.LogString("chkdsk was good.");
					CopyThread t;
					if (di.lastIssue == 1)
						t = new CopyThread(this, devicePath, community, di.serialNumber, sourceDir, dateTime, handIcons.isSelected(), "update1to3");
					else 
						t = new CopyThread(this, devicePath, community, di.serialNumber, sourceDir, dateTime, handIcons.isSelected(), "update");
					t.start();
				} else {
					Logger.LogString("chkdsk was bad for " + di.label.toString());
					int response = JOptionPane.showConfirmDialog(null, "TB is corrupted.\nBackup takes 6-8 minutes.\nBegin?",
			              "Confirm", JOptionPane.OK_CANCEL_OPTION);
					if (response == JOptionPane.OK_OPTION) {
						Logger.LogString("TB is corrupted.  Backup takes 6-8 minutes. Begin?  YES");
						backup(devicePath, community, di.serialNumber, dateTime);
						CopyThread t = new CopyThread(this, devicePath, community, di.serialNumber, sourceDir, dateTime, handIcons.isSelected(), "corrupted");
						t.start();
					} else {
						Logger.LogString("TB is corrupted.  Backup takes 6-8 minutes. Begin?  NO");
					}
				}
			} else if (b == backup) {
				int response = JOptionPane.showConfirmDialog(null, "Backup takes 6-8 minutes.\nBegin?",
			              "Confirm", JOptionPane.OK_CANCEL_OPTION);
				if (response == JOptionPane.OK_OPTION) {
					Logger.LogString("Backup takes 6-8 minutes. Begin?  YES");
					backup(devicePath, community, di.serialNumber, dateTime);
				} else
					Logger.LogString("Backup takes 6-8 minutes. Begin?  NO");
			} else if (b == reformat) {
					Logger.LogString("REFORMATTING");
					CopyThread t = new CopyThread(this, devicePath, community, di.serialNumber, sourceDir, dateTime, handIcons.isSelected(), "reformat");
					t.start();
			} else if (b == setCommunity)
					setCommunity(devicePath, community, dateTime);
			refreshUI();
			Logger.init();
			return;
		} catch (Exception ex) {
			Logger.LogString(ex.toString());
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "An error occured.",
	                "Error", JOptionPane.ERROR_MESSAGE);
			resetUI(false);
		} 
	}

	private void backup(String devicePath, String community, String serialNumber, String dateTime) {
		String imagePathForMkDir, imagePathForCmdLine, imageFile;
		String line;
		
		try {
			imagePathForCmdLine = "\"" + TEMP_COLLECTION_DIR + "\\" + community + "\\" + serialNumber + "\\" + dateTime;
			imagePathForMkDir = TEMP_COLLECTION_DIR + "\\" + community + "\\" + serialNumber + "\\" + dateTime;
			Logger.LogString("backing up to " + imagePathForCmdLine);
			File f = new File(imagePathForMkDir);
			for (int i=0; i<3;i++) {
				if (f.mkdirs()) {
					Logger.LogString("directories created");
					break;
				} else
					Logger.LogString("directories not created yet");
			}
			imageFile = imagePathForCmdLine + "\\" + serialNumber + ".zip\"";
			execute("cmd /C " + CREATE_BACKUP + devicePath + " " + imageFile + " " + SW_SUBDIR);
			JOptionPane.showMessageDialog(this, "Backup complete",
	                "Completed", JOptionPane.DEFAULT_OPTION);
			Logger.LogString("Backup complete");
		} catch (Exception e) {
			Logger.LogString(e.getLocalizedMessage());
			e.printStackTrace();
		}
		refreshUI();
	}

	private void resetUI(boolean resetDrives) {
		Logger.LogString("Resetting UI");
		Logger.LogString(" -clearing id text");
		id.setText("");
		if (resetDrives && !monitoringDrive) {
			Logger.LogString(" -fill drives list");
			fillList(getRoots());
		} else if (resetDrives && monitoringDrive) {
			Logger.LogString(" - drive list currently being filled by drive monitor");
		}
//		Logger.LogString(" -fill communities list");
//		fillCommunityList();
		Logger.LogString(" -refresh UI");
		refreshUI();
	}
	
	private synchronized boolean driveConnected() {
		boolean connected = false;
		File drive;
		
		if (driveList.getItemCount() > 0) {		
			drive = ((DriveInfo)driveList.getSelectedItem()).drive;
			if (drive != null)
				connected = true;
		}
		return connected;
	}

	private synchronized void checkDirUpdate() {
		String triggerFile = copyTo + "\\" + TRIGGER_FILE_CHECK;
		File f = new File (triggerFile);
		if (f.exists()) {
			status.setText("Updating list of files to send");
			try {
				f.delete();
				execute("cmd /C dir " + copyTo + " /S > " + copyTo + "\\dir.txt");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}			
	}
	
	private void refreshUI() {

		disableAll();
		checkDirUpdate();
		
		update.setText("Update");
		reformat.setText("Reformat");
		backup.setText("Backup");
		setCommunity.setText("Set Community");
		xfer.setEnabled((new File(TEMP_COLLECTION_DIR)).exists());			
		handIcons.setEnabled(true);
		fetchIDFromServer.setEnabled(true);
		if (driveConnected()) {
			update.setEnabled(true);
			reformat.setEnabled(true);
			backup.setEnabled(true);
			setCommunity.setEnabled(true);
			status.setText("STATUS: Ready");
			Logger.LogString("STATUS: Ready");
		} else {
			update.setEnabled(false);
			reformat.setEnabled(false);
			backup.setEnabled(false);
			setCommunity.setEnabled(false);
			Logger.LogString("STATUS: " + NO_DRIVE);
			status.setText("STATUS: " + NO_DRIVE);
		}
	}

	private void disableAll() {
		// This is used after startup and when there have been no changes to the drives 
		update.setEnabled(false);
		reformat.setEnabled(false);
		backup.setEnabled(false);
		xfer.setEnabled(false);	
		setCommunity.setEnabled(false);
		handIcons.setEnabled(false);
		fetchIDFromServer.setEnabled(false);
	}
	
	void finishCopy(boolean success, final String idString, final String mode) {
		final TBLoader parent = this;
		try {
			final Runnable runnable;
			if (success) {
				runnable = new Runnable() {
					@Override
					public void run() {
						JOptionPane.showMessageDialog(parent, "Success!",
				                "Success", JOptionPane.DEFAULT_OPTION);
						Logger.LogString("Success");
					}
				};
			} else {
				runnable = new Runnable() {
					@Override
					public void run() {
						JOptionPane.showMessageDialog(parent, "An error occured while copying files to the Talking Book.",
				                "Error", JOptionPane.ERROR_MESSAGE);
						Logger.LogString("ERROR!");
					}
				};
			}
			parent.refreshUI();
			Logger.init();
			SwingUtilities.invokeAndWait(runnable);
		} catch (InterruptedException e) {
			Logger.LogString(e.toString());
			Logger.init();
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			Logger.LogString(e.toString());
			Logger.init();
			e.printStackTrace();
		} catch (Exception e){
			Logger.LogString(e.toString());
			Logger.init();
			e.printStackTrace();
		}
	}

	public static class Logger {
		private static BufferedWriter bw;

		public Logger () {
			init();
		}
		public synchronized static void LogString(String s) {
			if (s == null)
				return;
			try {
				System.out.println("Log:" + s);
				if (bw != null) {
					bw.write(getDateTime() + ": " + s+"\r\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
				close();
				open();
			}
		}

		public static void close() {
			try {
				if (bw != null) {
					bw.flush();
					bw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private static void open() {
			try {
				bw = new BufferedWriter(new FileWriter(getLogFileName()));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public static void init () {
			try {
				close();
				open();
				LogString("Version: " + VERSION);
				LogString("App Path: " + new File(".").getAbsolutePath());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class CopyThread extends Thread {
		private final String devicePath;
		private final String community;
		private final String id;
		private final String sourceDir;
		private final String datetime;
		private final TBLoader callback;
		private final boolean useHandIcons;
		private final String mode;
		boolean success = false;
		private BufferedWriter out;
		
		public CopyThread(TBLoader callback, String devicePath, String community, String id, String sourceDir, String datetime, boolean useHandIcons, String mode) {
			this.callback = callback;
			this.community 	= community;
			this.devicePath = devicePath;
			this.id = id;
			this.sourceDir = sourceDir;
			this.datetime = datetime;
			this.useHandIcons = useHandIcons;
			this.mode = mode;
		}		
		@Override public void run() {
			try {
				String handValue;
				if (this.useHandIcons)
					handValue = "1";
				else
					handValue = "0";
				BufferedReader reader;
				reader = new BufferedReader(new FileReader(new File(SCRIPT_SUBDIR + mode +".txt")));
				Logger.LogString("ID="+id);
				while (reader.ready()) {
					String cmd = reader.readLine();
					if (cmd.startsWith("rem ")) {
						status.setText("STATUS: " + cmd.substring(4));
						Logger.LogString(cmd.substring(4));
						continue;
					}
					cmd = cmd.replaceAll("\\$\\{device_drive\\}", devicePath.substring(0, 2));
					cmd = cmd.replaceAll("\\$\\{community\\}", community);
					cmd = cmd.replaceAll("\\$\\{device_id\\}", id);
					cmd = cmd.replaceAll("\\$\\{source_dir\\}", sourceDir);
					cmd = cmd.replaceAll("\\$\\{datetime\\}", datetime);
					cmd = cmd.replaceAll("\\$\\{send_now_dir\\}", Matcher.quoteReplacement(copyTo));
					cmd = cmd.replaceAll("\\$\\{holding_dir\\}", Matcher.quoteReplacement(TEMP_COLLECTION_DIR));
					cmd = cmd.replaceAll("\\$\\{hand\\}", handValue);
					boolean alert = cmd.startsWith("!");
					if (alert)
						cmd = cmd.substring(1);
					if (cmd.startsWith("format")) {
						int response = JOptionPane.showConfirmDialog(null, "Talking Book will now be ERASED.\nAre you sure?",
			                "Confirm", JOptionPane.YES_NO_OPTION);
						if (response != JOptionPane.YES_OPTION) {
							return;
						}
					}					
					if (!execute("cmd /C " + cmd) && alert) {
						return;
					}
				}
				reader.close();
				// now that any possible reformatting is complete, copy over the new serial number 
				if (callback.fetchIDFromServer.isSelected()) {
					File f = new File(devicePath + "-erase-srn." + id + ".srn");
					f.createNewFile();
					f = new File(devicePath + "inspect");
					f.createNewFile();
				} 

				success = true;
				Toolkit.getDefaultToolkit().beep();
			} catch (Exception e) {
				Logger.LogString(e.toString());
				Logger.init();
				e.printStackTrace();
			} finally {
				callback.finishCopy(success, id, this.mode);
			}
		}
	}

	private static final boolean debug = false;

	static boolean chkDsk(DriveInfo di) throws Exception {
		String line;
		String foldersIssue1[]={"system","languages","messages","messages/lists","messages/audio","languages/dga","system/stats","system/ostats"};
		String foldersIssue2[]={"system","languages","messages","statistics","log-archive","messages/lists","messages/audio","languages/dga","statistics/stats","statistics/ostats"};
		String folders[];
		boolean goodDisk = true;
		
		Logger.LogString("cmd /C echo n|chkdsk " + di.drive.toString());
		// perform check disk
		Process proc = Runtime.getRuntime().exec("cmd /C echo n|chkdsk " + di.drive.toString());

		BufferedReader br1 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedReader br2 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

		while (goodDisk && (line = br1.readLine()) != null) {
			int i;
			Logger.LogString(line);
			if ((i = line.indexOf("is cross-linked on allocation unit")) > -1) {
				System.out.println(i);
				Logger.LogString(line.substring(0, i));
				di.corruptions[di.corruptionCount] = line.substring(0, i);
				Logger.LogString("***BAD:" + di.corruptions[di.corruptionCount]);
				di.corruptionCount++;
			}				
			if (line.startsWith("Windows found problems with the file system.")) {
				goodDisk = false;
			}
			if (line.startsWith("Volume ") && line.contains(" created ")) {
//				di.label = new String(line.substring(7, line.indexOf(' ', 7)));
				Logger.LogString("***LABEL:" + line.substring(7, line.indexOf(' ', 7)));
			}
		} 

		// check for existence of the most important folders, depending on Issue #
		if (di.lastIssue == 1)
			folders = foldersIssue1;
		else
			folders = foldersIssue2;
		
		for (int i = 0; i < folders.length; i++) {
			String folderName = new String(di.drive.toString() + folders[i]);
			Logger.LogString(folderName);
			if (!(new File(folderName)).exists()) {
				di.corruptions[di.corruptionCount] = folderName;
				Logger.LogString("***BAD:" + di.corruptions[di.corruptionCount]);
				di.corruptionCount++;
				goodDisk = false;
			}
		}
		
		do {
			line = br2.readLine();
			Logger.LogString(line); 
			
		} while (line != null);
		br1.close();
		br2.close();
		proc.waitFor();
		return goodDisk;
	}	
	static boolean execute(String cmd) throws Exception {
		String line;
		String log;
		Logger.LogString("Executing:" + cmd);
		if (debug) {
			return true;
		}

		Process proc = Runtime.getRuntime().exec(cmd);

		BufferedReader br1 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedReader br2 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		
		do {
			line = br1.readLine();
			Logger.LogString(line);
		} while (line != null);
		
		
		do {
			line = br2.readLine();
			Logger.LogString(line);
		} while (line != null);
		
		return proc.waitFor() == 0;
	}
	
	private static class DriveInfo {
		final File drive;
		String label;
		String serialNumber;
		int lastIssue;
		static final int MAX_CORRUPTIONS = 20;
		String corruptions[] = new String [MAX_CORRUPTIONS];
		int corruptionCount;
		
		public DriveInfo(File drive, String label) {
			this.drive = drive;
			this.label = label.trim();
			this.corruptionCount = 0;
			this.serialNumber = "";
			this.lastIssue = 0; // 0 means no info
			updateLastIssue();
		}

		public boolean updateLastIssue() {
			boolean foundIssue = false;
			int rev = 0;
			int i;
			
			if (this.drive == null) 
				return false;
			try {
				File[] files;
				File systemPath = new File(this.drive.getAbsolutePath(), "system");
				if (!systemPath.exists())
					return false;
				files = systemPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return (lowercase.endsWith(".rev") || lowercase.endsWith(".img"));
					}
				});
				for (i = 0; i<files.length; i++) {
					String filenameRev = files[i].getName();
					filenameRev = filenameRev.substring(1,filenameRev.indexOf('.'));
					rev = Integer.parseInt(filenameRev);
					if (rev > 700) {
						foundIssue = true;
						break;
					}
				}
				if (foundIssue) {
					if (rev < 860)
						this.lastIssue = 1;
					else if (rev < 900)
						this.lastIssue = 2;
					else this.lastIssue = 3;
					Logger.LogString(this.label + " is currently Issue #" + String.valueOf(this.lastIssue) + " (based on " + files[i].getName() + ")");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return foundIssue;
		}
		
		
		@Override
		public String toString() {
			if (label.isEmpty()) {
				return drive.toString();
			}
			return label;
		}
	}
	
}
