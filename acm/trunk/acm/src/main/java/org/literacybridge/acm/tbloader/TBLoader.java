package org.literacybridge.acm.tbloader;

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileSystemView;

import org.jdesktop.swingx.JXDatePicker;
// commenting out import below so that TBLoader can stand-alone as .class 
// until new ACM is running on Fidelis's laptop
//import org.literacybridge.acm.utils.OSChecker;

//import org.jdesktop.swingx.JXDatePicker;

@SuppressWarnings("serial")
public class TBLoader extends JFrame implements ActionListener {
	private static final String VERSION = "v1.20r1195";   // inclusion of flash stats TBInfo class
	private static final String COLLECTION_SUBDIR = "\\collected-data";
	private static String TEMP_COLLECTION_DIR = "";
	private static final String SW_SUBDIR = ".\\software\\";
	private static final String CONTENT_SUBDIR = ".\\content\\";
	private static final String CONTENT_BASIC_SUBDIR = "basic\\";
	private static final String COMMUNITIES_SUBDIR = "communities\\";
	private static final String IMAGES_SUBDIR = "packages\\";
	private static final String SCRIPT_SUBDIR = SW_SUBDIR + "scripts\\";
	private static final String NO_SERIAL_NUMBER = "UNKNOWN";
	private static final String NO_DRIVE = "(nothing connected)";
	private static final String TRIGGER_FILE_CHECK = "checkdir";
	private static String imageRevision = "(no rev)"; 
	private static String dateRotation;
	private static JComboBox newDeploymentList;
	private static JComboBox newCommunityList;
	private static JComboBox currentLocationList;
	private static JComboBox driveList;
	private static JTextField oldID;
	private static JTextField newID;
	private static JTextField oldRevisionText;
	private static JTextField newRevisionText;
	private static JTextField oldImageText;
	private static JTextField newImageText;
	private static JTextField oldDeploymentText;
	private static JTextField oldCommunityText;
	private static JTextField lastUpdatedText;
	private static JLabel oldValue;
	private static JLabel newValue;
	private static JTextArea status;
	private static JTextArea status2;
	private static String homepath;
	private static JButton update;
//	private static JButton reformat;
//	private static JButton backup;
//	private static JButton xfer;
	private static JButton grabStatsOnly;
//	private static JButton setCommunity;
	private static String copyTo;
	private static String pathOperationalData;
	private static String revision;
	public static String deploymentName;
	public static String sourcePackage;
	public static int durationSeconds;
	public static DriveInfo currentDrive;
	private static String srnPrefix;
	//private JCheckBox fetchIDFromServer;
	//private JCheckBox handIcons;
	TBInfo tbStats;
	static String volumeSerialNumber = "";
	private static String deviceID; // this device is the computer/tablet/phone that is running the TB Loader
	
	class WindowEventHandler extends WindowAdapter {
		public void windowClosing(WindowEvent evt) {
			checkDirUpdate();
			Logger.LogString("closing app");
			Logger.close();
		    System.exit(0);
		}
	}
	
	private String currentLocation[] = new String[] {
			"Select location",
			"Community",
			"Jirapa office",
			"Wa office",
			"Other"
	};
	
	void getRevisionNumbers() {
		revision = "(No firmware)";

		File basicContentPath = new File(CONTENT_SUBDIR + newDeploymentList.getSelectedItem().toString() + "\\" + CONTENT_BASIC_SUBDIR);
		Logger.LogString("DEPLOYMENT:"+newDeploymentList.getSelectedItem().toString());
		try {
			File[] files;
			if (basicContentPath.exists()) {
				// get Package
				files = basicContentPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".img");
					}
				});				
				if (files.length > 1)
					revision = "(Multiple Firmwares!)";
				else if (files.length == 1) {
					revision = files[0].getName();
					revision = revision.substring(0, revision.length() - 4);
				}
				newRevisionText.setText(revision);

/*				// get Package name
				File contentSystem = new File(basicContentPath,"system");
				files = contentSystem.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".pkg");
					}
				});				
				if (files.length > 1)
					sourcePackage = "(Multiple Pkg Names!)";
				else if (files.length == 1) {
					sourcePackage = files[0].getName();
					sourcePackage = sourcePackage.substring(0, sourcePackage.length() - 4);
				}
*/			}
		} catch (Exception ignore) {
			Logger.LogString("exception - ignore and keep going with default string");
		}

	}

	public TBLoader(String srnPrefix) throws Exception {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addWindowListener(new WindowEventHandler());
		setDeviceIDandPaths();
		if (srnPrefix != null) {
			TBLoader.srnPrefix = srnPrefix;
		} else {
			TBLoader.srnPrefix = "b-";
		}
		// get image revision
		File swPath = new File(".");
		File[] files = swPath.listFiles(new FilenameFilter() {
			@Override public boolean accept(File dir, String name) {
				String lowercase = name.toLowerCase();
				return lowercase.endsWith(".rev");
			}
		});				
		if (files.length > 1)
			imageRevision = "(Multiple Image Revisions!)";
		else if (files.length == 1) {
			imageRevision = files[0].getName();
			imageRevision = imageRevision.substring(0, imageRevision.length() - 4);
		}
		setTitle("TB-Loader " + VERSION + "/" + imageRevision); 

		//"   Update:" + deploymentName + "   Firmware:" + revision);
		
		JPanel panel = new JPanel();
		JLabel warning;
		if (TBLoader.srnPrefix.equals("a-")) {
			panel.setBackground(Color.CYAN);
			warning = new JLabel("Use with OLD TBS only");
			warning.setForeground(Color.RED);
		} else {
			warning = new JLabel("Use with NEW TBS only");
			warning.setForeground(Color.RED);
		}
		JLabel packageLabel = new JLabel("Update:");
		JLabel communityLabel = new JLabel("Community:");
		JLabel currentLocationLabel = new JLabel("Current Location:");
		JLabel dateLabel = new JLabel("First Rotation Date:");
		oldDeploymentText = new JTextField();
		oldDeploymentText.setEditable(false);
		oldValue = new JLabel("Previous");
		newValue = new JLabel("Next");
		lastUpdatedText = new JTextField();
		lastUpdatedText.setEditable(false);
		oldCommunityText = new JTextField();
		oldCommunityText.setEditable(false);
		JLabel deviceLabel = new JLabel("Talking Book Device:");
		JLabel idLabel = new JLabel("Serial number:");
		JLabel revisionLabel = new JLabel("Firmware:");
		JLabel imageLabel = new JLabel("Content:");
		status = new JTextArea(2,40);
		status.setEditable(false);
		status.setLineWrap(true);
		status2 = new JTextArea(2,40);
		status2.setEditable(false);
		status2.setLineWrap(true);
		oldID = new JTextField();
		oldID.setEditable(false);
		newID = new JTextField();
		newID.setEditable(false);
		oldRevisionText = new JTextField();
		oldRevisionText.setEditable(false);
		newRevisionText = new JTextField();
		newRevisionText.setEditable(false);
		oldImageText = new JTextField();
		oldImageText.setEditable(false);
		newImageText = new JTextField();
		newImageText.setEditable(false);
		final JXDatePicker datePicker = new JXDatePicker();
		datePicker.getEditor().setEditable(false);
		datePicker.setFormats(new String[] { "yyyy/MM/dd" }); //dd MMM yyyy
		datePicker.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dateRotation = datePicker.getDate().toString();
			}	
		});
		 	
		newDeploymentList = new JComboBox();
		newDeploymentList.addActionListener(this);
		newCommunityList = new JComboBox();
		newCommunityList.addActionListener(this);
		driveList = new JComboBox();
		driveList.addActionListener(this);
		currentLocationList = new JComboBox(currentLocation);		
		//fetchIDFromServer = new JCheckBox("Get new serial number");
		//fetchIDFromServer.setSelected(false);
		//handIcons = new JCheckBox("Use hand icon msgs");
		//handIcons.setSelected(false);
		update = new JButton("Update TB");
		update.addActionListener(this);
		grabStatsOnly = new JButton("Get Stats");
		grabStatsOnly.addActionListener(this);
