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
import java.util.StringTokenizer;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.store.LuceneMetadataStore;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.store.Taxonomy;

@SuppressWarnings("serial")
public class DBConfiguration extends Properties {
  private boolean initialized = false;
  private File repositoryDirectory;
  private File cacheDirectory;
  private File dbDirectory;
  private File tbLoadersDirectory;
  private File sharedACMDirectory;
  private String acmName = null;
  private List<Locale> audioLanguages = null;
  private Map<Locale, String> languageLables = new HashMap<Locale, String>();

  private AudioItemRepository repository;
  private MetadataStore store;

  private ControlAccess controlAccess;

  DBConfiguration(String acmName) {
    this.acmName = acmName;
  }

  public AudioItemRepository getRepository() {
    return repository;
  }

  public MetadataStore getMetadataStore() {
    return store;
  }

  void setRepository(AudioItemRepository newRepository) {
    repository = newRepository;
  }

  /**
   *  Gets the name of the ACM directory, like "ACM-DEMO".
   * @return The name of this content database, including "ACM-".
   */
  public String getSharedACMname() {
    return acmName;
  }

  /**
   * Gets a File representing global (ie, Dropbox) ACM directory.
   * @return The global File for this content database.
   */
  public File getSharedACMDirectory() {
    if (sharedACMDirectory == null) {
      sharedACMDirectory = new File(
              ACMConfiguration.getInstance().getGlobalShareDir(),
              getSharedACMname());
    }
    return sharedACMDirectory;
  }

  /**
   * Gets the File representing the local ACM Application directory.
   *
   * TODO: Why is this here? This is not a per-database property.
   * @return The local application directory.
   */
  public static File getLiteracyBridgeHomeDirectory() {
    // ~/LiteracyBridge
    return ACMConfiguration.getInstance().getApplicationDirectory();
  }

  // Call this methods to get the non-shared directory root for config, content
  // cache, builds, etc...
  private String getHomeAcmDirectory() {
    // ~/LiteracyBridge/ACM
    File acm = new File(getLiteracyBridgeHomeDirectory(), Constants.ACM_DIR_NAME);
    if (!acm.exists())
      acm.mkdirs();
    return acm.getAbsolutePath();
  }

  /**
   * Gets the name of the local temp file directory.
   * @return The name of the directory.
   */
  String getTempACMsDirectory() {
    // ~/LiteracyBridge/temp
    File temp = new File(getHomeAcmDirectory(), Constants.TempDir);
    if (!temp.exists())
      temp.mkdirs();
    return temp.getAbsolutePath();
  }

  /**
   * Gets a File representing the temporary database directory.
   * @return The File object for the directory.
   */
  File getDatabaseDirectory() {
    if (dbDirectory == null)
      // ~/LiteracyBridge/temp/ACM-DEMO/db
      dbDirectory = new File(getTempACMsDirectory(),
          getSharedACMname() + File.separator + Constants.DBHomeDir);
    return dbDirectory;
  }

  /**
   * Gets a File representing the location of the lucene index, in the
   * temporary database directory.
   * @return The File object for the lucene directory.
   */
  private File getLuceneIndexDirectory() {
    // ~/LiteracyBridge/temp/ACM-DEMO/db/index
    return new File(getDatabaseDirectory(), Constants.LuceneIndexDir);
  }

  /**
   * Gets a File representing the location of the content repository.
   * @return The File object for the content directory.
   */
  File getRepositoryDirectory() {
    if (repositoryDirectory == null) {
      // ~/Dropbox/ACM-DEMO/content
      repositoryDirectory = new File(getSharedACMDirectory(), Constants.RepositoryHomeDir);
    }
    return repositoryDirectory;
  }

  File getCacheDirectory() {
    if (cacheDirectory == null) {
      // ~/LiteracyBridge/ACM/cache/ACM-DEMO
      cacheDirectory = new File(getHomeAcmDirectory(),
              Constants.CACHE_DIR_NAME + "/" + getSharedACMname());
    }
    return cacheDirectory;
  }

  public File getTBLoadersDirectory() {
    if (tbLoadersDirectory == null) {
      // ~/Dropbox/ACM-DEMO/TB-Loaders
      tbLoadersDirectory = new File(getSharedACMDirectory(),
              Constants.TBLoadersHomeDir);
    }
    return tbLoadersDirectory;
  }

  /**
   * Gets the File, if it exists, containing the list of content updates that
   * are deferred until later for user feedback importing. (ie, user feedback
   * for these updates won't be imported.)
   * @return The File.
   */
  public File getUserFeedbackDeferredUpdatesFile() {
    // ~/Dropbox/ACM-DEMO/userfeedback.deferred
    return new File(getSharedACMDirectory(), Constants.USER_FEEDBACK_DEFERRED_UPDATES_FILENAME);
  }
  /**
   * Gets a File containing the configuration properties for this ACM database.
   * @return The File.
   */
  private File getConfigurationPropertiesFile() {
    // ~/Dropbox/ACM-DEMO/config.properties
    return new File(getSharedACMDirectory(), Constants.CONFIG_PROPERTIES);
  }

  private void writeProps() {
    try {
      BufferedOutputStream out = new BufferedOutputStream(
          new FileOutputStream(getConfigurationPropertiesFile()));
      super.store(out, null);
      out.flush();
      out.close();
    } catch (IOException e) {
      throw new RuntimeException("Unable to write configuration file: "
          + getConfigurationPropertiesFile(), e);
    }
  }

