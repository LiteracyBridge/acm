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
import org.literacybridge.androidtbloader.uploader.UploadManager;
import org.literacybridge.androidtbloader.util.Config;
import org.literacybridge.androidtbloader.util.OperationLogImpl;
import org.literacybridge.androidtbloader.util.PathsProvider;
import org.literacybridge.androidtbloader.util.S3Helper;
import org.literacybridge.core.fs.OperationLog;

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
    private UploadManager mUploadManager;
    private Config mConfig;

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
        mUploadManager = new UploadManager();
        mConfig = new Config(this);

        OperationLog.setImplementation(new OperationLogImpl());
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

    public UploadManager getUploadManager() {
        return mUploadManager;
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
}
