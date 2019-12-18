package org.literacybridge.acm.config;

import org.literacybridge.acm.Constants;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.CachingRepository;
import org.literacybridge.acm.repository.FileSystemGarbageCollector;
import org.literacybridge.acm.repository.FileSystemRepository;
import org.literacybridge.acm.store.*;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.literacybridge.acm.store.MetadataSpecification.DC_LANGUAGE;

@SuppressWarnings("serial")
public class DBConfiguration { //extends Properties {
  private static final Logger LOG = Logger
      .getLogger(DBConfiguration.class.getName());

  private Properties dbProperties;

  private boolean initialized = false;
  private File globalRepositoryDirectory;
  private File localCacheDirectory;
  private File dbDirectory;
  private File tbLoadersDirectory;
  private File sharedACMDirectory;
  private String acmName = null;
  private List<Locale> audioLanguages = null;
  private Map<Locale, String> languageLabels = new HashMap<Locale, String>();

  private AudioItemRepository repository;
  private MetadataStore store;

  private boolean sandboxed;

  private AccessControl accessControl;

  DBConfiguration(String acmName) {
    this.acmName = acmName;
  }

  public AudioItemRepository getRepository() {
    return repository;
  }

  private void setRepository(AudioItemRepository newRepository) {
    repository = newRepository;
  }

    public MetadataStore getMetadataStore() {
        return store;
    }

    /**
   *  Gets the name of the ACM directory, like "ACM-DEMO".
   * @return The name of this content database, including "ACM-".
   */
  public String getSharedACMname() {
    return acmName;
  }
  public String getProjectName() {
      return ACMConfiguration.cannonicalProjectName(acmName);
  }

  /**
   * Gets a File representing global (ie, Dropbox) ACM directory.
   * Like ~/Dropbox/ACM-TEST
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
     * The application's home directory.
     *  The non-shared directory root for config, content, cache, builds, etc...
     * @return ~/LiteracyBridge/ACM
     */
  private String getHomeAcmDirectory() {
    // ~/LiteracyBridge/ACM
    File acm = new File(ACMConfiguration.getInstance().getApplicationHomeDirectory(), Constants.ACM_DIR_NAME);
    if (!acm.exists())
      acm.mkdirs();
    return acm.getAbsolutePath();
  }

  /**
   * Gets the name of the local temp file directory. Each ACM has a sub-directory; those sub-dirs
   * are cleaned up at app exit.
   * @return ~/LiteracyBridge/ACM/temp
   */
  String getTempACMsDirectory() {
    // ~/LiteracyBridge/ACM/temp
    File temp = new File(getHomeAcmDirectory(), Constants.TempDir);
    if (!temp.exists())
      temp.mkdirs();
    return temp.getAbsolutePath();
  }

    /**
   * Gets a File representing the temporary database directory.
   * @return The File object for the directory.
   */
  File getTempDatabaseDirectory() {
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
    return new File(getTempDatabaseDirectory(), Constants.LuceneIndexDir);
  }

  /**
   * Gets a File representing the location of the content repository, like
   * ~/Dropbox/ACM-FOO/content
   * @return The File object for the content directory.
   */
  File getGlobalRepositoryDirectory() {
    if (globalRepositoryDirectory == null) {
      // ~/Dropbox/ACM-DEMO/content
      globalRepositoryDirectory = new File(getSharedACMDirectory(), Constants.RepositoryHomeDir);
    }
    return globalRepositoryDirectory;
  }

    /**
     * Gets a file representing the location of the local non-a18 content cache, like
     * ~/LiteracyBridge/ACM/cache/ACM-FOO
     * @return
     */
  File getLocalCacheDirectory() {
    if (localCacheDirectory == null) {
      // ~/LiteracyBridge/ACM/cache/ACM-DEMO
      localCacheDirectory = new File(getHomeAcmDirectory(),
              Constants.CACHE_DIR_NAME + "/" + getSharedACMname());
    }
    return localCacheDirectory;
  }

    /**
     * Gets a file representing a temporary, "scratch" area, the "Sandbox". Like
     * ~/LiteracyBridge/ACM/temp/ACM-FOO/content
     * @return The File object for the sandbox directory.
     */
    File getSandboxDirectory() {
        File fSandbox = null;
        if (isSandboxed()) {
            fSandbox = new File(getTempACMsDirectory(),
                getSharedACMname() + "/" + Constants.RepositoryHomeDir);
        }
        return fSandbox;
    }