  void init() throws Exception {
    if (!initialized) {
      InitializeAcmConfiguration();
      initializeLogger();
      controlAccess = new ControlAccess(this);
      controlAccess.init();

      final Taxonomy taxonomy = Taxonomy.createTaxonomy(sharedACMDirectory);
      this.store = new LuceneMetadataStore(taxonomy, getLuceneIndexDirectory());

      parseLanguageLabels();
      initialized = true;
    }
  }

  public ControlAccess getControlAccess() {
    return controlAccess;
  }

  long getCacheSizeInBytes() {
    long size = Constants.DEFAULT_CACHE_SIZE_IN_BYTES;
    String value = getProperty(Constants.CACHE_SIZE_PROP_NAME);
    if (value != null) {
      try {
        size = Long.parseLong(value);
      } catch (NumberFormatException e) {
        // ignore and use default value
      }
    }
    return size;
  }

  private final static Pattern LANGUAGE_LABEL_PATTERN = Pattern
      .compile(".*\\(\"(.+)\"\\).*");

  public String getLanguageLabel(Locale locale) {
    return languageLables.get(locale);
  }

  public boolean shouldPreCacheWav() {
    boolean ret = false;
    String preCache = getProperty(Constants.PRE_CACHE_WAV);
    if (preCache.equalsIgnoreCase("TRUE")) {
      ret = true;
    }
    return ret;
  }

  /**
   * Should locking be maintained in AWS. If not, it runs in the WordPress
   * server. Reads USE_AWS_LOCKING = true|false from config file.
   * Currently defaults to false. Next, default to true. Then remove old
   * WordPress code and remove this entirely.
   */
  boolean useAwsLocking() {
    String awsLocking = getProperty(Constants.USE_AWS_LOCKING);
    return awsLocking != null && awsLocking.equalsIgnoreCase("TRUE");
  }

  /**
   * Parses the language labels from the 'AUDIO_LANGUAGES' String property
   * contained in the config.properties file. The appropriate line in the file
   * has the following format:
   * AUDIO_LANGUAGES=en,dga("Dagaare"),tw("Twi"),sfw("Sehwi")
   */
  private void parseLanguageLabels() {
    if (audioLanguages == null) {
      audioLanguages = new ArrayList<Locale>();
      String languages = getProperty(Constants.AUDIO_LANGUAGES);
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
  }

  public List<Locale> getAudioLanguages() {
    return Collections.unmodifiableList(audioLanguages);
  }

  /**
   * The "correlation id" is a small, incrementing integer that is assigned
   * to user feedback imported into an ACM database (generally, a -FB- database.)
   *
   * These ids are managed by the feedback importer, and stored here so that
   * the latest value is available on every machine.
   * @return The next correlation id property.
   */
  public int getNextCorrelationId() {
    String nextId = getProperty("NEXT_CORRELATION_ID");
    if (nextId == null) {
      return 0;
    }
    return Integer.valueOf(nextId);
  }

  public void setNextCorrelationId(int nextId) {
    String id = String.valueOf(nextId);
    setProperty("NEXT_CORRELATION_ID", id);
    writeProps();
  }

  private void InitializeAcmConfiguration() {
    boolean propsChanged = false;

    if (!getSharedACMDirectory().exists()) {
      // TODO: Get all UI out of this configuration object!!
      JOptionPane.showMessageDialog(null, "ACM database " + getSharedACMname()
              + " is not found within Dropbox.\n\nBe sure that you have accepted the Dropbox invitation\nto share the folder"
              + " by logging into your account at\nhttp://dropbox.com and click on the 'Sharing' link.\n\nShutting down.");
      System.exit(0);
    }

    // Create the cache directory before it's actually needed, to trigger any security exceptions.
    getCacheDirectory().mkdirs();

    // like ~/Dropbox/ACM-UWR/config.properties
    if (getConfigurationPropertiesFile().exists()) {
      try {
        BufferedInputStream in = new BufferedInputStream(
            new FileInputStream(getConfigurationPropertiesFile()));
        load(in);
      } catch (IOException e) {
        throw new RuntimeException("Unable to load configuration file: "
            + getConfigurationPropertiesFile(), e);
      }
    }

    if (!containsKey(Constants.PRE_CACHE_WAV)) {
      put(Constants.PRE_CACHE_WAV, "FALSE");
      propsChanged = true;
    }
    if (!containsKey(Constants.AUDIO_LANGUAGES)) {
      put(Constants.AUDIO_LANGUAGES,
          "en,dga(\"Dagaare\"),ssl(\"Sisaala\"),tw(\"Twi\"),"); // sfw(\"Sehwi\"),
      propsChanged = true;
    }
    if (!containsKey(Constants.CACHE_SIZE_PROP_NAME)) {
      put(Constants.CACHE_SIZE_PROP_NAME,
          Long.toString(Constants.DEFAULT_CACHE_SIZE_IN_BYTES));
      propsChanged = true;
    }

    if (propsChanged) {
      writeProps();
    }
  }

  private void initializeLogger() {
    try {
      // Get the global logger to configure it
      // TODO: WTF? Shouldn't the *global* logger be initialized in some
      // *global* constructor? Or better yet, static initializer?
      Logger logger = Logger.getLogger("");

      logger.setLevel(Level.INFO);
      String fileNamePattern = getHomeAcmDirectory() + File.separator
          + "acm.log.%g.%u.txt";
      FileHandler fileTxt = new FileHandler(fileNamePattern);

      Formatter formatterTxt = new SimpleFormatter();
      fileTxt.setFormatter(formatterTxt);
      logger.addHandler(fileTxt);
    } catch (IOException e) {
      e.printStackTrace();
      System.err.println(
          "Unable to initialize log file. Will be logging to stdout instead.");
    }
  }
}
