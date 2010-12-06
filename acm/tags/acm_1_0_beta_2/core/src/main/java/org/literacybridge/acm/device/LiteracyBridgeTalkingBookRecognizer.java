package org.literacybridge.acm.device;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class LiteracyBridgeTalkingBookRecognizer extends DeviceRecognizer {

	@Override
	public DeviceInfo identifyDevice(File pathToDevice) {
		File configFile = new File(pathToDevice, DeviceContents.CONFIG_FILE);
		if (!configFile.exists()) {
			return null;
		}
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(configFile));
			String line;
			while( (line = reader.readLine()) != null) {
				// TODO: read actual device ID
				if (line.startsWith("LB_TALKINGBOOK_VERSION")) {
					DeviceInfo info = new DeviceInfo("1", pathToDevice);
					return info;
				}
			}
			
		} catch (IOException e) {
			return null;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		
		return null;	
	}
	

}
