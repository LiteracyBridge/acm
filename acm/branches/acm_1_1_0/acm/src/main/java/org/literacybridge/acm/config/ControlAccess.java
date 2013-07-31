package org.literacybridge.acm.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
	private static File sandboxDirectory = null;
    static boolean readOnly = false;
    private static boolean sandbox = false;
    private static String dbKey;
    private static String possessor;
    private static String currentZipFilename;
    private static String nextZipFilename;
    
    private static void setDBKey(String key) {
    	dbKey = key;
    }
    
    private static String getDBKey() {
    	return dbKey;
    }

    private static void setPossessor(String name) {
    	possessor = name;
    }
    
    private static String getPosessor() {
    	return possessor;
    }

    private static void setCurrentZipFilename(String filename) {
    	currentZipFilename = filename;
    	String newFilename;
    	
    	if (filename == null) // no db zip exists in dropbox but the db has been checked in; no need for setNextZipFilename, because time to shutdown
    		return;
    	if (filename.equalsIgnoreCase(DB_DOES_NOT_EXIST)) {
    		// ACM does not yet exist, so create name for newly created zip to use on updateDB()
    		newFilename = new String(DB_ZIP_FILENAME_INITIAL);
    	} else {
	    	String baseFilename = filename.substring(DB_ZIP_FILENAME_PREFIX.length(), filename.lastIndexOf('.'));
	    	try {
		    	int count = Integer.parseInt(baseFilename) + 1;
		    	newFilename = new String (DB_ZIP_FILENAME_PREFIX + String.valueOf(count) + filename.substring(filename.lastIndexOf('.')));
	    	} catch (NumberFormatException e) {
	    		LOG.log(Level.WARNING, "Unable to parse filename " + filename);
	    		return;
	    	}
    	}
    	setNextZipFilename(newFilename);
    }

    private static String getCurrentZipFilename() {
    	return currentZipFilename;
    }
    
    private static void setNextZipFilename(String filename) {
    	nextZipFilename = filename;
    }

    public static String getNextZipFilename() {
    	return nextZipFilename;
    }
    
	public static File getSandboxDirectory() {
		return sandboxDirectory;
	}

    private static void setSandboxDirectory(File f) {
    	sandboxDirectory = f; 
    }
    
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

	public static void determineAccess() {
//		if (!ControlAccess.isACMReadOnly() || !isSandbox())
			determineRWStatus();
//		else 
//			System.out.println("Command-line forced read-only or sandbox mode.");
//		if (ControlAccess.isACMReadOnly() || isSandbox()) {
			// ACM should be read-only -- change db pointer
			createDBMirror();
			File fDB = new File(Configuration.getACMDirectory(),Constants.DBHomeDir);
			Configuration.setDatabaseDirectory(fDB);
			File fSandbox = null; 
			if (isSandbox()) {
				fSandbox = new File(Configuration.getACMDirectory(),Constants.RepositoryHomeDir);
			} 
			setSandboxDirectory(fSandbox);				
	//	}
		System.out.println("  Database:" + Configuration.getDatabaseDirectory());
		System.out.println("  Repository:" + Configuration.getRepositoryDirectory());
//		if (isSandbox()) {
			System.out.println("  Sandbox:" + getSandboxDirectory());
//		}
		System.out.println("  UserRWAccess:" + userHasWriteAccess());
		System.out.println("  online:" + isOnline());

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
		File toDir = new File(Configuration.getACMDirectory());
		String dbFilename = getCurrentZipFilename();
		try {
			File oldDB = new File (toDir,Constants.DBHomeDir);
			FileUtils.deleteDirectory(oldDB);
			if (dbFilename.equalsIgnoreCase(ControlAccess.DB_DOES_NOT_EXIST)) {
				oldDB.mkdir(); // recreate db directory that was just deleted
				System.out.println("Created new empty DB dir");
			} else {
				File dbZip = new File(Configuration.getSharedACMDirectory(),dbFilename);
				System.out.println("Started DB Mirror:"+ Calendar.getInstance().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND)+":"+Calendar.getInstance().get(Calendar.MILLISECOND));
				ZipUnzip.unzip(dbZip, toDir, Constants.DBHomeDir);
				//FileUtils.copyDirectoryToDirectory(fromDir, toDir);
				System.out.println("Completed DB Mirror:"+ Calendar.getInstance().get(Calendar.MINUTE)+":"+Calendar.getInstance().get(Calendar.SECOND)+":"+Calendar.getInstance().get(Calendar.MILLISECOND));
			} 
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	private static boolean haveLatestDB() {
		String filenameShouldHave = getCurrentZipFilename();
		File fileShouldHave = new File(Configuration.getSharedACMDirectory(),filenameShouldHave);
		String filenameFallback = null;
		File[] files;
		boolean status;
		long lastModified = 0;
		if (fileShouldHave.exists()) {
			status = true;
		} else {
			// latest db zip to be checked in is not yet in dropbox, so find most recent zip and force sandbox mode
			status = false;
		
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
					setCurrentZipFilename(filenameFallback);
				}
			}
		}
		return status;
	}
	
	private static void determineRWStatus() {
		boolean sandboxMode = false;
		boolean forced = false;
		String dialogMessage = new String();
		boolean dbAvailable = false;
		boolean outdatedDB = false;
		
		if (!isOnline()) {
			sandboxMode = true;
			dialogMessage = "Cannot connect to dropbox.com.";
		} else {
			dbAvailable = checkOutDB(Configuration.getSharedACMname(), new String("statusCheck"));
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
		}
		if (!userHasWriteAccess() || !dbAvailable || outdatedDB) {
			sandboxMode = true; 
		} 
		if (outdatedDB) {
			dialogMessage = "The latest version of the ACM database has not yet downloaded to this computer.\nYou may shutdown and wait or begin demonstration mode with the previous version.";
		} else if (!dbAvailable) {
			dialogMessage = "Another user currently has write access to the ACM.\n";
			dialogMessage += getPosessor() + "\n";
		} 

		if (sandboxMode && userHasWriteAccess() && !Configuration.isDisableUI()) {
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
					forced = dbAvailable = checkOutDB(Configuration.getSharedACMname(), DB_KEY_OVERRIDE);
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
				dbAvailable = checkOutDB(Configuration.getSharedACMname(),"checkout");
				if (!dbAvailable) {
					JOptionPane.showMessageDialog(null,"Sorry, but another user must have just checked out this ACM a moment ago!\nTry contacting " + getPosessor() + "\nAfter clicking OK, the ACM will shutdown.");
					System.exit(0);
				} else  {
					if (getCurrentZipFilename().equalsIgnoreCase(DB_DOES_NOT_EXIST)) {
						setDBKey(DB_KEY_OVERRIDE);
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

	private static boolean checkOutDB(String db, String action) {
		boolean status = true;
		String filename = null, key = null, possessor = null;

		URL url;
		try {
			url = new URL("http://literacybridge.org/checkout.php?db=" + db + "&action=" + action + "&name=" + Configuration.getUserName() + "&contact=" + Configuration.getUserContact()+ "&version=" + Constants.ACM_VERSION);
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
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    if (filename != null)
	    	setCurrentZipFilename(filename);
 	    if (status) {
 	    	if (key != null) 
 	    		setDBKey(key);
 	    } else if (possessor != null) {
 	    	setPossessor(possessor);
 	    }
	    return status;
	}

	private static boolean checkInDB(String db, String key, String filename) {
		boolean status = true;
		final String END_OF_INPUT = "\\Z";
		String s = null;
		
		URL url;
		try {
			url = new URL("http://literacybridge.org/checkin.php?db=" + db + "&key=" + key + "&filename=" + filename + "&name=" + Configuration.getUserName() + "&contact=" + Configuration.getUserContact()+ "&version=" + Constants.ACM_VERSION);
			InputStream in;
			in = url.openStream();
			Scanner scanner = new Scanner(in);
		    scanner.useDelimiter(END_OF_INPUT);
	 	    s = scanner.next();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (s == null)
			status = false;
		else
			status = s.trim().equals("ok"); 
 	    return status;
	}

	public static void updateDB() {
		File inFolder= Configuration.getDatabaseDirectory();
		File outFile= new File (Configuration.getSharedACMDirectory(), ControlAccess.getNextZipFilename()); 
		try {
			ZipUnzip.zip(inFolder, outFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean status = checkInDB(Configuration.getSharedACMname(), ControlAccess.getDBKey(), ControlAccess.getNextZipFilename()); 
		if (status) {
			File oldDB = new File (Configuration.getSharedACMDirectory(),ControlAccess.getCurrentZipFilename());
			oldDB.delete();
		}
	}
}

