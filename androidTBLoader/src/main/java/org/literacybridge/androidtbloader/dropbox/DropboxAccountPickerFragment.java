package org.literacybridge.androidtbloader.dropbox;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;

import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;

public class DropboxAccountPickerFragment extends Fragment {
    private static final String TAG = DropboxAccountPickerFragment.class.getSimpleName();

    private TextView mAccountNameTextView;
    private TextView mAccountEmailTextView;
    private Button mChangeAccountButton;

    private boolean mAuthenticationStarted;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_dropbox_account_picker, container, false);

        mAccountNameTextView = (TextView) v.findViewById(R.id.dropbox_account_name_text_view);
        mAccountEmailTextView = (TextView) v.findViewById(R.id.dropbox_account_email_text_view);
        mChangeAccountButton = (Button) v.findViewById(R.id.dropbox_switch_account_button);

        final DropboxConnection dropboxConnection =
                ((TBLoaderAppContext) getActivity().getApplicationContext()).getDropboxConnecton();

        mChangeAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dropboxConnection.clearAuthentication();
                doAuthentication();
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        doAuthentication();
    }

    private void doAuthentication() {
        DropboxConnection dropboxConnection =
                ((TBLoaderAppContext) getActivity().getApplicationContext()).getDropboxConnecton();

        if (!mAuthenticationStarted && !dropboxConnection.isAuthenticated()) {
            mAuthenticationStarted = true;
            dropboxConnection.connect(getActivity());
        } else {
            try {
                if (mAuthenticationStarted) {
                    mAuthenticationStarted = false;
                    dropboxConnection.resumeAuthentication();
                }

                if (dropboxConnection.isAuthenticated()) {
                    showAccountInfo();
                }
            } catch (DropboxException e) {
                Log.d(TAG, "Unable to connect to Dropbox.", e);
            }
        }
    }

    private void showAccountInfo() {
        final DropboxConnection dropboxConnection =
                ((TBLoaderAppContext) getActivity().getApplicationContext()).getDropboxConnecton();

        if (dropboxConnection.isAuthenticated()) {
            final ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Loading Dropbox account information. Please wait...");
            dialog.setIndeterminate(true);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    final DropboxAPI.Account accountInfo = dropboxConnection.getAccountInfo();

                    DropboxAccountPickerFragment.this.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (accountInfo != null) {
                                mAccountNameTextView.setText(accountInfo.displayName);
                                mAccountEmailTextView.setText(accountInfo.email);
                            }
                            dialog.cancel();
                        }
                    });
                }
            }).start();
        }
    }
}
