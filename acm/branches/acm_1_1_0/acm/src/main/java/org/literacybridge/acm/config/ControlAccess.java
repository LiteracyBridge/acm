package org.literacybridge.acm.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.repository.AudioItemRepository.AudioFormat;
import org.literacybridge.acm.repository.CachingRepository;
import org.literacybridge.acm.repository.FileSystemRepository;
import org.literacybridge.acm.utils.ZipUnzip;

public class ControlAccess {
	private static final Logger LOG = Logger.getLogger(ControlAccess.class.getName());
	private final static String DB_ZIP_FILENAME_PREFIX = Constants.DBHomeDir;
	private final static String DB_ZIP_FILENAME_INITIAL = new String (DB_ZIP_FILENAME_PREFIX + "1.zip");
	private final static String DB_DOES_NOT_EXIST = "NULL"; // PHP returns this if no checkin file found
	private final static String DB_KEY_OVERRIDE = new String("force");
    static boolean readOnly = false;
    private static boolean sandbox = false;
    private static String possessor;
    private static DBInfo dbInfo;
    
    private static void setDBKey(String key) {
    	dbInfo.setDbKey(key);
    }
    
    private static String getDBKey() {
    	return dbInfo.getDbKey();
    }

    private static void setPossessor(String name) {
    	possessor = name;
    }
    
    private static String getPosessor() {
    	return possessor;
    }

    private static boolean setZipFilenames(String currentFilename) {
    	boolean goodName = false;
    	String nextFilename = null;
    	
    	if (currentFilename == null) { 
    		goodName = false;
    		// no db zip exists in dropbox but the db has been checked in; no need for setNextZipFilename, because time to shutdown
    	} else if (currentFilename.equalsIgnoreCase(DB_DOES_NOT_EXIST)) {
    		// ACM does not yet exist, so create name for newly created zip to use on updateDB()
    		nextFilename = new String(DB_ZIP_FILENAME_INITIAL);
    		goodName = true;
    	} else {
	    	String baseFilename = currentFilename.substring(DB_ZIP_FILENAME_PREFIX.length(), currentFilename.lastIndexOf('.'));
	    	try {
		    	int count = Integer.parseInt(baseFilename) + 1;
		    	nextFilename = new String (DB_ZIP_FILENAME_PREFIX + String.valueOf(count) + currentFilename.substring(currentFilename.lastIndexOf('.')));
		    	goodName = true;
	    	} catch (NumberFormatException e) {
	    		// there's some strange .zip -- probably a "(conflicted copy)" or something else weird -- don't use it!
	    		currentFilename = null;
	    		goodName = false;
	    		LOG.log(Level.WARNING, "Unable to parse filename " + currentFilename);
	    	}
    	}
    	dbInfo.setFilenames(currentFilename,nextFilename);
    	return goodName;
    }

    private static String getCurrentZipFilename() {
    	return dbInfo.getCurrentFilename();
    }
    
    public static String getNextZipFilename() {
    	return dbInfo.getNextFilename();
    }
    
	public static File getSandboxDirectory() {
		File fSandbox = null; 
		if (isSandbox()) {
			fSandbox = new File(Configuration.getTempACMsDirectory(),Configuration.getSharedACMname() + "/" + Constants.RepositoryHomeDir);
		}
		return fSandbox;
	}

// Commenting out since getSandboxDirectory() now calculates path from Constants and other roots
//    private static void setSandboxDirectory(File f) {
//   	sandboxDirectory = f; 
//    }
    
	public static boolean isACMReadOnly() {
		return readOnly;
	}	

	private void setReadOnly(boolean isRW) {
		readOnly = isRW;
	}

	private static File getDBAccessFile() {
		return new File(Configuration.getSharedACMDirectory(), Constants.DB_ACCESS_FILENAME);
	}

	public static boolean isSandbox() {
		return sandbox;
	}
	
	private static void setSandbox(boolean isSandbox) {
		sandbox = isSandbox;
	}

