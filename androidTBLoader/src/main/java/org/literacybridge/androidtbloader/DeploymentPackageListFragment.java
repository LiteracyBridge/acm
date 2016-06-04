package org.literacybridge.androidtbloader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import org.literacybridge.androidtbloader.dropbox.DropboxConnection;
import org.literacybridge.androidtbloader.dropbox.IOHandler;
import org.literacybridge.androidtbloader.talkingbook.TalkingBookConnectionManager;
import org.literacybridge.core.ProgressListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class DeploymentPackageListFragment extends Fragment implements IOHandler.DownloadStatusListener {
    private static final String TAG = DeploymentPackageListFragment.class.getSimpleName();

    private static final SimpleDateFormat EXPIRATION_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

    private TalkingBookConnectionManager mTalkingBookConnectionManager;
    private RadioButton mTalkingBookStatusRadioButton;
    private RadioButton mDropboxStatusRadioButton;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mDeploymentPackageRecyclerView;
    private SharedPreferences mSharedPreferences;
    private DeploymentPackageAdapter mAdapter;
    private IOHandler ioHandler;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        ioHandler = new IOHandler(getActivity());
        ioHandler.setDownloadStatusListener(this);
        mTalkingBookConnectionManager = ((TBLoaderAppContext) getActivity().getApplicationContext()).getTalkingBookConnectionManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deployment_package_list, container, false);
        mTalkingBookStatusRadioButton = (RadioButton) view.findViewById(R.id.connection_status_talking_book);
        mDropboxStatusRadioButton = (RadioButton) view.findViewById(R.id.connection_status_dropbox);
        mDeploymentPackageRecyclerView = (RecyclerView) view.findViewById(R.id.deployment_package_recycler_view);
        mDeploymentPackageRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefreshLayout);

        TBLoaderAppContext appContext = (TBLoaderAppContext) getActivity().getApplicationContext();

        final ConnectivityManager cm =
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final DropboxConnection dropboxConnection = appContext.getDropboxConnecton();


        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Refresh items
                updateUI(true);
            }
        });

        mTalkingBookConnectionManager
                .setTalkingBookConnectionEventListener(new TalkingBookConnectionManager.TalkingBookConnectionEventListener() {
                    @Override
                    public void onTalkingBookConnectEvent(final TalkingBookConnectionManager.TalkingBook connectedDevice) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateTBConnectionStatus(connectedDevice);
                            }
                        });
                    }

                    @Override
                    public void onTalkingBookDisConnectEvent() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                               updateTBConnectionStatus(null);
                            }
                        });
                    }
                });

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                boolean wasConnected = false;
                boolean firstRun = true;

                while (true) {
                    try {
                        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                        final boolean isConnected = activeNetwork != null
                                && activeNetwork.isConnected()
                                && dropboxConnection.isConnected();
                        final String accountName = isConnected
                                ? dropboxConnection.getAccountInfo().email : null;

                        final boolean triggerRefresh = firstRun || isConnected != wasConnected;

                        if (triggerRefresh) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (isConnected) {
                                        setConnectionStatus(mDropboxStatusRadioButton,
                                                getString(R.string.dropbox_connection_status_text, accountName),
                                                true);
                                    } else {
                                        if (dropboxConnection.isAuthenticated()) {
                                            setConnectionStatus(mDropboxStatusRadioButton,
                                                    getString(R.string.dropbox_not_connected_status_text),
                                                    false);
                                        } else {
                                            setConnectionStatus(mDropboxStatusRadioButton,
                                                    getString(R.string.dropbox_not_configured),
                                                    false);
                                        }
                                    }

                                    mSwipeRefreshLayout.setRefreshing(true);
                                    updateUI(true);
                                }
                            });
                        }

                        wasConnected = isConnected;
                        firstRun = false;
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        Log.d(TAG, "Error while checking connection status", e);
                    }
                }
            }
        });

        checkSettingsSetup();

        return view;
    }

    private void setConnectionStatus(RadioButton status, String text, boolean checked) {
        status.setText(text);
        status.setChecked(checked);
        if (checked) {
            status.setTextColor(getResources().getColor(R.color.material_deep_teal_200));
        } else {
            status.setTextColor(getResources().getColor(R.color.material_grey_600));
        }
    }

    private void updateTBConnectionStatus(TalkingBookConnectionManager.TalkingBook connectedDevice) {
        if (connectedDevice != null) {
            setConnectionStatus(mTalkingBookStatusRadioButton,
                    getString(R.string.talking_book_connection_status_text, connectedDevice.getSerialNumber()),
                    true);
        } else {
            setConnectionStatus(mTalkingBookStatusRadioButton,
                    getString(R.string.talking_book_not_connected_status_text),
                    false);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_deployment_package_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void checkSettingsSetup() {
        String myDeviceId = mSharedPreferences.getString("pref_tbloader_device_id", null);
        if (myDeviceId == null || myDeviceId.equals("")) {
            final EditText userIdEditText = new EditText(getActivity());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Setup TB Loader Device ID");
            builder.setMessage("Enter a TB Loader Device ID. Please talk to Bill to request one.");
            builder.setView(userIdEditText);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putString("pref_tbloader_device_id", userIdEditText.getText().toString());
                    editor.commit();
                }
            });
            builder.create().show();
        }
    }

    public void updateUI(boolean reloadRemotePackages) {
        if (mAdapter == null) {
            mAdapter = new DeploymentPackageAdapter();
            mDeploymentPackageRecyclerView.setAdapter(mAdapter);
        }

        if (reloadRemotePackages) {
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    mAdapter.setDeploymentPackages(ioHandler.getDeploymentPackageInfos(true));
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.notifyDataSetChanged();
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }

            });
        }

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                final TalkingBookConnectionManager.TalkingBook tb = mTalkingBookConnectionManager.canAccessConnectedDevice();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTBConnectionStatus(tb);
                    }
                });
            }

        });
    }

    @Override
    public void onDownloadStatusChanged(DeploymentPackage deploymentPackage) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private class DeploymentPackageHolder extends RecyclerView.ViewHolder {
        private DeploymentPackage mDeploymentPackage;

        private TextView mProjectTextView;
        private TextView mRevisionTextView;
        private TextView mExpirationTextView;

        private Button mDownloadButton;
        private Button mInstallButton;

        private ProgressBar mProgressBar;


        public DeploymentPackageHolder(View itemView) {
            super(itemView);

            mProjectTextView = (TextView) itemView.findViewById(R.id.list_item_deployment_package_project);
            mRevisionTextView = (TextView) itemView.findViewById(R.id.list_item_deployment_package_rev);
            mExpirationTextView = (TextView) itemView.findViewById(R.id.list_item_deployment_package_expiration);

            mDownloadButton = (Button) itemView.findViewById(R.id.list_item_deployment_package_download_button);
            mDownloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Download clicked!");

                    Executors.newSingleThreadExecutor().submit(new Runnable() {
                        @Override
                        public void run() {
                            ioHandler.download(mDeploymentPackage, new ProgressListener() {
                                @Override
                                public void updateProgress(final int progressPercent,
                                                           final String progressUpdate) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            setCurrentlyDownloading();
                                            mProgressBar.setProgress(progressPercent);
                                        }
                                    });
                                }

                                @Override
                                public void addDetail(String detail) {
                                    // nothing to do here
                                }
                            });
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setCurrentlyDownloading();
                                }
                            });
                        }
                    });
                }
            });

            mInstallButton = (Button) itemView.findViewById(R.id.list_item_deployment_package_install_button);
            mInstallButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Set<String> communitiesFilter = mDeploymentPackage.getCommunitiesFilter();
                    getCommunitiesDialog(mDeploymentPackage, communitiesFilter.toArray(new String[communitiesFilter.size()])).show();
                }
            });

            mProgressBar = (ProgressBar) itemView.findViewById(R.id.list_item_deployment_package_progress_spinner);
        }

        private void setCurrentlyDownloading() {
            if (mDeploymentPackage.isCurrentlyDownloading()) {
                mDownloadButton.setVisibility(View.INVISIBLE);
                mProgressBar.setVisibility(View.VISIBLE);
            } else {
                mDownloadButton.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        }

        private void bindDeploymentPackage(final DeploymentPackage deploymentPackage) {
            mDeploymentPackage = deploymentPackage;

            mProjectTextView.setText(deploymentPackage.getProjectName().substring(4)); // strip 'ACM-'
            mRevisionTextView.setText(getString(R.string.deployment_package_revision, mDeploymentPackage.getRevision()));
            final String expiration;
            if (deploymentPackage.hasExpiration()) {
                expiration = EXPIRATION_DATE_FORMAT.format(deploymentPackage.getExpiration());
            } else {
                expiration = getString(R.string.deployment_package_expiration_never);
            }
            mExpirationTextView.setText(getString(R.string.deployment_package_expiration, expiration));

            switch (deploymentPackage.getDownloadStatus()) {
                case NEVER_DOWNLOADED:
                    mDownloadButton.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mDownloadButton.setEnabled(true);
                    mDownloadButton.setText(getString(R.string.deployment_package_download));
                    mInstallButton.setEnabled(false);
                    break;
                case DOWNLOAD_FAILED:
                    mDownloadButton.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mDownloadButton.setEnabled(true);
                    mDownloadButton.setText(getString(R.string.deployment_package_download_retry));
                    mInstallButton.setEnabled(false);
                    break;
                case DOWNLOADED:
                    mDownloadButton.setVisibility(View.VISIBLE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    if (!deploymentPackage.isUpdateAvailable()) {
                        mDownloadButton.setEnabled(false);
                        mDownloadButton.setText(getString(R.string.deployment_package_download));
                        mInstallButton.setEnabled(mTalkingBookStatusRadioButton.isChecked());
                    } else {
                        mDownloadButton.setEnabled(true);
                        mDownloadButton.setText(getString(R.string.deployment_package_download_update));
                        mInstallButton.setEnabled(mTalkingBookStatusRadioButton.isChecked()
                                && mSharedPreferences.getBoolean("pref_allow_install_outdated", false));
                    }
                    break;
            }

            setCurrentlyDownloading();
        }
    }

    private class DeploymentPackageAdapter extends RecyclerView.Adapter<DeploymentPackageHolder> {
        private volatile List<DeploymentPackage> mDeploymentPackages;

        private void setDeploymentPackages(List<DeploymentPackage> deploymentPackages) {
            mDeploymentPackages = deploymentPackages;
        }

        @Override
        public DeploymentPackageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_deployment_package, parent, false);
            return new DeploymentPackageHolder(view);
        }

        @Override
        public void onBindViewHolder(DeploymentPackageHolder holder, int position) {
            holder.bindDeploymentPackage(mDeploymentPackages.get(position));
        }

        @Override
        public int getItemCount() {
            return mDeploymentPackages != null ? mDeploymentPackages.size() : 0;
        }
    }

    private Dialog getCommunitiesDialog(final DeploymentPackage deploymentPackage, final String[] communities) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.pick_community_dialog_title)
                .setItems(communities, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, final int which) {
                        getTBUpdateDialog(deploymentPackage, communities[which]).show();
                    }
                });
        return builder.create();
    }

    private Dialog getTBUpdateDialog(final DeploymentPackage deploymentPackage, final String community) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.fragment_talking_book_update_progress, null);
        TextView deploymentInfoTextView = (TextView) view.findViewById(R.id.fragment_tb_update_deployment_info);
        String projectName = deploymentPackage.getProjectName();
        if (projectName.startsWith("ACM-")) {
            projectName = projectName.substring(4);
        }

        final TextView statusTextView = (TextView) view.findViewById(R.id.update_talking_book_status_text_view);
        final ProgressBar statusProgressBar = (ProgressBar) view.findViewById(R.id.update_talking_book_progress_bar);

        deploymentInfoTextView.setText(getString(R.string.dialog_tb_update_deployment_info, projectName,
                deploymentPackage.getRevision(), community));
        builder.setView(view)
                .setCancelable(false)
                .setNegativeButton("Cancel", null);

        final Dialog dialog = builder.create();

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    ((TBLoaderAppContext) getActivity().getApplicationContext()).getTalkingBookConnectionManager()
                            .installDeploymentPackage(deploymentPackage, community, new ProgressListener() {
                                @Override
                                public void updateProgress(final int progressPercent,
                                                           final String progressUpdate) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            statusTextView.setText(progressUpdate);
                                            statusProgressBar.setProgress(progressPercent);
                                        }
                                    });
                                }

                                @Override
                                public void addDetail(String detail) {
                                    Log.d(TAG, "PROGRESS: " + detail);
                                }
                            });
                    dialog.dismiss();
                } catch (IOException e) {
                    Log.d(TAG, "Failed to install deployment", e);
                }
            }
        });


        return dialog;
    }
}