    /**
     * The global TB-Loaders directory, where content updates are published.
     * @return The global directory.
     */
  public File getTBLoadersDirectory() {
    if (tbLoadersDirectory == null) {
      // ~/Dropbox/ACM-DEMO/TB-Loaders
      tbLoadersDirectory = new File(getSharedACMDirectory(),
              Constants.TBLoadersHomeDir);
    }
    return tbLoadersDirectory;
  }

    /**
     * The local TB-Loaders directory. New content update distributions are CREATEd here, before
     * being PUBLISHed to the global directory. Also, content update distributions are expanded
     * here, before being opened by TB-Loader.
     * @return the local TB-Loaders directory
     */
  public File getLocalTbLoadersDirectory() {
      File tbLoaders = new File(ACMConfiguration.getInstance().getApplicationHomeDirectory(),
                                Constants.TBLoadersHomeDir + File.separator + getProjectName());
      if (!tbLoaders.exists())
          tbLoaders.mkdirs();
      return tbLoaders;
  }

  public boolean isSandboxed() {
      return sandboxed;
  }
  void setSandboxed(boolean sandboxed) {
      this.sandboxed = sandboxed;
  }

    /**
     * This is slightly different than sandboxed. If the database isn't open, it won't be
     * sandboxed, but it won't be writable, either.
     * @return true if database is writable.
     */
    public boolean isWritable() {
        return accessControl != null && accessControl.getOpenStatus().isOpen() && !sandboxed;
    }


    private File getDBAccessListFile() {
        return new File(getSharedACMDirectory(), Constants.DB_ACCESS_FILENAME);
    }

