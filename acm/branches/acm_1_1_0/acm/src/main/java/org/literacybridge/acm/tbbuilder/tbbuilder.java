package org.literacybridge.acm.tbbuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.literacybridge.acm.deviceimage.DeviceImages;
import org.literacybridge.acm.thrift.ThriftDeviceImage;
import org.literacybridge.acm.thrift.ThriftDeviceProfile;
import org.literacybridge.acm.thrift.ThriftPlaylistMapping;

public class tbbuilder {
	private final static TProtocolFactory PROTOCOL_FACTORY = new TJSONProtocol.Factory();
	String issue;
	String issueDir;
	File   issDir;
	File   DevImage;
	File   activeList;
	String   TBBuildOutDir;
	ThriftDeviceImage ThriftDI;
	FileOutputStream activeListout;
	String crlf = "\r\n";
	
	public tbbuilder(String whichissue) {
		issue = whichissue;
		
		issueDir = System.getProperty("user.dir"); 
		issueDir += File.separator;
		issueDir += "TB_Builds" +  File.separator;
		issueDir += whichissue; 
		
		issDir = new File(issueDir);
		issDir.mkdirs();   // make all dirs in output path
		
		activeList = new File(issDir.getAbsoluteFile() + File.separator + "_activeList.txt");
		activeList.delete();
		try {
			activeList.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.printf("activeList--");
		System.out.println(activeList);
			
		try {
			activeListout = new FileOutputStream(activeList);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
//		System.out.printf(" tbbuilder : issue file = %s\n",  whichissue + ".json");
		
		DevImage = new File( whichissue + ".json");
		
//		System.out.println(DevImage);
		try {
			ThriftDI =  DeviceImages.loadDeviceImage(DevImage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println(ThriftDI);
//		List<ThriftDeviceProfile> l = ThriftDI.getProfiles();
		
		
	}
	public boolean  Build() {
		
		FileOutputStream curListout = null;
		
		int passes = 0;
		
		Iterator<ThriftDeviceProfile> it = ThriftDI.getProfilesIterator();
		
		while (it.hasNext()) {
			// temporarily only the first profile is built, more work needed for multiple profiles
			if(passes > 0)  
				break;
			passes++;
			
			ThriftDeviceProfile curprof = it.next();
			System.out.printf("Profile name = %s\n", curprof.getName());
			java.util.Iterator<ThriftPlaylistMapping> plmap = curprof.getPlaylistsIterator();
			while (plmap.hasNext()) {
				ThriftPlaylistMapping pl = plmap.next();
				String lineout = "";
				if(pl.isLocked()) {
					lineout = "!";
				}
				lineout += pl.getAudioLabel();
				// lineout to _activeList.txt
				System.out.printf("%s : ", lineout);
				try {
					activeListout.write(lineout.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					activeListout.write(crlf.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// write to lineout.txt (skip possible "!" on front
				// pl.getAudioLabel();
				
				File curList = new File(issDir.getAbsoluteFile() + File.separator +  pl.getAudioLabel() + ".txt");
				curList.delete();
				try {
					curList.createNewFile();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				try {
					curListout = new FileOutputStream(curList);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Iterator<String> uids = pl.getAudioItemUIDsIterator();
				
				while(uids.hasNext()) {
//					Object ituid =  uids.next();
					String x = uids.next();
					System.out.printf("%s ", x);
					try {
						curListout.write(x.getBytes());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						curListout.write(crlf.getBytes());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					// for each of these find the .a18 file & copy to messages
				}
				
				
				System.out.printf("\n");
			}
		}
		
		return true;
	}
	public List<ThriftDeviceProfile> getProfiles() {
		return ThriftDI.getProfiles();
	}
	public String deviceImagename() {
		return ThriftDI.name;
	}
	public void setIssueDir (String dir) {
		issueDir = dir;
	}
	public String getIssueDir() {
		return issueDir;
	}
	/*
	public static ThriftDeviceImage loadDeviceImage(File file) throws IOException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(file), 2048);
		try {
			TProtocol protocol = PROTOCOL_FACTORY.getProtocol(new TIOStreamTransport(in));
			ThriftDeviceImage image = new ThriftDeviceImage();
			image.read(protocol);
			return image;
		} catch (TException e) {
			throw new IOException(e);
		} finally {
			in.close();
		}
	}
	*/

}
