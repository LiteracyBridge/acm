package org.literacybridge.androidtbloader;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;
import org.literacybridge.androidtbloader.content.ContentManager;
import org.literacybridge.androidtbloader.talkingbook.TalkingBookConnectionManager;
import org.literacybridge.androidtbloader.uploader.UploadService;
import org.literacybridge.androidtbloader.util.Config;
import org.literacybridge.androidtbloader.util.OperationLogImpl;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.androidtbloader.util.S3Helper;
import org.literacybridge.core.fs.OperationLog;
import org.literacybridge.core.spec.ProgramSpec;

import java.io.File;

import static org.literacybridge.androidtbloader.util.PathsProvider.getProgramSpecDir;

/**
 * An "Application" class. Used to hold long lifetime objects.
 */
public class TBLoaderAppContext extends Application {
    private static TBLoaderAppContext sApplicationInstance;

    public static TBLoaderAppContext getInstance() {
        return sApplicationInstance;
    }

    private boolean mDebug = false;

    private ConnectivityManager mConnectivityManager;

    private TalkingBookConnectionManager mTalkingBookConnectionManager;
    private ContentManager mContentManager;
    private UploadService mUploadService;
    private Config mConfig;

    private String mProject;
    private ProgramSpec mProgramSpec;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplicationInstance = this;
        mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        PackageManager pacMan = getPackageManager();
        String pacName = getPackageName();
        ApplicationInfo appInfo=null;
        try {
            appInfo = pacMan.getApplicationInfo(pacName, 0);
            mDebug = appInfo != null && (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            if (mDebug) Toast.makeText(this, "Debugging package " + pacName, Toast.LENGTH_LONG).show();
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, "Could not find package " + pacName, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        S3Helper.init(this);
        PathsProvider.init(this);
        mTalkingBookConnectionManager = new TalkingBookConnectionManager(this);
        mContentManager = new ContentManager(this);
        mUploadService = new UploadService();
        mConfig = new Config(this);

        OperationLog.setImplementation(OperationLogImpl.getInstance());
        OperationLog.log("ApplicationStart").finish();
    }

    public boolean isDebug() {
        return mDebug;
    }

    public TalkingBookConnectionManager getTalkingBookConnectionManager() {
        return mTalkingBookConnectionManager;
    }

    public ContentManager getContentManager() {
        return mContentManager;
    }

    public UploadService getUploadService() {
        return mUploadService;
    }

    public boolean isCurrentlyConnected() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null
                && activeNetwork.isConnected();
        return isConnected;
    }

    public Config getConfig() {
        return mConfig;
    }

    public void setProject(String project) {
        if (project==null || !project.equals(mProject)) {
            mProject = project;
            mProgramSpec = null;
        }
    }
    public String getProject() {
        return mProject;
    }
    public ProgramSpec getProgramSpec() {
        if (mProgramSpec == null) {
            File progspecDir = getProgramSpecDir(mProject);
            mProgramSpec = new ProgramSpec(progspecDir);
        }
        return mProgramSpec;
    }
}
