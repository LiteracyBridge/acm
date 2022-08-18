package org.literacybridge.acm.config;

import com.amazonaws.services.s3.model.S3ObjectSummary;
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
import org.literacybridge.core.spec.LanguageLabelProvider;
import org.literacybridge.core.spec.ProgramSpec;

import javax.swing.JOptionPane;
import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.literacybridge.acm.Constants.ALLOW_PACKAGE_CHOICE;
import static org.literacybridge.acm.cloud.ProjectsHelper.PROGSPEC_ETAGS_FILE_NAME;
import static org.literacybridge.acm.store.MetadataSpecification.DC_LANGUAGE;

public class DBConfiguration {
  private static final Logger LOG = Logger.getLogger(DBConfiguration.class.getName());

  private Properties dbProperties;
  private final PathsProvider pathsProvider;

  private boolean initialized = false;
  private LanguageLabelProvider languageLabelProvider = null;

  private AudioItemRepositoryImpl repository;
  private MetadataStore store;

  private Sandbox sandbox = null;
  private boolean sandboxed;
  private boolean syncFailure;

  private AccessControl accessControl;

    public DBConfiguration(PathsProvider pathsProvider) {
        this.pathsProvider = pathsProvider;
        if (!pathsProvider.isDropboxDb()) {
            System.out.printf("Program %s uses %s for storage.\n", this.pathsProvider.getProgramId(), this.pathsProvider.isDropboxDb() ? "Dropbox" : "S3");
        }
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
      return pathsProvider.getProgramId();
  }

    /**
     * Gets the program id of the program in the ACM database.
     * @return the program id.
     */
  public String getProgramId() {
      return pathsProvider.getProgramId();
  }

  /**
   * Gets a File representing global (ie, Dropbox) ACM directory.
   * Like ~/Dropbox/ACM-${programId} or ~/Amplio/acm-dbs/${programId}
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

          getLanguageLabelProvider();

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
            //noinspection ResultOfMethodCallIgnored
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
            //noinspection ResultOfMethodCallIgnored
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
      AccessControlResolver.UpdateDbStatus closeResult;
      if (hasMetadataChange || store.hasChanges()) {
          closeResult = accessControl.commitDbChanges();
      } else {
          closeResult = accessControl.discardDbChanges();
      }
      if (closeResult != AccessControlResolver.UpdateDbStatus.ok) {
          return;
      }
      if (getSandbox().hasChanges()) {
          getSandbox().commit();
      } else {
          getSandbox().discard();
      }
      deleteChangeMarkerFile();
      if (!pathsProvider.isDropboxDb()) {
          try {
              CloudSync.requestSync(getProgramId());
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
            getDbProperties().store(out, null);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write configuration file: "
                + propertiesFile.getName(), e);
        }
    }

    public long getCacheSizeInBytes() {
        long size = Constants.DEFAULT_CACHE_SIZE_IN_BYTES;
        String value = getDbProperties().getProperty(Constants.CACHE_SIZE_PROP_NAME);
        if (value != null) {
            try {
                size = Long.parseLong(value);
            } catch (NumberFormatException e) {
                // ignore and use default value
            }
        }
        return size;
    }

    public String getFriendlyName() {
        String description = getProjectName();
        String value = getDbProperties().getProperty(Constants.FRIENDLY_NAME_PROP_NAME);
        if (StringUtils.isNotBlank(value)) {
            description = value;
        } else {
            value = getDbProperties().getProperty(Constants.DESCRIPTION_PROP_NAME);
            if (StringUtils.isNotBlank(value)) {
                description = value;
            }
        }
        return description;
    }

    public String getConfigLanguages() {
        String languages = "en(\"English\")";
        String value = getDbProperties().getProperty(Constants.AUDIO_LANGUAGES);
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
        String value = getDbProperties().getProperty(Constants.PRE_CACHE_WAV);
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
        String strictNaming = getDbProperties().getProperty(Constants.STRICT_DEPLOYMENT_NAMING);
        return strictNaming == null || !strictNaming.equalsIgnoreCase("false");
    }

    public boolean isUserFeedbackHidden() {
        String userFeedbackHidden = getDbProperties().getProperty(Constants.USER_FEEDBACK_HIDDEN);
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
        String deDuplicateAudio = getDbProperties().getProperty(Constants.DE_DUPLICATE_AUDIO);
        return deDuplicateAudio != null && deDuplicateAudio.equalsIgnoreCase("true");
    }

    /**
     * If true, add a toolbar button for configuration. Default is false; override in properties.config.
     * @return true if we should add a toolbar button for configuration.
     */
    public boolean configurationDialog() {
        String configurable = getDbProperties().getProperty(Constants.CONFIGURATION_DIALOG);
        return ACMConfiguration.getInstance().isShowConfiguration() ||
            (configurable != null && configurable.equalsIgnoreCase("true"));
    }

