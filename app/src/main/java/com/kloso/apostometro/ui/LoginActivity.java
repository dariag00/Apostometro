package com.kloso.apostometro.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kloso.apostometro.Constants;
import com.kloso.apostometro.FirestoreViewModel;
import com.kloso.apostometro.R;
import com.kloso.apostometro.model.User;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {

    String TAG = LoginActivity.class.getName();

    @BindView(R.id.pb_login)
    ProgressBar progressBar;
    @BindView(R.id.et_login_email)
    EditText emailEditText;
    @BindView(R.id.et_login_password)
    EditText passwordEditText;
    @BindView(R.id.button_login)
    Button loginButton;
    @BindView(R.id.form_login)
    ScrollView formLogin;
    @BindView(R.id.button_sign_up)
    Button signUpButton;
    @BindView(R.id.google_sign_in)
    SignInButton googleSignInButton;
    @BindView(R.id.iv_login_image)
    ImageView loginImageView;

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private int RC_SIGN_IN = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ButterKnife.bind(this);

        checkIfUserAlreadyLoggedIn();

        loginButton.setOnClickListener(it -> processLoginAttempt());

        signUpButton.setOnClickListener(it -> goToSignUpActivity());

        changeFormVisibility(true);

        firebaseAuth = FirebaseAuth.getInstance();

        initGoogleSignInClient();

        googleSignInButton.setOnClickListener(it -> googleSignIn());

    }

    private void checkIfUserAlreadyLoggedIn(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        } else {
            Log.d(TAG, "checkIfUserAlreadyLoggedIn:User is signed out");
        }
    }

    private void initGoogleSignInClient(){
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
    }

    private void googleSignIn() {
        changeFormVisibility(false);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            System.out.println("onActivityResult");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Login is unavailable, try again later. " + e.toString(), Toast.LENGTH_SHORT).show();
                changeFormVisibility(true);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount googleSignInAccount) {
        System.out.println("firebaseAuthWithGoogle");
        AuthCredential credential = GoogleAuthProvider.getCredential(googleSignInAccount.getIdToken(), null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        Log.d(TAG, "firebaseAuthWithGoogle:success");

                        User user = new User();
                        user.setEmail(firebaseUser.getEmail());
                        user.setUsername(googleSignInAccount.getDisplayName());
                        user.setName(googleSignInAccount.getGivenName());
                        user.setSurname(googleSignInAccount.getFamilyName());
                        user.setProfilePicUri(googleSignInAccount.getPhotoUrl().toString());

                        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(runnable -> {
                            user.setTokenId(runnable.getResult());
                            FirestoreViewModel firestoreViewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);
                            firestoreViewModel.saveUser(user);

                            processLoginResult(firebaseUser, user,null);
                        });
                    } else {
                        Log.w(TAG, "firebaseAuthWithGoogle:failure", task.getException());
                        processLoginResult(null, null, task.getException().getMessage());
                    }
                });
    }

    private void processLoginAttempt(){
        if(validateLoginAttempt()) {
            changeFormVisibility(false);
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();

            Log.d(TAG, "Login attempt received. Username: <" + email + "> password: <" + password + ">");

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {

                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                            FirestoreViewModel firestoreViewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);
                            firestoreViewModel.getUserByEmail(firebaseUser.getEmail()).observe(this, user -> {
                                processLoginResult(firebaseUser, user,null);
                            });
                        } else {
                            Log.w(TAG, "processLoginAttempt: Sign in error: ", task.getException());
                            processLoginResult(null, null, task.getException().getMessage());
                        }
                    });
        }
    }

    private boolean validateLoginAttempt(){

        boolean validAttempt = true;

        if(emailEditText.getText().toString().isEmpty()){
            emailEditText.setError("Email is empty!");
            emailEditText.requestFocus();
            validAttempt = false;
        } else if(passwordEditText.getText().toString().isEmpty()){
            passwordEditText.setError("Password is empty!");
            passwordEditText.requestFocus();
            validAttempt = false;
        }

        return validAttempt;
    }

    private void changeFormVisibility(boolean showLoginForm){
        formLogin.setVisibility(showLoginForm ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(showLoginForm ? View.GONE :  View.VISIBLE);
    }

    private void goToSignUpActivity(){
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    private void processLoginResult(FirebaseUser user, User dbUser, String message){
        if(user != null) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(Constants.USER, dbUser);
            startActivity(intent);
        } else {
            Snackbar.make(formLogin, "Authentication Failed: " + message, Snackbar.LENGTH_LONG).show();
        }
        changeFormVisibility(true);
    }
}
