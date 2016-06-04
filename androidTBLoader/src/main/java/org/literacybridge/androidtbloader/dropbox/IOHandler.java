package org.literacybridge.androidtbloader.dropbox;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.literacybridge.androidtbloader.DeploymentPackage;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.db.TalkingBookDBHelper;
import org.literacybridge.androidtbloader.db.TalkingBookDBSchema;
import org.literacybridge.androidtbloader.db.TalkingBookDBSchema.DeploymentPackagesCursorWrapper;
import org.literacybridge.androidtbloader.db.TalkingBookDBSchema.DeploymentPackagesTable;
import org.literacybridge.core.ProgressListener;
import org.literacybridge.core.fs.DefaultTBFileSystem;
import org.literacybridge.core.fs.RelativePath;
import org.literacybridge.core.fs.TBFileSystem;
import org.literacybridge.core.fs.ZipUnzip;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IOHandler {
    private static final String TAG = IOHandler.class.getSimpleName();

    public static final String EXTERNAL_FILES_TYPE = "deploymentpackages";

    private static final RelativePath TB_LOADER_COMMUNITIES_WHITELIST_FILE
            = RelativePath.parse("LB-software/tb_loader_communities.json");
    public static final RelativePath PUBLISHED_SUB_PATH = RelativePath.parse("TB-Loaders/published");


    private TBFileSystem mDropboxFileSystem;
    private TBFileSystem mLocalFileSystem;

    private DownloadStatusListener mDownloadStatusListener;

    private final TBLoaderAppContext mAppContext;

    private final SQLiteDatabase mDatabase;

    private Map<String, DeploymentPackage> mDeploymentPackages;

    public IOHandler(Context context) {
        mAppContext = (TBLoaderAppContext) context.getApplicationContext();
        mDatabase = new TalkingBookDBHelper(mAppContext).getWritableDatabase();
        mLocalFileSystem = DefaultTBFileSystem.open(mAppContext.getExternalFilesDir(EXTERNAL_FILES_TYPE));
        mDeploymentPackages = new ConcurrentHashMap<>();
        readLocalDeploymentPackageInfos();
    }

    public synchronized void initDropbox() throws IOException {
        if (mDropboxFileSystem == null) {
            mDropboxFileSystem = new DropboxFileSystem(mAppContext.getDropboxConnecton().getApi(), "/");
        }
    }

    public void setDownloadStatusListener(DownloadStatusListener downloadStatusListener) {
        mDownloadStatusListener = downloadStatusListener;
    }

    public void download(DeploymentPackage existingDeploymentPackage, final ProgressListener progressListener) {
        try {
            Map<String, DeploymentPackage> remoteDeploymentPackages = loadRemoteDeploymentPackageInfos();
            DeploymentPackage remoteDeploymentPackage = remoteDeploymentPackages.get(existingDeploymentPackage.getProjectName());
            if (remoteDeploymentPackage != null) {
                RelativePath sourcePath = remoteDeploymentPackage.getRemotePath();
                RelativePath targetPath = remoteDeploymentPackage.getLocalPath();

                boolean success = false;
                try {
                    existingDeploymentPackage.setCurrentlyDownloading(true);
                    Log.d(TAG, "Downloading from " + sourcePath + " to " + new File(mLocalFileSystem.getRootPath(), targetPath.asString()) + ", size=" + mDropboxFileSystem.fileLength(sourcePath));
                    progressListener.updateProgress(0, "Downlading deployment package");
                    mLocalFileSystem.mkdirs(targetPath.getParent());
                    Log.d(TAG, "dir exists: " + mLocalFileSystem.fileExists(targetPath));
                    TBFileSystem.CopyFileProgressListener copyFileProgressListener = new TBFileSystem.CopyFileProgressListener() {
                        @Override
                        public void onProgressUpdate(long bytesRead, long bytesTotal) {
                            progressListener.updateProgress((int) (((double) bytesRead) / bytesTotal * 90.0d), "Downlading deployment package");
                        }
                    };
                    TBFileSystem.copy(mDropboxFileSystem, sourcePath, mLocalFileSystem, targetPath, copyFileProgressListener);
                    progressListener.updateProgress(90, "Unzipping deployment package");
                    unzip(mLocalFileSystem, targetPath, new RelativePath(targetPath.getParent(),
                            targetPath.getLastSegment().substring(0, targetPath.getLastSegment().length() - 4)));
                    progressListener.updateProgress(100, "Done");
                    Log.d(TAG, "Downloaded to " + targetPath + ", size=" + mLocalFileSystem.fileLength(targetPath));
                    success = true;
                } catch (Exception e) {
                    Log.d(TAG, "Ex", e);
                }


                existingDeploymentPackage.setCurrentlyDownloading(false);
                if (success) {
                    if (existingDeploymentPackage.getRevision().equals(remoteDeploymentPackage.getRevision())) {
                        updateDeploymentPackage(existingDeploymentPackage, remoteDeploymentPackage);
                    }

                    if (existingDeploymentPackage.getDownloadStatus() != DeploymentPackage.DownloadStatus.DOWNLOADED) {
                        existingDeploymentPackage.setDownloadStatus(DeploymentPackage.DownloadStatus.DOWNLOADED);
                        mDatabase.insert(DeploymentPackagesTable.NAME, null, TalkingBookDBSchema.getContentValues(existingDeploymentPackage));
                    } else {
                        mDatabase.update(DeploymentPackagesTable.NAME, TalkingBookDBSchema.getContentValues(existingDeploymentPackage),
                                DeploymentPackagesTable.Cols.PROJECT_NAME + " = ?", new String[]{existingDeploymentPackage.getProjectName()});
                    }

                    mDownloadStatusListener.onDownloadStatusChanged(existingDeploymentPackage);
                } else {
                    existingDeploymentPackage.setDownloadStatus(DeploymentPackage.DownloadStatus.DOWNLOAD_FAILED);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error while downloading deployment package", e);
        }
    }

    public List<DeploymentPackage> getDeploymentPackageInfos(boolean refreshRemote) {
        if (refreshRemote) {
            try {
                Map<String, DeploymentPackage> remoteDeploymentPackages = loadRemoteDeploymentPackageInfos();

                for (DeploymentPackage p : remoteDeploymentPackages.values()) {
                    DeploymentPackage existing = mDeploymentPackages.get(p.getProjectName());
                    if (existing != null) {
                        if (!existing.getRevision().equals(p.getRevision())) {
                            if (existing.getDownloadStatus() == DeploymentPackage.DownloadStatus.NEVER_DOWNLOADED
                                    || existing.getDownloadStatus() == DeploymentPackage.DownloadStatus.DOWNLOAD_FAILED) {
                                updateDeploymentPackage(existing, p);
                            } else {
                                existing.setUpdateAvailable(true);
                            }
                        }
                    } else {
                        mDeploymentPackages.put(p.getProjectName(), p);
                    }
                }

            } catch (IOException e) {
                Log.d(TAG, "Unable to load remote deployment packages", e);
            }
        }

        return new ArrayList<>(mDeploymentPackages.values());
    }

    private void updateDeploymentPackage(DeploymentPackage existing, DeploymentPackage update) {
        existing.setRevision(update.getRevision());
        existing.setExpiration(update.getExpiration());
        existing.setCommunitiesFilter(update.getCommunitiesFilter());
    }

    private void readLocalDeploymentPackageInfos() {
        DeploymentPackagesCursorWrapper cursor = queryCrimes(null, null);
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                DeploymentPackage p = cursor.getDeploymentPackage();
                p.setDownloadStatus(DeploymentPackage.DownloadStatus.DOWNLOADED);
                if (!mDeploymentPackages.containsKey(p.getProjectName())) {
                    mDeploymentPackages.put(p.getProjectName(), p);
                }
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }
    }

    private DeploymentPackagesCursorWrapper queryCrimes(String whereClause, String[] whereArgs) {
        Cursor cursor = mDatabase.query(
                DeploymentPackagesTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null
        );

        return new DeploymentPackagesCursorWrapper(cursor);
    }

    private Map<String, DeploymentPackage> loadRemoteDeploymentPackageInfos() throws IOException {
        initDropbox();

        final Map<String, DeploymentPackage> deploymentPackages = new HashMap<>();

        final Set<String> availableProjects = new HashSet<>();
        for (String project : mDropboxFileSystem.list(RelativePath.EMPTY, new TBFileSystem.FilenameFilter() {
            @Override
            public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
                return name.startsWith("ACM-");
            }
        })) {
            availableProjects.add(project);

        }

        Log.d(TAG, "Projects found: " + availableProjects.toString());

        PreferenceManager.getDefaultSharedPreferences(mAppContext);
        String myDeviceId = PreferenceManager.getDefaultSharedPreferences(mAppContext)
                .getString("pref_tbloader_device_id", null);

        try (InputStream in = mDropboxFileSystem.openFileInputStream(TB_LOADER_COMMUNITIES_WHITELIST_FILE)) {
            try {
                JSONObject whitelist = (JSONObject) new JSONParser().parse(new InputStreamReader(in));
                for (Object deviceId : whitelist.keySet()) {
                    if (deviceId.equals(myDeviceId)) {
                        JSONArray projectWhitelist = (JSONArray) whitelist.get(deviceId);
                        Log.d(TAG, "whitelist: " + projectWhitelist.toString());
                        for (Object p : projectWhitelist.toArray()) {
                            JSONObject project = (JSONObject) p;
                            for (Object projectName : project.keySet()) {
                                if (availableProjects.contains(projectName)) {
                                    JSONArray communities = (JSONArray) project.get(projectName);
                                    Set<String> communitiesFilterSet = new HashSet<>();
                                    for (Object community : communities.toArray()) {
                                        communitiesFilterSet.add(community.toString());
                                    }

                                    RelativePath projectPath = RelativePath.concat(
                                            RelativePath.parse(projectName.toString()),
                                            PUBLISHED_SUB_PATH);

                                    String[] revFile = mDropboxFileSystem.list(projectPath,
                                            new TBFileSystem.FilenameFilter() {
                                                @Override
                                                public boolean accept(TBFileSystem fs, RelativePath dir, String name) {
                                                    return name.toLowerCase().endsWith(".rev");
                                                }
                                            });
                                    if (revFile.length != 1) {
                                        throw new IOException("More than one rev file found!");
                                    }


                                    String rev = revFile[0].substring(0, revFile[0].length() - 4);
                                    Log.d(TAG, "rev: " + rev);

                                    deploymentPackages.put(projectName.toString(),
                                            new DeploymentPackage(projectName.toString(),
                                                    rev,
                                                    null,
                                                    communitiesFilterSet,
                                                    DeploymentPackage.DownloadStatus.NEVER_DOWNLOADED));
                                    Log.d(TAG, "added: " + deploymentPackages);
                                }
                            }
                        }
                    }

                }
            } catch (ParseException e) {
                Log.d(TAG, "ParseException", e);
            }
        }

        Log.d(TAG, deploymentPackages.toString());

        return deploymentPackages;
    }

    public long getDownloadedSizeInBytes(DeploymentPackage image) {
        try {
            return getSizeInBytes(mLocalFileSystem, image.getLocalUnzipPath());
        } catch (IOException e) {
            return 0;
        }
    }

    private static long getSizeInBytes(TBFileSystem fs, RelativePath path) throws IOException {
        if (fs.isDirectory(path)) {
            long sum = 0;
            for (String f : fs.list(path)) {
                sum += getSizeInBytes(fs, new RelativePath(path, f));
            }
            return sum;
        }

        return fs.fileLength(path);
    }

    // TODO(michael): implement zip/unzip for RelativePath
    private static void unzip(TBFileSystem fs, RelativePath zipFile, RelativePath targetPath) throws IOException {
        fs.mkdirs(targetPath);
        File sourcePathFile = new File(fs.getRootPath(), zipFile.asString());
        File targetTDir = new File(fs.getRootPath(), targetPath.asString());
        ZipUnzip.unzip(sourcePathFile, targetTDir);
    }

//    public void store(ACMDatabaseInfo.DeploymentPackage image) {
//        File localBaseDir = getLocalDownloadPath(image);
//        boolean success = localBaseDir.exists() || localBaseDir.mkdirs();
//        if (!success) {
//            image.setStatus(Status.FailedDownload);
//            Log.d("download", "Failed to create directory " + localBaseDir.getAbsolutePath());
//            return;
//        }
//        image.setStatus(Status.Downloading);
//
//        try {
//            Entry entry = mApi.metadata(image.getZipFileDropBoxPath(), 1000, null, true, null);
//            store(entry, localBaseDir);
//            //unzipContentFile(image);
//            getSuccessFile(image.getLocalZipFile()).createNewFile();
//            image.setStatus(Status.Downloaded);
//        } catch (DropboxException e) {
//            image.setStatus(Status.FailedDownload);
//            Log.d("download", "Failed", e);
//        } catch (IOException e) {
//            image.setStatus(Status.FailedDownload);
//            Log.d("download", "Failed", e);
//        }
//    }

//    private void unzipContentFile(ACMDatabaseInfo.DeploymentPackage image) throws IOException {
//        if (!image.getLocalZipFile().exists()) {
//            throw new FileNotFoundException(image.getLocalZipFile().getAbsolutePath());
//        }
//        DiskUtils.unzip(image.getLocalZipFile());
//        if (!image.getLocalContentFolder().exists() || !image.getLocalContentFolder().isDirectory()) {
//            throw new IOException("Failed to unzip content file for image " + image);
//        }
//    }

//    private void store(Entry entry, File localDir) throws DropboxException, IOException {
//        Log.d("michael", "store " + entry.path);
//
//        File file = new File(localDir, entry.fileName());
//
//        if (entry.isDir) {
//            Log.d("michael", "store dir " + entry.path);
//            file.mkdirs();
//            for (Entry ent : entry.contents) {
//                Entry sub = mApi.metadata(ent.path, 1000, null, true, null);
//                store(sub, file);
//            }
//        } else {
//            OutputStream out = null;
//            File successFile = getSuccessFile(file);
//            if (successFile.exists()) {
//                successFile.delete();
//            }
//
//            try {
//                out = new BufferedOutputStream(new FileOutputStream(file));
//                mApi.getFile(entry.path, null, out, null);
//                Log.d("michael",
//                        "downloaded " + entry.path + " to " + file.getAbsolutePath());
//            } finally {
//                if (out != null) {
//                    try {
//                        out.close();
//                    } catch (IOException e) {
//                        // ignore
//                    }
//                }
//            }
//        }
//    }

    private File getSuccessFile(File file) {
        return new File(file.getParent(), file.getName() + ".success");
    }

    public interface DownloadStatusListener {
        void onDownloadStatusChanged(DeploymentPackage deploymentPackage);
    }

//    private void refreshLocal() {
//        Log.d("REFRESH", "Refreshing DB list from local disk.");
//
//        File databaseFolder = new File(filesDir, "tbloaders");
//        if (databaseFolder.exists()) {
//            File[] files = databaseFolder.listFiles();
//            if (files != null) {
//                for (File db : files) {
//                    if (db.isDirectory() && ACMDatabaseInfo.isACMDatabaseFolder(db.getName())) {
//                        ACMDatabaseInfo dbInfo = new ACMDatabaseInfo(db.getName());
//                        for (File image : db.listFiles()) {
//                            String imageName = image.getName();
//                            ACMDatabaseInfo.DeploymentPackage deviceImage =
//                                    new ACMDatabaseInfo.DeploymentPackage(dbInfo, imageName, getSizeInBytes(image), image);
//                            if (getSuccessFile(new File(image, ACMDatabaseInfo.DeploymentPackage.getZipFileName(imageName))).exists()) {
//                                deviceImage.setStatus(Status.Downloaded);
//                            } else {
//                                deviceImage.setStatus(Status.FailedDownload);
//                            }
//                            dbInfo.addDeviceImage(deviceImage);
//                        }
//                        if (dbInfo.getDeviceImages().size() > 0 && !databaseInfos.containsKey(dbInfo.getName())) {
//                            databaseInfos.put(dbInfo.getName(), dbInfo);
//                        }
//                    }
//                }
//            }
//        }
//    }

//    public void refresh() {
//        databaseInfos.clear();
//
//        try {
//            Entry dirent = mApi.metadata("/", 1000, null, true, null);
//
//            for (Entry ent : dirent.contents) {
//                if (ent.isDir && ACMDatabaseInfo.isACMDatabaseFolder(ent.fileName())) {
//                    ACMDatabaseInfo dbInfo = new ACMDatabaseInfo(ent.fileName());
//                    Entry tbloaderDir = null;
//                    try {
//                        tbloaderDir = mApi.metadata(dbInfo.getTBLoadersDropBoxPath(), 1000, null, true, null);
//                    } catch (DropboxException e) {
//                        // skip this folder
//                    }
//                    if (tbloaderDir != null) {
//                        for (Entry file : tbloaderDir.contents) {
//                            if (file.fileName().endsWith(".rev")) {
//                                Log.d("Dropbox", "Found " + file.fileName());
//                                String imageName = file.fileName().substring(0, file.fileName().length() - 4);
//                                String zipFilePath = tbloaderDir.path + ACMDatabaseInfo.DeploymentPackage.getZipFileName(imageName);
//                                Log.d("Dropbox", zipFilePath);
//                                try {
//                                    Entry zipFile = mApi.metadata(zipFilePath, 1000, null, true, null);
//                                    if (zipFile != null) {
//                                        ACMDatabaseInfo.DeploymentPackage deviceImage =
//                                                new ACMDatabaseInfo.DeploymentPackage(dbInfo, imageName, zipFile.bytes,
//                                                        getLocalDownloadPath(dbInfo.getName(), imageName));
//                                        File localZipFile = deviceImage.getLocalZipFile();
//                                        if (localZipFile.exists()) {
//                                            if (getSuccessFile(localZipFile).exists()) {
//                                                deviceImage.setStatus(Status.Downloaded);
//                                            } else {
//                                                deviceImage.setStatus(Status.FailedDownload);
//                                            }
//                                        } else {
//                                            deviceImage.setStatus(Status.NotDownloaded);
//                                        }
//                                        dbInfo.addDeviceImage(deviceImage);
//                                    }
//                                } catch (DropboxException e) {
//                                    Log.d("Dropbox", "Exception", e);
//                                    // ignore this image
//                                }
//                            }
//                        }
//                        if (dbInfo.getDeviceImages().size() > 0) {
//                            databaseInfos.put(dbInfo.getName(), dbInfo);
//                        }
//                    }
//                }
//            }
//
//        } catch (DropboxException e) {
//            Log.d("Dropbox", "Failed to load DBs from Dropbox.", e);
//        }
//
//        refreshLocal();
//    }
}
