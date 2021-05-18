package org.literacybridge.acm.config;

import org.amplio.CloudSync;
import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.Constants;
import org.literacybridge.acm.cloud.Authenticator;
import org.literacybridge.acm.config.AccessControlResolver.AccessStatus;
import org.literacybridge.acm.repository.AudioItemRepository;
import org.literacybridge.acm.repository.AudioItemRepositoryImpl;
import org.literacybridge.acm.sandbox.Sandbox;
import org.literacybridge.acm.store.AudioItem;
import org.literacybridge.acm.store.LuceneMetadataStore;
import org.literacybridge.acm.store.MetadataStore;
import org.literacybridge.acm.store.MetadataValue;
import org.literacybridge.acm.store.RFC3066LanguageCode;
import org.literacybridge.acm.store.Taxonomy;
import org.literacybridge.acm.store.Transaction;

import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.literacybridge.acm.store.MetadataSpecification.DC_LANGUAGE;

public class DBConfiguration {
  private static final Logger LOG = Logger.getLogger(DBConfiguration.class.getName());

  private Properties dbProperties;
  private final PathsProvider pathsProvider;

  private boolean initialized = false;
  private List<Locale> audioLanguages = null;
  private final Map<Locale, String> languageLabels = new HashMap<>();

  private AudioItemRepositoryImpl repository;
  private MetadataStore store;

  private Sandbox sandbox = null;
  private boolean sandboxed;
  private boolean syncFailure;

  private AccessControl accessControl;

    public DBConfiguration(PathsProvider pathsProvider) {
        this.pathsProvider = pathsProvider;
        if (!pathsProvider.isDropboxDb()) {
            System.out.printf("Program %s uses %s for storage.\n", this.pathsProvider.getProgramName(), this.pathsProvider.isDropboxDb()?"Dropbox":"S3");
        }
    }

    public PathsProvider getPathProvider() {
        return pathsProvider;
    }

    public AudioItemRepository getRepository() {
    return repository;
  }

    public MetadataStore getMetadataStore() {
        return store;
    }

    public void setupWavCaching(Predicate<Long> queryGc) throws IOException {
      repository.setupWavCaching(queryGc);
  }

  public AudioItemRepositoryImpl.CleanResult cleanUnreferencedFiles() {
      return repository.cleanUnreferencedFiles();
  }

    /**
   *  Gets the name of the ACM directory, like "ACM-DEMO".
   * @return The name of this content database, including "ACM-", if the directory name has an "ACM-" prefix.
   */
  public String getProgramHomeDirName() {
    return pathsProvider.getProgramDirName();
  }
  @Deprecated
  public String getProjectName() {
      return pathsProvider.getProgramName();
  }

    /**
     * Gets the program id of the program in the ACM database.
     * @return the program id.
     */
  public String getProgramName() {
      return pathsProvider.getProgramName();
  }

  /**
   * Gets a File representing global (ie, Dropbox) ACM directory.
   * Like ~/Dropbox/ACM-TEST
   * @return The global File for this content database.
   */
  public File getProgramHomeDir() {
    return pathsProvider.getProgramHomeDir();
  }

    public boolean isSyncFailure() {
      return syncFailure;
    }
    public void setSyncFailure(boolean syncFailure) {
      this.syncFailure = syncFailure;
    }

    /**
   * Gets a File representing the temporary database directory.
   * ~/Amplio/ACM/temp/${acmDbDirName}/db
   * @return The File object for the directory.
   */
  File getLocalTempDbDir() {
    return pathsProvider.getLocalTempDbDir();
  }

  /**
   * Gets a File representing the location of the lucene index, in the
   * temporary database directory.
   * ~/Amplio/ACM/temp/${acmDbDirName}/db/index
   * @return The File object for the lucene directory.
   */
  private File getLocalLuceneIndexDir() {
    // ~/LiteracyBridge/temp/ACM-DEMO/db/index
    return new File(getLocalTempDbDir(), Constants.LuceneIndexDir);
  }

  /**
   * Gets a File representing the location of the content repository, like
   * ~/Dropbox/${programDbDir}/content or ~/Amplio/acm-dbs/${programDbDir}/content
   * @return The File object for the content directory.
   */
  public File getProgramContentDir() {
    return pathsProvider.getProgramContentDir();
  }

    /**
     * Gets a file representing the location of the local non-a18 content cache, like
     * ~/LiteracyBridge/ACM/cache/ACM-FOO
     * @return the file object for the local cache.
     */
    public File getLocalCacheDirectory() {
    return pathsProvider.getLocalAcmCacheDir();
  }

    /**
     * The global TB-Loaders directory, where content updates are published.
     * @return The global directory.
     */
  public File getProgramTbLoadersDir() {
    return pathsProvider.getProgramTbLoadersDir();
  }

