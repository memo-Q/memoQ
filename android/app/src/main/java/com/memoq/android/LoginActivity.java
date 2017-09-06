package com.memoq.android;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.memoq.android.models.User;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    @BindView(R.id.et_email) EditText mEmailEditText;
    @BindView(R.id.et_password) EditText mPasswordEditText;
    @BindView(R.id.progress_wheel) ProgressWheel mProgressWheel;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private CallbackManager mCallbackManager;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FacebookSdk.sdkInitialize(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        final int color = ContextCompat.getColor(this, R.color.white);
        mEmailEditText.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        mPasswordEditText.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                final FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser == null) {
                    Timber.d("onAuthStateChanged:signed_out");
                    return;
                }

                Timber.d("onAuthStateChanged:signed_in: %s", firebaseUser.getUid());
                User.find(firebaseUser.getUid(), new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        if (user == null) {
                            user = new User(dataSnapshot.getKey());
                            user.insertToFirebase();
                        } else {
                            user.setKey(dataSnapshot.getKey());
                        }
                        User.setCurrent(user);
                        goToNextActivity(MainActivity.class);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Timber.e(databaseError.toException());
                    }
                });
            }
        };

        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Timber.d("facebook:onSuccess: %s", loginResult);
                handleSignInFacebook(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Timber.d("facebook:onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Timber.e(error, "facebook:onError");
            }
        });

        GoogleSignInOptions gso = new GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .enableAutoManage(this, null)
            .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
            .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        mFirebaseAuth.addAuthStateListener(mAuthListener);

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            GoogleSignInResult result = opr.get();
            handleSignInGoogle(result);
        } else {
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    handleSignInGoogle(googleSignInResult);
                }
            });
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInGoogle(result);
            return;
        }
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void handleSignInFacebook(AccessToken token) {
        Timber.d("handleSignInFacebook: %s", token);
        mProgressWheel.setVisibility(View.VISIBLE);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        signInWithCredential(credential);
    }

    private void handleSignInGoogle(GoogleSignInResult result) {
        Timber.d("handleSignInGoogle: %b", result.isSuccess());
        if (result.isSuccess()) {
            mProgressWheel.setVisibility(View.VISIBLE);
            GoogleSignInAccount acct = result.getSignInAccount();
            AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
            signInWithCredential(credential);
        }
    }

    private void signInWithCredential(AuthCredential credential) {
        mFirebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    Timber.d("signInWithCredential:onComplete: %b", task.isSuccessful());
                    if (!task.isSuccessful()) {
                        mProgressWheel.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        Timber.e(task.getException(), "signInWithCredential");
                    }
                }
            });
    }

    private void goToNextActivity(Class<?> clazz) {
        mProgressWheel.setVisibility(View.GONE);
        Intent intent = new Intent(LoginActivity.this, clazz);
        startActivity(intent);
        finish();
    }

    private String getEmail() {
        return mEmailEditText.getText().toString();
    }

    private String getPassword() {
        return mPasswordEditText.getText().toString();
    }

    @OnClick(R.id.btn_login)
    public void onLoginButtonClick() {
        final String email = getEmail();
        final String password = getPassword();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.email_password_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressWheel.setVisibility(View.VISIBLE);
        mFirebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        mProgressWheel.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, R.string.login_error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    @OnClick(R.id.btn_fb_login)
    public void onFacebookLoginButtonClick() {
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"));
    }

    @OnClick(R.id.btn_google_login)
    public void onGoogleLoginButtonClick() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @OnClick(R.id.txt_create_account)
    public void onCreateAccountButtonClick() {
        final String email = getEmail();
        final String password = getPassword();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.email_password_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressWheel.setVisibility(View.VISIBLE);
        mFirebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        mProgressWheel.setVisibility(View.GONE);
                        try {
                            throw task.getException();
                        } catch(FirebaseAuthWeakPasswordException e) {
                            Toast.makeText(LoginActivity.this, R.string.password_week, Toast.LENGTH_SHORT).show();
                        } catch(FirebaseAuthInvalidCredentialsException e) {
                            Toast.makeText(LoginActivity.this, R.string.email_malformed, Toast.LENGTH_SHORT).show();
                        } catch(FirebaseAuthUserCollisionException e) {
                            Toast.makeText(LoginActivity.this, R.string.email_collision, Toast.LENGTH_SHORT).show();
                        } catch(Exception e) {
                            Timber.e(e);
                        }
                    }
                }
            });
    }

    @OnClick(R.id.txt_forget_password)
    public void onForgetPasswordButtonClick() {
        final String email = getEmail();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, R.string.email_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        mFirebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, R.string.forget_password_msg, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, R.string.forget_password_error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

}
