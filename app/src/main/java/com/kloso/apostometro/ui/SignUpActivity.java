package com.kloso.apostometro.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kloso.apostometro.FirestoreViewModel;
import com.kloso.apostometro.R;
import com.kloso.apostometro.model.User;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignUpActivity extends AppCompatActivity {


    String TAG = SignUpActivity.class.getName();

    @BindView(R.id.pb_sign_up)
    ProgressBar progressBar;
    @BindView(R.id.et_sign_up_email)
    EditText emailEditText;
    @BindView(R.id.et_sign_up_password)
    EditText passwordEditText;
    @BindView(R.id.et_sign_up_repeat_password)
    EditText repeatPasswordEditText;
    @BindView(R.id.et_sign_up_name)
    EditText nameEditText;
    @BindView(R.id.et_sign_up_surname)
    EditText surnameEditText;
    @BindView(R.id.form_sign_up)
    ScrollView formSignUp;
    @BindView(R.id.button_sign_up)
    Button signUpButton;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        ButterKnife.bind(this);

        signUpButton.setOnClickListener(it -> processSignUpAttempt());

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence password, int i, int i1, int i2) {
                if (password.length() < 6) {
                    passwordEditText.setError("Password length must be of at least, 6 characters");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        repeatPasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence repeatPassword, int i, int i1, int i2) {
                String password = passwordEditText.getText().toString();
                if (!password.contentEquals(repeatPassword)) {
                    repeatPasswordEditText.setError("Passwords do not match");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        firebaseAuth = FirebaseAuth.getInstance();

    }

    @Override
    protected void onResume() {
        super.onResume();
        changeFormVisibility(true);
    }

    private void processSignUpAttempt() {
        changeFormVisibility(false);
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String repeatPassword = repeatPasswordEditText.getText().toString();
        String name = nameEditText.getText().toString();
        String surname = surnameEditText.getText().toString();

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setSurname(surname);

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(runnable -> {
            user.setTokenId(runnable.getResult());

            Log.d(TAG, "SignUp attempt received. Username: <" + email + "> password: <" + password + ">");

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirestoreViewModel firestoreViewModel = new ViewModelProvider(this).get(FirestoreViewModel.class);
                            firestoreViewModel.saveUser(user);
                            Intent intent = new Intent(this, MainActivity.class);
                            startActivity(intent);
                        } else {
                            Log.w(TAG, "processSignUpAttempt: Sign up error: ", task.getException());
                            Toast.makeText(this, "Error when signing up: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            changeFormVisibility(true);
                        }
                    });


            validatePassword(password, repeatPassword);
        });
    }

    private void changeFormVisibility(boolean showLoginForm) {
        formSignUp.setVisibility(showLoginForm ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(showLoginForm ? View.GONE : View.VISIBLE);
    }

    private boolean validatePassword(String password, String repeatPassword) {

        boolean correct = false;

        if (!password.equals(repeatPassword)) {
            passwordEditText.setError("Passwords do not match");
        } else if (password.length() < 6) {
            passwordEditText.setError("Password length must be of at least, 6 characters");
        } else {
            correct = true;
        }

        return correct;
    }
}