	private static void deleteLocalDB() {
		try {
			// deleting old local DB so that next startup knows everything shutdown normally
			File oldDB = new File(Configuration.getTempACMsDirectory(),Configuration.getSharedACMname());
			FileUtils.deleteDirectory(oldDB);
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private static boolean isOnline() {
		boolean result = false;
		URLConnection connection = null;
		try {
			connection = new URL("http://literacybridge.org").openConnection();
			connection.connect();
			result = true;
		} catch (MalformedURLException e) {
			// this should not ever happen (if the URL above is good)
			e.printStackTrace();
		} catch (IOException e) {
			result = false;
			//e.printStackTrace();
		} 
		return result;
	}

	public static void init() {
		if (dbInfo == null)
			dbInfo = new DBInfo();
		if (dbInfo.isCheckedOut()) {
			setSandbox(false);
			if (!Configuration.isDisableUI()) {
				JOptionPane.showMessageDialog(null,"You have already checked out this ACM.\nYou can now continue making changes to it.");
			}				
		} else {
			deleteLocalDB();			
			determineRWStatus();
			createDBMirror();				
		}
		System.out.println("  Repository:                     " + Configuration.getRepositoryDirectory());
		System.out.println("  Temp Database:                  " + Configuration.getDatabaseDirectory());
		System.out.println("  Temp Repository (sandbox mode): " + getSandboxDirectory());
		System.out.println("  UserRWAccess:                   " + userHasWriteAccess());
		System.out.println("  online:                         " + isOnline());

		Configuration.setRepository(new CachingRepository(
				new FileSystemRepository(Configuration.getCacheDirectory(), 
						new FileSystemRepository.FileSystemGarbageCollector(Constants.CACHE_SIZE_IN_BYTES,
							new FilenameFilter() {
								@Override public boolean accept(File file, String name) {
									return name.toLowerCase().endsWith("." + AudioFormat.WAV.getFileExtension());
								}
							})),
				new FileSystemRepository(Configuration.getRepositoryDirectory()),
				isSandbox()?new FileSystemRepository(getSandboxDirectory()):null));
//		instance.repository.convert(audioItem, targetFormat);				
	}

	private static void createDBMirror() {
		// String line;
		File toDir = Configuration.getDatabaseDirectory();
		String dbFilename = getCurrentZipFilename();
		try {
			//File oldDB = new File (toDir,Configuration.getSharedACMname());
			//if (dbFilename.equalsIgnoreCase(ControlAccess.DB_DOES_NOT_EXIST)) {
			//	oldDB.mkdir(); // recreate db directory that was just deleted
			//	System.out.println("Created new empty DB dir");
			//} else {
				File dbZip = new File(Configuration.getSharedACMDirectory(),dbFilename);
				System.out.println("Started DB Mirror:"+ Calendar.getInstance().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND)+":"+Calendar.getInstance().get(Calendar.MILLISECOND));
				ZipUnzip.unzip(dbZip, toDir);
				//FileUtils.copyDirectoryToDirectory(fromDir, toDir);
				System.out.println("Completed DB Mirror:"+ Calendar.getInstance().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND)+":"+Calendar.getInstance().get(Calendar.MILLISECOND));
			//} 
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	private static boolean setCurrentFileToLastModified() {
		String filenameFallback = null;
		File[] files;
		long lastModified = 0;
		boolean foundOne = false;
		
		files = Configuration.getSharedACMDirectory().listFiles(new FilenameFilter() {
			@Override public boolean accept(File dir, String name) {
				String lowercase = name.toLowerCase();
				return lowercase.endsWith(".zip");
			}			
		});
		for (File file : files) {
			if (file.lastModified() > lastModified) {
				lastModified = file.lastModified();
				filenameFallback = file.getName();
				if (setZipFilenames(filenameFallback)) {
					foundOne = true;
				}
			}
		}
		return foundOne;
	}
	
	private static boolean haveLatestDB() {
		String filenameShouldHave = getCurrentZipFilename();
		
		if (filenameShouldHave.equalsIgnoreCase(ControlAccess.DB_DOES_NOT_EXIST))
			return true;  // if the ACM is new, you have the latest there is (nothing)
		
		File fileShouldHave = new File(Configuration.getSharedACMDirectory(),filenameShouldHave);
		boolean status;
		if (fileShouldHave.exists()) {
			status = true;
		} else {
			// latest db zip to be checked in is not yet in dropbox, so find most recent zip and force sandbox mode
			status = false;
			setCurrentFileToLastModified();
		}
		return status;
	}
	
	private static void determineRWStatus() {
		boolean sandboxMode = false;
		boolean forced = false;
		String dialogMessage = new String();
		boolean dbAvailable = false;
		boolean outdatedDB = false;
		boolean online;
		
		if (!isOnline()) {
			online = false;
			sandboxMode = true;
			if (setCurrentFileToLastModified()) {	
				if (!Configuration.isDisableUI()) {
					JOptionPane.showMessageDialog(null,"Cannot connect to Literacy Bridge server.");
				}
			} else {
				if (!Configuration.isDisableUI()) {
					JOptionPane.showMessageDialog(null,"Cannot connect to Literacy Bridge server and no available database. Shutting down.");
				}
				System.exit(0);
			}
		} else {
			online = true;
			int onlineChoice;
			do {
				onlineChoice = 1; // don't repeat loop unless user chooses to
				try {
					dbAvailable = checkOutDB(Configuration.getSharedACMname(), new String("statusCheck"));
					break;
				} catch (IOException e) {					
					dbAvailable = false;
					Object[] options = {"Try again", "Use Demo Mode"};
					if (!Configuration.isDisableUI()) {
						onlineChoice = JOptionPane.showOptionDialog(null, "Cannot reach Literacy Bridge web server.  Do you want to get online now and try again or use Demo Mode?", "Cannot Connect to Server",JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
					}
					if (onlineChoice == -1)
						System.exit(0);
				}
			} while (onlineChoice == 0);
			if (dbAvailable && !haveLatestDB()) {
				if (getCurrentZipFilename() == null) {
					// no zip exists at all -- must shut down
					if (!Configuration.isDisableUI()) {
						JOptionPane.showMessageDialog(null,"There is no copy of this ACM database on this computer.\nIt may be that the database has not been uploaded and downloaded yet.\nShutting down.");
					}
					System.exit(0);					
				} else {
					outdatedDB = true;
				}
			}
			if (!userHasWriteAccess() || !dbAvailable || outdatedDB) {
				sandboxMode = true; 
			} 
			if (outdatedDB) {
				dialogMessage = "The latest version of the ACM database has not yet downloaded to this computer.\nYou may shutdown and wait or begin demonstration mode with the previous version.";
			} else if (!dbAvailable && online) {
				dialogMessage = "Another user currently has write access to the ACM.\n";
				dialogMessage += getPosessor() + "\n";
			} 
			if (online && sandboxMode && userHasWriteAccess() && !Configuration.isDisableUI()) {
				Object[] optionsNoForce = {"Use Demo Mode", "Shutdown"};
				Object[] optionsForce = {"Use Demo Mode", "Shutdown", "Force Write Mode"};
	
				int n = JOptionPane.showOptionDialog(null, dialogMessage,"Cannot Get Write Access",JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE, null, (outdatedDB?optionsNoForce:optionsForce), optionsForce[0]);
				if (n == -1 || n == 1) { // user closed option pane or selected Shutdown
					JOptionPane.showMessageDialog(null,"Shutting down.");
					System.exit(0);
				} else if (n==2) {
					dialogMessage = "Forcing write mode will cause " + getPosessor() + "\nto lose their work!\n\n" +
							"If you are sure you want to force write access,\ntype the word 'force' below.";
					String confirmation = (String)JOptionPane.showInputDialog(null,dialogMessage);
					if (confirmation != null && confirmation.equalsIgnoreCase("force")) {
						sandboxMode = false;
						do {	
							onlineChoice = 1; // don't repeat loop unless user chooses to
							try {
								forced = dbAvailable = checkOutDB(Configuration.getSharedACMname(), DB_KEY_OVERRIDE);				
								sandboxMode = false;
								break;
							} catch (IOException e) {					
								forced = dbAvailable = false;
								sandboxMode = true;
								Object[] options = {"Try again", "Use Demo Mode"};
								onlineChoice = JOptionPane.showOptionDialog(null, "Cannot reach Literacy Bridge web server.  Do you want to get online now and try again or use Demo Mode?", "Cannot Connect to Server",JOptionPane.YES_NO_CANCEL_OPTION,
										JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
								if (onlineChoice == -1)
									System.exit(0);
							}
						} while (onlineChoice == 0);

						if (forced)
							JOptionPane.showMessageDialog(null,"You now have write access.");
						else
							JOptionPane.showMessageDialog(null,"Forcing did not work.");
					}
					else {
						sandboxMode = true;
					}
				}
			}
			if (!forced && !sandboxMode) {
				int n = 0;
				if (!Configuration.isDisableUI()) {
					if (getCurrentZipFilename().equalsIgnoreCase(DB_DOES_NOT_EXIST)) {
						JOptionPane.showMessageDialog(null,"ACM does not exist yet. Creating a new ACM and giving you write access.");					
					} else {
						Object[] options = {"Update Shared Database", "Use Demo Mode"};
						n = JOptionPane.showOptionDialog(null, "Do you want to update the shared database?","Update or Demo Mode?",JOptionPane.YES_NO_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
					}
				}
				if (n == -1) // user closed option pane
					System.exit(0);
				else if (n==1)
					sandboxMode = true;
				else {
					do {	
						onlineChoice = 1; // don't repeat loop unless user chooses to
						try {
							dbAvailable = checkOutDB(Configuration.getSharedACMname(),"checkout");
							sandboxMode = false;
							break;
						} catch (IOException e) {					
							dbAvailable = false;
							sandboxMode = true;
							if (!Configuration.isDisableUI()) {
								Object[] options = {"Try again", "Use Demo Mode"};
								onlineChoice = JOptionPane.showOptionDialog(null, "Cannot reach Literacy Bridge web server.  Do you want to get online now and try again or use Demo Mode?", "Cannot Connect to Server",JOptionPane.YES_NO_CANCEL_OPTION,
										JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
							}
							if (onlineChoice == -1)
								System.exit(0);
						}
					} while (onlineChoice == 0);


					if (!dbAvailable && !sandboxMode && !Configuration.isDisableUI()) {
						JOptionPane.showMessageDialog(null,"Sorry, but another user must have just checked out this ACM a moment ago!\nTry contacting " + getPosessor() + "\nAfter clicking OK, the ACM will shutdown.");
						System.exit(0);
					} else if (!sandboxMode) {
						if (getCurrentZipFilename().equalsIgnoreCase(DB_DOES_NOT_EXIST)) {
							setDBKey(DB_KEY_OVERRIDE);
						}
					}
				}
			}
		}
		setSandbox(sandboxMode);
		if (sandboxMode) {
			if (!Configuration.isDisableUI()) {
				JOptionPane.showMessageDialog(null,"The ACM is running in demonstration mode.\nPlease remember that your changes will not be saved.");
			}
		} 
	}
	
	public static boolean userHasWriteAccess() {
		String writeUser, thisUser;
		boolean userHasWriteAccess = false;
		
		thisUser = Configuration.getUserName();
		if (thisUser == null) {
			// No User Name found in config.properties.  Forcing Read-Only mode.
			return false;
		}
		File f = getDBAccessFile();
		if (f.exists()) {
			try {
				BufferedReader in = new BufferedReader (new FileReader(f));
				while ((writeUser = in.readLine()) != null) {
					if (writeUser.equalsIgnoreCase(thisUser)) {
						userHasWriteAccess = true;
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (Configuration.getSharedACMDirectory().list().length == 0) {
			// empty directory -- which means that a new directory was created to start an ACM in
			// Since the directory already exists, it is not the case that the user just hasn't accepted the dropbox invitaiton yet.
			// So, give this user write access to the newly created ACM
			userHasWriteAccess = true;
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter(f));
				out.write(thisUser + "\n");
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// Cannot find database access list. Forcing Read-Only mode. 
			return false;
		}
		return userHasWriteAccess;
	}

	private static boolean checkOutDB(String db, String action) throws IOException {
		boolean status = true;
		String filename = null, key = null, possessor = null;
		URL url;
		String computerName;

		try {
			computerName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e1) {
			computerName = "UNKNOWN";
		}
		

		url = new URL("http://literacybridge.org/checkout.php?db=" + db + "&action=" + action + "&name=" + Configuration.getUserName() + "&contact=" + Configuration.getUserContact()+ "&version=" + Constants.ACM_VERSION + "&computername=" + computerName);
		InputStream in;
		in = url.openStream();
		Scanner scanner = new Scanner(in);
	    //scanner.useDelimiter(END_OF_INPUT);
 	    while (scanner.hasNext()) {
 	    	String s = scanner.next();
 	    	if (s.startsWith("key="))
 	    		key = s.substring(s.indexOf('=')+1);
 	    	else if (s.startsWith("filename="))
 	    		filename = s.substring(s.indexOf('=')+1);
 	    	else if (s.startsWith("possessor=")) {
 	    		possessor = s.substring(s.indexOf('=')+1);
 	    		status = false;
 	    	} 	 	    	
 	    }
	    if (filename != null)
	    	setZipFilenames(filename);
 	    if (status) {
 	    	if (key != null) {
 	    		setDBKey(key);
 	    	    dbInfo.setCheckedOut(true);
 	    	}
 	    } else if (possessor != null) {
 	    	setPossessor(possessor);
 	    }
	    return status;
	}

	private static boolean checkInDB(String db, String key, String filename) throws IOException {
		boolean status = false;
		final String END_OF_INPUT = "\\Z";
		String s = null;
		String computerName;
		URL url;
		
		try {
			computerName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e1) {
			computerName = "UNKNOWN";
		}
		
//		try {
			url = new URL("http://literacybridge.org/checkin.php?db=" + db + "&key=" + key + "&filename=" + filename + "&name=" + Configuration.getUserName() + "&contact=" + Configuration.getUserContact()+ "&version=" + Constants.ACM_VERSION + "&computername=" + computerName);
			InputStream in;
			in = url.openStream();
			Scanner scanner = new Scanner(in);
		    scanner.useDelimiter(END_OF_INPUT);
	 	    s = scanner.next();
//		}
//		catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		if (s == null)
			status = false;
		else
			status = s.trim().equals("ok"); 
 	    return status;
	}

	public static boolean updateDB() {
		boolean status = false;
		boolean saveWork = false;
		String key = ControlAccess.getDBKey();
		File inFolder= Configuration.getDatabaseDirectory();
		File outFile= new File (Configuration.getSharedACMDirectory(), ControlAccess.getNextZipFilename()); 
		int n;
		try {
			do {
				n = 1;  // shutdown by default if no UI
				try {
					status = checkInDB(Configuration.getSharedACMname(), key, ControlAccess.getNextZipFilename()); 
					if (!status && !Configuration.isDisableUI()) {
						Object[] options = {"Force your version","Throw away your latest changes"};
						n = JOptionPane.showOptionDialog(null, "Someone has forced control of this ACM.", "Cannot Checkin!",JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
						if (n==0)
							key = ControlAccess.DB_KEY_OVERRIDE;
					}
				} catch (IOException e) {
					status = false;
					if (!Configuration.isDisableUI()) {
						Object[] options = {"Try again", "Shutdown"};
						n = JOptionPane.showOptionDialog(null, "Cannot reach Literacy Bridge web server.  Do you want to get online and try again or shutdown and try later?", "Cannot Connect to Server",JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
						if (n==1)
							saveWork = true;
					}
				}
			} while (!status && n==0);
			
			if (!Configuration.isDisableUI()) {
				if (status)
					JOptionPane.showMessageDialog(null,"Your changes have been checked in.");
				else if (saveWork)
					JOptionPane.showMessageDialog(null,"Your changes could not be checked in now, but you still have this ACM\nchecked out and can submit your changes later.");
				else 
					JOptionPane.showMessageDialog(null,"Your changes have been discarded.");
			}
			
			if (status) {
				ZipUnzip.zip(inFolder, outFile);
				// deleting old zip since we just got confirmation that the new zip was checkedin
				File oldzip = new File (Configuration.getSharedACMDirectory(),ControlAccess.getCurrentZipFilename());
				oldzip.delete();
			}
			if (status || (!saveWork)) {
				dbInfo.deleteCheckoutFile(); // do this first since it's most important to be deleted and the next line sometimes is unable to delete the entire directory
				// deleteLocalDB();  -- this line always fails to delete some files, so we'll just delete on startup if there is no checkout file
			}
		} catch (IOException e) {
			status = false;
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return status;
	}
}

