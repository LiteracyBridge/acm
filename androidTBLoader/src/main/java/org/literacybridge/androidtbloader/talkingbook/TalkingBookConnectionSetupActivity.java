package org.literacybridge.androidtbloader.talkingbook;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;

import org.literacybridge.androidtbloader.TBLoaderAppContext;

import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

public class TalkingBookConnectionSetupActivity extends Activity {
    private static final String TAG = TalkingBookConnectionSetupActivity.class.getSimpleName();
    private static final int READ_REQUEST_CODE = 1001;
    private static final String EXPLICIT_REQUEST_DEFAULT_PERMISSION
            = "org.literacybridge.androidtbloader.request_default_permission";

    public static Intent newIntent(Context parent, boolean requestDefaultPermission) {
        Intent intent = new Intent(parent, TalkingBookConnectionSetupActivity.class);
        intent.putExtra(EXPLICIT_REQUEST_DEFAULT_PERMISSION, requestDefaultPermission);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final TalkingBookConnectionManager talkingBookConnectionManager =
                ((TBLoaderAppContext) getApplicationContext()).getTalkingBookConnectionManager();

        talkingBookConnectionManager.setUsbWatcherDisabled(true);

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(getIntent().getBooleanExtra(EXPLICIT_REQUEST_DEFAULT_PERMISSION, false));
        dialog.setTitle("Waiting for Talking Book");
        dialog.setMessage("Waiting for a Talking Book to connect. In the Dialog that appears " +
                "please find the Talking Book and select it. You only have to do this once.");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        final AtomicBoolean canceled = new AtomicBoolean(false);

        if (getIntent().getBooleanExtra(EXPLICIT_REQUEST_DEFAULT_PERMISSION, false)) {
            dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    canceled.set(true);
                    dialog.cancel();
                }
            });
        }
        dialog.show();


        new Thread(new Runnable() {
            // @TODO: replace this with CountUpTimer.
            // Provides a count of the elapsed time as a connection is established to the Talking Book.
            // Gives the user something to see, and makes the UI seem more alive.
            long startTimeMillis = Calendar.getInstance().getTimeInMillis();
            CountDownTimer timer = new CountDownTimer(60000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            long delta = Calendar.getInstance().getTimeInMillis() - startTimeMillis;
                            dialog.setMessage(String.format("Establishing connection to Talking Book [%d]...", delta / 1000));
                        }
                    });
                }

                @Override
                public void onFinish() {

                }
            };


            @Override
            public void run() {
                while (!talkingBookConnectionManager.isDeviceConnected() && !canceled.get()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (canceled.get()) {
                    timer.cancel();
                    finish();
                }

                timer.start();

                while (!talkingBookConnectionManager.isDeviceMounted() && !canceled.get()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (canceled.get()) {
                    timer.cancel();
                    finish();
                }

                if (!talkingBookConnectionManager.hasPermission()) {
                    dialog.dismiss();
                    requestPermission();
                } else {
                    while (talkingBookConnectionManager.canAccessConnectedDevice() == null && !canceled.get()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    timer.cancel();
                    dialog.dismiss();
                    finish();
                }

            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        final TalkingBookConnectionManager talkingBookConnectionManager =
                ((TBLoaderAppContext) getApplicationContext()).getTalkingBookConnectionManager();

        talkingBookConnectionManager.setUsbWatcherDisabled(false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri baseURI = resultData.getData();
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(baseURI, flags);

            final TalkingBookConnectionManager talkingBookConnectionManager =
                    ((TBLoaderAppContext) getApplicationContext()).getTalkingBookConnectionManager();

            talkingBookConnectionManager.addPermission(baseURI);

            if (talkingBookConnectionManager.canAccessConnectedDevice() == null) {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle("Access Permission Setup Failed")
                        .setMessage("The connected device can not be accessed. ")
                        .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TalkingBookConnectionSetupActivity.this.finish();
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                TalkingBookConnectionSetupActivity.this.finish();
                            }
                        })
                        .create();

                dialog.show();
            } else if (getIntent().getBooleanExtra(EXPLICIT_REQUEST_DEFAULT_PERMISSION, false)) {
                if (!talkingBookConnectionManager.hasDefaultPermission()) {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setCancelable(false)
                            .setTitle("Default Access Permission Setup Failed")
                            .setMessage("The connected Talking Book does not have the default volume ID "
                            + "and can therefore not be used to setup the default Talking Book access permissions.")
                            .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    TalkingBookConnectionSetupActivity.this.finish();
                                }
                            })
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    TalkingBookConnectionSetupActivity.this.finish();
                                }
                            })
                            .create();

                    dialog.show();
                } else {
                    finish();
                }
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    private void requestPermission() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

}