    /**
     * Returns the list of native audio formats for this program. If no value is specified, the value is "a18".
     * @return a collection of strings.
     */
    public List<String> getNativeAudioFormats() {
        String formats = getDbProperties().getProperty(Constants.NATIVE_AUDIO_FORMATS, AudioItemRepository.AudioFormat.A18.getFileExtension());
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
        String value = getDbProperties().getProperty(Constants.FUZZY_THRESHOLD);
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
        getDbProperties().setProperty(Constants.FUZZY_THRESHOLD, Integer.toString(threshold));
    }

    public boolean isPackageChoice() {
        String valStr = getDbProperties().getProperty(ALLOW_PACKAGE_CHOICE, "FALSE");
        return Boolean.parseBoolean(valStr);
    }

    public void setIsPackageChoice(boolean isPackageChoice) {
        getDbProperties().setProperty(Constants.ALLOW_PACKAGE_CHOICE, Boolean.toString(isPackageChoice));
    }

    public boolean getWarnForMissingGreetings() {
        String value = getDbProperties().getProperty(Constants.WARN_FOR_MISSING_GREETINGS);
        return Boolean.parseBoolean(value);
    }

    public void setWarnForMissingGreetings(boolean warnForMissingGreetings) {
        getDbProperties().setProperty(Constants.WARN_FOR_MISSING_GREETINGS, Boolean.toString(warnForMissingGreetings));
    }

    public boolean isForceWavConversion() {
        String value = getDbProperties().getProperty(Constants.FORCE_WAV_CONVERSION);
        return Boolean.parseBoolean(value);
    }

    public void setForceWavConversion(boolean isForceWavConversion) {
        getDbProperties().setProperty(Constants.FORCE_WAV_CONVERSION, Boolean.toString(isForceWavConversion));
    }

    public Boolean hasTbV2Devices() {
        String value = getDbProperties().getProperty(Constants.HAS_TBV2_DEVICES);
        return Boolean.parseBoolean(value);
    }

    public void setHasTbV2Devices(boolean hasTbV2Devices) {
        getDbProperties().setProperty(Constants.HAS_TBV2_DEVICES, Boolean.toString(hasTbV2Devices));
    }

    public String getProperty(String propertyName) {
        return getProperty(propertyName, null);
    }
    public String getProperty(String propertyName, String defaultValue) {
        return getDbProperties().getProperty(propertyName, defaultValue);
    }

    /**
     * The configured value of "interested parties" for events in the ACM. This should
     * be a list of email addresses, separated by commas.
     * @return a possibly empty list of email addresses (not validated in any way).
     */
    public Collection<String> getNotifyList() {
        Set<String> result;
        String list = getDbProperties().getProperty(Constants.NOTIFY_LIST);
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
        getDbProperties().setProperty(Constants.NOTIFY_LIST, String.join(", ", list));
    }


    //*********************************************************************************************
    // Configured languages

    public String getLanguageLabel(Locale locale) {
        return getLanguageLabelProvider().getLanguageLabel(locale);
    }

    public String getLanguageLabel(String languagecode) {
        return getLanguageLabelProvider().getLanguageLabel(languagecode);
    }

    private synchronized LanguageLabelProvider getLanguageLabelProvider() {
        if (languageLabelProvider == null) {
            String languagesProperty = getConfigLanguages();
            languageLabelProvider = new LanguageLabelProvider(languagesProperty);
        }
        return languageLabelProvider;
    }

    public List<Locale> getAudioLanguages() {
        return getLanguageLabelProvider().getAudioLanguages();
    }