//		reformat = new JButton("Reformat");
//		reformat.addActionListener(this);
//		backup = new JButton("Backup");
//		backup.addActionListener(this);
//		xfer = new JButton("Upload Audio");
//		xfer.addActionListener(this);
//		setCommunity = new JButton("Set Community");
//		setCommunity.addActionListener(this);
		
		
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout.createSequentialGroup()
	        		.addGroup(layout.createParallelGroup(LEADING)
	        				.addComponent(deviceLabel)
	        				.addComponent(currentLocationLabel)
	        				.addComponent(packageLabel)
	        				.addComponent(communityLabel)
	                		.addComponent(imageLabel)
	        				.addComponent(dateLabel)
	                		.addComponent(revisionLabel)
	        				.addComponent(idLabel)
	        				)
 					.addGroup(layout.createParallelGroup(LEADING)
 							.addComponent(warning)
 							.addComponent(driveList)
							.addComponent(currentLocationList)
            				.addComponent(newValue)
            				.addComponent(newDeploymentList)
	                		.addComponent(newCommunityList)
	                		.addComponent(newImageText)
							.addComponent(datePicker)
	                		.addComponent(newRevisionText)
	                		.addComponent(newID)
            				//.addComponent(fetchIDFromServer)
//	    	                .addGroup(layout.createSequentialGroup()
	                				.addComponent(update)
	    	    		     .addComponent(status2)
	    	        		)
 					.addGroup(layout.createParallelGroup(LEADING)
            				.addComponent(oldValue)
	                		.addComponent(oldDeploymentText)
	                		.addComponent(oldCommunityText)
	                		.addComponent(oldImageText)
	                		.addComponent(lastUpdatedText)
	                		.addComponent(oldRevisionText)
	                		.addComponent(oldID)
	    	        		.addComponent(grabStatsOnly)
	                		//.addComponent(handIcons)
//	    	                .addGroup(layout.createSequentialGroup()
//	    	        				.addComponent(setCommunity)
//	                 				.addComponent(xfer)
//              				)
	    		    		.addComponent(status)
	                  	)
	               	);
        
        layout.setVerticalGroup(layout.createSequentialGroup()
        		.addComponent(warning)
                .addGroup(layout.createParallelGroup(BASELINE)
        				.addComponent(deviceLabel)
                		.addComponent(driveList))
                .addGroup(layout.createParallelGroup(BASELINE)
                		.addComponent(currentLocationLabel)
                		.addComponent(currentLocationList))
                .addGroup(layout.createParallelGroup(BASELINE)
        				.addComponent(newValue)
                		.addComponent(oldValue))
                .addGroup(layout.createParallelGroup(BASELINE)
                        .addComponent(packageLabel)
                        .addComponent(newDeploymentList)
	    	            .addComponent(oldDeploymentText))
                .addGroup(layout.createParallelGroup(BASELINE)
                		.addComponent(communityLabel)
                        .addComponent(newCommunityList)
                		.addComponent(oldCommunityText))
                .addGroup(layout.createParallelGroup(BASELINE)
                		.addComponent(imageLabel)
	    	            .addComponent(newImageText)
	    	            .addComponent(oldImageText))
                .addGroup(layout.createParallelGroup(BASELINE)
                		.addComponent(dateLabel)
                        .addComponent(datePicker)
                		.addComponent(lastUpdatedText))
                .addGroup(layout.createParallelGroup(BASELINE)
                		.addComponent(revisionLabel)
	    	            .addComponent(newRevisionText)
	    	            .addComponent(oldRevisionText))
	    	    .addGroup(layout.createParallelGroup(BASELINE)
                		.addComponent(idLabel)
                		.addComponent(oldID)
                		.addComponent(newID))
		        // .addGroup(layout.createParallelGroup(BASELINE)
		        	//.addComponent(fetchIDFromServer)
    				//.addComponent(handIcons))
    		    .addGroup(layout.createParallelGroup(BASELINE)
    		    	.addComponent(update)
    		    	.addComponent(grabStatsOnly)
//    		    	.addComponent(setCommunity)
//		        	.addComponent(xfer)
//    	    		.addComponent(backup)
//    	    		.addComponent(reformat)
    	    		)
    		    .addGroup(layout.createParallelGroup(BASELINE)
    		    		.addComponent(status2)
    		    		.addComponent(status))
            );
        
        setSize(600,500);
        add(panel, BorderLayout.CENTER);