    /**
     * Does the user named in ~/LiteracyBridge/acm_config.properties have write access to the
     * current database? (Is their name in the accessList.txt file?)
     * @param user The user in question.
     * @return true if user has write permission.
     */
    public boolean userHasWriteAccess(String user) {
        String writeUser;
        boolean userHasWriteAccess = false;

        user = user.trim();
        if (user == null) {
            // No User Name found in ~/LB/acm_config.properties. Forcing Read-Only mode.
            return userHasWriteAccess;
        }
        File f = getDBAccessListFile();
        if (f.exists()) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(f));
                while ((writeUser = in.readLine()) != null) {
                    if (writeUser.trim().equalsIgnoreCase(user)) {
                        userHasWriteAccess = true;
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (getSharedACMDirectory().list().length == 0) {
            // TODO: The "create new ACM" function should pre-populate the accessList.txt file,
            // rather than relying on this questionable side-effect.

            // empty directory -- which means that a new directory was created to
            // start an ACM in
            // Since the directory already exists, it is not the case that the user
            // just hasn't accepted the dropbox invitaiton yet.
            // So, give this user write access to the newly created ACM
            userHasWriteAccess = true;
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(f));
                out.write(user + "\n");
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return userHasWriteAccess;
    }

    public boolean userIsReadOnly() {
        return !userHasWriteAccess(ACMConfiguration.getInstance().getUserName());
    }

  /**
   * Gets a File containing the configuration properties for this ACM database.
   * @return The File.
   */
  File getConfigurationPropertiesFile() {
    // ~/Dropbox/ACM-DEMO/config.properties
    return ACMConfiguration.getInstance().getSharedConfigurationFileFor(getSharedACMname());
  }

  public boolean writeCategoryFilter(Taxonomy taxonomy) {
      // If sandboxed, *pretend* that we wrote it OK.
      if (isSandboxed()) return true;
      return CategoryFilter.writeCategoryFilter(sharedACMDirectory, taxonomy);
  }

  boolean init() throws Exception {
    if (!initialized) {
      InitializeAcmConfiguration();
      initializeLogger();
      // This is pretty hackey, knowing if we're GUI or not, here, deep in the guts.
      accessControl = (ACMConfiguration.getInstance().isDisableUI()) ? new AccessControl(this) : new GuiAccessControl(this);
      accessControl.initDb();

      if (accessControl.openStatus.isOpen()) {
          initializeRepositories();

          final Taxonomy taxonomy = Taxonomy.createTaxonomy(sharedACMDirectory);
          this.store = new LuceneMetadataStore(taxonomy, getLuceneIndexDirectory());

          parseLanguageLabels();

          fixupLanguageCodes();
          
          initialized = true;
      }
    }
    return initialized;
  }

  public void updateDb() {
      accessControl.updateDb();
  }

  public boolean commitDbChanges() {
      return accessControl.commitDbChanges() == AccessControl.UpdateDbStatus.ok;
  }

  public void closeDb() {
      if (!initialized) {
          throw new IllegalStateException("Can't close an un-opened database");
      }
      if (accessControl.getAccessStatus() != AccessControl.AccessStatus.none) {
          accessControl.discardDbChanges();
      }
  }

  public int getCurrentDbVersion() {
    return accessControl.getCurrentDbVersion();
  }

  public String getCurrentZipFilename() {
      return accessControl.getCurrentZipFilename();
  }

  public boolean isDbOpen() {
      return accessControl != null && accessControl.getOpenStatus().isOpen();
  }

  public AccessControl.AccessStatus getDbAccessStatus() {
      return accessControl != null ? accessControl.getAccessStatus() : AccessControl.AccessStatus.none;
  }

  public void writeProps() {
        try {
            BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(getConfigurationPropertiesFile()));
            dbProperties.store(out, null);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write configuration file: "
                + getConfigurationPropertiesFile(), e);
        }
    }

    long getCacheSizeInBytes() {
    long size = Constants.DEFAULT_CACHE_SIZE_IN_BYTES;
    String value = dbProperties.getProperty(Constants.CACHE_SIZE_PROP_NAME);
    if (value != null) {
      try {
        size = Long.parseLong(value);
      } catch (NumberFormatException e) {
        // ignore and use default value
      }
    }
    return size;
  }

  public String getLanguageLabel(Locale locale) {
    return languageLabels.get(locale);
  }

    public String getLanguageLabel(String languagecode) {
        Locale locale = new Locale(languagecode);
        return languageLabels.get(locale);
    }

    public boolean isShouldPreCacheWav() {
    boolean ret = false;
    String preCache = dbProperties.getProperty(Constants.PRE_CACHE_WAV);
    if (preCache.equalsIgnoreCase("TRUE")) {
      ret = true;
    }
    return ret;
  }

   public boolean isStrictDeploymentNaming() {
        String strictNaming = dbProperties.getProperty(Constants.STRICT_DEPLOYMENT_NAMING);
        return strictNaming == null || !strictNaming.equalsIgnoreCase("false");
    }

    public boolean isUserFeedbackHidden() {
        String userFeedbackHidden = dbProperties.getProperty(Constants.USER_FEEDBACK_HIDDEN);
        return userFeedbackHidden != null && userFeedbackHidden.equalsIgnoreCase("true");
    }

    /**
     * If true, add a toolbar button for configuration. Default is false; override in properties.config.
     * @return true if we should add a toolbar button for configuration.
     */
    public boolean configurationDialog() {
        String configurable = dbProperties.getProperty(Constants.CONFIGURATION_DIALOG);
        return ACMConfiguration.getInstance().isShowConfiguration() ||
            (configurable != null && configurable.equalsIgnoreCase("true"));
    }

    

    /**
     * The configured value of the fuzzy match threshold, for content matching. If no
     * value in the config file, returns null.
     * @return The value, or null if none is specified or is not parseable.
     */
    public Integer getFuzzyThreshold() {
        Integer result = Constants.FUZZY_THRESHOLD_DEFAULT;
        String value = dbProperties.getProperty(Constants.FUZZY_THRESHOLD);
        try {
            result = new Integer(value);
        } catch (Exception ignored) {
            // ignore and return default.
        }
        return result;
    }

    public void setFuzzyThreshold(int threshold) {
        threshold = Math.min(threshold, Constants.FUZZY_THRESHOLD_MAXIMUM);
        threshold = Math.max(threshold, Constants.FUZZY_THRESHOLD_MINIMUM);
        dbProperties.setProperty(Constants.FUZZY_THRESHOLD, Integer.toString(threshold));
    }

    /**
     * The configured value of "interested parties" for events in the ACM. This should
     * be a list of email addresses, separated by commas.
     * @return a possibly empty list of email addresses (not validated in any way).
     */
    public Collection<String> getNotifyList() {
        Set<String> result;
        String list = dbProperties.getProperty(Constants.NOTIFY_LIST);
        if (list != null) {
            result = Arrays.asList(list.split("[, ]+"))
                .stream()
                .map(String::trim)
                .collect(Collectors.toSet());
        } else {
            result = new HashSet<>();
        }
        return result;
    }

    public void setNotifyList(Collection<String> list) {
        dbProperties.setProperty(Constants.NOTIFY_LIST, String.join(", ", list));
    }

    /**
   * Parses the language labels from the 'AUDIO_LANGUAGES' String property
   * contained in the config.properties file. The appropriate line in the file
   * has the following format:
   * AUDIO_LANGUAGES=en,dga("Dagaare"),twi("Twi"),sfw("Sehwi")
   */
    private final static Pattern LANGUAGE_LABEL_PATTERN = Pattern
        .compile("^([a-zA-Z]{2,3})(?:\\(\"(.+)\"\\))?$");

    private void parseLanguageLabels() {
        if (audioLanguages == null) {
            audioLanguages = new ArrayList<Locale>();
            String languagesProperty = dbProperties.getProperty(Constants.AUDIO_LANGUAGES);
            if (languagesProperty != null) {
                String[] languages = languagesProperty.split(",");
                for (String language : languages) {
                    language = language.trim();
                    if (isEmpty(language)) continue;
                    Matcher labelMatcher = LANGUAGE_LABEL_PATTERN.matcher(language);
                    if (labelMatcher.matches()) {
                        String iso = labelMatcher.group(1);
                        String label = (labelMatcher.groupCount() > 1) ?
                                       labelMatcher.group(2) :
                                       null;
                        RFC3066LanguageCode rfc3066 = new RFC3066LanguageCode(iso);
                        Locale locale = rfc3066.getLocale();
                        if (locale != null) {
                            if (isEmpty(label) && !locale.getDisplayName().equalsIgnoreCase(iso)) {
                                label = locale.getDisplayName();
                            }
                            if (!isEmpty(label)) {
                                languageLabels.put(locale, label);
                            }
                            audioLanguages.add(locale);
                        }
                    }
                }
                if (audioLanguages.isEmpty()) {
                    languageLabels.put(Locale.ENGLISH, Locale.ENGLISH.getDisplayName());
                    audioLanguages.add(Locale.ENGLISH);
                }
            }
        }
    }

  public List<Locale> getAudioLanguages() {
    return Collections.unmodifiableList(audioLanguages);
  }

  private void InitializeAcmConfiguration() {
    boolean propsChanged = false;

    if (!getSharedACMDirectory().exists()) {
      // TODO: Get all UI out of this configuration object!!
      JOptionPane.showMessageDialog(null, "ACM database " + getSharedACMname()
              + " is not found within Dropbox.\n\nBe sure that you have accepted the Dropbox invitation\nto share the folder"
              + " by logging into your account at\nhttp://dropbox.com and click on the 'Sharing' link.\n\nShutting down.");
      System.exit(1);
    }

    // Create the cache directory before it's actually needed, to trigger any security exceptions.
    getLocalCacheDirectory().mkdirs();

    // like ~/Dropbox/ACM-UWR/config.properties
    if (getConfigurationPropertiesFile().exists()) {
      try {
        BufferedInputStream in = new BufferedInputStream(
            new FileInputStream(getConfigurationPropertiesFile()));
          dbProperties = new Properties();
          dbProperties.load(in);
      } catch (IOException e) {
        throw new RuntimeException("Unable to load configuration file: "
            + getConfigurationPropertiesFile(), e);
      }
    }

    if (!dbProperties.containsKey(Constants.PRE_CACHE_WAV)) {
        dbProperties.put(Constants.PRE_CACHE_WAV, "FALSE");
      propsChanged = true;
    }
    if (!dbProperties.containsKey(Constants.AUDIO_LANGUAGES)) {
        dbProperties.put(Constants.AUDIO_LANGUAGES,
          "en,dga(\"Dagaare\"),ssl(\"Sisaala\"),tw(\"Twi\"),"); // sfw(\"Sehwi\"),
      propsChanged = true;
    }
    if (!dbProperties.containsKey(Constants.CACHE_SIZE_PROP_NAME)) {
        dbProperties.put(Constants.CACHE_SIZE_PROP_NAME,
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

    private void initializeRepositories() {
        // Checked out, create the repository object.
        String user = ACMConfiguration.getInstance().getUserName();
        LOG.info(String.format(
            "  Repository:                     %s\n" +
                "  Temp Database:                  %s\n" +
                "  Temp Repository (sandbox mode): %s\n" +
                "  user:                           %s\n" +
                "  UserRWAccess:                   %s\n" +
                "  online:                         %s\n",
            getGlobalRepositoryDirectory(),
            getTempDatabaseDirectory(),
            getSandboxDirectory(),
            user,
            userHasWriteAccess(user),
            AccessControl.isOnline()));

        String wavExt = "." + AudioItemRepository.AudioFormat.WAV.getFileExtension();
        FileSystemGarbageCollector fsgc = new FileSystemGarbageCollector(
            getCacheSizeInBytes(),
            (file, name) -> name.toLowerCase().endsWith(wavExt));
        // The localCacheRepository lives in ~/LiteracyBridge/ACM/cache/ACM-FOO. It is used for all
        // non-A18 files. When .wav files (but not, say, mp3s) exceed max cache size, they'll be gc-ed.
        FileSystemRepository localCacheRepository = new FileSystemRepository(getLocalCacheDirectory(), fsgc);
        // If there is no sandbox directory, all A18s are read from and written to this directory. If there
        // IS a sandbox directory, then A18s are written there, and read from here if they're not in the
        // sandbox. (That behaviour is broken because there is no mechanism to clean out stale items from
        // the sandbox.)
        FileSystemRepository globalSharedRepository = new FileSystemRepository(getGlobalRepositoryDirectory());
        // If the ACM is opened in "sandbox" mode, all A18s are written here. A18s are read from here if
        // present, but read from the global shared repository if absent from the sandbox. Note that
        // if not sandboxed, this one will be null.
        FileSystemRepository sandboxRepository
            = isSandboxed() ? new FileSystemRepository(getSandboxDirectory()) : null;

        // The caching repository directs resolve requests to one of the three above file based
        // repositories.
        CachingRepository cachingRepository
            = new CachingRepository(localCacheRepository, globalSharedRepository, sandboxRepository);
        setRepository(new AudioItemRepository(cachingRepository));
    }


//    private void fixupCategories() {
//        long timer = -System.currentTimeMillis();
//
//        Category genHealth = this.store.getCategory("2-0");
//
//        Transaction transaction = this.store.newTransaction();
//        Collection<AudioItem> items = this.store.getAudioItems();
//        int itemsFixed = 0;
//
//        boolean success = false;
//        try {
//            for (AudioItem audioItem : items) {
//                if (audioItem.getCategoryList().contains(genHealth)) {
//                    audioItem.removeCategory(genHealth);
//                    transaction.add(audioItem);
//                    itemsFixed++;
//                }
//            }
//            transaction.commit();
//            success = true;
//        } catch (IOException e1) {
//            e1.printStackTrace();
//        } finally {
//            if (!success) {
//                try {
//                    transaction.rollback();
//                } catch (IOException e) {
//                    LOG.log(Level.SEVERE, "Unable to rollback transaction.", e);
//                }
//            }
//        }
//
//        timer += System.currentTimeMillis();
//        // If we did anything, or if the delay was perceptable (1/10th second), show the time.
//        if (itemsFixed > 0 || timer > 100) {
//            System.out.printf("Took %d ms to fix %d categories%n", timer, itemsFixed);
//        }
//    }

    /**
     * A number of years ago (I write this on 2018-05-10), we needed a new language, Tumu Sisaala.
     * The person implementing the language did not know the ISO 639-3 code, nor did he know that
     * he should strictly restrict the codes to that well-known list. He just made up "ssl1", as
     * a modification of Lambussie Sisaala, "ssl". But the correct code should have been "sil".
     * <p>
     * We've lived with this, as I say, for years. But today the pain of keeping the non-standard
     * language code exceeds the cost of fixing it, and so, here we are. This code translates
     * "ssl1" => "sil". It's only needed once per ACM, after which it adds perhaps 1ms to start
     * up time.
     */
    private void fixupLanguageCodes() {
        long timer = -System.currentTimeMillis();

        // Hack to fix ssl1->sil. If we ever have more, abstract this a bit more.
        String from = "ssl1";
        String to = "sil";
        RFC3066LanguageCode abstractLanguageCode = new RFC3066LanguageCode(to);
        MetadataValue<RFC3066LanguageCode> abstractMetadataLanguageCode = new MetadataValue(
            abstractLanguageCode);

        Transaction transaction = this.store.newTransaction();
        Collection<AudioItem> items = this.store.getAudioItems();
        int itemsFixed = 0;

        boolean success = false;
        try {
            for (AudioItem audioItem : items) {
                if (audioItem.getLanguageCode().equalsIgnoreCase(from)) {
                    audioItem.getMetadata().putMetadataField(DC_LANGUAGE, abstractMetadataLanguageCode);
                    transaction.add(audioItem);
                    itemsFixed++;
                }
            }
            transaction.commit();
            success = true;
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (!success) {
                try {
                    transaction.rollback();
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "Unable to rollback transaction.", e);
                }
            }
        }

        timer += System.currentTimeMillis();
        // If we did anything, or if the delay was perceptable (1/10th second), show the time.
        if (itemsFixed > 0 || timer > 100) {
            System.out.printf("Took %d ms to fix %d language codes%n", timer, itemsFixed);
        }
    }

}
