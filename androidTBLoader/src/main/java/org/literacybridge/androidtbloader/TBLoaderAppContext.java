package org.literacybridge.androidtbloader;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.literacybridge.androidtbloader.dropbox.DropboxConnection;
import org.literacybridge.androidtbloader.talkingbook.TalkingBookConnectionManager;

public class TBLoaderAppContext extends Application {
    private DropboxConnection mDropboxConnection;
    private TalkingBookConnectionManager mTalkingBookConnectionManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mDropboxConnection = new DropboxConnection(this);
        mTalkingBookConnectionManager = new TalkingBookConnectionManager(this);


    }

    public DropboxConnection getDropboxConnecton() {
        return mDropboxConnection;
    }

    public TalkingBookConnectionManager getTalkingBookConnectionManager() {
        return mTalkingBookConnectionManager;
    }
}
