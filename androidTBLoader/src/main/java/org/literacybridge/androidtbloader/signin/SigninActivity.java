/*
 *  Copyright 2013-2016 Amazon.com,
 *  Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Amazon Software License (the "License").
 *  You may not use this file except in compliance with the
 *  License. A copy of the License is located at
 *
 *      http://aws.amazon.com/asl/
 *
 *  or in the "license" file accompanying this file. This file is
 *  distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *  CONDITIONS OF ANY KIND, express or implied. See the License
 *  for the specific language governing permissions and
 *  limitations under the License.
 */

package org.literacybridge.androidtbloader.signin;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TextInputLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.NewPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.androidtbloader.R;
import org.literacybridge.androidtbloader.TBLoaderAppContext;
import org.literacybridge.androidtbloader.main.MainActivity;
import org.literacybridge.androidtbloader.util.Constants;

import java.util.Locale;
import java.util.Map;

public class SigninActivity extends AppCompatActivity {
    private final String TAG="TBL!:" + "SigninActivity";

    private final static int REGISTER_ACTIVITY_CODE = 1;
    private final static int CONFIRM_ACTIVITY_CODE = 2;
    private final static int FORGOT_PASSWORD_ACTIVITY_CODE = 3;
    private final static int MAIN_ACTIVITY_CODE = 4;
    private final static int MFA_ACTIVITY_CODE = 5;
    private final static int NEW_PASSWORD_ACTIVITY_CODE = 6;

    private Button mSignInButton;

    private NavigationView nDrawer;
    private DrawerLayout mDrawer;
    private ActionBarDrawerToggle mDrawerToggle;
    private Toolbar toolbar;
    private AlertDialog userDialog;
    private ProgressDialog waitDialog;
    private boolean mExplicitSignIn = false;

    // Screen fields
    private EditText inUsername;
    private EditText inPassword;
    private TextInputLayout inPasswordLayout;

    //Continuations
    private MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation;
    private ForgotPasswordContinuation forgotPasswordContinuation;
    private NewPasswordContinuation newPasswordContinuation;

    // User Details
    private String username;
    private String password;

    // Mandatory overrides first
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set toolbar for this screen
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle("Login");
        TextView main_title = (TextView) findViewById(R.id.main_toolbar_title);
        main_title.setText("");
        setSupportActionBar(toolbar);

        mSignInButton = (Button) findViewById(R.id.buttonLogIn);

        // Set navigation drawer for this screen
        mDrawer = (DrawerLayout) findViewById(R.id.signin_drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawer, toolbar, R.string.nav_drawer_open, R.string.nav_drawer_close);
        mDrawer.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        nDrawer = (NavigationView) findViewById(R.id.nav_view);
        setNavDrawer();