    /**
     * Gets the list of language codes defined in the config file, as language codes, not as locales.
     * @return The language codes from config.properties.
     */
    public List<String> getAudioLanguageCodes() {
        return getAudioLanguages()
            .stream()
            .map(locale -> {
                String l = locale.getLanguage();
                if (StringUtils.isNotBlank(locale.getCountry())) {
                    l += '-' + locale.getCountry().toLowerCase();
                }
                return l;
            })
            .collect(Collectors.toList());
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

        if (ACMConfiguration.getInstance().isForceSandbox()) {
            setSandboxed(true);
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
        MetadataValue<RFC3066LanguageCode> abstractMetadataLanguageCode = new MetadataValue<>(
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

    /**
     * Reads the config.properties file for the program. Caches for next time.
     * @return the Properties object from the config.properties file.
     */
    private Properties getDbProperties() {
        if (dbProperties == null) {
            // like ~/Dropbox/ACM-UWR/config.properties
            File propertiesFile = getSandbox().inputFile(pathsProvider.getProgramConfigFile().toPath());
            if (propertiesFile.exists()) {
                try {
                    BufferedInputStream in = new BufferedInputStream(
                        new FileInputStream(propertiesFile));
                    Properties props = new Properties();
                    props.load(in);
                    dbProperties = props;
                } catch (IOException e) {
                    throw new RuntimeException("Unable to load configuration file: "
                        + propertiesFile.getName(), e);
                }
            }
        }
        return dbProperties;
    }

    private ProgramSpec programSpec;

    /**
     * Discards the cached progspec. The next read of the progspec will refresh from S3 if necessary (and we're online).
     */
    public void clearProgramSpecCache() {
        programSpec = null;
    }

    /**
     * Gets the current program spec. If there is a cached spec, return it. If not, load the spec. If online
     * check that the local copy is up-to-date.
     * @return the ProgramSpec.
     */
    public ProgramSpec getProgramSpec() {
        if (programSpec == null)  {
            File programSpecDir = pathsProvider.getProgramSpecDir();
            if (Authenticator.getInstance().isAuthenticated()) {
                List<S3ObjectSummary> needDownload = findObsoleteProgspecFiles();
                if (needDownload.size() > 0) {
                    downloadProgSpecFiles(needDownload);
                }
            }

            programSpec = new ProgramSpec(filenames -> {
                for (String filename : filenames) {
                    File csvFile = new File(programSpecDir, filename);
                    if (sandbox.exists(csvFile)) {
                        try {
                            return sandbox.fileInputStream(csvFile);
                        } catch (FileNotFoundException ignored) {
                            // ignore -- this shouldn't happen; we just checked for existance.
                        }
                    }
                }
                return null;
            });
        }
        return programSpec;
    }

    /**
     * Downloads the out-of-date progspec files parts, as determined by findObsoleteProgspecFiles()
     * @param toDownload: a list of S3ObjectSummaries of the objects to be downloaded.
     */
    private void downloadProgSpecFiles(List<S3ObjectSummary> toDownload) {
        boolean anyDownloaded = false;
        int prefixLen = getProgramId().length() + 1; // Program id + trailing "/"
        Properties localProgspec = getProgSpecETags();
        for (S3ObjectSummary os : toDownload) {
            File progSpecFile = new File(pathsProvider.getProgramSpecDir(), os.getKey().substring(prefixLen));
            boolean downloaded = Authenticator.getInstance()
                .getProjectsHelper()
                .downloadProgSpecFile(os.getKey(), os.getETag(), getSandbox().outputFile(progSpecFile.toPath()));
            if (downloaded) {
                localProgspec.setProperty(os.getKey().substring(prefixLen), os.getETag());
                anyDownloaded = true;
            }
        }
        if (anyDownloaded) {
            Path eTagsPath = new File(pathsProvider.getProgramSpecDir(), PROGSPEC_ETAGS_FILE_NAME).toPath();

            try (BufferedOutputStream out = new BufferedOutputStream(getSandbox().fileOutputStream(eTagsPath))) {
                localProgspec.store(out, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Determine if any progspec files need to be refreshed, based on their S3 etags.
     * @return a list of the S3ObjectSummary objects for out-of-date files.
     */
    private List<S3ObjectSummary> findObsoleteProgspecFiles() {
        // Get the current local program specification state.
        Properties localProgspec = getProgSpecETags();

        // Compare the actual state to the state cached lcoally.
        Map<String, S3ObjectSummary> currentProgspec = Authenticator.getInstance().getProjectsHelper().getProgSpecInfo(getProgramId());
        List<S3ObjectSummary> result = new ArrayList<>();
        for (Map.Entry<String, S3ObjectSummary> e : currentProgspec.entrySet()) {
            // If we don't have the file locally, or if the eTag doesn't match, add to the result list.
            String localTag = localProgspec.containsKey(e.getKey()) ? localProgspec.get(e.getKey()).toString() : "";
            if (!e.getValue().getETag().equalsIgnoreCase(localTag))
                result.add(e.getValue());
        }
        return result;
    }

    /**
     * Loads the current progrspec etags.
     * @return The etags, in a properties object. {filename : etag}
     */
    private Properties getProgSpecETags() {
        // Get the current local program specification state.
        Properties localProgspec = new Properties();
        File eTagsFile = new File(pathsProvider.getProgramSpecDir(), PROGSPEC_ETAGS_FILE_NAME);
        if (getSandbox().exists(eTagsFile.toPath())) {
            try (BufferedInputStream is = new BufferedInputStream(getSandbox().fileInputStream(eTagsFile))) {
                localProgspec.load(is);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return localProgspec;
    }
}
