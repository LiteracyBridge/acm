package org.literacybridge.acm.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import org.literacybridge.acm.Constants;

public class Configuration extends Properties {
	private static final File CONFIG_FILE = new File(Constants.LB_SYSTEM_DIR, "config.properties");

	private static Configuration instance;
	
	private Configuration() {
		// singleton
	}
	
	public static Configuration getConfiguration() throws IOException {
		if (instance == null) {
			instance = new Configuration();
			
			if (CONFIG_FILE.exists()) {
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(CONFIG_FILE));
				instance.load(in);
			}

		}
		
		return instance;
	}
	
	public void writeProps() throws IOException {
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(CONFIG_FILE));
		super.store(out, null);
		out.flush();
		out.close();
	}
	
	
	//====================================================================================================================
	
	
	
	
	private final static String RECORDING_COUNTER_PROP = "RECORDING_COUNTER";
	private final static String DEVICE_ID_PROP = "DEVICE_ID";
	
	public String getRecordingCounter() {
		return getProperty(RECORDING_COUNTER_PROP);
	}
	
	public void setRecordingCounter(String counter) {
		setProperty(RECORDING_COUNTER_PROP, counter);
	}
	
	public String getDeviceID() throws IOException {
		String value = getProperty(DEVICE_ID_PROP);
		if (value == null) {
			final int n = 10;
			Random rnd = new Random();
			// generate 10-digit unique ID for this acm instance
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < n; i++) {
				builder.append(Character.forDigit(rnd.nextInt(Character.MAX_RADIX), Character.MAX_RADIX));
			}
			value = builder.toString();
			setProperty(DEVICE_ID_PROP, value);
			writeProps();
		}
		
		return value;
	}
}