        // Initialize application
        initApp();
        UserHelper.initInstance(getApplicationContext(), Constants.cognitoConfig);
        // If we last authenticated with the fallback user pool, don't try to use any cached credentials.
        if (!((TBLoaderAppContext)getApplicationContext()).getConfig().isFallbackLogin()) {
            findCurrent();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        mSignInButton.setEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Open/Close the navigation drawer when menu icon is selected
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REGISTER_ACTIVITY_CODE:
                // Register user
                if(resultCode == RESULT_OK) {
                    String name = data.getStringExtra("name");
                    if (!name.isEmpty()) {
                        inUsername.setText(name);
                        inPassword.setText("");
                        inPassword.requestFocus();
                    }
                    String userPasswd = data.getStringExtra("password");
                    if (!userPasswd.isEmpty()) {
                        inPassword.setText(userPasswd);
                    }
                    if (!name.isEmpty() && !userPasswd.isEmpty()) {
                        // We have the user details, so login!
                        username = name;
                        password = userPasswd;
                        UserHelper.getInstance().getPool().getUser(username).getSessionInBackground(authenticationHandler);
                    }
                }
                break;
            case CONFIRM_ACTIVITY_CODE:
                // Confirm register user
                if(resultCode == RESULT_OK) {
                    String name = data.getStringExtra("name");
                    if (!name.isEmpty()) {
                        inUsername.setText(name);
                        inPassword.setText("");
                        inPassword.requestFocus();
                    }
                }
                break;
            case FORGOT_PASSWORD_ACTIVITY_CODE:
                // Forgot password
                if(resultCode == RESULT_OK) {
                    String newPass = data.getStringExtra("newPass");
                    String code = data.getStringExtra("code");
                    if (newPass != null && code != null) {
                        if (!newPass.isEmpty() && !code.isEmpty()) {
                            showWaitDialog("Setting new password...");
                            forgotPasswordContinuation.setPassword(newPass);
                            forgotPasswordContinuation.setVerificationCode(code);
                            forgotPasswordContinuation.continueTask();
                        }
                    }
                }
                break;
            case MAIN_ACTIVITY_CODE:
                // User
                if(resultCode == RESULT_OK) {
                    Log.d(TAG, "Back in Signin Activity.");
                    // Not sure about this. If one signs in, then goes back, should credentials still be there?
                    // If so, remove this.
                    clearInput();
                    // Did the "main" activity want us to exit the application?
                    boolean exitApplication = data.getBooleanExtra(Constants.EXIT_APPLICATION, false);
                    if(exitApplication) {
                        Log.d(TAG, String.format("Got an 'exitApplication'."));
                        onBackPressed();
                    }
                    // No, need to login again.
                    // Logout first?
                    boolean signout = data.getBooleanExtra(Constants.SIGNOUT, false);
                    if (signout) {
                        clearInput();
                        UserHelper.getInstance().getPool().getCurrentUser().signOut();
                        UserHelper.getInstance().setUserId("");
                        UserHelper.getInstance().getCredentialsProvider(getApplicationContext()).clear();
                        // Clears any fallback instance.
                        UserHelper.initInstance(getApplicationContext(), Constants.cognitoConfig);
                        ////////////////////////////////////////////////////////////////////////////
                        // @TODO: This is cheating; looking into the internals of Cognito.
                        try {
                            // Clear all cached tokens.
                            SharedPreferences csiCachedTokens =  getApplicationContext().getSharedPreferences("CognitoIdentityProviderCache", 0);

                            // Format "key" strings
                            String csiLastAuthUserKey =  String.format("CognitoIdentityProvider.%s.LastAuthUser", UserHelper.getInstance().getConfig().COGNITO_APP_CLIENT_ID);

                            SharedPreferences.Editor cacheEdit = csiCachedTokens.edit();
                            cacheEdit.remove(csiLastAuthUserKey);
                            cacheEdit.apply();
                        } catch (Exception e) {
                            // Logging exception, this is not a fatal error
                            Log.e(TAG, "Error while deleting from SharedPreferences");
                        }
                        ////////////////////////////////////////////////////////////////////////////
                    }
                }
                break;
            case MFA_ACTIVITY_CODE:
                //MFA
                closeWaitDialog();
                if(resultCode == RESULT_OK) {
                    String code = data.getStringExtra("mfacode");
                    if(code != null) {
                        if (code.length() > 0) {
                            showWaitDialog("Signing in...");
                            multiFactorAuthenticationContinuation.setMfaCode(code);
                            multiFactorAuthenticationContinuation.continueTask();
                        } else {
                            inPassword.setText("");
                            inPassword.requestFocus();
                        }
                    }
                }
                break;
            case NEW_PASSWORD_ACTIVITY_CODE:
                //New password
                closeWaitDialog();
                boolean continueSignIn = false;
                if (resultCode == RESULT_OK) {
                   continueSignIn = data.getBooleanExtra("continueSignIn", false);
                }
                if (continueSignIn) {
                    continueWithFirstTimeSignIn();
                }
        }
    }

    // App methods
    // Register user - start process
    public void signUp(View view) {
        signUpNewUser();
    }

    /**
     * Called implicitly from "Login" button
     * @param view - unused
     */
    public void logIn(View view) {
        signInUser();
    }

    // Forgot password processing
    public void forgotPassword(View view) {
        forgotpasswordUser();
    }


    // Private methods
    // Handle when the a navigation item is selected
    private void setNavDrawer() {
        nDrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                performAction(item);
                return true;
            }
        });
    }

    // Perform the action for the selected navigation item
    private void performAction(MenuItem item) {
        // Close the navigation drawer
        mDrawer.closeDrawers();

        // Find which item was selected
        switch(item.getItemId()) {
            case R.id.nav_sign_up:
                // Start sign-up
                signUpNewUser();
                break;
            case R.id.nav_sign_up_confirm:
                // Confirm new user
                confirmUser();
                break;
            case R.id.nav_sign_in_forgot_password:
                // User has forgotten the password, start the process to set a new password
                forgotpasswordUser();
                break;
            case R.id.nav_about:
                // For the inquisitive
                Intent aboutAppActivity = new Intent(this, AboutApp.class);
                startActivity(aboutAppActivity);
                break;

        }
    }

    private void signUpNewUser() {
        Intent registerActivity = new Intent(this, RegisterUser.class);
        startActivityForResult(registerActivity, REGISTER_ACTIVITY_CODE);
    }

    private void signInUser() {
        username = inUsername.getText().toString().trim();
        if(username.length() < 1) {
            TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText(inUsername.getHint()+" cannot be empty");
            inUsername.setBackground(getDrawable(R.drawable.text_border_error));
            return;
        }

        UserHelper.getInstance().setUserId(username);

        password = inPassword.getText().toString().trim();
        if(password.length() < 1) {
            TextView label = (TextView) findViewById(R.id.textViewUserPasswordMessage);
            label.setText(inPassword.getHint()+" cannot be empty");
            inPassword.setBackground(getDrawable(R.drawable.text_border_error));
            return;
        }

        String uid = this.username;
        String pwd = password;
        Log.d(TAG, String.format("User: '%s', Pwd: '%s'", uid, pwd));

        showWaitDialog("Signing in...");
        mExplicitSignIn = true;
        // TODO: Handle fallback
        UserHelper.getInstance().getPool().getUser(uid).getSessionInBackground(authenticationHandler);
    }

    private void forgotpasswordUser() {
        username = inUsername.getText().toString();

        if(username.length() < 1) {
            TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText(inUsername.getHint()+" cannot be empty");
            inUsername.setBackground(getDrawable(R.drawable.text_border_error));
            return;
        }

        showWaitDialog("");
        UserHelper.getInstance().getPool().getUser(username).forgotPasswordInBackground(forgotPasswordHandler);
    }

    private void getForgotPasswordCode(ForgotPasswordContinuation forgotPasswordContinuation) {
        this.forgotPasswordContinuation = forgotPasswordContinuation;
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        intent.putExtra("destination",forgotPasswordContinuation.getParameters().getDestination());
        intent.putExtra("deliveryMed", forgotPasswordContinuation.getParameters().getDeliveryMedium());
        startActivityForResult(intent, FORGOT_PASSWORD_ACTIVITY_CODE);
    }

    private void mfaAuth(MultiFactorAuthenticationContinuation continuation) {
        multiFactorAuthenticationContinuation = continuation;
        Intent mfaActivity = new Intent(this, MFAActivity.class);
        mfaActivity.putExtra("mode", multiFactorAuthenticationContinuation.getParameters().getDeliveryMedium());
        startActivityForResult(mfaActivity, MFA_ACTIVITY_CODE);
    }

    private void firstTimeSignIn() {
        Intent newPasswordActivity = new Intent(this, NewPassword.class);
        startActivityForResult(newPasswordActivity, NEW_PASSWORD_ACTIVITY_CODE);
    }

    private void continueWithFirstTimeSignIn() {
        newPasswordContinuation.setPassword(UserHelper.getInstance().getPasswordForFirstTimeLogin());
        Map <String, String> newAttributes = UserHelper.getInstance().getUserAttributesForFirstTimeLogin();
        if (newAttributes != null) {
            for(Map.Entry<String, String> attr: newAttributes.entrySet()) {
                Log.e(TAG, String.format("Adding attribute: %s, %s", attr.getKey(), attr.getValue()));
                newPasswordContinuation.setUserAttribute(attr.getKey(), attr.getValue());
            }
        }
        try {
            newPasswordContinuation.continueTask();
        } catch (Exception e) {
            closeWaitDialog();
            TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText("Login failed");
            inPassword.setBackground(getDrawable(R.drawable.text_border_error));

            label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText("Login failed");
            inUsername.setBackground(getDrawable(R.drawable.text_border_error));

            showDialogMessage("Login failed", UserHelper.getInstance().formatException(e));
        }
    }

    private void confirmUser() {
        Intent confirmActivity = new Intent(this, SignUpConfirm.class);
        confirmActivity.putExtra("source", "org/literacybridge/androidtbloader/main");
        startActivityForResult(confirmActivity, CONFIRM_ACTIVITY_CODE);

    }

    private void launchMainActivity() {
        Intent userActivity = new Intent(this, MainActivity.class);
        startActivityForResult(userActivity, MAIN_ACTIVITY_CODE);
    }

    // Login if a user is already present
    private void findCurrent() {
        CognitoUser user = UserHelper.getInstance().getPool().getCurrentUser();
        username = user.getUserId();
        if(username != null) {
            UserHelper.getInstance().setUserId(username);
            inUsername.setText(user.getUserId());
            if (((TBLoaderAppContext)getApplicationContext()).isCurrentlyConnected()) {
                showWaitDialog("Attempting login...");
                // TODO: Handle fallback
                user.getSessionInBackground(authenticationHandler);
            } else {
                Log.d(TAG, "Offline; continuing with cached user id");
                launchMainActivity();
            }
        }
    }

    private void getUserAuthentication(AuthenticationContinuation continuation, String username) {
        if(username != null) {
            this.username = username;
            UserHelper.getInstance().setUserId(username);
        }
        // We might have the password from the sign-up activity. If not, see if it is on the form.
        if(this.password == null) {
            inUsername.setText(username);
            password = inPassword.getText().toString();

            if(password.length() < 1) {
                TextView label = (TextView) findViewById(R.id.textViewUserPasswordMessage);
                label.setText(inPassword.getHint()+" enter password");
                inPassword.setBackground(getDrawable(R.drawable.text_border_error));
                return;
            }
        }
        String uid = this.username;
        String pwd = password;
        Log.d(TAG, String.format("getUserAuthentication with %s / %s", uid, pwd));
        AuthenticationDetails authenticationDetails = new AuthenticationDetails(uid, pwd, null);
        continuation.setAuthenticationDetails(authenticationDetails);
        continuation.continueTask();
    }

    // initialize app
    private void initApp() {
        inUsername = (EditText) findViewById(R.id.editTextUserId);
        inUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.textViewUserIdLabel);
                    label.setText(R.string.Username);
                    inUsername.setBackground(getDrawable(R.drawable.text_border_selector));
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
                label.setText("");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.textViewUserIdLabel);
                    label.setText("");
                }
            }
        });

        inPasswordLayout = (TextInputLayout) findViewById(R.id.password_layout);
        inPasswordLayout.setHintEnabled(false);

        inPassword = (EditText) findViewById(R.id.editTextUserPassword);
        inPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.textViewUserPasswordLabel);
                    label.setText(R.string.Password);
                    inPassword.setBackground(getDrawable(R.drawable.text_border_selector));
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TextView label = (TextView) findViewById(R.id.textViewUserPasswordMessage);
                label.setText("");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.length() == 0) {
                    TextView label = (TextView) findViewById(R.id.textViewUserPasswordLabel);
                    label.setText("");
                }
            }
        });
    }


    // Callbacks
    ForgotPasswordHandler forgotPasswordHandler = new ForgotPasswordHandler() {
        @Override
        public void onSuccess() {
            closeWaitDialog();
            showDialogMessage("Password successfully changed!","");
            inPassword.setText("");
            inPassword.requestFocus();
        }

        @Override
        public void getResetCode(ForgotPasswordContinuation forgotPasswordContinuation) {
            closeWaitDialog();
            getForgotPasswordCode(forgotPasswordContinuation);
        }

        @Override
        public void onFailure(Exception e) {
            closeWaitDialog();
            showDialogMessage("Forgot password failed", UserHelper.getInstance().formatException(e));
        }
    };

    //
    boolean tryFallbackAllowed = false;
    boolean inFallback = false;
    UserHelper fallbackHelperInstance = null;
    AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(CognitoUserSession cognitoUserSession, final CognitoDevice device) {
            Log.e(TAG, "Auth Success");
            ((TBLoaderAppContext)getApplicationContext()).getConfig().setIsFallbackLogin(inFallback);
            if (inFallback) {
                Log.e(TAG, "Fallback signin was successful");
                fallbackHelperInstance.setUserId(UserHelper.getInstance().getUserId());
                UserHelper.setFallbackInstance(fallbackHelperInstance);
                inFallback = false;
                fallbackHelperInstance = null;
            }

            UserHelper.getInstance().setCurrSession(getApplicationContext(), cognitoUserSession, new Runnable() {
                @Override
                public void run() {
                    UserHelper.getInstance().newDevice();
                    closeWaitDialog();
                    mSignInButton.setEnabled(false);
                    String mod = UserHelper.getInstance().getAuthenticationPayload("mod");
                    if (StringUtils.isNotBlank(mod)) {
                        String buttonText = UserHelper.getInstance().getAuthenticationPayload("modButton");
                        showDialogMessage("Message From Amplio", mod, buttonText, ()->launchMainActivity());
                    } else {
                        launchMainActivity();
                    }
                }
            });
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String username) {
            closeWaitDialog();
            Locale.setDefault(Locale.US);
            getUserAuthentication(authenticationContinuation, username);
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation) {
            closeWaitDialog();
            mfaAuth(multiFactorAuthenticationContinuation);
        }

        @Override
        public void onFailure(Exception e) {
            if (tryFallbackAllowed && !inFallback) {
                Log.e(TAG, "Attempting fallback");
                inFallback = true;
                fallbackHelperInstance = UserHelper.createInstance(getApplicationContext(), Constants.cognitoFallbackConfig);
                fallbackHelperInstance.setUserId(UserHelper.getInstance().getUserId());
                CognitoUser cognitoUser = fallbackHelperInstance.getPool().getUser(username);
                cognitoUser.getSessionInBackground(authenticationHandler);
                return;
            }

            closeWaitDialog();
            TextView label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText("Login failed");
            inPassword.setBackground(getDrawable(R.drawable.text_border_error));

            label = (TextView) findViewById(R.id.textViewUserIdMessage);
            label.setText("Login failed");
            inUsername.setBackground(getDrawable(R.drawable.text_border_error));

            showDialogMessage("Login failed", UserHelper.getInstance().formatException(e));
        }

        @Override
        public void authenticationChallenge(ChallengeContinuation continuation) {
            /**
             * For Custom authentication challenge, implement your logic to present challenge to the
             * user and pass the user's responses to the continuation.
             */
            if ("NEW_PASSWORD_REQUIRED".equals(continuation.getChallengeName())) {
                // This is the first login attempt for an admin created user
                newPasswordContinuation = (NewPasswordContinuation) continuation;
                UserHelper.getInstance().setUserAttributeForDisplayFirstLogIn(newPasswordContinuation.getCurrentUserAttributes(),
                        newPasswordContinuation.getRequiredAttributes());
                closeWaitDialog();
                firstTimeSignIn();
            }
        }
    };

    private void clearInput() {
        if(inUsername == null) {
            inUsername = (EditText) findViewById(R.id.editTextUserId);
        }

        if(inPassword == null) {
            inPassword = (EditText) findViewById(R.id.editTextUserPassword);
        }

        inUsername.setText("");
        inUsername.requestFocus();
        inUsername.setBackground(getDrawable(R.drawable.text_border_selector));
        inPassword.setText("");
        inPassword.setBackground(getDrawable(R.drawable.text_border_selector));
    }

    private void showWaitDialog(String message) {
        closeWaitDialog();
        waitDialog = new ProgressDialog(this);
        waitDialog.setTitle(message);
        waitDialog.show();
    }

    private void showDialogMessage(String title, String body) {
        showDialogMessage(title, body, "OK", ()->{});
    }

    private void showDialogMessage(String title, String body, String buttonText, Runnable onClose) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (StringUtils.isBlank(buttonText)) buttonText = "OK";
        builder.setTitle(title).setMessage(body).setNeutralButton(buttonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    userDialog.dismiss();
                    if (onClose != null) onClose.run();
                } catch (Exception e) {
                    //
                }
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }

    private void closeWaitDialog() {
        try {
            if (waitDialog != null)
                waitDialog.dismiss();
        }
        catch (Exception e) {
            //
        }
        waitDialog = null;
    }
}
