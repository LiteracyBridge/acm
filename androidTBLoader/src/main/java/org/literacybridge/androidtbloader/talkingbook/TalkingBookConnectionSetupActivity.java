package org.literacybridge.androidtbloader.talkingbook;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.util.CountUpTimer;
import org.literacybridge.core.fs.OperationLog;

public class TalkingBookConnectionSetupActivity extends Activity {
    private static final String TAG = TalkingBookConnectionSetupActivity.class.getSimpleName();
    private static final int READ_REQUEST_CODE = 1001;
    /**
     * This extra value is used to signal that the activity has been started from the preference
     * option to "request default permission".
     */
    private static final String EXPLICIT_REQUEST_DEFAULT_PERMISSION
            = "org.literacybridge.androidtbloader.request_default_permission";

    private boolean requestingDefaultPermission = false;
    private boolean cancelled = false;
    private OperationLog.Operation opLog;

    /**
     * Create an intent to launch this activity. This is only used in response to USB activity.
     * @param parent The application context.
     * @return the new intent.
     */
    static Intent newIntent(Context parent) {
        return new Intent(parent, TalkingBookConnectionSetupActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final TalkingBookConnectionManager talkingBookConnectionManager =
                ((TBLoaderAppContext) getApplicationContext()).getTalkingBookConnectionManager();
        requestingDefaultPermission = getIntent().getBooleanExtra(EXPLICIT_REQUEST_DEFAULT_PERMISSION, false);

        opLog = OperationLog.startOperation("TalkingBookConnectionSetupActivity");
        talkingBookConnectionManager.setUsbWatcherDisabled(true);

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        // If the dialog is launched from the settings item "request default permission", let it be
        // cancelable. Any other reason for launching, do not let it be cancelable.
        dialog.setCancelable(requestingDefaultPermission);
        if (requestingDefaultPermission) {
            opLog.put("requestingDefaultPermission", true);
            dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cancelled = true;
                    opLog.put("cancelled", true);
                    dialog.cancel();
                }
            });
        }
        dialog.setTitle("Waiting for Talking Book");
        // This message is only displayed until 'isDeviceMounted()' returns true. If a device is
        // *already* mounted, it will never actually be seen, because if a device was previously
        // mounted, this activity is started from the USB handler.
        dialog.setMessage("Waiting for a Talking Book to connect. In the Dialog that appears " +
                "please find the Talking Book and select it. You only have to do this once.");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);

        dialog.show();


        new Thread(new Runnable() {
            // Provides a count of the elapsed time as a connection is established to the Talking Book.
            // Gives the user something to see, and makes the UI seem more alive.
            CountUpTimer timer = new CountUpTimer(1000) {
                @Override
                public void onTick(final long elapsedTime) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.setMessage(String.format(getString(R.string.establishing_connection), elapsedTime / 1000));
                        }
                    });
                }
            };


            @Override
            public void run() {
                // Show the "Waiting for Talking Book to connect..." message.
                while (!talkingBookConnectionManager.isDeviceConnected() && !cancelled) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (cancelled) {
                    finish();
                    return;
                } else {
                    opLog.split("deviceConnected.time");
                }

                // Counts the seconds until the device is accessible.
                timer.start();

                // (After timer.tick...) show the "Establishing connection..." message.
                while (!talkingBookConnectionManager.isDeviceMounted() && !cancelled) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                if (cancelled) {
                    timer.cancel();
                    finish();
                    return;
                } else {
                    opLog.split("deviceMounted.time");
                }

                // Do we already have permission to this TB device?
                if (!talkingBookConnectionManager.hasPermission()) {
                    // No, let the user open the device through the OS.
                    timer.cancel();
                    dialog.dismiss();
                    requestPermission();
                } else {
                    // Yes. Wait for the device to be accessible. Can take a *long* time (15~20 seconds,
                    // maybe longer, depending on the Android device).
                    while (talkingBookConnectionManager.canAccessConnectedDevice() == null && !cancelled) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    if (!cancelled) {
                        opLog.split("deviceAccessible.time");
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
            opLog.split("requestedpermission.time");
            Uri baseURI = resultData.getData();
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(baseURI, flags);

            final TalkingBookConnectionManager talkingBookConnectionManager =
                    ((TBLoaderAppContext) getApplicationContext()).getTalkingBookConnectionManager();

            talkingBookConnectionManager.addPermission(baseURI);
            opLog.put("hasDefaultPermission", talkingBookConnectionManager.hasDefaultPermission());

            if (talkingBookConnectionManager.canAccessConnectedDevice() == null) {
                opLog.put("permissionRequestFailed", true);
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
            } else if (requestingDefaultPermission) {
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

    /**
     * Call this when your activity is done and should be closed.  The
     * ActivityResult is propagated back to whoever launched you via
     * onActivityResult().
     */
    @Override
    public void finish() {
        opLog.finish();
        super.finish();
    }

    private void requestPermission() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        opLog.put("requestPermission", true);
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

}