    public Sandbox getSandbox() {
        if (sandbox == null) {
            this.sandbox = new Sandbox(pathsProvider.getProgramHomeDir(), pathsProvider.getSandboxDir());
        }
        return sandbox;
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

    public boolean userIsReadOnly() {
        return !Authenticator.getInstance().hasUpdatingRole();
    }

  boolean init(AccessControlResolver accessControlResolver) throws Exception {
    if (!initialized) {
      InitializeAcmConfiguration();
      if (accessControlResolver == null) accessControlResolver = AccessControlResolver.getDefault();
      accessControl = new AccessControl(this, accessControlResolver);
      accessControl.initDb();

      if (accessControl.openStatus.isOpen()) {
          findChangeMarkerFile();
          initializeRepositories();
          final Taxonomy taxonomy = Taxonomy.createTaxonomy(loadCategoryFilter(), getProgramHomeDir());
          this.store = new LuceneMetadataStore(taxonomy, getLocalLuceneIndexDir());
          this.store.addDataChangeListener(metadataChangeListener);

          parseLanguageLabels();

          fixupLanguageCodes();
          
          initialized = true;
      }
    }
    return initialized;
  }
    // Tracking whether there are changes to the metadata, ie, changes to be checked in.
    private boolean hasMetadataChange = false;

    /**
     * When a change is detected, write a marker file. This is deleted if the database is either persisted
     * or abandoned.
     */
    private void writeMetadataChangeMarkerFile() {
        try {
            new File(pathsProvider.getLocalProgramTempDir(), "dbchangemarker.txt").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * At startup, see if there is a change marker file. If so, we have changes from last time. We'll want to
     * ask the user if they want to keep or abandon those changes.
     */
    private void findChangeMarkerFile() {
        if (new File(pathsProvider.getLocalProgramTempDir(), "dbchangemarker.txt").exists()) {
            if (hasMetadataChange) System.out.print("Found previous changes.\n");
            hasMetadataChange = true;
        }
    }

    private void deleteChangeMarkerFile() {
        File markerFile = new File(pathsProvider.getLocalProgramTempDir(), "dbchangemarker.txt");
        if (markerFile.exists()) {
            markerFile.delete();
        }
    }

    /**
     * Listener for metadata changes. On the first change, create a marker file that will persist if we crash
     * or the comptuter shuts down.
     */
    private final MetadataStore.DataChangeListener metadataChangeListener = events -> {
        if (!hasMetadataChange) {
            hasMetadataChange = true;
            writeMetadataChangeMarkerFile();
        }
    };

    public boolean hasChanges() {
        return hasMetadataChange || store.hasChanges() || getSandbox().hasChanges();
  }

  public void commitDbChanges() {
      if (hasMetadataChange || store.hasChanges()) {
          accessControl.commitDbChanges();
      } else {
          accessControl.discardDbChanges();
      }
      if (getSandbox().hasChanges()) {
          getSandbox().commit();
      } else {
          getSandbox().discard();
      }
      deleteChangeMarkerFile();
      if (!pathsProvider.isDropboxDb()) {
          try {
              CloudSync.requestSync(getProgramName() + "_DB");
          } catch (IOException e) {
              e.printStackTrace();
          }
      }

  }

    public void discardDbChanges() {
        accessControl.discardDbChanges();
        getSandbox().discard();
        deleteChangeMarkerFile();
    }

  public void closeDb() {
      if (!initialized) {
          throw new IllegalStateException("Can't close an un-opened database");
      }
      if (accessControl.getAccessStatus() != AccessStatus.none) {
          accessControl.discardDbChanges();
      }
  }

  public int getCurrentDbVersion() {
    return accessControl.getCurrentDbVersion();
  }

//  public String getCurrentZipFilename() {
//      return accessControl.getCurrentZipFilename();
//  }
//
//  public boolean isDbOpen() {
//      return accessControl != null && accessControl.getOpenStatus().isOpen();
//  }

  public AccessStatus getDbAccessStatus() {
      return accessControl != null ? accessControl.getAccessStatus() : AccessStatus.none;
  }

    //*********************************************************************************************
    // Visible categories

    public void writeCategoryFilter(Taxonomy taxonomy) {
        File catFile = new File(getProgramHomeDir(), Constants.CATEGORY_INCLUDELIST_FILENAME);
        File bakFile = new File(getProgramHomeDir(), Constants.CATEGORY_INCLUDELIST_FILENAME + ".bak");
        try {
            getSandbox().moveFile(catFile, bakFile);
        } catch (Exception ignored) {}
        File newFile = getSandbox().outputFile(catFile.toPath());
        CategoryFilter.writeCategoryFilter(newFile, taxonomy);
    }

    public CategoryFilter loadCategoryFilter() {
        File categoryFile = getSandbox().inputFile(new File(getProgramHomeDir(),
            Constants.CATEGORY_INCLUDELIST_FILENAME).toPath());
        return new CategoryFilter(categoryFile);
    }

    //*********************************************************************************************
    // Configuration Properties

    public void writeProps() {
        File propertiesFile = getSandbox().outputFile(pathsProvider.getProgramConfigFile().toPath());
        try {
            BufferedOutputStream out = new BufferedOutputStream(
                new FileOutputStream(propertiesFile));
            dbProperties.store(out, null);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write configuration file: "
                + propertiesFile.getName(), e);
        }
    }

    public long getCacheSizeInBytes() {
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

    public String getDescription() {
        String description = getProjectName();
        String value = dbProperties.getProperty(Constants.DESCRIPTION_PROP_NAME);
        if (StringUtils.isNotBlank(value)) {
            description = value;
        }
        return description;
    }

    public String getConfigLanguages() {
        String languages = "en(\"English\")";
        String value = dbProperties.getProperty(Constants.AUDIO_LANGUAGES);
        if (StringUtils.isNotBlank(value)) {
            try {
                languages = value;
            } catch (Exception e) {
                // ignore and use default value
            }
        }
        return languages;
    }

    public boolean isShouldPreCacheWav() {
        boolean preCacheWav = false;
        String value = dbProperties.getProperty(Constants.PRE_CACHE_WAV);
        if (value != null) {
            try {
                preCacheWav = Boolean.parseBoolean(value);
            } catch (Exception e) {
                // ignore and use default value
            }
        }
        return preCacheWav;
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
     * Switch to control leaving a zero-byte marker file in images, with a single copy of the actual
     * (audio) file. Saves significant space in multi-variant deployments.
     *
     * Default to false.
     *
     * @return true if audio files should be de-duplicated.
     */
    public boolean isDeDuplicateAudio() {
        String deDuplicateAudio = dbProperties.getProperty(Constants.DE_DUPLICATE_AUDIO);
        return deDuplicateAudio != null && deDuplicateAudio.equalsIgnoreCase("true");
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
     * Returns the list of native audio formats for this program. If no value is specified, the value is "a18".
     * @return a collection of strings.
     */
    public List<String> getNativeAudioFormats() {
        String formats = dbProperties.getProperty(Constants.NATIVE_AUDIO_FORMATS, AudioItemRepository.AudioFormat.A18.getFileExtension());
        return Arrays.stream(formats.split("[;, ]"))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
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

    public boolean getWarnForMissingGreetings() {
        String value = dbProperties.getProperty(Constants.WARN_FOR_MISSING_GREETINGS);
        return Boolean.parseBoolean(value);
    }

    public void setWarnForMissingGreetings(boolean warnForMissingGreetings) {
        dbProperties.setProperty(Constants.WARN_FOR_MISSING_GREETINGS, Boolean.toString(warnForMissingGreetings));
    }

    public String getProperty(String propertyName) {
        return getProperty(propertyName, null);
    }
    public String getProperty(String propertyName, String defaultValue) {
        return dbProperties.getProperty(propertyName, defaultValue);
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
            result = Arrays.stream(list.split("[, ]+"))
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


    //*********************************************************************************************
    // Configured languages

    public String getLanguageLabel(Locale locale) {
        return languageLabels.get(locale);
    }

    public String getLanguageLabel(String languagecode) {
        Locale locale = new Locale(languagecode);
        return languageLabels.get(locale);
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
            audioLanguages = new ArrayList<>();
            String languagesProperty = getConfigLanguages();
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

        if (!getProgramHomeDir().exists()) {
            if (GraphicsEnvironment.isHeadless()) {
                System.err.println("ACM database " + getProgramHomeDirName() + " not found. Aborting.");
                LOG.log(Level.SEVERE, "ACM database " + getProgramHomeDirName() + " not found. Aborting.");
            } else {
                JOptionPane.showMessageDialog(null, "ACM database " + getProgramHomeDirName()
                    + " is not found within Dropbox.\n\nBe sure that you have accepted the Dropbox invitation\nto share the folder"
                    + " by logging into your account at\nhttp://dropbox.com and click on the 'Sharing' link.\n\nShutting down.");
            }
            System.exit(1);
        }

        // like ~/Dropbox/ACM-UWR/config.properties
        File propertiesFile = getSandbox().inputFile(pathsProvider.getProgramConfigFile().toPath());
        if (propertiesFile.exists()) {
            try {
                BufferedInputStream in = new BufferedInputStream(
                    new FileInputStream(propertiesFile));
                dbProperties = new Properties();
                dbProperties.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Unable to load configuration file: "
                    + propertiesFile.getName(), e);
            }
        }

    }

    private void initializeRepositories() throws IOException {
        // Checked out, create the repository object.
        String user = ACMConfiguration.getInstance().getUserName();
        LOG.info(String.format(
                "  Home directory:                 %s\n" +
                "  Content repository:             %s\n" +
                "  Temp Database:                  %s\n" +
                "  Temp Repository (sandbox mode): %s\n" +
                "  user:                           %s\n" +
                "  UserRWAccess:                   %s\n" +
                "  online:                         %s\n",
            getProgramHomeDir(),
            getProgramContentDir(),
            getLocalTempDbDir(),
            pathsProvider.getSandboxDir(),
            user,
            Authenticator.getInstance().hasUpdatingRole(),
            AccessControl.isOnline()));

        repository = AudioItemRepositoryImpl.buildAudioItemRepository(this);
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
        //noinspection unchecked
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
            if (transaction.size() > 0) {
                transaction.commit();
            } else {
                transaction.rollback();
            }
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