//      add(status, BorderLayout.SOUTH);
//      add(xfer, BorderLayout.EAST);
		setLocationRelativeTo(null);
		
		//Logger.init();
		filldeploymentList();
		resetUI(true);
		setVisible(true);
		Logger.LogString("set visibility - starting drive monitoring");
		deviceMonitorThread.setDaemon(true);
		deviceMonitorThread.start();
		startUpDone = true;
		JOptionPane.showMessageDialog(null, "Remember to power Talking Book with batteries before connecting with USB.",
                "Use Batteries!", JOptionPane.DEFAULT_OPTION);
	}
	
	public static boolean startUpDone = false;
	public static boolean monitoringDrive = false;
	public static boolean updatingTB = false;
	private static String lastSynchDir;
	
	public static void main(String[] args) throws Exception {
		if (args.length == 1) {
			new TBLoader(args[0]);
		} else {
			new TBLoader(null);
		} 
	}

	private static String twoOrFourChar(int i) {
		String s;
		s=String.valueOf(i);
		if (s.length()==1 || s.length()==3)
			s = "0" + s;
		return s;
	}
	
	private static String getDateTime() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1	;
		int date = cal.get(Calendar.DAY_OF_MONTH);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		String dateTime = twoOrFourChar(year) + "y" + twoOrFourChar(month) + "m" + twoOrFourChar(date) + "d" + 
				twoOrFourChar(hour) + "h" + twoOrFourChar(min) + "m" + twoOrFourChar(sec) + "s";
		return dateTime;
	}
	
	private static String getLogFileName() {
		String filename;
		File f;
		
		filename = pathOperationalData + "/logs";
		f = new File(filename);
		if (!f.exists())
			f.mkdirs();
		filename += "\\log-" + (TBLoader.currentDrive.datetime.equals("")?getDateTime():TBLoader.currentDrive.datetime) +".txt";
		return filename;
	}
	private void setDeviceIDandPaths() {
		int i = 0;
		final int MAX_PATHS = 5;
		String paths[]= new String[MAX_PATHS];

		try {
			Process proc = Runtime.getRuntime().exec("cmd /C echo %HOMEDRIVE%%HOMEPATH%");
			BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			homepath = new String(reader.readLine());
			reader.close();
			TEMP_COLLECTION_DIR = new String(homepath + "\\LiteracyBridge");
			File f = new File(TEMP_COLLECTION_DIR);
			f.mkdirs();

			File[] files = f.listFiles(new FilenameFilter() {
				@Override public boolean accept(File dir, String name) {
					String lowercase = name.toLowerCase();
					return lowercase.endsWith(".dev");
				}
			});
			if (files.length == 1) {
				TBLoader.deviceID = files[0].getName().substring(0, files[0].getName().length() - 4);
			} else {
				// create new device ID
				String systemString = System.getenv("COMPUTERNAME") + System.getenv("USERNAME");
				int hash = systemString.hashCode();
				hash *= System.currentTimeMillis();
				systemString = Integer.toHexString(hash);
				systemString = systemString.substring(systemString.length()-8);
				TBLoader.deviceID = systemString;
				File newFile = new File(f,TBLoader.deviceID + ".dev");
				newFile.createNewFile();
			}

			// now that local path is established, the 'collected-data' subdir is what should 
			// only be present when there is local storage (the xfer button is only enabled if it's there
			TEMP_COLLECTION_DIR += COLLECTION_SUBDIR; 
			reader = new BufferedReader(new FileReader(new File(SW_SUBDIR + "paths.txt")));
			copyTo = "";
			while (reader.ready() && i < MAX_PATHS) {
				paths[i] = reader.readLine().replaceAll("%HOMEPATH%", Matcher.quoteReplacement(homepath));
				//Logger.LogString(paths[i]);
				if ((new File(paths[i])).exists()) {
					copyTo = paths[i];
					break;						
				}
			}
			reader.close();
		} catch (Exception e) {
			Logger.LogString(e.toString());
			Logger.flush();
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
		pathOperationalData = copyTo + "/OperationalData/" + TBLoader.deviceID;
		Logger.LogString("copyTo:"+copyTo);
	}
	
	int idCounter = 0;
	
/*	private String fetchNextDeviceID() throws Exception {
		try {
			if (!debug) {
				URL url = new URL("http://literacybridge.org/TBsrns.php");
				InputStream in = url.openStream();
				Scanner scanner = new Scanner(in);
			    scanner.useDelimiter(END_OF_INPUT);
			    String json = scanner.next();
			    return json;
			} else {
				return "TB-" + idCounter++;
			}
		} catch (Exception ex){
			Logger.LogString("Tried to get a new serial number, but the Internet does not seem to be available.\nPlease check your internet connection if you need to get a new serial number for this device.");
			Logger.LogString(ex.toString());
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "Tried to get a new serial number, but the Internet does not seem to be available.\nPlease check your internet connection if you need to get a new serial number for this device.",
	                "Error", JOptionPane.ERROR_MESSAGE);
			throw ex;
		}
	}
*/	
	private File prevSelected = null;
	private int prevSelectedCommunity = -1;
	
	private void getStatsFromCurrentDrive() throws IOException {
		DriveInfo di = TBLoader.currentDrive;
		if (di.drive == null)
			return;
		File rootPath = new File(di.drive.getAbsolutePath());
		File statsPath = new File(rootPath,"statistics/stats/flashData.bin");
		tbStats = new TBInfo(statsPath.toString());
		if (tbStats.countReflashes == -1)
			tbStats = null;
		if (!statsPath.exists())
			throw new IOException();
	}
	
	private String getCommunityFromCurrentDrive() {
		String communityName = "UNKNOWN";
		DriveInfo di = TBLoader.currentDrive;
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
			Logger.flush();
			ignore.printStackTrace();	
			// ignore and keep going with empty string
		}
		Logger.LogString("TB's current community name is "+communityName);
		return communityName;
	}
	
	private synchronized void filldeploymentList() {

		int indexSelected = -1;
		File contentPath = new File (CONTENT_SUBDIR);
		newDeploymentList.removeAllItems();
		File[] packageFolder = contentPath.listFiles();
		for (int i = 0; i<packageFolder.length; i++) {
			newDeploymentList.addItem(packageFolder[i].getName());
			if (imageRevision.startsWith(packageFolder[i].getName())) {
				indexSelected = i;
			}
		}
		if (indexSelected != -1) {
			newDeploymentList.setSelectedIndex(indexSelected);
		}
	}

	private synchronized void fillCommunityList() throws IOException {		
		newCommunityList.removeAllItems();
		File[] files;

		File fCommunityDir = new File(CONTENT_SUBDIR + newDeploymentList.getSelectedItem().toString() + "\\" + COMMUNITIES_SUBDIR);
		
		files = fCommunityDir.listFiles(new FilenameFilter() {
			@Override public boolean accept(File dir, String name) {
				return dir.isDirectory();
			}
		});
		newCommunityList.addItem("Non-specific"); 	// This string must match firmware code to generate special dings on startup 
												// as reminder that specific name has not been set.
		for(File f:files) {
			newCommunityList.addItem(f.getName());
		}
		setCommunityList();
	}

	private synchronized void setCommunityList() throws IOException {
		String driveCommunity;
		String driveLabel;
		try {
			getStatsFromCurrentDrive();
		} catch (IOException e) { 
			driveLabel = TBLoader.currentDrive.getLabelWithoutDriveLetter();
			if (isSerialNumberFormatGood(driveLabel)) {
				 // could not find flashStats file -- but TB should save flashstats on normal shutdown and on *-startup.
				JOptionPane.showMessageDialog(null, "The TB's statistics cannot be found. Please follow these steps:\n 1. Unplug the TB\n 2. Hold down the * while turning on the TB\n "
				+ "3. Observe the solid red light.\n 4. Now plug the TB into the laptop.\n 5. If you see this message again, please continue with the loading -- you tried your best.",
		                "Cannot find the statistics!", JOptionPane.DEFAULT_OPTION);			
			}
		}
		driveCommunity = getCommunityFromCurrentDrive();
		if (tbStats != null && tbStats.location != null && !tbStats.location.equals(""))
			oldCommunityText.setText(tbStats.location);
		else
			oldCommunityText.setText(driveCommunity);
		
		if (prevSelectedCommunity != -1)
			newCommunityList.setSelectedIndex(prevSelectedCommunity);
		else {
			int count = newCommunityList.getItemCount();
			for (int i =0; i<count; i++) {
				if (newCommunityList.getItemAt(i).toString().equalsIgnoreCase(driveCommunity)) {
					newCommunityList.setSelectedIndex(i);
					break;
				}
			}
		}
		try {
			getImageFromCommunity(newCommunityList.getSelectedItem().toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private synchronized void setSNandRevFromCurrentDrive() {
		String sn=NO_SERIAL_NUMBER;
		String rev="UNKNOWN";
		String pkg="UNKNOWN";
		DriveInfo di = TBLoader.currentDrive;
		if (di == null || di.drive == null)
			return;
		File systemPath = new File(di.drive.getAbsolutePath(), "system");
		try {
			File[] files;
			if (systemPath.exists()) {
				// get Serial Number file info
				
				if (tbStats != null && isSerialNumberFormatGood(tbStats.serialNumber))
					sn = tbStats.serialNumber;
				else {
					files = systemPath.listFiles(new FilenameFilter() {
						@Override public boolean accept(File dir, String name) {
							String lowercase = name.toLowerCase();
							return lowercase.endsWith(".srn") && !lowercase.startsWith("-erase");
						}
					});				
					if (files.length > 0) {
						String tsnFileName = files[0].getName();
						sn = tsnFileName.substring(0, tsnFileName.length() - 4);
						Logger.LogString("No stats SRN. Found *.srn file:" + sn);
					} else {
							Logger.LogString("No stats SRN and no good *.srn file found.");
							sn = NO_SERIAL_NUMBER;					
					}
				}	
				if (!isSerialNumberFormatGood(sn)) {
					if (sn.subSequence(1, 2).equals("-")) {
						if (sn.compareToIgnoreCase(TBLoader.srnPrefix) < 0)
							JOptionPane.showMessageDialog(null, "This appears to be an OLD TB.  If so, please close this program and open the TB Loader for old TBs.",
					                "OLD TB!", JOptionPane.WARNING_MESSAGE);
						else if (sn.compareToIgnoreCase(TBLoader.srnPrefix) > 0)
							JOptionPane.showMessageDialog(null, "This appears to be a NEW TB.  If so, please close this program and open the TB Loader for new TBs.",
					                "NEW TB!", JOptionPane.WARNING_MESSAGE);
					}
					sn=NO_SERIAL_NUMBER;
				}
				
				
				// get Revision number from .rev or .img file
				files = systemPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".img") || lowercase.endsWith(".rev") ;
					}
				});
				
				rev = "UNKNOWN";
				for (int i= 0; i<files.length;i++) {
					String revFileName = files[i].getName();
					revFileName = revFileName.substring(0, revFileName.length() - 4);
					//System.out.println(revFileName);
					if (i == 0)
						rev = revFileName;
					else if (!rev.equalsIgnoreCase(revFileName)) {
						break;
					} 
				}
				if (rev.length() == 0)
					rev = "UNKNOWN";  // eliminate problem of zero length filenames being inserted into batch statements
			}
			if (tbStats != null && tbStats.imageName != null && !tbStats.imageName.equals("")) 
				pkg = tbStats.imageName;
			else {
				// get packaage name from .pkg file
				files = systemPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".pkg");
					}
				});
				if (files.length == 1) {
					pkg = files[0].getName().substring(0, files[0].getName().length() - 4);
				}
			}
			oldImageText.setText(pkg);
			if (tbStats != null && tbStats.deploymentNumber != null && !tbStats.deploymentNumber.equals("")) 
				oldDeploymentText.setText(tbStats.deploymentNumber);
			else {
				// get package name from .pkg file
				files = systemPath.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".dep");
					}
				});
				if (files.length == 1) {
					oldDeploymentText.setText(files[0].getName().substring(0, files[0].getName().length() - 4));
				} else
					oldDeploymentText.setText("UNKNOWN");
			}
			
			// get last updated date from file
			files = systemPath.listFiles(new FilenameFilter() {
				@Override public boolean accept(File dir, String name) {
					String lowercase = name.toLowerCase();
					return lowercase.equals("last_updated.txt");
				}
			});
			if (files.length == 1) {
				try{
				  FileInputStream fstream = new FileInputStream(files[0]);
				  DataInputStream in = new DataInputStream(fstream);
				  BufferedReader br = new BufferedReader(new InputStreamReader(in));
				  String strLine;
				  if ((strLine = br.readLine()) != null)   {
					  TBLoader.lastSynchDir = new String(strLine);
				  }
				  //Close the input stream
				  in.close();
				  } catch (Exception e){//Catch exception if any
				  System.err.println("Error: " + e.getMessage());
				}					 
			}
			if (tbStats != null && tbStats.updateDate != -1) 
				lastUpdatedText.setText(tbStats.updateYear + "/" + tbStats.updateMonth + "/" + tbStats.updateDate); 
			else {
				  String strLine = TBLoader.lastSynchDir;
				  if (strLine != null) {
					  int y = strLine.indexOf('y');
					  int m = strLine.indexOf('m');
					  int d = strLine.indexOf('d');
					  lastUpdatedText.setText(strLine.substring(0, y) + "/" + strLine.substring(y+1,m) + "/" + strLine.substring(m+1,d));
				  } else {
					  lastUpdatedText.setText("UNKNOWN");
				  }
			}			
		} catch (Exception ignore) {
			Logger.LogString(ignore.toString());
			Logger.LogString("exception - ignore and keep going with empty string");
		}
		if (sn.equals("")) {
			sn = NO_SERIAL_NUMBER;
		}
		oldID.setText(sn);
		di.serialNumber = sn;
		newID.setText(sn);
		oldRevisionText.setText(rev);
		//if (di.serialNumber.equals("UNKNOWN") || di.serialNumber.equals(NO_SERIAL_NUMBER)) {
		//	JOptionPane.showMessageDialog(null, "The serial number cannot be found.\nIf you have internet access, please check the box for 'get new serial number'.\nIf you do not have internet access, you may continue without checking the box.",
	    //            "Need Date and Location!", JOptionPane.DEFAULT_OPTION);
			//this.fetchIDFromServer.setSelected(true);			
		//}
	}	

	private synchronized void fillList(File[] roots) {
		driveList.removeAllItems();
		TBLoader.currentDrive = null;
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
				} else if (label.startsWith("TB") || label.startsWith("a-") || label.substring(1, 2).equals("-"))
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
			TBLoader.currentDrive = (DriveInfo)driveList.getSelectedItem();
		}
	}

	private synchronized File[] getRoots() {
		File[] roots = null;
		// changing line below to allow TBLoader to run as a single .class file 
		// (until new ACM version is running on Fidelis's laptop)
		if (System.getProperty("os.name").startsWith("Windows")) { // (OSChecker.WINDOWS) {
			roots = File.listRoots();
		} else if (System.getProperty("os.name").startsWith("Mac OS")) { //(OSChecker.MAC_OS) {
			roots = new File("/Volumes").listFiles();
		}
		return roots;
	}
	
	private Thread deviceMonitorThread = new Thread() {
		@Override public void run() {
			Set<String> oldList = new HashSet<String>();
			
			while (true) { // don't do this during a TB update

				if (!updatingTB) {
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
						if (!((DriveInfo)driveList.getItemAt(0)).label.equals(NO_DRIVE)) {
							Logger.init();
							status2.setText("");
						}
	//					Logger.LogString("monitor filled drive list");
						try {
							fillCommunityList();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	//					Logger.LogString("monitor filled community list");
						setSNandRevFromCurrentDrive();
						getRevisionNumbers();
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
				}
				try {
					sleep(2000);
				} catch (InterruptedException e) {
					Logger.LogString(e.toString());
					Logger.flush();
					throw new RuntimeException(e);
				}

			}
		}
	};

	private void logTBData(String action) {
		final String VERSION_TBDATA = "v01";
		BufferedWriter bw;
		new Date();
		Calendar cal = Calendar.getInstance();
		int month = cal.get(Calendar.MONTH) + 1	;
		int date = cal.get(Calendar.DAY_OF_MONTH);
		int year = cal.get(Calendar.YEAR);
		String tbDataPath = pathOperationalData + "/tbData";
		File f = new File(tbDataPath);
		if (!f.exists())
			f.mkdirs();
		String dateTime = twoOrFourChar(year) + "y" + twoOrFourChar(month) + "m" + twoOrFourChar(date) + "d"; 
		String filename = tbDataPath + "/tbData-" + VERSION_TBDATA  + "-" + dateTime + "-"+ TBLoader.deviceID + ".csv";
		try {
			DriveInfo di = TBLoader.currentDrive;
			boolean isNewFile;
			f = new File(filename);
			isNewFile = !f.exists();
			bw = new BufferedWriter(new FileWriter(filename,true));
			if (bw != null) {
				if (isNewFile) {
					bw.write("UPDATE_DATE_TIME,LOCATION,ACTION,DURATION_SEC,");
					bw.write("OUT-SN,OUT-DEPLOYMENT,OUT-IMAGE,OUT-FW-REV,OUT-COMMUNITY,OUT-ROTATION-DATE,");
					bw.write("IN-SN,IN-DEPLOYMENT,IN-IMAGE,IN-FW-REV,IN-COMMUNITY,IN-LAST-UPDATED,IN-SYNCH-DIR,IN-DISK-LABEL,CHKDSK CORRUPTION?,");
					bw.write("FLASH-SN,FLASH-REFLASHES,");
					bw.write("FLASH-DEPLOYMENT,FLASH-IMAGE,FLASH-COMMUNITY,FLASH-LAST-UPDATED,FLASH-CUM-DAYS,FLASH-CORRUPTION-DAY,FLASH-VOLT,FLASH-POWERUPS,FLASH-PERIODS,FLASH-ROTATIONS,");
					bw.write("FLASH-MSGS,FLASH-MINUTES,FLASH-STARTS,FLASH-PARTIAL,FLASH-HALF,FLASH-MOST,FLASH-ALL,FLASH-APPLIED,FLASH-USELESS");
					for (int i=0;i<5;i++) {
						bw.write(",FLASH-ROTATION,FLASH-MINUTES-R"+i+",FLASH-PERIOD-R"+i+",FLASH-HRS-POST-UPDATE-R"+i+",FLASH-VOLT-R"+i);
					}
					bw.write("\n");
				}
				bw.write(TBLoader.currentDrive.datetime + ",");
				bw.write(currentLocationList.getSelectedItem().toString() + ",");
				bw.write(action + ",");
				bw.write(Integer.toString(TBLoader.durationSeconds) + ",");
				bw.write(newID.getText() + ",");
				bw.write(newDeploymentList.getSelectedItem().toString() + ",");
				bw.write(newImageText.getText() + ",");
				bw.write(newRevisionText.getText() + ",");
				bw.write(newCommunityList.getSelectedItem().toString() + ",");
				bw.write(dateRotation + ",");
				bw.write(di.serialNumber + ",");
				bw.write(oldDeploymentText.getText() + ",");
				bw.write(oldImageText.getText() + ",");
				bw.write(oldRevisionText.getText() + ",");
				bw.write(oldCommunityText.getText() + ",");
				bw.write(lastUpdatedText.getText() + ",");
				bw.write(TBLoader.lastSynchDir + ",");
				bw.write(TBLoader.currentDrive.label + ",");
				bw.write(di.corrupted + ",");
				if (tbStats != null) {
					bw.write(tbStats.serialNumber + ",");
					bw.write(tbStats.countReflashes + ",");
					bw.write(tbStats.deploymentNumber + ",");
					bw.write(tbStats.imageName + ",");
					bw.write(tbStats.location + ",");
					bw.write(tbStats.updateYear + "/" + tbStats.updateMonth + "/" + tbStats.updateDate + ",");
					bw.write(tbStats.cumulativeDays + ",");
					bw.write(tbStats.corruptionDay + ",");
					bw.write(tbStats.lastInitVoltage + ",");
					bw.write(tbStats.powerups + ",");
					bw.write(tbStats.periods + ",");
					bw.write(tbStats.profileTotalRotations + ",");
					bw.write(tbStats.totalMessages + ",");
					int totalSecondsPlayed=0, countStarted=0,countQuarter=0,countHalf=0,countThreequarters=0,countCompleted=0,countApplied=0,countUseless=0;
					for (int m=0;m < tbStats.totalMessages; m++) {
						for (int r=0;r < (tbStats.profileTotalRotations<5?tbStats.profileTotalRotations:5);r++) {
							totalSecondsPlayed += tbStats.stats[m][r].totalSecondsPlayed;
							countStarted += tbStats.stats[m][r].countStarted;
							countQuarter += tbStats.stats[m][r].countQuarter;
							countHalf += tbStats.stats[m][r].countHalf;
							countThreequarters += tbStats.stats[m][r].countThreequarters;
							countCompleted += tbStats.stats[m][r].countCompleted;
							countApplied += tbStats.stats[m][r].countApplied;
							countUseless += tbStats.stats[m][r].countUseless;
							}
					}
					bw.write(totalSecondsPlayed/60 + ",");
					bw.write(countStarted + ",");
					bw.write(countQuarter + ",");
					bw.write(countHalf + ",");
					bw.write(countThreequarters + ",");
					bw.write(countCompleted + ",");
					bw.write(countApplied + ",");
					bw.write(String.valueOf(countUseless));
					for (int r=0; r<(tbStats.profileTotalRotations<5?tbStats.profileTotalRotations:5); r++) {
						bw.write(","+ r + "," + tbStats.totalPlayedSecondsPerRotation(r)/60 + "," + tbStats.rotations[r].startingPeriod + ",");
						bw.write(tbStats.rotations[r].hoursAfterLastUpdate + "," + tbStats.rotations[r].initVoltage);
					}					
				}
				bw.write("\n");
				bw.flush();
				bw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
/*		s.append("Serial Number : " + this.serialNumber + NEW_LINE);
		s.append(NEW_LINE);
*/	}
	
//	public static void setCommunity(String path, String community, String dateTime) {
//		String locFilename = path + community + ".loc";
//		String rtcFilename = path + dateTime + ".rtc";
//		
//		try {
//			execute("cmd /C del \"" + path + "*.loc\" \"" + path + "system\\*.loc\" \"" + path + "*.rtc\"");
//
//			File locFile = new File(locFilename);
//			File rtcFile = new File(rtcFilename);
//			File inspectFile = new File(path + "inspect");
//			locFile.createNewFile();
//			rtcFile.createNewFile();
//			inspectFile.createNewFile();
//		} catch (Exception e) {
//			Logger.LogString(e.toString());
//			Logger.flush();
//			e.printStackTrace();
//		}
//	}

	private void xferFiles() {
		int response = JOptionPane.showConfirmDialog(this, "Do you currently have a good\n" +
				"Internet connection to upload large files" +
				"\n   in" + TEMP_COLLECTION_DIR + 
				"\n   to " + copyTo + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
		if (response != JOptionPane.YES_OPTION)
			return;
//		xfer.setEnabled(false);
		update.setEnabled(false);
		grabStatsOnly.setEnabled(false);
//		setCommunity.setEnabled(false);
		Logger.LogString("Transfering audio files to dropbox.");
		try {
			execute("cmd /C " + SW_SUBDIR + "robocopy \"" + TEMP_COLLECTION_DIR + "\" \"" + copyTo +"\" /MOVE /E");
		} catch (Exception e) {
			Logger.LogString(e.toString());
			Logger.flush();
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
	private String getImageFromCommunity(String community) throws Exception {
		String imageName = "UNKNOWN";
		if (community.equalsIgnoreCase("Non-specific")) {
			// grab first image package
			File imagedir = new File(CONTENT_SUBDIR + newDeploymentList.getSelectedItem().toString() + "\\" + IMAGES_SUBDIR + "\\");
			File[] images = imagedir.listFiles(new FilenameFilter() {
				@Override public boolean accept(File dir, String name) {
					return dir.isDirectory();
				}
			});
			imageName = images[0].getName();
		}		
		try {
			File[] files;
			File fCommunityDir = new File(CONTENT_SUBDIR + newDeploymentList.getSelectedItem().toString() + "\\" + COMMUNITIES_SUBDIR + "\\" + community + "\\" + "system");
			
			if (fCommunityDir.exists()) {
				// get Package
				files = fCommunityDir.listFiles(new FilenameFilter() {
					@Override public boolean accept(File dir, String name) {
						String lowercase = name.toLowerCase();
						return lowercase.endsWith(".pkg");
					}
				});				
				if (files.length > 1)
					throw new Exception();
				else if (files.length == 1) {
					imageName = files[0].getName();
					imageName = imageName.substring(0, imageName.length() - 4);
				}
			}
		} catch (IOException ignore) {
			Logger.LogString("exception - ignore and keep going with default string");
		}
		newImageText.setText(imageName);		
		return imageName;
	}
	
	public boolean isSerialNumberFormatGood (String srn) {
		boolean isGood;
		if (srn == null)
			isGood = false;
		else if (srn.toLowerCase().startsWith(TBLoader.srnPrefix.toLowerCase()) && srn.length()==10)
			isGood = true;
		else  {
			isGood = false;
			Logger.LogString("***Incorrect Serial Number Format:"+srn+"***");
		}
		return isGood;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		DriveInfo di;
		Object o = e.getSource();
		JButton b;
		if (monitoringDrive || !startUpDone)
			return;
		if (o instanceof JButton) {
				b = (JButton)o;
				if (b==update && (dateRotation == null || currentLocationList.getSelectedIndex()==0)) {
					JOptionPane.showMessageDialog(null, "You must first select a rotation date and a location.",
			                "Need Date and Location!", JOptionPane.DEFAULT_OPTION);
					return;
				}
			}
		else if (o instanceof JComboBox) {
			if (o == driveList) {
				di = (DriveInfo)((JComboBox)e.getSource()).getSelectedItem();
				TBLoader.currentDrive = di;
				if (di != null) {
					Logger.LogString("Drive changed: " + di.drive + di.label);
					try {
						fillCommunityList();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					setSNandRevFromCurrentDrive();
					getRevisionNumbers();
				}	
				oldID.setText("");
				newID.setText("");
			} else if (o == newCommunityList) {
				JComboBox cl = (JComboBox)o;
				try {
					if (cl.getSelectedItem() != null) {
						getImageFromCommunity(cl.getSelectedItem().toString());
					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} else if (o == newDeploymentList) {
				getRevisionNumbers();
				refreshUI();
			}
			return;
		} else
			return;
		
		disableAll();
		try {
			Logger.LogString("ACTION: " + b.getText());
			di = TBLoader.currentDrive;
			File drive = di.drive;
			if (drive == null) { 
				refreshUI();
				return;
			}
			if (oldCommunityText.getText().trim().length()==0)
				oldCommunityText.setText("UNKNOWN");
			if (oldDeploymentText.getText().trim().length()==0)
				oldDeploymentText.setText("UNKNOWN");
			Logger.LogString("ID:"+((DriveInfo)driveList.getSelectedItem()).serialNumber);
			setSNandRevFromCurrentDrive();
			status.setText("STATUS: Starting\n");
//			if (b == xfer) {
//				xferFiles();
//				//Logger.init();
//				return;
//			} 
			if (TBLoader.currentDrive != null) {
				prevSelected = drive;
			}
			
			String devicePath = drive.getAbsolutePath();
			String community = newCommunityList.getSelectedItem().toString();
			
			Logger.LogString("Community: " + community);
//			if (b == update && di.serialNumber==NO_SERIAL_NUMBER) { //fetchIDFromServer.isSelected()) {
//				di.serialNumber = fetchNextDeviceID();
//				di.isNewSerialNumber = true;
//				this.id.setText(di.serialNumber);
//				Logger.LogString("SN: " + di.serialNumber);
//			} 
			if ((b == update /*|| b == setCommunity*/) && newCommunityList.getSelectedIndex() == 0) {
				int response = JOptionPane.showConfirmDialog(this, "No community selected.\nAre you sure?", 
	                "Confirm", JOptionPane.YES_NO_OPTION);
				if (response != JOptionPane.YES_OPTION) {
					Logger.LogString("No community selected. Are you sure? NO");
					refreshUI();
					return;
				} else 
					Logger.LogString("No community selected. Are you sure? YES");
			} else
				prevSelectedCommunity = newCommunityList.getSelectedIndex();
//			if ( (b == update || b == setCommunity) && (communityList.getSelectedIndex() == 6 ||communityList.getSelectedIndex() == 8) && !handIcons.isSelected()) {
//				// warn if reformatting for Jonga or Gyanvuuri but not adding hand icon messages
//				int response = JOptionPane.showConfirmDialog(this, "Hand icons not selected for " + community + ". \nAre you sure?",
//	                "Confirm", JOptionPane.YES_NO_OPTION);
//				if (response != JOptionPane.YES_OPTION) {
//					Logger.LogString("Hand icons not selected for " + community + ". Are you sure? NO");
//					refreshUI();
//					return;
//				} else
//					Logger.LogString("Hand icons not selected for " + community + ". Are you sure? YES");
//			} else if ( (b == update || b == setCommunity) &&  (communityList.getSelectedIndex() != 6 && communityList.getSelectedIndex() != 8) && handIcons.isSelected()){
//				// warn if adding hand icon messages to arrow communities when either updating or reformatting 
//				int response = JOptionPane.showConfirmDialog(this, "Hand icons are selected for " + community + ". \nAre you sure?",
//		                "Confirm", JOptionPane.YES_NO_OPTION);
//				if (response != JOptionPane.YES_OPTION) {
//					Logger.LogString("Hand icons are selected for " + community + ". Are you sure? NO");
//					refreshUI();
//					return;
//				} else
//					Logger.LogString("Hand icons are selected for " + community + ". Are you sure? YES");
//			} else
//				prevSelectedCommunity = communityList.getSelectedIndex();
			if (b == grabStatsOnly) {
				updatingTB = true;
				disableAll();
				boolean goodDisk = true;//chkDsk(di);
				if (goodDisk) {
					//Logger.LogString("chkdsk was good.");
					CopyThread t;
					t = new CopyThread(this, devicePath, di.serialNumber, di.isNewSerialNumber,  "grabStatsOnly");
					t.start();
				} 				
			}
			else if (b == update) {
				updatingTB = true;
				disableAll();
				boolean goodDisk = true;//chkDsk(di);
				if (goodDisk) {
					//Logger.LogString("chkdsk was good.");
					CopyThread t;
					t = new CopyThread(this, devicePath, di.serialNumber, di.isNewSerialNumber,  "update");
					t.start();
				} 
/*				else {
					Logger.LogString("chkdsk was bad for " + di.label.toString());
					int response = JOptionPane.showConfirmDialog(null, "TB is corrupted.\nBackup takes 6-8 minutes.\nBegin?",
			              "Confirm", JOptionPane.OK_CANCEL_OPTION);
					if (response == JOptionPane.OK_OPTION) {
						Logger.LogString("TB is corrupted.  Backup takes 6-8 minutes. Begin?  YES");
						backup(devicePath, community, di.serialNumber, dateTime);
						CopyThread t = new CopyThread(this, devicePath, community, di.serialNumber, dateTime, handIcons.isSelected(), revision,di.revision,"corrupted");
						t.start();
					} else {
						Logger.LogString("TB is corrupted.  Backup takes 6-8 minutes. Begin?  NO");
					}
				}
*/			} 
/*			else if (b == backup) {
				int response = JOptionPane.showConfirmDialog(null, "Backup takes 6-8 minutes.\nBegin?",
			              "Confirm", JOptionPane.OK_CANCEL_OPTION);
				if (response == JOptionPane.OK_OPTION) {
					Logger.LogString("Backup takes 6-8 minutes. Begin?  YES");
					backup(devicePath, community, di.serialNumber, dateTime);
				} else
					Logger.LogString("Backup takes 6-8 minutes. Begin?  NO");
			} else if (b == reformat) {
					Logger.LogString("REFORMATTING");
					CopyThread t = new CopyThread(this, devicePath, community, di.serialNumber, dateTime, handIcons.isSelected(), revision,di.revision,"compare-basic");//"reformat");
					t.start();
			} 
*/
//			else if (b == setCommunity) {
//				//setCommunity(devicePath, community, dateTime);
//				CopyThread t;
//				t = new CopyThread(this, devicePath, di.serialNumber, di.isNewSerialNumber, dateTime, "setCommunity");
//				t.start();
//			}
			refreshUI();
			return;
		} catch (Exception ex) {
			Logger.LogString(ex.toString());
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "An error occured.",
	                "Error", JOptionPane.ERROR_MESSAGE);
			filldeploymentList();
			resetUI(false);
		} 
	}
/*
	private void backup(String devicePath, String community, String serialNumber, String dateTime) {
		String imagePathForMkDir, imagePathForCmdLine, imageFile;
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
*/
	private void resetUI(boolean resetDrives) {
		Logger.LogString("Resetting UI");
		oldID.setText("");
		newID.setText("");
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
		boolean connected;
		disableAll();
		checkDirUpdate();
		
		update.setText("Update TB");
//		reformat.setText("Reformat");
//		backup.setText("Backup");
//		setCommunity.setText("Set Community");
		//handIcons.setEnabled(false);
		//fetchIDFromServer.setEnabled(false);
		connected = driveConnected();
		if (connected && !updatingTB) {
			update.setEnabled(true);
			grabStatsOnly.setEnabled(true);
//			reformat.setEnabled(true);
//			backup.setEnabled(true);
//			xfer.setEnabled(true);
			//setCommunity.setEnabled(true);
//			xfer.setEnabled((new File(TEMP_COLLECTION_DIR)).exists());			
			status.setText("STATUS: Ready");
			status2.setText(status2.getText() + "\n\n");
			Logger.LogString("STATUS: Ready");
		} else {
			update.setEnabled(false);
			grabStatsOnly.setEnabled(false);
//			setCommunity.setEnabled(false);
//			xfer.setEnabled(false);
			if (!connected) {
        		oldDeploymentText.setText("");
        		oldCommunityText.setText("");
        		oldRevisionText.setText("");
        		oldImageText.setText("");
        		newID.setText("");
				oldID.setText("");
				lastUpdatedText.setText("");
				Logger.LogString("STATUS: " + NO_DRIVE);
				status.setText("STATUS: " + NO_DRIVE);
			}
			try {
				if (newCommunityList.getSelectedItem() != null) {
					getImageFromCommunity(newCommunityList.getSelectedItem().toString());
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			reformat.setEnabled(false);
//			backup.setEnabled(false);
		}
		Logger.flush();
	}

	private void disableAll() {
		// This is used after startup and when there have been no changes to the drives 
		update.setEnabled(false);
//		reformat.setEnabled(false);
//		backup.setEnabled(false);
//		xfer.setEnabled(false);	
//		setCommunity.setEnabled(false);
		//handIcons.setEnabled(false);
		//fetchIDFromServer.setEnabled(false);
		
	}
	
	void finishCopy(boolean success, final String idString, final String mode, final String endMsg, final String endTitle) {
		final TBLoader parent = this;
		updatingTB = false;
		parent.resetUI(true);
		Logger.LogString(endMsg);
		JOptionPane.showMessageDialog(null, endMsg, endTitle, JOptionPane.DEFAULT_OPTION);

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
				close();
				open();
				try {
					bw.write(getDateTime() + ": " + s+"\r\n");
				} catch (IOException ee) {
					e.printStackTrace();
					ee.printStackTrace();					
				}
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

		public static void flush() {
			try {
				if (bw != null)
					bw.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public static void init () {
			try {
				close();
				open();
				LogString("DEVICE:" + TBLoader.deviceID);
				LogString("JAVA VERSION:" + System.getProperty("java.version"));
				LogString("OPERATING SYSTEM:" + System.getProperty("os.name") +" v" + System.getProperty("os.version") + " - " + System.getProperty("os.arch"));
				LogString("TB LOADER VERSION:" + VERSION);
				LogString("IMAGE REVISION:"+imageRevision);
				LogString("COMPUTERNAME:"+System.getenv("COMPUTERNAME"));
				LogString("USERNAME:" + System.getenv("USERNAME"));
				LogString("APP PATH: " + new File(".").getAbsolutePath());
				LogString("LB HOMEPATH:" + TEMP_COLLECTION_DIR);
				LogString("DROPBOX PATH = "+copyTo);
				flush();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class CopyThread extends Thread {
		final String devicePath;
		final String id;
		final boolean isNewSerialNumber;
		//final String datetime;
		final TBLoader callback;
		final String mode;
		boolean criticalError = false;
		boolean alert = false;
		boolean success = false;
		long startTime;
		

		private void setStartTime() {
			startTime = System.nanoTime();
		}
		
		private String getDuration() {
			String elapsedTime;
			double durationSeconds;
			int durationMinutes;
			long durationNanoseconds = System.nanoTime() - startTime;
			durationSeconds = (double)durationNanoseconds  / 1000000000.0;
			TBLoader.durationSeconds = (int)durationSeconds;
			if (durationSeconds > 60) {
				durationMinutes = (int)durationSeconds / 60;
				durationSeconds -= durationMinutes *60;
				elapsedTime = new String(Integer.toString(durationMinutes) + " minutes " + Integer.toString((int) durationSeconds) + " seconds");
			} else
				elapsedTime = new String(Integer.toString((int) durationSeconds) + " seconds");
			return elapsedTime;
		}
		
		private boolean executeFile(File file) { 
			boolean success = true;
			String errorLine = "";
			criticalError = false;
			Calendar cal = Calendar.getInstance();
			String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
			String dateInMonth = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
			String year = String.valueOf(cal.get(Calendar.YEAR));
			
			try {
				BufferedReader reader;
				//if (this.useHandIcons)
				//	handValue = "1";
				//else
				//	handValue = "0";
				reader = new BufferedReader(new FileReader(file));
				while (reader.ready() && !criticalError) {
					String cmd = reader.readLine();
					if (cmd.startsWith("rem ")) {
						status.setText("STATUS: " + cmd.substring(4));
						Logger.LogString(cmd.substring(4));
						continue;
					}
					cmd = cmd.replaceAll("\\$\\{device_drive\\}", devicePath.substring(0, 2));
					cmd = cmd.replaceAll("\\$\\{srn\\}", id);
					cmd = cmd.replaceAll("\\$\\{new_srn\\}", newID.getText());
					cmd = cmd.replaceAll("\\$\\{device_id\\}", TBLoader.deviceID);  // this is the computer/tablet/phone id
					cmd = cmd.replaceAll("\\$\\{datetime\\}", TBLoader.currentDrive.datetime);
					cmd = cmd.replaceAll("\\$\\{syncdir\\}", TBLoader.currentDrive.datetime + "-" + TBLoader.deviceID);
					cmd = cmd.replaceAll("\\$\\{dateInMonth\\}", dateInMonth);
					cmd = cmd.replaceAll("\\$\\{month\\}", month);					
					cmd = cmd.replaceAll("\\$\\{year\\}", year);					
					cmd = cmd.replaceAll("\\$\\{send_now_dir\\}", Matcher.quoteReplacement(copyTo));
					cmd = cmd.replaceAll("\\$\\{new_revision\\}", TBLoader.newRevisionText.getText());
					cmd = cmd.replaceAll("\\$\\{old_revision\\}", TBLoader.oldRevisionText.getText());
					cmd = cmd.replaceAll("\\$\\{new_deployment\\}", TBLoader.newDeploymentList.getSelectedItem().toString());
					cmd = cmd.replaceAll("\\$\\{old_deployment\\}", TBLoader.oldDeploymentText.getText());
					cmd = cmd.replaceAll("\\$\\{new_community\\}", TBLoader.newCommunityList.getSelectedItem().toString());
					cmd = cmd.replaceAll("\\$\\{old_community\\}", TBLoader.oldCommunityText.getText());
					cmd = cmd.replaceAll("\\$\\{new_image\\}", TBLoader.newImageText.getText());
					cmd = cmd.replaceAll("\\$\\{old_image\\}", TBLoader.oldImageText.getText());
					cmd = cmd.replaceAll("\\$\\{isNewSRN\\}", (id.equals(NO_SERIAL_NUMBER)? "1" : "0"));
					cmd = cmd.replaceAll("\\$\\{volumeSRN\\}", volumeSerialNumber);
					//cmd = cmd.replaceAll("\\$\\{holding_dir\\}", Matcher.quoteReplacement(TEMP_COLLECTION_DIR));
					//cmd = cmd.replaceAll("\\$\\{hand\\}", handValue);
					alert = cmd.startsWith("!");
					if (alert)
						cmd = cmd.substring(1);
					errorLine = execute("cmd /C " + cmd);
					if (errorLine != null && alert) {
						if (!errorLine.equalsIgnoreCase("TB not found.  Unplug/replug USB and try again.") && !errorLine.equalsIgnoreCase("File system corrupted")) {			
							JOptionPane.showMessageDialog(null, errorLine,
									"Error", JOptionPane.ERROR_MESSAGE);
						}
						criticalError = true;
						success = false;
						break;
					}
				}
				reader.close();
			} catch (Exception e) {
				Logger.LogString(e.toString());
				Logger.flush();
				e.printStackTrace();
			}
			return success;
		}
		
		public CopyThread(TBLoader callback, String devicePath, String id, boolean isNewSerialNumber, String mode) {
			this.callback = callback;
			this.devicePath = devicePath;
			this.id = id;
			this.isNewSerialNumber = isNewSerialNumber;
			//this.datetime = datetime;
			// this.useHandIcons = useHandIcons;
			this.mode = mode;
		}		

		private void grabStatsOnly() {
			String endMsg ="";
			String endTitle ="";
			try {
				boolean gotStats,hasCorruption,goodCard;
				setStartTime();
				success = false;
				goodCard = executeFile(new File(SCRIPT_SUBDIR + "checkConnection.txt"));
				if (!goodCard) {
					return;
				}
				TBLoader.status2.setText("Checking Memory Card");
				Logger.LogString("STATUS:Checking Memory Card");
				hasCorruption = !executeFile(new File(SCRIPT_SUBDIR + "chkdsk.txt"));
				if (hasCorruption) {
					TBLoader.currentDrive.corrupted = true;
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Corrupted\nGetting Stats");
					Logger.LogString("STATUS:Corrupted...Getting Stats");
					executeFile(new File(SCRIPT_SUBDIR + "chkdsk-save.txt"));
				} else {
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Good\nGetting Stats");
					Logger.LogString("STATUS:Good Card\nGetting Stats");
				}
				gotStats = executeFile(new File(SCRIPT_SUBDIR + "grab.txt"));
				callback.logTBData("stats-only");
				if (gotStats) {
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Got Stats\nErasing Flash Stats");
					Logger.LogString("STATUS:Got Stats!\nErasing Flash Stats");
					executeFile(new File(SCRIPT_SUBDIR + "eraseFlashStats.txt"));
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Erased Flash Stats\nDisconnecting");
					Logger.LogString("STATUS:Erased Flash Stats");
					Logger.LogString("STATUS:Disconnecting TB");
					executeFile(new File(SCRIPT_SUBDIR + "disconnect.txt"));
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Complete");
					Logger.LogString("STATUS:Complete");
					success = true;
					endMsg = new String("Got Stats!");
					endTitle = new String("Success");
				}
				else {
					TBLoader.status2.setText(TBLoader.status2.getText() + "...No Stats!\n");
					Logger.LogString("STATUS:No Stats!");
					endMsg = new String("Could not get stats for some reason.");
					endTitle = new String("Failure");					
				}
			} finally {
				callback.finishCopy(success, id, this.mode, endMsg, endTitle);
			}
		}
		
		private void update() {
			String endMsg = "";
			String endTitle = "";
			
			try {
				boolean gotStats,hasCorruption,verified, goodCard;
				setStartTime();
				success = false;
				goodCard = executeFile(new File(SCRIPT_SUBDIR + "checkConnection.txt"));
				if (!goodCard) {
					return;
				}
				TBLoader.status2.setText("Checking Memory Card");
				Logger.LogString("STATUS:Checking Memory Card");
				hasCorruption = !executeFile(new File(SCRIPT_SUBDIR + "chkdsk.txt"));
				if (hasCorruption) {
					TBLoader.currentDrive.corrupted = true;
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Corrupted\nGetting Stats");
					Logger.LogString("STATUS:Corrupted...Getting Stats\n");
					executeFile(new File(SCRIPT_SUBDIR + "chkdsk-save.txt"));
				} else {
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Good\nGetting Stats");
					Logger.LogString("STATUS:Good Card...Getting Stats\n");
				}
				gotStats = executeFile(new File(SCRIPT_SUBDIR + "grab.txt"));
				if (gotStats) {
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Got Stats\n");
					Logger.LogString("STATUS:Got Stats\n");
				}
				else {
					TBLoader.status2.setText(TBLoader.status2.getText() + "...No Stats!\n");
					Logger.LogString("STATUS:No Stats!\n");
				}
				if (hasCorruption) {
					TBLoader.status2.setText(TBLoader.status2.getText() + "Reformatting");
					Logger.LogString("STATUS:Reformatting");
					goodCard = executeFile(new File(SCRIPT_SUBDIR + "reformat.txt"));
					if (!goodCard) {
						TBLoader.status2.setText(TBLoader.status2.getText() + "...Failed\n");
						Logger.LogString("STATUS:Reformat Failed");
						Logger.LogString("Could not reformat memory card.\nMake sure you have a good USB connection\nand that the Talking Book is powered with batteries, then try again.\n\nIf you still cannot reformat, replace the memory card.");
						JOptionPane.showMessageDialog(null, "Could not reformat memory card.\nMake sure you have a good USB connection\nand that the Talking Book is powered with batteries, then try again.\n\nIf you still cannot reformat, replace the memory card.",
				                "Failure!", JOptionPane.ERROR_MESSAGE);
						return;
					} else { 
						TBLoader.status2.setText(TBLoader.status2.getText() + "...Good\n");
						Logger.LogString("STATUS:Format was good");
					}
				} else {
					if (!newID.getText().equalsIgnoreCase(TBLoader.currentDrive.getLabelWithoutDriveLetter())) {
						Logger.LogString("STATUS:Relabeling volume");
						TBLoader.status2.setText(TBLoader.status2.getText() + "Relabeling\n");
						executeFile(new File(SCRIPT_SUBDIR + "relabel.txt"));
					}
				}
				TBLoader.status2.setText(TBLoader.status2.getText() + "Updating TB Files");
				Logger.LogString("STATUS:Updating TB Files");
				executeFile(new File(SCRIPT_SUBDIR + "update.txt"));
				TBLoader.status2.setText(TBLoader.status2.getText() + "...Updated\n");
				Logger.LogString("STATUS:Updated");
				//verified = executeFile(new File(SCRIPT_SUBDIR + "verify.txt"));
				//if (verified) {
				//	TBLoader.status2.setText(TBLoader.status2.getText() + "Verified Basic\nAdding Community Content\n");
				//	Logger.LogString("STATUS:Verified Basic...Adding Any Custom Community Content");
				//	verified = executeFile(new File(SCRIPT_SUBDIR + "customCommunity.txt"));					
				//}
				TBLoader.status2.setText(TBLoader.status2.getText() + "\nAdding Community Content\n");
				Logger.LogString("STATUS:Adding Any Custom Community Content");
				verified = executeFile(new File(SCRIPT_SUBDIR + "customCommunity.txt"));					
				if (verified) {
					String duration;
					TBLoader.status2.setText(TBLoader.status2.getText() + "Updated & Verified\nDisconnecting TB");
					Logger.LogString("STATUS:Updated & Verified...Disconnecting TB");
					executeFile(new File(SCRIPT_SUBDIR + "disconnect.txt"));
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Complete");
					Logger.LogString("STATUS:Complete");
					success = true;
					duration = getDuration();
					callback.logTBData("update");
					endMsg = new String("Talking Book has been updated and verified\nin " + duration + ".");
					endTitle = new String("Success");
				} else {
					String duration;
					duration = getDuration();
					callback.logTBData("update-failed verification");
					success = false;
					TBLoader.status2.setText(TBLoader.status2.getText() + "...Failed Verification in " + duration + "\n");
					Logger.LogString("STATUS:Failed Verification");
					endMsg = new String("Update failed verification.  Try again or replace memory card.");
					endTitle = new String("Failure");
				}
				for (int i=1;i<=(success?3:6);i++)
					Toolkit.getDefaultToolkit().beep();
			} catch (Exception e) {
				if (alert) {
					JOptionPane.showMessageDialog(null, e.getMessage(),
		                "Error", JOptionPane.ERROR_MESSAGE);
					criticalError = true;
					Logger.LogString("CRITICAL ERROR:" + e.getMessage());
				} else
					Logger.LogString("NON-CRITICAL ERROR:" + e.getMessage());
				Logger.flush();
				e.printStackTrace();
			} finally {
				callback.finishCopy(success, id, this.mode,endMsg,endTitle);
			}
			
		}
		
//		private void setCommunity() {
//			try {
//				boolean hasCorruption,goodCard;
//				success = false;
//				Logger.LogString("Setting Community -- first checking connectin and memory card.");
//				goodCard = executeFile(new File(SCRIPT_SUBDIR + "checkConnection.txt"));
//				if (!goodCard) {
//					return;
//				}
//				TBLoader.status2.setText("Checking Memory Card");
//				Logger.LogString("STATUS:Checking Memory Card");
//				hasCorruption = !executeFile(new File(SCRIPT_SUBDIR + "chkdsk.txt"));
//				if (hasCorruption) {
//					((DriveInfo)driveList.getSelectedItem()).corrupted = true;
//					Logger.LogString("Could not set community due to memory card corruption.  Run 'Update' to attempt to fix.");
//					TBLoader.status2.setText(TBLoader.status2.getText() + "...Corrupted");
//					Logger.LogString("STATUS:Corrupted");
//					JOptionPane.showMessageDialog(null, "Could not set community due to memory card corruption.  Run 'Update' to attempt to fix.",
//			                "Failure", JOptionPane.DEFAULT_OPTION);
//					return;
//				}
//				TBLoader.status2.setText(TBLoader.status2.getText() + "...Good\nSetting Community");
//				Logger.LogString("STATUS:Memory Card Good...Setting Community");
//				TBLoader.setCommunity(devicePath + "/system/", TBLoader.newCommunityList.getSelectedItem().toString(), this.datetime);
//				TBLoader.status2.setText(TBLoader.status2.getText() + "...Set\n");
//				Logger.LogString("STATUS:Community Set");
//				success = executeFile(new File(SCRIPT_SUBDIR + "customCommunity.txt"));
//				callback.logTBData("set community");
//				if (success) {
//					TBLoader.status2.setText(TBLoader.status2.getText() + "Custom Files Applied\n");
//					Logger.LogString("STATUS:Custom Files Applied");
//					executeFile(new File(SCRIPT_SUBDIR + "disconnect.txt"));
//					TBLoader.status2.setText(TBLoader.status2.getText() + "Complete\n");
//					Logger.LogString("STATUS:Complete");
//					Logger.LogString("Community set to " + TBLoader.newCommunityList.getSelectedItem().toString() + " and custom files applied.");
//					JOptionPane.showMessageDialog(null, "Community set to " + TBLoader.newCommunityList.getSelectedItem().toString() + ".",
//			                "Success", JOptionPane.DEFAULT_OPTION);
//				}
//			for (int i=1;i<=3;i++)
//					Toolkit.getDefaultToolkit().beep();
//			} catch (Exception e) {
//				if (alert) {
//					JOptionPane.showMessageDialog(null, e.getMessage(),
//		                "Error", JOptionPane.ERROR_MESSAGE);
//					Logger.LogString("CRITICAL ERROR:" + e.getMessage());
//					criticalError = true;
//				}
//				Logger.LogString(e.toString());
//				Logger.flush();
//				e.printStackTrace();
//			} finally {
//				callback.finishCopy(success, id, this.mode);
//			}
//		}
		
		@Override public void run() {
			if (this.mode == "update")
				update();			
			else if (this.mode == "grabStatsOnly")
				grabStatsOnly();
//			else if (this.mode == "setCommunity")
//				setCommunity();			
		}
	}

	/*	static boolean chkDsk(DriveInfo di) throws Exception {
		String line;
		String folders[] = {"system","languages","messages","statistics","log","log-archive","messages/lists","messages/audio","languages/dga","statistics/stats","statistics/ostats"};
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
*/
	private static String dosErrorCheck(String line) {
		String errorMsg = null;
		
		if (line.contains("New")) {
			//  file copy validation failed (some files missing on target) 
			errorMsg = line;//.substring(line.length()-30);
		} else if (line.contains("Invalid media or Track 0 bad - disk unusable")) {
			// formatting error
			errorMsg = "Bad memory card.  Please discard and replace it.";
		} else if (line.contains("Specified drive does not exist.") || line.startsWith("The volume does not contain a recognized file system.")) {
			errorMsg = "Either bad memory card or USB connection problem.  Try again.";
		} else if (line.contains("Windows found problems with the file system") /* || line.startsWith("File Not Found") */ || line.startsWith("The system cannot find the file")) {
			// checkdisk shows corruption
			errorMsg = "File system corrupted";
		} else if (line.startsWith("The system cannot find the path specified.")) {
			errorMsg = "TB not found.  Unplug/replug USB and try again.";
		} else if (line.startsWith("Volume Serial Number is ", 0)) {
			volumeSerialNumber = TBLoader.srnPrefix + line.substring(24,28) + line.substring(29,33);
			if (newID.getText().equals(NO_SERIAL_NUMBER)) {
				newID.setText(volumeSerialNumber);
				Logger.LogString("TB Serial Number will be set to " + volumeSerialNumber);
			}
		} else if (line.startsWith("Volume Serial Number is ", 1)) {
			volumeSerialNumber = TBLoader.srnPrefix + line.substring(25,29) + line.substring(30,34);			
			if (newID.getText().equals(NO_SERIAL_NUMBER)) {
				newID.setText(volumeSerialNumber);
				Logger.LogString("TB Serial Number will be set to " + volumeSerialNumber);
			}
		}
		return errorMsg;
	}
	//Log:ID=TB000336
	//Log:REFORMAT
	//Log:Formatting Talking Book
	//Log:Executing:cmd /C format F: /FS:FAT32 /v:TB000336 /Y /Q
	//Log:Cannot open volume for direct access.
	
	//Log:  Options : *.* /S /E /COPY:DAT /PURGE /MIR /NP /R:1000000 /W:30 
	//Log:------------------------------------------------------------------------------
	//Log:2013/07/11 23:40:38 ERROR 2 (0x00000002) Creating Destination Directory F:\
	//Log:The system cannot find the file specified.

	static String execute(String cmd) throws Exception {
		String line;
		String errorLine = null;
		Logger.LogString("Executing:" + cmd);
		Process proc = Runtime.getRuntime().exec(cmd);

		BufferedReader br1 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		BufferedReader br2 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
		
		do {
			line = br1.readLine();
			Logger.LogString(line);
			if (line!=null && errorLine == null)
				errorLine = dosErrorCheck(line);				
		} while (line != null);
		
		
		do {
			line = br2.readLine();
			Logger.LogString(line);
			if (line!=null && errorLine == null)
				errorLine = dosErrorCheck(line);
		} while (line != null);
		
		proc.waitFor();
		return errorLine;
	}
	
	private static class DriveInfo {
		final File drive;
		String label;
		String serialNumber;
		//String volumeSerialNumber;
		boolean isNewSerialNumber;
		boolean corrupted;
		String datetime="";
		
		public DriveInfo(File drive, String label) {
			this.drive = drive;
			this.label = label.trim();
			this.corrupted = false;
			this.serialNumber = "";
			//this.volumeSerialNumber =""; 
			this.isNewSerialNumber = false;
			this.datetime = getDateTime();
			//updateLastIssue();
		}
		
		public String getLabelWithoutDriveLetter() {
			String label = this.label.substring(0, this.label.lastIndexOf('(')-1);
			return label;
		}
		
/*		public boolean updateLastIssue() {
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
*/		
		
		@Override
		public String toString() {
			if (label.isEmpty()) {
				return drive.toString();
			}
			return label;
		}
	}
	
	private static class TBInfo {
		static final int MAX_MESSAGES=40;
		// struct SystemData
		// int structType
		boolean debug = false;
		String serialNumber;
		String deploymentNumber;
		short countReflashes;
		String location;
		String imageName;
		short updateDate = -1;
		short updateMonth = -1;
		short updateYear = -1;
		
		// struct SystemCounts2
		// short structType
		short periods;
		short cumulativeDays;
		short corruptionDay;
		short powerups;
		short lastInitVoltage;
		RotationTiming[] rotations = new RotationTiming[5];
		
		// struct NORmsgMap
		// short structType
		short totalMessages;
		String[] msgIdMap = new String[MAX_MESSAGES]; // 40 messages, 20 chars
		
		//struct NORallMsgStats
		short profileOrder;
		String profileName;
		short profileTotalMessages;
		short profileTotalRotations;
		NORmsgStats[][] stats = new NORmsgStats[MAX_MESSAGES][5];
		RandomAccessFile f;
		
		private class RotationTiming {
			short rotationNumber;
			short startingPeriod;
			short hoursAfterLastUpdate;
			short initVoltage;
			
			public RotationTiming() throws IOException {
				f.skipBytes(2);
				//System.out.println("pointer:"+f.getFilePointer());
				this.rotationNumber = readShort();
				this.startingPeriod = readShort();
				this.hoursAfterLastUpdate = readShort();
				this.initVoltage = readShort();
				//System.out.println("pointer:"+f.getFilePointer());
			}
		}
		
		private class NORmsgStats {
			// short structType
			short indexMsg;
			short numberRotation;
			short numberProfile;
			short countStarted;
			short countQuarter;
			short countHalf;
			short countThreequarters;
			short countCompleted;
			short countApplied;
			short countUseless;
			int totalSecondsPlayed;
			
			public NORmsgStats() throws IOException {
				f.skipBytes(2);
				this.indexMsg = readShort();
				this.numberProfile = readShort();
				this.numberRotation = readShort();
				this.countStarted = readShort();
				this.countQuarter = readShort();
				this.countHalf = readShort();
				this.countThreequarters = readShort();
				this.countCompleted = readShort();
				this.countApplied = readShort();
				this.countUseless = readShort();
				this.totalSecondsPlayed = readUnsignedShort();
			}
		}

		public TBInfo(String flashDataPath) throws IOException {
			File file = new File(flashDataPath);
			if (!file.exists()) {
				System.out.print("No flash binary file to analyze.");
				this.countReflashes = -1;
				return;
			}
			f = new RandomAccessFile(flashDataPath,"r");
			f.skipBytes(2);
			this.countReflashes = readShort();
			this.serialNumber = readString(12);
			this.deploymentNumber = readString(20);
			this.location = readString(40);
			this.imageName = readString(20);
			this.updateDate = readShort();
			this.updateMonth = readShort();
			this.updateYear = readShort();
			
			f.skipBytes(2);
			this.periods = readShort();
			this.cumulativeDays = readShort();
			this.corruptionDay = readShort();
			this.powerups = readShort();
			this.lastInitVoltage = readShort();
			for (int i=0;i < 5;i++) {
				if (debug) {
					System.out.println("i:"+i);
					System.out.println("pointer:"+f.getFilePointer());
				}
				rotations[i] = new RotationTiming();
			}
			
			f.skipBytes(2);
			this.totalMessages = readShort();
			for (int i=0; i < 40; i++) {
				if (i < this.totalMessages)
					this.msgIdMap[i] = readString(20);
				else
					f.skipBytes(40);
			}
			
			f.skipBytes(2);
			this.profileOrder = readShort();
			this.profileName = readString(20);
			this.profileTotalMessages = readShort();
			if (debug)
				System.out.print("About to read totalrotations:");
			this.profileTotalRotations = readShort();
			for (int m=0; m < this.totalMessages; m++) {
				for (int r=0; r < 5; r++) {
					if (debug)
						System.out.println("msg:"+m+" rot:"+r+" at "+f.getFilePointer());
					this.stats[m][r] = new NORmsgStats();
				}
			}
			f.close();
			Logger.LogString("FOUND FLASH STATS\n" + this.toString());
		}

		short readShort() throws IOException {
			 long sum = 0;
			 int b, i;
			 short ret;

			 for (int l= 0; l<2; l++) {
				 b = f.readByte() & 0xFF; // remove sign
				 //System.out.print("          b:"+b);
				 i = b << (8 * l);
				 sum += (0xFFFF & i);
			 }
			 ret = (short) sum;
			 if (debug)
				 System.out.println("       readShort (" + sum + ") at " + f.getFilePointer() + ": "+ ret);
			 return ret;
		}
		
		int readUnsignedShort() throws IOException {
			 long sum = 0;
			 int b, i;
			 int ret;

			 for (int l= 0; l<2; l++) {
				 b = f.readByte() & 0xFF; // remove sign
				 //System.out.print("          b:"+b);
				 i = b << (8 * l);
				 sum += (0xFFFF & i);
			 }
			 ret = (int) sum;
			 if (debug)
				 System.out.println("       readShort (" + sum + ") at " + f.getFilePointer() + ": "+ ret);
			 return ret;
		}

		String readString(int maxChars) throws IOException {
			char[] c = new char[maxChars];
			boolean endString = false;
			long start = f.getFilePointer();
			for (int i=0; i < maxChars; i++) {
				c[i] = (char)f.readByte();
				if (endString)
					c[i] = 0;
				else if (c[i] == 0)
					endString = true;
				f.readByte();
			}			
			if (debug)
				System.out.println("     string:" + String.valueOf(c) + " at " + start);
			return new String(c).trim();
		}		
		
		public long totalPlayedSecondsPerMsg (int msg) {
			long totalSec = 0;
			for (int r=0;r<5;r++) {
				totalSec += this.stats[msg][r].totalSecondsPlayed;
			}
			return totalSec;
		}
		
		public long totalPlayedSecondsPerRotation (int rotation) {
			long totalSec = 0;
			for (int m=0;m<this.totalMessages;m++) {
				totalSec += this.stats[m][rotation].totalSecondsPlayed;
			}
			return totalSec;
		}

		public String toString() {
			StringBuilder s = new StringBuilder();
			String NEW_LINE = System.getProperty("line.separator");
			
			s.append("Serial Number : " + this.serialNumber + NEW_LINE);
			s.append("Reflashes     : " + this.countReflashes  + NEW_LINE);
			s.append("Deployment    : " + this.deploymentNumber + NEW_LINE);
			s.append("Image         : " + this.imageName + NEW_LINE);
			s.append("Profile       : " + this.profileName + NEW_LINE);
			s.append("Location      : " + this.location + NEW_LINE);
			s.append("Last Updated  : " + this.updateYear + "/" + this.updateMonth + "/" + this.updateDate + NEW_LINE);
			s.append("Powered Days  : " + this.cumulativeDays + NEW_LINE);
			s.append("Last PowerupV : " + this.lastInitVoltage + NEW_LINE);
			s.append("StartUps      : " + this.powerups + NEW_LINE);
			s.append("Corruption Day: " + this.corruptionDay + NEW_LINE);
			s.append("Periods       : " + this.periods + NEW_LINE);
			s.append("Rotations     : " + this.profileTotalRotations + NEW_LINE);
			s.append(NEW_LINE);
			s.append("TOTAL STATS (" + this.totalMessages + " messages)" + NEW_LINE);
			int totalSecondsPlayed=0, countStarted=0,countQuarter=0,countHalf=0,countThreequarters=0,countCompleted=0,countApplied=0,countUseless=0;
			for (int m=0;m < this.totalMessages; m++) {
				for (int r=0;r < (this.profileTotalRotations<5?this.profileTotalRotations:5);r++) {
					totalSecondsPlayed += this.stats[m][r].totalSecondsPlayed;
					countStarted += this.stats[m][r].countStarted;
					countQuarter += this.stats[m][r].countQuarter;
					countHalf += this.stats[m][r].countHalf;
					countThreequarters += this.stats[m][r].countThreequarters;
					countCompleted += this.stats[m][r].countCompleted;
					countApplied += this.stats[m][r].countApplied;
					countUseless += this.stats[m][r].countUseless;
					}
			}
			s.append("       Time:" + totalSecondsPlayed/60 + "min " + totalSecondsPlayed%60 + "sec   Started:" + countStarted + "   P:" + countQuarter + 
					 "   H:"  + countHalf + "   M:" + countThreequarters + 
					 "   F:" + countCompleted);
			s.append("   A:" + countApplied + "   U:" + countUseless + NEW_LINE);
			s.append(NEW_LINE);	
			
			for (int r=0; r<(this.profileTotalRotations<5?this.profileTotalRotations:5); r++) {
				s.append("  Rotation:" + r + "     " + totalPlayedSecondsPerRotation(r)/60 + "min " + totalPlayedSecondsPerRotation(r)%60 + "sec    Starting Period:"
						+ this.rotations[r].startingPeriod + "   Hours After Update:" + 
						this.rotations[r].hoursAfterLastUpdate + "   Init Voltage:" + this.rotations[r].initVoltage + NEW_LINE);
			}
			s.append(NEW_LINE);
			s.append("Message Stats  (" + this.totalMessages + " messages)" + NEW_LINE);
			for (int m=0;m < this.totalMessages; m++) {
				s.append("  MESSAGE ID:" + this.msgIdMap[m] + " (" + totalPlayedSecondsPerMsg(m)/60 + "min "+ totalPlayedSecondsPerMsg(m)%60 + "sec)" + NEW_LINE);
				for (int r=0;r<(this.profileTotalRotations<5?this.profileTotalRotations:5); r++) {
					s.append("     ROTATION: " + r);
					s.append("       Time:" + this.stats[m][r].totalSecondsPlayed/60 + "min " + this.stats[m][r].totalSecondsPlayed%60 + "sec   Started:" + this.stats[m][r].countStarted + "   P:" + this.stats[m][r].countQuarter + 
							 "   H:"  + this.stats[m][r].countHalf + "   M:" + this.stats[m][r].countThreequarters + 
							 "   F:" + this.stats[m][r].countCompleted);
					s.append("   A:" + this.stats[m][r].countApplied + "   U:" + this.stats[m][r].countUseless + NEW_LINE);
				}
			}
			return s.toString();
		}
	}
}
