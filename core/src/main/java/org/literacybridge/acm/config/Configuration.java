package org.literacybridge.acm.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.metadata.RFC3066LanguageCode;

public class Configuration extends Properties {
	private static final File CONFIG_FILE = new File(Constants.LB_SYSTEM_DIR, "config.properties");

	private static Configuration instance;
	
	private Configuration() {
		// singleton
	}
	
	public static Configuration getConfiguration() {
		if (instance == null) {
			instance = new Configuration();
			
			if (CONFIG_FILE.exists()) {
				try {
					BufferedInputStream in = new BufferedInputStream(new FileInputStream(CONFIG_FILE));
					instance.load(in);
				} catch (IOException e) {
					throw new RuntimeException("Unable to load configuration file: " + CONFIG_FILE, e);
				}
			}
			
			if (!instance.containsKey(AUDIO_LANGUAGES)) {
				instance.put(AUDIO_LANGUAGES, "en,dga(\"Dagaare\"),tw(\"Twi\"),sfw(\"Sehwi\")");
				instance.writeProps();
			}

		}
		
		return instance;
	}
	
	public void writeProps() {
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(CONFIG_FILE));
			super.store(out, null);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException("Unable to write configuration file: " + CONFIG_FILE, e);
		}
	}
	
	
	//====================================================================================================================
	
	
	
	
	private final static String RECORDING_COUNTER_PROP = "RECORDING_COUNTER";
	private final static String DEVICE_ID_PROP = "DEVICE_ID";
	private final static String AUDIO_LANGUAGES = "AUDIO_LANGUAGES";
	
	private List<Locale> audioLanguages = null;
	private Map<Locale,String> languageLables = new HashMap<Locale, String>();
	
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
	
	private final static Pattern LANGUAGE_LABEL_PATTERN = Pattern.compile(".*\\(\"(.+)\"\\).*");
	
	public String getLanguageLabel(Locale locale) {
		return languageLables.get(locale);
	}
	
	public List<Locale> getAudioLanguages() {
		if (audioLanguages == null) {
			audioLanguages = new ArrayList<Locale>();
			String languages = getProperty(AUDIO_LANGUAGES);
			if (languages != null) {
				StringTokenizer tokenizer = new StringTokenizer(languages, ", ");
				while (tokenizer.hasMoreTokens()) {
					String code = tokenizer.nextToken();
					String label = null;
					Matcher labelMatcher = LANGUAGE_LABEL_PATTERN.matcher(code);
					if (labelMatcher.matches()) {
						label = labelMatcher.group(1);
						code = code.substring(0, code.indexOf("("));
					}
					RFC3066LanguageCode language = new RFC3066LanguageCode(code);
					Locale locale = language.getLocale();
					if (locale != null) {
						if (label != null) {
							languageLables.put(locale, label);
						}
						audioLanguages.add(locale);
					}
				}
				if (audioLanguages.isEmpty()) {
					audioLanguages.add(Locale.ENGLISH);
				}
			}
		}
		
		return Collections.unmodifiableList(audioLanguages);
	}
}
