package com.project.chatflix.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.project.chatflix.MainActivity;
import com.project.chatflix.R;
import com.project.chatflix.utils.StaticConfig;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends Activity
//        implements SinchService.StartFailedListener
{

    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";

    public static final String MY_PREFERENCES = "MyPrefs";

    EditText txtEmailLogin, txtPasswordLogin;
    Button btnSignup, btnReg;
    String email, password;

    private final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    private ProgressDialog progressDialog;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseUser user;
    private boolean firstTimeAccess;
    private LinearLayout layoutRememberLogin;
    SharedPreferences sharedpreferences;

    private LovelyProgressDialog waitingDialog;
    private ImageView imgRemember;
    private Button btnForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
        addEvent();
        loadAccount();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            email = (String) extras.get(getString(R.string.email));
            password = (String) extras.get(getString(R.string.password_field));
            txtEmailLogin.setText(email);
            txtPasswordLogin.setText(password);
        }
        firstTimeAccess = true;
        initFirebase();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = firebaseAuth -> {
            user = firebaseAuth.getCurrentUser();
            if (user != null) {
                StaticConfig.UID = user.getUid();
                if (firstTimeAccess) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    LoginActivity.this.finish();
                }
            }
            firstTimeAccess = false;
        };
    }

    private void initView() {
        getWindow().setBackgroundDrawableResource(R.drawable.background);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        waitingDialog = new LovelyProgressDialog(this);
        txtEmailLogin = findViewById(R.id.txtEmailLogin);
        txtPasswordLogin = findViewById(R.id.txtPasswordLogin);
        btnSignup = findViewById(R.id.txtSignup);
        btnReg = findViewById(R.id.btnReg);
        layoutRememberLogin = findViewById(R.id.layout_remember_login);
        imgRemember = findViewById(R.id.radioButton);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);

        waitingDialog = new LovelyProgressDialog(this).setCancelable(false);
    }

    private void addEvent() {
        btnReg.setOnClickListener(v -> {
            Intent reg_intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(reg_intent);
        });

        btnSignup.setOnClickListener(v -> {

            String email = txtEmailLogin.getText().toString();
            String password = txtPasswordLogin.getText().toString();

            if (!TextUtils.isEmpty(email) || !TextUtils.isEmpty(password)) {

                if (validate(email, password)) {
                    progressDialog.setTitle(getString(R.string.sign_in));
                    progressDialog.setMessage(getString(R.string.please_wait));
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.show();

                    loginUser(email, password);
                    if (imgRemember.getTag().toString().equalsIgnoreCase("1")) {
                        SharedPreferences.Editor editor = sharedpreferences.edit();

                        editor.putString(EMAIL, email);
                        editor.putString(PASSWORD, password);
                        editor.apply();
                    }
//                        if (!getSinchServiceInterface().isStarted()) {
//                            getSinchServiceInterface().startClient(email);
////                            Toast.makeText(LoginActivity.this,"Start Service",Toast.LENGTH_SHORT).show();
//                        }

                } else {
                    Toast.makeText(LoginActivity.this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(LoginActivity.this, getString(R.string.fill_all_requirements), Toast.LENGTH_SHORT).show();
            }
        });

        layoutRememberLogin.setOnClickListener(v -> {
            int tag = Integer.parseInt(imgRemember.getTag().toString());

            if (tag == 0) {
                imgRemember.setTag(1);
                imgRemember.setBackgroundResource(R.drawable.rb_checked);
            } else {
                imgRemember.setTag(0);
                imgRemember.setBackgroundResource(R.drawable.rb_unchecked);
            }
        });

        btnForgotPassword.setOnClickListener(v -> {
            new LovelyTextInputDialog(this, R.style.EditTextTintTheme)
                    .setTopColorRes(R.color.colorPrimary)
                    .setTitle(getString(R.string.reset_password))
                    .setMessage(getString(R.string.enter_your_email))
                    .setIcon(R.drawable.ic_lock)
                    .setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                    .setInputFilter(getString(R.string.email_not_found), text -> {
                        Pattern VALID_EMAIL_ADDRESS_REGEX =
                                Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
                        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(text);
                        return matcher.find();
                    })
                    .setConfirmButton(android.R.string.ok, text -> {
                        sendEmailResetPassword(text);
                    })
                    .show();
        });
    }

    private void sendEmailResetPassword(String text) {
        waitingDialog.setCancelable(false)
                .setIcon(R.drawable.ic_add_friend)
                .setTitle(getString(R.string.sending_email))
                .setTopColorRes(R.color.colorPrimary)
                .show();
        mAuth.sendPasswordResetEmail(text)
                .addOnCompleteListener(task -> {
                    waitingDialog.dismiss();
                    if (task.isSuccessful() && task.isComplete()) {
                        Toast.makeText(this, getString(R.string.we_sent_email_reset_password), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, getString(R.string.error_occured_please_try_again), Toast.LENGTH_LONG).show();
                    }
                });

    }

//    @Override
//    protected void onServiceConnected() {
//        getSinchServiceInterface().setStartListener(this);
//    }
//
//    @Override
//    public void onStartFailed(SinchError error) {
//        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show();
//    }

//    @Override
//    public void onStarted() {
//
//    }

    private void loadAccount() {
        sharedpreferences = getSharedPreferences(MY_PREFERENCES,
                Context.MODE_PRIVATE);
        if (sharedpreferences.contains(EMAIL)) {
            txtEmailLogin.setText(sharedpreferences.getString(EMAIL, ""));
        }
        if (sharedpreferences.contains(PASSWORD)) {
            txtPasswordLogin.setText(sharedpreferences.getString(PASSWORD, ""));
        }
    }

    private boolean validate(String emailStr, String password) {
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        return (password.length() > 0 || password.equals(";")) && matcher.find();
    }

    private void loginUser(final String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                progressDialog.dismiss();
                // Khởi động service Sinch
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                progressDialog.hide();
                Toast.makeText(LoginActivity.this, getString(R.string.email_password_invalid), Toast.LENGTH_LONG).show();
            }
        });
    }
}
